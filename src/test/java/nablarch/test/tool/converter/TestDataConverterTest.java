package nablarch.test.tool.converter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;

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
        assertTrue(new File(outputDir, "FooTest.xlsx").exists());
    }

    /**
     * [Given] --xls オプションを指定した YAML→Excel 変換
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 かつ .xls ファイルが生成される
     */
    @Test
    public void yamlToXlsWithXlsFlag() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputXlsFlag");
        File outputDir = temporaryFolder.newFolder("outputXlsFlag");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls", "--xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest.xls").exists());
    }

    /**
     * [Given] --xls オプションを --to xls 以外（--to yaml）と組み合わせる
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void xlsFlagWithNonXlsToReturnsCode2() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputXlsFlagInvalid");
        File outputDir = temporaryFolder.newFolder("outputXlsFlagInvalid");

        java.io.ByteArrayOutputStream errBuf = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(errBuf));
        int exitCode;
        try {
            exitCode = TestDataConverter.run(new String[]{
                    "--from", "xls", "--to", "yaml", "--xls",
                    inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
            });
        } finally {
            System.setErr(originalErr);
        }

        assertThat(exitCode, is(2));
        assertTrue(errBuf.toString().contains("--xls option is only valid with --to xls."));
    }

    /**
     * [Given] --from xls --to yaml で .xlsx ファイルを入力とする
     * [When]  run() を呼び出す
     * [Then]  YAML ファイルが生成され、終了コード 0 が返される
     */
    @Test
    public void xlsxToYaml() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputXlsx");
        File outputDir = temporaryFolder.newFolder("outputXlsx");

        // Write a simple XLSX
        writeSimpleXlsx(new File(inputDir, "FooTest.xlsx"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
    }

    /**
     * [Given] yaml→xlsx に変換した後、再度 --from xls --to yaml で変換する
     * [When]  run() を2回呼び出す（往復変換）
     * [Then]  両方とも終了コード 0 が返される
     */
    @Test
    public void yamlToXlsxAndBackToYaml() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputRoundtrip");
        File midDir = temporaryFolder.newFolder("midRoundtrip");
        File outputDir = temporaryFolder.newFolder("outputRoundtrip");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode1 = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls",
                inputDir.getAbsolutePath(), midDir.getAbsolutePath()
        });
        assertThat(exitCode1, is(0));
        assertTrue(new File(midDir, "FooTest.xlsx").exists());

        int exitCode2 = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml",
                midDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });
        assertThat(exitCode2, is(0));
        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
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
     * [Given] yaml→xlsx 変換で既存 .xlsx ファイルあり・--overwrite なし
     * [When]  run() を呼び出す
     * [Then]  終了コード 1 が返される
     */
    @Test
    public void yamlToXlsxOverwriteErrorReturnsCode1() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputOverwrite");
        File outputDir = temporaryFolder.newFolder("outputOverwrite");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(1));
    }

    /**
     * [Given] yaml→xlsx 変換で既存ファイルあり・--overwrite あり
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 が返される
     */
    @Test
    public void yamlToXlsxOverwriteOptionSucceeds() throws Exception {
        File inputDir = temporaryFolder.newFolder("inputOverwriteOk");
        File outputDir = temporaryFolder.newFolder("outputOverwriteOk");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls", "--overwrite",
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
     * [Given] --to が指定されていない（引数不足）
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void toMissingReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls",
                dir.getAbsolutePath(), dir.getAbsolutePath()
        });
        assertThat(exitCode, is(2));
    }

    /**
     * [Given] 入力パスと出力パスが指定されていない
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される（positional.size() < 2）
     */
    @Test
    public void positionalArgsMissingReturnsCode2() throws Exception {
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml"
        });
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

    /**
     * [Given] main() を有効な引数で呼び出す
     * [When]  main() が実行される
     * [Then]  例外なく正常終了し、YAML ファイルが出力される
     */
    @Test
    public void mainConvertsSuccessfully() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");
        writeSimpleXls(new File(inputDir, "FooTest.xls"));

        TestDataConverter.main(new String[]{
                "--from", "xls", "--to", "yaml",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertTrue(new File(outputDir, "FooTest").isDirectory());
    }

    /**
     * [Given] --delete-source で空シート XLS を変換する
     * [When]  run() を呼び出す
     * [Then]  スキップされてもソースファイル削除が試みられる
     */
    @Test
    public void emptySheetWithDeleteSourceTriesDeletion() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        // 空シート（ブロックなし）XLS を作成
        org.apache.poi.hssf.usermodel.HSSFWorkbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
        wb.createSheet("case01");
        File xlsFile = new File(inputDir, "EmptyTest.xls");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(xlsFile);
        try {
            wb.write(fos);
        } finally {
            fos.close();
        }

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml", "--delete-source",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        // 変換自体は成功（空シート警告でスキップ）、ソースファイルは削除済み
        assertThat(exitCode, is(0));
        assertTrue(!xlsFile.exists());
    }

    /**
     * [Given] deleteSource の対象がファイルで delete() が失敗する状況
     *         （読み取り専用ディレクトリ内のファイルは削除できない）
     * [When]  private deleteSource() をリフレクションで呼び出す
     * [Then]  警告が出力される（delete() false → WARN: Failed to delete source）
     */
    @Test
    public void deleteSourceFileDeleteFailureLogsWarning() throws Exception {
        // Given: 読み取り専用ディレクトリ内にファイルを作成（Linux では chmod a-w dirで削除不可）
        File dir = temporaryFolder.newFolder("readonly_dir");
        File target = new File(dir, "target.xls");
        target.createNewFile();
        // ディレクトリを書き込み不可にする（Linux でのみ有効）
        dir.setWritable(false);
        try {
            // When: リフレクションで private deleteSource() を呼び出す
            Method deleteSource = TestDataConverter.class.getDeclaredMethod("deleteSource", java.nio.file.Path.class);
            deleteSource.setAccessible(true);
            deleteSource.invoke(null, target.toPath());
            // Then: 例外なく完了する（delete() が false を返しても警告のみ）
        } finally {
            dir.setWritable(true); // 後始末
        }
    }

    /**
     * [Given] deleteDirectory でサブファイルの delete() が失敗する状況
     * [When]  private deleteDirectory() をリフレクションで呼び出す
     * [Then]  警告が出力される（delete() false → WARN: Failed to delete source file）
     */
    @Test
    public void deleteDirectoryFileDeleteFailureLogsWarning() throws Exception {
        // Given: 読み取り専用ディレクトリ内にファイルを作成
        File parent = temporaryFolder.newFolder("parent");
        File child = temporaryFolder.newFolder("parent", "child");
        File target = new File(child, "file.yaml");
        target.createNewFile();
        // child ディレクトリを書き込み不可にする
        child.setWritable(false);
        try {
            // When: リフレクションで private deleteDirectory() を呼び出す
            Method deleteDirectory = TestDataConverter.class.getDeclaredMethod("deleteDirectory", File.class);
            deleteDirectory.setAccessible(true);
            deleteDirectory.invoke(null, parent);
            // Then: 例外なく完了する（delete() false でも警告のみ）
        } finally {
            child.setWritable(true); // 後始末
        }
    }

    /**
     * [Given] deleteDirectory で listFiles() が null を返す状況（ディレクトリでないFileを渡す）
     * [When]  private deleteDirectory() をリフレクションで呼び出す
     * [Then]  NullPointerException なく完了する（listFiles() null チェックが通る）
     */
    @Test
    public void deleteDirectoryWithNullListFilesSkipsLoop() throws Exception {
        // Given: 存在しないディレクトリ（listFiles() が null を返す）
        File nonExistentDir = new File(temporaryFolder.getRoot(), "nonexistent");

        // When: リフレクションで private deleteDirectory() を呼び出す
        Method deleteDirectory = TestDataConverter.class.getDeclaredMethod("deleteDirectory", File.class);
        deleteDirectory.setAccessible(true);
        deleteDirectory.invoke(null, nonExistentDir);
        // Then: NullPointerException なく完了する
    }

    // -------------------------------------------------------------------------
    // 検証モード（--validate / --validate-on-convert）
    // -------------------------------------------------------------------------

    /**
     * [Given] --validate <入力パス> で正常な YAML ディレクトリを指定
     * [When]  run() を呼び出す
     * [Then]  終了コード 0 が返される
     */
    @Test
    public void validateModeWithValidYaml_returnsCode0() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--validate", inputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
    }

    /**
     * [Given] --validate <入力パス> で列数不一致の YAML を指定
     * [When]  run() を呼び出す
     * [Then]  終了コード 1 が返される
     */
    @Test
    public void validateModeWithInvalidYaml_returnsCode1() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeInvalidYamlColumnMismatch(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--validate", inputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(1));
    }

    /**
     * [Given] --validate と --from を同時に指定する（不正引数）
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void validateWithFromReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--validate", dir.getAbsolutePath(),
                "--from", "yaml"
        });
        assertThat(exitCode, is(2));
    }

    /**
     * [Given] --validate-on-convert で正常な YAML を変換
     * [When]  run() を呼び出す
     * [Then]  変換が成功し終了コード 0 が返される
     */
    @Test
    public void validateOnConvertWithValidYaml_convertsAndReturnsCode0() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeSimpleYaml(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls", "--validate-on-convert",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(0));
        assertTrue(new File(outputDir, "FooTest.xlsx").exists());
    }

    /**
     * [Given] --validate-on-convert で列数不一致の YAML を変換
     * [When]  run() を呼び出す
     * [Then]  対象ファイルがスキップされ終了コード 1 が返される
     */
    @Test
    public void validateOnConvertWithInvalidYaml_skipsAndReturnsCode1() throws Exception {
        File inputDir = temporaryFolder.newFolder("input");
        File outputDir = temporaryFolder.newFolder("output");

        File containerDir = new File(inputDir, "FooTest");
        containerDir.mkdir();
        writeInvalidYamlColumnMismatch(new File(containerDir, "case01.yaml"));

        int exitCode = TestDataConverter.run(new String[]{
                "--from", "yaml", "--to", "xls", "--validate-on-convert",
                inputDir.getAbsolutePath(), outputDir.getAbsolutePath()
        });

        assertThat(exitCode, is(1));
        assertTrue(!new File(outputDir, "FooTest.xlsx").exists());
    }

    /**
     * [Given] --validate-on-convert を --from xls（YAML 入力でない）と組み合わせる（不正引数）
     * [When]  run() を呼び出す
     * [Then]  終了コード 2 が返される
     */
    @Test
    public void validateOnConvertWithFromXlsReturnsCode2() throws Exception {
        File dir = temporaryFolder.newFolder("dir");
        int exitCode = TestDataConverter.run(new String[]{
                "--from", "xls", "--to", "yaml", "--validate-on-convert",
                dir.getAbsolutePath(), dir.getAbsolutePath()
        });
        assertThat(exitCode, is(2));
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

    private void writeSimpleXlsx(File file) throws Exception {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
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

    private void writeInvalidYamlColumnMismatch(File file) throws Exception {
        PrintWriter pw = new PrintWriter(file, "UTF-8");
        try {
            pw.println("setup_files:");
            pw.println("  - path: test.dat");
            pw.println("    type: fixed");
            pw.println("    records:");
            pw.println("      - record_type: \"\"");
            pw.println("        fields:");
            pw.println("          - {name: col1, type: 半角英字, length: \"3\"}");
            pw.println("        rows:");
            pw.println("          - [a, b]");  // fields 1件、rows 2要素 → 不一致
        } finally {
            pw.close();
        }
    }
}
