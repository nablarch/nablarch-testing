# NTF テストデータ記述例 — 6章: メッセージングテストデータ

<a name="messaging"></a>

## 6.1 MESSAGE セクション（メッセージ送受信）

受信電文と応答電文を定義するケース。実物のデータは `MessageParserTest.xls` の `testParse` シートを参照。

### Excel

| MESSAGE=requestMessages | | | |
|---|---|---|---|
| text-encoding | Windows-31J | | |
| requestId | hoge | | |
| userId | moge | | |
| | ユーザ名 | 備考 | FILLER |
| | 全角 | 全角 | 半角 |
| | 50 | 200 | 252 |
| 1 | 電文太郎 | 特筆なし | |
| 2 | | ユーザ名が空欄なのでエラーが発生します。 | |

| MESSAGE=responseMessages | | | |
|---|---|---|---|
| no | 処理結果コード | 会員ID | FILLER |
| | X | X | X |
| | 2 | 10 | 490 |
| 1 | 00 | 1234567890 | |
| 2 | 01 | | |

- ディレクティブ行（`text-encoding` など）はフィールド定義より前に記述します
- フィールド名称行の先頭セルは空にします（Excel 固有）
- `no` 列（先頭列）はフレームワークが除去します。データとして保存されません

### YAML

```yaml
messages:
  - id: requestMessages
    records:
      - record_type: DEFAULT
        directives:
          text-encoding: Windows-31J
          requestId: hoge
          userId: moge
        fields:
          - {name: ユーザ名, type: 全角,  length: 50}
          - {name: 備考,     type: 全角,  length: 200}
          - {name: FILLER,   type: 半角,  length: 252}
        rows:
          - ["電文太郎", "特筆なし",                          ""]
          - ["",         "ユーザ名が空欄なのでエラーが発生します。", ""]
  - id: responseMessages
    records:
      - record_type: DEFAULT
        fields:
          - {name: 処理結果コード, type: X, length: 2}
          - {name: 会員ID,         type: X, length: 10}
          - {name: FILLER,         type: X, length: 490}
        rows:
          - ["00", "1234567890", ""]
          - ["01", "",           ""]
```

- `record_type` の値はフレームワーク内部で `"default"` に置き換えられます。任意の値を記述できます

---

## 6.2 要求電文・応答電文の期待値（SendSync メッセージング）

バッチリクエスト単体テストで電文の送受信をテストするケース。実物のデータは `RequestTestingSendSyncSupportTest.xls` を参照。

### Excel

| LIST_MAP=testShots | | | | | | | | | | |
|---|---|---|---|---|---|---|---|---|---|---|
| no | description | expectedStatusCode | setUpTable | expectedTable | expectedLog | diConfig | requestPath | userId | expectedMessage | responseMessage |
| 1 | 電文送受信テスト | 0 | | | | batch-test-component-configuration.xml | BM21AA0106 | batch_user | case1 | res_case1 |

| EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_01 | | | |
|---|---|---|---|
| text-encoding | ms932 | | |
| no | requestId | | |
| | 半角 | | |
| | 20 | | |
| 1 | RM21AA0104_01 | | |

- `expectedMessage` カラムには要求電文の groupId、`responseMessage` カラムには応答電文の groupId を指定します
- ヘッダとボディのエントリ数（rows 合計）は一致が必須です

### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "電文送受信テスト"
        expectedStatusCode: "0"
        setUpTable: ""
        expectedTable: ""
        expectedLog: ""
        diConfig: "batch-test-component-configuration.xml"
        requestPath: "BM21AA0106"
        userId: "batch_user"
        expectedMessage: "case1"
        responseMessage: "res_case1"

expected_request_header_messages:
  - group_id: case1
    id: RM21AA0104_01
    records:
      - record_type: DEFAULT
        directives:
          text-encoding: ms932
        fields:
          - {name: requestId, type: 半角, length: 20}
        rows:
          - ["RM21AA0104_01"]
```

---

## 6.3 sendSyncTestData の配置規則

テストデータファイルを `sendSyncTestData/{requestId}/message` に配置するケース。

### Excel

| MESSAGE=sendSyncTestData/REQ001/message | | | |
|---|---|---|---|
| no | errorMode | field1 | field2 |
| 1 | | value1 | value2 |
| 2 | | value3 | value4 |

- `MESSAGE=sendSyncTestData/{requestId}/message` というパスで配置します
- `no` 列の値は送信順序と一致させます
- `errorMode` に `errorMode:timeout` を指定するとタイムアウトエラー、`errorMode:msgException` を指定すると例外エラーのシミュレーションになります

### YAML

```yaml
messages:
  - id: sendSyncTestData/REQ001/message
    records:
      - record_type: DATA
        fields:
          - {name: no,        type: X, length: 2}
          - {name: errorMode, type: X, length: 10}
          - {name: field1,    type: X, length: 10}
          - {name: field2,    type: X, length: 10}
        rows:
          - ["1", "",        "value1", "value2"]
          - ["2", "",        "value3", "value4"]
```

- `errorMode` に `errorMode:timeout` または `errorMode:msgException` を指定すると他フィールドはパース対象外になります
- N 回送信する場合はヘッダ件数とボディ件数をともに N 件ずつ記述します

---

## 6.4 ステータスコードのデフォルト値

HTTP 同期応答テストでステータスコードカラムを省略するケース。

### Excel

| RESPONSE_BODY_MESSAGES=REQ001 | | |
|---|---|---|
| no | body | |
| | X | |
| | 10 | |
| 1 | RESULT_OK | |

- ステータスコードカラムがない場合はデフォルト値 `"200"` が使用されます

### YAML

```yaml
response_body_messages:
  - id: REQ001
    records:
      - record_type: DATA
        fields:
          - {name: body, type: X, length: 10}
        rows:
          - ["RESULT_OK"]
```

- ステータスコード列がない場合、実行時にデフォルト値 `"200"` が使用されます
