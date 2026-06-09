package nablarch.test.core.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nablarch.test.NablarchTestUtils;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

/**
 * {@literal List<Map<String, String>>}形式のデータを解釈するクラス。
 *
 * @author T.Kawasaki
 */
class ListMapParser extends SingleDataParsingTemplate<List<Map<String, String>>> {

    /** 解析結果 */
    private List<Map<String, String>> result;

    /** ヘッダー行 (キー名一覧) */
    private HeaderLine header;

    /**
     * コンストラクタ
     *
     * @param reader       リーダ
     * @param interpreters 解釈クラス
     */
    ListMapParser(TestDataReader reader, List<TestDataInterpreter> interpreters) {
        super(reader, interpreters, DataType.LIST_MAP);
    }

    /** キャッシュ(同一データを複数回読み込まないようにするため) */
    private static final Map<String, List<Map<String, String>>> CACHE = NablarchTestUtils.createLRUMap(8);

    /**
     * {@inheritDoc}
     *
     * 本クラスでは、パフォーマンス対策で一度読み込んだデータはキャッシュを行う。
     * 同一のリソースに対して、2回以降取得要求があった場合にはキャッシュからデータを返却する。
     * キャッシュにデータが存在しない場合（1回目）には、親クラスにテストデータの解析処理を委譲する。
     */
    @Override
    void parse(String id) {
        String key = directory + '/' + resource + '/' + id;
        if (CACHE.containsKey(key)) {
            result = CACHE.get(key);
        } else {
            result = new ArrayList<Map<String, String>>();
            CACHE.put(key, result);
            super.parse(id);
        }
    }

    /**
     * {@inheritDoc}
     * 処理対象データが発見された場合、次の行を読み込みヘッダ行とする。
     *
     * @param notUsed 使用しない
     */
    @Override
    void onTargetTypeFound(List<String> notUsed) {
        List<String> firstLine = readLine();
        header = new HeaderLine(firstLine);
    }

    /** {@inheritDoc} */
    @Override
    void onReadLine(List<String> line) {
        Map<String, String> row = header.getMapExcludingMarkerColumns(line);
        result.add(row);
    }
    /** {@inheritDoc} */
    @Override
    List<Map<String, String>> getResult() {
        return result;
    }
}

