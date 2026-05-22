# NTF テストデータ記述例（Excel / YAML 対比）

- **目的**: `ntf-spec.md` の各節に対応する Excel 表と YAML コードブロックを対比形式で示します
- **Excel 表の読み方**: 各セルをテーブルのセルとして読んでください。ファイルデータのデータ行で先頭セルが空のセルは、レコード種別が先頭列に入らないことを表しています

---

## 概要: 1ファイルに3種類のデータを共存させる {#overview}

テストケース（`testShots`）・セットアップ（`SETUP_TABLE`）・検証（`EXPECTED_TABLE`）の3種を1ファイルにまとめて記述した例です。

### Excel

| LIST_MAP=testShots | | | |
|---|---|---|---|
| no | description | expectedStatusCode | forwardUri |
| 1 | 正常ケース | 200 | /result |

| SETUP_TABLE=USER_TABLE | | |
|---|---|---|
| USER_ID | USER_NAME | STATUS |
| U001 | 山田太郎 | 01 |

| EXPECTED_TABLE=USER_TABLE | | |
|---|---|---|
| USER_ID | STATUS | |
| U001 | 02 | |

### YAML

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正常ケース"
        expectedStatusCode: "200"
        forwardUri: "/result"

setup_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "U001"
        USER_NAME: "山田太郎"
        STATUS: "01"

expected_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "U001"
        STATUS: "02"
```

### ポイント

- テストケース・セットアップ・検証の3種を1ファイルに共存させることができます
- セクションの記述順序は問いません
- `EXPECTED_TABLE` で `USER_NAME` を省略しているため、`USER_NAME` カラムは比較対象外になります

---

## セクション識別 {#section-identifier}

### Excel

| SETUP_TABLE=USER_TABLE | | |
|---|---|---|
| USER_ID | USER_NAME | STATUS |
| 001 | 山田太郎 | 01 |
| 002 | 鈴木花子 | 02 |

| SETUP_TABLE[case1]=ORDER_TABLE | | | |
|---|---|---|---|
| ORDER_ID | USER_ID | AMOUNT | [MARKER] |
| 1001 | 001 | 5000 | X |

### YAML

```yaml
setup_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        STATUS: "01"
      - USER_ID: "002"
        USER_NAME: "鈴木花子"
        STATUS: "02"
  - group_id: case1
    table: ORDER_TABLE
    rows:
      - ORDER_ID: "1001"
        USER_ID: "001"
        AMOUNT: "5000"
        "[MARKER]": "X"
```

### ポイント・差異

- Excel ではセクション識別行に `SETUP_TABLE=テーブル名` と書きます。YAML では `setup_tables:` というセクションキーを使い、各エントリの `table:` にテーブル名を記述します
- groupId は Excel では `[case1]` と DataType 名に続けて書きます。YAML では `group_id: case1` フィールドとして記述します
- マーカーカラム `[MARKER]` は YAML では `"[MARKER]"` とダブルクォートで囲みます（YAML の角括弧構文との衝突を避けるため）
- **Excel 固有**: Excel では DataType 判定に前方一致（`startsWith`）を使用します。YAML では完全なセクションキーを使用します

---

## テストケース定義 {#test-shots}

### Excel

| LIST_MAP=testShots | | | | | |
|---|---|---|---|---|---|
| no | description | isValidToken | expectedStatusCode | forwardUri | context |
| 1 | 正常ケース | 0 | 200 | /success | context001 |
| 2 | 認証エラー | 0 | 400 | /error | context002 |

| LIST_MAP=context001 | | |
|---|---|---|
| REQUEST_ID | USER_ID | HTTP_METHOD |
| REQ_001 | user001 | POST |

### YAML

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

### ポイント

- `testShots` はフレームワークが自動読み込みする予約 ID です
- `context` カラムの値は対応する `LIST_MAP` の ID を指定します。その `LIST_MAP` から `REQUEST_ID`・`USER_ID`・`HTTP_METHOD` を取得します
- `testShots` が0件の場合は例外がスローされます

---

## テーブルデータ {#table-data}

### SETUP_TABLE {#setup-table}

#### Excel

| SETUP_TABLE=USER_TABLE | | | |
|---|---|---|---|
| USER_ID | USER_NAME | AGE | STATUS |
| 001 | 山田太郎 | 30 | 01 |
| 002 | 鈴木花子 | 25 | 02 |

#### YAML

```yaml
setup_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        AGE: "30"
        STATUS: "01"
      - USER_ID: "002"
        USER_NAME: "鈴木花子"
        AGE: "25"
        STATUS: "02"
```

#### ポイント・差異

- Excel ではカラム名を1行目に並べ、2行目以降にデータを記述します。YAML では各行がオブジェクトになり、カラム名がキーになります
- 全値は文字列として記述します（YAML でも `"001"` のようにクォートします）
- **主キーカラムは省略不可**です。省略するとデフォルト値（`"0"` やスペース）が INSERT されます

---

### EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE {#expected-complete-table}

#### Excel

| EXPECTED_TABLE=USER_TABLE | | |
|---|---|---|
| USER_ID | USER_NAME | STATUS |
| 001 | 山田太郎 | 01 |

| EXPECTED_COMPLETE_TABLE=USER_TABLE | | | |
|---|---|---|---|
| USER_ID | USER_NAME | AGE | STATUS |
| 001 | 山田太郎 | | 01 |

#### YAML

```yaml
expected_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        STATUS: "01"

expected_complete_tables:
  - table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        # AGE を省略 → BasicDefaultValues のデフォルト値（"0"）で補完されて比較
        STATUS: "01"
```

#### ポイント・差異

- `EXPECTED_TABLE` で省略したカラムは比較対象外になります
- `EXPECTED_COMPLETE_TABLE` で省略したカラムには `BasicDefaultValues` のデフォルト値が補完されてから比較されます
- YAML では省略したいカラムのキーを書かないだけです。Excel では値を空セルにすることで省略できます
- **混在禁止**: 同一ファイル内で `expected_tables` と `expected_complete_tables` を混在させると後半のデータが読み込まれません。種別ごとにまとめて記述してください

---

### LIST_MAP {#list-map}

#### Excel

| LIST_MAP=params | | |
|---|---|---|
| KEY | VALUE | NOTE |
| userId | user001 | テストユーザー |
| requestId | REQ001 | |

#### YAML

```yaml
list_maps:
  - id: params
    rows:
      - KEY: "userId"
        VALUE: "user001"
        NOTE: "テストユーザー"
      - KEY: "requestId"
        VALUE: "REQ001"
        NOTE: null
```

#### ポイント・差異

- Excel では `LIST_MAP=id` でセクションを識別します。YAML では `list_maps:` セクション内の `id:` フィールドで識別します
- `testShots` は予約 ID です。フレームワークがテストケース定義として自動読み込みします

---

## ファイルデータ {#file-data}

### 固定長ファイル

#### Excel

ファイルデータはセクション識別行の直後に「レコード種別とフィールド名称」行が来ます。データ行の先頭セルはレコード種別を繰り返さないため空になります。

| SETUP_FIXED=input/data.dat | | | | |
|---|---|---|---|---|
| text-encoding | MS932 | | | |
| DATA | USER_ID | USER_NAME | AMOUNT | |
| | X | N | Z | |
| | 10 | 20 | 10 | |
| | 001 | 山田太郎 | 5000 | |
| | 002 | 鈴木花子 | 3000 | |

#### YAML

```yaml
setup_files:
  - path: input/data.dat
    type: fixed
    directives:
      text-encoding: MS932
    records:
      - record_type: DATA
        fields:
          - {name: USER_ID,   type: X, length: 10}
          - {name: USER_NAME, type: N, length: 20}
          - {name: AMOUNT,    type: Z, length: 10}
        rows:
          - ["001", "山田太郎", "5000"]
          - ["002", "鈴木花子", "3000"]
```

#### ポイント・差異

- Excel では「レコード種別+フィールド名称行・データ型行・フィールド長行」の3行でフィールドを定義します。YAML では `fields:` 配列の1要素（`name`/`type`/`length`）にまとめます
- Excel のデータ行は先頭セルが空です（レコード種別は定義行にのみ記述する Excel 固有の規則）。YAML の `rows:` は配列の配列形式で先頭セルの概念はありません
- `rows:` の各配列は `fields:` と**完全に同じ順序・件数**で値を並べます
- **パディング不要**: 固定長ファイルのデータ値はパディングなしで記述します（フレームワークが自動付与します）

---

### 可変長ファイル

#### Excel

| SETUP_VARIABLE=input/data.csv | | | |
|---|---|---|---|
| field-separator | , | | |
| DATA | USER_ID | USER_NAME | AMOUNT |
| | X | N | X |
| | 001 | 山田太郎 | 5000 |
| | 002 | 鈴木花子 | 3000 |

#### YAML

```yaml
setup_files:
  - path: input/data.csv
    type: variable
    directives:
      field-separator: ","
    records:
      - record_type: DATA
        fields:
          - {name: USER_ID,   type: X}
          - {name: USER_NAME, type: N}
          - {name: AMOUNT,    type: X}
        rows:
          - ["001", "山田太郎", "5000"]
          - ["002", "鈴木花子", "3000"]
```

#### ポイント・差異

- 可変長ファイルは `length` が不要です。`fields:` の各要素から `length` を省略できます
- 固定長との見た目の差異は `type: fixed` / `type: variable` と `length` の有無だけです

---

### 複数レコードレイアウト {#multi-record}

#### Excel

| SETUP_FIXED=input/multi.dat | | | |
|---|---|---|---|
| HEADER | SEQ | TYPE | |
| | X | X | |
| | 4 | 2 | |
| | H001 | 01 | |
| DATA | USER_ID | AMOUNT | NOTE |
| | X | Z | N |
| | 10 | 10 | 20 |
| | 001 | 5000 | 備考 |

#### YAML

```yaml
setup_files:
  - path: input/multi.dat
    type: fixed
    records:
      - record_type: HEADER
        fields:
          - {name: SEQ,  type: X, length: 4}
          - {name: TYPE, type: X, length: 2}
        rows:
          - ["H001", "01"]
      - record_type: DATA
        fields:
          - {name: USER_ID, type: X, length: 10}
          - {name: AMOUNT,  type: Z, length: 10}
          - {name: NOTE,    type: N, length: 20}
        rows:
          - ["001", "5000", "備考"]
```

#### ポイント・差異

- Excel では同一セクション内でレコード種別+フィールド名称行を続けて書くことで複数レコードレイアウトを表現します
- YAML では `records:` 配列に複数のレコードレイアウトを並べます

---

### 空ファイル {#empty-file}

#### Excel

| SETUP_FIXED=input/empty.dat | |
|---|---|
| text-encoding | MS932 |

#### YAML

```yaml
setup_files:
  - path: input/empty.dat
    type: fixed
    directives:
      text-encoding: MS932
    records: []
```

#### ポイント・差異

- Excel ではディレクティブ行のみ記述してレコード定義以降を省略します
- YAML では `records: []` と空配列を記述します

---

## メッセージングテストデータ {#messaging}

### MESSAGE セクション

#### Excel

| MESSAGE=REQ001 | | | | |
|---|---|---|---|---|
| FW_HEADER | requestId | userId | resendFlag | resultCode |
| | REQ001 | user001 | 0 | 0 |
| BODY | field1 | field2 | | |
| | X | X | | |
| | 10 | 20 | | |
| | value1 | value2 | | |

#### YAML

```yaml
messages:
  - id: REQ001
    records:
      - record_type: FW_HEADER
        fields:
          - {name: requestId,  type: X, length: 10}
          - {name: userId,     type: X, length: 10}
          - {name: resendFlag, type: X, length: 1}
          - {name: resultCode, type: X, length: 2}
        rows:
          - ["REQ001", "user001", "0", "0"]
      - record_type: BODY
        fields:
          - {name: field1, type: X, length: 10}
          - {name: field2, type: X, length: 20}
        rows:
          - ["value1", "value2"]
```

#### ポイント・差異

- `record_type` の値（`FW_HEADER`、`BODY` 等）はフレームワーク内部で `"default"` に置き換えられます。任意の値を記述できます
- `no` 列（先頭列）はフレームワークが除去します。データとして保存されません

---

### SendSync メッセージ

#### Excel

| MESSAGE=sendSyncTestData/REQ001/message | | | | |
|---|---|---|---|---|
| no | errorMode | field1 | field2 | |
| 1 | | value1 | value2 | |
| 2 | | value3 | value4 | |

#### YAML

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
          - ["1", "", "value1", "value2"]
          - ["2", "", "value3", "value4"]
```

#### ポイント・差異

- `no` 列の値は送信順序と一致させます
- `errorMode` に `timeout` または `msgException` を指定すると他フィールドはパース対象外になります

---

## 特殊値・インタープリタ {#datetime}

### 日付型・Timestamp・特殊値

#### Excel

| EXPECTED_TABLE=SCHEDULE | | | |
|---|---|---|---|
| ID | EVENT_NAME | START_DATE | CREATED_AT |
| 1 | 会議 | 2024-01-15 | 2024-01-01 09:00:00.0 |
| 2 | ${systemTime} テスト | ${systemTime} | ${systemTime} |
| 3 | NULL テスト | NULL | NULL |

#### YAML

```yaml
expected_tables:
  - table: SCHEDULE
    rows:
      - ID: "1"
        EVENT_NAME: "会議"
        START_DATE: "2024-01-15"
        CREATED_AT: "2024-01-01 09:00:00.0"
      - ID: "2"
        EVENT_NAME: "${systemTime} テスト"
        START_DATE: "${systemTime}"
        CREATED_AT: "${systemTime}"
      - ID: "3"
        EVENT_NAME: null
        START_DATE: null
        CREATED_AT: null
```

#### ポイント・差異

- `java.sql.Timestamp` 型カラムの期待値は末尾 `.0` が必須です（`"2024-01-01 09:00:00.0"`）
- `${systemTime}` は `DateTimeInterpreter` が変換します。完全一致のみ変換されます。文字列中の `${systemTime}` を変換するには `CompositeInterpreter` との組み合わせが必要です
- NULL 値は Excel では `NULL`（`NullInterpreter` が変換）、YAML では `null`（アンクォート）で記述します
- **注意**: YAML で `"null"` とクォートすると文字列 `"null"` として格納されます（NULL にはなりません）

---

## ディレクティブ {#directive}

### 固定長ファイルのディレクティブ

#### Excel

| SETUP_FIXED=input/data.dat | | |
|---|---|---|
| text-encoding | MS932 | |
| positive-zone-sign-nibble | C | |
| DATA | USER_ID | AMOUNT |
| | X | Z |
| | 10 | 10 |
| | 001 | 5000 |

### 可変長ファイルのディレクティブ

#### Excel

| SETUP_VARIABLE=input/data.tsv | | |
|---|---|---|
| field-separator | \t | |
| record-separator | CRLF | |
| DATA | FIELD1 | FIELD2 |
| | X | X |
| | value1 | value2 |

#### YAML

```yaml
setup_files:
  - path: input/data.tsv
    type: variable
    directives:
      field-separator: "\\t"
      record-separator: CRLF
    records:
      - record_type: DATA
        fields:
          - {name: FIELD1, type: X}
          - {name: FIELD2, type: X}
        rows:
          - ["value1", "value2"]
```

#### ポイント・差異

- Excel ではディレクティブ行を「キー | 値」の2セルで記述します。YAML では `directives:` オブジェクトの `key: value` 形式で記述します
- `file-type` と `record-length` はフレームワークが自動設定するため通常は記述不要です
- `field-separator` に `\t` を指定するとタブ文字になります。YAML では `"\\t"` と記述します
- 無効なディレクティブキーを指定すると `IllegalArgumentException` がスローされます
