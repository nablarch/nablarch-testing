# NTF テストデータ記述例 — testShots カラム一覧

処理方式ごとの `testShots` カラムと記述例。どの処理方式でも `testShots` は `LIST_MAP` として記述します。

---

## ウェブアプリケーション（HttpRequestTestSupport）

### 必須カラム

| カラム名 | 説明 |
|---|---|
| `no` | テストケース番号 |
| `description` | テストケースの説明（旧名 `case` も可） |
| `isValidToken` | CSRF トークン制御フラグ（`1`: あり、`0`: なし） |
| `expectedStatusCode` | 期待する HTTP ステータスコード |
| `forwardUri` | 期待するフォワード先 URI |
| `context` | リクエスト ID・ユーザ・HTTP メソッドを記載した `LIST_MAP` 名 |

`context` LIST_MAP は1エントリのみ有効です。`REQUEST_ID` が空の場合は例外がスローされます。

### オプションカラム

| カラム名 | 説明 | 空の場合 |
|---|---|---|
| `setUpTable` | この値と同じ groupId を持つ `SETUP_TABLE` セクションを収集して INSERT します | スキップ |
| `expectedTable` | この値と同じ groupId を持つ `EXPECTED_TABLE`/`EXPECTED_COMPLETE_TABLE` セクションで DB を検証します | スキップ |
| `expectedSearch` | 検索結果期待値の groupId（対応する `LIST_MAP` セクションを収集） | スキップ |
| `expectedMessageId` | 期待するメッセージ ID（カンマ区切りで複数指定可） | スキップ |
| `requestParams` | HTTP リクエストパラメータの `LIST_MAP` 名 | — |
| `cookie` | Cookie 値の `LIST_MAP` 名 | Cookie なし |
| `queryParams` | クエリパラメータの `LIST_MAP` 名 | パラメータなし |
| `HTTP_METHOD` | HTTP メソッド | `"POST"` |
| `expectedContentLength` | 期待する Content-Length | スキップ |
| `expectedContentType` | 期待する Content-Type | スキップ |
| `expectedContentFileName` | 期待する Content-Disposition ファイル名 | スキップ |
| `expectedMessage` | この値と同じ groupId を持つ要求電文セクション（`EXPECTED_REQUEST_HEADER/BODY_MESSAGES`）で検証します | スキップ |
| `responseMessage` | この値と同じ groupId を持つ応答電文セクション（`RESPONSE_HEADER/BODY_MESSAGES`）をレスポンスとして返します | スキップ |
| `expectedMessageByClient` | HTTP 同期応答メッセージ送信の要求電文グループ ID | スキップ |
| `responseMessageByClient` | HTTP 同期応答メッセージ送信の応答電文グループ ID | スキップ |

### 記述例

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
```

---

## バッチ処理（BatchRequestTestSupport）

### 必須カラム

| カラム名 | 説明 |
|---|---|
| `no` | テストケース番号 |
| `description` | テストケースの説明 |
| `expectedStatusCode` | 期待するステータスコード |
| `diConfig` | DI コンポーネント設定ファイルパス |
| `requestPath` | リクエストパス |
| `userId` | 実行ユーザ ID |

### オプションカラム

| カラム名 | 説明 | 空の場合 |
|---|---|---|
| `setUpTable` | この値と同じ groupId を持つ `SETUP_TABLE` セクションを収集して INSERT します | スキップ |
| `expectedTable` | この値と同じ groupId を持つ `EXPECTED_TABLE`/`EXPECTED_COMPLETE_TABLE` セクションで DB を検証します | スキップ |
| `setUpFile` | この値と同じ groupId を持つ `SETUP_FIXED`/`SETUP_VARIABLE` セクションを入力ファイルとして配置します | スキップ |
| `expectedFile` | この値と同じ groupId を持つ `EXPECTED_FIXED`/`EXPECTED_VARIABLE` セクションで出力ファイルを検証します | スキップ |
| `expectedLog` | 期待ログの `LIST_MAP` 名 | スキップ |
| `args[0]`, `args[1]`, ... | コマンドライン引数 | — |
| その他任意カラム | コマンドラインオプション | — |

### 記述例

#### Excel

| LIST_MAP=testShots | | | | | | |
|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | diConfig | requestPath | userId | setUpFile |
| 1 | 正しく更新されます | 0 | nablarch/test/core/batch/BatchSample.xml | DBtoDBBatchSample | test | |
| 2 | 入力ファイルあり | 0 | nablarch/test/core/batch/BatchSample.xml | FileToFileBatchSample | test | case2 |

#### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正しく更新されます"
        expectedStatusCode: "0"
        diConfig: "nablarch/test/core/batch/BatchSample.xml"
        requestPath: "DBtoDBBatchSample"
        userId: "test"
        setUpFile: ""
      - no: "2"
        description: "入力ファイルあり"
        expectedStatusCode: "0"
        diConfig: "nablarch/test/core/batch/BatchSample.xml"
        requestPath: "FileToFileBatchSample"
        userId: "test"
        setUpFile: "case2"
```

---

## メッセージング（MessagingRequestTestSupport）

### 必須カラム

| カラム名 | 説明 |
|---|---|
| `no` | テストケース番号 |
| `description` | テストケースの説明 |
| `expectedStatusCode` | 期待するステータスコード |
| `diConfig` | DI コンポーネント設定ファイルパス |
| `requestPath` | リクエストパス |
| `userId` | 実行ユーザ ID |

### オプションカラム

| カラム名 | 説明 | 空の場合 |
|---|---|---|
| `setUpTable` | この値と同じ groupId を持つ `SETUP_TABLE` セクションを収集して INSERT します | スキップ |
| `expectedTable` | この値と同じ groupId を持つ `EXPECTED_TABLE`/`EXPECTED_COMPLETE_TABLE` セクションで DB を検証します | スキップ |
| `expectedMessage` | この値と同じ groupId を持つ要求電文セクション（`EXPECTED_REQUEST_HEADER/BODY_MESSAGES`）で検証します | スキップ |
| `responseMessage` | この値と同じ groupId を持つ応答電文セクション（`RESPONSE_HEADER/BODY_MESSAGES`）をレスポンスとして返します | スキップ |
| `expectedLog` | 期待ログの `LIST_MAP` 名 | スキップ |

### 記述例

#### Excel

| LIST_MAP=testShots | | | | | | | |
|---|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | diConfig | requestPath | userId | expectedMessage | responseMessage |
| 1 | 電文送受信テスト | 0 | batch-test-component-configuration.xml | BM21AA0106 | batch_user | case1 | res_case1 |

#### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "電文送受信テスト"
        expectedStatusCode: "0"
        diConfig: "batch-test-component-configuration.xml"
        requestPath: "BM21AA0106"
        userId: "batch_user"
        expectedMessage: "case1"
        responseMessage: "res_case1"
```
