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
            {"MSG00010", "ja", "入力必須項目です。"},
            {"MSG00011", "ja", "{0}は{1}桁以上{2}桁以下で入力してください。"},
            {"MSG00012", "ja", "{0}を入力してください。"},
            {"MSG00023", "ja", "{0}は{1}桁で入力してください。"},
            {"MSG00024", "ja", "{0}は{2}文字以下で入力して下さい。"},
            {"MSG90001", "ja", "{0}が正しくありません。"},
            {"nablarch.core.validation.ee.Length.max.message", "ja", "{0}は{2}桁以下で入力してくださいよ。"},
            {"nablarch.core.validation.ee.Length.min.max.message", "ja", "{0}は{1}桁以上{2}桁以下で入力してくださいよ。"},
            {"nablarch.core.validation.ee.Length.fixed.message", "ja", "{0}は{1}桁で入力してくださいよ。"},
            {"nablarch.core.validation.ee.Length.min.message", "ja", "{0}は{1}桁以上で入力してくださいよ。"},
            {"nablarch.core.validation.ee.Required.message", "ja", "{0}は必須ですよ"},
            {"nablarch.core.validation.ee.SystemChar.message", "ja", "文字集合は{0}を使用してくださいよ。"}
    };

    @Before
    public void before() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                .setMessages(MESSAGES);
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setMaxMessageId("nablarch.core.validation.ee.Length.max.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setMaxAndMinMessageId("nablarch.core.validation.ee.Length.min.max.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setFixLengthMessageId("nablarch.core.validation.ee.Length.fixed.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setUnderLimitMessageId("nablarch.core.validation.ee.Length.min.max.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setEmptyInputMessageId("nablarch.core.validation.ee.Required.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setMinMessageId("nablarch.core.validation.ee.Length.min.message");
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setValidationTestStrategy(new BeanValidationTestStrategy());
    }

    private final Class<TestBean> targetClass = TestBean.class;

    /**
     * 文字種と文字列長のテスト
     */
    @Test
    public void testCharsetAndLength() {
        String sheetName = "testCharsetAndLength";
        String id = "charsetAndLength";
        testValidateCharsetAndLength(targetClass, sheetName, id);
    }

    @Test
    public void testSingleValidation() {
        String sheetName = "testSingleValidation";
        String id = "singleValidation";
        testSingleValidation(targetClass, sheetName, id);
    }

    @Test
    public void testCharsetAndLengthWithGroup() {
        String sheetName = "testCharsetAndLengthWithGroup";
        String id = "charsetAndLength";
        testValidateCharsetAndLength(targetClass, sheetName, id);
    }
}
