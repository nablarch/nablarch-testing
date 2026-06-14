package nablarch.test.tool.converter.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.TestCoreFileAdapter;
import nablarch.test.core.file.TestCoreFileAdapter.FileView;
import nablarch.test.core.file.TestCoreFileAdapter.FragmentView;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.YamlTestCoreAdapter;
import nablarch.test.core.reader.yaml.YamlMessageBuilder.MessageContent;
import nablarch.test.core.reader.yaml.YamlSection;
import nablarch.test.tool.converter.TestDataFormatReader;
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
 * YAML（1 ファイル）を読み込み、中間モデル（{@link TestDataContainer}）へ写す IN リーダ。
 *
 * <p>
 * 独自の YAML 構造解析は持たない。本体の読み込みは {@link YamlTestCoreAdapter}
 * （本体 {@code reader} パッケージ相乗りのアダプタ）へ委譲し、本クラスは
 * {@link YamlTestCoreAdapter#loadRawMap(String, String) トップレベル Map}（{@link java.util.LinkedHashMap}
 * ＝YAML 記述順）を走査して既知セクションをブロックへ展開するオーケストレーションに徹する。
 * グループ絞り込み・fixed/variable 判定・FW_HEADER スキップ・送信系グループ／NO 扱い・テーブル名／
 * カラム名の大文字化・マーカー除外といった YAML 構造解釈はすべてアダプタ（本体器）側が担う。
 * </p>
 *
 * <p>
 * 各ブロックは「{@code adapter.read*} が返す本体器（構造の権威）」と「同じ Map から取る原文」を
 * 突き合わせて組み立てる。Excel 経路（{@code XlsFormatReader}）が器の正規化を<b>生行</b>から復元するのに対し、
 * YAML 経路は器が正規化する値（カラム名の大文字化・長さ省略・型表記）の原文を<b>{@code loadRawMap} の Map</b>から
 * 復元する（設計書 §共通「器が正規化する値の原文復元」＝Excel=生行／YAML=YamlLoader Map）。
 * </p>
 *
 * <p>
 * YAML は 1 ファイル＝1 読み込み単位。1 つの {@link TestDataSection} を持つ {@link TestDataContainer} を返す
 * （コンテナ名・セクション名ともリソース名）。
 * </p>
 *
 * @author kiyobot
 */
public class YamlFormatReader implements TestDataFormatReader {

    /** 本体再利用のためのアダプタ */
    private final YamlTestCoreAdapter adapter;

    /**
     * 本番用コンストラクタ。実 YAML を読む {@link YamlTestCoreAdapter} を構成する。
     */
    public YamlFormatReader() {
        this(new YamlTestCoreAdapter());
    }

    /**
     * アダプタを注入するコンストラクタ（主にテスト用）。
     *
     * @param adapter 本体再利用のためのアダプタ
     */
    YamlFormatReader(YamlTestCoreAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * YAML の 1 ファイルを読み、1 つの {@link TestDataSection} を持つ {@link TestDataContainer} を返す。
     * トップレベルキーを記述順に走査し、既知セクションのみブロックへ展開する（未知キーは無視）。
     * </p>
     */
    @Override
    public TestDataContainer read(String basePath, String resourceName) {
        Map<String, Object> yaml = adapter.loadRawMap(basePath, resourceName);
        List<TestDataBlock> blocks = new ArrayList<TestDataBlock>();
        for (String key : yaml.keySet()) {
            addBlocksForSection(basePath, resourceName, yaml, key, blocks);
        }
        TestDataSection section = new TestDataSection(resourceName, blocks);
        return new TestDataContainer(resourceName, Collections.singletonList(section));
    }

    /**
     * 1 セクションキーに対応するブロック群を {@code blocks} へ追加する。未知キーは何もしない。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param sectionKey   セクションキー
     * @param blocks       追加先
     */
    private void addBlocksForSection(String basePath, String resourceName, Map<String, Object> yaml,
                                     String sectionKey, List<TestDataBlock> blocks) {
        if (YamlSection.KEY_SETUP_TABLES.equals(sectionKey)) {
            addTableBlocks(basePath, resourceName, yaml, sectionKey, DataType.SETUP_TABLE_DATA, blocks);
        } else if (YamlSection.KEY_EXPECTED_TABLES.equals(sectionKey)) {
            addTableBlocks(basePath, resourceName, yaml, sectionKey, DataType.EXPECTED_TABLE_DATA, blocks);
        } else if (YamlSection.KEY_EXPECTED_COMPLETE_TABLES.equals(sectionKey)) {
            addTableBlocks(basePath, resourceName, yaml, sectionKey, DataType.EXPECTED_COMPLETED, blocks);
        } else if (YamlSection.KEY_LIST_MAPS.equals(sectionKey)) {
            addListMapBlocks(basePath, resourceName, yaml, blocks);
        } else if (YamlSection.KEY_SETUP_FILES.equals(sectionKey)) {
            addFileBlocks(basePath, resourceName, yaml, sectionKey, true, blocks);
        } else if (YamlSection.KEY_EXPECTED_FILES.equals(sectionKey)) {
            addFileBlocks(basePath, resourceName, yaml, sectionKey, false, blocks);
        } else if (YamlSection.KEY_MESSAGES.equals(sectionKey)) {
            addMessageBlocks(basePath, resourceName, yaml, blocks);
        } else if (YamlSection.KEY_EXPECTED_REQUEST_HEADER_MESSAGES.equals(sectionKey)) {
            addSendSyncBlocks(basePath, resourceName, yaml, sectionKey,
                    DataType.EXPECTED_REQUEST_HEADER_MESSAGES, blocks);
        } else if (YamlSection.KEY_EXPECTED_REQUEST_BODY_MESSAGES.equals(sectionKey)) {
            addSendSyncBlocks(basePath, resourceName, yaml, sectionKey,
                    DataType.EXPECTED_REQUEST_BODY_MESSAGES, blocks);
        } else if (YamlSection.KEY_RESPONSE_HEADER_MESSAGES.equals(sectionKey)) {
            addSendSyncBlocks(basePath, resourceName, yaml, sectionKey,
                    DataType.RESPONSE_HEADER_MESSAGES, blocks);
        } else if (YamlSection.KEY_RESPONSE_BODY_MESSAGES.equals(sectionKey)) {
            addSendSyncBlocks(basePath, resourceName, yaml, sectionKey,
                    DataType.RESPONSE_BODY_MESSAGES, blocks);
        }
        // 未知キーは無視。
    }

    // ------------------------------------------------------------------------
    // テーブル系（SETUP_TABLE_DATA／EXPECTED_TABLE_DATA／EXPECTED_COMPLETED）
    // ------------------------------------------------------------------------

    /**
     * テーブル系ブロックを写す。原文は不要（器のみ。Excel と同一）。器のテーブル名・カラム名は大文字化済み、
     * マーカーカラムは除外済みで、値も器の保持値（記法のまま）をそのまま用いる。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param sectionKey   セクションキー
     * @param type         データタイプ（テーブル系）
     * @param blocks       追加先
     */
    private void addTableBlocks(String basePath, String resourceName, Map<String, Object> yaml,
                               String sectionKey, DataType type, List<TestDataBlock> blocks) {
        for (String formattedGroup : formattedGroupsInOrder(yaml, sectionKey)) {
            List<TableData> tables = adapter.readTables(basePath, resourceName, formattedGroup, type);
            for (TableData table : tables) {
                String[] columns = table.getColumnNames();
                List<List<String>> rows = new ArrayList<List<String>>(table.size());
                for (int r = 0; r < table.size(); r++) {
                    List<String> row = new ArrayList<String>(columns.length);
                    for (String column : columns) {
                        Object value = table.getValue(r, column);
                        row.add(value == null ? null : value.toString());
                    }
                    rows.add(row);
                }
                blocks.add(new TableDataBlock(type, formattedGroup, table.getTableName(),
                        Arrays.asList(columns), rows));
            }
        }
    }

    // ------------------------------------------------------------------------
    // LIST_MAP
    // ------------------------------------------------------------------------

    /**
     * LIST_MAP ブロックを写す。器（{@code List<Map>}・マーカー除外・記法のまま）を行値の権威とし、
     * カラムの並びだけ Map（YAML 記述順）から復元する。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param blocks       追加先
     */
    private void addListMapBlocks(String basePath, String resourceName, Map<String, Object> yaml,
                                  List<TestDataBlock> blocks) {
        for (Object entryObj : YamlSection.getList(yaml, YamlSection.KEY_LIST_MAPS)) {
            Map<String, Object> entry = YamlSection.castMap(entryObj);
            String id = YamlSection.toStr(entry.get(YamlSection.FIELD_ID));
            List<Map<String, String>> mapRows = adapter.readListMap(basePath, resourceName, id);
            List<String> orderedColumns = nonMarkerColumns(entry);
            List<List<String>> rows = new ArrayList<List<String>>(mapRows.size());
            for (Map<String, String> mapRow : mapRows) {
                List<String> row = new ArrayList<String>(orderedColumns.size());
                for (String column : orderedColumns) {
                    row.add(mapRow.get(column));
                }
                rows.add(row);
            }
            blocks.add(new ListMapBlock(formatGroup(entry), id, orderedColumns, rows));
        }
    }

    // ------------------------------------------------------------------------
    // ファイル系（SETUP_FIXED／EXPECTED_FIXED／SETUP_VARIABLE／EXPECTED_VARIABLE）
    // ------------------------------------------------------------------------

    /**
     * ファイル系ブロックを写す。器（{@link DataFile}・グループ絞り込み済み・fixed/variable 混在・Map 順）と
     * 当該グループの Map エントリ列は 1:1 同順のため zip して原文（レコード種別・フィールド名／型／長さ・値の並び）を復元する。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param sectionKey   セクションキー（{@code setup_files}／{@code expected_files}）
     * @param setup        セットアップ系なら真、期待値系なら偽
     * @param blocks       追加先
     */
    private void addFileBlocks(String basePath, String resourceName, Map<String, Object> yaml,
                              String sectionKey, boolean setup, List<TestDataBlock> blocks) {
        // 代表データタイプはセクション解決（固定長／可変長で同一セクション）にのみ用いる。
        DataType representativeType = setup ? DataType.SETUP_FIXED : DataType.EXPECTED_FIXED;
        for (String formattedGroup : formattedGroupsInOrder(yaml, sectionKey)) {
            List<DataFile> files = adapter.readFiles(basePath, resourceName, formattedGroup, representativeType);
            List<Map<String, Object>> entries = entriesForFormattedGroup(yaml, sectionKey, formattedGroup);
            requireSameSize(files.size(), entries.size(), sectionKey, formattedGroup);
            for (int i = 0; i < files.size(); i++) {
                DataFile file = files.get(i);
                Map<String, Object> entry = entries.get(i);
                FileView view = TestCoreFileAdapter.read(file);
                FileDataBlock.FileType fileType = file instanceof FixedLengthFile
                        ? FileDataBlock.FileType.FIXED : FileDataBlock.FileType.VARIABLE;
                blocks.add(new FileDataBlock(fileDataType(setup, fileType), formattedGroup, view.getPath(),
                        fileType, toStringDirectives(view.getDirectives()),
                        toRecordLayouts(view, records(entry))));
            }
        }
    }

    // ------------------------------------------------------------------------
    // MESSAGE
    // ------------------------------------------------------------------------

    /**
     * MESSAGE ブロックを写す。本文（固定長ファイルの器）のレコードレイアウトと FW 制御ヘッダ（原文・文字列化済み）を持つ。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param blocks       追加先
     */
    private void addMessageBlocks(String basePath, String resourceName, Map<String, Object> yaml,
                                  List<TestDataBlock> blocks) {
        for (Object entryObj : YamlSection.getList(yaml, YamlSection.KEY_MESSAGES)) {
            Map<String, Object> entry = YamlSection.castMap(entryObj);
            String id = YamlSection.toStr(entry.get(YamlSection.FIELD_ID));
            MessageContent content = adapter.readMessage(basePath, resourceName, id);
            if (content == null) {
                continue;
            }
            FileView view = TestCoreFileAdapter.read(content.getBody());
            blocks.add(new MessageDataBlock(DataType.MESSAGE, "", id,
                    toStringDirectives(view.getDirectives()),
                    new LinkedHashMap<String, String>(content.getFwHeader()),
                    toRecordLayouts(view, recordsWithoutFwHeader(entry))));
        }
    }

    // ------------------------------------------------------------------------
    // 送信同期メッセージ（要求/応答電文 4 種）
    // ------------------------------------------------------------------------

    /**
     * 送信系ブロックを写す。グループは生値で一致（{@code group_id} 必須）させ、器（本文 {@link FixedLengthFile} 群・Map 順）と
     * 当該グループの Map エントリ列を zip する。FW 制御ヘッダは送信系では常に空。中間グループ ID は整形（{@code "[xxx]"}）して
     * Excel 中間と対称にする（マッチは生値・格納は整形）。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param yaml         トップレベル Map
     * @param sectionKey   セクションキー（送信系 4 種）
     * @param type         データタイプ（送信系 4 種）
     * @param blocks       追加先
     */
    private void addSendSyncBlocks(String basePath, String resourceName, Map<String, Object> yaml,
                                  String sectionKey, DataType type, List<TestDataBlock> blocks) {
        for (String rawGroup : rawGroupsInOrder(yaml, sectionKey)) {
            List<FixedLengthFile> bodies = adapter.readSendSyncMessages(basePath, resourceName, rawGroup, type);
            List<Map<String, Object>> entries = entriesForRawGroup(yaml, sectionKey, rawGroup);
            requireSameSize(bodies.size(), entries.size(), sectionKey, rawGroup);
            String formattedGroup = "[" + rawGroup + "]";
            for (int i = 0; i < bodies.size(); i++) {
                FixedLengthFile body = bodies.get(i);
                Map<String, Object> entry = entries.get(i);
                FileView view = TestCoreFileAdapter.read(body);
                blocks.add(new MessageDataBlock(type, formattedGroup, body.getPath(),
                        toStringDirectives(view.getDirectives()),
                        new LinkedHashMap<String, String>(),
                        toRecordLayouts(view, recordsWithoutFwHeader(entry))));
            }
        }
    }

    // ------------------------------------------------------------------------
    // 原文復元の共通処理
    // ------------------------------------------------------------------------

    /**
     * 器のビュー（断片構造）と整列済みの原文レコード Map から、レコードレイアウト群を組み立てる。
     * <p>
     * フィールド定義（名称・型・長さ）は Map 原文を正とする（器は型表記をフレームワーク表記へ・長さ省略を
     * 実バイト長へ正規化するため）。レコード種別も Map 原文（{@code null} 可）を用いる。データ行は器の保持値
     * （記法のまま）を断片のフィールド名順に positional 化する。
     * </p>
     * <p>
     * 器の断片数と原文レコード数が一致しない（対応が破綻している）場合は、誤った原文を静かに充填せず
     * 即座に失敗させる（設計書 §共通の fail-fast 思想）。
     * </p>
     *
     * @param view           器のビュー
     * @param alignedRecords 器の断片と整列済みの原文レコード Map（FILE は全件／MESSAGE・送信系は FW_HEADER 除外済み）
     * @return レコードレイアウト群
     */
    private List<RecordLayout> toRecordLayouts(FileView view, List<Map<String, Object>> alignedRecords) {
        List<FragmentView> fragments = view.getFragments();
        if (fragments.size() != alignedRecords.size()) {
            throw new IllegalStateException(
                    "器の断片構造と原文レコードが不整合です。fragments=" + fragments.size()
                            + ", records=" + alignedRecords.size());
        }
        List<RecordLayout> records = new ArrayList<RecordLayout>(fragments.size());
        for (int i = 0; i < fragments.size(); i++) {
            FragmentView fragment = fragments.get(i);
            Map<String, Object> record = alignedRecords.get(i);
            String recordType = YamlSection.toStr(record.get(YamlSection.FIELD_RECORD_TYPE));
            List<FieldDef> fields = toFieldDefs(record);
            List<String> names = fragment.getNames();
            List<List<String>> rows = new ArrayList<List<String>>(fragment.getValues().size());
            for (Map<String, String> valueMap : fragment.getValues()) {
                List<String> row = new ArrayList<String>(names.size());
                for (String name : names) {
                    row.add(valueMap.get(name));
                }
                rows.add(row);
            }
            records.add(new RecordLayout(recordType, fields, rows));
        }
        return records;
    }

    /**
     * 原文レコード Map の {@code fields} から、記法を加工しないフィールド定義群を組み立てる。
     *
     * @param record 原文レコード Map
     * @return フィールド定義群（記述順）
     */
    private static List<FieldDef> toFieldDefs(Map<String, Object> record) {
        List<Object> fieldList = YamlSection.getList(record, YamlSection.FIELD_FIELDS);
        List<FieldDef> fields = new ArrayList<FieldDef>(fieldList.size());
        for (Object fieldObj : fieldList) {
            Map<String, Object> field = YamlSection.castMap(fieldObj);
            fields.add(new FieldDef(
                    YamlSection.toStr(field.get(YamlSection.FIELD_NAME)),
                    YamlSection.toStr(field.get(YamlSection.FIELD_TYPE)),
                    YamlSection.toStr(field.get(YamlSection.FIELD_LENGTH))));
        }
        return fields;
    }

    /**
     * エントリの {@code records} を Map のリストとして取り出す（全件・スキップなし）。
     *
     * @param entry エントリ Map
     * @return レコード Map のリスト
     */
    private static List<Map<String, Object>> records(Map<String, Object> entry) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object recordObj : YamlSection.getList(entry, YamlSection.FIELD_RECORDS)) {
            result.add(YamlSection.castMap(recordObj));
        }
        return result;
    }

    /**
     * エントリの {@code records} を Map のリストとして取り出し、{@code record_type} が {@code FW_HEADER} の
     * レコードを除外する（メッセージ系で器の断片と整列させるため。器は FW_HEADER 断片を生成しない）。
     *
     * @param entry エントリ Map
     * @return FW_HEADER を除いたレコード Map のリスト
     */
    private static List<Map<String, Object>> recordsWithoutFwHeader(Map<String, Object> entry) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object recordObj : YamlSection.getList(entry, YamlSection.FIELD_RECORDS)) {
            Map<String, Object> record = YamlSection.castMap(recordObj);
            if (YamlSection.FW_HEADER_RECORD_TYPE.equals(YamlSection.toStr(record.get(YamlSection.FIELD_RECORD_TYPE)))) {
                continue;
            }
            result.add(record);
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // グループ列挙・整列
    // ------------------------------------------------------------------------

    /**
     * セクション内エントリの整形済みグループ ID を初出順で列挙する（重複排除）。
     *
     * @param yaml       トップレベル Map
     * @param sectionKey セクションキー
     * @return 整形済みグループ ID のリスト
     */
    private static List<String> formattedGroupsInOrder(Map<String, Object> yaml, String sectionKey) {
        List<String> groups = new ArrayList<String>();
        for (Object entryObj : YamlSection.getList(yaml, sectionKey)) {
            String group = formatGroup(YamlSection.castMap(entryObj));
            if (!groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
    }

    /**
     * 送信系セクション内エントリの生グループ ID（{@code group_id} 非 null のもの）を初出順で列挙する（重複排除）。
     *
     * @param yaml       トップレベル Map
     * @param sectionKey セクションキー
     * @return 生グループ ID のリスト
     */
    private static List<String> rawGroupsInOrder(Map<String, Object> yaml, String sectionKey) {
        List<String> groups = new ArrayList<String>();
        for (Object entryObj : YamlSection.getList(yaml, sectionKey)) {
            String group = YamlSection.toStr(YamlSection.castMap(entryObj).get(YamlSection.FIELD_GROUP_ID));
            if (group != null && !groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
    }

    /**
     * 指定の整形済みグループに属するエントリを記述順で集める。器（同一グループ絞り込み済み）と 1:1 同順で対応する。
     *
     * @param yaml           トップレベル Map
     * @param sectionKey     セクションキー
     * @param formattedGroup 整形済みグループ ID
     * @return エントリ Map のリスト
     */
    private static List<Map<String, Object>> entriesForFormattedGroup(Map<String, Object> yaml, String sectionKey,
                                                                      String formattedGroup) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object entryObj : YamlSection.getList(yaml, sectionKey)) {
            Map<String, Object> entry = YamlSection.castMap(entryObj);
            if (formatGroup(entry).equals(formattedGroup)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 指定の生グループに属するエントリ（{@code group_id} が生値で一致）を記述順で集める。送信系の器と 1:1 同順で対応する。
     *
     * @param yaml       トップレベル Map
     * @param sectionKey セクションキー
     * @param rawGroup   生グループ ID
     * @return エントリ Map のリスト
     */
    private static List<Map<String, Object>> entriesForRawGroup(Map<String, Object> yaml, String sectionKey,
                                                                String rawGroup) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object entryObj : YamlSection.getList(yaml, sectionKey)) {
            Map<String, Object> entry = YamlSection.castMap(entryObj);
            if (rawGroup.equals(YamlSection.toStr(entry.get(YamlSection.FIELD_GROUP_ID)))) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * エントリの {@code group_id} を整形済みグループ ID（{@code "[xxx]"}／省略時は空文字）へ変換する。
     * 本体ビルダの {@code groupMatches} と同一規則。
     *
     * @param entry エントリ Map
     * @return 整形済みグループ ID
     */
    private static String formatGroup(Map<String, Object> entry) {
        String groupId = YamlSection.toStr(entry.get(YamlSection.FIELD_GROUP_ID));
        return groupId != null ? "[" + groupId + "]" : "";
    }

    /**
     * エントリ先頭行のキー（YAML 記述順）からマーカーカラム（{@code [COL]}）を除いたカラム名を返す。
     *
     * @param entry エントリ Map（{@code rows} を持つ）
     * @return 非マーカーカラム名（記述順）
     */
    private static List<String> nonMarkerColumns(Map<String, Object> entry) {
        List<String> columns = new ArrayList<String>();
        for (String column : YamlSection.resolveColumns(YamlSection.getList(entry, YamlSection.FIELD_ROWS))) {
            if (!YamlSection.isMarker(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    // ------------------------------------------------------------------------
    // その他ヘルパー
    // ------------------------------------------------------------------------

    /**
     * セットアップ／期待値と固定長／可変長から、ファイル系データタイプを決定する。
     *
     * @param setup    セットアップ系なら真
     * @param fileType 固定長／可変長
     * @return データタイプ
     */
    private static DataType fileDataType(boolean setup, FileDataBlock.FileType fileType) {
        if (fileType == FileDataBlock.FileType.FIXED) {
            return setup ? DataType.SETUP_FIXED : DataType.EXPECTED_FIXED;
        }
        return setup ? DataType.SETUP_VARIABLE : DataType.EXPECTED_VARIABLE;
    }

    /**
     * 本体ディレクティブ（{@code Map<String, Object>}）を文字列ディレクティブへ写す。
     * <p>
     * 本体器のディレクティブ値は型変換済み（{@code Charset}・整数等）で、順序も {@code HashMap} 由来で
     * 記述順を保たない。YAML 経路も Excel 経路と対称にこの器固有挙動を受容し、値は {@link Object#toString()} で
     * 文字列化する。null 値は（テーブル/LIST_MAP 経路と対称に）null のまま保持する。
     * </p>
     *
     * @param directives 本体ディレクティブ
     * @return 文字列ディレクティブ
     */
    private static Map<String, String> toStringDirectives(Map<String, Object> directives) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> entry : directives.entrySet()) {
            Object value = entry.getValue();
            result.put(entry.getKey(), value == null ? null : value.toString());
        }
        return result;
    }

    /**
     * 器の要素数と原文エントリ数が一致することを検証する。不一致は対応の破綻として即座に失敗させる。
     *
     * @param containerSize 器の要素数
     * @param entrySize     原文エントリ数
     * @param sectionKey    診断用：セクションキー
     * @param group         診断用：グループ ID
     */
    private static void requireSameSize(int containerSize, int entrySize, String sectionKey, String group) {
        if (containerSize != entrySize) {
            throw new IllegalStateException(
                    "器と原文エントリが不整合です。section=" + sectionKey + ", group=" + group
                            + ", containers=" + containerSize + ", entries=" + entrySize);
        }
    }
}
