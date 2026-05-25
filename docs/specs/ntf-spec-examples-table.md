# NTF テストデータ記述例

<a name="table-data"></a>

## 4.1 テーブルデータの基本形式

<a name="setup-table"></a>

### SETUP_TABLE

DB への INSERT データ。実物のデータは `BasicTestDataParserTest.xls` の `withoutGroupId` シートを参照。

#### Excel

| SETUP_TABLE=TEST_TABLE | | | | | | |
|---|---|---|---|---|---|---|
| PK_COL1 | PK_COL2 | NUMBER_COL | VARCHAR2_COL | NUMBER_COL2 | CLOB_COL | BLOB_COL |
| 0000000005 | IJ | 10000 | なにぬねの | 2.2 | CLOBです1 | ${binaryFile:testdata.txt} |
| 0000000006 | KL | 100000 | Null | 2.22 | CLOBです2 | ${binaryFile:BasicTestDataParserTest.xls} |

- カラム名を1行目に並べ、2行目以降にデータを記述します
- `//` で始まる行はコメントです（型情報・桁数などの注記に使われます）
- **主キーカラムは省略不可**です。省略すると `"0"` やスペースのデフォルト値が INSERT されます
- `NULL` 文字列は `NullInterpreter` により Java null に変換されます
- `${binaryFile:パス}` でファイル内容をバイナリ読み込みして HexString に変換できます

#### YAML

```yaml
setup_tables:
  - table: TEST_TABLE
    rows:
      - PK_COL1: "0000000005"
        PK_COL2: "IJ"
        NUMBER_COL: "10000"
        VARCHAR2_COL: "なにぬねの"
        NUMBER_COL2: "2.2"
        CLOB_COL: "CLOBです1"
        BLOB_COL: "${binaryFile:testdata.txt}"
      - PK_COL1: "0000000006"
        PK_COL2: "KL"
        NUMBER_COL: "100000"
        VARCHAR2_COL: null
        NUMBER_COL2: "2.22"
        CLOB_COL: "CLOBです2"
        BLOB_COL: "${binaryFile:BasicTestDataParserTest.xls}"
```

- 各行がオブジェクトになりカラム名がキーになります
- 全値は文字列として記述します（`"0000000005"` のようにクォートします）
- NULL 値はアンクォートの `null` で記述します。`"null"` とクォートすると文字列として格納されます

---

<a name="expected-complete-table"></a>

### EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE

テスト後の DB 状態を検証するデータ。

#### Excel

| EXPECTED_TABLE=TEST_TABLE | | | | | | |
|---|---|---|---|---|---|---|
| PK_COL1 | PK_COL2 | NUMBER_COL | VARCHAR2_COL | NUMBER_COL2 | CLOB_COL | BLOB_COL |
| 0000000001 | AB | 1 | あいうえお | 1.1 | CLOBです1 | ${binaryFile:testdata.txt} |
| 0000000002 | CD | 10 | かきくけこ | 1.11 | CLOBです2 | ${binaryFile:BasicTestDataParserTest.xls} |

| EXPECTED_COMPLETE_TABLE=BATCH_SAMPLE | | | |
|---|---|---|---|
| ID | COUNTER | MESSAGE | UPDATE_DATE |
| 10001 | 11 | こんにちは | 2010-09-13 12:34:56.0 |
| 10002 | 21 | さようなら | |

- `EXPECTED_TABLE`: 省略したカラムは比較対象外になります。検証したいカラムだけを列挙できます
- `EXPECTED_COMPLETE_TABLE`: 省略カラムには `BasicDefaultValues` のデフォルト値が補完されてから比較されます
- **混在禁止**: 同一ファイル内で `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を混在させると後半のデータが読み込まれません

#### YAML

```yaml
expected_tables:
  - table: TEST_TABLE
    rows:
      - PK_COL1: "0000000001"
        PK_COL2: "AB"
        NUMBER_COL: "1"
        VARCHAR2_COL: "あいうえお"
        NUMBER_COL2: "1.1"
        CLOB_COL: "CLOBです1"
        BLOB_COL: "${binaryFile:testdata.txt}"
      - PK_COL1: "0000000002"
        PK_COL2: "CD"
        NUMBER_COL: "10"
        VARCHAR2_COL: "かきくけこ"
        NUMBER_COL2: "1.11"
        CLOB_COL: "CLOBです2"
        BLOB_COL: "${binaryFile:BasicTestDataParserTest.xls}"

expected_complete_tables:
  - table: BATCH_SAMPLE
    rows:
      - ID: "10001"
        COUNTER: "11"
        MESSAGE: "こんにちは"
        UPDATE_DATE: "2010-09-13 12:34:56.0"
      - ID: "10002"
        COUNTER: "21"
        MESSAGE: "さようなら"
        # UPDATE_DATE を省略 → BasicDefaultValues のデフォルト値で補完されて比較
```

- 省略したいカラムのキーを書かないだけです
- `expected_tables:` と `expected_complete_tables:` は別キーのため混在可能です（YAMLパーサーが両方を独立して読み込んでマージします）

---

<a name="list-map"></a>

### LIST_MAP

キーバリュー形式の汎用データ。マーカーカラム・期待ログ・リクエストパラメータ等に使用します。

#### Excel — マーカーカラム付き

| LIST_MAP=params | | | | | |
|---|---|---|---|---|---|
| [no] | id | name | address | [desc] | |
| 1 | 0000000001 | 山田太郎 | 1 | 1番目のレコードです | |
| 2 | 0000000002 | 鈴木一郎 | 10 | 2番目のレコードです | |

- `[no]`・`[desc]` のように角括弧で囲まれたカラムはマーカーカラムです。DB 操作から除外されます
- マーカーカラムは Excel 上の見やすさのために使われることが多いです

#### Excel — 期待ログ

| LIST_MAP=expectedLog | | |
|---|---|---|
| message | logLevel | |
| 会員ID[10001] | INFO | |
| 会員ID[10002] | INFO | |

#### YAML

```yaml
list_maps:
  - id: params
    rows:
      - "[no]": "1"
        id: "0000000001"
        name: "山田太郎"
        address: "1"
        "[desc]": "1番目のレコードです"
      - "[no]": "2"
        id: "0000000002"
        name: "鈴木一郎"
        address: "10"
        "[desc]": "2番目のレコードです"
  - id: expectedLog
    rows:
      - message: "会員ID[10001]"
        logLevel: "INFO"
      - message: "会員ID[10002]"
        logLevel: "INFO"
```

- マーカーカラム `[no]`・`[desc]` は `"[no]"` とダブルクォートで囲みます（YAML の角括弧構文との衝突を避けるため）
- `testShots` は予約 ID です。フレームワークがテストケース定義として自動読み込みします
- ID は完全一致で検索されます。同一 ID の重複エントリは先着一致で 2 件目以降は無視されます
