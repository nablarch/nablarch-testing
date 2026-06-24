package nablarch.test.core.reader;

/**
 * データタイプ定義クラス。<br>
 *
 * @author Hisaaki Sioiri
 */
public enum DataType {

    /** デフォルトのデータタイプ(どのタイプにも属さないことを意味する。) */
    DEFAULT(0, "DEFAULT"),

    /** 事前準備用のテーブルデータ */
    SETUP_TABLE_DATA(1, "SETUP_TABLE"),

    /** 期待値を示すテーブルデータ */
    EXPECTED_TABLE_DATA(2, "EXPECTED_TABLE"),

    /**
     * 期待値を示すテーブルデータ(更新用）
     * 省略されたカラムにはデフォルト値が設定される。
     */
    EXPECTED_COMPLETED(4, "EXPECTED_COMPLETE_TABLE"),

    /**
     * {@literal List<Map<String, String>}形式のデータ。<br/>
     * SqlResultSetとの比較等に用いる。
     */
    LIST_MAP(3, "LIST_MAP"),

    /** 事前準備用の固定長ファイル */
    SETUP_FIXED(5, "SETUP_FIXED"),

    /** 期待値を示す固定長ファイル */
    EXPECTED_FIXED(6, "EXPECTED_FIXED"),

    /** 事前準備用の固定長ファイル */
    SETUP_VARIABLE(7, "SETUP_VARIABLE"),

    /** 期待値を示す固定長ファイル */
    EXPECTED_VARIABLE(8, "EXPECTED_VARIABLE"),

    /** メッセージ */
    MESSAGE(9, "MESSAGE"), 

    /** 要求電文（ヘッダ）の期待値を示す固定長ファイル  */
    EXPECTED_REQUEST_HEADER_MESSAGES(10, "EXPECTED_REQUEST_HEADER_MESSAGES"),

    /** 要求電文（本文）の期待値を示す固定長ファイル  */
    EXPECTED_REQUEST_BODY_MESSAGES(11, "EXPECTED_REQUEST_BODY_MESSAGES"),

    /** 応答電文（ヘッダ）を示す固定長ファイル */
    RESPONSE_HEADER_MESSAGES(12, "RESPONSE_HEADER_MESSAGES"),
    
    /** 応答電文（本文）を示す固定長ファイル */
    RESPONSE_BODY_MESSAGES(13, "RESPONSE_BODY_MESSAGES");
    
    /** データタイプ */
    private final int type;

    /** データ名 */
    private final String name;

    /**
     * コンストラクタ。
     *
     * @param type データタイプ
     * @param name データ名
     */
    private DataType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * データタイプを取得する。
     *
     * @return データタイプ
     */
    public int getType() {
        return type;
    }

    /**
     * データ名を取得する。
     *
     * @return データ名
     */
    public String getName() {
        return name;
    }
}
