# NTF テストデータ記述例 — 5章: ファイルデータ

<a name="file-data"></a>

## 5.1 固定長ファイル

バッチリクエスト単体テストで固定長ファイルを読み書きするケース。実物のデータは `FileToFileBatchSampleTest.xls` の `testHandle` シートを参照。

### Excel

| SETUP_FIXED=work/input.txt | | | | |
|---|---|---|---|---|
| データ | ID | COUNTER | MESSAGE | |
| | 半角 | 数値 | 半角 | |
| | 5 | 5 | 10 | |
| | 10001 | 10 | hello | |
| | 10002 | 20 | good bye. | |

| EXPECTED_FIXED=work/output.txt | | | | |
|---|---|---|---|---|
| データ | ID | COUNTER | MESSAGE | |
| | 半角 | 数値 | 半角 | |
| | 5 | 5 | 10 | |
| | 10001 | 11 | HELLO | |
| | 10002 | 21 | GOOD BYE. | |

- 「レコード種別+フィールド名称行・データ型行・フィールド長行」の3行でフィールドを定義します
- データ行の先頭セルは空です（Excel 固有の制約: データの先頭要素は必ず空にする必要があります）
- データ値はパディングなしで記述します。フレームワークが自動付与します

### YAML

```yaml
setup_files:
  - path: work/input.txt
    type: fixed
    records:
      - record_type: データ
        fields:
          - {name: ID,      type: 半角, length: 5}
          - {name: COUNTER, type: 数値, length: 5}
          - {name: MESSAGE, type: 半角, length: 10}
        rows:
          - ["10001", "10", "hello"]
          - ["10002", "20", "good bye."]

expected_files:
  - path: work/output.txt
    type: fixed
    records:
      - record_type: データ
        fields:
          - {name: ID,      type: 半角, length: 5}
          - {name: COUNTER, type: 数値, length: 5}
          - {name: MESSAGE, type: 半角, length: 10}
        rows:
          - ["10001", "11", "HELLO"]
          - ["10002", "21", "GOOD BYE."]
```

- `fields:` 配列の1要素（`name`/`type`/`length`）にフィールド定義をまとめます
- `rows:` の各配列は `fields:` と**完全に同じ順序・件数**で値を並べます
- YAML では先頭要素を空にする制約はありません

---

## 5.2 エンコーディング指定付き固定長ファイル

ファイルエンコーディングを明示するケース。

### Excel

| SETUP_FIXED=input/data.dat | | | |
|---|---|---|---|
| text-encoding | MS932 | | |
| DATA | USER_ID | USER_NAME | AMOUNT |
| | X | N | Z |
| | 10 | 20 | 10 |
| | 001 | 山田太郎 | 5000 |
| | 002 | 鈴木花子 | 3000 |

- ディレクティブ行はレコード定義より前に記述します（「キー | 値」の2セル構成）

### YAML

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

- `directives:` オブジェクトの `key: value` 形式でディレクティブを記述します

---

## 5.3 groupId 付き固定長ファイル

複数テストケースで異なる入力ファイルを使い分けるケース。

### Excel

| SETUP_FIXED=work/input.txt | | | | |
|---|---|---|---|---|
| データ | ID | COUNTER | MESSAGE | |
| | 半角 | 数値 | 半角 | |
| | 5 | 5 | 10 | |
| | 10001 | 10 | hello | |

| SETUP_FIXED[case2]=work/input.txt | | | | |
|---|---|---|---|---|
| データ | ID | COUNTER | MESSAGE | |
| | 半角 | 数値 | 半角 | |
| | 5 | 5 | 10 | |
| | 20001 | 30 | morning | |

- `SETUP_FIXED[case2]=パス` のように groupId を指定します

### YAML

```yaml
setup_files:
  - path: work/input.txt
    type: fixed
    records:
      - record_type: データ
        fields:
          - {name: ID,      type: 半角, length: 5}
          - {name: COUNTER, type: 数値, length: 5}
          - {name: MESSAGE, type: 半角, length: 10}
        rows:
          - ["10001", "10", "hello"]
  - group_id: case2
    path: work/input.txt
    type: fixed
    records:
      - record_type: データ
        fields:
          - {name: ID,      type: 半角, length: 5}
          - {name: COUNTER, type: 数値, length: 5}
          - {name: MESSAGE, type: 半角, length: 10}
        rows:
          - ["20001", "30", "morning"]
```

---

## 5.4 可変長ファイル

CSV 形式のファイルを扱うケース。

### Excel

| SETUP_VARIABLE=input/data.csv | | | |
|---|---|---|---|
| field-separator | , | | |
| DATA | USER_ID | USER_NAME | AMOUNT |
| | X | N | X |
| | 001 | 山田太郎 | 5000 |
| | 002 | 鈴木花子 | 3000 |

### YAML

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

- 可変長では `length` が不要です。`fields:` の各要素から `length` を省略できます
- 固定長との差異は `type: fixed` / `type: variable` と `length` の有無だけです

---

<a name="multi-record"></a>

## 5.5 複数レコードレイアウト

1ファイルセクション内に HEADER レコードと DATA レコードが混在するケース。

### Excel

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

- 同一セクション内でレコード種別+フィールド名称行を続けて書くことで複数レコードレイアウトを表現します

### YAML

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

- `records:` 配列に複数のレコードレイアウトを並べます

---

<a name="empty-file"></a>

## 5.6 空ファイル

0バイトの空ファイルを表現するケース。

### Excel

| SETUP_FIXED=input/empty.dat | |
|---|---|
| text-encoding | MS932 |

- ディレクティブ行のみ記述してレコード定義以降を省略します

### YAML

```yaml
setup_files:
  - path: input/empty.dat
    type: fixed
    directives:
      text-encoding: MS932
    records: []
```

- `records: []` と空配列を記述します
