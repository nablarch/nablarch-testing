package nablarch.test.core.entity;

import nablarch.core.message.Message;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.Builder.join;
import static nablarch.core.util.StringUtil.hasValue;
import static nablarch.core.util.StringUtil.isNullOrEmpty;
import static org.junit.Assert.assertFalse;


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
    private final ValidationTestStrategy validationStrategy;

    /**
     * コンストラクタ
     *
     * @param entityClass        エンティティクラス
     * @param targetPropertyName プロパティ名
     */
    public SingleValidationTester(Class<ENTITY> entityClass, String targetPropertyName) {
        this.entityClass = entityClass;
        this.targetPropertyName = targetPropertyName;
        this.validationStrategy = new NablarchValidationTestStrategy();
    }

    /**
     * 単項目のバリデーションテストを行う。
     *
     * @param paramValue          パラメータとして使用する値
     * @param expectedMessageId   期待するメッセージID（期待しない場合はnullまたは空文字）
     * @param additionalMsgOnFail テスト失敗時の追加メッセージ文言
     */
    public void testSingleValidation(String paramValue, String expectedMessageId, String... additionalMsgOnFail) {
        testSingleValidation(new String[]{paramValue}, expectedMessageId, additionalMsgOnFail);
    }

    /**
     * 単項目のバリデーションテストを行う。
     *
     * @param paramValue          パラメータとして使用する値
     * @param expectedMessageId   期待するメッセージID（期待しない場合はnullまたは空文字）
     * @param additionalMsgOnFail テスト失敗時の追加メッセージ文言
     */
    public void testSingleValidation(String[] paramValue, String expectedMessageId, String... additionalMsgOnFail) {

        // バリデーションを実行する。
        ValidationTestContext ctx = validationStrategy.invokeValidation(entityClass, targetPropertyName, paramValue);
        // 実際のメッセージ
        List<Message> actualMessages = ctx.getMessages();
        // テスト失敗時のメッセージを作成
        String msgOnFail = createMessageOnFailure(
                paramValue,
                expectedMessageId,
                actualMessages,
                additionalMsgOnFail);

        // バリデーション結果確認
        boolean isMessageExpected = hasValue(expectedMessageId);
        if (isMessageExpected) {  // メッセージを期待するか否か
            //-- 異常系 --
            // バリデーションが失敗していること
            assertFalse(msgOnFail, ctx.isValid());
            // メッセージIDが期待通りであること
            assertEquals(msgOnFail, expectedMessageId, actualMessages.get(0).getMessageId());
        } else {
            //-- 正常系 ---
            // バリデーションが成功していること
            assertTrue(msgOnFail, ctx.isValid());
        }
    }

    /**
     * バリデーションを実行する。
     *
     * @param paramValue パラメータ値
     * @return バリデーション実行後の{@link ValidationContext}
     */
//    ValidationContext<ENTITY> invokeValidation(String paramValue) {
//        return invokeValidation(new String[]{paramValue});
//    }

    /**
     * バリデーションを実行する。
     *
     * @param paramValue 入力値
     * @return バリデーション実行後の{@link ValidationContext}
     */
//    public ValidationContext<ENTITY> invokeValidation(String[] paramValue) {
//        // 入力値（1項目分のみ）
//        Map<String, String[]> params = new HashMap<String, String[]>(1);
//        params.put(targetPropertyName, paramValue);
//        // バリデーション実行
//        ValidationContext<ENTITY> ctx
//                = getValidationManager().createValidationContext(
//                entityClass, params, "", null);
//        try {
//            ValidationUtil.validate(ctx, new String[]{targetPropertyName});
//        } catch (RuntimeException e) {
//            throw new RuntimeException(concat(
//                    "unexpected exception occurred. ", toString(),
//                    " parameter=[", paramValue, "]"), e);
//        }
//        return ctx;
//    }
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return concat("target entity=[", entityClass.getName(), "] property=[", targetPropertyName, "]");
    }

    /**
     * テスト失敗時のメッセージ文言を作成する。
     *
     * @param paramValue        実際のパラメータ
     * @param actualMessages    実際のバリデーション実行後に発生したメッセージ群
     * @param expectedMessageId 期待するメッセージID
     * @param additionalMsg     テスト失敗時の追加メッセージ文言
     * @return テスト失敗時のメッセージ文言
     */
    private String createMessageOnFailure(String[] paramValue, String expectedMessageId,
                                          List<Message> actualMessages, String... additionalMsg) {

        // 追加の文言
        String additional = concat(additionalMsg);
        // 期待値
        String expected = isNullOrEmpty(expectedMessageId)
                ? "no message"
                : concat("messageId [", expectedMessageId, "]");
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
