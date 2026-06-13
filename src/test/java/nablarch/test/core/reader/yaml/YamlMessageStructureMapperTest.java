package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.yaml.model.RawMessage;
import nablarch.test.core.reader.yaml.model.RawRecordLayout;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link YamlMessageStructureMapper} のテストクラス。
 *
 * <p>
 * 構造マッピング層が <b>値を一切加工せず</b>生の構造レコード {@link RawMessage} を返すことを、値加工層を
 * 通さずに直接検証する。特に {@code fw_header} は「マップであること」の検証すらせず生の {@link Object} の
 * まま保持し（検証は値加工層が読み出し時に遅延実行＝同一ファイル内の誤記エントリで他エントリを巻き添えに
 * しない旧挙動）、FW_HEADER レコードもスキップせず保持し、length・record_type の省略を {@code null} で
 * 保持することを確認する。
 * </p>
 */
public class YamlMessageStructureMapperTest {

    private static final String DIR = "src/test/java/nablarch/test/core/reader/yaml/";

    private final YamlMessageStructureMapper sut = new YamlMessageStructureMapper();

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    /**
     * mapMessages: {@code fw_header} マップは検証・文字列化せず生の {@link Map} のまま保持し、
     * directives・{@code ${...}} も未加工で保持されること。
     *
     * <p>Given: fw_header マップ・{@code ${...}} を含む messages<br>
     * When: mapMessages(yaml, "messages")<br>
     * Then: 値が一切加工されず RawMessage へ写ること</p>
     */
    @Test
    public void testMapMessages_preservesRawStructure() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When
        RawMessage req = sut.mapMessages(yaml, "messages").get(0);

        // Then
        assertThat(req.getId(), is("req001"));
        assertThat("group_id 省略は null", req.getGroupId(), is(nullValue()));
        assertThat("directives は未加工", req.getDirectives().get("text-encoding"), is("Windows-31J"));
        assertThat("fw_header は生の Map のまま（検証・文字列化しない）",
                req.getFwHeader(), is(instanceOf(Map.class)));
        Map<?, ?> fw = (Map<?, ?>) req.getFwHeader();
        assertThat("fw_header の ${...} も未加工", fw.get("requestId"), is("${rid}"));
        assertThat("本文セルの ${...} も未加工",
                req.getRecords().get(0).getRows().get(0).get(0), is("${key}"));
    }

    /**
     * mapMessages: {@code fw_header} がマップでない（リスト誤記）場合も、構造層は <b>例外を投げず</b>
     * 生の {@link java.util.List} のまま保持すること（マップ検証は値加工層が読み出し時に遅延実行）。
     */
    @Test
    public void testMapMessages_malformedFwHeaderKeptAsRawObjectWithoutException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When: 誤記エントリを含むファイルでも mapMessages 自体は例外を投げない
        RawMessage malformed = sut.mapMessages(yaml, "messages").get(1);

        // Then
        assertThat(malformed.getId(), is("malformed001"));
        assertThat("マップでない fw_header も生の値のまま保持（構造層は検証しない）",
                malformed.getFwHeader(), is(instanceOf(List.class)));
    }

    /**
     * mapMessages: {@code fw_header} 省略エントリは {@link RawMessage#getFwHeader()} が {@code null}。
     */
    @Test
    public void testMapMessages_omittedFwHeaderIsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When
        RawMessage bodyOnly = sut.mapMessages(yaml, "messages").get(2);

        // Then
        assertThat(bodyOnly.getId(), is("bodyOnly001"));
        assertThat("fw_header 省略は null", bodyOnly.getFwHeader(), is(nullValue()));
    }

    /**
     * mapMessages: FW_HEADER レコードをスキップせず保持し、length 省略は {@code null}、
     * record_type 省略は {@code null} で保持すること（スキップ・default 補完・{@code -} 注入は値加工層）。
     */
    @Test
    public void testMapMessages_keepsFwHeaderRecordAndOmittedLengthRecordType() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When
        RawMessage header = sut.mapMessages(yaml, "expected_request_header_messages").get(0);

        // Then
        List<RawRecordLayout> records = header.getRecords();
        assertThat("2 レコードを順序保持", records.size(), is(2));
        assertThat("FW_HEADER もスキップせず保持", records.get(0).getRecordType(), is("FW_HEADER"));
        assertThat("record_type 省略は null", records.get(1).getRecordType(), is(nullValue()));
        assertThat("length 省略は null（メッセージ長の \"-\" 注入は値加工層）",
                records.get(1).getFields().get(0).getLength(), is(nullValue()));
    }

    /**
     * mapMessages: SendSync 系の {@code group_id} も未加工で保持されること。
     */
    @Test
    public void testMapMessages_preservesGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When
        RawMessage sync = sut.mapMessages(yaml, "response_body_messages").get(0);

        // Then
        assertThat("group_id を未加工で保持", sync.getGroupId(), is("grp1"));
        assertThat(sync.getId(), is("sync001"));
    }

    /**
     * mapMessages: 該当セクションが存在しない場合は空リストを返すこと。
     */
    @Test
    public void testMapMessages_absentSectionReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageStructureMapperTest/raw");

        // When
        List<RawMessage> result = sut.mapMessages(yaml, "response_header_messages");

        // Then
        assertThat(result.isEmpty(), is(true));
    }
}
