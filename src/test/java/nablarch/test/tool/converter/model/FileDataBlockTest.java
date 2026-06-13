package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.model.FileDataBlock.FileType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link FileDataBlock} のテスト。
 *
 * <p>固定長／可変長の区別・ディレクティブ順序・レコードレイアウト群の保持を検証する。</p>
 */
public class FileDataBlockTest {

    @Test
    public void 固定長ファイルのディレクティブとレコードを順序保持する() {
        // Given: ディレクティブは記述順を保つ（LinkedHashMap）
        Map<String, String> directives = new LinkedHashMap<>();
        directives.put("file-type", "Fixed");
        directives.put("text-encoding", "MS932");
        List<RecordLayout> records = List.of(
                new RecordLayout("data", List.of(new FieldDef("id", "数値", "5")), List.of(List.of("1"))));

        // When
        FileDataBlock sut = new FileDataBlock(
                DataType.SETUP_FIXED, "g1", "test.dat", FileType.FIXED, directives, records);

        // Then
        assertThat(sut.getDataType(), is(DataType.SETUP_FIXED));
        assertThat(sut.getGroupId(), is("g1"));
        assertThat(sut.getIdentifier(), is("test.dat"));
        assertThat(sut.getFileType(), is(FileType.FIXED));
        assertThat(new ArrayList<>(sut.getDirectives().keySet()), is(List.of("file-type", "text-encoding")));
        assertThat(sut.getRecords(), is(records));
    }

    @Test
    public void 可変長ファイルの種別を区別し空のディレクティブとレコードを保持する() {
        // Given/When: 可変長・EXPECTED。ディレクティブもレコードも空
        FileDataBlock sut = new FileDataBlock(
                DataType.EXPECTED_VARIABLE, "", "out.csv", FileType.VARIABLE,
                new LinkedHashMap<>(), List.of());

        // Then
        assertThat(sut.getFileType(), is(FileType.VARIABLE));
        assertThat(sut.getDataType(), is(DataType.EXPECTED_VARIABLE));
        assertThat(sut.getDirectives().isEmpty(), is(true));
        assertThat(sut.getRecords().isEmpty(), is(true));
    }

    @Test
    public void 固定可変とSETUP_EXPECTEDの全組合せを保持する() {
        // Given: 4 つのファイル系データ種別と FileType の対角を網羅
        // When/Then
        assertFileBlock(DataType.SETUP_FIXED, FileType.FIXED);
        assertFileBlock(DataType.EXPECTED_FIXED, FileType.FIXED);
        assertFileBlock(DataType.SETUP_VARIABLE, FileType.VARIABLE);
        assertFileBlock(DataType.EXPECTED_VARIABLE, FileType.VARIABLE);
    }

    private static void assertFileBlock(DataType dataType, FileType fileType) {
        FileDataBlock sut = new FileDataBlock(
                dataType, "", "f", fileType, new LinkedHashMap<>(), List.of());
        assertThat(sut.getDataType(), is(dataType));
        assertThat(sut.getFileType(), is(fileType));
    }

    @Test
    public void FWヘッダレコードをスキップせず複数レコードを記述順で保持する() {
        // Given: FW_HEADER レコード + データレコードの 2 つを記述順で保持する
        RecordLayout fwHeader = new RecordLayout(
                "FW_HEADER", List.of(new FieldDef("requestId", "半角英字", "10")), List.of(List.of("RM11")));
        RecordLayout data = new RecordLayout(
                "data", List.of(new FieldDef("body", "全角", "-")), List.of(List.of("あ")));
        List<RecordLayout> records = List.of(fwHeader, data);

        // When
        FileDataBlock sut = new FileDataBlock(
                DataType.SETUP_FIXED, "", "msg.dat", FileType.FIXED, new LinkedHashMap<>(), records);

        // Then: FW_HEADER も保持され、レコード順は崩れない
        assertThat(sut.getRecords().size(), is(2));
        assertThat(sut.getRecords().get(0).getRecordType(), is("FW_HEADER"));
        assertThat(sut.getRecords().get(1).getRecordType(), is("data"));
    }

    @Test
    public void FileType列挙は固定長と可変長の2種() {
        // Then: 種別は FIXED / VARIABLE のみ
        assertThat(FileType.values().length, is(2));
        assertThat(FileType.valueOf("FIXED"), is(FileType.FIXED));
        assertThat(FileType.valueOf("VARIABLE"), is(FileType.VARIABLE));
    }
}
