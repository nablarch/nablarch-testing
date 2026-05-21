package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link YamlFileBuilder} のテストクラス。
 *
 * <p>
 * DataFile の構築ロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlFileBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private YamlFileBuilder sut;

    @Before
    public void before() {
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        sut = new YamlFileBuilder(interpreters);
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ========================================================================
    // buildFileList: 固定長・可変長ファイルが取得できること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: グループ ID なしで固定長・可変長ファイルが取得できること。
     *
     * <p>
     * Given: setup_files に fixed と variable の 2 エントリ<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void testBuildFileList_fixedAndVariable() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: setup_files に path=dummy/setup_fixed.dat のエントリ<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  getPath() が正しいパスを返すこと
     * </p>
     */
    @Test
    public void testBuildFileList_pathIsSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.get(0).getPath(), is("dummy/setup_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/setup_variable.csv"));
    }

    /**
     * [YamlFileBuilder] buildFileList: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files に grp1 グループのエントリ<br>
     * When:  buildFileList(yaml, "setup_files", "[grp1]", path) を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void testBuildFileList_withGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "[grp1]", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: expected_files の末尾セクションデータが欠落しないこと（RS-07）。
     *
     * <p>
     * Given: setup_files の後に expected_files が YAML 末尾に記述されている<br>
     * When:  buildFileList(yaml, "expected_files", "", path) を呼ぶ<br>
     * Then:  末尾セクションのデータが欠落せず 2 件返ること（RS-07）
     * </p>
     */
    @Test
    public void testBuildFileList_lastSectionNotLost() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "expected_files", "", DIR);

        // Then: 末尾セクションが欠落していないこと
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: セクションが存在しない場合は空リストが返ること。
     *
     * <p>
     * Given: setup_files キーが存在しない YAML<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testBuildFileList_sectionNotExists() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/emptyYaml");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // ディレクティブが正しく設定されること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 複数のグループ（グループIDなし・grp1）が存在する場合、
     * グループIDなしの件数が正しく取得されること。
     *
     * <p>
     * Given: setup_files にグループIDなし 2 件 + grp1 の 1 件<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  グループIDなしの 2 件のみ返ること
     * </p>
     */
    @Test
    public void testBuildFileList_onlyNoGroupIdEntries() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);

        // Then: グループIDなしの 2 件のみ
        assertThat(result.size(), is(2));
    }

    // ========================================================================
    // ディレクティブが正しく設定されること（QA-2）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: directives が DataFile に正しく設定されること（QA-2）。
     *
     * <p>
     * Given: setup_files の fixed エントリに text-encoding: Windows-31J が指定されている<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  getDirective("text-encoding") が "Windows-31J" を返すこと
     * </p>
     */
    @Test
    public void testBuildFileList_directivesAreSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.get(0).createLayout().getDirective().get("text-encoding"), is("Windows-31J"));
    }

    // ========================================================================
    // record_type が YAML に存在しない場合 "default" にフォールバックすること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: records に record_type キーが存在しない場合 "default" にフォールバックすること（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_files の noRecordType グループのエントリで records に record_type キーなし<br>
     * When:  buildFileList(yaml, "setup_files", "[noRecordType]", path) を呼ぶ<br>
     * Then:  FixedLengthFile のフラグメントの record_type が "default" であること
     * </p>
     */
    @Test
    public void testBuildFileList_recordTypeNullFallbackToDefault() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "[noRecordType]", DIR);

        // Then: record_type がない場合 "default" にフォールバックすること
        assertThat(result.size(), is(1));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("record_type なしの場合は 'default' にフォールバックすること",
                layout.getRecords().get(0).getTypeName(), is("default"));
    }

    // ========================================================================
    // 可変長ファイルで length なしのフィールドが正しく扱われること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 可変長ファイルで length が指定されていない場合、setLengths が呼ばれないこと（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_files の variable エントリで fields に length なし<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  VariableLengthFile が返り、レコード定義に lengths が含まれないこと（record-length ディレクティブなし）
     * </p>
     */
    @Test
    public void testBuildFileList_variableFileWithNoLength() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = sut.buildFileList(yaml, "setup_files", "", DIR);
        VariableLengthFile variableFile = (VariableLengthFile) result.get(1);

        // Then: length なしフィールドの場合 record-length ディレクティブが null であること（setLengths は呼ばれない）
        LayoutDefinition layout = variableFile.createLayout();
        assertThat("可変長ファイルでは record-length ディレクティブが設定されないこと",
                layout.getDirective().get("record-length"), nullValue());
    }
}
