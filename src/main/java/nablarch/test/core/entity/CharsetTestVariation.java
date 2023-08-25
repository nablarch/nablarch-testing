package nablarch.test.core.entity;

import nablarch.core.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

/**
 * 文字種のバリデーションテストの種類。
 *
 * @param <ENTITY> テスト対象エンティティの型
 * @author T.Kawasaki
 */
public class CharsetTestVariation<ENTITY> {

    /** 未入力許容可否のカラム名 */
    private static final String ALLOW_EMPTY = "allowEmpty";
    /** プロパティ名のカラム名 */
    private static final String PROPERTY_NAME = "propertyName";
    /** 桁数不正時のメッセージIDのカラム名 */
    private static final String MESSAGE_ID_WHEN_INVALID_LENGTH = "messageIdWhenInvalidLength";
    /** 未入力時のメッセージIDのカラム名 */
    private static final String MESSAGE_ID_WHEN_EMPTY_INPUT = "messageIdWhenEmptyInput";
    /** 文字が適応不可の場合のメッセージIDのカラム名 */
    private static final String MESSAGE_ID_WHEN_NOT_APPLICABLE = "messageIdWhenNotApplicable";
    /** 最長桁数のカラム名 */
    private static final String MAX = "max";
    /** 最短桁数のカラム名 */
    private static final String MIN = "min";
    /** Bean Validationのメッセージ補完用属性キーのカラム名 */
    private static final String INTERPOLATE_KEY_PREFIX = "interpolateKey_";
    /** Bean Validationのメッセージ補完用属性値のカラム名 */
    private static final String INTERPOLATE_VALUE_PREFIX = "interpolateValue_";

    /** 必須カラム */
    private static final List<String> REQUIRED_COLUMNS = Arrays.asList(
            ALLOW_EMPTY,
            PROPERTY_NAME,
            MESSAGE_ID_WHEN_NOT_APPLICABLE,
            MAX,
            MIN
    );

    private static final List<String> COLUMNS_EXCEPT_CHARSET = new ArrayList<String>(){{
        addAll(REQUIRED_COLUMNS);
        add(MESSAGE_ID_WHEN_INVALID_LENGTH);
        add(MESSAGE_ID_WHEN_EMPTY_INPUT);
        add(INTERPOLATE_KEY_PREFIX);
        add(INTERPOLATE_VALUE_PREFIX);
    }};


    /** OKマーク */
    private static final String OK = "o";

    /** 未入力を許容するか */
    private final boolean isEmptyAllowed;

    /** 桁数不正時のメッセージID */
    private final String messageIdWhenInvalidLength;

    /** 未入力時のメッセージID */
    private final String messageIdWhenEmptyInput;

    /** 適用不可時に期待するメッセージID */
    private final String messageIdWhenNotApplicable;

    /** 最大桁数 */
    private final int max;

    /** 最小桁数 */
    private final int min;

    /** 最大桁数欄は空欄か */
    private final boolean isMaxEmpty;

    /** 最小桁数欄は空欄か */
    private final boolean isMinEmpty;

    /** Bean Validationのメッセージ補完用属性のマップ */
    private final Map<String, Object> options;

    /** 文字種テスト情報のマップ */
    private final Map<String, String> charsetTestMap;

    /** 単項目バリデーションテスト */
    private final SingleValidationTester<ENTITY> tester;

    /** BeanValidationのグループ */
    private final Class<?> group;


    /**
     * コンストラクタ。
     *
     * @param entityClass         テスト対象エンティティクラス
     * @param beanValidationGroup Bean Validationのグループ
     * @param rowData             1行分のテストデータ
     */
    public CharsetTestVariation(Class<ENTITY> entityClass, Class<?> beanValidationGroup, Map<String, String> rowData) {

        // 必須カラム存在チェック
        checkRequiredColumns(rowData);

        // 最初に文字種の情報を抽出する
        charsetTestMap = createCharsetMap(rowData);

        // 未入力を許容するか
        isEmptyAllowed = rowData.get(ALLOW_EMPTY).equals(OK);
        // 桁数不正時のメッセージID
        messageIdWhenInvalidLength = rowData.get(MESSAGE_ID_WHEN_INVALID_LENGTH);
        // 未入力時のメッセージID
        messageIdWhenEmptyInput = rowData.get(MESSAGE_ID_WHEN_EMPTY_INPUT);
        // 文字種バリデーション失敗時のメッセージID
        messageIdWhenNotApplicable = rowData.get(MESSAGE_ID_WHEN_NOT_APPLICABLE);
        // 最長桁数（Nablarch Validationで最長桁数が指定されていない場合は実行時エラーとする）
        String maxStr = rowData.get(MAX);
        isMaxEmpty = isNullOrEmpty(maxStr);
        if(isMaxEmpty &&
                EntityTestConfiguration.getConfig().getValidationTestStrategy() instanceof NablarchValidationTestStrategy) {
            // Nablarch Validationでは最大桁数の指定が必須。Bean Validationでは任意項目。
            throw new IllegalArgumentException("max must be specified.");
        }
        max = isMaxEmpty ? Integer.MAX_VALUE : Integer.parseInt(maxStr);
        // 最短桁数
        String minStr = rowData.get(MIN);
        isMinEmpty = isNullOrEmpty(minStr);
        min = isMinEmpty
                ? (isEmptyAllowed ? 0 : 1)  // 未入力許可時は最短0桁、必須の場合は1桁
                : Integer.parseInt(minStr);

        // Bean Validationのグループ
        group = beanValidationGroup;
        // Bean Validationのメッセージ補完用属性のマップ
        options = createInterpolationMap(rowData);

        // 単項目バリデーションテスト用クラス
        tester = new SingleValidationTester<ENTITY>(entityClass, rowData.get(PROPERTY_NAME));
    }

    /**
     * 必須カラムの存在チェックを行う。
     *
     * @param testData テストデータ
     */
    private void checkRequiredColumns(Map<String, String> testData) {
        if (testData == null) {
            throw new IllegalArgumentException("testData must not be null.");
        }
        for (String column : REQUIRED_COLUMNS) {
            if (!testData.containsKey(column)) {
                throw new IllegalArgumentException(
                        "column [" + column + " is required. but was "
                                + testData.keySet());
            }
        }
    }


    /**
     * テストデータから文字種情報を抽出する。
     *
     * @param rowData 1行分のテストデータ
     * @return 文字種情報のマップ
     */
    private Map<String, String> createCharsetMap(Map<String, String> rowData) {
        Map<String, String> charsetMap = new HashMap<String, String>();
        boolean isElem = false;
        for(Map.Entry<String, String> entry : rowData.entrySet()) {
            for(String definedKey : COLUMNS_EXCEPT_CHARSET) {
                String inputKey = entry.getKey();
                if(inputKey.startsWith(definedKey)) {
                    isElem = true;
                    break;
                }
            }
            if(!isElem) {
                charsetMap.put(entry.getKey(), entry.getValue());
            }
            isElem = false;
        }
        return charsetMap;
    }

    /**
     * Bean Validationのメッセージ補完用属性情報を抽出する。
     *
     * @param rowData 1行分のテストデータ
     * @return メッセージ補完用属性情報のマップ
     */
    private Map<String, Object> createInterpolationMap(Map<String, String> rowData) {
        Map<String, Object> result = new HashMap<String, Object>();
        for(int i = 1;; i++) {
            String keyOfInterpolateKey = INTERPOLATE_KEY_PREFIX + i;
            String keyOfInterpolateValue = INTERPOLATE_VALUE_PREFIX + i;

            // i番目の補完用属性のキーがいずれか見つからない場合、その時点で抽出を終了する。
            if(!(rowData.containsKey(keyOfInterpolateKey) && rowData.containsKey(keyOfInterpolateValue))) {
                break;
            }

            String interpolateKey = rowData.get(keyOfInterpolateKey);
            String interpolateValue = rowData.get(keyOfInterpolateValue);
            if(StringUtil.isNullOrEmpty(interpolateKey)) {
                continue;
            }

            result.put(interpolateKey, interpolateValue);
        }
        return result;
    }

    /**
     * 適応可能なタイプを取得する。
     *
     * @return 適応可能なタイプ
     */
    private String getApplicableType() {
        for (Map.Entry<String, String> e : charsetTestMap.entrySet()) {
            if (e.getValue().equals(OK)) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("can't find applicable charset type in [" + charsetTestMap + "]");
    }

    /** 全種類のテストを行う。 */
    public void testAll() {
        testMaxLength();    // 最長桁
        testOverLimit();    // 最長桁 + 1
        testMinLength();    // 最短桁
        testUnderLimit();   // 最短桁 - 1
        testEmptyInput();   // 未入力
        testAllCharsetVariation();  // 文字種
    }

    /** 最長桁数のテストを行う。 */
    public void testMaxLength() {
        // maxが空の場合は、最大値に制限が無いことを意味するため、テストしない。
        if(isMaxEmpty) {
            return;
        }
        testValidationWithValidCharset(max, null, "max length test.");
    }

    /** 最短桁数のテストを行う。 */
    public void testMinLength() {
        testValidationWithValidCharset(min, null, "minimum length test.");
    }

    /** 最長桁数超過のテストを行う。 */
    public void testOverLimit() {
        // 最長桁がInteger.MAX_VALUEの場合はテストしない (オーバーフローしてしまうため)
        if (max == Integer.MAX_VALUE) {
            return;
        }
        Integer min = isMinEmpty ? null : this.min;
        String expectedMessageString = StringUtil.isNullOrEmpty(messageIdWhenInvalidLength)
                ? EntityTestConfiguration.getConfig().getOverLimitMessageId(max, min) // デフォルトのメッセージを出力
                : messageIdWhenInvalidLength;                                         // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(max + 1, expectedMessageString, "over limit length test.");
    }

    /** 最短桁数不足のテストを行う。 */
    public void testUnderLimit() {
        // 最短桁が0桁の場合はテストしない（負の桁でテストできないので）
        // 最短桁1桁の場合はテストしない(0桁のテストは未入力のテストでやる）
        if (min <= 1) {
            return;
        }
        Integer max = isMaxEmpty ? null : this.max;
        String expectedMessageString = StringUtil.isNullOrEmpty(messageIdWhenInvalidLength)
                ? EntityTestConfiguration.getConfig().getUnderLimitMessageId(max, min) // デフォルトのメッセージを出力
                : messageIdWhenInvalidLength;                                          // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(min - 1, expectedMessageString, "under limit length test.");
    }

    /** 未入力のテストを行う。 */
    public void testEmptyInput() {
        String expectedMessageString = (isEmptyAllowed)
                ? ""                                      // 必須項目でない場合（メッセージが出ないこと）
                : StringUtil.isNullOrEmpty(messageIdWhenEmptyInput)            // 必須項目の場合、メッセージを出力する。
                ? EntityTestConfiguration.getConfig().getEmptyInputMessageId() // デフォルトのメッセージを出力
                : messageIdWhenEmptyInput;                                     // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(0, expectedMessageString, "empty input test.");
    }

    /** 適応可能な文字種のテストを行う。 */
    public void testAllCharsetVariation() {
        for (Map.Entry<String, String> e : charsetTestMap.entrySet()) {
            // 文字種
            String charsetType = e.getKey();
            // 期待するメッセージID
            String expectedMessageString = e.getValue().equals(OK)
                    ? ""                              // メッセージが出ないこと
                    : messageIdWhenNotApplicable;     // 適用不可時のメッセージID
            // 入力値
            String param = generate(charsetType, isMaxEmpty ? min : max);
            // 単項目バリデーションを実行
            tester.testSingleValidation(group, options, param, expectedMessageString, "charset type=[" + charsetType + "]");
        }
    }

    /**
     * 文字列を生成する。
     *
     * @param charsetType 生成する文字種
     * @param length      文字列長
     * @return 生成した文字列
     */
    private String generate(String charsetType, int length) {
        return EntityTestConfiguration.getConfig().getCharacterGenerator().generate(charsetType, length);
    }

    /**
     * 適応可能な文字でバリデーションを行う。
     *
     * @param length                文字列長
     * @param expectedMessageString 期待するメッセージ文字列
     * @param msgOnFail             テスト失敗時のメッセージ
     */
    private void testValidationWithValidCharset(int length, String expectedMessageString,
            String... msgOnFail) {

        String charsetType = getApplicableType();       // 文字種
        String param = generate(charsetType, length);   // 入力値
        tester.testSingleValidation(group, options, param, expectedMessageString, msgOnFail);
    }

    /**
     * 固定長のプロパティであるかどうか判定する。<br/>
     * 最短桁カラムが空欄の場合は、固定長とみなさない（偽を返却する。）
     * 最長桁と最短桁が同値の場合、固定長とみなす（真を返却する）。
     *
     * @return 判定結果
     */
    private boolean isFixedLength() {
        return !isMinEmpty && min == max;
    }
}
