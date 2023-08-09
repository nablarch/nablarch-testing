package nablarch.test.core.entity;

import nablarch.core.repository.SystemRepository;
import nablarch.test.core.util.generator.BasicJapaneseCharacterGenerator;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class EntityTestConfigurationTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/entity/EntityTestConfigurationTest.xml");

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void エンティティテスト設定を取得できること() {
        // setup
        EntityTestConfiguration config = EntityTestConfiguration.getConfig();

        // execute, verify
        assertEquals("HOGE", config.getOverLimitMessageId(10, null));
        assertEquals("FUGA", config.getOverLimitMessageId(10, 1));
        assertEquals("PIYO", config.getOverLimitMessageId(10, 10));
        assertEquals("PIYO", config.getUnderLimitMessageId(10, 10));
        assertEquals("HOGEHOGE", config.getUnderLimitMessageId(10, 1));
        assertEquals("FUGAFUGA", config.getEmptyInputMessageId());
        assertEquals("PIYOPIYO", config.getUnderLimitMessageId(null, 10));
        assertEquals(BasicJapaneseCharacterGenerator.class, config.getCharacterGenerator().getClass());
    }

    @Test
    public void minMessageIdが指定されていない場合_例外が送出されること() {
        // setup
        EntityTestConfiguration config = EntityTestConfiguration.getConfig();
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setMinMessageId(null);
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("If max is not specified, minMessageId must be specified.");

        config.getUnderLimitMessageId(null, 10);
    }


    @Test
    public void エンティティテスト設定を取得できない場合_例外が送出されること() {
        // setup
        SystemRepository.clear();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("can't get EntityTestConfiguration from SystemRepository. key=[entityTestConfiguration]");

        // execute
        EntityTestConfiguration.getConfig();
    }



    @Test
    public void 文字数超過メッセージ取得時に_最大最小の指定が不正の場合_例外が送出されること() {
        // setup
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid argument. max=[10]min=[20]");

        // execute
        EntityTestConfiguration.getConfig().getOverLimitMessageId(10, 20);
    }

    @Test
    public void 文字数不足メッセージ取得時に_最大最小の指定が不正の場合_例外が送出されること() {
        // setup
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("invalid argument. max=[10]min=[20]");

        // execute
        EntityTestConfiguration.getConfig().getUnderLimitMessageId(10, 20);
    }

    @Test
    public void テスト用バリデーションストラテジを取得できること() {
        // setup
        repositoryResource.getComponentByType(EntityTestConfiguration.class)
                .setValidationTestStrategy(new BeanValidationTestStrategy());

        // execute, verify
        assertEquals(BeanValidationTestStrategy.class, EntityTestConfiguration.getConfig().getValidationTestStrategy().getClass());
    }

    @Test
    public void テスト用バリデーションストラテジが明示的に設定されていない場合はデフォルトのものを取得できること() {
        // execute, verify
        assertEquals(NablarchValidationTestStrategy.class, EntityTestConfiguration.getConfig().getValidationTestStrategy().getClass());
    }


}