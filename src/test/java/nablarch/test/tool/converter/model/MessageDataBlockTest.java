package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link MessageDataBlock} のテスト。
 *
 * <p>ディレクティブ・FW 制御ヘッダ・本文レコードを順序保持し、FW ヘッダ無し経路を空 Map で表せることを検証する。</p>
 */
public class MessageDataBlockTest {

    @Test
    public void メッセージのディレクティブとFWヘッダと本文を順序保持する() {
        // Given: messages 経路。fw_header マップと本文レコード
        Map<String, String> directives = new LinkedHashMap<>();
        directives.put("text-encoding", "UTF-8");
        Map<String, String> fwHeader = new LinkedHashMap<>();
        fwHeader.put("requestId", "RM11AC0101");
        fwHeader.put("userId", "${userId}");
        List<RecordLayout> records = List.of(
                new RecordLayout(null, List.of(new FieldDef("data", "半角英字", "-")), List.of(List.of("X"))));

        // When
        MessageDataBlock sut = new MessageDataBlock(
                DataType.MESSAGE, "g1", "RM11AC0101", directives, fwHeader, records);

        // Then: 値は未加工（${userId} もそのまま）、順序保持
        assertThat(sut.getDataType(), is(DataType.MESSAGE));
        assertThat(sut.getGroupId(), is("g1"));
        assertThat(sut.getIdentifier(), is("RM11AC0101"));
        assertThat(new ArrayList<>(sut.getDirectives().keySet()), is(List.of("text-encoding")));
        assertThat(new ArrayList<>(sut.getFwHeaderFields().keySet()), is(List.of("requestId", "userId")));
        assertThat(sut.getFwHeaderFields().get("userId"), is("${userId}"));
        assertThat(sut.getRecords(), is(records));
    }

    @Test
    public void FWヘッダ非使用経路は空Mapで表しディレクティブとレコードも空で保持する() {
        // Given/When: expected_request_body_messages 等は fw_header を読まない＝空 Map
        MessageDataBlock sut = new MessageDataBlock(
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "", "RM11AC0101",
                new LinkedHashMap<>(), new LinkedHashMap<>(), List.of());

        // Then: 各コレクションが空であること
        assertThat(sut.getFwHeaderFields().isEmpty(), is(true));
        assertThat(sut.getDirectives().isEmpty(), is(true));
        assertThat(sut.getRecords().isEmpty(), is(true));
        assertThat(sut.getDataType(), is(DataType.EXPECTED_REQUEST_BODY_MESSAGES));
    }

    @Test
    public void メッセージ系の全データ種別を保持する() {
        // Given: 電文が取りうる 5 種別すべて
        for (DataType dt : List.of(
                DataType.MESSAGE,
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES,
                DataType.EXPECTED_REQUEST_BODY_MESSAGES,
                DataType.RESPONSE_HEADER_MESSAGES,
                DataType.RESPONSE_BODY_MESSAGES)) {
            // When
            MessageDataBlock sut = new MessageDataBlock(
                    dt, "", "RM11", new LinkedHashMap<>(), new LinkedHashMap<>(), List.of());
            // Then
            assertThat(sut.getDataType(), is(dt));
        }
    }

    @Test
    public void 複数の本文レコードを記述順で保持する() {
        // Given: ヘッダレコードと本文レコードの 2 つ
        RecordLayout header = new RecordLayout(
                "FW_HEADER", List.of(new FieldDef("requestId", "半角英字", "10")), List.of(List.of("RM11")));
        RecordLayout body = new RecordLayout(
                "data", List.of(new FieldDef("amount", "数値", "5")), List.of(List.of("100")));
        List<RecordLayout> records = List.of(header, body);

        // When
        MessageDataBlock sut = new MessageDataBlock(
                DataType.MESSAGE, "", "RM11", new LinkedHashMap<>(), new LinkedHashMap<>(), records);

        // Then: レコード順は崩れない
        assertThat(sut.getRecords().size(), is(2));
        assertThat(sut.getRecords().get(0).getRecordType(), is("FW_HEADER"));
        assertThat(sut.getRecords().get(1).getRecordType(), is("data"));
    }
}
