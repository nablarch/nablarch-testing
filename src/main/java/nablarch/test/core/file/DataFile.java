package nablarch.test.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport.Directive;
import nablarch.core.dataformat.FileRecordWriter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.core.util.annotation.Published;
import nablarch.test.core.util.MapCollector;

/**
 * ファイルを表すクラス。<br/>
 * <p>
 * ファイルへ書き出す機能（準備データ作成用）と
 * ファイルから読み込む機能（結果ファイル確認用）を持つ。
 * </p>
 * <p>
 * 本インスタンスは複数のDataFileFragmentにより構成される。
 * ファイル全体に関わる情報（ディレクティブなど）は本クラスが保持し、
 * レコードレイアウト毎の情報（レコード長など）はDataFileFragmentが保持する。
 * </p>
 * ほとんどの機能は本クラスで実装されているが、ファイルタイプにより処理が異なる部分は
 * サブクラスで定義される。
 *
 * @author T.Kawasaki
 */
@Published(tag = "architect")
public abstract class DataFile {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(DataFile.class);

    /** この固定長ファイルを構成する断片 */
    protected final List<DataFileFragment> all = new ArrayList<DataFileFragment>();  // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** ファイルパス */
    private final String path;

    /** ディレクティブ一覧 */
    protected final Map<String, Object> directives = new HashMap<String, Object>();  // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** ディレクティブのデフォルト値取得用キー */
    private static final String DEFAULT_DIRECTIVES = "defaultDirectives";

    /**
     * デフォルトのディレクティブを取得する。<br/>
     * デフォルト値が設定されていない場合は空のMapを返却する（デフォルト値なし）。
     *
     * @param key コンポーネント設定ファイルのキー
     */
    protected final void prepareDefaultDirectives(String key) {
        Map<String, String> defaultDirectives = SystemRepository.get(key);
        if (defaultDirectives == null) {
            return;
        }
        new MapCollector<Void, String, String>() {
            @Override
            protected Void evaluate(String key1, String value) {
                setDirective(key1, value);
                return null;
            }
        }
        .collect(defaultDirectives);
    }


    /**
     * コンストラクタ。<br/>
     *
     * @param path ファイルパス
     */
    protected DataFile(String path) {
        this.path = path;
        prepareDefaultDirectives(DEFAULT_DIRECTIVES);
        setDirective("file-type", getFileType());
    }


    /**
     * ファイルタイプを取得する。
     *
     * @return ファイルタイプ
     */
    protected abstract String getFileType();


    /**
     * ファイルへ出力する。<br/>
     * 出力先はコンストラクタで設定したパスとなる。
     */
    public void write() {
        FileRecordWriter writer = null;
        LayoutDefinition layout = createLayout();
        File outFile = new File(path);
        try {
            writer = new FileRecordWriter(outFile, layout);
            for (DataFileFragment e : all) {
                e.writeWith(writer);
            }
        } catch (RuntimeException e) {
            LOGGER.logDebug("exception occurred in writing file. file=[" + path + "] ");
            throw e;
        } finally {
            FileUtil.closeQuietly(writer);
        }
    }

    /**
     * この固定長ファイルを構成する、新しい断片を取得する。
     *
     * @return DataFileFragment
     */
    public DataFileFragment getNewFragment() {
        // レコード種別を判別する必要がないので、
        // FixedLengthRecordFormatterはシングルレイアウトのものを使用する。
        // マルチレイアウトの場合は、シングルレイアウトの組み合わせで対応する。
        DataFileFragment fragment = createNewFragment();
        all.add(fragment);
        return fragment;
    }

    /**
     * 新しいファイルの断片を生成する。<br/>
     * 生成された断片はこのファイルの管理下に置かれる。
     *
     * @return 新しいファイル断片
     */
    protected abstract DataFileFragment createNewFragment();


    /**
     * {@link DataRecord}へ変換する。<br/>
     * 本メソッドの結果と、{@link #read()}メソッドの結果を突合することで、
     * 期待データと実際のファイルとの比較を行うことができる。
     *
     * @return 変換結果
     */
    public List<DataRecord> toDataRecords() {
        List<DataRecord> dataRecords = new ArrayList<DataRecord>();
        for (DataFileFragment e : all) {
            dataRecords.addAll(e.toDataRecords());
        }
        return dataRecords;
    }


    /**
     * ファイルから読み込む。<br/>
     * <p>
     * 読み込み先はコンストラクタで指定したファイルパスとなる。
     * 読み込み時に使用されるファイルフォーマットは、
     * 本インスタンスが持つDataFileFragmentを使用する。
     * </p>
     * <p>
     * 本インスタンスに対応するデータを読み終えたあとで、
     * 入力ストリームが終端に達していない場合、最末尾のレコードレイアウトで残りのストリームを読み切る。
     * </p>
     *
     * @return 読み込み結果
     */
    public List<DataRecord> read() {

        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return read(in);
        } catch (IOException e) {
            throw new RuntimeException("read file failed. path=[" + path + "]", e);
        } finally {
            FileUtil.closeQuietly(in);
        }
    }

    /**
     * データレコードを読み込む。<br/>
     *
     * @param in 入力元となる入力ストリーム
     * @return 読み込んだデータレコード全件
     * @throws IOException 入出力例外
     */
    private List<DataRecord> read(InputStream in) throws IOException {
        List<DataRecord> result = new ArrayList<DataRecord>();
        DataRecordFormatter formatter = null;
        for (Iterator<DataFileFragment> iterator = all.iterator(); iterator.hasNext();) {
            DataFileFragment fragment = iterator.next();
            LayoutDefinition layout = createLayout(fragment);
            if (formatter == null) {
                // 初回は新規作成
                formatter = createFormatter(layout).setInputStream(in).initialize();
            } else {
                // 2回目以降は定義を設定し直す。
                formatter.setDefinition(layout).initialize();
            }
            // 読み込みレコード数(
            int numberOfRecordToRead = fragment.getNumberOfRecords();
            // 最後の要素かどうか判定（最後の要素であればEOFまで読みきる）
            boolean readTilEof = !iterator.hasNext();
            // 指定したレコード数分だけ読み込み
            List<DataRecord> recordsOfThisFragment
                    = read(formatter, numberOfRecordToRead, readTilEof);
            if (recordsOfThisFragment == null) {  // EOF
                break;
            }
            result.addAll(recordsOfThisFragment);
        }
        return result;
    }

    /**
     * 指定したレコード数分だけレコードを読み込む。
     *
     * @param formatter  読み込みに使用するフォーマッタ
     * @param size       読み込むレコード件数
     * @param readTilEof EOFまで読みきるかどうか
     * @return 読み込んだレコード
     * @throws IOException 入出力例外
     */
    private List<DataRecord> read(DataRecordFormatter formatter, int size, boolean readTilEof)
            throws IOException {
        List<DataRecord> result = new ArrayList<DataRecord>();
        // この断片がデータを持たない場合、空のリストを返却
        if (size == 0 && !readTilEof) {
            return result;
        }
        for (int i = 0; i < size || readTilEof; i++) {  // EOFまで読む場合サイズは無視される
            DataRecord record = formatter.readRecord();
            if (record == null) {     // EOF
                break;
            }
            result.add(record);
        }
        return result.isEmpty()
                ? null          // ファイルから1レコードも読めずにEOFに到達した場合
                : result;       // 1レコード以上読み込めた場合
    }

    /**
     * フォーマット定義を作成する。<br/>
     * 本インスタンスおよび所有するDataFileFragmentのレイアウト情報を元に
     * フォーマット定義を生成する。
     * 書き込み時に使用する。
     * （書き込み時はレイアウト種別を明示的に指定できるため、１つのレイアウトでまかなえる）
     *
     * @return フォーマット定義
     */
    public LayoutDefinition createLayout() {
        return createLayout(all.toArray(new DataFileFragment[all.size()]));
    }

    /**
     * フォーマット定義を作成する。<br/>
     * 本インスタンスおよび所有するDataFileFragmentのレイアウト情報を元に
     * フォーマット定義を生成する。
     *
     * @param fragments 元となる断片
     * @return フォーマット定義
     */
    public LayoutDefinition createLayout(DataFileFragment... fragments) {
        LayoutDefinition layout = new LayoutDefinition();
        // ディレクティブ
        layout.getDirective().putAll(directives);
        // レコード定義
        for (DataFileFragment e : fragments) {
            layout.addRecord(e.getRecordDefinition());
        }
        return layout;
    }


    /**
     * ディレクティブを設定する。
     *
     * @param directiveName ディレクティブ名称
     * @param stringValue   値
     * @throws IllegalArgumentException 許容されないディレクティブが設定された場合
     */
    public void setDirective(String directiveName, String stringValue) throws IllegalArgumentException {
        Directive directive = valueOf(directiveName);
        if (directive == null) {
            // 許容する以外のディレクティブが含まれていた場合
            throw new IllegalArgumentException(Builder.concat(
                    "invalid directive found. [", directiveName, "]"));
        } else if (directive.equals(Directive.TEXT_ENCODING)) {
            // エンコーディング指定の場合、値を保持
            encoding = Charset.forName(stringValue);
        }
        Object value = convertDirectiveValue(directive, stringValue.trim());
        directives.put(directiveName, value);
    }


    /**
     * ファイルパスを取得する。
     *
     * @return ファイルパス
     */
    public String getPath() {
        return path;
    }

    /**
     * ディレクティブの値を、文字列から各ディレクティブが許容する型に変換する。
     *
     * @param directive   ディレクティブ
     * @param stringValue 値
     * @return 変換後の値
     */
    protected Object convertDirectiveValue(Directive directive, String stringValue) {
        if (directive.equals(Directive.RECORD_SEPARATOR)) {
            // レコード終端文字
            return LineSeparator.evaluate(stringValue);
        }
        // 上記以外
        // ディレクティブが許容する型を表すClassを取得
        Class<?> valueType = directive.getType();
        return Builder.valueOf(valueType, stringValue);
    }

    /**
     * ディレクティブ名称からディレクティブを取得する。
     *
     * @param directiveName ディレクティブ名称
     * @return ディレクティブ
     */
    protected abstract Directive valueOf(String directiveName);

    /**
     * ディレクティブからエンコーディングを取得する。
     *
     * @return ディレクティブで定義されたエンコーディングF
     */
    protected final Charset getEncodingFromDirectives() {
        return encoding;
    }

    /** ディレクティブで指定されたエンコーディング */
    private Charset encoding;

    /**
     * フォーマッタを作成する。
     *
     * @param layout フォーマット定義
     * @return 作成したフォーマッタ
     */
    private DataRecordFormatter createFormatter(LayoutDefinition layout) {
        return FormatterFactory.getInstance().createFormatter(layout);
    }

}
