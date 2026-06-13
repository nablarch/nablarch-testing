package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.yaml.model.RawDataFile;
import nablarch.test.core.reader.yaml.model.RawFieldDef;
import nablarch.test.core.reader.yaml.model.RawRecordLayout;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link YamlFileStructureMapper} のテストクラス。
 *
 * <p>
 * 構造マッピング層が <b>値を一切加工せず</b>（記法・空文字・マーカー・長さ省略・record_type 省略・
 * FW_HEADER・YAML 順を保持して）生の構造レコード {@link RawDataFile} を返すことを、値加工層を通さずに
 * 直接検証する。必須項目チェック（path 欠落）やマーカー除外・{@code -} 注入は値加工層の責務であり、
 * 本層では行わないことも確認する。
 * </p>
 */
public class YamlFileStructureMapperTest {

    private static final String DIR = "src/test/java/nablarch/test/core/reader/yaml/";

    private final YamlFileStructureMapper sut = new YamlFileStructureMapper();

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    /**
     * mapFiles: directives は YAML 順・未加工で保持され、{@code ${...}} と空文字 {@code ""} がセル値として
     * 加工されずに残り、マーカー列名・大文字小文字も保持されること。
     *
     * <p>Given: directives・{@code ${...}}・空文字・マーカー列を含む固定長 setup_files<br>
     * When: mapFiles(yaml, "setup_files")<br>
     * Then: 値が一切加工されず RawDataFile へ写ること</p>
     */
    @Test
    public void testMapFiles_preservesRawStructure() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        List<RawDataFile> result = sut.mapFiles(yaml, "setup_files");

        // Then
        RawDataFile fixed = result.get(0);
        assertThat("group_id 省略は null", fixed.getGroupId(), is(nullValue()));
        assertThat("path は未加工", fixed.getPath(), is("dummy/setup_fixed.dat"));
        assertThat("type は未加工", fixed.getFileType(), is("fixed"));
        assertThat("directives は未加工で保持", fixed.getDirectives().get("text-encoding"), is("Windows-31J"));

        RawRecordLayout rec = fixed.getRecords().get(0);
        assertThat(rec.getRecordType(), is("DATA"));
        assertThat("マーカー列名も保持（除外は値加工層の責務）",
                rec.getFields().get(1).getName(), is("[MARKER]"));
        List<String> row0 = rec.getRows().get(0);
        assertThat("${...} は未加工", row0.get(0), is("${userId}"));
        assertThat("空文字は \"\" のまま（null でもトリムでもない）", row0.get(1), is(""));
    }

    /**
     * mapFiles: 可変長エントリの length 省略フィールドは {@link RawFieldDef#getLength()} が {@code null}、
     * 2 文字セパレータも長さを検証せず未加工で保持されること（値加工層の {@code -} 注入前）。
     */
    @Test
    public void testMapFiles_lengthOmittedIsNullAndSeparatorsRaw() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        RawDataFile variable = sut.mapFiles(yaml, "setup_files").get(1);

        // Then
        assertThat(variable.getGroupId(), is("grpVar"));
        assertThat("type は variable", variable.getFileType(), is("variable"));
        assertThat("セパレータは未加工", variable.getDirectives().get("field-separator"), is(","));
        assertThat("2 文字セパレータも長さ検証せず保持（検証は値加工層）",
                variable.getDirectives().get("multi-char-sep"), is(",,"));
        List<RawFieldDef> fields = variable.getRecords().get(0).getFields();
        assertThat("length 省略は null（既定補完しない）", fields.get(0).getLength(), is(nullValue()));
        assertThat(fields.get(1).getLength(), is(nullValue()));
    }

    /**
     * mapFiles: record_type 省略エントリは {@link RawRecordLayout#getRecordType()} が {@code null}
     * （default 補完は値加工層の責務）。
     */
    @Test
    public void testMapFiles_recordTypeOmittedIsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        RawDataFile noRt = sut.mapFiles(yaml, "setup_files").get(2);

        // Then
        assertThat(noRt.getGroupId(), is("noRecordType"));
        assertThat("record_type 省略は null", noRt.getRecords().get(0).getRecordType(), is(nullValue()));
    }

    /**
     * mapFiles: 複数レコードは YAML 順で保持され、FW_HEADER レコードもスキップされず残り、
     * length の {@code -} リテラルが未加工で保持されること。
     */
    @Test
    public void testMapFiles_keepsFwHeaderRecordAndLiteralDashLength() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        RawDataFile multi = sut.mapFiles(yaml, "setup_files").get(3);

        // Then
        List<RawRecordLayout> records = multi.getRecords();
        assertThat("2 レコードを順序保持", records.size(), is(2));
        assertThat("FW_HEADER もスキップせず保持", records.get(0).getRecordType(), is("FW_HEADER"));
        assertThat(records.get(1).getRecordType(), is("DATA"));
        assertThat("length の \"-\" リテラルは未加工で保持",
                records.get(1).getFields().get(0).getLength(), is("-"));
    }

    /**
     * mapFiles: path 省略エントリも例外を投げず {@code path=null} で保持し、records 空エントリは
     * 空リストで保持すること（必須チェック・空判定は値加工層の責務）。
     */
    @Test
    public void testMapFiles_missingPathKeptAsNullAndEmptyRecordsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        List<RawDataFile> result = sut.mapFiles(yaml, "setup_files");

        // Then
        assertThat("全 6 エントリ保持", result.size(), is(6));
        RawDataFile missingPath = result.get(4);
        assertThat(missingPath.getGroupId(), is("missingPath"));
        assertThat("path 未指定は null（例外を投げない）", missingPath.getPath(), is(nullValue()));
        RawDataFile emptyFile = result.get(5);
        assertThat(emptyFile.getGroupId(), is("emptyFile"));
        assertThat("records 空は空リスト保持", emptyFile.getRecords().isEmpty(), is(true));
    }

    /**
     * mapFiles: 該当セクションが存在しない場合は空リストを返すこと。
     */
    @Test
    public void testMapFiles_absentSectionReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileStructureMapperTest/raw");

        // When
        List<RawDataFile> result = sut.mapFiles(yaml, "expected_files");

        // Then
        assertThat(result.isEmpty(), is(true));
    }
}
