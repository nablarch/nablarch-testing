package nablarch.test.tool.findbugs;

import static nablarch.test.Assertion.fail;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import nablarch.test.tool.findbugs.PublishedApisInfoTest.AbnormalSuite;
import nablarch.test.tool.findbugs.PublishedApisInfoTest.NormalSuite;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

// 順序によってテストが失敗する場合があるので、順序を明示的に指定。
@RunWith(Suite.class)
@SuiteClasses({NormalSuite.class, AbnormalSuite.class, UsageOfUnpublishedMethodDetectorTest.class})
/**
 * {@link PublishedApisInfo}のテスト
 * 
 * 使用しているfindbugsが1.3.9のため、java8には対応しておらずエラーが出ます。
 * java6、もしくはjava7で実行してください。
 * また、テストを実行する際はgradleのtestタスクで実行してください。
 * 
 * @author 香川朋和
 */
public class PublishedApisInfoTest {

    private static final String CONFIG_FILE_PATH = "nablarch-findbugs-config";

    /**
     * 正常系のテストケース。
     * （こちらが先に実行されないとテストが失敗する。）
     */
    public static class NormalSuite {

        /**
         * コンフィグファイルに何も記述されていない場合、
         * すべてのクラスで使用不許可となる
         */
        @Test
        public void testReadConfigs1File0Record() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/onesetting0record");

            PublishedApisInfo.readConfigFiles();
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod3", "()V"));

            //カバレッジ用に、スーパークラスを持たない、非公開なクラスを読み込ませる
            Assert.assertFalse(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.setting.method.unpublishedpackage.UnpublishedPackage",
                    "unpublishedPackageTest1", "()V"));
        }

        /**
         * 指定したディレクトリにコンフィグファイルが存在しない場合。
         * この場合もすべてのクラスが使用不許可となる。
         */
        @Test
        public void testReadConfigsNoFile() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/nosettings");
            PublishedApisInfo.readConfigFiles();
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod3", "()V"));
        }

        /**
         * {@link nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass#testMethod()}
         * のみコンフィグファイルに記述されている場合。
         */
        @Test
        public void testReadConfigs1File1Record() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/onesetting1record");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod3", "()V"));
        }

        /**
         * {@link nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass#testMethod()}と
         * {@link nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass#testMethod2()}が
         * コンフィグファイルに記述されている場合。
         */
        @Test
        public void testReadConfigs1File2Record() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/onesetting2record");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod3", "()V"));
        }

        /**
         * Innerクラスの場合のケース
         * Innerクラスのコンストラクタ、メソッドを確認する。
         */
        @Test
        public void testInnerClass() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/innerClass");
            PublishedApisInfo.readConfigFiles();

            // 許可リストに定義されているInnerクラス。
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$OK", "<init>", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$OK", "<init>", "([Ljava/lang/String;)V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$OK", "isHoge", "()" +
                            "boolaen"));

            // 許可リストにてぎされていないInnerクラス。
            Assert.assertFalse(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$NG", "<init>", "()V"));
        }

        /**
         * Innerクラスの場合のケース
         * パッケージに対して使用許可がある場合、Innerクラスも使用許可となること。
         */
        @Test
        public void testInnerClass2() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/innerClass2");
            PublishedApisInfo.readConfigFiles();

            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$OK", "<init>", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$OK", "<init>", "([Ljava/lang/String;)V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass$NG", "<init>", "()V"));
        }

        /**
         * 指定したディレクトリ直下にコンフィグファイルが複数ある場合、
         * すべてのコンフィグファイルが読み込めること。
         */
        @Test
        public void testReadConfigs2Files() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/twosettings");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings" +
                                                                    ".data.java.TestClass", "testMethod", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings" +
                                                                     ".data.java.TestClass", "testMethod3", "()V"));
        }

        /**
         * パッケージを指定すると、そのパッケージに存在するクラスのメソッドはすべて
         * 使用許可となること。
         */
        @Test
        public void testReadConfigsPackage() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/packaze");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod2", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "testMethod3", "()V"));
        }

        /**
         * コンフィグファイルに記述されたInterfaceが
         * 使用許可となること。
         */
        @Test
        public void testIsPermitted1Interface() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/oneinterface");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.InterfaceFor1Interface",
                                                            "test1InterfaceImple", "()V"));
        }

        /**
         * 記述のないインターフェースに対して、使用不許可となること
         * 
         */
        @Test
        public void testIsPermittedSuperInterface() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/superinterface");
            PublishedApisInfo.readConfigFiles();
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface", "superInterfaceMethod",
                                                             "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface", "subInterfaceMethod",
                                                             "()V"));
        }

        /**
         * 別のInterfaceを継承したInterfaceに対して、
         * 継承元のメソッド、自身のメソッドともにコンフィグファイルに記述されたもののみ
         * 使用許可となること。
         */
        @Test
        public void testIsPermittedSubInterface() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/subinterface");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface",
                                                            "superPublishedInterfaceMethod", "()V"));
            Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface",
                                                            "subPublishedInterfaceMethod", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface",
                                                             "superUnpublishedInterfaceMethod", "()V"));
            Assert.assertFalse(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.SubInterface",
                                                             "subUnpublishedInterfaceMethod", "()V"));
        }

        /**
         * メソッドのシグネチャを正しく読み込めていること。
         */
        @Test
        public void testIsPermittedParameterConvert() {
            System.setProperty(CONFIG_FILE_PATH,
                    "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/parameter/convert");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo
                    .isPermitted(
                            "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.parameter.convert.ParameterConvert",
                            "testParameterConvert",
                            "(Ljava.lang.String;IJBSCFDZ[Ljava.lang.String;[I[J[B[S[C[F[D[Z[[Ljava.lang.String;[[I[[J[[B[[S[[C[[F[[D[[Z)V"));
        }

        /**
         * 使用許可のあるクラスを継承したサブクラスで、
         * コンフィグファイルに記述はなくても継承元の許可されたメソッドは
         * 使用できること。
         */
        @Test
        public void testIsPermittedSuperClass() {
            System.setProperty(CONFIG_FILE_PATH,
                    "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/superclass");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.superclass.Sub", "testSuper",
                    "()V"));
        }

        /**
         * privateなメソッドはコンフィグファイルに記述がなくても
         * trueが返ること
         */
        @Test
        public void testIsPermittedPrivateMethod() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/");
            PublishedApisInfo.readConfigFiles();
            Assert.assertTrue(PublishedApisInfo.isPermitted(
                    "nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.TestClass", "privateMethod",
                    "()V"));
        }

    }

    /**
     * 異常系テストケース。
     * こちらを後に実行しないと、{@link PublishedApisInfo}のstatic initializerでエラーになる。
     */
    public static class AbnormalSuite {

        /**
         * 読み込むJavaクラスが見つからない場合
         */
        @Test
        public void testIsPermittedNoExistingClass() {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/subinterface");

            PublishedApisInfo.readConfigFiles();
            try {
                Assert.assertTrue(PublishedApisInfo.isPermitted("nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.NoExistingClass",
                                                                "superInterfaceMethod", "()V"));
            } catch (RuntimeException e) {
                Assert.assertEquals(
                        "Couldn't find JavaClass of itself or super class. ClassName=[nablarch.test.tool.findbugs.data.publishedapi.settings.data.java.interfaze.NoExistingClass]",
                        e.getMessage());
            }
        }

        /**
         * 指定された設定ディレクトリが存在しない場合、例外が発生すること。
         * また、例外のメッセージから、設定の問題箇所を判断できること。
         */
        @Test
        public void testReadConfigFiles_NotExistingDirectory() {
            try {
                System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configured/notExistingDirectory");
                PublishedApisInfo.readConfigFiles();
                fail();
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), containsString("Config file directory doesn't exist"));
                assertThat(e.getMessage(), containsString("src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configured/notExistingDirectory"));
            }
        }

        /**
         * 設定ファイルのディレクトリが設定されていなかった場合、
         * {@link java.lang.System#getProperty(String)}が設定する例外が発生すること。
         */
        @Test
        public void testReadConfigNoSettingDirectory() {
            try {
                System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/notexsitingdir");
                PublishedApisInfo.readConfigFiles();
            } catch (RuntimeException e) {
                Assert.assertEquals("Config file directory doesn't exist.Path=[src/test/java/nablarch/test/tool/findbugs/data/publishedapi/settings/configread/notexsitingdir]", e.getMessage());
            }
        }

        /**
         * 指定されたパスがファイルだった場合、例外が発生すること。
         * また、例外のメッセージから、設定の問題箇所を判断できること。
         */
        @Test
        public void testReadConfigDirectory() {
            try {
                System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/expected/settingsTest.txt");
                PublishedApisInfo.readConfigFiles();
            } catch (RuntimeException e) {
                Assert.assertEquals("Config file directory doesn't exist.Path=[src/test/java/nablarch/test/tool/findbugs/expected/settingsTest.txt]", e.getMessage());
            }
        }
    }
}
