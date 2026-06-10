# NTF テストデータ解説書 — 記述例（概要・groupId）

<a name="overview"></a>

## 1. NTF テストデータ

リクエスト単体テスト（バッチ処理）の例。テストケース・セットアップ・検証の3種類が共存しています。

### Excel

| LIST_MAP=testShots | | | | | | | | | | |
|---|---|---|---|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | setUpTable | expectedTable | setUpFile | expectedFile | diConfig | requestPath | userId | expectedLog |
| 1 | 注文カウンタが正しくインクリメントされます | 0 | default | default | | | nablarch/test/core/batch/BatchSample.xml | DBtoDBBatchSample | test | expectedLog |

| SETUP_TABLE=ORDER_HEADER | | | |
|---|---|---|---|
| ORDER_ID | ITEM_COUNT | REMARKS | |
| 10001 | 10 | 通常注文 | |
| 10002 | 20 | まとめ買い | |

| EXPECTED_TABLE=ORDER_HEADER | | | |
|---|---|---|---|
| ORDER_ID | ITEM_COUNT | REMARKS | UPDATE_DATE |
| 10001 | 11 | 通常注文 | 2010-09-13 12:34:56.0 |
| 10002 | 21 | まとめ買い | 2010-09-13 12:34:56.0 |

| LIST_MAP=expectedLog | | |
|---|---|---|
| message | logLevel | |
| 注文ID[10001] | INFO | |
| 注文ID[10002] | INFO | |

- `LIST_MAP=testShots` がテストケース定義、`SETUP_TABLE` がセットアップ、`EXPECTED_TABLE` が検証、`LIST_MAP=expectedLog` が期待ログ

### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正しく更新されます"
        expectedStatusCode: "0"
        setUpTable: "default"
        expectedTable: "default"
        setUpFile: ""
        expectedFile: ""
        diConfig: "nablarch/test/core/batch/BatchSample.xml"
        requestPath: "DBtoDBBatchSample"
        userId: "test"
        expectedLog: "expectedLog"
  - id: expectedLog
    rows:
      - message: "注文ID[10001]"
        logLevel: "INFO"
      - message: "注文ID[10002]"
        logLevel: "INFO"

setup_tables:
  - table: ORDER_HEADER
    rows:
      - ORDER_ID: "10001"
        ITEM_COUNT: "10"
        REMARKS: "通常注文"
      - ORDER_ID: "10002"
        ITEM_COUNT: "20"
        REMARKS: "まとめ買い"

expected_tables:
  - table: ORDER_HEADER
    rows:
      - ORDER_ID: "10001"
        ITEM_COUNT: "11"
        REMARKS: "通常注文"
        UPDATE_DATE: "2010-09-13 12:34:56.0"
      - ORDER_ID: "10002"
        ITEM_COUNT: "21"
        REMARKS: "まとめ買い"
        UPDATE_DATE: "2010-09-13 12:34:56.0"
```

- `list_maps:` の `id: testShots` がテストケース定義、`setup_tables:` がセットアップ、`expected_tables:` が検証です
- `id: expectedLog` のような任意 ID の `list_maps:` エントリも同一ファイルに共存できます
- 同一の `list_maps:` キーに複数エントリをリストとして並べます（YAMLはトップレベルキーの重複不可）

---

<a name="groupid"></a>

## 4.3 セクションのグループ化（groupId）

テストケースごとに異なるセットアップデータを使い分けるシナリオ。

**ポイント**: `testShots` の `setUpTable` カラムに groupId を書く → そのgroupIdが付いたセクションだけが投入される。

- ケース1（正常注文）: `setUpTable=case01` → `SETUP_TABLE[case01]` のデータが使われる
- ケース2（大量注文）: `setUpTable=case02` → `SETUP_TABLE[case02]` のデータが使われる

### Excel

| LIST_MAP=testShots | | | | |
|---|---|---|---|---|
| no | description | expectedStatusCode | setUpTable | expectedTable |
| 1 | 正常注文 | 0 | case01 | case01 |
| 2 | 大量注文 | 0 | case02 | case02 |

| SETUP_TABLE[case01]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | PRODUCT_CODE | QUANTITY | UNIT_PRICE | |
| 1001 | P-001 | 5 | 1500 | |

| EXPECTED_TABLE[case01]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | PRODUCT_CODE | QUANTITY | UNIT_PRICE | |
| 1001 | P-001 | 5 | 1500 | |

| SETUP_TABLE[case02]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | PRODUCT_CODE | QUANTITY | UNIT_PRICE | |
| 2001 | P-003 | 100 | 500 | |
| 2001 | P-004 | 200 | 300 | |

| EXPECTED_TABLE[case02]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | PRODUCT_CODE | QUANTITY | UNIT_PRICE | |
| 2001 | P-003 | 100 | 500 | |
| 2001 | P-004 | 200 | 300 | |

- `testShots` の `setUpTable` カラムに groupId（`case01`/`case02`）を指定することで、そのケースで使うセクションを選択します
- `expectedTable` も同様に groupId を指定して検証データを切り替えます

### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正常注文"
        expectedStatusCode: "0"
        setUpTable: "case01"
        expectedTable: "case01"
      - no: "2"
        description: "大量注文"
        expectedStatusCode: "0"
        setUpTable: "case02"
        expectedTable: "case02"

setup_tables:
  - group_id: case01
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "1001"
        PRODUCT_CODE: "P-001"
        QUANTITY: "5"
        UNIT_PRICE: "1500"
  - group_id: case02
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "2001"
        PRODUCT_CODE: "P-003"
        QUANTITY: "100"
        UNIT_PRICE: "500"
      - ORDER_ID: "2001"
        PRODUCT_CODE: "P-004"
        QUANTITY: "200"
        UNIT_PRICE: "300"

expected_tables:
  - group_id: case01
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "1001"
        PRODUCT_CODE: "P-001"
        QUANTITY: "5"
        UNIT_PRICE: "1500"
  - group_id: case02
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "2001"
        PRODUCT_CODE: "P-003"
        QUANTITY: "100"
        UNIT_PRICE: "500"
      - ORDER_ID: "2001"
        PRODUCT_CODE: "P-004"
        QUANTITY: "200"
        UNIT_PRICE: "300"
```

- `testShots` の `setUpTable`/`expectedTable` に書いた値（`case01`/`case02`）がそのまま groupId として使われ、対応するセクションが収集されます
- groupId を省略したセクションは `setUpTable` が空のケースで使われます（groupId なし = デフォルトグループ）

