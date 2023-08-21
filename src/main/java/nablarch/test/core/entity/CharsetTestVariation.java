package nablarch.test.core.entity;

import nablarch.core.util.StringUtil;

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

    /** 必須カラム */
    private static final List<String> REQUIRED_COLUMNS = Arrays.asList(
            ALLOW_EMPTY,
            PROPERTY_NAME,
            MESSAGE_ID_WHEN_NOT_APPLICABLE,
            MAX,
            MIN
    );
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

    /** テストデータ */
    private final Map<String, String> testData;

    /** 単項目バリデーションテスト */
    private final SingleValidationTester<ENTITY> tester;

    /** BeanValidationのグループ */
    private final Class<?> group;


    /**
     * コンストラクタ。
     *
     * @param entityClass テスト対象エンティティクラス
     * @param testData    テストデータ
     */
    public CharsetTestVariation(Class<ENTITY> entityClass, Class<?> group, Map<String, String> testData) {

        // 引数を破壊しないようにシャローコピー
        testData = new HashMap<String, String>(testData);

        // 必須カラム存在チェック
        checkRequiredColumns(testData);
        // 未入力を許容するか
        isEmptyAllowed = testData.remove(ALLOW_EMPTY).equals(OK);
        // テスト対象プロパティ名
        String targetPropertyName = testData.remove(PROPERTY_NAME);
        // 桁数不正時のメッセージID
        messageIdWhenInvalidLength = testData.remove(MESSAGE_ID_WHEN_INVALID_LENGTH);
        // 未入力時のメッセージID
        messageIdWhenEmptyInput = testData.remove(MESSAGE_ID_WHEN_EMPTY_INPUT);
        // 文字種バリデーション失敗時のメッセージID
        messageIdWhenNotApplicable = testData.remove(MESSAGE_ID_WHEN_NOT_APPLICABLE);
        // 最長桁数（Nablarch Validationで最長桁数が指定されていない場合は実行時エラーとする）

        String maxStr = testData.remove(MAX);
        isMaxEmpty = isNullOrEmpty(maxStr);
        if(isMaxEmpty &&
                EntityTestConfiguration.getConfig().getValidationTestStrategy() instanceof NablarchValidationTestStrategy) {
            // Nablarch Validationでは最大桁数の指定が必須。Bean Validationでは任意項目。
            throw new IllegalArgumentException("max must be specified.");
        }
        max = isMaxEmpty ? Integer.MAX_VALUE : Integer.parseInt(maxStr);
        // 最短桁数
        String minStr = testData.remove(MIN);
        isMinEmpty = isNullOrEmpty(minStr);
        min = isMinEmpty
                ? (isEmptyAllowed ? 0 : 1)  // 未入力許可時は最短0桁、必須の場合は1桁
                : Integer.parseInt(minStr);

        // Bean Validationのグループ
        this.group = group;

        // 残りのデータ
        this.testData = testData;
        // 単項目バリデーションテスト用クラス
        tester = new SingleValidationTester<ENTITY>(entityClass, targetPropertyName);
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
     * 適応可能なタイプを取得する。
     *
     * @return 適応可能なタイプ
     */
    private String getApplicableType() {
        for (Map.Entry<String, String> e : testData.entrySet()) {
            if (e.getValue().equals(OK)) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("can't find applicable charset type in [" + testData + "]");
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
        String expectedMessageId = StringUtil.isNullOrEmpty(messageIdWhenInvalidLength)
                ? EntityTestConfiguration.getConfig().getOverLimitMessageId(max, min) // デフォルトのメッセージを出力
                : messageIdWhenInvalidLength;                                         // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(max + 1, expectedMessageId, "over limit length test.");
    }

    /** 最短桁数不足のテストを行う。 */
    public void testUnderLimit() {
        // 最短桁が0桁の場合はテストしない（負の桁でテストできないので）
        // 最短桁1桁の場合はテストしない(0桁のテストは未入力のテストでやる）
        if (min <= 1) {
            return;
        }
        Integer max = isMaxEmpty ? null : this.max;
        String expectedMessageId = StringUtil.isNullOrEmpty(messageIdWhenInvalidLength)
                ? EntityTestConfiguration.getConfig().getUnderLimitMessageId(max, min) // デフォルトのメッセージを出力
                : messageIdWhenInvalidLength;                                          // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(min - 1, expectedMessageId, "under limit length test.");
    }

    /** 未入力のテストを行う。 */
    public void testEmptyInput() {
        String expectedMessageId = (isEmptyAllowed)
                ? ""                                      // 必須項目でない場合（メッセージが出ないこと）
                : StringUtil.isNullOrEmpty(messageIdWhenEmptyInput)            // 必須項目の場合、メッセージを出力する。
                ? EntityTestConfiguration.getConfig().getEmptyInputMessageId() // デフォルトのメッセージを出力
                : messageIdWhenEmptyInput;                                     // テストケースで明示的に指定したメッセージを出力
        testValidationWithValidCharset(0, expectedMessageId, "empty input test.");
    }

    /** 適応可能な文字種のテストを行う。 */
    public void testAllCharsetVariation() {
        for (Map.Entry<String, String> e : testData.entrySet()) {
            // 文字種
            String charsetType = e.getKey();
            // 期待するメッセージID
            String expectedMessageId = e.getValue().equals(OK)
                    ? ""                              // メッセージが出ないこと
                    : messageIdWhenNotApplicable;     // 適用不可時のメッセージID
            // 入力値
            String param = generate(charsetType, isMaxEmpty ? min : max);
            // 単項目バリデーションを実行
            tester.testSingleValidation(group, param, expectedMessageId, "charset type=[" + charsetType + "]");
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
     * @param length            文字列長
     * @param expectedMessageId 期待するメッセージID
     * @param msgOnFail         テスト失敗時のメッセージ
     */
    private void testValidationWithValidCharset(int length, String expectedMessageId,
            String... msgOnFail) {

        String charsetType = getApplicableType();       // 文字種
        String param = generate(charsetType, length);   // 入力値
        tester.testSingleValidation(group, param, expectedMessageId, msgOnFail);
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
