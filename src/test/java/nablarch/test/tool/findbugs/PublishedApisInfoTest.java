package nablarch.test.tool.findbugs;

import static nablarch.test.Assertion.fail;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.TextUICommandLine;

import nablarch.test.tool.findbugs.PublishedApisInfoTest.AbnormalSuite;
import nablarch.test.tool.findbugs.PublishedApisInfoTest.NormalSuite;
import nablarch.test.tool.findbugs.PublishedApisInfoTest.UsageOfUnpublishedMethodDetector;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

// 順序によってテストが失敗する場合があるので、順序を明示的に指定。
@RunWith(Suite.class)
@SuiteClasses({NormalSuite.class, AbnormalSuite.class, UsageOfUnpublishedMethodDetector.class})
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


    public static class UsageOfUnpublishedMethodDetector {

        private static final String CONFIG_FILE_PATH = "nablarch-findbugs-config";

        /**
         * 以下の動作を確認する。
         * ・コンストラクタ、メソッド単位で公開非公開を設定した場合
         *   ・公開指定されたコンストラクタ、メソッド以外を出力
         *   ・シグネチャが違えば別の要素として判断
         * ・パッケージ単位で設定された場合
         *   ・公開設定されたパッケージのクラス、サブパッケージも公開される
         * ・クラス単位で設定された場合
         *   ・公開されたクラスのメソッドはすべて公開される
         *   ・内部クラス、抽象クラス、インターフェースに対しても普通のクラスと同じ動作をする
         *   ・無名クラス内部は検知されない。
         *
         * @throws Exception
         */
        @Test
        public void testSettings() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/settings/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/settingTest.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/settings/Caller.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/settingsTest.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 各種構文中で使用されるAPIに対する動作を確認する。
         * 確認している構文は以下の通り
         * ・メソッドの使用
         * ・メソッドチェイン
         * ・if文
         * ・for文
         * ・while文
         * ・do-while文
         * ・switch文
         * ・三項演算子
         * ・return文
         * ・catch文、finally文
         * ・継承
         * ・インターフェース
         * @throws Exception
         */
        @Test
        public void testMethodCall() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallTest.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/Caller.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallTest.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 様々な位置でのメソッド、コンストラクタ呼び出し時の動作を確認する。
         * 以下の内部での動作を確認する。
         * ・静的初期化子
         * ・インスタンス初期化子
         * ・コンストラクタ
         * ・無名クラス
         * ・ローカルクラス
         * ・継承有ローカルクラス
         * ・内部クラス
         * @throws Exception
         */
        @Test
        public void testMethodCallInNonMethod() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallInNonMethodTest.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/ClassForVariousLocation.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallInNonMethodTest.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 無名クラスを読み込ませた際の動作を確認する。
         */
        @Test
        public void testMethodCallInAnonymousClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallInAnnonymousClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/ClassForVariousLocation$1.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallInAnnonymousClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * ローカルクラスを読み込ませた際の動作を確認する。
         * @throws Exception
         */
        @Test
        public void testMethodCallInLocalClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallInLocalClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/ClassForVariousLocation$1Local.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallInLocalClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 内部クラスを読み込ませた際の動作を確認する。
         * @throws Exception
         */
        @Test
        public void testMethodCallInInnerClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallInInnerClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/ClassForVariousLocation$Inner.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallInInnerClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * サブクラスを読み込ませた際の動作を確認する。
         *
         * @throws Exception
         */
        @Test
        public void testMethodCallInSubClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/methodcall/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/methodCallInSubClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/methodcall/inherit/method/ClassC.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/methodCallInSubClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 例外クラスに対する動作の確認を行う。
         * それぞれの位置に書かれた例外クラスに対して、正しく検査できることを確認する。
         * ・静的初期化子中
         * ・インスタンス初期化子中
         * ・トップレベルクラスthrows指摘
         * ・トップレベルクラス中catch指摘
         * ・catch句内のネストしたtry-catch
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsTopLevelClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionTopLevelClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/exception/Caller.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionTopLevelClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 内部クラスで使用されている例外クラスに対する動作の確認を行う。
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsInnerClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionInnerClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/exception/Caller$InnerClass.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionInnerClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * ローカルクラスで使用されている例外クラスに対する動作の確認を行う。
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsLocalClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionTest.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/exception/Caller$1LocalClass.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionLocalClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 無名クラスで使用されている例外クラスに対する動作の確認を行う。
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsAnonymousClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionAnnonymousClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/exception/Caller$1.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionAnnonymousClass.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * Java1.4以前の、オペコードにjsrが使用されている
         * classファイルに対する動作の確認を行う。
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsJsrMode() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionJsrMode.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/jsrbin/nablarch/test/tool/findbugs/data/exception/Caller.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionJsrMode.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * Java1.6でコンパイルされた
         * classファイルに対する動作の確認を行う。
         *
         * @throws Exception
         */
        @Test
        public void testExceptionsJava6() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionJaba6.txt";
            doFindBugs(outputFile,
                    "src/test/java/nablarch/test/tool/findbugs/data/compilejava1.6/nablarch/test/tool/findbugs/data/exception/Caller.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionJava6.txt", outputFile);
            deleteFile(outputFile);
        }

        /**
         * 例外クラス(内部クラス)がthrows句、catch句に定義されているときの動作を確認する.
         * <ul>
         * <li>
         * 使用が許可された例外クラス(内部クラス)がthrows句、catch句に定義されているとき、
         * Findbugsが使用が禁止されたクラスとして検知しないことを確認する.
         * </li>
         * <li>
         * 使用が許可されない例外クラス（内部クラス）がthrows句、catch句に定義されているとき、
         * Findbugsが使用が禁止されたクラスとして検知することを確認する.
         * </li>
         * </ul>
         */
        @Test
        public void testInnerExceptionClass() throws Exception {
            System.setProperty(CONFIG_FILE_PATH, "src/test/java/nablarch/test/tool/findbugs/data/exception/settings2");
            PublishedApisInfo.readConfigFiles();

            String outputFile = "src/test/java/nablarch/test/tool/findbugs/exceptionInnerExceptionClass.txt";
            doFindBugs(outputFile, "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/nablarch/test/tool/findbugs/data/exception/CallerForExceptionInnerClass.class");
            assertFiles("src/test/java/nablarch/test/tool/findbugs/expected/exceptionInnerExceptionClass.txt", outputFile);
            deleteFile(outputFile);
        }

        private void deleteFile(String outputFile) {
            File file = new File(outputFile);
            if (!file.delete()) {
                fail();
            }
        }

        /**
         * FindBugsの実行を行う。
         *
         * @param args コマンドライン引数
         * @throws IOException 処理実行中の例外
         * @throws InterruptedException 処理実行中の例外
         */
        private void doFindBugs(String outputFile, String classForCheck) throws IOException, InterruptedException {

            String[] args = new String[7];
            args[0] = "-include";
            args[1] = "src/test/java/nablarch-findbugs-include.xml";
            args[2] = "-output";
            args[3] = outputFile;
            args[4] = "-auxclasspath";
            args[5] = "src/test/java/nablarch/test/tool/findbugs/data/jsrbin/"
                    + File.pathSeparator
                    + "src/test/java/nablarch/test/tool/findbugs/data/notjsrmode/" + File.pathSeparator;
            args[6] = classForCheck;
            FindBugs2 findBugs = new FindBugs2();
            TextUICommandLine commandLine = new TextUICommandLine();
            FindBugs.processCommandLine(commandLine, args, findBugs);
            findBugs.execute();
        }

        /**
         * ファイルの内容を比較する。
         *
         * @param expectedFilePath 期待する内容が記述されたファイルパス
         * @param actualFilePath 実際のファイルパス
         * @throws IOException ファイル入出力の際に発生した例外
         */
        private void assertFiles(String expectedFilePath, String actualFilePath) throws IOException {
            String expectedString = getStringFromFile(expectedFilePath);
            String actualString = getStringFromFile(actualFilePath);
            Assert.assertEquals(expectedString, actualString);
        }

        /**
         * filePathにて指定されるファイルの内容を文字列として返却する。
         *
         * @param filePath 取得するファイルのパス
         * @return filePathにて指定されるファイルの内容を文字列としたもの
         * @throws IOException ファイル入出力の際のエラー
         */
        private String getStringFromFile(String filePath) throws IOException {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(new File(filePath)));

                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\r\n");
                }

                return sb.toString();

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }
}
