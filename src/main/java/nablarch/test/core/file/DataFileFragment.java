package nablarch.test.core.file;

import static nablarch.core.util.Builder.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FileRecordWriter;
import nablarch.core.dataformat.FixedLengthDataRecordFormatter;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.core.dataformat.convertor.ConvertorFactorySupport;
import nablarch.core.dataformat.convertor.datatype.DataType;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.util.MapCollector;

/**
 * ファイルの断片を表すクラス。<br/>
 * １つのレコード種別とそのレコード種別に適合する複数のレコードにより構成される。
 *
 * @author T.Kawasaki
 */
public abstract class DataFileFragment {

    /** この断片を包含するファイル */
    protected final DataFile container;  // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化の必要がないため

    /** データ型のマッピング表 */
    private static final String DATATYPE_MAPPING = "dataTypeMapping";

    /** フィールド名称 */
    protected List<String> names = null; // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化の必要がないため

    /** データ型のシンボル */
    protected List<String> types = null; // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化の必要がないため

    /** フィールド長 */
    protected List<String> lengths = null; // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化の必要がないため

    /** レコード追加時に文字列長を計算するか否かフラグ */
    private List<Boolean> isOndemandCalcFieldSizeList = null;
    
    /** レコード種別 */
    private String recordType = null;

    /** フィールド名称と{@link FieldDefinition}のペア */
    private Map<String, FieldDefinition> fields = new HashMap<String, FieldDefinition>();

    /** レコードのデータ */
    protected List<Map<String, String>> values = new ArrayList<Map<String, String>>(); // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化の必要がないため

    /** レコードフォーマッタ */
    private RecordDefinition recordDefinition;

    /** 最初のフィールドの値（連番）が格納されるキー */
    public static final String FIRST_FIELD_NO = "DataFileFragment:firstFieldKey";

    /** テスト用のデータ型シンボルのプレフィクス */
    private static final String TEST_SYMBOL_PREFIX = "TEST_";

    /** フィールドサイズをデータ追加時に動的計算する文字列 */
    private static final String ONDEMAND_CALC_FIELD_SIZE = "-";
    
    /** 改行および空白除去時のパターン */
    private static final Pattern REMOVE_LS_SP_PATTERN = Pattern.compile("\\s*[\\r\\n]\\s*");
    

    /**
     * コンストラクタ。
     *
     * @param container 本インスタンスが所属するファイル
     */
    protected DataFileFragment(DataFile container) {
        this.container = container;
    }

    /**
     * レコード種別を設定する。
     *
     * @param recordType レコード種別
     */
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    /**
     * 値を追加する。
     *
     * @param line 行データ
     */
    public void addValue(List<String> line) {

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String value = i < line.size() ? line.get(i) : "";
            if (isOndemandCalcFieldSize(i)) {
                value = removeLineSeparatorWithTrim(value);
                replaceFieldSize(i, value);
            }
            map.put(name, value);
        }
        values.add(map);
    }

    /**
     * フィールドサイズをデータ追加時に動的計算するかを返却する
     * @param idx インデックス
     * @return フィールドサイズをデータ追加時に動的計算する場合true
     */
    private boolean isOndemandCalcFieldSize(int idx) {
        if (isOndemandCalcFieldSizeList == null) {
            return false;
        }
        return isOndemandCalcFieldSizeList.get(idx);
    }

    /**
     * フィールドデータからフィールドサイズを計算し、置換する
     *
     * ※すでに設定されているフィールドサイズが今回のフィールドデータ長より長い場合には何もしない。
     *
     * @param idx インデックス
     * @param data フィールドデータ
     */
    private void replaceFieldSize(int idx, String data) {
        int currentLength = data.getBytes(container.getEncodingFromDirectives()).length;
        int originalLength;
        try {
            originalLength = Integer.parseInt(lengths.get(idx));
        } catch (NumberFormatException ignore) {
            originalLength = 0;
        }

        if (originalLength > currentLength) {
            return;
        }

        String size = Integer.toString(currentLength);
        lengths.set(idx, size);
    }
    
    /**
     * 各行についてトリムを行い、改行コードも除去する
     * @param data フィールドデータ
     * @return トリムおよび改行が除去されたフィールドデータ
     */
    private String removeLineSeparatorWithTrim(String data) {
        return REMOVE_LS_SP_PATTERN.matcher(data).replaceAll("");
    }
    
    /**
     * 最初のフィールドの値（連番）を追加する。
     *
     * @param line 行データ
     * @param no   最初のフィールドの値（連番）
     */
    public void addValueWithId(List<String> line, String no) {

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(FIRST_FIELD_NO, no);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String value = i < line.size() ? line.get(i) : "";
            if (isOndemandCalcFieldSize(i)) {
                value = removeLineSeparatorWithTrim(value);
                replaceFieldSize(i, value);
            }
            map.put(name, value);
        }
        values.add(map);
    }

    /**
     * フィールド名称を設定する。
     *
     * @param names フィールド名称
     */
    public void setNames(List<String> names) {
        assertNotNullOrEmpty(names, "names");
        assertNotContainDuplicateNames(names);
        this.names = names;
    }

    /**
     * データ型のシンボルを設定する。<br/>
     * データ型の要素数はフィールド名と同じでなければならない。
     *
     * @param types データ型のシンボル
     */
    public void setTypes(List<String> types) {
        assertSameSizeAsNames(types, "types");
        List<String> t = new ArrayList<String>(types.size());
        for (String dataType : types) {
            t.add(convertToFrameworkExpression(dataType));
        }
        this.types = t;
    }

    /**
     * フィールドに対するテスト用のデータ型シンボルを取得する。<br/>
     * <p/>
     * <p>テスト用のデータ型シンボルは、通常typesのデータ型シンボルであるが、
     * "TEST_<typesのデータ型シンボル>" というデータ型シンボルが存在した場合はこのデータ型シンボルを使用する。
     * </p>
     * <p/>
     * <p>
     * 例えば下記の様なデータ型がフィールド定義に存在していた場合を考える。
     * <p/>
     * <table border>
     * <tr><td>X</td><tr>
     * <tr><td>X9</td><tr>
     * <tr><td>TEST_X9</td><tr>
     * </table>
     * <p/>
     * この場合、このメソッドの返す値は下記のようになる。
     * <p/>
     * <table border>
     * <tr><th>typesに入ったデータ型</th><td>X</td><td>X9</td><tr>
     * <tr><th>戻り値のデータ型シンボル</th><td>X</td><td>TEST_X9</td><tr>
     * </table>
     * </p>
     *
     * @param fieldIndex 対象となるフィールドのインデックス
     * @return テスト用のデータ型シンボル
     */
    protected String getTypeForTest(int fieldIndex) {
        String baseType = types.get(fieldIndex);
        String testTypeName = TEST_SYMBOL_PREFIX + baseType;
        ConvertorFactorySupport support = getConvertorFactorySupport();
        boolean typeExists = support.getConvertorTable().containsKey(testTypeName);

        return typeExists ? testTypeName : baseType;
    }

    /**
     * コンバータの生成を行うクラスを取得する。
     *
     * @return コンバータの生成を行うファクトリクラス
     */
    protected abstract ConvertorFactorySupport getConvertorFactorySupport();

    /**
     * 外部インタフェース設計書のデータ型記法から
     * フレームワークのデータ型シンボルへ変換する。
     *
     * @param dataTypeInDesign 外部インタフェース設計書のデータ型
     * @return フレームワークのデータ型シンボル
     */
    private String convertToFrameworkExpression(String dataTypeInDesign) {
        DataTypeMapping mapping = null;

        if (container.getEncodingFromDirectives() != null) {
            //文字コード別マッピングの取得を試みる
            String encoding = container.getEncodingFromDirectives().name();
            mapping = SystemRepository.get(DATATYPE_MAPPING + "_" + encoding);
        }
        if (mapping == null) {
            //文字コード別マッピングが定義されていなければ、標準的な名前で定義されたマッピングの取得を試みる。
            mapping = SystemRepository.get(DATATYPE_MAPPING);
        }
        if (mapping == null) {
            //通常のマッピングも定義されていなければ、組み込みのマッピングを使用する。
            mapping = BasicDataTypeMapping.getDefault();
        }
        return mapping.convertToFrameworkExpression(dataTypeInDesign);
    }

    /**
     * フィールド長を設定する。<br/>
     * フィールド長の要素数はフィールド名と同じでなければならない。
     *
     * @param lengths フィールド長
     */
    public void setLengths(List<String> lengths) {
        assertSameSizeAsNames(lengths, "lengths");
        this.lengths = lengths;
        isOndemandCalcFieldSizeList = new ArrayList<Boolean>();
        for (String length : lengths) {
            isOndemandCalcFieldSizeList.add(ONDEMAND_CALC_FIELD_SIZE.equals(length));
        }
    }

    /**
     * この断片が持つレコード長を取得する。
     *
     * @return レコード長
     */
    int getRecordLength() {
        return calcRecordLength(lengths);
    }

    /**
     * レコード長を計算する。<br/>
     * 与えられたフィールド長を合算しレコード長とする。
     *
     * @param fieldLengths フィールド長のリスト
     * @return レコード長
     */
    int calcRecordLength(List<String> fieldLengths) {
        int sum = 0;
        for (String length : fieldLengths) {
            sum += NablarchTestUtils.parseInt(length);
        }
        return sum;
    }

    /**
     * リストがnullまたは空でないことを表明する。
     *
     * @param list 検査対象のList
     * @param name 名称
     * @throws IllegalArgumentException 引数のListがnullまたは空の時
     */
    private void assertNotNullOrEmpty(List<?> list, String name) throws IllegalArgumentException {
        if (NablarchTestUtils.isNullOrEmpty(list)) {
            throw new IllegalArgumentException(name + " must not be null or empty.");
        }
    }

    /**
     * 与えられたリストの要素数が、フィールド名の要素数と合致することを表明する。
     *
     * @param list 調査対象のリスト
     * @param name そのリストの名称（例外メッセージ出力に使用）
     * @throws IllegalArgumentException 要素数が合致しなかった場合
     */
    private void assertSameSizeAsNames(List<?> list, String name) throws IllegalArgumentException {
        assertNotNullOrEmpty(list, name);
        if (list.size() != names.size()) {
            throw new IllegalArgumentException(Builder.concat(
                    "field name size is ", names.size(),
                    ". but ", name, " size is ", list.size(), ". ", toString()));
        }
    }

    /**
     * 指定されたフィールド名に重複がないことを表明する。
     *
     * @param names フィールド名
     * @throws IllegalArgumentException フィールド名に重複が存在する場合
     */
    private void assertNotContainDuplicateNames(List<String> names) throws IllegalArgumentException {
        Set<String> duplicate = extractDuplicateElement(names);
        if (!duplicate.isEmpty()) {
            throw new IllegalArgumentException(
                    "Duplicate field names are not permitted in a record. duplicate field=" + duplicate + " . "
                            + "file=[" + container.getPath() + "]"
            );
        }
    }

    /**
     * コレクション内の重複した要素を抽出する。
     *
     * @param target 対象となるコレクション
     * @param <T>    コレクションの要素の型
     * @return 重複した要素一覧（重複がない場合、空のリスト）
     */
    private <T> Set<T> extractDuplicateElement(Collection<T> target) {
        List<T> orig = new ArrayList<T>(target);  // コピーを作成
        Set<T> unique = new HashSet<T>(target);   // 重複を取り除いたSet
        for (T e : unique) {
            orig.remove(e);  // 元のコレクションから消しこみ
        }
        Set<T> duplicate = new LinkedHashSet<T>(orig); // 残った要素が重複した要素(Assert用にLinkedHashSetを使用）
        return duplicate;
    }

    /**
     * {@link DataRecord}型に変換する。
     *
     * @return DataRecordのリスト
     */
    List<DataRecord> toDataRecords() {
        List<DataRecord> dataRecords = new ArrayList<DataRecord>(values.size());
        for (Map<String, String> value : values) {
            DataRecord record = toDataRecord(value);
            dataRecords.add(record);
        }
        return dataRecords;
    }

    /**
     * {@link DataRecord}型に変換する。
     *
     * @param value 変換対象フィールドの値
     * @return {@link DataRecord}
     */
    private DataRecord toDataRecord(Map<String, String> value) {
        DataRecord record = new DataRecord().setRecordType(recordType);
        Map<String, Object> converted = convertForDataRecord(value);
        record.putAll(converted);
        return record;
    }


    /**
     * テストデータ（文字列）から{@link DataRecord}用に値を変換する。
     *
     * @param value 元の値（文字列）
     * @return 変換後の値
     */
    protected abstract Map<String, Object> convertForDataRecord(Map<String, String> value);

    /**
     * 値の型変換を行う。<br/>
     *
     * @param fieldName        フィールド名称
     * @param stringExpression 処理対象フィールドの文字列表現
     * @return 型変換された値
     */
    protected abstract Object convertValue(String fieldName, String stringExpression);


    /**
     * フィールド名から、そのフィールドのレコードタイプを取得する。
     *
     * @param fieldName フィールド名
     * @return レコードタイプ
     */
    protected final String getTypeOf(String fieldName) {
        return getTypeForTest(getIndexOf(fieldName));
    }

    /**
     * フィールド名からそのフィールドが格納されている位置（インデックス）を取得する。
     *
     * @param fieldName フィールド名
     * @return インデックス
     */
    protected final int getIndexOf(String fieldName) {
        int index = names.indexOf(fieldName);
        if (index == -1) {
            throw new IllegalArgumentException("no such field [" + fieldName + "]. " + this);
        }
        return index;
    }


    /**
     * フィールド定義を取得する。
     *
     * @param fieldName フィールド名
     * @return フィールド定義
     */
    protected FieldDefinition getFieldDefinition(String fieldName) {
        return fields.get(fieldName);
    }

    /**
     * パディングを取り除く。
     *
     * @param fieldName フィールド名称
     * @param value     処理対象となる値
     * @param formatter バディング除去用のフォーマッタ
     * @return パディングを取り除いた値
     */
    protected Object removePadding(String fieldName, Object value, FixedLengthDataRecordFormatter formatter) {
        if (FIRST_FIELD_NO.equals(fieldName)) {  // No列は実際のデータではないのでパディング除去対象外
            return value;
        }

        FieldDefinition field = getFieldDefinition(fieldName);
        DataType<?, ?> dataType = field.getDataType();
        if (dataType == null) {
            String typeSymbol = getTypeOf(fieldName);
            int length = getLengthOf(fieldName);
            // しょうがなく
            dataType = getDataType(typeSymbol, field, length);
            formatter.setDataTypeProperty(dataType);
        }
        return dataType.removePadding(value);
    }

    /**
     * データタイプを取得する。
     *
     * @param typeSymbol データタイプ名
     * @param field      フィールド定義
     * @param length     フィールド長
     * @return データタイプ
     */
    protected DataType getDataType(String typeSymbol, FieldDefinition field, int length) {
        return getConvertorFactorySupport().typeOf(typeSymbol, field, length);
    }

    /**
     * この断片が持つレコード定義を取得する。
     *
     * @return レコード定義
     */
    RecordDefinition getRecordDefinition() {
        if (recordDefinition != null) {
            return recordDefinition;
        }
        recordDefinition = prepareRecordDefinition();
        return recordDefinition;
    }

    /**
     * {@link RecordDefinition}を生成する。<br/>
     * 設定されたデータから{@link FieldDefinition}を生成し、それらを組み合わせて
     * {@link RecordDefinition}を作成する。
     *
     * @return 生成したRecordDefinition
     */
    private RecordDefinition prepareRecordDefinition() {
        checkSize();
        RecordDefinition record = new RecordDefinition();
        for (int fieldIdx = 0; fieldIdx < names.size(); fieldIdx++) {
            FieldDefinition field = createFieldDefinition(fieldIdx);
            fields.put(field.getName(), field);
            record.addField(field);
        }
        record.setTypeName(recordType);
        return record;
    }


    /**
     * {@link FieldDefinition}を生成する。<br/>
     * 設定されたデータから{@link FieldDefinition}を生成する。
     *
     * @param fieldIndex 対象となるフィールドのインデックス
     * @return 生成したFieldDefinition
     */
    protected abstract FieldDefinition createFieldDefinition(int fieldIndex);


    /** データの要素数が妥当であることを確認する。 */
    private void checkSize() {
        if (isSizeValid()) {
            throw new IllegalStateException("invalid data. " + toString());
        }
    }

    /**
     * 各要素のサイズが妥当であるかどうか判定する。
     *
     * @return 妥当である場合、真
     */
    protected abstract boolean isSizeValid();


    /** {@inheritDoc} */
    @Override
    public String toString() {
        return concat(getClass().getSimpleName(), "{",
                      "recordType=", recordType,
                      ", values=", values,
                      ", types=", types,
                      ", lengths=", lengths,
                      '}');
    }


    /**
     * ライターに書きだす。
     *
     * @param writer ライター
     */
    void writeWith(FileRecordWriter writer) {

        for (Map<String, String> row : values) {
            // 文字列からObjectに変換
            Map<String, Object> converted = new MapCollector<Object, String, String>() {
                @Override
                protected Object evaluate(String key, String value) {
                    return convertValue(key, value);
                }
            }
                    .collect(row);
            // 書き出し
            writer.write(recordType, converted);
        }
    }

    /**
     * このインスタンスがもつ要素数（レコード数）を取得する。
     *
     * @return レコード数
     */
    int getNumberOfRecords() {
        return values.size();
    }

    /**
     * フィールド名から、そのフィールドのフィールド長を取得する。
     *
     * @param fieldName フィールド名
     * @return フィールド長
     */
    protected final int getLengthOf(String fieldName) {
        return Integer.parseInt(lengths.get(getIndexOf(fieldName)));
    }
}
