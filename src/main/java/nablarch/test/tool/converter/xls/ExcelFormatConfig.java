package nablarch.test.tool.converter.xls;

import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * {@link XlsFormatWriter} の整形設定。
 *
 * <p>
 * Excel OUT は人が見て編集する前提のため、行種別ごとの装飾やレイアウトで読みやすく整える
 * （設計書 §OUT 整形方針）。整形は NTF 仕様上の意味を持たず、読み戻し（{@link XlsFormatReader}）は
 * 装飾を無視するため、整形の有無・配色は往復の可逆性に影響しない。
 * </p>
 *
 * <p>
 * 不変オブジェクトで、{@link #defaults()} が見やすい既定値を提供する。各 {@code with*} は当該項目だけを
 * 差し替えたコピーを返すため、{@code ExcelFormatConfig.defaults().withBlankRowsBetweenBlocks(0)} のように
 * 既定値を部分的に上書きできる。
 * </p>
 *
 * <p>
 * 背景色は POI のインデックスカラー（{@link IndexedColors#getIndex()}）で保持する。既定は識別行・各種
 * ヘッダ行に淡い青（{@link IndexedColors#PALE_BLUE}）、マーカーカラムに淡い橙
 * （{@link IndexedColors#LIGHT_ORANGE}）を当て、データ行と視覚的に区別する。
 * </p>
 *
 * @author kiyobot
 */
public final class ExcelFormatConfig {

    /** データタイプ識別行・各種ヘッダ行の背景色（POI インデックスカラー）。 */
    private final short headerColorIndex;

    /** マーカーカラムの背景色（POI インデックスカラー）。 */
    private final short markerColumnColorIndex;

    /** 列幅を各列の値の最大文字数に合わせて自動調整するか。 */
    private final boolean autoColumnWidth;

    /** データブロックの外枠に細線の罫線を引くか。 */
    private final boolean drawBlockBorder;

    /** データブロック間に挿入する空行数（0 以上）。 */
    private final int blankRowsBetweenBlocks;

    /**
     * コンストラクタ。
     *
     * @param headerColorIndex       識別行・ヘッダ行の背景色（POI インデックスカラー）
     * @param markerColumnColorIndex マーカーカラムの背景色（POI インデックスカラー）
     * @param autoColumnWidth        列幅を自動調整するか
     * @param drawBlockBorder        ブロック外枠に罫線を引くか
     * @param blankRowsBetweenBlocks ブロック間の空行数（0 以上）
     * @throws IllegalArgumentException 空行数が負の場合
     */
    public ExcelFormatConfig(short headerColorIndex, short markerColumnColorIndex,
                             boolean autoColumnWidth, boolean drawBlockBorder, int blankRowsBetweenBlocks) {
        if (blankRowsBetweenBlocks < 0) {
            throw new IllegalArgumentException(
                    "blankRowsBetweenBlocks must not be negative. but was [" + blankRowsBetweenBlocks + "]");
        }
        this.headerColorIndex = headerColorIndex;
        this.markerColumnColorIndex = markerColumnColorIndex;
        this.autoColumnWidth = autoColumnWidth;
        this.drawBlockBorder = drawBlockBorder;
        this.blankRowsBetweenBlocks = blankRowsBetweenBlocks;
    }

    /**
     * 見やすい既定値の設定を返す。
     *
     * @return 既定設定（ヘッダ＝淡い青／マーカー＝淡い橙／列幅自動／外枠罫線あり／ブロック間 1 空行）
     */
    public static ExcelFormatConfig defaults() {
        return new ExcelFormatConfig(
                IndexedColors.PALE_BLUE.getIndex(),
                IndexedColors.LIGHT_ORANGE.getIndex(),
                true, true, 1);
    }

    /**
     * 識別行・ヘッダ行の背景色を差し替えたコピーを返す。
     *
     * @param colorIndex 背景色（POI インデックスカラー）
     * @return 差し替え済みコピー
     */
    public ExcelFormatConfig withHeaderColor(short colorIndex) {
        return new ExcelFormatConfig(colorIndex, markerColumnColorIndex, autoColumnWidth,
                drawBlockBorder, blankRowsBetweenBlocks);
    }

    /**
     * マーカーカラムの背景色を差し替えたコピーを返す。
     *
     * @param colorIndex 背景色（POI インデックスカラー）
     * @return 差し替え済みコピー
     */
    public ExcelFormatConfig withMarkerColumnColor(short colorIndex) {
        return new ExcelFormatConfig(headerColorIndex, colorIndex, autoColumnWidth,
                drawBlockBorder, blankRowsBetweenBlocks);
    }

    /**
     * 列幅自動調整の有無を差し替えたコピーを返す。
     *
     * @param autoColumnWidth 自動調整するか
     * @return 差し替え済みコピー
     */
    public ExcelFormatConfig withAutoColumnWidth(boolean autoColumnWidth) {
        return new ExcelFormatConfig(headerColorIndex, markerColumnColorIndex, autoColumnWidth,
                drawBlockBorder, blankRowsBetweenBlocks);
    }

    /**
     * ブロック外枠罫線の有無を差し替えたコピーを返す。
     *
     * @param drawBlockBorder 罫線を引くか
     * @return 差し替え済みコピー
     */
    public ExcelFormatConfig withBlockBorder(boolean drawBlockBorder) {
        return new ExcelFormatConfig(headerColorIndex, markerColumnColorIndex, autoColumnWidth,
                drawBlockBorder, blankRowsBetweenBlocks);
    }

    /**
     * ブロック間空行数を差し替えたコピーを返す。
     *
     * @param blankRowsBetweenBlocks 空行数（0 以上）
     * @return 差し替え済みコピー
     */
    public ExcelFormatConfig withBlankRowsBetweenBlocks(int blankRowsBetweenBlocks) {
        return new ExcelFormatConfig(headerColorIndex, markerColumnColorIndex, autoColumnWidth,
                drawBlockBorder, blankRowsBetweenBlocks);
    }

    /** @return 識別行・ヘッダ行の背景色（POI インデックスカラー） */
    public short getHeaderColorIndex() {
        return headerColorIndex;
    }

    /** @return マーカーカラムの背景色（POI インデックスカラー） */
    public short getMarkerColumnColorIndex() {
        return markerColumnColorIndex;
    }

    /** @return 列幅を自動調整するか */
    public boolean isAutoColumnWidth() {
        return autoColumnWidth;
    }

    /** @return ブロック外枠に罫線を引くか */
    public boolean isDrawBlockBorder() {
        return drawBlockBorder;
    }

    /** @return ブロック間の空行数 */
    public int getBlankRowsBetweenBlocks() {
        return blankRowsBetweenBlocks;
    }
}
