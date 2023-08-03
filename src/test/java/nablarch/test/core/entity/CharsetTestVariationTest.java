package nablarch.test.core.entity;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import nablarch.core.message.MockStringResourceHolder;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * {@link CharsetTestVariation}のテストクラス
 *
 * @author T.Kawasaki
 */
public class CharsetTestVariationTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/entity/SingleValidationTesterTest.xml");

    private static final String[][] MESSAGES = {
            {"MSG00010", "ja", "{0}を入力してください。"},
            {"MSG00011", "ja", "{0}は{1}桁以上{2}桁以下で入力してください。"},
            {"MSG00012", "ja", "{0}は半角英数字または記号で入力してください。"},
            {"MSG00013", "ja", "{0}は半角英字で入力してください。"},
            {"MSG00014", "ja", "{0}は半角英数字で入力してください。"},
            {"MSG00015", "ja", "{0}は半角文字で入力してください。"},
            {"MSG00016", "ja", "{0}は半角カナで入力してください。"},
            {"MSG00017", "ja", "{0}は全角文字で入力してください。"},
            {"MSG00018", "ja", "{0}は全角ひらがなで入力してください。"},
            {"MSG00019", "ja", "{0}は半角数字で入力してください。"},
            {"MSG00020", "ja", "{0}は全角カタカナで入力してください。"},
            {"MSG00021", "ja", "{0}は{1}から{2}の間の数値を入力してください。"},
            {"MSG00022", "ja", "画面遷移が不正です。"},
            {"MSG00023", "ja", "{0}は{1}桁で入力してください。"},
            {"MSG00024", "ja", "{0}は{2}文字以下で入力して下さい。"},
            {"MSG90001", "ja", "{0}が正しくありません。"},
    };

    @Before
    public void before() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                          .setMessages(MESSAGES);
    }

    /**
     * ASCII文字を許容するプロパティに対して、各種文字列でバリデーション実行した結果が
     * 想定通りである場合、例外が発生しない。
     */
    @Test
    public void testAsciiSuccess() {

        Map<String, String> paramsForAscii = newMap(new String[][] {
                {"propertyName", "ascii"},
                {"allowEmpty", "o"},
                {"min", ""},
                {"max", "50"},
                {"messageIdWhenNotApplicable", "MSG00012"},
                {"半角英字", "o"},
                {"半角数字", "o"},
                {"半角記号", "o"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "x"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsForAscii, null);
        target.testAllCharsetVariation();
        target.testOverLimit();  // 最大桁数超過
        target.testUnderLimit(); // 桁数不足
        target.testMaxLength();  // 最大桁数
        target.testMinLength();  // 最短桁数
        target.testEmptyInput(); // 未入力
    }

    /**
     * 全角カタカナ文字を許容するプロパティに対して、各種文字列でバリデーション実行した結果が
     * 想定通りである場合、例外が発生しない。
     */
    @Test
    public void testZenkakuKataKanaSuccess() {

        Map<String, String> paramsForAscii = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "o"},
                {"min", "1"},
                {"max", "10"},
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsForAscii, null);
        target.testAllCharsetVariation();

    }

    /**
     * 文字列長に関する各種テストを実行し、
     * 想定通りである場合、例外が発生しない。
     */
    @Test
    public void testLength() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "x"},
                {"min", "5"},
                {"max", "10"},
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        target.testOverLimit();  // 最大桁数超過
        target.testUnderLimit(); // 桁数不足
        target.testMaxLength();  // 最大桁数
        target.testMinLength();  // 最短桁数
        target.testEmptyInput(); // 未入力
    }

    /**
     * 桁数超過のテスト（最大桁数+1桁）を実行した結果、
     * （本来は最大桁数超過のためバリデーションが失敗するはずだが、予想に反して）
     * バリデーションが成功した場合、例外が発生することを確認する。
     */
    @Test
    public void testOverLimitFail() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "x"},
                {"min", "5"},
                {"max", "9"},  // 実際のプロパティ10桁なので期待したメッセージが発生しない
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        try {
            target.testOverLimit(); // 期待したメッセージが出ないのでアサート失敗
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("messageId [MSG00011] is expected"));
        }
    }

    /**
     * 桁数不足のテスト（最小桁数-1桁）を実行した結果、
     * （本来は桁数不足のためバリデーションが失敗するはずだが、予想に反して）
     * バリデーションが成功した場合、例外が発生することを確認する。
     */
    @Test
    public void testUnderLimitFail() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "x"},
                {"min", "6"},   // 実際のプロパティは1桁小さい(5桁）ので期待したメッセージが発生しない
                {"max", "10"},
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        try {
            target.testUnderLimit(); // 期待したメッセージが出ないのでアサート失敗
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("messageId [MSG00011] is expected"));
        }
    }

    /**
     * 最大桁数のテストを実行した結果、
     * （本来は最大桁数のためバリデーションが成功するはずだが、予想に反して）
     * バリデーションが失敗した場合、例外が発生することを確認する。
     */
    @Test
    public void testMaxLengthFail() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "x"},
                {"min", "5"},
                {"max", "11"},  // 実際のプロパティは1桁小さい（10桁）ので予期しないメッセージが返却される
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        try {
            target.testMaxLength();
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("no message is expected"));
            assertThat(e.getMessage(), containsString("messageId=[MSG00011]"));
        }
    }

    /**
     * 最小桁数のテストを実行した結果、
     * （本来は最小桁数のためバリデーションが成功するはずだが、予想に反して）
     * バリデーションが失敗した場合、例外が発生することを確認する。
     */
    @Test
    public void testMinLengthFail() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "x"},
                {"min", "4"},   // 実際より1桁小さいので予期しないメッセージが返却される
                {"max", "10"},
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        try {
            target.testMinLength();
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("no message is expected"));
            assertThat(e.getMessage(), containsString("messageId=[MSG00011]"));
        }
    }


    /** 最短桁が１桁の時、桁数不足のテストケースがスキップされること。（必須項目） */
    @Test
    public void testUnderLimitSkippedRequired() {
        Map<String, String> paramsAlphaNumeric = newMap(new String[][] {
                {"propertyName", "alphaNumeric"},
                {"allowEmpty", "x"},  // 未入力を許容しない（必須）
                {"min", "1"},         // 最短桁１文字
                {"max", "1"},
                {"messageIdWhenNotApplicable", "MSG00023"},
                {"半角英字", "o"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "x"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsAlphaNumeric, null);
        // テストがスキップされるので例外は発生しないこと
        target.testUnderLimit();
    }

    /** 最短桁が１桁の時、桁数不足のテストケースがスキップされること。（任意項目） */
    @Test
    public void testUnderLimitSkippedAllowEmpty() {
        Map<String, String> paramsAlphaNumeric = newMap(new String[][] {
                {"propertyName", "hankaku"},
                {"allowEmpty", "o"},  // 未入力を許容する
                {"min", "1"},         // 最短桁１文字
                {"max", "1"},
                {"messageIdWhenNotApplicable", "MSG00023"},
                {"半角英字", "o"},
                {"半角数字", "o"},
                {"半角記号", "o"},
                {"半角カナ", "o"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "x"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsAlphaNumeric, null);
        // テストがスキップされるので例外は発生しないこと
        target.testUnderLimit();
    }


    /**
     * 未入力のテストを実行した結果、
     * （必須項目でないためバリデーションが成功するはずだが、予想に反して）
     * バリデーションが失敗した場合、例外が発生することを確認する。
     */
    @Test
    public void testEmptyInputFail() {
        Map<String, String> paramsZenkakuKatakana = newMap(new String[][] {
                {"propertyName", "zenkakuKatakana"},
                {"allowEmpty", "o"}, // 実際は未入力不可なので予期しないメッセージが返却される
                {"min", "5"},
                {"max", "10"},
                {"messageIdWhenNotApplicable", "MSG00020"},
                {"半角英字", "x"},
                {"半角数字", "x"},
                {"半角記号", "x"},
                {"半角カナ", "x"},
                {"全角英字", "x"},
                {"全角数字", "x"},
                {"全角ひらがな", "x"},
                {"全角カタカナ", "o"},
                {"全角漢字", "x"},
                {"全角記号その他", "x"},
                {"外字", "x"}
        });
        CharsetTestVariation<TestEntity> target
                = new CharsetTestVariation<TestEntity>(TestEntity.class, paramsZenkakuKatakana, null);
        try {
            target.testEmptyInput();
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("no message is expected"));
        }
    }


    /**
     * 2次元配列からMapインスタンスを生成する。
     *
     * @param s 配列
     * @return Map
     */
    private static Map<String, String> newMap(String[][] s) {
        Map<String, String> result = new HashMap<String, String>(s.length);
        for (String[] e : s) {
            String k = e[0];
            String v = e[1];
            result.put(k, v);
        }
        return result;
    }
}
