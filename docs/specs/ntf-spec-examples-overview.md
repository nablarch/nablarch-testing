# NTF テストデータ記述例

<a name="overview"></a>

## 1.1 NTF テストデータ

リクエスト単体テスト（バッチ処理）の例。テストケース・セットアップ・検証の3種類が共存しています。

### Excel

| LIST_MAP=testShots | | | | | | | | | | |
|---|---|---|---|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | setUpTable | expectedTable | setUpFile | expectedFile | diConfig | requestPath | userId | expectedLog |
| 1 | 正しく更新されます | 0 | default | default | | | nablarch/test/core/batch/BatchSample.xml | DBtoDBBatchSample | test | expectedLog |

| SETUP_TABLE=BATCH_SAMPLE | | | |
|---|---|---|---|
| ID | COUNTER | MESSAGE | |
| 10001 | 10 | こんにちは | |
| 10002 | 20 | さようなら | |

| EXPECTED_TABLE=BATCH_SAMPLE | | | |
|---|---|---|---|
| ID | COUNTER | MESSAGE | UPDATE_DATE |
| 10001 | 11 | こんにちは | 2010-09-13 12:34:56.0 |
| 10002 | 21 | さようなら | 2010-09-13 12:34:56.0 |

| LIST_MAP=expectedLog | | |
|---|---|---|
| message | logLevel | |
| 会員ID[10001] | INFO | |
| 会員ID[10002] | INFO | |

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
      - message: "会員ID[10001]"
        logLevel: "INFO"
      - message: "会員ID[10002]"
        logLevel: "INFO"

setup_tables:
  - table: BATCH_SAMPLE
    rows:
      - ID: "10001"
        COUNTER: "10"
        MESSAGE: "こんにちは"
      - ID: "10002"
        COUNTER: "20"
        MESSAGE: "さようなら"

expected_tables:
  - table: BATCH_SAMPLE
    rows:
      - ID: "10001"
        COUNTER: "11"
        MESSAGE: "こんにちは"
        UPDATE_DATE: "2010-09-13 12:34:56.0"
      - ID: "10002"
        COUNTER: "21"
        MESSAGE: "さようなら"
        UPDATE_DATE: "2010-09-13 12:34:56.0"
```

- `list_maps:` の `id: testShots` がテストケース定義、`setup_tables:` がセットアップ、`expected_tables:` が検証です
- `id: expectedLog` のような任意 ID の `list_maps:` エントリも同一ファイルに共存できます
- 同一の `list_maps:` キーに複数エントリをリストとして並べます（YAMLはトップレベルキーの重複不可）

---

<a name="section-identifier"></a>

## 2. セクション識別: groupId の使い方

複数テストケースで異なるセットアップデータを使い分けるため、groupId でセクションを区別します。

### Excel

| SETUP_TABLE[case01]=TEST_TABLE | | | | |
|---|---|---|---|---|
| PK_COL1 | PK_COL2 | NUMBER_COL | VARCHAR2_COL | NUMBER_COL2 |
| 0000000005 | IJ | 10000 | なにぬねの | 2.2 |
| 0000000006 | KL | 100000 | はひふへほ | 2.22 |

| SETUP_TABLE[case02]=TEST_TABLE | | | | |
|---|---|---|---|---|
| PK_COL1 | PK_COL2 | NUMBER_COL | VARCHAR2_COL | NUMBER_COL2 |
| 0000000007 | MN | 1000000 | まみむめも | 2.222 |

| SETUP_TABLE[case02]=TEST_TABLE | | | | |
|---|---|---|---|---|
| PK_COL1 | PK_COL2 | NUMBER_COL | VARCHAR2_COL | NUMBER_COL2 |
| 0000000008 | OP | 10000000 | やゆよ | 2.2222 |

- `SETUP_TABLE[case01]` と `SETUP_TABLE[case02]` で groupId を使いケースごとに異なるセットアップデータを使い分けます
- 同一 groupId のセクションを複数記述するとすべて収集されます（case02 が2件）

### YAML

```yaml
setup_tables:
  - group_id: case01
    table: TEST_TABLE
    rows:
      - PK_COL1: "0000000005"
        PK_COL2: "IJ"
        NUMBER_COL: "10000"
        VARCHAR2_COL: "なにぬねの"
        NUMBER_COL2: "2.2"
      - PK_COL1: "0000000006"
        PK_COL2: "KL"
        NUMBER_COL: "100000"
        VARCHAR2_COL: "はひふへほ"
        NUMBER_COL2: "2.22"
  - group_id: case02
    table: TEST_TABLE
    rows:
      - PK_COL1: "0000000007"
        PK_COL2: "MN"
        NUMBER_COL: "1000000"
        VARCHAR2_COL: "まみむめも"
        NUMBER_COL2: "2.222"
  - group_id: case02
    table: TEST_TABLE
    rows:
      - PK_COL1: "0000000008"
        PK_COL2: "OP"
        NUMBER_COL: "10000000"
        VARCHAR2_COL: "やゆよ"
        NUMBER_COL2: "2.2222"
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
