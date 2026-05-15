# NTF テストデータ YAML スキーマ設計メモ

## Excel概念 → YAML構造 対応表

| Excel概念 | YAML構造 | 備考 |
|---|---|---|
| `.xls` / `.xlsx` ファイル | 1つの `.yaml` ファイル | ファイル1:1対応 |
| シート | ファイル内のトップレベルセクション（`setup_tables:` 等） | シートのシート名概念は消滅 |
| データタイプ行（`SETUP_TABLE=...`） | セクションキー（`setup_tables`）+ 各要素の `table:` フィールド | 種別とテーブル名を分離して表現 |
| グループID（`[groupId]`） | `group_id:` フィールド（省略可） | 省略時はグループIDなし扱い |
| ヘッダ行（カラム名） | `rows` 内の各オブジェクトのキー | 各行ごとにキーを書くため冗長だが可読性高 |
| マーカーカラム（`[COLNAME]`） | `[COLNAME]` 形式のキー | 角括弧をキー名に含める |
| ディレクティブ行（`key\|value`） | `directives:` オブジェクト | 構造化されて型安全 |
| フィールド名行・データ型行・フィールド長行（3行1組） | `fields:` 配列の1要素（`name`/`type`/`length`） | 行分割をなくし1フィールド1定義に統合 |
| データ行（先頭空の行） | `rows:` 配列内の値配列 | `fields` と同順 |
| レコード種別（先頭セルが種別名） | `record_type:` フィールド | `records:` 配列の1要素 |
| コメント行（`//`始まり） | YAMLコメント（`#`） | YAML標準のコメント構文を使用 |

---

## 設計上のトレードオフと注意点

### 1. テーブルデータの行表現: オブジェクト形式 vs 配列形式

**採用: オブジェクト形式**（`{USER_ID: "001", NAME: 太郎}`）

| | オブジェクト形式（採用） | 配列形式 |
|---|---|---|
| 可読性 | 高い（カラム名が値に隣接） | 低い（カラム名と値が離れる） |
| AI書きやすさ | 高い（カラム名を都度確認不要） | 低い（列順を常に意識） |
| 冗長性 | 高い（カラム名が全行に繰り返される） | 低い |
| 一部カラム省略 | 自然（キーを書かなければ省略） | 不自然 |

テーブルデータは可読性とAI利用を優先してオブジェクト形式を採用。

### 2. ファイルデータの値表現: 配列形式

**採用: 配列形式**（`["val1", "val2"]`）

固定長・可変長ファイルのレコード値は `fields` と同順の配列で表現。  
理由: フィールド名は `fields` セクションに定義済みのため、各データ行でキー名を繰り返すと冗長かつ長くなる。ファイルデータは行数が多い傾向があるため配列形式で圧縮。

### 3. SETUP_FIXED と SETUP_VARIABLE の統合

Excelでは別のデータ種別だが、`BasicTestDataParser#getSetupFile()` が両者をまとめて返す実装に揃え、`type: fixed/variable` で区別する1つのセクションに統合した。`expected_files` も同様。

### 4. EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE の分離維持

両者は `getExpectedTableData()` でマージされて返されるが、`EXPECTED_COMPLETE_TABLE` が `fillDefaultValues()` を呼ぶかどうかの違いがある。YAMLでは `expected_tables` と `expected_complete_tables` を分けて保持し、変換時に呼び分けられるようにした。

### 5. マーカーカラムのキー名表現

Excel では `[COLNAME]` 形式のカラム名がマーカーとして扱われる（`HeaderLine` の規則）。  
YAMLでもキー名に `[COLNAME]` をそのまま使用する設計を採用。  
YAMLのキーには角括弧を含めることができる（クォート不要）。

### 6. 特殊値の表現

`NullInterpreter`・`QuotationTrimmer` 等の変換ルールはYAML側では変換**しない**。  
YAML値として以下のように記述し、Javaのパーサ側で既存の `Interpreter` チェーンが変換する設計を前提とする。

| 意図 | YAML記述 | 変換するクラス |
|---|---|---|
| DBにnull | `null`（YAMLのnull） | `NullInterpreter`（文字列`"null"`と等価） |
| 空文字 | `""` | `QuotationTrimmer` |
| システム日時 | `"${systemTime}"` | `DateTimeInterpreter` |

**注意**: YAMLの`null`（アンクォート）と文字列`"null"`は区別される。パーサ実装時にどちらを`NullInterpreter`相当とするか決定が必要。

### 7. グループIDなしの場合

Excel では `SETUP_TABLE=TABLE_NAME`（角括弧なし）がグループIDなしを意味する。  
YAMLでは `group_id:` フィールドを省略することで表現する。

### 8. SingleData系（LIST_MAP、MESSAGE）の制約

SingleData系は同一シート内でIDが一致した最初の1ブロックのみ取得する（`SingleDataParsingTemplate` の規則）。  
YAMLでは `id:` が重複した場合の動作はパーサ実装依存となる。設計として `id` はユニークとすることを推奨。

---

## 成果物ファイル一覧

| ファイル | 内容 |
|---|---|
| `ntf-testdata-structure.md` | Phase 1: コード調査報告（データ構造の完全な記述） |
| `ntf-testdata-yaml-schema.json` | Phase 2: JSON Schema定義 |
| `ntf-testdata-yaml-examples.yaml` | Phase 2: 各データ種別のYAML記述例 |
| `ntf-testdata-yaml-design.md` | Phase 2: 設計判断・トレードオフ（本ファイル） |
