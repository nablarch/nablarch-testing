package nablarch.test.core.reader;

import nablarch.test.NablarchTestUtils;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * テストデータから{@link TableData}を解析するクラス。
 *
 * @author T.Kawasaki
 */
public class TableDataParser extends GroupDataParsingTemplate<List<TableData>> {

    /** 解析結果 */
    private List<TableData> result;

    /** {@link DbInfo} */
    private final DbInfo dbInfo;

    /** 値が省略された場合のデフォルト値 */
    private final DefaultValues defaultValues;

    /** データタイプ */
    private final DataType targetDataType;

    /** ヘッダー行(カラム名) */
    private HeaderLine header;

    /** 処理中のデータ */
    private TableData processing;

    /**
     * コンストラクタ。
     *
     * @param reader         リーダ
     * @param interpreters 解釈クラス
     * @param dbInfo       {@link DbInfo}
     * @param defaultValues 値が省略された場合のデフォルト値
     * @param targetDataType 処理対象のデータ型
     */
    TableDataParser(TestDataReader reader,
                    List<TestDataInterpreter> interpreters,
                    DbInfo dbInfo,
                    DefaultValues defaultValues,
                    DataType targetDataType) {

        super(reader, interpreters, targetDataType);
        this.dbInfo = dbInfo;
        this.defaultValues = defaultValues;
        this.targetDataType = targetDataType;
    }

    /** キャッシュ(同一データを複数回読み込まないようにするため) */
    private static final Map<String, List<TableData>> CACHE = NablarchTestUtils.createLRUMap(8);

    @Override
    void parse(String id) {
        String key = directory + '/' + resource + '/' + targetDataType + '/' + id;
        if (CACHE.containsKey(key)) {
            result = CACHE.get(key);
        } else {
            result = new ArrayList<TableData>();
            CACHE.put(key, result);
            super.parse(id);
        }
    }

    /**
     * {@inheritDoc}
     * マーカーカラム以外を追加する。
     */
    @Override
    void onReadLine(List<String> line) {
        List<String> row = header.excludeMarkerColumns(line);
        processing.addRow(row);
    }

    /**
     * {@inheritDoc}
     * 新たな{@link TableData}を生成しカラム名を設定する。
     */
    @Override
    void onTargetTypeFound(List<String> line) {
        // テーブル名
        String tableName = getTypeValue(line);
        // カラム名の行を読み込み
        header = new HeaderLine(readLine());
        String[] columnNames = header.getEffectiveColumnNames();
        // テーブルデータオブジェクト生成
        processing = new TableData(dbInfo, tableName, columnNames, defaultValues);
        result.add(processing);
    }

    /** {@inheritDoc} */
    @Override
    List<TableData> getResult() {
        return result;
    }


}
