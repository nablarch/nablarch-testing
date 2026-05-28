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
     * [Given] --from に不正値を指定
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void invalidFromValueReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "csv", "--to", "yaml",
                dir.getAbsolutePath(), dir.getAbsolutePath()
        });
        assertThat(exitCode, is(2));
    }

    /**
     * [Given] --to に不正値を指定
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void invalidToValueReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "json",
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
    // 追加テスト（カバレッジ拡充）
    // -------------------------------------------------------------------------

    /**
     * [Given] --from の後に値がない（引数が不足）
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void fromWithMissingValueReturnsCode2() throws Exception {
        int exitCode = TestDataConverter.run(new String[]{"--from"});
        assertThat(exitCode, is(2));
    }

    /**
     * [Given] --include オプションで特定ファイルのみ指定
     * [When]  run() を呼び出す
     * [Then]  指定ファイルのみが変換され、他のファイルは出力されない
     */
    @Test
    public void includeOptionFiltersFiles() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        writeSimpleXls(new File(inputDir, "FooTest.xls"));
        writeSimpleXls(new File(inputDir, "BarTest.xls"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                "--include", "FooTest.xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
        assertTrue(!new File(outputDir, "BarTest/case01.yaml").exists());
    }

    /**
     * [Given] コメント行（"//" で始まる行）を含む XLS ファイル
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 が返される（コメント行はスキップされ警告が出る）
     */
    @Test
    public void xlsWithCommentLinesSucceedsWithWarning() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        // XLS with comment line before the data block
        org.apache.poi.hssf.usermodel.HSSFWorkbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("case01");
        sheet.createRow(0).createCell(0).setCellValue("// this is a comment");
        sheet.createRow(1).createCell(0).setCellValue("SETUP_TABLE=TBL");
        sheet.createRow(2).createCell(0).setCellValue("COL1");
        sheet.createRow(3).createCell(0).setCellValue("val1");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(inputDir, "FooTest.xls"));
        try {
            wb.write(fos);
        } finally {
            fos.close();
        }

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
    }

    /**
     * [Given] DataType 識別行が 1 つも存在しないシート（コメント行か不明行のみ）
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 が返される（空シート警告が出るが変換エラーではない）
     */
    @Test
    public void emptySheetWarnsAndSucceeds() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        // XLS with only comment rows (no data blocks)
        org.apache.poi.hssf.usermodel.HSSFWorkbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("case01");
        sheet.createRow(0).createCell(0).setCellValue("// comment only");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(inputDir, "FooTest.xls"));
        try {
            wb.write(fos);
        } finally {
            fos.close();
        }

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
    }

    /**
     * [Given] inputPath が存在しないディレクトリ
     * [When]  run() を呼び出す
     * [Then]  終了コード 1 が返される
     */
    @Test
    public void nonExistentInputPathReturnsCode1() throws Exception {
        File outputDir = temporaryFolder.newFolder("output");
        String nonExistentPath = temporaryFolder.getRoot().getAbsolutePath() + "/nonexistent";

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                nonExistentPath, outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(1));
    }

    /**
     * [Given] --delete-source で yaml→xls 変換を実行
     * [When]  run() を呼び出す
     * [Then]  変換後にソースディレクトリが削除される
     */
    @Test
    public void deleteSourceWithYamlToXlsDeletesSourceDirectory() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls", "--delete-source",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(!containerDir.exists());
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
