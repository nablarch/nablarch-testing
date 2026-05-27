# NTF テストデータ Excel ↔ YAML 双方向変換ツール 設計書

- **対象ツール**: `XlsToYamlConverter` / `YamlToXlsConverter`
- **作成日**: 2026-05-27
- **ステータス**: 設計確定

---

## 目次

1. [目的・スコープ](#1-目的スコープ)
2. [設計方針](#2-設計方針)
3. [フェーズ定義](#3-フェーズ定義)
4. [コメント行の扱い（Ph-2 の核心）](#4-コメント行の扱いph-2-の核心)
5. [変換対象ファイル](#5-変換対象ファイル)
6. [対応 NTF 仕様 ID](#6-対応-ntf-仕様-id)
7. [データモデル設計](#7-データモデル設計)
8. [クラス設計](#8-クラス設計)
9. [変換ルール詳細](#9-変換ルール詳細)
10. [実行方法](#10-実行方法)
11. [エラー処理方針](#11-エラー処理方針)

---

## 1. 目的・スコープ

### 1.1 目的

`src/test/` 配下に存在する NTF テストデータ Excel ファイル（`.xls` / `.xlsx`）を、NTF が読み込める YAML 形式へ一括変換する。また逆方向の変換（YAML → Excel）も提供する。

変換後は NTF が YAML ファイルから同等のテストデータを読み込めることが保証される。Excel 読み込みパス（`PoiXlsReader`）と YAML 読み込みパス（`YamlTestDataParser`）の両者が同じ NTF データモデルを生成することが、変換の等価性の根拠となる。

### 1.2 スコープ

| 対象 | 内容 |
|---|---|
| 変換対象（Excel → YAML） | `src/test/` 配下の NTF テストデータ Excel（詳細は [5章](#5-変換対象ファイル)） |
| 変換対象（YAML → Excel） | `src/test/` 配下の NTF テストデータ YAML（詳細は [5章](#5-変換対象ファイル)） |
| 変換対象外 | `src/main/resources/nablarch/test/core/http/dump/template.xls`（HTTP ダンプテンプレート）、`src/main/script/master_data/MASTER_DATA.xls`（DB 初期データ） |

---

## 2. 設計方針

### 2.1 データモデル中心の変換

Excel と YAML は NTF データモデルの表現形式に過ぎない。変換ツールは「ある形式でデータを読み込み、共通の中間データモデルに乗せ、別形式で書き出す」パイプラインとして設計する。

```
Excel シート
    ↓ ExcelSheetReader
中間データモデル (SheetModel)
    ↓ YamlSheetWriter
YAML ファイル
```

```
YAML ファイル
    ↓ YamlSheetReader
中間データモデル (SheetModel)
    ↓ ExcelSheetWriter
Excel シート
```

このアーキテクチャにより、Reader と Writer を独立してテスト・差し替えできる。

### 2.2 NTF 実装への非依存

変換ツールは NTF の内部クラス（`TestDataParsingTemplate`、`DataFile` 等）を直接呼び出さない。変換ツールが依存するのは以下に限定する。

- **Apache POI**: Excel ファイルの読み書き（`test` スコープで既存）
- **SnakeYAML Engine**: YAML ファイルの読み書き（`compile` スコープで既存）
- `PoiXlsReader.getSheetNames()`: Excel シート名一覧取得にのみ利用

変換ルールは `ntf-testdata-doc.md`（NTF 仕様書）を根拠とし、ツール内に自己完結した形で実装する。

### 2.3 既存ファイルの上書き禁止（デフォルト動作）

デフォルトでは出力先に既存ファイルがある場合はエラーで停止する。`--overwrite` オプションを指定した場合のみ上書きを許可する。これにより意図しないデータ損失を防ぐ。

### 2.4 変換の等価性の定義

「変換が等価である」とは、Excel シートから読み込んだ NTF データモデルと、変換後の YAML ファイルから読み込んだ NTF データモデルが論理的に同一であることを指す。セル書式・色・結合セル・コメントポップアップ等、`PoiXlsReader` が無視する Excel 固有要素は等価性の評価対象外とする。

---

## 3. フェーズ定義

変換対象データを2つのフェーズに分けて整理する。

### Ph-1: NTF データモデル変換（本実装の主対象）

NTF 仕様（`ntf-impl-spec-list.md` 145件）として定義されているデータ要素をすべて変換対象とする。

| カテゴリ | 変換対象要素 |
|---|---|
| DT | セクション識別行（`DataType名[groupId]=値`）の解析・生成 |
| SS | テーブルデータ（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE / LIST_MAP）、ファイルデータ（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）のヘッダ・データ行 |
| HC | ヘッダ行・マーカーカラム・空行スキップ |
| IV | セル値のそのまま転写（インタープリタはランタイム側の責務であり変換ツールは適用しない） |
| DR | ディレクティブ行のキー・値 |
| MS | メッセージングテストデータ（MESSAGE / EXPECTED_REQUEST_HEADER_MESSAGES / EXPECTED_REQUEST_BODY_MESSAGES / RESPONSE_HEADER_MESSAGES / RESPONSE_BODY_MESSAGES） |

### Ph-2: NTF 仕様外要素（コメント行）

`PoiXlsReader` が処理するが NTF データモデルに含まれない要素。詳細は [4章](#4-コメント行の扱いph-2-の核心) で扱う。

---

## 4. コメント行の扱い（Ph-2 の核心）

### 4.1 前提事実

Excel のコメント行（先頭セルが `//` で始まる行）は `PoiXlsReader` の `readOneLine()` で読み取られるが、`readLine()` メソッドからは返却されない（`isBlankLine()` による空行スキップとは別の処理として、上位の `TestDataParsingTemplate` に渡らず読み捨てられる）。したがってコメント行は NTF の動作に一切影響しない。

YAML の `#` コメントは SnakeYAML Engine パーサーレベルで破棄され、`YamlLoader` には渡らない。

### 4.2 ラウンドトリップ可否の分析

| 変換方向 | コメント変換の可否 | 理由 |
|---|---|---|
| Excel → YAML | 技術的には可能（`//` 行を `#` コメントとして書き出せる） | ただし `YamlLoader` はコメントを読み込まないため YAML → NTF の等価性は維持される |
| YAML → Excel | 不可能 | SnakeYAML Engine がパース時にコメントを破棄するため、変換ツールはコメント内容を取得できない |

YAML → Excel の変換でコメントが失われる以上、Excel → YAML で `#` コメントを出力しても、YAML を経由した再変換（YAML → Excel）でそのコメントは復元できない。すなわちコメントを含めた真のラウンドトリップは不可能である。

### 4.3 設計上の結論

**コメント行（`//` で始まる行）は変換対象外とし、変換時にロストさせる。**

根拠は以下の通り。

1. NTF の動作に影響しないため、変換等価性の観点からロストは許容される。
2. Excel → YAML でコメントを `#` として出力すると、その YAML から再度 YAML → Excel へ変換した際にコメントが消える非対称な動作が生じ、ツールの動作が混乱する。
3. 双方向変換の一貫性（どちらの向きに変換しても同じ結果になる性質）を保つためには、両方向でコメントをロストさせる対称な動作が適切である。

変換実行時に Excel ファイル中にコメント行が存在した場合は、標準エラー出力に警告メッセージを1件出力する（コメント行の総数と代表的なファイル名を含む）。変換自体は継続する。

---

## 5. 変換対象ファイル

### 5.1 変換対象となるファイルの条件

以下の条件をすべて満たすファイルが変換対象となる。

- **Excel → YAML**: `src/test/` 配下のディレクトリツリーに存在する `.xls` または `.xlsx` ファイル
- **YAML → Excel**: `src/test/` 配下のディレクトリツリーに存在する `.yaml` ファイル

### 5.2 明示的な変換対象外ファイル

パスで一致する以下のファイルは変換対象から除外する。除外判定は絶対パスの末尾部分による後方一致で行う。

| ファイル | 除外理由 |
|---|---|
| `src/main/resources/nablarch/test/core/http/dump/template.xls` | HTTP ダンプテンプレートであり NTF テストデータではない |
| `src/main/script/master_data/MASTER_DATA.xls` | DB 初期データであり NTF テストデータではない |

これらのファイルが入力パスに含まれる場合、ツールはそのファイルをスキップし、標準エラー出力に INFO レベルのスキップメッセージを出力する。

### 5.3 ディレクトリ対応規則（Excel ↔ YAML）

NTF の Excel と YAML はファイルシステム上の配置が異なる（`ntf-testdata-doc.md` 2章参照）。

| 形式 | 配置規則 | リソース名 |
|---|---|---|
| Excel | `{テストクラスと同名}.xls` ファイル。シートが読み込み単位 | `ファイル名/シート名` |
| YAML | `{テストクラスと同名}/` ディレクトリ。1ファイルが1読み込み単位 | `ファイル名`（`.yaml` 拡張子を除いた名前） |

**Excel → YAML の変換規則**:

```
{basePath}/FooTest.xls（シート: case01, case02）
    ↓
{outputPath}/FooTest/case01.yaml
{outputPath}/FooTest/case02.yaml
```

**YAML → Excel の変換規則**:

```
{basePath}/FooTest/case01.yaml
{basePath}/FooTest/case02.yaml
    ↓
{outputPath}/FooTest.xls（シート: case01, case02）
```

YAML → Excel 変換では、同一ディレクトリ内の複数 YAML ファイルを1つの Excel ブックにまとめる。シートの順序はファイル名のアルファベット昇順とする。

---

## 6. 対応 NTF 仕様 ID

変換ツールが変換対象として扱う仕様 ID を以下に示す。`ntf-impl-spec-list.md` の仕様 ID 体系に従う。

### 6.1 変換対象（Ph-1）

| カテゴリ | 対象仕様 ID | 非対象 ID と理由 |
|---|---|---|
| DT | DT-01, DT-02, DT-03, DT-04, DT-05, DT-06, DT-07 | DT-08（引数エラーはランタイム挙動・変換ツールには不要） |
| SS | SS-01〜SS-13, SS-15〜SS-17, SS-19, SS-20 | SS-14（重複フィールド名エラー）・SS-21〜SS-30（ランタイムエラー）は変換ツールの責務外 |
| HC | HC-01, HC-02, HC-03, HC-04, HC-07 | HC-05, HC-06（コメント行は変換対象外。[4章](#4-コメント行の扱いph-2-の核心)参照） |
| IV | IV-01〜IV-15 | インタープリタはランタイム側の責務。変換ツールはセル値を文字列としてそのまま転写するのみ |
| DR | DR-01〜DR-10 | DR-11, DR-12（ランタイムエラー） |
| MS | MS-01〜MS-09, MS-11〜MS-13 | MS-10（no 列複数回送信はランタイム動作）・MS-14（ランタイムエラー） |

### 6.2 変換対象外（Ph-2 / スコープ外）

| カテゴリ | 仕様 ID | 理由 |
|---|---|---|
| HC | HC-05, HC-06 | コメント行は変換対象外（[4章](#4-コメント行の扱いph-2-の核心)で結論） |
| TS | TS-01〜TS-34 | テストサポート層（上位層）の仕様であり、テストデータ形式の変換ツールのスコープ外 |
| RS | RS-01〜RS-22 | YAML リーダー実装仕様。変換ツールは YAML の読み書きに SnakeYAML Engine を直接使用するため、これらの仕様はツール内部で別途実装する |

---

## 7. データモデル設計

変換ツールの中間データモデル（`SheetModel`）を以下に定義する。このモデルは Excel シートと YAML ファイルの両者を表現できる共通の中間表現である。

### 7.1 SheetModel

読み込み単位（Excel の1シート / YAML の1ファイル）に対応する中間データモデル。

```
SheetModel
  sections: List<SectionModel>
```

### 7.2 SectionModel

1セクションに対応するモデル。セクション種別（`DataType`）・groupId・識別子の値・行データを保持する。

```
SectionModel
  dataType: DataType          // DT-01 の14種
  groupId: String             // DT-06。省略時は空文字
  identifier: String          // テーブル名・ファイルパス・ID 等
  rows: List<List<String>>    // ヘッダ行を含む全行（先頭行がヘッダ）
```

`rows` はセクション識別行を除いたデータ（ヘッダ行＋データ行）を保持する。セクション識別行の情報は `dataType`・`groupId`・`identifier` フィールドに分解して格納する。

### 7.3 BookModel

Excel ブック（または YAML ディレクトリ）単位の集約モデル。

```
BookModel
  name: String                // テストクラス名（ファイル名から拡張子を除いたもの）
  sheets: List<SheetEntry>

SheetEntry
  sheetName: String           // シート名（YAML ではファイル名から拡張子を除いたもの）
  sheet: SheetModel
```

---

## 8. クラス設計

### 8.1 パッケージ構成

```
nablarch.test.core.reader.converter
  XlsToYamlConverter          // Excel → YAML 変換エントリポイント（main メソッド）
  YamlToXlsConverter          // YAML → Excel 変換エントリポイント（main メソッド）
  model
    BookModel                 // ブック単位集約モデル
    SheetModel                // シート単位中間データモデル
    SectionModel              // セクション単位データモデル
    SheetEntry                // シート名とシートモデルの対
  reader
    ExcelBookReader           // Excel ブックを BookModel として読み込む
    YamlSheetReader           // YAML ファイルを SheetModel として読み込む
  writer
    YamlSheetWriter           // SheetModel を YAML ファイルとして書き出す
    ExcelBookWriter           // BookModel を Excel ブックとして書き出す
  util
    ConverterFileFilter       // 変換対象外ファイルの除外ロジック
    ConverterPathResolver     // ファイルパス解決ユーティリティ
```

### 8.2 各クラスの責務

#### XlsToYamlConverter

`main` メソッドを持つエントリポイント。コマンドライン引数を解析し、変換処理を統括する。

- 引数解析・バリデーション
- 変換対象ファイルの列挙（`ConverterFileFilter` を使用）
- `ExcelBookReader` で BookModel を構築
- `YamlSheetWriter` で YAML ファイルを書き出す
- コメント行の警告出力

#### YamlToXlsConverter

`main` メソッドを持つエントリポイント。

- 引数解析・バリデーション
- 変換対象ファイルの列挙（同一ディレクトリの YAML をグルーピング）
- `YamlSheetReader` で SheetModel を構築
- `ExcelBookWriter` で Excel ブックを書き出す

#### ExcelBookReader

Apache POI を使用して Excel ファイルを読み込み、`BookModel` を返す。

- `PoiXlsReader` の `getSheetNames()` を使用してシート一覧を取得
- 各シートのセルを文字列リストとして読み込む
- コメント行（先頭セルが `//`）を検出してカウントし、スキップする
- 空行（全セル空）をスキップする
- セクション識別行を解析して `SectionModel` を構築する
- セクション識別行の判定は前方一致（`DataType.getDataType()` の仕様に従う）

#### YamlSheetReader

SnakeYAML Engine を使用して YAML ファイルを読み込み、`SheetModel` を返す。

- トップレベルキーを `YamlSection` の定数に照合してセクション種別を決定
- 各セクションエントリから `group_id`・`id`（または `table` / `path`）・`rows` を取得して `SectionModel` を構築

#### YamlSheetWriter

`SheetModel` を YAML ファイルとして書き出す。

- `YamlSection` のセクションキー定数に従って YAML 構造を生成
- SnakeYAML Engine の `Dump` クラスを使用して YAML 文字列を生成
- 全データ値はダブルクォートで囲む（YAML での型変換を防ぐ）
- `null` 値はアンクォートの `null` として書き出す（RS-03 準拠）

#### ExcelBookWriter

`BookModel` を Excel ブックとして書き出す。

- Apache POI の `XSSFWorkbook` を使用（xlsx 形式で出力）
- 全セルを文字列書式に設定する
- シートの順序は `BookModel.sheets` の順序に従う

#### ConverterFileFilter

変換対象外ファイルを除外するフィルタ。

- 絶対パスの後方一致で除外パスを判定する
- 除外パスはコンストラクタ引数または設定ファイルで指定可能とする（デフォルトは `template.xls`・`MASTER_DATA.xls`）

#### ConverterPathResolver

Excel ↔ YAML のパス対応を解決するユーティリティ。

- Excel ファイルパス → YAML ディレクトリパスの変換
- YAML ファイルパス群 → Excel ファイルパスの変換

---

## 9. 変換ルール詳細

### 9.1 セクション識別行の変換

#### Excel → YAML

`DataType名[groupId]=識別子` の形式を解析し、以下の YAML フィールドにマッピングする。

| Excel の要素 | YAML への展開 |
|---|---|
| `DataType名` | トップレベルキー（`YamlSection` の定数から解決） |
| `[groupId]`（省略可） | `group_id:` フィールド（省略時は出力しない） |
| `=` 以降の値 | `table:` / `path:` / `id:` のいずれか（DataType に応じて決定） |

DataType と YAML トップレベルキーの対応は `ntf-testdata-doc.md` 3.1節の表に従う。

識別子フィールドの種別は以下の通り。

| DataType | 識別子フィールド |
|---|---|
| `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` | `table:` |
| `SETUP_FIXED` / `SETUP_VARIABLE` / `EXPECTED_FIXED` / `EXPECTED_VARIABLE` | `path:` |
| `LIST_MAP` / `MESSAGE` / `EXPECTED_REQUEST_*` / `RESPONSE_*` | `id:` |

#### YAML → Excel

上記の逆変換。YAML のトップレベルキーから `DataType` を解決し、`DataType名[groupId]=識別子` 形式のセル値を生成する。`group_id` が空文字または存在しない場合は `[groupId]` を省略する。

### 9.2 テーブルデータ（SS カテゴリ）の変換

#### Excel → YAML

```
行1: SETUP_TABLE=USER_MASTER
行2: COL1  COL2  COL3
行3: val1  val2  val3
```

↓

```yaml
setup_tables:
  - table: "USER_MASTER"
    rows:
      - COL1: "val1"
        COL2: "val2"
        COL3: "val3"
```

- ヘッダ行末尾の空カラムは除去する（HC-03）
- データ行がヘッダより短い場合の補完は行わない（変換元データの記述に忠実に変換する。補完はランタイム側の責務）
- マーカーカラム（`[カラム名]` 形式）はそのまま出力する（HC-01。マーカー除外はランタイム側の責務）
- 空行（全セル空）はスキップする（HC-07）

#### YAML → Excel

上記の逆変換。`rows:` の各エントリをヘッダ行の順序に従ってセルに展開する。YAML エントリのキー順序はファイル内の記述順序に従う。

### 9.3 LIST_MAP の変換

#### Excel → YAML

```
行1: LIST_MAP=testShots
行2: no  description  status
行3: 1   正常系        active
```

↓

```yaml
list_maps:
  - id: "testShots"
    rows:
      - no: "1"
        description: "正常系"
        status: "active"
```

### 9.4 ファイルデータ（SS-08〜SS-17、DR カテゴリ）の変換

ファイルセクションは行の順序が意味を持つ（ディレクティブ行 → フィールド名行 → データ型行 → [フィールド長行] → データ行）ため、状態機械として解析する。

#### Excel → YAML（固定長ファイル）

Excel のファイルセクションは「ディレクティブ行（キー/値が先頭2列）→ レコード種別+フィールド名行 → データ型行 → フィールド長行 → データ行（先頭セルは空）」の順で構成される。

```
行1: SETUP_FIXED=input/data.dat [空]       [空]      [空]
行2: text-encoding              MS932      [空]      [空]
行3: DATA                       USER_ID    AMOUNT    [空]
行4: [空]                       X          Z         [空]
行5: [空]                       10         10        [空]
行6: [空]                       001        5000      [空]
```

↓

```yaml
setup_files:
  - path: "input/data.dat"
    type: "fixed"
    directives:
      text-encoding: "MS932"
    records:
      - record_type: "DATA"
        fields:
          - {name: "USER_ID", type: "X", length: "10"}
          - {name: "AMOUNT",  type: "Z", length: "10"}
        rows:
          - ["001", "5000"]
```

#### Excel → YAML（可変長ファイル）

固定長との差異は `type: "variable"` であり、`fields:` に `length:` が含まれない点のみ。

```yaml
setup_files:
  - path: "work/input.csv"
    type: "variable"
    directives:
      text-encoding: "UTF-8"
    records:
      - record_type: "DATA"
        fields:
          - {name: "COL1", type: "X"}
          - {name: "COL2", type: "X"}
        rows:
          - ["val1", "val2"]
```

#### 複数レコードレイアウトの変換

1ファイルセクション内に複数レコードレイアウトが存在する場合（SS-11）、`records:` リストに複数エントリとして展開する。

### 9.5 メッセージングデータ（MS カテゴリ）の変換

メッセージングセクションの行構造は、FW 制御ヘッダ行・フィールド名行・データ型行・データ行の順序で構成される。FW 制御ヘッダ（`requestId` 等）は `record_type: FW_HEADER` として YAML に出力する。

`no` カラム（先頭列）と `errorMode`（MS-02, MS-04）はデータ値としてそのまま転写する（除去はランタイム側の責務）。

### 9.6 値の変換ルール

| Excel のセル値 | YAML への変換 |
|---|---|
| 通常の文字列 `abc` | `"abc"`（ダブルクォートで囲む） |
| 空セル（空文字） | `""`（空文字のダブルクォート） |
| `null`（大文字小文字不問） | `null`（アンクォート） |
| その他すべての文字列 | ダブルクォートで囲む |

YAML → Excel の逆変換では、YAML のダブルクォートで囲まれた値からクォートを除去してセルに書き込む。`null`（アンクォート）は `null` という文字列としてセルに書き込む。

インタープリタ（`${systemTime}` 等の特殊値）は変換ツールで解釈せず、文字列として転写する。

### 9.7 groupId の変換（DT-06）

Excel のセクション識別行 `SETUP_TABLE[case01]=USER_MASTER` は以下のように変換する。

```yaml
setup_tables:
  - group_id: "case01"
    table: "USER_MASTER"
    rows: ...
```

groupId が省略された場合（`SETUP_TABLE=USER_MASTER`）は `group_id:` フィールドを出力しない。

逆方向（YAML → Excel）では、`group_id:` フィールドが存在する場合は `[groupId]` を付加し、存在しない場合は省略する。

---

## 10. 実行方法

### 10.1 pom.xml への追加設定

`exec-maven-plugin` を `pom.xml` の `<build><plugins>` に追加する。

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
</plugin>
```

### 10.2 Excel → YAML 変換コマンド

```
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.XlsToYamlConverter \
  -Dexec.args="[オプション] <入力ディレクトリ> <出力ディレクトリ>"
```

**引数仕様**:

| 引数 | 必須 | 説明 |
|---|---|---|
| `<入力ディレクトリ>` | 必須 | Excel ファイルを再帰検索するルートディレクトリ（例: `src/test/java`） |
| `<出力ディレクトリ>` | 必須 | YAML ファイルを出力するルートディレクトリ（例: `src/test/java`）。入力と同一ディレクトリを指定した場合はすでに YAML が存在するシートをスキップする |

**オプション仕様**:

| オプション | 説明 |
|---|---|
| `--overwrite` | 出力先に既存ファイルがある場合に上書きを許可する（デフォルト: 上書き禁止でエラー停止） |
| `--dry-run` | 実際にファイルを書き出さずに変換対象の一覧を標準出力に表示する |

**実行例**（入力と出力を同一ディレクトリに指定する場合）:

```
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.XlsToYamlConverter \
  -Dexec.args="src/test/java src/test/java"
```

### 10.3 YAML → Excel 変換コマンド

```
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.YamlToXlsConverter \
  -Dexec.args="[オプション] <入力ディレクトリ> <出力ディレクトリ>"
```

**引数仕様**は Excel → YAML と同一（入力と出力を入れ替えた形）。出力は `.xlsx` 形式で書き出す。

**オプション仕様**は Excel → YAML と同一。

### 10.4 終了コード

| 終了コード | 意味 |
|---|---|
| `0` | 正常終了（変換対象ファイルが0件の場合も含む） |
| `1` | 引数エラー・入力ファイル読み込みエラー・出力ファイル書き込みエラー |

---

## 11. エラー処理方針

### 11.1 変換中止条件

以下の状況では変換を中止し、終了コード `1` で終了する。

- 入力ディレクトリが存在しない
- 出力ディレクトリが存在しないかつ作成に失敗した
- 個別ファイルの読み込み中に例外が発生した（破損ファイル等）
- `--overwrite` なしで出力先に既存ファイルが存在する

### 11.2 警告のみで継続する条件

以下の状況では標準エラー出力に警告を出力し、変換を継続する。

- Excel ファイルにコメント行（`//`）が存在した（[4章](#4-コメント行の扱いph-2-の核心)参照）
- 変換対象外ファイル（`template.xls`・`MASTER_DATA.xls`）が入力パスに含まれていた

### 11.3 エラーメッセージのフォーマット

エラーおよび警告メッセージは標準エラー出力（stderr）に以下の形式で出力する。

```
[ERROR] <ファイルパス>: <エラー内容>
[WARN]  <ファイルパス>: <警告内容>
[INFO]  <ファイルパス>: <情報>
```
