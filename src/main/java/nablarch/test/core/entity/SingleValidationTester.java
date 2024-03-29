package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.Builder.join;
import static nablarch.core.util.StringUtil.hasValue;
import static nablarch.core.util.StringUtil.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * 単項目のバリデーションテストを行うクラス。
 *
 * @param <ENTITY> テスト対象エンティティの型
 * @author T.Kawasaki
 */
public class SingleValidationTester<ENTITY> {

    /** テスト対象エンティティクラス */
    private final Class<ENTITY> entityClass;

    /** テスト対象プロパティ名 */
    private final String targetPropertyName;

    /** バリデーションストラテジ */
    private final ValidationTestStrategy strategy;

    /**
     * コンストラクタ
     *
     * @param entityClass        エンティティクラス
     * @param targetPropertyName プロパティ名
     */
    public SingleValidationTester(Class<ENTITY> entityClass, String targetPropertyName) {
        this.entityClass = entityClass;
        this.targetPropertyName = targetPropertyName;
        this.strategy = EntityTestConfiguration.getConfig().getValidationTestStrategy();
    }

    /**
     * 単項目のバリデーションテストを行う。
     *
     * @param group                 Bean Validationのグループ
     * @param options               Bean Validationのメッセージ補完用マップ
     * @param paramValue            パラメータとして使用する値
     * @param expectedMessageString 期待するメッセージ文字列（期待しない場合はnullまたは空文字）
     * @param additionalMsgOnFail   テスト失敗時の追加メッセージ文言
     */
    public void testSingleValidation(Class<?> group, Map<String, Object> options, String paramValue, String expectedMessageString, String... additionalMsgOnFail) {
        testSingleValidation(group, options, new String[]{paramValue}, expectedMessageString, additionalMsgOnFail);
    }

    /**
     * 単項目のバリデーションテストを行う。
     *
     * @param group                 Bean Validationのグループ
     * @param options               Bean Validationのメッセージ補完用マップ
     * @param paramValue            パラメータとして使用する値
     * @param expectedMessageString 期待するメッセージ文字列（期待しない場合はnullまたは空文字）
     * @param additionalMsgOnFail   テスト失敗時の追加メッセージ文言
     */
    public void testSingleValidation(Class<?> group, Map<String, Object> options, String[] paramValue, String expectedMessageString, String... additionalMsgOnFail) {

        // バリデーションを実行する。
        ValidationTestContext ctx = strategy.invokeValidation(entityClass, targetPropertyName, paramValue, group);
        // 実際のメッセージ
        List<Message> actualMessages = ctx.getMessages();
        // テスト失敗時のメッセージを作成
        String msgOnFail = createMessageOnFailure(
                paramValue,
                expectedMessageString,
                options,
                actualMessages,
                additionalMsgOnFail);

        // バリデーション結果確認
        boolean isMessageExpected = hasValue(expectedMessageString);
        if (isMessageExpected) {  // メッセージを期待するか否か
            //-- 異常系 --
            // バリデーションが失敗していること
            assertFalse(msgOnFail, ctx.isValid());
            // メッセージが期待通りであること
            Message expectedMessage = strategy.createExpectedMessage(MessageLevel.ERROR, expectedMessageString, new Object[]{options});
            assertEquals(msgOnFail, expectedMessage, actualMessages.get(0));
        } else {
            //-- 正常系 ---
            // バリデーションが成功していること
            assertTrue(msgOnFail, ctx.isValid());
        }
    }

    /**
     * テスト失敗時のメッセージ文言を作成する。
     *
     * @param paramValue            実際のパラメータ
     * @param actualMessages        実際のバリデーション実行後に発生したメッセージ群
     * @param expectedMessageString 期待するメッセージ文字列
     * @param additionalMsg         テスト失敗時の追加メッセージ文言
     * @return テスト失敗時のメッセージ文言
     */
    private String createMessageOnFailure(String[] paramValue, String expectedMessageString, Map<String, Object> options,
                                          List<Message> actualMessages, String... additionalMsg) {

        // 追加の文言
        String additional = concat(additionalMsg);
        // 期待値
        String expected;
        if(isNullOrEmpty(expectedMessageString)) {
            expected = "no message";
        } else {
            Message expectedMessage = strategy.createExpectedMessage(MessageLevel.ERROR, expectedMessageString, new Object[]{options});
            expected = concat("message [", expectedMessage, "]");
        }

        // 入力パラメータ
        String inputParam = (paramValue == null) ? "null" : join(paramValue, ",");
        // 長さ
        String lengths = (paramValue == null) ? "N/A" : join(getLengths(paramValue), ",");

        return concat(
                additional,
                " target property=[", targetPropertyName, "] ",
                expected, " is expected. but was ", actualMessages, ".",
                " input parameter=[", inputParam, "] ",
                " length=[", lengths, "]");

    }

    /**
     * パラメータ値の文字列長を取得する。
     *
     * @param paramValue パラメータ値
     * @return 文字列長
     */
    private List<Integer> getLengths(String[] paramValue) {
        List<Integer> lengths = new ArrayList<Integer>(paramValue.length);
        for (String e : paramValue) {
            lengths.add(e.length());
        }
        return lengths;
    }
}
