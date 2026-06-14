package nablarch.test.tool.converter.yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.yaml.YamlSection;
import nablarch.test.tool.converter.TestDataFormatWriter;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;

/**
 * 中間モデル（{@link TestDataContainer}）を YAML へ書き出す OUT ライタ。
 *
 * <p>
 * {@link YamlFormatReader} と<b>記法対称</b>に直列化する。設計書 OUT 方針に従い、値はすべて
 * ダブルクォートで囲む（数値・null・前後空白を文字列として保つため）。ただし {@code null}（記述省略でなく
 * 明示的な NULL）はアンクォートの {@code null} とする。{@code ""}・{@code ${...}} は記法のまま保持する。
 * キーは原則クォートしない（YAML 特殊文字を含む場合のみクォートする）。
 * </p>
 *
 * <p>
 * セクションはブロックの {@link DataType} からセクションキーへ写し、初出順にグルーピングして出力する
 * （同一セクションキーの複数ブロックは 1 つのセクション配下へ記述順に並べる）。各ブロックの記法は
 * {@link YamlFormatReader} が読む形（テーブル＝{@code table:}＋行マップ／LIST_MAP＝{@code id:}＋列／
 * ファイル＝{@code path:}/{@code type:}/{@code directives:}/{@code records:}／メッセージ＝{@code id:}＋
 * {@code fw_header:}／送信系＝{@code group_id:} 必須）と一致させる。
 * </p>
 *
 * <p>
 * {@link DataType} からセクションキーへの対応は本ライタが保持する（本体の
 * {@link YamlSection#dataTypeToSectionKey(DataType)} は messaging 系専用で全種別を網羅しないため）。
 * </p>
 *
 * @author kiyobot
 */
public final class YamlFormatWriter implements TestDataFormatWriter {

    /** YAML 拡張子。 */
    private static final String YAML_EXTENSION = ".yaml";

    /**
     * {@inheritDoc}
     * <p>
     * コンテナの各セクションを {@code <basePath>/<セクション名>.yaml}（UTF-8）へ書き出す。
     * </p>
     */
    @Override
    public void write(TestDataContainer container, String basePath) {
        for (TestDataSection section : container.getSections()) {
            Path file = Paths.get(basePath, section.getName() + YAML_EXTENSION);
            try {
                Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(file, serialize(section).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("failed to write YAML: " + file, e);
            }
        }
    }

    /**
     * 1 セクションを YAML テキストへ直列化する。
     *
     * @param section セクション
     * @return YAML テキスト（ブロックが無ければ空文字）
     */
    public String serialize(TestDataSection section) {
        Map<String, List<TestDataBlock>> bySection = new LinkedHashMap<String, List<TestDataBlock>>();
        for (TestDataBlock block : section.getBlocks()) {
            String key = sectionKey(block.getDataType());
            List<TestDataBlock> list = bySection.get(key);
            if (list == null) {
                list = new ArrayList<TestDataBlock>();
                bySection.put(key, list);
            }
            list.add(block);
        }

        StringBuilder sb = new StringBuilder();
        boolean firstSection = true;
        for (Map.Entry<String, List<TestDataBlock>> entry : bySection.entrySet()) {
            if (!firstSection) {
                sb.append('\n');
            }
            firstSection = false;
            sb.append(entry.getKey()).append(":\n");
            for (TestDataBlock block : entry.getValue()) {
                emitBlock(sb, block);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ブロック種別ごとの出力
    // ------------------------------------------------------------------------

    /**
     * ブロックを種別に応じて出力する。
     *
     * @param sb    出力先
     * @param block ブロック
     */
    private void emitBlock(StringBuilder sb, TestDataBlock block) {
        if (block instanceof TableDataBlock) {
            emitTable(sb, (TableDataBlock) block);
        } else if (block instanceof ListMapBlock) {
            emitListMap(sb, (ListMapBlock) block);
        } else if (block instanceof FileDataBlock) {
            emitFile(sb, (FileDataBlock) block);
        } else if (block instanceof MessageDataBlock) {
            emitMessage(sb, (MessageDataBlock) block);
        } else {
            throw new IllegalArgumentException("unsupported block: " + block.getClass().getName());
        }
    }

    /**
     * テーブル系ブロックを出力する（{@code table:}＋行マップ）。
     *
     * @param sb    出力先
     * @param block テーブルブロック
     */
    private void emitTable(StringBuilder sb, TableDataBlock block) {
        Seq entry = new Seq(sb, 1);
        emitGroupId(entry, block.getGroupId());
        entry.prop("table", block.getIdentifier());
        emitMapRows(sb, entry, block.getColumnNames(), block.getRows());
    }

    /**
     * LIST_MAP ブロックを出力する（{@code id:}＋行マップ）。
     *
     * @param sb    出力先
     * @param block LIST_MAP ブロック
     */
    private void emitListMap(StringBuilder sb, ListMapBlock block) {
        Seq entry = new Seq(sb, 1);
        emitGroupId(entry, block.getGroupId());
        entry.prop("id", block.getIdentifier());
        emitMapRows(sb, entry, block.getColumnNames(), block.getRows());
    }

    /**
     * ファイル系ブロックを出力する（{@code path:}/{@code type:}/{@code directives:}/{@code records:}）。
     *
     * @param sb    出力先
     * @param block ファイルブロック
     */
    private void emitFile(StringBuilder sb, FileDataBlock block) {
        Seq entry = new Seq(sb, 1);
        emitGroupId(entry, block.getGroupId());
        entry.prop("path", block.getIdentifier());
        entry.prop("type", block.getFileType() == FileDataBlock.FileType.FIXED ? "fixed" : "variable");
        emitMap(sb, entry, "directives", block.getDirectives());
        emitRecords(sb, entry, block.getRecords());
    }

    /**
     * メッセージ系ブロック（MESSAGE／送信系 4 種）を出力する。
     * <p>
     * グループ ID が非空なら {@code group_id:}（送信系）、FW 制御ヘッダが非空なら {@code fw_header:}（MESSAGE）を
     * 出力する。いずれもブロックの内容に応じて自然に切り替わる。
     * </p>
     *
     * @param sb    出力先
     * @param block メッセージブロック
     */
    private void emitMessage(StringBuilder sb, MessageDataBlock block) {
        Seq entry = new Seq(sb, 1);
        emitGroupId(entry, block.getGroupId());
        entry.prop("id", block.getIdentifier());
        emitMap(sb, entry, "directives", block.getDirectives());
        emitMap(sb, entry, "fw_header", block.getFwHeaderFields());
        emitRecords(sb, entry, block.getRecords());
    }

    // ------------------------------------------------------------------------
    // 構造要素の出力
    // ------------------------------------------------------------------------

    /**
     * グループ ID を出力する（整形済み {@code [xxx]} を生値へ戻す。空なら出力しない）。
     *
     * @param entry   エントリ
     * @param groupId 整形済みグループ ID
     */
    private static void emitGroupId(Seq entry, String groupId) {
        String raw = rawGroup(groupId);
        if (raw != null) {
            entry.prop("group_id", raw);
        }
    }

    /**
     * テーブル／LIST_MAP の行を「カラム名→値」のマップ列として出力する。
     *
     * @param sb      出力先
     * @param parent  親エントリ
     * @param columns カラム名（記述順）
     * @param rows    行（カラムと同順）
     */
    private void emitMapRows(StringBuilder sb, Seq parent, List<String> columns, List<List<String>> rows) {
        if (rows.isEmpty()) {
            parent.line(key("rows") + ": []");
            return;
        }
        parent.header("rows");
        int rowLevel = parent.level + 2;
        for (List<String> row : rows) {
            Seq item = new Seq(sb, rowLevel);
            if (columns.isEmpty()) {
                item.line("{}");
                continue;
            }
            for (int i = 0; i < columns.size(); i++) {
                item.prop(columns.get(i), i < row.size() ? row.get(i) : null);
            }
        }
    }

    /**
     * レコードレイアウト群を出力する（空なら {@code records:} 自体を出力しない）。
     *
     * @param sb      出力先
     * @param parent  親エントリ
     * @param records レコードレイアウト群
     */
    private void emitRecords(StringBuilder sb, Seq parent, List<RecordLayout> records) {
        if (records.isEmpty()) {
            return;
        }
        parent.header("records");
        int recordLevel = parent.level + 2;
        for (RecordLayout record : records) {
            Seq item = new Seq(sb, recordLevel);
            if (record.getRecordType() != null) {
                item.prop("record_type", record.getRecordType());
            }
            emitFlowList(sb, item, "fields", record.getFields(), YamlFormatWriter::fieldFlow);
            emitFlowList(sb, item, "rows", record.getRows(), YamlFormatWriter::rowFlow);
        }
    }

    /**
     * 文字列マップ（directives／fw_header）をブロックマップとして出力する（空なら出力しない）。
     *
     * @param sb      出力先
     * @param parent  親エントリ
     * @param keyName キー名
     * @param map     マップ（記述順）
     */
    private void emitMap(StringBuilder sb, Seq parent, String keyName, Map<String, String> map) {
        if (map.isEmpty()) {
            return;
        }
        parent.header(keyName);
        String indent = ind(parent.level + 2);
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append(indent).append(key(e.getKey())).append(": ").append(q(e.getValue())).append('\n');
        }
    }

    /**
     * フロー記法のリスト（{@code fields:}＝フロー map／{@code rows:}＝フロー list）を出力する。
     * 空なら {@code key: []} を 1 行で出力する。
     *
     * @param sb       出力先
     * @param parent   親エントリ
     * @param keyName  キー名
     * @param items    要素
     * @param renderer 各要素のフロー記法レンダラ
     * @param <T>      要素型
     */
    private <T> void emitFlowList(StringBuilder sb, Seq parent, String keyName, List<T> items,
                                  Function<T, String> renderer) {
        if (items.isEmpty()) {
            parent.line(key(keyName) + ": []");
            return;
        }
        parent.header(keyName);
        int itemLevel = parent.level + 2;
        for (T item : items) {
            new Seq(sb, itemLevel).line(renderer.apply(item));
        }
    }

    /**
     * フィールド定義をフロー map 記法へ整形する（{@code type}／{@code length} は null なら省略）。
     *
     * @param field フィールド定義
     * @return フロー map 文字列
     */
    private static String fieldFlow(FieldDef field) {
        StringBuilder b = new StringBuilder("{");
        b.append(key("name")).append(": ").append(q(field.getName()));
        if (field.getType() != null) {
            b.append(", ").append(key("type")).append(": ").append(q(field.getType()));
        }
        if (field.getLength() != null) {
            b.append(", ").append(key("length")).append(": ").append(q(field.getLength()));
        }
        return b.append('}').toString();
    }

    /**
     * データ行をフロー list 記法へ整形する。
     *
     * @param values 値（フィールド順）
     * @return フロー list 文字列
     */
    private static String rowFlow(List<String> values) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(q(values.get(i)));
        }
        return b.append(']').toString();
    }

    // ------------------------------------------------------------------------
    // スカラ・キー・インデント
    // ------------------------------------------------------------------------

    /**
     * 値をダブルクォートで囲んだ YAML スカラへ整形する。{@code null} はアンクォートの {@code null}。
     *
     * @param value 値（{@code null} 可）
     * @return YAML スカラ文字列
     */
    private static String q(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder(value.length() + 2);
        b.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"':  b.append("\\\""); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\x%02X", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }

    /**
     * キーを整形する。YAML 特殊文字・空白・空文字を含む場合のみダブルクォートで囲む。
     *
     * @param k キー
     * @return キー文字列
     */
    private static String key(String k) {
        return isPlainSafeKey(k) ? k : q(k);
    }

    /**
     * キーをクォートなしで安全に書けるか判定する。
     *
     * @param k キー
     * @return クォート不要なら真
     */
    private static boolean isPlainSafeKey(String k) {
        if (k.isEmpty()) {
            return false;
        }
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            if (c < 0x20 || c == ' ' || "\"'#:,[]{}&*!|>%@`?".indexOf(c) >= 0) {
                return false;
            }
        }
        return "-?:".indexOf(k.charAt(0)) < 0;
    }

    /**
     * インデント（1 レベル＝半角スペース 2 個）を返す。
     *
     * @param level レベル
     * @return インデント文字列
     */
    private static String ind(int level) {
        StringBuilder b = new StringBuilder(level * 2);
        for (int i = 0; i < level; i++) {
            b.append("  ");
        }
        return b.toString();
    }

    /**
     * 整形済みグループ ID（{@code [xxx]}）を生値へ戻す。空文字（グループ省略）は出力対象外として {@code null} を返す。
     * 中間モデルの契約上 {@code groupId} は非 null（省略時は空文字）。
     *
     * @param groupId 整形済みグループ ID（非 null）
     * @return 生グループ値（出力不要なら {@code null}）
     */
    private static String rawGroup(String groupId) {
        if (groupId.isEmpty()) {
            return null;
        }
        int last = groupId.length() - 1;
        if (groupId.charAt(0) == '[' && groupId.charAt(last) == ']') {
            return groupId.substring(1, last);
        }
        return groupId;
    }

    /**
     * {@link DataType} を YAML セクションキーへ写す（全種別）。
     *
     * @param type データ種別
     * @return セクションキー
     */
    private static String sectionKey(DataType type) {
        switch (type) {
            case SETUP_TABLE_DATA:                 return YamlSection.KEY_SETUP_TABLES;
            case EXPECTED_TABLE_DATA:              return YamlSection.KEY_EXPECTED_TABLES;
            case EXPECTED_COMPLETED:               return YamlSection.KEY_EXPECTED_COMPLETE_TABLES;
            case LIST_MAP:                         return YamlSection.KEY_LIST_MAPS;
            case SETUP_FIXED:
            case SETUP_VARIABLE:                   return YamlSection.KEY_SETUP_FILES;
            case EXPECTED_FIXED:
            case EXPECTED_VARIABLE:                return YamlSection.KEY_EXPECTED_FILES;
            case MESSAGE:                          return YamlSection.KEY_MESSAGES;
            case EXPECTED_REQUEST_HEADER_MESSAGES: return YamlSection.KEY_EXPECTED_REQUEST_HEADER_MESSAGES;
            case EXPECTED_REQUEST_BODY_MESSAGES:   return YamlSection.KEY_EXPECTED_REQUEST_BODY_MESSAGES;
            case RESPONSE_HEADER_MESSAGES:         return YamlSection.KEY_RESPONSE_HEADER_MESSAGES;
            case RESPONSE_BODY_MESSAGES:           return YamlSection.KEY_RESPONSE_BODY_MESSAGES;
            default:
                throw new IllegalArgumentException("unsupported DataType: " + type);
        }
    }

    /**
     * YAML のシーケンス項目（マップ）を「先頭プロパティを {@code - } 行に載せ、以降を字下げ」して出力する補助。
     * <p>
     * 1 項目は自身のレベル {@code level} を持ち、{@code - } は {@code ind(level)} に、各プロパティの内容は
     * {@code ind(level+1)} に揃う。
     * </p>
     */
    private static final class Seq {

        /** 出力先。 */
        private final StringBuilder sb;

        /** 項目のレベル（{@code - } の字下げ）。 */
        private final int level;

        /** 先頭行（{@code - } を載せる行）が未出力なら真。 */
        private boolean dashPending = true;

        /**
         * @param sb    出力先
         * @param level 項目のレベル
         */
        Seq(StringBuilder sb, int level) {
            this.sb = sb;
            this.level = level;
        }

        /**
         * {@code key: value} のプロパティ行を出力する。
         *
         * @param k キー
         * @param v 値（クォートして出力。{@code null} は {@code null}）
         */
        void prop(String k, String v) {
            line(key(k) + ": " + q(v));
        }

        /**
         * {@code key:}（値は後続の字下げで続く）のヘッダ行を出力する。
         *
         * @param k キー
         */
        void header(String k) {
            line(key(k) + ":");
        }

        /**
         * 1 行を出力する。先頭行なら {@code - } を載せ、以降は {@code ind(level+1)} に揃える。
         *
         * @param content 行の内容
         */
        void line(String content) {
            if (dashPending) {
                sb.append(ind(level)).append("- ").append(content).append('\n');
                dashPending = false;
            } else {
                sb.append(ind(level + 1)).append(content).append('\n');
            }
        }
    }
}
