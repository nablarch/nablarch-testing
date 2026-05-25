# NTF テストデータ記述例

<a name="overview"></a>

## 1.1 NTF テストデータ

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

<a name="section-identifier"></a>

## 2. セクション識別: groupId の使い方

受注管理テーブルのデータをテストケース別（正常注文 / 大量注文）で使い分けるシナリオ。groupId でセクションを区別します。

### Excel

| SETUP_TABLE[case01]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | LINE_NO | PRODUCT_CODE | QUANTITY | UNIT_PRICE |
| 1001 | 1 | P-001 | 5 | 1500 |
| 1001 | 2 | P-002 | 3 | 2800 |

| SETUP_TABLE[case02]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | LINE_NO | PRODUCT_CODE | QUANTITY | UNIT_PRICE |
| 2001 | 1 | P-003 | 100 | 500 |

| SETUP_TABLE[case02]=ORDER_DETAIL | | | | |
|---|---|---|---|---|
| ORDER_ID | LINE_NO | PRODUCT_CODE | QUANTITY | UNIT_PRICE |
| 2001 | 2 | P-004 | 200 | 300 |

- `SETUP_TABLE[case01]` と `SETUP_TABLE[case02]` で groupId を使いケースごとに異なるセットアップデータを使い分けます
- 同一 groupId のセクションを複数記述するとすべて収集されます（case02 が2件）

### YAML

```yaml
setup_tables:
  - group_id: case01
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "1001"
        LINE_NO: "1"
        PRODUCT_CODE: "P-001"
        QUANTITY: "5"
        UNIT_PRICE: "1500"
      - ORDER_ID: "1001"
        LINE_NO: "2"
        PRODUCT_CODE: "P-002"
        QUANTITY: "3"
        UNIT_PRICE: "2800"
  - group_id: case02
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "2001"
        LINE_NO: "1"
        PRODUCT_CODE: "P-003"
        QUANTITY: "100"
        UNIT_PRICE: "500"
  - group_id: case02
    table: ORDER_DETAIL
    rows:
      - ORDER_ID: "2001"
        LINE_NO: "2"
        PRODUCT_CODE: "P-004"
        QUANTITY: "200"
        UNIT_PRICE: "300"
```

- `group_id:` フィールドで groupId を指定します。省略するとグループIDなし（デフォルトグループ）扱いです
- 同一 `group_id` のエントリを複数並べるとすべて収集されます（`case02` が2件）

---

<a name="test-shots"></a>

## 3. テストケース定義

### リクエスト単体テスト（ウェブアプリケーション）

#### Excel

| LIST_MAP=testShots | | | | | |
|---|---|---|---|---|---|
| no | description | isValidToken | expectedStatusCode | forwardUri | context |
| 1 | 正常ケース | 0 | 200 | /success | context001 |
| 2 | 認証エラー | 0 | 400 | /error | context002 |

| LIST_MAP=context001 | | |
|---|---|---|
| REQUEST_ID | USER_ID | HTTP_METHOD |
| REQ_001 | user001 | POST |

| LIST_MAP=context002 | | |
|---|---|---|
| REQUEST_ID | USER_ID | HTTP_METHOD |
| REQ_001 | invalid_user | POST |

- `testShots` の `context` カラムに `LIST_MAP` の ID を指定し、リクエスト情報（`REQUEST_ID`・`USER_ID`・`HTTP_METHOD`）を参照します
- `isValidToken` は CSRF トークン制御フラグです（`1`: あり、`0`: なし）

#### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正常ケース"
        isValidToken: "0"
        expectedStatusCode: "200"
        forwardUri: "/success"
        context: "context001"
      - no: "2"
        description: "認証エラー"
        isValidToken: "0"
        expectedStatusCode: "400"
        forwardUri: "/error"
        context: "context002"
  - id: context001
    rows:
      - REQUEST_ID: "REQ_001"
        USER_ID: "user001"
        HTTP_METHOD: "POST"
  - id: context002
    rows:
      - REQUEST_ID: "REQ_001"
        USER_ID: "invalid_user"
        HTTP_METHOD: "POST"
```

- `id: testShots` エントリの `context` フィールドに `LIST_MAP` の ID を指定し、対応する `context001`/`context002` エントリからリクエスト情報を参照します
- `testShots` が 0 件の場合は例外がスローされます

---

### リクエスト単体テスト（バッチ処理）

#### Excel

| LIST_MAP=testShots | | | | | | | | | | |
|---|---|---|---|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | setUpTable | expectedTable | setUpFile | expectedFile | expectedLog | diConfig | requestPath | userId |
| 1 | 正しく更新されます | 0 | default | default | | | | nablarch/test/core/batch/BatchSample.xml | DBtoDBBatchSample | test |
| 2 | 入力ファイルあり | 0 | | | case2 | case2 | | nablarch/test/core/batch/BatchSample.xml | FileToFileBatchSample | test |

- `setUpTable`・`expectedTable` には groupId を指定します（`default` は groupId なし扱い）
- `setUpFile`・`expectedFile` には `SETUP_FIXED`/`EXPECTED_FIXED` の groupId を指定します

#### YAML

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
        expectedLog: ""
        diConfig: "nablarch/test/core/batch/BatchSample.xml"
        requestPath: "DBtoDBBatchSample"
        userId: "test"
      - no: "2"
        description: "入力ファイルあり"
        expectedStatusCode: "0"
        setUpTable: ""
        expectedTable: ""
        setUpFile: "case2"
        expectedFile: "case2"
        expectedLog: ""
        diConfig: "nablarch/test/core/batch/BatchSample.xml"
        requestPath: "FileToFileBatchSample"
        userId: "test"
```

- `setUpTable`/`expectedTable` には `SETUP_TABLE`/`EXPECTED_TABLE` の groupId を指定します（`default` は groupId なし扱い）
- `setUpFile`/`expectedFile` には `SETUP_FIXED`/`EXPECTED_FIXED` の groupId を指定します。空の場合はスキップされます
