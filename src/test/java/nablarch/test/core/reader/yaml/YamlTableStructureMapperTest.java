package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.yaml.model.RawListMap;
import nablarch.test.core.reader.yaml.model.RawTableData;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link YamlTableStructureMapper} のテストクラス。
 *
 * <p>
 * 構造マッピング層が <b>値を一切加工せず</b>（記法・null・大文字小文字・マーカー・YAML 順を保持して）
 * 生の構造レコードを返すことを検証する。
 * </p>
 */
public class YamlTableStructureMapperTest {

    private static final String DIR = "src/test/java/nablarch/test/core/reader/yaml/";

    private final YamlTableStructureMapper sut = new YamlTableStructureMapper();

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    /**
     * mapTables: 先頭エントリの group_id は null、テーブル名・カラム名は大文字化されず YAML 記述のまま、
     * マーカーカラムも保持され、{@code ${...}} と null が未加工で保持されること。
     *
     * <p>Given: 大文字小文字混在・マーカー・{@code ${...}}・null を含む setup_tables<br>
     * When: mapTables(yaml, "setup_tables")<br>
     * Then: 値が一切加工されずに RawTableData へ写ること</p>
     */
    @Test
    public void testMapTables_preservesRawStructure() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableStructureMapperTest/raw");

        // When
        List<RawTableData> result = sut.mapTables(yaml, "setup_tables");

        // Then
        RawTableData first = result.get(0);
        assertThat("group_id 省略は null", first.getGroupId(), is(nullValue()));
        assertThat("テーブル名は大文字化されない", first.getTableName(), is("test_table"));
        assertThat("カラムはマーカー含む・YAML 順・大文字小文字保持",
                first.getColumnNames(), is(Arrays.asList("[NO]", "Pk_Col1", "Mixed_Case", "NullCol")));
        List<String> row0 = first.getRows().get(0);
        assertThat("マーカー値も保持", row0.get(0), is("1"));
        assertThat("${...} は未加工", row0.get(1), is("${updateTime}"));
        assertThat("通常値", row0.get(2), is("plain"));
        assertThat("null は null のまま", row0.get(3), is(nullValue()));
    }

    /**
     * mapTables: 空マッピング（{@code {}}）行は空リストとして保持され（除外せず）、行順が保たれること。
     * テーブルが空行をスキップするか list_maps が残すかの差異は値加工層の責務であり、構造層では行の有無を保持する。
     */
    @Test
    public void testMapTables_keepsEmptyMapRowAsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableStructureMapperTest/raw");

        // When
        List<RawTableData> result = sut.mapTables(yaml, "setup_tables");

        // Then: g1 エントリ（index 1）は [v1, {}, v2] → 3 行（中央は空リスト）
        RawTableData g1 = result.get(1);
        assertThat(g1.getGroupId(), is("g1"));
        assertThat("行数は保持（空マッピングも空リストとして残す）", g1.getRows().size(), is(3));
        assertThat(g1.getRows().get(0).get(0), is("v1"));
        assertThat("空マッピング行は空リスト", g1.getRows().get(1).isEmpty(), is(true));
        assertThat(g1.getRows().get(2).get(0), is("v2"));
    }

    /**
     * mapTables: rows が空のエントリも（除外せず）保持し、カラム・行は空であること。
     * 構造マッピング層は本体読み込みの「空行スキップ」方針を持たず、エントリ自体は保持する。
     */
    @Test
    public void testMapTables_keepsEmptyRowsEntry() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableStructureMapperTest/raw");

        // When
        List<RawTableData> result = sut.mapTables(yaml, "setup_tables");

        // Then: 4 エントリすべて保持（emptyRows・missingTable 含む）
        assertThat(result.size(), is(4));
        RawTableData emptyRows = result.get(2);
        assertThat(emptyRows.getGroupId(), is("emptyRows"));
        assertThat(emptyRows.getColumnNames().isEmpty(), is(true));
        assertThat(emptyRows.getRows().isEmpty(), is(true));
    }

    /**
     * mapTables: table 未指定エントリも例外を投げず tableName=null で保持すること
     * （必須チェックは値加工層の責務）。
     */
    @Test
    public void testMapTables_missingTableKeptAsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableStructureMapperTest/raw");

        // When
        List<RawTableData> result = sut.mapTables(yaml, "setup_tables");

        // Then
        RawTableData missing = result.get(3);
        assertThat(missing.getGroupId(), is("missingTable"));
        assertThat("table 未指定は null（例外を投げない）", missing.getTableName(), is(nullValue()));
    }

    /**
     * mapListMaps: カラム順は YAML 記述順（TreeMap によるソートをしない）、マーカー保持、
     * {@code ${...}} 未加工であること。
     */
    @Test
    public void testMapListMaps_preservesYamlOrderAndMarkers() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableStructureMapperTest/raw");

        // When
        List<RawListMap> result = sut.mapListMaps(yaml);

        // Then
        RawListMap lm = result.get(0);
        assertThat(lm.getId(), is("lm1"));
        assertThat("YAML 順を保持（zebra → alpha、ソートしない）・マーカー保持",
                lm.getColumnNames(), is(Arrays.asList("zebra", "alpha", "[MK]")));
        assertThat("${...} は未加工", lm.getRows().get(0).get(0), is("${z}"));
        assertThat(lm.getRows().get(0).get(1), is("a"));
        assertThat(lm.getRows().get(0).get(2), is("m"));
    }
}
