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
 * <h2>キャッシュの構成</h2>
 * <p>
 * キャッシュは2段で構成する。
 * </p>
 * <ul>
 *   <li>生テストデータのキャッシュ（{@code TEST_DATA_CACHE}）: 本クラスが持つ。
 *       リーダで読み込み、コメント・空行を除いてインタープリタで変換した行データを、
 *       {@code ディレクトリ/リソース名} をキーに保持する。</li>
 *   <li>解析結果のキャッシュ: サブクラスが持つ。行データから組み立てた解析結果を保持する。</li>
 * </ul>
 * <p>
 * {@link #parse(String, String, String, boolean)} の {@code saveCache} が {@code false} の場合は、
 * どちらのキャッシュも参照せず、保存もしない。この経路は、本体の static キャッシュを参照も汚染もせずに
 * テストデータを読みたい場合に使う。例えば、Excel→YAML 変換ツール（nablarch-testing-converter）が
 * 本体のパーサで Excel を読む場合や、テスト実行中に更新されたメッセージを読み直す
 * {@link BasicTestDataParser#getMessageWithoutCache(String, String, DataType, String)} である。
 * </p>
 *
 * <h2>解析結果のキャッシュを持つサブクラスの実装規約</h2>
 * <ul>
 *   <li>{@link #cacheEnabled()} で {@code true} を返す。</li>
 *   <li>{@link #prepareResult()}・{@link #tryLoadFromCache(String)}・{@link #storeToCache(String)} を実装する。
 *       本クラスの既定実装は、キャッシュを持たないサブクラス向けのもので、キャッシュを持つサブクラスからは呼ばれない。</li>
 *   <li>既定の流れ（{@link #cachedParse(String)}）では、{@link #storeToCache(String)} を
 *       {@link #doParse(String)} より前に呼ぶ。空の結果リストの参照を先にキャッシュへ登録し、
 *       {@link #doParse(String)} がその同じリストへ要素を追加するためである。</li>
 *   <li>独自のキャッシュ戦略を持つ場合は、{@link #cachedParse(String)} を override してよい
 *       （例: {@link DataFileParser}）。</li>
 * </ul>
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

    /** 生テストデータのキャッシュ（キャッシュの構成はクラスの Javadoc を参照） */
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
     * キャッシュを参照・保存するか。
     * {@code false} の場合は、生テストデータのキャッシュ・解析結果のキャッシュのどちらも参照せず、書き込みもしない。
     */
    private boolean saveCache = true;

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
     * @param saveCache  キャッシュを参照・保存するか。{@code false} の場合はキャッシュを参照せず、ファイルを読み直す
     */
    public final void parse(String directory, String resource, String id, boolean saveCache) {
        this.directory = directory;
        this.resource = resource;
        this.saveCache = saveCache;
        String dataCacheKey = directory + '/' + resource;
        if (saveCache && TEST_DATA_CACHE.containsKey(dataCacheKey)) {
            testData = TEST_DATA_CACHE.get(dataCacheKey);
        } else {
            // テストデータがキャッシュ上に存在しない場合、またはキャッシュを使わない場合は、テストデータをロードする。
            // キャッシュを使う場合だけ、ロードしたテストデータをキャッシュする。
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
     * <p>
     * キャッシュを持つサブクラス（{@code cacheEnabled() == true}）の場合、
     * {@code saveCache} が {@code true} のときのみキャッシュへの保存・参照を行う。
     * {@code saveCache} が {@code false}（変換ツール等のキャッシュ抑止経路）、
     * またはキャッシュを持たないサブクラスの場合は、キャッシュを介さず直接解析する。
     * </p>
     *
     * @param id ID
     */
    void parse(String id) {
        if (!cacheEnabled() || !saveCache) {
            // キャッシュを持たない、またはキャッシュ抑止経路: 直接解析する。
            prepareResult();
            doParse(id);
            return;
        }
        cachedParse(id);
    }

    /**
     * キャッシュ経路の解析。{@code cacheEnabled() == true} かつ {@code saveCache == true}
     * のときに呼ばれる。既定はサブクラスが {@link #tryLoadFromCache(String)}／
     * {@link #storeToCache(String)} を実装する標準フローだが、独自のキャッシュ戦略を持つ
     * サブクラスは本メソッドを override してよい（その場合 {@link #doParse(String)} を直接呼ぶ）。
     *
     * <p>
     * 標準フローでは {@link #storeToCache(String)} を {@link #doParse(String)} より前に呼ぶ
     * （理由はクラスの Javadoc の実装規約を参照）。doParse が例外で中断した場合はキャッシュに空リストが残るが、
     * 次回アクセス時には空リストが返却されるため再解析よりも安全に失敗する。
     * </p>
     *
     * @param id ID
     */
    void cachedParse(String id) {
        if (!tryLoadFromCache(id)) {
            prepareResult();
            storeToCache(id);
            doParse(id);
        }
    }

    /**
     * このサブクラスが解析結果の静的キャッシュを持つかを返す。
     * <p>
     * キャッシュを持つサブクラスは {@code true} を返し、
     * {@link #tryLoadFromCache(String)}・{@link #storeToCache(String)}・
     * {@link #prepareResult()} を実装する。既定はキャッシュ無し（{@code false}）。
     * </p>
     *
     * @return キャッシュを持つ場合 {@code true}
     */
    boolean cacheEnabled() {
        return false;
    }

    /**
     * 解析結果フィールドを空の状態に初期化する。
     * <p>キャッシュを持つサブクラスが実装する。既定は何もしない。</p>
     */
    void prepareResult() {
        // キャッシュを持たないサブクラスは onTargetTypeFound 等で result を構築するため何もしない。
    }

    /**
     * 指定 ID の解析結果がキャッシュに存在すれば結果フィールドへ設定する。
     * <p>キャッシュを持つサブクラスが実装する。既定は常に未ヒット。</p>
     *
     * @param id ID
     * @return キャッシュにヒットした場合 {@code true}
     */
    boolean tryLoadFromCache(String id) {
        // 既定実装。cacheEnabled() が false の既定ではここに到達しない（クラスの Javadoc の実装規約を参照）。
        return false;
    }

    /**
     * 現在の結果フィールドを指定 ID のキーでキャッシュへ保存する。
     * <p>キャッシュを持つサブクラスが実装する。既定は何もしない。</p>
     *
     * @param id ID
     */
    void storeToCache(String id) {
        // 既定はキャッシュ無し。cacheEnabled() が false の既定ではここに到達しない（クラスの Javadoc の実装規約を参照）。
    }

    /**
     * テストデータを実際に走査して結果を組み立てる（構造解析の本体）。
     *
     * @param id ID
     */
    final void doParse(String id) {
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
