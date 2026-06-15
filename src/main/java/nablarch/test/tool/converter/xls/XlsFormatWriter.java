package nablarch.test.tool.converter.xls;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.TestDataFormatWriter;
import nablarch.test.tool.converter.model.ColumnRowDataBlock;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 中間モデル（{@link TestDataContainer}）を Excel（{@code .xlsx}）へ書き出す OUT ライタ。
 *
 * <p>
 * {@link XlsFormatReader} と<b>版面対称</b>に直列化する。Reader が委譲する本体パーサ
 * （{@code nablarch.test.core.reader}）が読み戻せる版面で書くため、書いた Excel を Reader で読み直すと
 * 同じ中間モデルへ戻る（往復。色・書式・列幅といった整形は NTF 仕様上の意味を持たないため可逆性の対象外）。
 * </p>
 *
 * <p>各データタイプの版面（本体パーサが要求する行/列の配置）は次のとおり。</p>
 * <ul>
 *   <li><b>TABLE／LIST_MAP</b>: 識別行（{@code TYPE[group]=id}）→ カラム名行 → データ行。
 *       いずれも列 0 始まり。マーカーカラム（{@code [...]}）は読み戻し時に本体が除外する。</li>
 *   <li><b>FILE</b>: 識別行 → ディレクティブ行（{@code key, value}）→ レコードごとに
 *       名前行（{@code レコード種別, フィールド名…}）・型行（{@code "", 型…}）・
 *       長さ行（固定長のみ {@code "", 長さ…}）・データ行（{@code "", 値…}）。
 *       本体は「列 0 が空＝データ行 / 非空＝新レコードの名前行」で判別するため、2 レコード目以降の
 *       名前行（レコード種別）は非空である必要がある。</li>
 *   <li><b>MESSAGE</b>: 識別行 → ディレクティブ行 → FW 制御ヘッダ行（{@code key, value}）→ 本文レコード
 *       （FILE と同型・本文は固定長）。本文データ行の列 0 は空。</li>
 *   <li><b>送信系 4 種</b>: MESSAGE と同型だが FW 制御ヘッダは無く、データ行の列 0 は {@code no}（連番）。
 *       本体は列 0 を {@code no} 値として取り除く。</li>
 * </ul>
 *
 * <p>
 * 値は記法のまま書く（クォート付与・トリムはしない。本体読み込みは空インタープリタで原文を保つ）。
 * 明示的な {@code null} 値はセルに {@code null} リテラルを書く（NTF のテストデータ慣習。空インタープリタの
 * 読み戻しでは文字列 {@code "null"} として戻るため、{@code null}↔{@code null} は Excel 経路では復元されない）。
 * </p>
 *
 * <p>
 * 1 コンテナ＝1 ブック（{@code <basePath>/<コンテナ名>.xlsx}）、1 セクション＝1 シートへ出力する。
 * 整形は {@link ExcelFormatConfig}（既定を備え上書き可能）に従う。
 * </p>
 *
 * @author kiyobot
 */
public final class XlsFormatWriter implements TestDataFormatWriter {

    /** 出力拡張子。 */
    private static final String EXTENSION = ".xlsx";

    /** 整形設定。 */
    private final ExcelFormatConfig config;

    /**
     * 既定の整形設定（{@link ExcelFormatConfig#defaults()}）で構成する。
     */
    public XlsFormatWriter() {
        this(ExcelFormatConfig.defaults());
    }

    /**
     * 整形設定を指定して構成する。
     *
     * @param config 整形設定
     */
    public XlsFormatWriter(ExcelFormatConfig config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     * <p>
     * コンテナを {@code <basePath>/<コンテナ名>.xlsx}（各セクション＝1 シート）へ書き出す。
     * </p>
     */
    @Override
    public void write(TestDataContainer container, String basePath) {
        Path file = Paths.get(basePath, container.getName() + EXTENSION);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Workbook workbook = build(container);
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write Excel: " + file, e);
        }
    }

    /**
     * コンテナを Excel ブック（メモリ上）へ組み立てる。テスト容易化のためファイル出力と分離する。
     *
     * @param container 中間モデル
     * @return 組み立て済みブック
     */
    Workbook build(TestDataContainer container) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Styles styles = new Styles(workbook, config);
        for (TestDataSection section : container.getSections()) {
            Sheet sheet = workbook.createSheet(section.getName());
            writeSection(sheet, section, styles);
        }
        return workbook;
    }

    /**
     * 1 セクションを 1 シートへ書き出す。
     *
     * @param sheet   出力先シート
     * @param section セクション
     * @param styles  セルスタイルキャッシュ
     */
    private void writeSection(Sheet sheet, TestDataSection section, Styles styles) {
        WidthTracker widths = new WidthTracker();
        int rowNum = 0;
        boolean first = true;
        for (TestDataBlock block : section.getBlocks()) {
            if (!first) {
                rowNum += config.getBlankRowsBetweenBlocks();
            }
            first = false;
            rowNum = render(sheet, rowNum, layout(block), styles, widths);
        }
        if (config.isAutoColumnWidth()) {
            widths.applyTo(sheet);
        }
    }

    // ------------------------------------------------------------------------
    // 版面（BlockLayout）の組み立て
    // ------------------------------------------------------------------------

    /**
     * ブロックを版面（行と種別の並び）へ写す。
     *
     * @param block ブロック
     * @return 版面
     */
    private BlockLayout layout(TestDataBlock block) {
        if (block instanceof ColumnRowDataBlock) {
            return layoutColumnRow((ColumnRowDataBlock) block);
        } else if (block instanceof FileDataBlock) {
            return layoutFile((FileDataBlock) block);
        } else if (block instanceof MessageDataBlock) {
            return layoutMessage((MessageDataBlock) block);
        }
        throw new IllegalArgumentException("unsupported block: " + block.getClass().getName());
    }

    /**
     * テーブル／LIST_MAP の版面を組み立てる（識別行 → カラム名行 → データ行）。
     *
     * @param block カラム・行ブロック
     * @return 版面
     */
    private BlockLayout layoutColumnRow(ColumnRowDataBlock block) {
        BlockLayout l = new BlockLayout();
        l.add(RowKind.HEADER, Arrays.asList(marker(block)));
        List<String> columns = block.getColumnNames();
        l.add(RowKind.HEADER, new ArrayList<String>(columns));
        for (int c = 0; c < columns.size(); c++) {
            if (isMarkerColumn(columns.get(c))) {
                l.markMarkerColumn(c);
            }
        }
        for (List<String> row : block.getRows()) {
            l.add(RowKind.DATA, literals(row));
        }
        return l;
    }

    /**
     * ファイルの版面を組み立てる（識別行 → ディレクティブ → レコード群）。
     *
     * @param block ファイルブロック
     * @return 版面
     */
    private BlockLayout layoutFile(FileDataBlock block) {
        BlockLayout l = new BlockLayout();
        l.add(RowKind.HEADER, Arrays.asList(marker(block)));
        appendKeyValueRows(l, block.getDirectives());
        boolean fixed = block.getFileType() == FileDataBlock.FileType.FIXED;
        for (RecordLayout record : block.getRecords()) {
            appendRecord(l, record, fixed, false);
        }
        return l;
    }

    /**
     * メッセージ（MESSAGE／送信系）の版面を組み立てる
     * （識別行 → ディレクティブ → FW 制御ヘッダ → 本文レコード群）。
     *
     * @param block メッセージブロック
     * @return 版面
     */
    private BlockLayout layoutMessage(MessageDataBlock block) {
        BlockLayout l = new BlockLayout();
        l.add(RowKind.HEADER, Arrays.asList(marker(block)));
        appendKeyValueRows(l, block.getDirectives());
        appendKeyValueRows(l, block.getFwHeaderFields());
        boolean sendSync = isSendSync(block.getDataType());
        for (RecordLayout record : block.getRecords()) {
            // 本文は固定長（長さ行を持つ）。送信系はデータ行の列 0 に no（連番）を置く。
            appendRecord(l, record, true, sendSync);
        }
        return l;
    }

    /**
     * 1 レコードレイアウト（名前行・型行・長さ行・データ行）を版面へ追加する。
     *
     * @param l        版面
     * @param record   レコードレイアウト
     * @param fixed    固定長（長さ行を持つ）なら真
     * @param sendSync 送信系（データ行の列 0 に no を置く）なら真
     */
    private void appendRecord(BlockLayout l, RecordLayout record, boolean fixed, boolean sendSync) {
        List<FieldDef> fields = record.getFields();

        List<String> nameRow = new ArrayList<String>();
        nameRow.add(nullToEmpty(record.getRecordType()));
        for (FieldDef field : fields) {
            nameRow.add(field.getName());
        }
        l.add(RowKind.HEADER, nameRow);

        List<String> typeRow = new ArrayList<String>();
        typeRow.add("");
        for (FieldDef field : fields) {
            typeRow.add(nullToEmpty(field.getType()));
        }
        l.add(RowKind.HEADER, typeRow);

        if (fixed) {
            List<String> lengthRow = new ArrayList<String>();
            lengthRow.add("");
            for (FieldDef field : fields) {
                lengthRow.add(nullToEmpty(field.getLength()));
            }
            l.add(RowKind.HEADER, lengthRow);
        }

        int seq = 1;
        for (List<String> values : record.getRows()) {
            List<String> dataRow = new ArrayList<String>();
            dataRow.add(sendSync ? String.valueOf(seq++) : "");
            for (String value : values) {
                dataRow.add(nullToLiteral(value));
            }
            l.add(RowKind.DATA, dataRow);
        }
    }

    /**
     * {@code key, value} 形式のメタ行（ディレクティブ／FW 制御ヘッダ）を版面へ追加する。
     *
     * @param l   版面
     * @param map キー → 値（記述順）
     */
    private void appendKeyValueRows(BlockLayout l, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            l.add(RowKind.META, Arrays.asList(entry.getKey(), nullToEmpty(entry.getValue())));
        }
    }

    // ------------------------------------------------------------------------
    // 版面 → シート（セル生成・スタイル・列幅）
    // ------------------------------------------------------------------------

    /**
     * 版面をシートへ描画する。ブロックを矩形に整え（不足セルは空文字で補填）、外枠罫線・背景色・列幅を付す。
     *
     * @param sheet    出力先シート
     * @param startRow 開始行
     * @param layout   版面
     * @param styles   セルスタイルキャッシュ
     * @param widths   列幅トラッカ
     * @return 次に書き込むべき行番号
     */
    private int render(Sheet sheet, int startRow, BlockLayout layout, Styles styles, WidthTracker widths) {
        int width = layout.width();
        int rowCount = layout.size();
        for (int r = 0; r < rowCount; r++) {
            Row row = sheet.createRow(startRow + r);
            List<String> cells = layout.row(r);
            boolean header = layout.kind(r) == RowKind.HEADER;
            for (int c = 0; c < width; c++) {
                String value = c < cells.size() ? cells.get(c) : "";
                Cell cell = row.createCell(c);
                cell.setCellValue(value);
                Fill fill = layout.isMarkerColumn(c) ? Fill.MARKER
                        : (header ? Fill.HEADER : Fill.NONE);
                cell.setCellStyle(styles.get(r == 0, r == rowCount - 1, c == 0, c == width - 1, fill));
                widths.observe(c, value.length());
            }
        }
        return startRow + rowCount;
    }

    // ------------------------------------------------------------------------
    // 値・カラムのヘルパ
    // ------------------------------------------------------------------------

    /**
     * ブロックの識別セル文字列（{@code TYPE[group]=identifier}）を生成する。
     * グループ ID は中間モデルが整形済み（{@code [g1]} もしくは空文字）で保持するためそのまま連結する。
     *
     * @param block ブロック
     * @return 識別セル文字列
     */
    private static String marker(TestDataBlock block) {
        return block.getDataType().getName() + block.getGroupId() + "=" + block.getIdentifier();
    }

    /**
     * マーカーカラム（{@code [...]} 形式）か判定する。
     *
     * @param columnName カラム名
     * @return マーカーカラムなら真
     */
    private static boolean isMarkerColumn(String columnName) {
        return columnName != null && columnName.startsWith("[") && columnName.endsWith("]");
    }

    /**
     * データ行の値を版面用に写す（{@code null} はリテラル {@code null}、それ以外は記法のまま）。
     *
     * @param row 値（{@code null} セルを含みうる）
     * @return 版面用の文字列リスト
     */
    private static List<String> literals(List<String> row) {
        List<String> result = new ArrayList<String>(row.size());
        for (String value : row) {
            result.add(nullToLiteral(value));
        }
        return result;
    }

    /**
     * {@code null} をリテラル {@code null} へ、それ以外は記法のまま返す（データ行の値用）。
     *
     * @param value 値
     * @return セル文字列
     */
    private static String nullToLiteral(String value) {
        return value == null ? "null" : value;
    }

    /**
     * {@code null} を空文字へ、それ以外は記法のまま返す（メタ・型・長さ等、省略を空セルで表す箇所用）。
     *
     * @param value 値
     * @return セル文字列
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 送信系（要求/応答電文 4 種）のデータタイプか判定する。
     *
     * @param type データタイプ
     * @return 送信系なら真
     */
    private static boolean isSendSync(DataType type) {
        return type == DataType.EXPECTED_REQUEST_HEADER_MESSAGES
                || type == DataType.EXPECTED_REQUEST_BODY_MESSAGES
                || type == DataType.RESPONSE_HEADER_MESSAGES
                || type == DataType.RESPONSE_BODY_MESSAGES;
    }

    // ------------------------------------------------------------------------
    // 補助型
    // ------------------------------------------------------------------------

    /** 行の種別（背景色の付与判定に使う）。 */
    private enum RowKind {
        /** 識別行・各種ヘッダ行（背景色を付す）。 */
        HEADER,
        /** ディレクティブ・FW 制御ヘッダ等のメタ行（背景色なし）。 */
        META,
        /** データ行（背景色なし）。 */
        DATA
    }

    /** セル背景色の種類。 */
    private enum Fill {
        /** 背景色なし。 */
        NONE,
        /** ヘッダ背景色。 */
        HEADER,
        /** マーカーカラム背景色。 */
        MARKER
    }

    /**
     * 1 ブロックの版面（行・行種別・マーカーカラム位置）。
     * <p>各行のセル数は不揃いでよく、描画時にブロック最大幅へ矩形整形される。</p>
     */
    private static final class BlockLayout {

        /** 行（記述順）。 */
        private final List<List<String>> rows = new ArrayList<List<String>>();

        /** 各行の種別（{@link #rows} と同順）。 */
        private final List<RowKind> kinds = new ArrayList<RowKind>();

        /** マーカーカラムの列番号。 */
        private final Set<Integer> markerColumns = new HashSet<Integer>();

        /** ブロック最大幅（列数）。 */
        private int width = 0;

        /**
         * 1 行を追加する。
         *
         * @param kind  行種別
         * @param cells セル
         */
        void add(RowKind kind, List<String> cells) {
            rows.add(cells);
            kinds.add(kind);
            if (cells.size() > width) {
                width = cells.size();
            }
        }

        /**
         * マーカーカラムを登録する。
         *
         * @param column 列番号
         */
        void markMarkerColumn(int column) {
            markerColumns.add(column);
        }

        /** @return 行数 */
        int size() {
            return rows.size();
        }

        /** @return ブロック最大幅 */
        int width() {
            return width;
        }

        /**
         * @param r 行番号
         * @return 当該行のセル
         */
        List<String> row(int r) {
            return rows.get(r);
        }

        /**
         * @param r 行番号
         * @return 当該行の種別
         */
        RowKind kind(int r) {
            return kinds.get(r);
        }

        /**
         * @param column 列番号
         * @return マーカーカラムなら真
         */
        boolean isMarkerColumn(int column) {
            return markerColumns.contains(column);
        }
    }

    /**
     * 列ごとの最大文字数を集計し、自動列幅をシートへ適用するトラッカ。
     */
    private static final class WidthTracker {

        /** Excel の列幅上限（文字数）。 */
        private static final int MAX_CHARS = 255;

        /** 余白（文字数）。 */
        private static final int PADDING = 2;

        /** 1 文字あたりの列幅単位（POI）。 */
        private static final int WIDTH_UNIT = 256;

        /** 列番号 → 最大文字数。 */
        private final Map<Integer, Integer> maxChars = new HashMap<Integer, Integer>();

        /**
         * 1 セルの文字数を観測する。
         *
         * @param column 列番号
         * @param length 文字数
         */
        void observe(int column, int length) {
            Integer current = maxChars.get(column);
            if (current == null || length > current) {
                maxChars.put(column, length);
            }
        }

        /**
         * 集計した最大文字数に基づき列幅を設定する。
         *
         * @param sheet 対象シート
         */
        void applyTo(Sheet sheet) {
            for (Map.Entry<Integer, Integer> entry : maxChars.entrySet()) {
                int chars = Math.min(entry.getValue() + PADDING, MAX_CHARS);
                sheet.setColumnWidth(entry.getKey(), chars * WIDTH_UNIT);
            }
        }
    }

    /**
     * セルスタイルのキャッシュ。外枠罫線（細線）と背景色の組み合わせは有限（外枠 4 辺 × 背景 3 種）のため、
     * 同一構成のスタイルを使い回して POI のスタイル数上限を回避する。
     */
    private static final class Styles {

        /** ブック。 */
        private final Workbook workbook;

        /** 整形設定。 */
        private final ExcelFormatConfig config;

        /** 構成キー → スタイル。 */
        private final Map<String, CellStyle> cache = new HashMap<String, CellStyle>();

        /**
         * コンストラクタ。
         *
         * @param workbook ブック
         * @param config   整形設定
         */
        Styles(Workbook workbook, ExcelFormatConfig config) {
            this.workbook = workbook;
            this.config = config;
        }

        /**
         * 指定構成のセルスタイルを返す（キャッシュ）。
         *
         * @param top    上辺に罫線を引くか
         * @param bottom 下辺に罫線を引くか
         * @param left   左辺に罫線を引くか
         * @param right  右辺に罫線を引くか
         * @param fill   背景色
         * @return セルスタイル
         */
        CellStyle get(boolean top, boolean bottom, boolean left, boolean right, Fill fill) {
            String key = (top ? 1 : 0) + "" + (bottom ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0) + fill;
            CellStyle style = cache.get(key);
            if (style == null) {
                style = workbook.createCellStyle();
                if (config.isDrawBlockBorder()) {
                    if (top) {
                        style.setBorderTop(CellStyle.BORDER_THIN);
                    }
                    if (bottom) {
                        style.setBorderBottom(CellStyle.BORDER_THIN);
                    }
                    if (left) {
                        style.setBorderLeft(CellStyle.BORDER_THIN);
                    }
                    if (right) {
                        style.setBorderRight(CellStyle.BORDER_THIN);
                    }
                }
                if (fill != Fill.NONE) {
                    short color = fill == Fill.HEADER
                            ? config.getHeaderColorIndex()
                            : config.getMarkerColumnColorIndex();
                    style.setFillForegroundColor(color);
                    style.setFillPattern(CellStyle.SOLID_FOREGROUND);
                }
                cache.put(key, style);
            }
            return style;
        }
    }
}
