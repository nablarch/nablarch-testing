package nablarch.test.core.entity;

import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.validation.validator.unicode.LiteralCharsetDef;
import nablarch.test.core.db.EntityTestSupport;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author T.Kawasaki
 */
public class TestBeanTest extends EntityTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

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
            {"nablarch.core.validation.ee.Required.message", "ja", "{0}は必須でっせ"},

    };

    @Before
    public void before() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                .setMessages(MESSAGES);
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setValidationTestStrategy(new BeanValidationTestStrategy());
    }

    private final Class<TestBean> targetClass = TestBean.class;

    /**
     * 文字種と文字列長のテスト
     */
    @Test
    @Ignore
    public void testCharsetAndLength() {
        String sheetName = "testCharsetAndLength";
        String id = "charsetAndLength";
        testValidateCharsetAndLength(targetClass, sheetName, id);
    }

    @Test
    @Ignore
    public void testSingleValidation() {
        String sheetName = "testSingleValidation";
        String id = "singleValidation";
        testSingleValidation(targetClass, sheetName, id);
    }
}
