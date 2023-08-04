package nablarch.test.core.db;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import nablarch.core.util.Builder;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ValidationUtil;
import nablarch.test.Assertion;
import nablarch.test.core.entity.*;
import nablarch.test.event.TestEventDispatcher;

import static nablarch.core.util.Builder.concat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * エンティティ自動テスト用基底クラス。<br/>
 * エンティティクラスの自動テストを行う場合には、本クラスを継承しテストクラスを作成する。
 * 本クラス以外の基底クラスを継承しなければならない場合は、
 * 本クラスのインスタンスを生成し処理を委譲することで代替可能である。
 *
 * @author Tsuyoshi Kawasaki
 */
@Published
public class EntityTestSupport extends TestEventDispatcher {

    /** テストケース表のID(互換性の為） */
    private static final String TEST_CASE_ID = "testCases";

    /** テストケース表のID */
    private static final String TEST_SHOTS_ID = "testShots";

    /** エンティティに投入するパラメータ表のID */
    private static final String PARAMS = "params";

    /** テストケース番号カラム名 */
    private static final String TEST_TITLE = "title";

    /** 期待するメッセージIDのプレフィクス */
    private static final String MSG_ID_PREFIX = "expectedMessageId";

    /** プロパティ名のプレフィックス */
    private static final String PROP_NAME_PREFIX = "propertyName";

    /** 必須カラム一覧 */
    private static final Set<String> REQUIRED_COLUMNS_FOR_RELATIONAL_VALIDATION = new TreeSet<String>(
            Arrays.asList(
                    TEST_TITLE,
                    MSG_ID_PREFIX + '1',
                    PROP_NAME_PREFIX + '1'
            ));

    /** 期待値(getterから取得される値)のキー値 */
    private static final String GET_KEY = "get";

    /** プロパティ名のキー値 */
    private static final String NAME_KEY = "name";

    /** setter(コンストラクタ)に指定する値のキー値 */
    private static final String SET_KEY = "set";

    /**
     * データベースアクセス自動テスト用基底クラス。<br/>
     * Entityのクラス単体テストに必要な機能のみ委譲する。
     */
    private final DbAccessTestSupport dbSupport;

    /**
     * コンストラクタ。<br/>
     * 本クラスを継承する場合に呼び出されることを想定している。
     */
    protected EntityTestSupport() {
        dbSupport = new DbAccessTestSupport(getClass());
    }

    /**
     * コンストラクタ。<br/>
     * 本クラスを継承せずに使用する場合に呼び出されることを想定している。
     *
     * @param testClass テストクラス
     */
    public EntityTestSupport(Class<?> testClass) {
        dbSupport = new DbAccessTestSupport(testClass);
    }

    /**
     * バリデーションテストを実行する。
     *
     * @param entityClass バリデーション対象のエンティティのクラス
     * @param sheetName   シート名
     * @param validateFor バリデーション対象メソッド名
     * @param <T>         バリデーション結果で取得できる型（エンティティ）
     * @see ValidationUtil#validateAndConvertRequest(Class, Map, String)
     */
    public <T> void testValidateAndConvert(Class<T> entityClass, String sheetName, String validateFor) {
        testValidateAndConvert(null, entityClass, sheetName, validateFor);
    }

    /**
     * バリデーションテストを実行する。
     *
     * @param prefix      パラメータのMapに入ったキーのプレフィクス
     * @param entityClass バリデーション対象のエンティティのクラス
     * @param sheetName   シート名
     * @param validateFor バリデーション対象メソッド名
     * @param <T>         バリデーション結果で取得できる型（エンティティ）
     * @see ValidationUtil#validateAndConvertRequest(Class, Map, String)
     */
    public <T> void testValidateAndConvert(String prefix, Class<T> entityClass, String sheetName, String validateFor) {
        ValidationTestStrategy validationTestStrategy = EntityTestConfiguration.getConfig().getValidationTestStrategy();

        if (!(validationTestStrategy instanceof NablarchValidationTestStrategy)) {
            throw new IllegalArgumentException("not allowed.");
        }

        // テストケース表
        List<Map<String, String>> testCases = getTestCasesFromSheet(sheetName);

        // パラメータ表
        List<Map<String, String[]>> httpParamsList = getListParamMapRequired(sheetName, PARAMS);

        // データ行数が合致していることを確認
        checkSizeSame(testCases, httpParamsList);

        // 全テストケース実行
        for (int i = 0; i < testCases.size(); i++) {
            Map<String, String> testCase = testCases.get(i);
            Map<String, String[]> httpParams = httpParamsList.get(i);
            // バリデーション実行
            ValidationContext<T> ctx =
                    ValidationUtil.validateAndConvertRequest(prefix, entityClass, httpParams, validateFor);
            // メッセージID確認
            assertMessageEquals(testCase, ctx);
        }
    }

    /**
     * データ行数が同じであることを確認する。
     *
     * @param testCases      テストケース表
     * @param httpParamsList パラメータ表
     */
    private void checkSizeSame(List<Map<String, String>> testCases, List<Map<String, String[]>> httpParamsList) {
        int sizeOfTestCases = testCases.size();
        int sizeOfHttpParamsList = httpParamsList.size();
        if (sizeOfTestCases != sizeOfHttpParamsList) {
            throw new IllegalArgumentException(
                    "'testCases' has " + sizeOfTestCases + " line(s). "
                            + "but 'params' has " + sizeOfHttpParamsList + ".");
        }
    }

    /**
     * テストケース表を取得する。
     *
     * @param sheetName 取得元シート名
     * @return テストケース表
     */
    private List<Map<String, String>> getTestCasesFromSheet(String sheetName) {

        List<Map<String, String>> testCases = getListMap(sheetName, TEST_SHOTS_ID);
        if (testCases.isEmpty()) {
            // 下位互換性維持の為、testCasesでも検索する。
            testCases = getListMap(sheetName, TEST_CASE_ID);
        }
        checkNotEmpty(testCases, sheetName, TEST_SHOTS_ID);
        // テストケース表のチェック
        checkRequiredColumns(REQUIRED_COLUMNS_FOR_RELATIONAL_VALIDATION, testCases, sheetName, TEST_SHOTS_ID);
        return testCases;
    }


    /**
     * 必須カラム存在チェックを行う。<br/>
     * 必須カラムが存在しない場合、例外が発生する。
     *
     * @param required  必須カラム名
     * @param actual    実際のカラム
     * @param sheetName シート名（エラー発生時のメッセージ用）
     * @param id        ID（エラー発生時のメッセージ用）
     */
    private void checkRequiredColumns(final Set<String> required, List<Map<String, String>> actual,
                                      String sheetName, String id) {

        // １行以上存在するか
        if (actual == null || actual.isEmpty()) {
            throw new IllegalArgumentException(concat(
                    "TestCase has no data.", "sheet name=[", sheetName, "]", " ID=[", id, "]"
            ));
        }
        // 必須カラムが存在するか
        Set<String> columns = actual.get(0).keySet();
        if (!columns.containsAll(required)) {
            throw new IllegalArgumentException(concat(
                    "TestCase does NOT have required columns. required columns=[", required, "] ",
                    "but was [", columns, "]", "sheet name=[", sheetName, "]", "ID=[", id, "]"
            ));
        }
    }

    /**
     * メッセージが等しいことを表明する。
     *
     * @param aTestCase テストケース（テストケース表の1行）
     * @param ctx       バリデーション結果
     */
    private void assertMessageEquals(Map<String, String> aTestCase, ValidationContext<?> ctx) {

        // 比較失敗時のメッセージ
        String msg = createMessageOnFailure(aTestCase);
        // 期待値
        List<Message> expected = createExpectedMessages(aTestCase);
        // 実際の値
        List<Message> actual = ctx.getMessages();

        // 比較失敗時に原因がわかりやすいようにソートして比較。
        Assertion.assertEqualsIgnoringOrder(msg,
                                            MessageComparator.sort(expected),
                                            MessageComparator.sort(actual));
    }


    /**
     * 期待値として使用するメッセージを生成する。
     *
     * @param aTestCase テストケース（テストケース表の1行）
     * @return メッセージ
     */
    private List<Message> createExpectedMessages(Map<String, String> aTestCase) {
        List<Message> msgs = new ArrayList<Message>();
        for (int i = 1;; i++) {
            String msgId = aTestCase.get(MSG_ID_PREFIX + i);
            String prop = aTestCase.get(PROP_NAME_PREFIX + i);
            if (StringUtil.isNullOrEmpty(msgId)) {
                break;
            }
            StringResource stringResource = new StringMessageMock(msgId);
            Message msg = StringUtil.isNullOrEmpty(prop)
                    ? new Message(MessageLevel.ERROR, stringResource, new Object[0])
                    : new ValidationResultMessage(prop, stringResource, null);
            msgs.add(msg);
        }
        return msgs;
    }


    /**
     * テスト失敗時のメッセージを作成する。
     *
     * @param testCase テストケース
     * @return メッセージ
     */
    private String createMessageOnFailure(Map<String, String> testCase) {
        return concat("Case [", testCase.get(TEST_TITLE), "]");
    }

    /** テスト用StringResource実装クラス */
    static class StringMessageMock implements StringResource {

        /** ID */
        private final String id;

        /**
         * コンストラクタ
         *
         * @param id ID
         */
        StringMessageMock(String id) {
            this.id = id;
        }

        /** {@inheritDoc} */
        public String getId() {
            return id;
        }

        /**
         * {@inheritDoc}
         * 使用不可
         */
        public String getValue(Locale locale) {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * {@link DbAccessTestSupport#getParamMap(String, String)} への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return Map形式のデータ
     */
    public Map<String, String[]> getParamMap(String sheetName, String id) {
        return dbSupport.getParamMap(sheetName, id);
    }

    /**
     * {@link DbAccessTestSupport#getListParamMap(String, String)} への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     */
    public List<Map<String, String[]>> getListParamMap(String sheetName, String id) {
        return dbSupport.getListParamMap(sheetName, id);
    }

    /**
     * {@link DbAccessTestSupport#setUpDb(String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @see DbAccessTestSupport#setUpDb(String)
     */
    public void setUpDb(String sheetName) {
        dbSupport.setUpDb(sheetName);
    }


    /**
     * {@link DbAccessTestSupport#setUpDb(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param groupId   グループID
     * @see DbAccessTestSupport#setUpDb(String, String)
     */
    public void setUpDb(String sheetName, String groupId) {
        dbSupport.setUpDb(sheetName, groupId);
    }

    /**
     * {@link DbAccessTestSupport#getListMap(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @see DbAccessTestSupport#getListMap(String, String)
     */
    private List<Map<String, String>> getListMap(String sheetName, String id) {
        return dbSupport.getListMap(sheetName, id);
    }

    /**
     * setterとgetterのテストを行う。
     *
     * @param entityClass エンティティクラス名
     * @param sheetName   シート名
     * @param id          ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param <T>         エンティティクラスの型
     */
    public <T> void testSetterAndGetter(Class<T> entityClass, String sheetName, String id) {
        List<Map<String, String[]>> data = getListParamMapRequired(sheetName, id);
        T entity = createEntityInstance(entityClass, Collections.<String, Object>emptyMap());

        // EXCELに定義された全てのsetterに値を設定する。
        for (Map<String, String[]> map : data) {
            String[] name = map.get(NAME_KEY);
            String[] set = map.get(SET_KEY);

            if (set.length == 1 && StringUtil.isNullOrEmpty(set[0])) {
                continue;
            }

            // setterに対応したオブジェクトにキャスト
            Method method = ObjectUtil.getSetterMethod(entityClass, name[0]);
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 1) {
                throw new RuntimeException(String.format(
                        "multi arguments is unsupported. target setter name = [%s]",
                        method.getName()));
            }
            Object obj = cast(types[0], set, name[0]);
            ObjectUtil.setProperty(entity, name[0], obj);
        }

        // getterを呼び出し結果をアサート
        assertGetterMethod(sheetName, id, entity);
    }

    /**
     * Constructor and getterのテストを行う。
     *
     * @param entityClass テスト対象のEntityクラス
     * @param sheetName   データの記述されたシート名
     * @param id          ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     */
    public void testConstructorAndGetter(Class<?> entityClass,
                                         String sheetName, String id) {
        Map<String, Object> params = getHttpParams(entityClass, sheetName, id);
        Object instance = createEntityInstance(entityClass, params);
        assertGetterMethod(sheetName, id, instance);
    }

    /**
     * httpパラメータマップを生成する。
     *
     * @param entityClass entityクラス。
     * @param sheetName   シート名
     * @param id          ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @return httpパラメータマップ
     */
    private Map<String, Object> getHttpParams(
            Class<?> entityClass, String sheetName, String id) {
        List<Map<String, String[]>> data = getListParamMapRequired(sheetName, id);
        Map<String, Object> httpParams = new HashMap<String, Object>();
        int rowNum = 0;
        for (Map<String, String[]> stringMap : data) {
            rowNum++;
            String name = stringMap.get(NAME_KEY)[0];
            Class<?> type;
            try {
                type = ObjectUtil.getPropertyType(entityClass, name);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(Builder.concat(
                        "getter is not found.", " getter name=[", name, "].",
                        " please make sure \"", NAME_KEY, "\" column is valid.",
                        " sheet=[", sheetName, "]", " id=[", id, "]", " row=[",
                        rowNum, "]."), e);
            }

            Object set = cast(type, stringMap.get(SET_KEY), name);
            httpParams.put(name, set);
        }
        return httpParams;
    }

    /**
     * getterのテストを行う。
     *
     * @param sheetName シート名
     * @param id        ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param entity    entity
     */
    public void assertGetterMethod(String sheetName,
                                   String id, Object entity) {
        List<Map<String, String[]>> data = getListParamMapRequired(sheetName, id);

        int rowNum = 0;
        for (Map<String, String[]> map : data) {
            rowNum++;
            String[] name = map.get(NAME_KEY);
            String[] get = map.get(GET_KEY);
            if (get.length == 1 && StringUtil.isNullOrEmpty(get[0])) {
                continue;
            }

            Method getterMethod;
            
            try {
                getterMethod = ObjectUtil.getGetterMethod(entity.getClass(),
                        name[0]);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(Builder.concat(
                        "getter is not found.", " getter name=[", name[0], "]",
                        " sheet=[", sheetName, "]", " id=[", id, "]", " row=[",
                        rowNum, "]."), e);
            }

            // getter経由で値の取得
            Object actual = ObjectUtil.getProperty(entity, name[0]);

            // 期待値の型変換
            Class<?> returnType = getterMethod.getReturnType();
            Object expected = cast(returnType, get, name[0]);
            if (returnType.isArray()) {
                assertArrayEquals(name[0], (Object[]) expected,
                                  (Object[]) actual);
            } else {
                assertEquals(name[0], expected, actual);
            }
        }
    }

    /**
     * Entityインスタンスを生成する。<br/>
     *
     * @param entityClass エンティティクラス
     * @param params      コンストラクタに指定するパラメータ
     * @param <ENTITY>    エンティティクラスの型
     * @return 生成したインスタンス
     */
    private static <ENTITY> ENTITY createEntityInstance(Class<ENTITY> entityClass, Map<String, Object> params) {

        Constructor<ENTITY> constructor;
        try {
            constructor = entityClass.getConstructor(Map.class);
        } catch (NoSuchMethodException e) {
            // Mapを引数に取るコンストラクタが存在しない場合は、
            // デフォルトコンストラクタを使用してインスタンスを生成する。
            return createEntityInstance(entityClass);
        }
        try {
            return constructor.newInstance(params);
        } catch (Exception e) {
            throw toRuntimeException(entityClass, e);
        }
    }

    /**
     * デフォルトコンストラクタでEntityインスタンスを生成する。<br/>
     *
     * @param entityClass エンティティクラス
     * @param <ENTITY>    エンティティクラスの型
     * @return 生成したインスタンス
     */
    private static <ENTITY> ENTITY createEntityInstance(Class<ENTITY> entityClass) {
        try {
            return entityClass.newInstance();
        } catch (Exception e) {
            throw toRuntimeException(entityClass, e);
        }
    }

    /**
     * Entityインスタンス生成時に発生したチェック例外を非チェック例外に載せ替える。
     *
     * @param entityClass 生成に失敗したEntityクラス
     * @param e 実際に発生した例外
     * @return 引数の例外をネストさせた非チェック例外
     */
    private static RuntimeException toRuntimeException(Class<?> entityClass, Exception e) {
        return new RuntimeException(String.format("failed to instantiate the class." + " class name = [%s].",
                entityClass.getName()), e);
    }

    /**
     * 指定された文字列配列を、指定されたクラスに変換する。
     *
     * @param clazz        変換対象のクラスオブジェクト
     * @param strings      変換対象の文字列配列
     * @param propertyName 設定対象プロパティ名
     * @return 変換後のオブジェクト
     */
    private static Object cast(Class<?> clazz, String[] strings, String propertyName) {
        try {
            return cast(clazz, strings);
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "data cast error. target property name = [%s]",
                    propertyName), e);
        }
    }

    /**
     * 指定された文字列配列を、指定されたクラスに変換する。<br/>
     * <p/>
     * 型変換には、変換対象クラスのvalueOf(String)メソッドの呼び出しによって実現する。
     * このため、valueOf(String)を持たないクラスへの変換は行えない。
     * ただし、BigDecimalとStringとjava.util.DateはvalueOf(String)メソッドを定義していないが、
     * 変換対象とする。
     * java.util.Date型は、yyyy-MM-dd、yyyy-MM-dd hh:mm:ssの2種類の形式のみ変換を行う。
     * <br/>
     * 変換対象のクラスが配列の場合は、文字列配列の全ての要素を変換し配列として返却する。
     * <br/>
     * 変換対象のクラスが配列以外の場合は、文字列配列の先頭要素のみを変換し返却する。
     *
     * @param clazz   変換対象のクラスオブジェクト
     * @param strings 変換対象の文字列配列
     * @return 変換後のオブジェクト
     * @throws Exception 予期しない例外
     */
    static Object cast(Class<?> clazz, String[] strings) throws Exception {

        if (clazz.isAssignableFrom(String.class)) {
            // Stringの場合は、先頭の要素を返却
            return strings[0];
        } else if (clazz.isAssignableFrom(String[].class)) {
            // String[]の場合は、そのまま返却
            return strings;
        } else if (clazz.isAssignableFrom(BigDecimal.class)) {
            // BigDecimalの場合は、先頭要素をBigDecimalに変換
            return new BigDecimal(strings[0]);
        } else if (clazz.isAssignableFrom(BigDecimal[].class)) {
            // BigDecimal[]の場合は、全要素を変換
            BigDecimal[] decimal = new BigDecimal[strings.length];
            for (int i = 0; i < decimal.length; i++) {
                decimal[i] = new BigDecimal(strings[i]);
            }
            return decimal;
        } else if (clazz.isAssignableFrom(Date.class)) {
            // java.util.Dateの場合は、先頭要素をjava.util.Dateに変換
            return parseDateString(strings[0]);
        } else if (clazz.isAssignableFrom(Date[].class)) {
            // java.util.Date[]の場合は、全要素を変換
            Date[] dates = new Date[strings.length];
            for (int i = 0; i < dates.length; i++) {
                dates[i] = parseDateString(strings[i]);
            }
            return dates;
        }
        // 上記以外の場合は、valueOf(String)メソッドを使用して、変換
        if (clazz.isArray()) {
            // 対象が配列の場合
            Class componentType = clazz.getComponentType();
            Method method = componentType.getMethod("valueOf", String.class);
            Object[] result = (Object[]) Array.newInstance(componentType, strings.length);
            for (int i = 0; i < strings.length; i++) {
                result[i] = method.invoke(null, strings[i]);
            }
            return result;
        } else {
            Method method = clazz.getMethod("valueOf", String.class);
            return method.invoke(null, strings[0]);
        }
    }

    /**
     * 日付文字列をjava.util.Date型に変換する。
     * 変換できる形式はyyyy-MM-ddとyyyy-MM-dd HH:mm:ssの2種類である。
     * @param dateStr 日付文字列
     * @return 変換後のDate型オブジェクト
     * @throws IllegalArgumentException 日付文字列の形式が不正な場合。
     */
    private static Date parseDateString(String dateStr) throws IllegalArgumentException {
        try{
            final String s = (dateStr + " 00:00:00").substring(0, 19);
            return new Date(Timestamp.valueOf(s).getTime());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Date pattern allowed only \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\" .");
        }
    }

    /**
     * 文字種と文字列長のバリデーションテストをする。
     *
     * @param targetClass テスト対象エンティティクラス
     * @param sheetName   シート名
     * @param id          ID
     * @param <ENTITY>    テスト対象エンティティの型
     */
    public <ENTITY> void testValidateCharsetAndLength(
            Class<ENTITY> targetClass, String sheetName, String id) {

        List<Map<String, String>> testDataList = getListMapRequired(sheetName, id);

        for (Map<String, String> testData : testDataList) {
            List<Map<String, String>> packageListMap = getListMap(sheetName, testData.remove(PACKAGE_KEY));
            CharsetTestVariation<ENTITY> tester
                    = new CharsetTestVariation<ENTITY>(targetClass, testData, packageListMap);
            tester.testAll();
        }
    }


    /** プロパティ名のカラム名 */
    private static final String PROPERTY_NAME = "propertyName";

    /** メッセージIDのカラム名 */
    private static final String MESSAGE_ID = "messageId";

    /** 入力パラメータを取得するためのキー */
    private static final String INPUT = "input";

    /** 入力パラメータを取得するためのキー（ひとつめ） */
    private static final String INPUT_1 = INPUT + "1";

    /** 必須カラム */
    private static final Set<String> REQUIRED_COLUMNS_FOR_SINGLE_VALIDATION = new TreeSet<String>(
            Arrays.asList(
                    PROPERTY_NAME,
                    INPUT_1,
                    MESSAGE_ID
            ));

    /** BeanValidationのグループが属するパッケージ名のキー */
    private static final String PACKAGE_KEY = "packageKey";

    /** BeanValidationのグループのクラス名 */
    private static final String GROUP_NAME = "groupName";

    /**
     * 単項目のバリデーションテストをする。
     *
     * @param targetClass テスト対象エンティティクラス
     * @param sheetName   シート名
     * @param id          ID
     * @param <ENTITY>    テスト対象エンティティの型
     */
    public <ENTITY> void testSingleValidation(Class<ENTITY> targetClass, String sheetName, String id) {
        List<Map<String, String>> list = getListMapRequired(sheetName, id);

        // 必須カラム存在チェック
        checkRequiredColumns(REQUIRED_COLUMNS_FOR_SINGLE_VALIDATION, list, sheetName, id);

        ValidationTestStrategy validationTestStrategy = EntityTestConfiguration.getConfig().getValidationTestStrategy();

        // 全件実行
        for (Map<String, String> row : list) {
            String[] input = getInputParameter(row);
            String propertyName = row.get(PROPERTY_NAME);
            String messageId = row.get(MESSAGE_ID);
            String groupName = row.get(GROUP_NAME);
            List<Map<String, String>> packageListMap = getListMap(sheetName, row.get(PACKAGE_KEY));
            Class<?> group = validationTestStrategy.getGroupFromTestCase(groupName, packageListMap);
            new SingleValidationTester<ENTITY>(targetClass, propertyName)
                    .testSingleValidation(group, input, messageId);
        }
    }

    /**
     * 入力パラメータを取得する。
     *
     * @param row 行データ
     * @return 入力パラメータ
     */
    private String[] getInputParameter(Map<String, String> row) {
        List<String> result = new ArrayList<String>();
        String first = row.get(INPUT_1);
        if (first == null) {
            return null;
        }
        result.add(first);
        for (int i = 2;; i++) {
            String key = INPUT + i;
            if (!row.containsKey(key)) {
                break;
            }
            result.add(row.get(key));
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * 必須のList-Mapデータを取得する。<br/>
     * {@link #getListMap(String, String)}を実行し、
     * その結果が空の場合は指定したIDに合致するデータが存在しないとみなし、例外を発生させる。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @throws IllegalArgumentException 指定したIDのデータが存在しない場合
     */
    private List<Map<String, String>> getListMapRequired(String sheetName, String id)
            throws IllegalArgumentException {
        List<Map<String, String>> ret = getListMap(sheetName, id);
        checkNotEmpty(ret, sheetName, id);
        return ret;
    }

    /**
     * 必須のList-Mapデータを取得する。<br/>
     * {@link #getListParamMap(String, String)} を実行し、
     * その結果が空の場合は指定したIDに合致するデータが存在しないとみなし、例外を発生させる。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @throws IllegalArgumentException 指定したIDのデータが存在しない場合
     */
    private List<Map<String, String[]>> getListParamMapRequired(String sheetName, String id)
            throws IllegalArgumentException {
        List<Map<String, String[]>> ret = getListParamMap(sheetName, id);
        checkNotEmpty(ret, sheetName, id);
        return ret;
    }

    /**
     * テストデータとして取得したListが空でないことを検査する。
     *
     * @param list      取得したList
     * @param sheetName 取得元シート名（エラーメッセージに使用する）
     * @param id        取得元ID（エラーメッセージに使用する）
     * @throws IllegalArgumentException Listが空の場合
     */
    private void checkNotEmpty(List<?> list, String sheetName, String id) throws IllegalArgumentException {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("data [" + id + "] not found in sheet [" + sheetName + "].");
        }
    }
}

