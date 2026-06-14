package nablarch.test.tool.converter.xls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.PoiXlsReader;
import nablarch.test.core.reader.TestDataParserAdapter;
import nablarch.test.core.reader.TestDataParserAdapter.BlockHeader;
import nablarch.test.core.reader.TestDataParserAdapter.MessageData;
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
 * Excel（1 シート）を読み込み、中間モデル（{@link TestDataContainer}）へ写す IN リーダ。
 *
 * <p>
 * 独自の POI パース・構造解析は持たない。本体の読み込みは {@link TestDataParserAdapter}
 * （本体パッケージ相乗りのアダプタ）へ委譲し、本クラスは「どのブロックが存在するか」を
 * {@link TestDataParserAdapter#readHeaders(String, String)} で得て、各ブロックをデータタイプ別の
 * {@code read*} で取り出し、本体の器を中間モデルへ写すオーケストレーションに徹する。
 * 行のデータタイプ判定・マーカー解釈はすべてアダプタ（本体）側が担う。
 * </p>
 *
 * <p>
 * IN 値は記法のまま（未加工）で運ばれる（アダプタが空 interpreters で配線するため）。一方、
 * カラム名・テーブル名の大文字化やファイル型の本体シンボル化、ディレクティブの型変換などは
 * 本体器が固有に行う挙動であり、Excel 経路（設計書 判断 A）はこれを受容する。
 * </p>
 *
 * <p>
 * 同一データタイプ・同一グループの複数ブロックは本体 API が一括取得するため、ブロックの並びは
 * 「各 (データタイプ, グループ) を最初に検出した位置」にまとめて展開する。データタイプをまたぐ
 * 厳密な記述順は保たれない場合があるが、NTF はデータを (データタイプ, ID) で取得するため
 * ブロックの並び順は意味を持たない。
 * </p>
 *
 * @author kiyobot
 */
public class XlsFormatReader implements TestDataFormatReader {

    /** 本体再利用のためのアダプタ */
    private final TestDataParserAdapter adapter;

    /**
     * 本番用コンストラクタ。実 Excel を読む {@link PoiXlsReader} を注入したアダプタを構成する。
     */
    public XlsFormatReader() {
        this(new TestDataParserAdapter(new PoiXlsReader()));
    }

    /**
     * アダプタを注入するコンストラクタ（主にテスト用）。
     *
     * @param adapter 本体再利用のためのアダプタ
     */
    XlsFormatReader(TestDataParserAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Excel の 1 シートを読み、1 つの {@link TestDataSection} を持つ {@link TestDataContainer} を返す。
     * {@code resourceName} は {@code "ブック名/シート名"}（ブック名 → コンテナ名、シート名 → セクション名）。
     * </p>
     */
    @Override
    public TestDataContainer read(String basePath, String resourceName) {
        List<BlockHeader> headers = adapter.readHeaders(basePath, resourceName);
        List<TestDataBlock> blocks = new ArrayList<TestDataBlock>();
        Set<String> processed = new HashSet<String>();
        for (BlockHeader header : headers) {
            DataType type = header.getType();
            if (isTableType(type)) {
                if (processed.add(batchKey(type, header.getGroupId()))) {
                    blocks.addAll(readTableBlocks(basePath, resourceName, header.getGroupId(), type));
                }
            } else if (isFileType(type)) {
                if (processed.add(batchKey(type, header.getGroupId()))) {
                    blocks.addAll(readFileBlocks(basePath, resourceName, header.getGroupId(), type));
                }
            } else if (type == DataType.LIST_MAP) {
                if (processed.add(singleKey(type, header.getIdentifier()))) {
                    blocks.add(readListMapBlock(basePath, resourceName, header));
                }
            } else if (type == DataType.MESSAGE) {
                if (processed.add(singleKey(type, header.getIdentifier()))) {
                    TestDataBlock block = readMessageBlock(basePath, resourceName, header);
                    if (block != null) {
                        blocks.add(block);
                    }
                }
            }
            // それ以外（要求/応答電文 4 種）は #6 スコープ外（別タスクで対応）
        }
        TestDataSection section = new TestDataSection(sheetName(resourceName), blocks);
        return new TestDataContainer(bookName(resourceName), Collections.singletonList(section));
    }

    /**
     * テーブル系ブロック（指定の (データタイプ, グループ) に属する全テーブル）を写す。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param groupId      グループ ID
     * @param type         データタイプ（テーブル系）
     * @return テーブルデータブロック一覧
     */
    private List<TestDataBlock> readTableBlocks(String basePath, String resourceName, String groupId, DataType type) {
        List<TableData> tables = adapter.readTables(basePath, resourceName, groupId, type);
        List<TestDataBlock> result = new ArrayList<TestDataBlock>();
        for (TableData table : tables) {
            String[] columns = table.getColumnNames();
            List<String> columnNames = Arrays.asList(columns);
            List<List<String>> rows = new ArrayList<List<String>>();
            for (int r = 0; r < table.size(); r++) {
                List<String> row = new ArrayList<String>(columns.length);
                for (String column : columns) {
                    Object value = table.getValue(r, column);
                    row.add(value == null ? null : value.toString());
                }
                rows.add(row);
            }
            result.add(new TableDataBlock(type, groupId, table.getTableName(), columnNames, rows));
        }
        return result;
    }

    /**
     * LIST_MAP ブロックを写す。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param header       ブロックヘッダ
     * @return LIST_MAP ブロック
     */
    private TestDataBlock readListMapBlock(String basePath, String resourceName, BlockHeader header) {
        List<Map<String, String>> mapRows = adapter.readListMap(basePath, resourceName, header.getIdentifier());
        List<String> columnNames = new ArrayList<String>();
        for (Map<String, String> mapRow : mapRows) {
            for (String key : mapRow.keySet()) {
                if (!columnNames.contains(key)) {
                    columnNames.add(key);
                }
            }
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        for (Map<String, String> mapRow : mapRows) {
            List<String> row = new ArrayList<String>(columnNames.size());
            for (String column : columnNames) {
                row.add(mapRow.get(column));
            }
            rows.add(row);
        }
        return new ListMapBlock(header.getGroupId(), header.getIdentifier(), columnNames, rows);
    }

    /**
     * ファイル系ブロック（指定の (データタイプ, グループ) に属する全ファイル）を写す。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param groupId      グループ ID
     * @param type         データタイプ（ファイル系）
     * @return ファイルデータブロック一覧
     */
    private List<TestDataBlock> readFileBlocks(String basePath, String resourceName, String groupId, DataType type) {
        List<? extends DataFile> files = adapter.readFiles(basePath, resourceName, groupId, type);
        FileDataBlock.FileType fileType = isFixed(type) ? FileDataBlock.FileType.FIXED : FileDataBlock.FileType.VARIABLE;
        List<TestDataBlock> result = new ArrayList<TestDataBlock>();
        for (DataFile file : files) {
            result.add(new FileDataBlock(type, groupId, file.getPath(), fileType,
                    toStringDirectives(file.getDirectives()), toRecordLayouts(file.getAllFragments())));
        }
        return result;
    }

    /**
     * MESSAGE ブロックを写す。本文（固定長ファイル）のレコードレイアウトと FW 制御ヘッダを持つ。
     *
     * @param basePath     ディレクトリ
     * @param resourceName リソース名
     * @param header       ブロックヘッダ
     * @return MESSAGE ブロック。対象が存在しない場合は {@code null}
     */
    private TestDataBlock readMessageBlock(String basePath, String resourceName, BlockHeader header) {
        MessageData message = adapter.readMessage(basePath, resourceName, header.getIdentifier());
        if (message == null) {
            return null;
        }
        FixedLengthFile body = message.getBody();
        Map<String, String> fwHeaderFields = new LinkedHashMap<String, String>(message.getFwHeader());
        return new MessageDataBlock(DataType.MESSAGE, header.getGroupId(), header.getIdentifier(),
                toStringDirectives(body.getDirectives()), fwHeaderFields, toRecordLayouts(body.getAllFragments()));
    }

    /**
     * 本体フラグメント群をレコードレイアウト群へ写す。
     *
     * @param fragments フラグメント群
     * @return レコードレイアウト群
     */
    private List<RecordLayout> toRecordLayouts(List<DataFileFragment> fragments) {
        List<RecordLayout> records = new ArrayList<RecordLayout>();
        for (DataFileFragment fragment : fragments) {
            List<String> names = fragment.getNames();
            // 可変長ファイルは長さ行を持たず getLengths()/getTypes() が null になりうる。
            List<String> types = orEmpty(fragment.getTypes());
            List<String> lengths = orEmpty(fragment.getLengths());
            List<FieldDef> fields = new ArrayList<FieldDef>(names.size());
            for (int i = 0; i < names.size(); i++) {
                fields.add(new FieldDef(names.get(i),
                        i < types.size() ? types.get(i) : null,
                        i < lengths.size() ? lengths.get(i) : null));
            }
            List<List<String>> rows = new ArrayList<List<String>>();
            for (Map<String, String> valueMap : fragment.getValues()) {
                List<String> row = new ArrayList<String>(names.size());
                for (String name : names) {
                    row.add(valueMap.get(name));
                }
                rows.add(row);
            }
            records.add(new RecordLayout(fragment.getRecordType(), fields, rows));
        }
        return records;
    }

    /**
     * 本体ディレクティブ（{@code Map<String, Object>}）を文字列ディレクティブへ写す。
     * <p>
     * 本体器のディレクティブ値は型変換済み（{@code Charset}・enum・整数等）で、順序も
     * {@code HashMap} 由来で記述順を保たない。Excel 経路（判断 A）はこの器固有挙動を受容し、
     * 値は {@link String#valueOf(Object)} で文字列化する。
     * </p>
     *
     * @param directives 本体ディレクティブ
     * @return 文字列ディレクティブ
     */
    private Map<String, String> toStringDirectives(Map<String, Object> directives) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> entry : directives.entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * {@code null} のリストを空リストへ正規化する。
     *
     * @param list 対象（{@code null} 可）
     * @return {@code null} なら空リスト、さもなくばそのまま
     */
    private static List<String> orEmpty(List<String> list) {
        return list == null ? Collections.<String>emptyList() : list;
    }

    /**
     * 一括取得型（テーブル・ファイル）の重複排除キー。
     *
     * @param type    データタイプ
     * @param groupId グループ ID
     * @return キー
     */
    private static String batchKey(DataType type, String groupId) {
        return type.name() + ' ' + groupId;
    }

    /**
     * 単体取得型（LIST_MAP・MESSAGE）の重複排除キー。
     *
     * @param type       データタイプ
     * @param identifier 識別子
     * @return キー
     */
    private static String singleKey(DataType type, String identifier) {
        return type.name() + ' ' + identifier;
    }

    /**
     * テーブル系データタイプか判定する。
     *
     * @param type データタイプ
     * @return テーブル系なら真
     */
    private static boolean isTableType(DataType type) {
        return type == DataType.SETUP_TABLE_DATA
                || type == DataType.EXPECTED_TABLE_DATA
                || type == DataType.EXPECTED_COMPLETED;
    }

    /**
     * ファイル系データタイプか判定する。
     *
     * @param type データタイプ
     * @return ファイル系なら真
     */
    private static boolean isFileType(DataType type) {
        return isFixed(type)
                || type == DataType.SETUP_VARIABLE
                || type == DataType.EXPECTED_VARIABLE;
    }

    /**
     * 固定長ファイルのデータタイプか判定する。
     *
     * @param type データタイプ
     * @return 固定長なら真
     */
    private static boolean isFixed(DataType type) {
        return type == DataType.SETUP_FIXED || type == DataType.EXPECTED_FIXED;
    }

    /**
     * リソース名（{@code "ブック名/シート名"}）からブック名を取り出す。
     *
     * @param resourceName リソース名
     * @return ブック名（{@code '/'} が無ければリソース名全体）
     */
    private static String bookName(String resourceName) {
        int slash = resourceName.indexOf('/');
        return slash < 0 ? resourceName : resourceName.substring(0, slash);
    }

    /**
     * リソース名（{@code "ブック名/シート名"}）からシート名を取り出す。
     *
     * @param resourceName リソース名
     * @return シート名（{@code '/'} が無ければリソース名全体）
     */
    private static String sheetName(String resourceName) {
        int slash = resourceName.indexOf('/');
        return slash < 0 ? resourceName : resourceName.substring(slash + 1);
    }
}
