# NTF テストデータ記述例 — 7〜9章: 特殊値・ディレクティブ・ヘッダ

<a name="datetime"></a>

## 7. 特殊値・インタープリタ

### 7.1 日付型・Timestamp・特殊値

`EXPECTED_TABLE` で日付・タイムスタンプ・NULL・システム日時を使うケース。実物のデータは `BasicTestDataParserTest.xls` の `convertedValues` シートを参照。

#### Excel

| EXPECTED_TABLE=SCHEDULE | | | |
|---|---|---|---|
| ID | EVENT_NAME | START_DATE | CREATED_AT |
| 1 | 会議 | 2024-01-15 | 2024-01-01 09:00:00.0 |
| 2 | NULLテスト | NULL | NULL |
| 3 | システム時刻 | ${systemTime} | ${systemTime} |
| 4 | 更新時刻 | ${updateTime} | ${setUpTime} |

- `NULL` 文字列は `NullInterpreter` が Java null に変換します（大文字小文字不問: `null`・`Null` も同様）
- `${systemTime}` は完全一致のみ変換されます。文字列中に埋め込む場合は `CompositeInterpreter` との組み合わせが必要です
- `java.sql.Timestamp` 型カラムの期待値は末尾 `.0` が必須です（`"2024-01-01 09:00:00.0"`）。末尾 `.0` がないとアサートが失敗します

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
        EVENT_NAME: "NULLテスト"
        START_DATE: null
        CREATED_AT: null
      - ID: "3"
        EVENT_NAME: "システム時刻"
        START_DATE: "${systemTime}"
        CREATED_AT: "${systemTime}"
      - ID: "4"
        EVENT_NAME: "更新時刻"
        START_DATE: "${updateTime}"
        CREATED_AT: "${setUpTime}"
```

- NULL 値はアンクォートの `null` で記述します。`"null"` とクォートすると文字列として格納されます
- `java.sql.Timestamp` 型カラムの期待値は必ず末尾 `.0` を付けます

---

### 7.2 QuotationTrimmer によるスペース値明示記法

空白値やダブルクォート文字を明示して記述するケース。

#### Excel

| EXPECTED_TABLE=ITEM | | |
|---|---|---|
| ID | NAME | MEMO |
| 1 | " " | """ |

- `" "` → 半角スペース1文字
- `"""` → ダブルクォート1文字
- 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去します

#### YAML

```yaml
expected_tables:
  - table: ITEM
    rows:
      - ID: "1"
        NAME: " "
        MEMO: "\""
```

- YAML では `" "` と記述するとスペース1文字になります
- ダブルクォート文字は `"\""` または `'"'` で記述します

---

### 7.3 バイナリデータ

BLOB カラムにバイナリデータを記述するケース。

#### Excel

| SETUP_TABLE=FILE_TABLE | | |
|---|---|---|
| FILE_ID | FILE_DATA | |
| 001 | 0xCAFEBABE | |
| 002 | ${binaryFile:testdata.bin} | |

- `0x` プレフィクス付き16進数でバイナリ値を記述します
- `${binaryFile:パス}` でファイル内容をバイナリ読み込みして HexString に変換できます
- `0x` がない場合は文字列としてエンコードされます

#### YAML

```yaml
setup_tables:
  - table: FILE_TABLE
    rows:
      - FILE_ID: "001"
        FILE_DATA: "0xCAFEBABE"
      - FILE_ID: "002"
        FILE_DATA: "${binaryFile:testdata.bin}"
```

---

<a name="directive"></a>

## 8. ディレクティブ

### 8.1 固定長ファイルのディレクティブ

エンコーディングとゾーン10進数の符号ニブルを指定するケース。

#### Excel

| SETUP_FIXED=input/data.dat | | |
|---|---|---|
| text-encoding | MS932 | |
| positive-zone-sign-nibble | C | |
| DATA | USER_ID | AMOUNT |
| | X | Z |
| | 10 | 10 |
| | 001 | 5000 |

- ディレクティブ行は「キー | 値」の2セルで記述します
- `file-type` と `record-length` はフレームワークが自動設定するため通常は記述不要です

#### YAML

```yaml
setup_files:
  - path: input/data.dat
    type: fixed
    directives:
      text-encoding: MS932
      positive-zone-sign-nibble: C
    records:
      - record_type: DATA
        fields:
          - {name: USER_ID, type: X, length: 10}
          - {name: AMOUNT,  type: Z, length: 10}
        rows:
          - ["001", "5000"]
```

- `directives:` オブジェクトの `key: value` 形式で記述します
- 無効なディレクティブキーを指定すると `IllegalArgumentException` がスローされます

---

### 8.2 可変長ファイルのディレクティブ

タブ区切り・CRLF 改行のファイルを扱うケース。

#### Excel

| SETUP_VARIABLE=input/data.tsv | | |
|---|---|---|
| field-separator | \t | |
| record-separator | CRLF | |
| DATA | FIELD1 | FIELD2 |
| | X | X |
| | value1 | value2 |

- `field-separator` に `\t` を指定するとタブ文字になります
- `record-separator` には `NONE` / `CR` / `LF` / `CRLF` または任意リテラル文字列が有効です
- `field-separator` は1文字のみ有効です。2文字以上は `IllegalArgumentException` がスローされます

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

- `field-separator` のタブ文字は `"\\t"` と記述します（YAML の `\t` は実際のタブ文字になるため、バックスラッシュをエスケープします）

---

## 9. ヘッダ・コメント・空エントリ

### 9.1 コメントとマーカーカラム

#### Excel

| SETUP_TABLE=TEST_TABLE | | | | |
|---|---|---|---|---|
| // この行はコメントです | | | | |
| [no] | PK_COL1 | PK_COL2 | NUMBER_COL | [desc] |
| 1 | 0000000001 | AB | 100 | テスト1 |
| // この行もスキップされます | | | | |
| 2 | 0000000002 | CD | 200 | テスト2 |

- `//` で始まる行は丸ごとスキップされます（テスト実行に影響しません）
- `[no]`・`[desc]` のように角括弧で囲まれたカラムはマーカーカラムです。DB 操作から除外されます
- **行内コメント**: 先頭以外の要素が `//` で始まる場合、その要素以降が切り捨てられます（Excel 固有）

#### YAML

```yaml
setup_tables:
  - table: TEST_TABLE
    rows:
      # この行はコメントです（YAML の # 構文）
      - "[no]": "1"
        PK_COL1: "0000000001"
        PK_COL2: "AB"
        NUMBER_COL: "100"
        "[desc]": "テスト1"
      - "[no]": "2"
        PK_COL1: "0000000002"
        PK_COL2: "CD"
        NUMBER_COL: "200"
        "[desc]": "テスト2"
```

- YAML では標準のコメント構文（`#`）を使用します
- 行末コメントも使用できます: `NUMBER_COL: "100"  # 数値カラム`

---

### 9.2 空エントリのスキップ

全要素が null または空文字のエントリは読み飛ばされます。

#### Excel

| SETUP_TABLE=USER | | |
|---|---|---|
| USER_ID | NAME | |
| 001 | 山田太郎 | |
| | | |
| 002 | 鈴木花子 | |

- 全セルが空の行は自動的にスキップされます

#### YAML

YAML ではキーを省略するだけなので空エントリを記述する機会はほとんどありません。空行を挿入しても無視されます。

```yaml
setup_tables:
  - table: USER
    rows:
      - USER_ID: "001"
        NAME: "山田太郎"
      # 空行はここには書かない（YAML にはそもそも空エントリの概念がない）
      - USER_ID: "002"
        NAME: "鈴木花子"
```
