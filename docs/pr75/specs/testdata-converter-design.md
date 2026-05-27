# NTF テストデータ形式間変換ツール 設計書

- **作成日**: 2026-05-27
- **対象ブランチ**: convert-testdata-excel-to-text

---

## 目次

1. [目的・スコープ](#1-目的スコープ)
2. [設計方針](#2-設計方針)
3. [フェーズ定義](#3-フェーズ定義)
4. [変換対象ファイル](#4-変換対象ファイル)
5. [対応 NTF 仕様 ID](#5-対応-ntf-仕様-id)
6. [データモデル設計](#6-データモデル設計)
7. [クラス設計](#7-クラス設計)
8. [変換ルール詳細](#8-変換ルール詳細)
9. [実行方法](#9-実行方法)
10. [エラー処理方針](#10-エラー処理方針)

---

## 1. 目的・スコープ

### 1.1 目的

`src/test/` 配下に配置されている NTF（Nablarch Testing Framework）テストデータの Excel ファイル（`.xls`）を YAML ファイルに一括変換する。また将来的な逆方向変換（YAML → Excel）にも対応できる設計とする。

変換後は Excel ファイルを削除し、テストが YAML ファイルのみで動作することを確認する。これにより、Excel に依存しない YAML ベースのテストデータ管理体制へ移行する。

### 1.2 スコープ

**変換ツールがカバーすること**

- Excel（`.xls`）→ YAML（`.yaml`）への一括変換
- YAML（`.yaml`）→ Excel（`.xls`）への一括逆変換（将来対応として設計に含める）
- 変換対象は `src/test/` 配下のテストデータ Excel 59 件

**変換ツールがカバーしないこと**

- テストの実行・検証（NTF 本体の責務）
- `src/main/resources/nablarch/test/core/http/dump/template.xls`（HTTP ダンプテンプレート）の変換
- `src/main/script/master_data/MASTER_DATA.xls`（DB 初期データ）の変換
- Excel のセル書式・色・結合セル・コメントポップアップ等の変換（NTF 本体が無視するため）

---

## 2. 設計方針

### 2.1 データモデル中心設計

変換ツールは「Excel を読む」「YAML を書く」という形で直接変換するのではなく、形式非依存の中間データモデルを中心に設計する。Reader が中間データモデルに変換し、Writer が中間データモデルから出力形式に変換する。これにより、将来 CSV・JSON 等の新形式を追加しても既存の Reader/Writer を変更せずに済む。

```
Excel → [XlsFormatReader] → BookModel → [YamlFormatWriter] → YAML
YAML  → [YamlFormatReader] → BookModel → [XlsFormatWriter] → Excel
```

### 2.2 形式名をクラス名に入れない原則

将来の形式追加を見越し、インターフェース名・抽象クラス名に形式名（XLS/YAML/CSV 等）を含めない。

- `TestDataFormatReader`（形式非依存のインターフェース）
- `TestDataFormatWriter`（形式非依存のインターフェース）

実装クラスは形式名を含めてよい（`XlsFormatReader`、`YamlFormatWriter` 等）。

### 2.3 NTF 内部クラス非依存

変換ツールは NTF テストデータのパース処理（`BasicTestDataParser`、`TableData`、`DataFile` 等）を再利用しない。これらは「テストデータを読み込んでテストを実行する」という別の責務を持っており、変換ツールの「形式間でデータを忠実に変換する」責務とは異なる。変換ツールは独立したデータモデルを持つ。

### 2.4 上書き禁止デフォルト

既に変換先ファイルが存在する場合、デフォルト動作は上書きせずにエラーとして扱う（終了コード 1）。明示的に `--overwrite` オプションを指定した場合のみ上書きを許可する。誤操作による既存データの消失を防ぐ。

### 2.5 変換等価性の定義

変換における「等価」とは「NTF が読み込んだとき同じデータオブジェクトが生成されること」と定義する。以下は等価の範囲外とする。

- コメント行（`//`）: NTF が読み捨てるため、変換でもロストしてよい（[3章](#3-フェーズ定義) 参照）
- Excel のセル書式・色・結合セル: NTF が無視するため、変換ツールも無視する
- YAML ファイル内のコメント（`#`）: SnakeYAML がパース時に破棄するため、逆変換でロストしてよい

---

## 3. フェーズ定義

### Ph-1: NTF データモデル変換（基本変換）

NTF が読み込む全セクション種別（`SETUP_TABLE`、`EXPECTED_TABLE`、`EXPECTED_COMPLETE_TABLE`、`LIST_MAP`、`SETUP_FIXED`、`SETUP_VARIABLE`、`EXPECTED_FIXED`、`EXPECTED_VARIABLE`、`MESSAGE`、`EXPECTED_REQUEST_HEADER_MESSAGES`、`EXPECTED_REQUEST_BODY_MESSAGES`、`RESPONSE_HEADER_MESSAGES`、`RESPONSE_BODY_MESSAGES`）について、Excel ↔ YAML 間の変換を実装する。

このフェーズで変換等価性（NTF が同一データオブジェクトを生成すること）を保証する。

### Ph-2: コメント行のロスト（両方向）

コメント行（Excel の `//` 行）は NTF の動作に影響しないが、変換時にロストする。

**方針: 両方向でロストする**

- Excel → YAML 変換: `//` 行を読み捨てる（YAML に出力しない）
- YAML → Excel 変換: YAML コメント（`#`）は SnakeYAML がパース時に破棄するため、Excel に出力できない

一方向だけ保持すると変換の対称性が崩れ、ツールの動作が混乱する。両方向でロストとすることで動作を単純化する。変換実行時に対象ファイルのコメント行数を警告メッセージとして標準エラー出力し、処理は継続する。

---

## 4. 変換対象ファイル

### 4.1 対象ファイル

`src/test/` 配下の `.xls` ファイル 59 件。

### 4.2 除外ファイル

以下のファイルは変換対象から除外する。

| ファイルパス | 除外理由 |
|---|---|
| `src/main/resources/nablarch/test/core/http/dump/template.xls` | HTTP ダンプテンプレート。NTF テストデータではない |
| `src/main/script/master_data/MASTER_DATA.xls` | DB 初期データ。NTF テストデータではない |
| `src/test/java/MASTER_DATA.xls` | テスト用 DB マスタデータ。変換ツールの対象となる NTF テストデータではない |
| `src/test/java/MASTER_DATA2.xls` | テスト用 DB マスタデータ。変換ツールの対象となる NTF テストデータではない |
| `src/test/resources/nablarch/test/core/db/masterdata/MASTER_DATA.xls` | テスト用 DB マスタデータ。変換ツールの対象となる NTF テストデータではない |
| `src/test/resources/nablarch/test/core/db/masterdata/MASTER_DATA2.xls` | テスト用 DB マスタデータ。変換ツールの対象となる NTF テストデータではない |

### 4.3 ディレクトリ対応規則

#### Excel → YAML

Excel ブック内の各シートを個別の YAML ファイルに変換する。出力先ディレクトリはブック名（拡張子なし）と同名のディレクトリとする。

```
{inputPath}/com/example/FooTest.xls（シート: case01, case02）
    ↓
{outputPath}/com/example/FooTest/case01.yaml
{outputPath}/com/example/FooTest/case02.yaml
```

`inputPath` と `outputPath` が同一の場合、変換後に元の Excel ファイルを残す（`--delete-source` オプション指定時のみ削除する）。

#### YAML → Excel

同一ディレクトリ内の YAML ファイル群をまとめて 1 Excel ブックに変換する。シート順はファイル名のアルファベット昇順とする。

```
{inputPath}/com/example/FooTest/case01.yaml
{inputPath}/com/example/FooTest/case02.yaml
    ↓
{outputPath}/com/example/FooTest.xls（シート: case01, case02）
```

### 4.4 resourceName の対応

NTF は形式によって異なる resourceName で識別する。

| 形式 | resourceName の形式 | 例 |
|---|---|---|
| Excel | `ファイル名/シート名`（拡張子なし） | `FooTest/case01` |
| YAML | `ディレクトリ名/ファイル名`（拡張子なし） | `FooTest/case01` |

変換後も resourceName が変わらないよう、ファイル名・シート名・ディレクトリ名・YAML ファイル名を対応させる。

---

## 5. 対応 NTF 仕様 ID

変換ツールが正しく動作するために準拠する NTF 仕様 ID の一覧。

### 5.1 主対象（変換ツールが直接実装する仕様）

| カテゴリ | 仕様 ID | 概要 |
|---|---|---|
| DT | DT-01 | DataType は `DEFAULT` を含む 14 エントリ。変換ツールが変換対象とするのは `DEFAULT` を除く 13 種（SETUP_TABLE〜RESPONSE_BODY_MESSAGES） |
| DT | DT-02 | セクション識別行の書式 `<DataType名>[groupId]=<値>` |
| DT | DT-03 | DataType 判定は前方一致（`startsWith`）|
| DT | DT-06 | groupId 書式 `[groupId]`（省略時は空文字扱い） |
| SS | SS-08 | ファイルセクションの行順序（ディレクティブ → フィールド名 → データ型 → フィールド長 → データ） |
| SS | SS-09 | 固定長: `names`/`types`/`lengths` の 3 リスト |
| SS | SS-10 | 可変長: `names`/`types` の 2 リスト（`lengths` 不要） |
| SS | SS-11 | 複数レコードレイアウト |
| SS | SS-13 | データ行の先頭セルは必ず空（Excel 固有） |
| SS | SS-15 | 空ファイル表現（ディレクティブのみ） |
| HC | HC-01 | マーカーカラム `[カラム名]` の保持 |
| HC | HC-05 | コメント行（`//`）はスキップ（Ph-2: 両方向ロスト） |
| HC | HC-07 | 空行スキップ |
| MS | MS-01〜MS-14 | メッセージングテストデータの全仕様 |
| DR | DR-01〜DR-12 | ディレクティブの全仕様 |

### 5.2 スコープ外（変換ツールが関与しない仕様）

| カテゴリ | 理由 |
|---|---|
| TS（テストサポート層） | NTF のテスト実行ロジック。変換ツールはデータの形式変換のみを行う |
| RS（YAML リーダー実装仕様） | NTF の YAML 読み込みロジック。変換後の動作は NTF 本体が保証する |
| IV（インタープリタ・特殊値） | `${systemTime}` 等の特殊値は文字列としてそのまま変換する。インタープリタを変換ツールで実行しない |

---

## 6. データモデル設計

変換ツールは以下の 3 層のデータモデルを使用する。

### 6.1 BookModel

Excel ブック / YAML ディレクトリに相当するコンテナ。

```
BookModel
  name: String            // ブック名（拡張子なし）。例: "FooTest"
  sheets: List<SheetModel>  // シートのリスト
```

### 6.2 SheetModel

Excel シート / YAML ファイル 1 枚に相当する。NTF の読み込み単位。

```
SheetModel
  name: String              // シート名 / YAML ファイル名（拡張子なし）。例: "case01"
  sections: List<SectionModel>  // セクションのリスト
```

### 6.3 SectionModel

NTF の 1 セクションに相当する。セクション種別ごとにサブクラスを持つ。

```
SectionModel（抽象）
  dataType: DataType        // セクション種別（DataType 列挙値）
  groupId: String           // groupId（省略時は空文字）
  identifier: String        // 識別子の値（テーブル名・ファイルパス・LIST_MAP の ID 等）
```

#### 6.3.1 TableSectionModel（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

```
TableSectionModel extends SectionModel
  columnNames: List<String>           // カラム名リスト（マーカーカラムを含む）
  rows: List<List<String>>            // データ行のリスト（null・空文字を区別して保持）
```

#### 6.3.2 ListMapSectionModel（LIST_MAP）

```
ListMapSectionModel extends SectionModel
  columnNames: List<String>           // カラム名リスト
  rows: List<List<String>>            // データ行のリスト
```

#### 6.3.3 FileSectionModel（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）

```
FileSectionModel extends SectionModel
  fileType: FileType                  // FIXED / VARIABLE（SETUP_FIXED/EXPECTED_FIXED → FIXED、SETUP_VARIABLE/EXPECTED_VARIABLE → VARIABLE）
  directives: Map<String, String>     // ディレクティブ（キー → 値）
  records: List<RecordLayoutModel>    // レコードレイアウトのリスト
```

`fileType` は `dataType` から一意に決定できるが、YAML Writer が SETUP/EXPECTED を問わず「FIXED か VARIABLE か」だけを見て type フィールドを出力するために正規化フィールドとして保持する。

```
RecordLayoutModel
  recordType: String                  // レコード種別名
  fields: List<FieldModel>            // フィールド定義リスト
  rows: List<List<String>>            // データ行のリスト
```

```
FieldModel
  name: String      // フィールド名
  type: String      // データ型記号（"X", "N", "Z" 等）
  length: String    // フィールド長（固定長のみ。可変長は null。YAML 出力時は null の場合 length キーを省略する）
```

#### 6.3.4 MessageSectionModel（MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES）

```
MessageSectionModel extends SectionModel
  fwHeaderFields: Map<String, String>   // FW 制御ヘッダフィールド（FW_HEADER レコード）
  records: List<RecordLayoutModel>      // レコードレイアウトのリスト（FieldModel は name のみ）
```

---

## 7. クラス設計

### 7.1 パッケージ

```
nablarch.test.core.reader.converter
```

### 7.2 インターフェース

#### TestDataFormatReader

形式に依存しない読み込みインターフェース。

```java
package nablarch.test.core.reader.converter;

import java.nio.file.Path;

/**
 * テストデータを読み込んで {@link BookModel} に変換するインターフェース。
 */
public interface TestDataFormatReader {

    /**
     * 指定されたパスを読み込み、BookModel として返す。
     *
     * @param sourcePath 読み込み元パス（Excel ファイル / YAML ディレクトリ）
     * @return 変換結果の BookModel
     */
    BookModel read(Path sourcePath);
}
```

#### TestDataFormatWriter

形式に依存しない書き込みインターフェース。

```java
package nablarch.test.core.reader.converter;

import java.nio.file.Path;

/**
 * {@link BookModel} を指定された形式で書き出すインターフェース。
 */
public interface TestDataFormatWriter {

    /**
     * BookModel を指定されたパスに書き出す。
     *
     * @param book       書き出す BookModel
     * @param outputPath 書き出し先の基底パス（Excel ファイル / YAML ディレクトリの親）
     * @param overwrite  既存ファイルを上書きするか
     */
    void write(BookModel book, Path outputPath, boolean overwrite);
}
```

### 7.3 実装クラス

#### XlsFormatReader

Apache POI を使用して `.xls` ファイルを読み込み、`BookModel` に変換する。

**責務**

- `.xls` ファイルを開き、全シートを走査する
- 各シートを行 × 列の文字列リストとして読む（POI の `PoiXlsReader` の動作に相当）
- セル書式・色・結合セル・コメントポップアップは無視する
- 先頭セルが `//` で始まる行はコメント行としてスキップし、コメント行数を集計して警告ログに出力する
- 全セルが空の行はスキップする
- セクション識別行（DataType の前方一致 + `[groupId]=identifier` 形式）を検出し、各セクションを適切な `SectionModel` サブクラスに変換する

#### XlsFormatWriter

Apache POI を使用して `BookModel` を `.xls` ファイルとして書き出す。

**責務**

- `BookModel` の各 `SheetModel` をシートとして書き出す
- 全セルを文字列書式で書き出す（NTF の動作保証条件に合わせる）
- セクション識別行（`SETUP_TABLE=USER_MASTER` 等）を先頭行に書き出す
- テーブルデータのカラム名行・データ行を書き出す
- ファイルセクションのディレクティブ行・フィールド名行・データ型行・フィールド長行・データ行を正しい順序で書き出す
- メッセージングセクションの FW ヘッダ行（ディレクティブと同じ位置）を書き出す
- 既存ファイルが存在し `overwrite=false` の場合は `IllegalStateException` をスローする

#### YamlFormatReader

SnakeYAML Engine を使用して `.yaml` ファイルを読み込み、`BookModel` に変換する。

**責務**

- YAML ディレクトリ内の全 `.yaml` ファイルをファイル名アルファベット昇順で走査する
- 各 `.yaml` ファイルをトップレベル Map として読み込む
- `YamlSection` の定数（`KEY_SETUP_TABLES` 等）を使ってセクションキーを識別する
- 各エントリを適切な `SectionModel` サブクラスに変換する
- `BookModel` の `name` にディレクトリ名を設定する

**注意**: 既存の `YamlSection.dataTypeToSectionKey()` はメッセージ系 DataType（`MESSAGE`、`EXPECTED_REQUEST_*`、`RESPONSE_*`）のみ対応しており、テーブル系・ファイル系 DataType では `IllegalArgumentException` をスローする。`YamlFormatReader` は `YamlSection.dataTypeToSectionKey()` に依存せず、以下の変換ツール独自のマッピングテーブルを使用する。

DataType 列は `DataType` enum の定数名（コード上の識別子）を示す。`DataType.getName()` が返す文字列（Excel/YAML のセクション識別名）は別であることに注意（例: `SETUP_TABLE_DATA` の `getName()` は `"SETUP_TABLE"`）。

| YAML キー | DataType（enum 定数名） | `getName()` 値 | SectionModel サブクラス |
|---|---|---|---|
| `setup_tables` | `SETUP_TABLE_DATA` | `"SETUP_TABLE"` | `TableSectionModel` |
| `expected_tables` | `EXPECTED_TABLE_DATA` | `"EXPECTED_TABLE"` | `TableSectionModel` |
| `expected_complete_tables` | `EXPECTED_COMPLETED` | `"EXPECTED_COMPLETE_TABLE"` | `TableSectionModel` |
| `list_maps` | `LIST_MAP` | `"LIST_MAP"` | `ListMapSectionModel` |
| `setup_files` + `type: fixed` | `SETUP_FIXED` | `"SETUP_FIXED"` | `FileSectionModel` |
| `setup_files` + `type: variable` | `SETUP_VARIABLE` | `"SETUP_VARIABLE"` | `FileSectionModel` |
| `expected_files` + `type: fixed` | `EXPECTED_FIXED` | `"EXPECTED_FIXED"` | `FileSectionModel` |
| `expected_files` + `type: variable` | `EXPECTED_VARIABLE` | `"EXPECTED_VARIABLE"` | `FileSectionModel` |
| `messages` | `MESSAGE` | `"MESSAGE"` | `MessageSectionModel` |
| `expected_request_header_messages` | `EXPECTED_REQUEST_HEADER_MESSAGES` | `"EXPECTED_REQUEST_HEADER_MESSAGES"` | `MessageSectionModel` |
| `expected_request_body_messages` | `EXPECTED_REQUEST_BODY_MESSAGES` | `"EXPECTED_REQUEST_BODY_MESSAGES"` | `MessageSectionModel` |
| `response_header_messages` | `RESPONSE_HEADER_MESSAGES` | `"RESPONSE_HEADER_MESSAGES"` | `MessageSectionModel` |
| `response_body_messages` | `RESPONSE_BODY_MESSAGES` | `"RESPONSE_BODY_MESSAGES"` | `MessageSectionModel` |

#### YamlFormatWriter

SnakeYAML Engine を使用して `BookModel` を YAML ファイル群として書き出す。

**責務**

- `BookModel` の各 `SheetModel` を `{bookName}/{sheetName}.yaml` として書き出す
- `YamlSection` の定数を使って各セクションを正しいキーで書き出す
- テーブルデータの `rows:` は `{カラム名: "値"}` 形式で書き出す
- ファイルデータの `fields:` は `{name: X, type: Y, length: Z}` 形式で書き出す
- ファイルデータの `rows:` は配列形式 `["値1", "値2"]` で書き出す
- 出力先ディレクトリが存在しない場合は自動生成する
- 既存ファイルが存在し `overwrite=false` の場合は `IllegalStateException` をスローする

### 7.4 エントリポイント

#### TestDataConverter

`main` メソッドを持つエントリポイントクラス。コマンドライン引数を解析し、適切な Reader/Writer を組み合わせて変換を実行する。

**責務**

- `--from` / `--to` 引数で形式を選択して Reader/Writer インスタンスを生成する
- `--overwrite` オプションを解析する
- `--delete-source` オプションを解析する（変換成功後に入力ファイルを削除する）
- 入力ディレクトリを再帰走査し、変換対象ファイル（`.xls` または YAML ディレクトリ）を列挙する
- 除外パターン（`template.xls`、`MASTER_DATA.xls`）に合致するファイルをスキップする
- 各ファイルに対して Reader → Writer の変換処理を実行する
- 変換結果サマリー（成功件数・スキップ件数・エラー件数・コメント行ロスト件数）を標準出力に表示する
- エラーが 1 件以上あった場合は終了コード 1 で終了する

**引数仕様**

```
TestDataConverter --from <形式> --to <形式> [options] <入力パス> <出力パス>
```

| 引数 | 必須 | 説明 |
|---|---|---|
| `--from <形式>` | 必須 | 入力形式。`xls` または `yaml` |
| `--to <形式>` | 必須 | 出力形式。`xls` または `yaml` |
| `--overwrite` | 任意 | 既存ファイルを上書きする（デフォルト: 上書き禁止） |
| `--delete-source` | 任意 | 変換成功後に入力ファイルを削除する |
| `<入力パス>` | 必須 | 変換対象のルートディレクトリ |
| `<出力パス>` | 必須 | 変換結果の出力先ルートディレクトリ |

### 7.5 ユーティリティクラス

#### ConverterFileFilter

変換対象ファイルの列挙・除外判定を担当する。

**責務**

- 指定ルートディレクトリを再帰走査して変換対象ファイルを列挙する
- 除外パターン（絶対パス末尾一致）に合致するファイルをスキップする。除外対象は 4.2 節の一覧に定義する。パスの末尾一致でマッチするため、パターン例: `template.xls`、`MASTER_DATA.xls`、`MASTER_DATA2.xls`
- Excel 読み込み時は `.xls` ファイルを、YAML 読み込み時は YAML ディレクトリ（`.yaml` ファイルを含む最下位ディレクトリ）を列挙する

#### ConverterPathResolver

入力パスと出力パスの対応関係を計算するユーティリティクラス。

**責務**

- Excel ファイルパスから YAML 出力ディレクトリパスを計算する
- YAML ディレクトリパスから Excel 出力ファイルパスを計算する
- 入力パスと出力パスのルートを考慮した相対パス計算を行う

---

## 8. 変換ルール詳細

### 8.1 セクション識別行

#### Excel → YAML

Excel シートを走査し、セクション識別行（セル値が `DataType.getName()` で前方一致する行）を検出する。

```
SETUP_TABLE=USER_MASTER       → setup_tables: [{table: "USER_MASTER", ...}]
SETUP_TABLE[case01]=USER_MASTER → setup_tables: [{group_id: "case01", table: "USER_MASTER", ...}]
```

識別行検出のロジック:
1. 行の先頭セルの値を取得する
2. `DataType` の全列挙値の `getName()` と前方一致（`startsWith`）で比較する
3. 合致した場合、`[groupId]=identifier` を解析して `dataType`・`groupId`・`identifier` を抽出する

#### YAML → Excel

YAML のトップレベルキーから `DataType` を逆引きし、Excel のセクション識別行を生成する。

```
setup_tables: [{table: "USER_MASTER", ...}]  → SETUP_TABLE=USER_MASTER
setup_tables: [{group_id: "case01", table: "USER_MASTER", ...}] → SETUP_TABLE[case01]=USER_MASTER
```

### 8.2 テーブルデータ（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

`EXPECTED_COMPLETE_TABLE` は `EXPECTED_TABLE` と同じ変換ルールを適用する。

#### Excel → YAML

```
行1: SETUP_TABLE=USER_MASTER  [空]  [空]
行2: USER_ID                  NAME  AGE
行3: 001                      taro  20
行4: 002                      jiro  30
```

↓

```yaml
setup_tables:
  - table: "USER_MASTER"
    rows:
      - USER_ID: "001"
        NAME: "taro"
        AGE: "20"
      - USER_ID: "002"
        NAME: "jiro"
        AGE: "30"
```

- セル値は全て文字列として保持する。空セル（BLANK セル / cell == null）は空文字として扱う。セル値が文字列 `"null"` のときはアンクォートの `null` として YAML に出力する（8.6 節参照）
- ヘッダ末尾の空カラムは除去する
- データ行がヘッダより短い場合、不足分は空文字として補完する
- マーカーカラム（`[カラム名]` 形式）はカラム名をそのまま保持する

#### YAML → Excel

- `rows:` の各マップを行として書き出す
- `group_id:` が存在する場合、識別行を `SETUP_TABLE[group_id]=テーブル名` 形式にする
- `null` 値はセルに `null` と書き出す
- 空文字はセルを空にする

### 8.3 LIST_MAP

#### Excel → YAML

```
行1: LIST_MAP=testShots  [空]   [空]
行2: no                  case   status
行3: 1                   正常系  active
行4: 2                   異常系  error
```

↓

```yaml
list_maps:
  - id: "testShots"
    rows:
      - no: "1"
        case: "正常系"
        status: "active"
      - no: "2"
        case: "異常系"
        status: "error"
```

### 8.4 ファイルデータ（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）

#### Excel 構造の解析

ファイルセクションの Excel 構造は以下の順序で読む。

1. **セクション識別行**: 先頭セルが `SETUP_FIXED=パス` 等の形式
2. **ディレクティブ行**（0 行以上）: 先頭セルがレコード種別名でなく、2 列目以降が値の行
3. **フィールド名行**: 先頭セル = レコード種別名、2 列目以降 = フィールド名
4. **データ型行**: 先頭セルが空、2 列目以降 = データ型記号
5. **フィールド長行**（固定長のみ）: 先頭セルが空、2 列目以降 = フィールド長（数値または `"-"`）
6. **データ行**（1 行以上）: 先頭セルが空、2 列目以降 = フィールド値

ディレクティブ行とフィールド名行の区別: 先頭セルが DataType の名前で始まらない非空セルである行はディレクティブ行とみなす。フィールド名行はデータ型行（2列目以降が型記号）が後続するものとして状態機械で解析する。

**ファイルセクション解析の状態遷移**

| 状態 | 遷移条件 | 遷移先 |
|---|---|---|
| `SECTION_START`（識別行直後） | 先頭セルが非空かつ DataType 名で始まらない | `DIRECTIVE`（ディレクティブ行として読む） |
| `SECTION_START` | 先頭セルが非空かつ DataType 名で始まらない → 次行が型記号行 | `FIELD_NAMES`（フィールド名行として読む） |
| `SECTION_START` / `DIRECTIVE` | 先頭セルが非空かつ DataType 名で始まらない | `DIRECTIVE` 継続 |
| `DIRECTIVE` | 先頭セルが非空、かつ翌行の先頭が空（型記号行相当） | `FIELD_NAMES` |
| `FIELD_NAMES` | 先頭セルが空、2 列目以降が型記号 | `DATA_TYPES` |
| `DATA_TYPES` | 先頭セルが空、固定長の場合 | `FIELD_LENGTHS` |
| `DATA_TYPES` | 先頭セルが空、可変長の場合（長さ行スキップ） | `DATA` |
| `FIELD_LENGTHS` | 先頭セルが空 | `DATA` |
| `DATA` | 先頭セルが空 | `DATA` 継続（次のデータ行） |
| `DATA` | 先頭セルが非空（新レコード種別名）→ 次行が型記号行 | `FIELD_NAMES`（新 `RecordLayoutModel` 追加） |
| いずれかの状態 | 次の DataType 識別行を検出 | 新セクション開始 |

固定長 Excel 例（エンコーディング付き）:

```
行1: SETUP_FIXED=input/data.dat  [空]    [空]    [空]
行2: text-encoding               MS932  [空]    [空]
行3: DATA                        USER_ID AMOUNT [空]
行4: [空]                        X       Z      [空]
行5: [空]                        10      10     [空]
行6: [空]                        001     5000   [空]
```

↓

```yaml
setup_files:
  - path: "input/data.dat"
    type: fixed
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

#### ファイル種別の判定

| DataType | YAML の `type:` |
|---|---|
| `SETUP_FIXED` / `EXPECTED_FIXED` | `fixed` |
| `SETUP_VARIABLE` / `EXPECTED_VARIABLE` | `variable` |

YAML のセクションキー（`setup_files` / `expected_files`）は DataType を問わず共通。逆変換時は `type:` フィールドを参照して `SETUP_FIXED` か `SETUP_VARIABLE` かを決定する。

```
setup_files + type: fixed  → SETUP_FIXED
setup_files + type: variable → SETUP_VARIABLE
expected_files + type: fixed → EXPECTED_FIXED
expected_files + type: variable → EXPECTED_VARIABLE
```

#### 複数レコードレイアウト

Excel でデータ行の後に新たなフィールド名行が来る場合、新しいレコードレイアウトとして `RecordLayoutModel` を追加する。YAML の `records:` 配列に複数の要素として出力される。

#### 空ファイル表現

ディレクティブのみのファイルセクション（レコード定義なし）は `records: []` として出力する。逆変換時は `records:` が空配列の場合、ディレクティブ行のみを書き出す。

### 8.5 メッセージングテストデータ

#### MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES

メッセージングデータの Excel 構造はファイルデータと似ているが、以下の違いがある。

- FW 制御ヘッダ: Excel では `| フィールド名 | 値 |` 形式のディレクティブ行として記述され、YAML では `record_type: FW_HEADER` のレコードとして表現される
- `record_type` 値: NTF が内部で `"default"` に置き換えるが、変換ツールは元の値を保持する（変換後も同じ値が書かれる）
- `no` 列: Excel ではフィールド名行の先頭セルが空。YAML では `rows:` のリスト要素に含める

#### FW ヘッダの変換

Excel:
```
MESSAGE=sendSyncTestData/REQ001/message
requestId  REQ001
userId     usr001
[空]  FIELD1  FIELD2
[空]  X       X
[空]  req1    data1
```

YAML:
```yaml
messages:
  - id: "sendSyncTestData/REQ001/message"
    records:
      - record_type: "FW_HEADER"
        fields:
          - {name: "requestId"}
          - {name: "userId"}
        rows:
          - ["REQ001", "usr001"]
      - record_type: "default"
        fields:
          - {name: "FIELD1", type: "X"}
          - {name: "FIELD2", type: "X"}
        rows:
          - ["req1", "data1"]
```

**FW_HEADER レコードの注意事項**:
- `YamlMessageBuilder` の実装では、FW_HEADER の `fields:` に含まれる `name` のみを参照してフィールドインデックスを決定する。`type` / `length` は参照しないため、FW_HEADER の `fields:` には `name` のみを出力する
- `rows:` の先頭要素がフィールド値の配列。フィールド順と値の順序が対応する（`rows[0][fieldIndex]` が `fields[fieldIndex].name` の値）
- Excel での FW_HEADER 行（`fieldName | value` 形式）は、フィールド名の列挙順を保持して `fields:` に変換し、値を `rows[0]` の対応インデックスに出力する

### 8.6 値変換ルール

#### Excel → YAML

| Excel セル値 | YAML 出力 |
|---|---|
| 空セル（BLANK セル / cell == null） | `""` （空文字列としてダブルクォートで出力する） |
| セル値が文字列 `"null"`（大文字小文字不問） | アンクォートの `null`（NTF の NullInterpreter が Java null に変換する） |
| `"true"` / `"false"` | `"true"` / `"false"` |
| `"001"` 等の先頭ゼロ付き数値文字列 | `"001"` （ダブルクォートを付けて出力する） |
| `${systemTime}` 等の特殊値 | `"${systemTime}"` （そのまま文字列として出力する） |
| `"\\"` で始まる値（`\\r` 等） | `"\\r"` 等をそのまま出力する |

- `rows:` 内の値は原則ダブルクォートで囲む。ただし `null`（Java null を表す）はクォートなしで出力する

#### YAML → Excel

| YAML 値 | Excel 出力 |
|---|---|
| `""`（空文字） | 空セル |
| `null`（アンクォート） | `null`（NTF の `NullInterpreter` が Java null に変換） |
| `"null"`（ダブルクォートあり） | `null`（NTF の `NullInterpreter` は文字列 `"null"` と Java null を等価に扱うため） |
| `"true"` / `"false"` | `true` / `false` |
| `"001"` | `001` |

### 8.7 groupId の変換

#### Excel → YAML

| Excel 識別行 | YAML 出力 |
|---|---|
| `SETUP_TABLE=USER_MASTER` | `group_id` フィールドなし |
| `SETUP_TABLE[case01]=USER_MASTER` | `group_id: "case01"` |

#### YAML → Excel

| YAML `group_id` | Excel 識別行 |
|---|---|
| なし（フィールド不在） | `SETUP_TABLE=USER_MASTER` |
| `group_id: "case01"` | `SETUP_TABLE[case01]=USER_MASTER` |

---

## 9. 実行方法

### 9.1 pom.xml 設定

`exec-maven-plugin` を `pom.xml` に追加する。

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <configuration>
    <mainClass>nablarch.test.core.reader.converter.TestDataConverter</mainClass>
    <classpathScope>test</classpathScope>
  </configuration>
</plugin>
```

POI（`poi-ooxml`）および SnakeYAML Engine（`snakeyaml-engine`）はともに `compile` スコープで宣言済みのため、`classpathScope` は省略（デフォルトの `compile`）でよい。ただし `TestDataConverter` クラスはテストコード（`src/test/java`）に配置するため、`classpathScope` を `test` にしてテストクラスパスを含める。

### 9.2 コマンド例

#### Excel → YAML 変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml src/test/java src/test/java"
```

- `src/test/java` を入力パス兼出力パスとして指定する
- 変換後の `.yaml` ファイルは元の `.xls` ファイルと同じディレクトリ（またはその直下）に生成される
- デフォルトでは既存 `.yaml` ファイルがあればエラー（`--overwrite` で上書き許可）

#### 上書き許可での変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite src/test/java src/test/java"
```

#### 変換後に元 Excel を削除

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite --delete-source src/test/java src/test/java"
```

#### YAML → Excel 逆変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from yaml --to xls src/test/java src/test/java"
```

### 9.3 引数仕様（再掲）

```
TestDataConverter --from <形式> --to <形式> [--overwrite] [--delete-source] <入力パス> <出力パス>
```

| 引数 | 値 | 説明 |
|---|---|---|
| `--from` | `xls` / `yaml` | 入力形式 |
| `--to` | `xls` / `yaml` | 出力形式 |
| `--overwrite` | フラグ | 既存ファイルを上書きする |
| `--delete-source` | フラグ | 変換成功後に入力ファイルを削除する |
| `<入力パス>` | パス文字列 | 変換対象のルートディレクトリ |
| `<出力パス>` | パス文字列 | 変換結果の出力先ルートディレクトリ |

### 9.4 終了コード

| 終了コード | 意味 |
|---|---|
| `0` | 全ファイルの変換が成功した |
| `1` | 1 件以上の変換エラーが発生した（未変換ファイルあり） |
| `2` | 引数エラー（`--from` / `--to` の値が不正、必須引数の欠落等） |

---

## 10. エラー処理方針

### 10.1 基本方針

- 1 ファイルのエラーで全体を停止しない。エラーが発生したファイルをスキップして次のファイルの変換を継続する
- 全ファイルの処理完了後にサマリーを出力し、エラーがあれば終了コード 1 で終了する
- エラーメッセージにはファイルパスと原因を含める

### 10.2 エラーケースと対処

| エラーケース | 対処 |
|---|---|
| 入力ファイルが存在しない | エラーとして記録し、スキップして続行 |
| 入力ファイルが読み取れない（IO エラー・破損） | エラーとして記録し、スキップして続行 |
| 変換先ファイルが存在し `--overwrite` 未指定 | エラーとして記録し、スキップして続行 |
| セクション識別行の書式が不正 | エラーとして記録し、対象ファイルをスキップして続行 |
| フィールド名/型/長さリストのサイズ不一致 | エラーとして記録し、対象ファイルをスキップして続行 |
| 引数が不正（`--from` の値が `xls`/`yaml` 以外等） | 即時終了コード 2 で終了。ヘルプメッセージを出力する |

### 10.3 警告ケースと対処

| 警告ケース | 対処 |
|---|---|
| コメント行（`//`）が存在する（Ph-2 相当） | 標準エラー出力に警告を出力し、コメント行を読み捨てて処理を継続する |
| 除外パターンに合致するファイル | 標準出力にスキップメッセージを出力し、スキップして続行 |

### 10.4 変換サマリー出力例

```
=== TestDataConverter 変換サマリー ===
変換成功: 59 件
スキップ: 2 件（除外パターン合致）
エラー:   0 件
コメント行ロスト: 12 行（3 ファイル）
```
