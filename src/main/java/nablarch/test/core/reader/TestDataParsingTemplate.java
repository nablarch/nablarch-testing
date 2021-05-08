package nablarch.test.core.reader;

import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.Builder.concat;


/**
 * テストデータ解析用のテンプレートクラス。
 *
 * @param <RET> 解析後の型
 * @author T.Kawasaki
 */
abstract class TestDataParsingTemplate<RET> {

    /** 解析に使用するリーダ */
    private final TestDataReader reader;

    /** 委譲先の{@link nablarch.test.core.util.interpreter.TestDataInterpreter} */
    private final List<TestDataInterpreter> interpreters;

    /** 解析対象のデータ型 */
    private final DataType targetType;

    /** テストデータのキャッシュ */
    private static final Map<String, List<List<String>>> TEST_DATA_CACHE = NablarchTestUtils.createLRUMap(8);

    /** テストデータ */
    private List<List<String>> testData;

    /** テストデータを読み込む際のインデックス */
    private int index;

    /** ディレクトリ */
    protected String directory;     // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /** リソース名 */
    protected String resource;      // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化が不要なため

    /**
     * 行読み込み時に起動されるコールバックメソッド。
     *
     * @param line 読み込まれた行データ
     */
    abstract void onReadLine(List<String> line);

    /**
     * 処理対象のデータ型を発見した場合に起動されるコールバックメソッド。
     *
     * @param line データ型の定義を含む行データ
     */
    abstract void onTargetTypeFound(List<String> line);


    /**
     * 処理対象のデータ型であるかどうか判定する。
     *
     * @param line 読み込み中の行データ
     * @param id ID
     * @return 処理対象のデータ型である場合、真
     */
    abstract boolean isTargetType(List<String> line, String id);

    /**
     * 次の処理対象のデータを発見した場合に処理を停止するか否かを判定する。
     *
     * @return 次の処理対象のデータを発見した場合に処理停止する場合は、真
     */
    abstract boolean shouldStopOnNextOne();

    /**
     * 解析結果を返却する。
     *
     * @return 解析結果
     */
    abstract RET getResult();

    /**
     * コンストラクタ。
     *
     * @param reader       解析に使用するリーダ
     * @param interpreters 解釈クラス
     * @param targetType   解析対象のデータ型
     */
    TestDataParsingTemplate(TestDataReader reader, List<TestDataInterpreter> interpreters, DataType targetType) {
        this.reader = reader;
        this.interpreters = interpreters;
        this.targetType = targetType;
    }

    /**
     * 解析対象のデータ型を返却する。
     *
     * @return 解析対象のデータ型
     */
    protected final DataType getTargetType() {
        return targetType;
    }

    /**
     * 解析を実行する。
     * <p>
     * 読み込んだシートはキャッシュする。
     * </p>
     * @param directory 読み込み元のディレクトリ
     * @param resource  リソース名称
     * @param id        ID
     */
    public final void parse(String directory, String resource, String id) {
        parse(directory, resource, id, true);
    }
    
    /**
     * 解析を実行する。
     * @param directory 読み込み元のディレクトリ
     * @param resource  リソース名称
     * @param id        ID
     * @param saveCache  読み込んだファイルのデータをキャッシュに保存するか
     */
    public final void parse(String directory, String resource, String id, boolean saveCache) {
        this.directory = directory;
        this.resource = resource;
        String dataCacheKey = directory + '/' + resource;
        if (!TEST_DATA_CACHE.containsKey(dataCacheKey)) {
            // テストデータがキャッシュ上に存在しない場合は、テストデータをロードしキャッシュする。
            if (reader instanceof PoiXlsReader) {
                ((PoiXlsReader) reader).setUseCache(saveCache);
            }
            reader.open(directory, resource);
            try {
                testData = readTestData();
                if (saveCache) {
                    TEST_DATA_CACHE.put(dataCacheKey, testData);
                }
            } finally {
                reader.close();
            }
        } else {
            testData = TEST_DATA_CACHE.get(dataCacheKey);
        }

        try {
            index = 0;
            parse(id);
        } catch (RuntimeException e) {
            String msg = concat("can't get data. ",
                    "directory=[", directory, "] resource=[", resource, "] id=[", id, "]");
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * テストデータを読み込む。
     *
     * @return 読み込んだテストデータ
     */
    private List<List<String>> readTestData() {
        List<List<String>> lines = new ArrayList<List<String>>(1024);
        while (true) {
            List<String> line = reader.readLine();
            if (line == null) {
                break;
            }
            if (isCommentRow(line)) {
                continue;
            }
            line = cutComment(line);
            if (isBlankLine(line)) {
                continue;
            }
            List<String> interpret = interpret(line);
            // キャッシュするので、他の箇所で書き換えられないようにする。
            List<String> unmodifiable = Collections.unmodifiableList(interpret);
            lines.add(unmodifiable);
        }
        // キャッシュするので、他の箇所で書き換えられないようにする。
        return Collections.unmodifiableList(lines);
    }

    /**
     * 解析を実行する。
     *
     * @param id ID
     */
    void parse(String id) {
        boolean nowReading = false;   // データ行読み込み中か
        List<String> line;
        while ((line = readLine()) != null) {
            // テーブル名の行
            String firstCol = line.get(0);
            DataType dataType = getDataType(firstCol);
            // データタイプとグループIDが一致するか
            if (isTargetType(line, id)) {
                if (nowReading && shouldStopOnNextOne()) {
                    break;
                }
                nowReading = true;
                onTargetTypeFound(line);
            } else if (dataType.equals(DataType.DEFAULT)) {
                // データ行読み込み中か？
                if (nowReading) {
                    onReadLine(line);
                }
            } else {
                // データ行読み込み中なら処理終了
                if (nowReading) {
                    break;
                }
            }
        }
    }

    /** データタイプのリスト */
    private static final DataType[] DATA_TYPES = DataType.values();

    /**
     * データタイプを返却する。<br/>
     *
     * @param dataTypeCell データタイプが記載されたセル
     * @return データタイプ
     */
    protected final DataType getDataType(String dataTypeCell) {

        if (dataTypeCell == null) {
            return DataType.DEFAULT;
        }

        for (DataType type : DATA_TYPES) {
            if (dataTypeCell.startsWith(type.getName())) {
                return type;
            }
        }
        return DataType.DEFAULT;
    }

    /**
     * テストデータタイプのバリュー値を取得する。
     *
     * @param dataTypeRow 1行分のデータ
     * @return バリュー値
     */
    protected final String getTypeValue(List<String> dataTypeRow) {
        String str = dataTypeRow.get(0);
        return str.substring(str.indexOf('=') + 1);
    }

    /**
     * テストデータから1行文のデータを読み込む。<br/>
     * テストデータをすべて読み込んだ場合は、nullを返却する。
     *
     * @return 1行データ
     */
    protected final List<String> readLine() {
        if (index < testData.size()) {
            return testData.get(index++);
        }
        return null;
    }

    /** コメントを表す文字列 */
    private static final String COMMENT_EXPRESSION = "//";

    /**
     * コメント行であるか判定する。<br/>
     * 先頭が//始まりの場合はコメント行とみなす。
     *
     * @param line 行データ
     * @return 判定結果
     */
    boolean isCommentRow(List<String> line) {
        String first = line.get(0);
        return isComment(first);
    }

    /**
     * コメントセルであるかどうか判定する。
     *
     * @param cell セルの値
     * @return そのセルの値が//からスタートしている場合、真
     */
    private boolean isComment(String cell) {
        return cell != null && cell.startsWith(COMMENT_EXPRESSION);
    }
    /**
     * 1行を表すデータからコメントを削除する。<br>
     * コメント部は、「//」以降の要素
     *
     * @param src 1行を表すテストデータ
     * @return コメント部を取り除いた1行データ
     */
    private List<String> cutComment(List<String> src) {
        List<String> result = new ArrayList<String>(src.size());
        for (String s : src) {
            if (isComment(s)) {
                break;
            }
            result.add(s);
        }
        return result;
    }

    /**
     * 全要素が空かどうか判定
     *
     * @param line １行分のデータ
     * @return 配列の全要素が空であればtrue、空でなければfalse
     */
    private boolean isBlankLine(List<String> line) {
        return StringUtil.isNullOrEmpty(line);
    }

    /**
     * テストデータ記法を解釈する。
     *
     * @param originalLine 元のデータ行
     * @return 解釈後のデータ行
     */
    private List<String> interpret(List<String> originalLine) {
        List<String> result = new ArrayList<String>(originalLine.size());
        for (String e : originalLine) {
            InterpretationContext context
                    = new InterpretationContext(e, interpreters);
            String interpreted = context.invokeNext();
            result.add(interpreted);
        }
        return result;
    }
}
