# NTF テストデータ解説書 — 記述例（テーブルデータ）

<a name="table-data"></a>

## 5.1 テーブルデータの基本形式

<a name="setup-table"></a>

### SETUP_TABLE

会員テーブルへ初期データを INSERT するケース。

#### Excel

| SETUP_TABLE=MEMBER | | | | | | |
|---|---|---|---|---|---|---|
| MEMBER_ID | NAME | RANK | SCORE | RATE | PROFILE | PHOTO |
| 0000000101 | 山田太郎 | 1 | 85000 | 1.5 | ゴールド会員です | ${binaryFile:testdata.txt} |
| 0000000102 | 鈴木花子 | 2 | Null | 2.25 | シルバー会員 | ${binaryFile:member_photo.jpg} |

- カラム名を1行目に並べ、2行目以降にデータを記述します
- `//` で始まる行はコメントです（型情報・桁数などの注記に使われます）
- **主キーカラムは省略不可**です。省略すると `"0"` やスペースのデフォルト値が INSERT されます
- `NULL` 文字列は `NullInterpreter` により Java null に変換されます
- `${binaryFile:パス}` でファイル内容をバイナリ読み込みして HexString に変換できます

#### YAML

```yaml
setup_tables:
  - table: MEMBER
    rows:
      - MEMBER_ID: "0000000101"
        NAME: "山田太郎"
        RANK: "1"
        SCORE: "85000"
        RATE: "1.5"
        PROFILE: "ゴールド会員です"
        PHOTO: "${binaryFile:testdata.txt}"
      - MEMBER_ID: "0000000102"
        NAME: "鈴木花子"
        RANK: "2"
        SCORE: null
        RATE: "2.25"
        PROFILE: "シルバー会員"
        PHOTO: "${binaryFile:member_photo.jpg}"
```

- 各行がオブジェクトになりカラム名がキーになります
- 全値は文字列として記述します（`"0000000101"` のようにクォートします）
- NULL 値はアンクォートの `null` で記述します。`"null"` とクォートすると文字列として格納されます

---

<a name="expected-complete-table"></a>

### EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE

バッチ処理後の会員スコアと注文カウンタを検証するケース。

#### Excel

| EXPECTED_TABLE=MEMBER | | | | |
|---|---|---|---|---|
| MEMBER_ID | NAME | RANK | SCORE | UPDATED_DATE |
| 0000000101 | 山田太郎 | 1 | 87500 | 2024-04-01 09:00:00.0 |
| 0000000102 | 鈴木花子 | 2 | 42000 | 2024-04-01 09:00:00.0 |

| EXPECTED_COMPLETE_TABLE=ORDER_HEADER | | | |
|---|---|---|---|
| ORDER_ID | ITEM_COUNT | STATUS | UPDATE_DATE |
| 10001 | 3 | 1 | 2024-04-01 12:30:00.0 |
| 10002 | 5 | 1 | |

- `EXPECTED_TABLE`: 省略したカラムは比較対象外になります。検証したいカラムだけを列挙できます
- `EXPECTED_COMPLETE_TABLE`: 省略カラムには `BasicDefaultValues` のデフォルト値が補完されてから比較されます
- **混在禁止**: 同一ファイル内で `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を混在させると後半のデータが読み込まれません

#### YAML

```yaml
expected_tables:
  - table: MEMBER
    rows:
      - MEMBER_ID: "0000000101"
        NAME: "山田太郎"
        RANK: "1"
        SCORE: "87500"
        UPDATED_DATE: "2024-04-01 09:00:00.0"
      - MEMBER_ID: "0000000102"
        NAME: "鈴木花子"
        RANK: "2"
        SCORE: "42000"
        UPDATED_DATE: "2024-04-01 09:00:00.0"

expected_complete_tables:
  - table: ORDER_HEADER
    rows:
      - ORDER_ID: "10001"
        ITEM_COUNT: "3"
        STATUS: "1"
        UPDATE_DATE: "2024-04-01 12:30:00.0"
      - ORDER_ID: "10002"
        ITEM_COUNT: "5"
        STATUS: "1"
        # UPDATE_DATE を省略 → BasicDefaultValues のデフォルト値で補完されて比較
```

- 省略したいカラムのキーを書かないだけです
- `expected_tables:` と `expected_complete_tables:` は別キーのため混在可能です（YAMLパーサーが両方を独立して読み込んでマージします）

---

<a name="list-map"></a>

### LIST_MAP

キーバリュー形式の汎用データ。マーカーカラム・期待ログ・リクエストパラメータ等に使用します。

#### Excel — リクエストパラメータ（マーカーカラム付き）

注文検索画面の HTTP リクエストパラメータを定義するケース。

| LIST_MAP=searchParams | | | | | |
|---|---|---|---|---|---|
| [no] | memberId | orderStatus | fromDate | toDate | [desc] |
| 1 | 0000000101 | 1 | 2024-04-01 | 2024-04-30 | 4月注文検索 |
| 2 | 0000000102 | | 2024-01-01 | | 全件検索 |

- `[no]`・`[desc]` のように角括弧で囲まれたカラムはマーカーカラムです。DB 操作から除外されます
- マーカーカラムは Excel 上の見やすさのために使われることが多いです

#### Excel — 期待ログ

| LIST_MAP=expectedLog | | |
|---|---|---|
| message | logLevel | |
| 会員ID[0000000101]の注文を処理しました | INFO | |
| 会員ID[0000000102]の注文を処理しました | INFO | |

#### YAML

```yaml
list_maps:
  - id: searchParams
    rows:
      - "[no]": "1"
        memberId: "0000000101"
        orderStatus: "1"
        fromDate: "2024-04-01"
        toDate: "2024-04-30"
        "[desc]": "4月注文検索"
      - "[no]": "2"
        memberId: "0000000102"
        orderStatus: ""
        fromDate: "2024-01-01"
        toDate: ""
        "[desc]": "全件検索"
  - id: expectedLog
    rows:
      - message: "会員ID[0000000101]の注文を処理しました"
        logLevel: "INFO"
      - message: "会員ID[0000000102]の注文を処理しました"
        logLevel: "INFO"
```

- マーカーカラム `[no]`・`[desc]` は `"[no]"` とダブルクォートで囲みます（YAML の角括弧構文との衝突を避けるため）
- `testShots` は予約 ID です。フレームワークがテストケース定義として自動読み込みします
- ID は完全一致で検索されます。同一 ID の重複エントリは先着一致で 2 件目以降は無視されます
