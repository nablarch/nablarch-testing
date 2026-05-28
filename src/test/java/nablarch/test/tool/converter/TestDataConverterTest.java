package nablarch.test.tool.converter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link TestDataConverter} のテスト（6.4節）。
 *
 * <p>main() の代わりに run() を呼び出して終了コードを検証する。</p>
 */
public class TestDataConverterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // XLS → YAML
    // -------------------------------------------------------------------------

    /**
     * [Given] --from xls --to yaml で有効な XLS ファイルがある
     * [When]  run() を呼び出す
     * [Then]  YAML ファイルが生成され、終了コード 0 が返される
     */
    @Test
    public void xlsToYaml() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        // Write a simple XLS
        writeSimpleXls(new File(inputDir, "FooTest.xls"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
    }

    /**
     * [Given] --from yaml --to xls で有効な YAML ディレクトリがある
     * [When]  run() を呼び出す
     * [Then]  XLS ファイルが生成され、終了コード 0 が返される
     */
    @Test
    public void yamlToXls() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        // Write a simple YAML dir
        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest.xls").exists());
    }

    /**
     * [Given] 既存ファイルあり・--overwrite なし
     * [When]  run() を呼び出す
     * [Then]  終了コード 1 が返される
     */
    @Test
    public void overwriteErrorReturnsCode1() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        writeSimpleXls(new File(inputDir, "FooTest.xls"));

        TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        // Second run without --overwrite
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(1));
    }

    /**
     * [Given] --overwrite オプションがある・既存ファイルあり
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 が返される
     */
    @Test
    public void overwriteOptionSucceeds() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        writeSimpleXls(new File(inputDir, "FooTest.xls"));

        TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml", "--overwrite",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
    }

    /**
     * [Given] --from と --to が同じ形式
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void sameFromToReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "xls",
                dir.getAbsolutePath(), dir.getAbsolutePath()
        });
        assertThat(exitCode, is(2));
    }

    /**
     * [Given] --delete-source オプション付き
     * [When]  run() を呼び出す
     * [Then]  変換後に入力ファイルが削除される
     */
    @Test
    public void deleteSourceOption() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        File xlsFile = new File(inputDir, "FooTest.xls");
        writeSimpleXls(xlsFile);

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml", "--delete-source",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(!xlsFile.exists());
    }

    /**
     * [Given] --exclude パターンが指定されている
     * [When]  run() を呼び出す
     * [Then]  パターンに合致するファイルがスキップされる
     */
    @Test
    public void excludeOptionSkipsFiles() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        writeSimpleXls(new File(inputDir, "FooTest.xls"));
        writeSimpleXls(new File(inputDir, "template.xls"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                "--exclude", "template.xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
        assertTrue(!new File(outputDir, "template/case01.yaml").exists());
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    private void writeSimpleXls(File file) throws Exception {
        org.apache.poi.hssf.usermodel.HSSFWorkbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("case01");
        org.apache.poi.ss.usermodel.Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("SETUP_TABLE=TBL");
        org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("COL1");
        org.apache.poi.ss.usermodel.Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("val1");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        try {
            wb.write(fos);
        } finally {
            fos.close();
        }
    }

    private void writeSimpleYaml(File file) throws Exception {
        PrintWriter pw = new PrintWriter(file, "UTF-8");
        try {
            pw.println("setup_tables:");
            pw.println("  - table: TBL");
            pw.println("    rows:");
            pw.println("      - COL1: \"val1\"");
        } finally {
            pw.close();
        }
    }
}
