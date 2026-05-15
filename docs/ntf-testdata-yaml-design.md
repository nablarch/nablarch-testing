# NTF テストデータ YAML スキーマ設計メモ

## Excel概念 → YAML構造 対応表

| Excel概念 | YAML構造 | 備考 |
|---|---|---|
| `.xls` / `.xlsx` ファイル | 1つの `.yaml` ファイル | Excelファイル1つが YAMLファイル1つに対応 |
| シート | ファイル内のトップレベルセクション（`setup_tables:` 等）にデータを記述 | シート名の概念は消滅。1ファイルに全種別データを共存可能 |
| データタイプ行（`SETUP_TABLE=...`） | セクションキー（`setup_tables`）+ 各要素の `table:` フィールド | 種別とテーブル名を分離して表現 |
| グループID（`[groupId]`） | `group_id:` フィールド（省略可） | 省略時はグループIDなし扱い |
| ヘッダ行（カラム名） | `rows` 内の各オブジェクトのキー | 各行ごとにキーを書くため冗長だが可読性高 |
| マーカーカラム（`[COLNAME]`） | `"[COLNAME]"` 形式のキー（ダブルクォートが必須） | YAMLで角括弧がフロー配列と誤解釈されないようクォートが必要 |
| ディレクティブ行（`key\|value`） | `directives:` オブジェクト | 構造化されて型安全 |
| フィールド名行・データ型行・フィールド長行（3行1組） | `fields:` 配列の1要素（`name`/`type`/`length`） | 行分割をなくし1フィールド1定義に統合 |
| データ行（先頭空の行） | `rows:` 配列内の値配列 | `fields` と同順 |
| レコード種別（先頭セルが種別名） | `record_type:` フィールド | `records:` 配列の1要素 |
| コメント行（`//`始まり） | YAMLコメント（`#`） | YAML標準のコメント構文を使用 |

---

## 変換ビフォーアフター（Excel → YAML）

### テーブルデータ

**Excel（シート上の表示）:**
```
行1: SETUP_TABLE[case1]=USER_TABLE
行2: USER_ID | USER_NAME | AGE | [MARKER]
行3: 001     | 山田太郎   | 30  | X
行4: 002     | 鈴木花子   | 25  | Y
```

**YAML（変換後）:**
```yaml
setup_tables:
  - group_id: case1
    table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        AGE: "30"
        "[MARKER]": "X"
      - USER_ID: "002"
        USER_NAME: "鈴木花子"
        AGE: "25"
        "[MARKER]": "Y"
```

### 固定長ファイル（Excel 6行 → YAML records 1ブロック）

**Excel（シート上の表示）:**
```
行1: SETUP_FIXED[grp1]=input/data.dat
行2: text-encoding | MS932          ← ディレクティブ行
行3: DATA    | USER_ID | USER_NAME | AMOUNT    ← フィールド名行（先頭がレコード種別名）
行4:         | X       | N         | Z         ← データ型行（先頭空）
行5:         | 10      | 20        | 10        ← フィールド長行（先頭空）
行6:         | 001     | 山田太郎   | 0000005000 ← データ行（先頭空）
```

**YAML（変換後）:**
```yaml
setup_files:
  - group_id: grp1
    path: input/data.dat
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
          - ["001       ", "山田太郎            ", "0000005000"]
```

**変換のポイント:**
- Excel の3行（フィールド名・型・長さ）が `fields:` の1要素に横方向統合される
- `rows:` の各配列は `fields` と完全に同じ順序・件数で値を並べること（列順ミスはパーサがランタイムエラーで検出）

---

## 設計上のトレードオフと注意点

### 1. テーブルデータの行表現: オブジェクト形式 vs 配列形式

**採用: オブジェクト形式**（`{USER_ID: "001", NAME: "太郎"}`）

| | オブジェクト形式（採用） | 配列形式 |
|---|---|---|
| 可読性 | 高い（カラム名が値に隣接） | 低い（カラム名と値が離れる） |
| AI書きやすさ | 高い（カラム名を都度確認不要） | 低い（列順を常に意識） |
| 冗長性 | 高い（カラム名が全行に繰り返される） | 低い |
| 一部カラム省略 | 自然（キーを書かなければ省略） | 不自然 |

カラム数が多い（15列以上）テーブルを大量行扱う場合はトークン消費が増えるが、可読性とAI利用を優先してオブジェクト形式を採用。

### 2. ファイルデータの値表現: 配列形式

**採用: 配列形式**（`["val1", "val2"]`）

固定長・可変長ファイルのレコード値は `fields` と同順の配列で表現。  
理由: フィールド名は `fields` セクションに定義済みのため、各データ行でキー名を繰り返すと冗長かつ長くなる。ファイルデータは行数が多い傾向があるため配列形式で圧縮。

**注意: テーブル系とファイル系で `rows` の形式が異なる**

| 種別 | `rows` の形式 | 例 |
|---|---|---|
| `setup_tables` / `expected_tables` / `list_maps` | **オブジェクト配列** | `[{COL: "val"}, ...]` |
| `setup_files` / `expected_files` / `messages` 等の `record_fragment` | **配列の配列** | `[["val1", "val2"], ...]` |

### 3. SETUP_FIXED と SETUP_VARIABLE の統合

Excelでは別のデータ種別だが、`BasicTestDataParser#getSetupFile()` が両者をまとめて返す実装に揃え、`type: fixed/variable` で区別する1つのセクションに統合した。`expected_files` も同様。

### 4. EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE の分離維持

両者は `getExpectedTableData()` でマージされて返されるが、`EXPECTED_COMPLETE_TABLE` が `fillDefaultValues()` を呼ぶかどうかの違いがある。YAMLでは `expected_tables` と `expected_complete_tables` を分けて保持し、変換時に呼び分けられるようにした。

### 5. マーカーカラムのキー名表現

Excel では `[COLNAME]` 形式のカラム名がマーカーとして扱われる（`HeaderLine` の規則）。  
YAMLでは `"[COLNAME]"` のようにダブルクォートで囲む必要がある。  
（クォートなしの `[COLNAME]: val` はYAMLパーサがフロー配列として誤解釈する）

### 6. 特殊値の表現と null の仕様

**null の仕様（確定）:** YAMLネイティブ `null`（アンクォート）を正式採用。

| 意図 | YAML記述 | 動作 |
|---|---|---|
| DBにNULL | `null` | YAMLパーサがJava nullとして渡す |
| DBに空文字 | `""` | 空文字列として渡す |
| 文字列 "null" をDBに格納 | `'"null"'` | QuotationTrimmerが外側クォートを除去して "null" を格納 |
| システム日時 | `"${systemTime}"` | DateTimeInterpreter が変換 |

**すべての値は文字列（クォート付き）で記述すること。** YAMLパーサが数値・真偽値として解釈するとスキーマバリデーション違反になる。

```yaml
# NG
rows:
  - AGE: 30
    ACTIVE: true
# OK
rows:
  - AGE: "30"
    ACTIVE: "true"
```

### 7. グループIDなしの場合

Excel では `SETUP_TABLE=TABLE_NAME`（角括弧なし）がグループIDなしを意味する。  
YAMLでは `group_id:` フィールドを省略することで表現する。

### 8. SingleData系（LIST_MAP、MESSAGE）の制約

SingleData系は同一ファイル内でIDが一致した最初の1ブロックのみ取得する（`SingleDataParsingTemplate` の規則）。  
`id:` はファイル内でユニークにすることを推奨。

### 9. GroupData系（RESPONSE_HEADER_MESSAGES 等）の id と group_id の区別

`GroupDataParsingTemplate#isTargetType()` は **`group_id`** でフィルタリングする。  
`id`（`=` 以降の値）はフィルタリングには使われず、識別子として記録されるのみ。  
`id` は `required` だが、GroupDataのフィルタリングには影響しない。

---

## 段階的移行戦略

### ExcelとYAMLの並存

現状のNTFパーサ（`PoiXlsReader` + `BasicTestDataParser`）はExcelのみを読み込む実装になっている。  
YAML対応のパーサを実装する際は、以下の段階的移行が可能な設計を推奨する。

1. **段階1: YAMLパーサの追加実装**  
   `TestDataReader` インタフェースを実装したYAMLパーサを新規作成。  
   既存の `PoiXlsReader` と共存させ、ファイル拡張子（`.yaml`/`.yml`）で切り替え。

2. **段階2: テストクラス単位での移行**  
   各テストクラスが参照するテストデータファイルをExcel→YAMLに1ファイルずつ変換。  
   変換ツール（Excel→YAML変換スクリプト）を整備して機械的に移行。

3. **段階3: Excelの廃止**  
   全ファイルのYAML移行完了後、`PoiXlsReader` への依存を削除。

### 移行優先度の基準

以下の順で移行を優先することを推奨する。

- **優先度高**: 更新頻度が高いExcelファイル（手書きコストが高い）
- **優先度高**: テーブルデータのみで構成されるシンプルなExcel（変換が容易）
- **優先度低**: 固定長ファイル定義が複雑なExcel（変換スクリプトの作り込みが必要）
- **後回し可**: 更新頻度が低く安定しているExcel（移行コストに見合わない）

### 変換ツール方針

自動変換スクリプトの実装時には以下に注意する。

- テーブル名・カラム名は `toUpperCase()` されているため、YAML側では大文字で出力する
- マーカーカラム（`[COLNAME]`）はYAMLキーとして `"[COLNAME]"` にクォートする
- Excel のセル値が空（`""`）でも意図的に空文字として出力する（省略しない）
- `null` セルは `null` として出力する

---

## AI向けプロンプト補助情報

このスキーマをAIにテストデータ生成させる際に一緒に渡すべき補助情報:

```
# NTF テストデータ YAML 生成ルール

## rows の形式の区別
- テーブル系（setup_tables / expected_tables / expected_complete_tables / list_maps）の rows は
  オブジェクト配列: [{COL: "val"}, ...]
- ファイル系（setup_files / expected_files / messages 等）の record_fragment の rows は
  配列の配列: [["val1", "val2"], ...]

## 値の型ルール
- すべての値は文字列型（ダブルクォート）で記述すること
- 数値・真偽値もクォートする: "30", "true"
- DBにNULLを入れる場合: null （YAMLキーワード、クォートなし）
- DBに空文字を入れる場合: "" （ダブルクォート2つ）

## record_fragment の列順保証
- records[].rows の各配列は、同ブロックの fields 配列と完全に同じ順序・同じ件数で値を並べること

## group_id の省略ルール
- グループIDがない場合は group_id フィールド自体を省略すること（null や "" は不可）

## マーカーカラム
- キー名を "[COLNAME]" と角括弧で囲みダブルクォートする
- 値は任意の文字列（マーキング用途。DB操作から除外される）

## 特殊値
- null（DB NULL）: null
- 空文字: ""
- システム日時: "${systemTime}"
- セットアップ時刻: "${setUpTime}"
- 文字種生成（例）: "${全角英字, 10}"
- バイナリファイル: "${binaryFile:path/to/file.bin}"
- CR文字: "\r"
```

---

## 成果物ファイル一覧

| ファイル | 内容 |
|---|---|
| `ntf-testdata-structure.md` | Phase 1: コード調査報告（データ構造の完全な記述） |
| `ntf-testdata-yaml-schema.json` | Phase 2: JSON Schema定義 |
| `ntf-testdata-yaml-examples.yaml` | Phase 2: 各データ種別のYAML記述例 |
| `ntf-testdata-yaml-design.md` | Phase 2: 設計判断・トレードオフ（本ファイル） |
| `tasks.md` | 作業タスクリスト（中断・再開用） |
