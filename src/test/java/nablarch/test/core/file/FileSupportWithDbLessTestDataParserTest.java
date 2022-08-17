package nablarch.test.core.file;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.convertor.VariableLengthConvertorSetting;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.test.TestSupport;
import nablarch.test.Trap;
import nablarch.test.core.file.SimpleWriter.LS;
import nablarch.test.core.util.FileUtils;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.Builder.join;
import static nablarch.core.util.Builder.lines;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link FileSupport}で{@link nablarch.test.core.reader.DbLessTestDataParser}を使用する場合のテストクラス。
 * 
 * @author Shinya Hijiri
 */
public class FileSupportWithDbLessTestDataParserTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-dbless.xml");

    /** テスト対象クラス */
    private final FileSupport target = new FileSupport(getClass());

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final File work = new File("work");
    @BeforeClass
    public static void setUp() {
        FileUtils.mkdir(work);
    }

    /** テスト名 (JUnit4の制約によりpublic) */
    @Rule
    public final TestName testName = new TestName();

    private String methodName;

    /** テストメソッド名を設定する */
    @Before
    public void name() {
        methodName = testName.getMethodName();
    }

    /** 対象ディレクトリ配下のファイルを削除する。 */
    @Before
    public void clearDir() {
        for (File file : work.listFiles()) {
            FileUtil.deleteFile(file);
        }
    }

    /** 固定長のデータ */
    private static final String fixedData = Builder.concat(
            "1                                                                                                       " +
                    "" +
                    "                          ", "\r\n",
            "2ﾍﾝｺｳｺﾞ ｶﾅｼﾒｲ        変更後　漢字氏名　　　　　　　　　　　　Henkougo romaji        1henkougo@emal.cellar          01234    " +
                    "" +
                    "      ", "\r\n",
            "8  0000001                                                                                              " +
                    "" +
                    "                          ", "\r\n"
    );

    /** 可変長のデータ */
    private static final String variableData = Builder.concat(
            "\"1\",\"\"", "\r\n",
            "\"2\",\"ﾍﾝｺｳｺﾞ ｶﾅｼﾒｲ\",\"変更後　漢字氏名\",\"Henkougo romaji\",\"1\",\"\"", "\r\n",
            "\"8\",\"\",\"1\",\"\"", "\r\n"
    );

    /**
     * 固定長ファイルの準備ができること。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testSetUpFixedLengthFile() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);

        target.setUpFile(methodName);

        assertThat(result.exists(), is(true));
        String actual = readAll(new FileInputStream(result));
        Assert.assertEquals(fixedData, actual);
    }

    /**
     * 空行を含むセットアップデータから、固定長ファイルの準備ができること。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testSetUpFixedEmptyLine() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);
        target.setUpFile(methodName);

        assertThat(result.exists(), is(true));
        String expected = concat(
                "ｶﾅｶﾅ かなかな　\n",
                "     　　　　　\n",
                "ｶﾅﾅﾝ かななん　\n");
        String actual = readAll(new FileInputStream(result));
        Assert.assertEquals(expected, actual);
    }

    /**
     * 可変長ファイルの準備ができること。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testSetUpVariableLengthFile() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);

        target.setUpFile(methodName);

        assertThat(result.exists(), is(true));
        String actual = readAll(new FileInputStream(result));
        Assert.assertEquals(variableData, actual);
    }

    /**
     * 空行を含むセットアップデータから、可変長ファイルの準備ができること。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testSetUpVariableEmptyLine() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);

        target.setUpFile(methodName);

        assertThat(result.exists(), is(true));
        String expected = join(Arrays.asList(
                "'abc'\t'def'\t'ghi'",
                "''\t''\t''",
                "'jkl'\t'mno'\t'pqr'",
                "")
                ,"\r");

        String actual = readAll(new FileInputStream(result));
        Assert.assertEquals(expected, actual);
    }

    /**
     * 空行を含むセットアップデータから、可変長ファイルの準備ができること。
     * 可変長ファイルのフィールドは１つのみ（区切り文字が使用されないパターン）。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testSetUpVariableEmptyLine2() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);

        target.setUpFile(methodName);

        assertThat(result.exists(), is(true));
        String expected = join(Arrays.asList(
                "'abc'",
                "''",   // 空行
                "'def'",
                "''",   // 空行
                ""), "\r\n");
        String actual = readAll(new FileInputStream(result));
        Assert.assertEquals(expected, actual);
    }


    private void remove(File file) {
        if (file.exists()) {
            assertTrue(file.delete());
        }
        assertThat(file.exists(), is(false));
    }


    /**
     * データ型のバリエーションをテストする。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testVariation() throws FileNotFoundException {
        File result = new File("work/" + methodName + ".txt");
        remove(result);

        // ファイル作成
        target.setUpFile(methodName);
        // 想定通りのファイルが作成されていること
        assertThat(result.exists(), is(true));
        byte[] expected = readBytes(getClass().getResourceAsStream("testVariation_expected.txt"));
        byte[] actual = readBytes(new FileInputStream(result));

        Assert.assertArrayEquals(expected, actual);

        // アサートが成功すること。
        target.assertFile("", methodName);
    }

    /**
     * データ型のバリエーションをテストする。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testVariationUTF8() throws FileNotFoundException {

        // テスト用のリポジトリ構築
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/customBasicDataTypeMapping.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        File result = new File("work/" + methodName + ".txt");
        remove(result);

        // ファイル作成
        target.setUpFile(methodName);
        // 想定通りのファイルが作成されていること
        assertThat(result.exists(), is(true));
        byte[] expected = readBytes(getClass().getResourceAsStream("testVariationUTF8_expected.txt"));
        byte[] actual = readBytes(new FileInputStream(result));

        Assert.assertArrayEquals(expected, actual);
        
        // アサートが成功すること。
        target.assertFile("", methodName);
        

        // デフォルトのリポジトリに戻す
        loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/defaultBasicDataTypeMapping.xml");
        container = new DiContainer(loader);
        SystemRepository.load(container);
        VariableLengthConvertorSetting.getInstance();
    }

    /**
     * NumberStringDecimalのテスト。正常系。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testNumberStringDecimal1() throws FileNotFoundException {

        File result = new File("work/" + methodName + ".txt");
        remove(result);

        // ファイル作成
        target.setUpFile(methodName);
        // 想定通りのファイルが作成されていること
        assertThat(result.exists(), is(true));
        byte[] expected = readBytes(getClass().getResourceAsStream("testNumberStringDecimal1_expected.txt"));
        byte[] actual = readBytes(new FileInputStream(result));

        Assert.assertArrayEquals(expected, actual);

        // アサートが成功すること。
        target.assertFile("", methodName);
    }

    @Test
    public void testAssertEmptyVariableFile() throws IOException {
        // 空ファイル
        SimpleWriter.touch("work/" + methodName + "1.txt");

        File empty = SimpleWriter.touch("work/" + methodName + "2.txt");
        new SimpleWriter() {
            @Override
            protected void write() {
                println();  // 改行のみ
            }
        }.to(empty);

        target.assertFile("", methodName, "1");

    }

    /**
     * NumberStringDecimalのテスト、異常系。
     *
     * @throws FileNotFoundException 予期しない例外
     */
    @Test
    public void testNumberStringDecimal2() throws FileNotFoundException {

        File result = new File("work/" + methodName + ".txt");
        remove(result);

        // ファイル作成
        try {
            target.setUpFile(methodName);
            fail();
        } catch (InvalidDataFormatException e) {
            assertThat(e.getMessage(),
                       is("invalid parameter was specified. data byte length did not match specified field size. data size=[10], field size=[9], data=[1234567890]. field name=[符号付数値1]. record number=[1]."));
        }
    }


    /** 固定長ファイルのアサートが成功すること */
    @Test
    public void testAssertFixedLengthFile() {
        // 期待値と一致するファイルを作成
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                print(fixedData);
            }
        }.to("work/" + methodName + ".txt")
         .deleteOnExit();

        target.assertFile("message on failure...", methodName);
    }

    /** 可変長ファイルのアサートが成功すること */
    @Test
    public void testAssertVariableLengthFile() {
        // 期待値と一致するファイルを作成
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                print(variableData);
            }
        }.to("work/" + methodName + ".txt");


        target.assertFile("message on failure...", methodName);
    }

    /** 空行を含む期待データ（可変長）で、正しく比較ができること。*/
    @Test
    public void testAssertEmptyLineVariable() {
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                println("abc,def,ghi");
                println(",,");    // 空行
                println("jkl,mno,pqr");
            }
        }.to("work/" + methodName + ".txt");
        target.assertFile("message on failure...", methodName);
    }

    /**
     * 空行を含む期待データ（可変長）で、正しく比較ができること。
     * 可変長ファイルのフィールドは１つのみ（区切り文字が使用されないパターン）。
     */
    @Test
    public void testAssertEmptyLineVariable2() {
        new SimpleWriter("EUC-JP") {
            @Override
            protected void write() {
                println("abc");  // フィールドが１つなので区切り文字','が出てこない
                println("");     // 空行
                println("jkl");
            }
        }.with(LS.LF)
         .to("work/" + methodName + ".txt");
        target.assertFile("message on failure...", methodName);
    }

    /**
     * 空行を含む期待データ（固定長）で、正しく比較ができること。
     */
    @Test
    public void testAssertEmptyLineFixed() {
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                println("ｶﾅｶﾅ かなかな　");
                println("     　　　　　");  // 空行
                println("ｶﾅﾅﾝ かななん　");
            }
        }.with(LS.LF)
         .to("work/" + methodName + ".txt");
        target.assertFile("message on failure...", methodName);

    }
    @Test
    public void testAssertEmptyLine2() {
        final String data = lines(
                "abc",
                "",
                "jkl");


        new SimpleWriter("EUC-JP") {
            @Override
            protected void write() {
                print(data);
            }
        }.to("work/" + methodName + ".txt");
        target.assertFile("message on failure...", methodName);

    }


    /** 固定長ファイルのアサートが失敗した場合、例外が発生すること。 */
    @Test
    public void testAssertFixedLengthFileFail() {
        // 期待値と異なるファイルを準備
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                println("1                                                                        ",
                        "                                                         ");
                println("2ﾍﾝｺｳｺﾞ ｶﾅｼﾒｲ        変更後　漢字氏名　　　　　　　　　　　　Henkougo romaji        ",
                        "1henkougo@emal.cellar          01234          ");
                // ３行目がマッチしない。
                println("8  0000002                                                               ",
                        "                                                         ");
            }
        }.to("work/testAssertFixedLengthFile.txt")
         .deleteOnExit();


        // 例外が発生するはず
        ComparisonFailure actual = new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertFixedLengthFile");
            }
        }.capture(ComparisonFailure.class)
         .whichMessageContains("message on failure...")
         .getActual();

        // 期待値と実際の値の差異が示される
        assertThat(actual.getExpected(), containsString("総件数=1"));
        assertThat(actual.getActual(), containsString("総件数=2"));
    }

    /** 実際のファイルが空ファイルの場合アサートが失敗し、実際の値は空であること （固定長） */
    @Test
    public void testAssertFixedActuallyEmpty() {
        // 空ファイルを作成
        SimpleWriter.touch("work/testAssertFixedLengthFile.txt").deleteOnExit();
        // 例外が発生するはず
        ComparisonFailure actual = new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertFixedLengthFile");
            }
        }.capture(ComparisonFailure.class)
         .whichMessageContains("message on failure...")
         .getActual();
        // 期待値と実際の値の差異が示される
        assertThat(actual.getExpected(), containsString("総件数=1"));
        assertThat(actual.getActual(), is("[]"));     // 実際の値は空
    }

    /** 実際のファイルが存在しない場合、例外が発生すること(固定長) */
    @Test
    public void testAssertFixedActuallyNotExists() {
        remove(new File("work/testAssertFixedLengthFile.txt"));
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertFixedLengthFile");
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains("message on failure...")
                .whichContains(FileNotFoundException.class)
                        // FileNotFoundExceptionのメッセージ
                .whichMessageContains("work" + FILE_SEPARATOR + "testAssertFixedLengthFile.txt");
    }

    /** 可変長ファイルのアサートが失敗した場合、例外が発生すること。 */
    @Test
    public void testAssertVariableLengthFileFail() {
        // 期待値と異なるファイルを準備
        new SimpleWriter() {
            @Override
            protected void write() {
                println("1,");
                println("2,ﾍﾝｺｳｺﾞ ｶﾅｼﾒｲ,変更後　漢字氏名,Henkougo romaji,1,");
                println("8,,2,");   // ３行目がマッチしない。
            }
        }.to("work/testAssertVariableLengthFile.txt")
         .deleteOnExit();


        // 例外が発生するはず
        ComparisonFailure actual = new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertVariableLengthFile");
            }
        }.capture(ComparisonFailure.class)
         .whichMessageContains("message on failure...")
         .getActual();

        // 期待値と実際の値の差異が示される
        assertThat(actual.getExpected(), containsString("総件数=1"));
        assertThat(actual.getActual(), containsString("総件数=2"));
    }

    /** 実際のファイルが空ファイルの場合アサートが失敗し、実際の値は空であること （可変長） */
    @Test
    public void testAssertVariableActuallyEmpty() {
        // 空ファイル作成
        SimpleWriter.touch("work/testAssertVariableLengthFile.txt").deleteOnExit();

        // 例外が発生するはず
        ComparisonFailure actual = new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertVariableLengthFile");
            }
        }.capture(ComparisonFailure.class)
         .whichMessageContains("message on failure...")
         .getActual();
        // 期待値と実際の値の差異が示される
        assertThat(actual.getExpected(), containsString("総件数=1"));
        assertThat(actual.getActual(), is("[]"));     // 実際の値は空
    }

    /** 実際のファイルが存在しない場合、例外が発生すること(可変長) */
    @Test
    public void testAssertVariableActuallyNotExists() {
        remove(new File("work/testAssertVariableLengthFile.txt"));
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message on failure...", "testAssertVariableLengthFile");
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains("message on failure...")
                .whichContains(FileNotFoundException.class)
                        // FileNotFoundExceptionのメッセージ
                .whichMessageContains("work" + FILE_SEPARATOR + "testAssertVariableLengthFile.txt");
    }


    /** セットアップ対象のデータが存在しない場合、例外が発生すること。 */
    @Test
    public void testSetUpFixedLengthFileFail() {
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.setUpFile(methodName);
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains("data not found.");
    }

    /** 重複した名前を持つデータをセットアップしようとすると、例外が発生すること。 */
    @Test
    public void testSetUpFixedWithDuplicateName() {
        new Trap("重複した項目名があるデータはセットアップできない。") {
            @Override
            protected void shouldFail() throws Exception {
                target.setUpFile("testFixedDuplicateName");
            }
        }.capture(IllegalStateException.class)
         .whichContains(IllegalArgumentException.class)
         .whichMessageContains("Duplicate field names are not permitted in a record",
                               "duplicate field=[いいい]");
    }

    /** 重複した名前を持つデータでアサートしようとすると、例外が発生すること。 */
    @Test
    public void testAssertFixedWithDuplicateName() {
        new Trap("重複した項目名があるデータではアサートできない") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("", "testFixedDuplicateName");
            }
        }.capture(IllegalStateException.class)
         .whichContains(IllegalArgumentException.class)
         .whichMessageContains("Duplicate field names are not permitted in a record",
                               "duplicate field=[いいい]");
    }

    /** 重複した名前を持つデータをセットアップしようとすると、例外が発生すること。 */
    @Test
    public void testSetUpVariableWithDuplicateName() {
        new Trap("重複した項目名があるデータはセットアップできない。") {
            @Override
            protected void shouldFail() throws Exception {
                target.setUpFile("testVariableDuplicateName");
            }
        }.capture(IllegalStateException.class)
         .whichContains(IllegalArgumentException.class)
         .whichMessageContains("Duplicate field names are not permitted in a record",
                               "duplicate field=[カナ氏名]");
    }

    /** 重複した名前を持つデータでアサートしようとすると、例外が発生すること。 */
    @Test
    public void testAssertVariableWithDuplicateName() {
        new Trap("重複した項目名があるデータではアサートできない") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("", "testVariableDuplicateName");
            }
        }.capture(IllegalStateException.class)
         .whichContains(IllegalArgumentException.class)
         .whichMessageContains("Duplicate field names are not permitted in a record",
                               "duplicate field=[カナ氏名]");
    }

    /** 空ファイルを期待するが実際のファイルが空でない場合、比較失敗すること。 */
    @Test
    public void testAssertFixedAsEmptyFail() {
        // 1レコード分のデータを作成
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                int byteCnt = 130;
                for (int i = 0; i < byteCnt; i++) {
                    print('1');
                }
                println();
            }
        }.to("work/" + methodName + ".txt")
         .deleteOnExit();

        new Trap("実際のファイルが空で*ない*ので、比較失敗すること。") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("actual file is empty.", methodName);
            }
        }.capture(AssertionError.class)
         .whichMessageContains("file=[work/testAssertFixedAsEmptyFail.txt]",
                               "expected:<[[]]> but was:");
    }

    /** 空ファイルを期待するが実際のファイルが空でない場合、比較失敗すること。 */
    @Test
    public void testAssertVariableAsEmptyFail() {
        // 1レコード分のデータを作成
        new SimpleWriter("Windows-31J") {
            @Override
            protected void write() {
                println("8,,0,");  // トレーラレコード（総件数０件）
            }
        }.to("work/" + methodName + ".txt")
         .deleteOnExit();

        new Trap("実際のファイルが空で*ない*ので、比較失敗すること。") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("actual file is empty.", methodName);
            }
        }.capture(AssertionError.class)
         .whichMessageContains("file=[work/testAssertVariableAsEmptyFail.txt]",
                               "expected:<[[]]> but was:");
    }

    private VariableLengthFile invalid = new VariableLengthFile("path/to/file") {
        @Override
        public List<DataRecord> read() {
            throw new InvalidDataFormatException("for test.");
        }
    };

    @Test
    public void testAssertFileFailExpectingEmptyFile() {
        new Trap("空ファイルを期待する場合に例外が発生した場合、"
                         + "その旨のメッセージが例外に含まれること。") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertFile("message", invalid);
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains(
                 "message",
                 "file=[path/to/file]",
                 "empty file is expectedContent. but reading actual file was failed because of nested exception below"
         );
    }


    /** コンストラクタ呼び出しができること。*/
    @Test
    public void testConstructor() {
        new FileSupport(new TestSupport(getClass()));
    }

    @Test
    public void testIsEmptyFile() {

        List<DataRecord> records = new ArrayList<DataRecord>();
        assertThat("空のリストなので偽",
                target.isEmptyFile(records), is(true));

        DataRecord emptyRecord = new DataRecord();
        records.add(emptyRecord);
        assertThat("DataRecordの要素が0なので偽",
                target.isEmptyFile(records), is(true));

        DataRecord notEmpty = new DataRecord();
        notEmpty.put("not", "empty");
        records.add(notEmpty);
        assertThat("DataRecordの要素が1以上なので真",
                target.isEmptyFile(records), is(false));

    }

    /**
     * 入力ストリームから全データをテキストとして読み込む。
     * ストリームはクローズされる。
     *
     * @param in 入力ストリーム
     * @return テキスト
     */
    private static String readAll(InputStream in) {
        return readAll(in, Charset.forName("Windows-31J"));
    }

    private static String readAll(InputStream in, Charset encoding) {
        byte[] bytes = readBytes(in);
        return StringUtil.toString(bytes, encoding);
    }

    /**
     * 入力ストリームから全データをテキストとして読み込む。<br/>
     * ストリームはクローズされる。
     *
     * @param in 入力ストリーム
     * @return 読み込んだデータ
     */
    private static byte[] readBytes(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("input stream was null.");
        }
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        int c;
        try {
            while ((c = in.read()) != -1) {
                temp.write(c);
            }
            return temp.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("unexpected problem occurred.", e);
        } finally {
            FileUtil.closeQuietly(in);
        }
    }
}
