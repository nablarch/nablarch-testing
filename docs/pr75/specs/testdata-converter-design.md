# NTF テストデータ形式間変換ツール 設計書

- **作成日**: 2026-05-27
- **更新日**: 2026-05-27（C-1-2: 命名を TestDataContainer/TestDataSection/TestDataBlock に統一、汎用化）
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

NTF（Nablarch Testing Framework）テストデータの Excel ファイル（`.xls`）を YAML ファイルに変換する。また逆方向変換（YAML → Excel）にも対応する設計とする。

変換ツールの目標は、Excel 依存を排除して YAML ベースのテストデータ管理体制へ移行することである。

### 1.2 スコープ

**変換ツールのスコープ**

変換ツールが対応する NTF 仕様 ID の全一覧は `docs/pr75/ntf-impl-spec-list.md` の「変換ツール対象」列を参照すること。仕様リスト全 145 件のうち「対象」と記載された仕様が変換ツールの実装範囲である（[5章](#5-対応-ntf-仕様-id) 参照）。

**変換ツールがカバーすること**

- Excel（`.xls`）→ YAML（`.yaml`）への変換
- YAML（`.yaml`）→ Excel（`.xls`）への逆変換

**変換ツールがカバーしないこと**

- テストの実行・検証（NTF 本体の責務）
- Excel のセル書式・色・結合セル・コメントポップアップ等の変換（NTF 本体が無視するため）
- 仕様リストで「対象外」と記載された NTF 仕様（実行時動作・入力値検証・内部実装）

---

## 2. 設計方針

### 2.1 データモデル中心設計

変換ツールは「Excel を読む」「YAML を書く」という形で直接変換するのではなく、形式非依存の中間データモデルを中心に設計する。Reader が中間データモデルに変換し、Writer が中間データモデルから出力形式に変換する。これにより、将来 CSV・JSON 等の新形式を追加しても既存の Reader/Writer を変更せずに済む。

```
Excel → [XlsFormatReader]  → TestDataContainer → [YamlFormatWriter] → YAML
YAML  → [YamlFormatReader] → TestDataContainer → [XlsFormatWriter]  → Excel
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

NTF が読み込む全データブロック種別（`SETUP_TABLE`、`EXPECTED_TABLE`、`EXPECTED_COMPLETE_TABLE`、`LIST_MAP`、`SETUP_FIXED`、`SETUP_VARIABLE`、`EXPECTED_FIXED`、`EXPECTED_VARIABLE`、`MESSAGE`、`EXPECTED_REQUEST_HEADER_MESSAGES`、`EXPECTED_REQUEST_BODY_MESSAGES`、`RESPONSE_HEADER_MESSAGES`、`RESPONSE_BODY_MESSAGES`）について、Excel ↔ YAML 間の変換を実装する。

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

指定入力ルートディレクトリ配下の `.xls` ファイル。ただし 4.2 節の除外パターンに合致するファイルは除く。

### 4.2 除外ファイル

以下のファイルパターン（絶対パス末尾一致）に合致するファイルは変換対象から除外する。

| 除外パターン | 除外理由 |
|---|---|
| `template.xls` | HTTP ダンプテンプレート等。NTF テストデータ以外の XLS ファイル |
| `MASTER_DATA.xls` | DB 初期データ等。NTF テストデータ以外の XLS ファイル |
| `MASTER_DATA2.xls` | 同上 |

除外パターンはリスト構成とし、実行時に追加指定できるものとする。

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

変換ツールが正しく動作するために準拠する NTF 仕様 ID の網羅的な一覧は `docs/pr75/ntf-impl-spec-list.md` の「変換ツール対象」列を参照すること。同列が `対象` となっている仕様 ID が変換ツールの実装範囲である。

### 5.1 対象仕様の分類サマリー

仕様リスト全 145 件のうち変換ツールが「対象」とする仕様は以下のカテゴリから選定される。

| カテゴリ | 変換ツール対象の主な仕様 |
|---|---|
| DT | データブロック識別行の解析・生成（DT-01〜DT-03, DT-06） |
| SS | テーブル・ファイルデータブロック構造の解析・生成（SS-01, SS-08〜SS-13, SS-15, SS-17） |
| RS | YAML 出力値のエンコーディングルール・ファイル命名（RS-01, RS-03〜RS-05, RS-10, RS-11, RS-22） |
| HC | Excel 読み取り時のヘッダ・コメント・空行処理（HC-01, HC-03〜HC-07） |
| IV | なし（インタープリタはNTF実行時の変換動作。変換ツールは文字列値をそのまま変換する） |
| DR | ディレクティブ行の解析・生成（DR-01, DR-07, DR-09, DR-10） |
| MS | メッセージングFW制御ヘッダ・no列構造（MS-01, MS-02） |
| TS | なし（テストサポート層の実行時動作） |

### 5.2 対象外仕様の理由区分

「対象外」と分類した仕様は以下のいずれかに該当する。

| 区分 | 意味 | 例 |
|---|---|---|
| `対象外（実行時）` | NTF がデータを読み込んだ後に実行する処理。変換ツールは文字列として保持すれば等価性が保たれる | DT-04〜DT-05（GroupData/SingleData収集）, IV-01〜IV-16（インタープリタ）, TS-01〜TS-34（テストサポート層） |
| `対象外（検証）` | NTF が実行時に行う入力値の検証。変換ツールは検証を行わずエラーはNTF実行時に検出される | SS-14（フィールド名重複）, SS-16（レコード長一致）, DR-02〜DR-03（ディレクティブキー検証） |
| `対象外（内部）` | NTF の内部実装・APIであり変換ツールが依存しない | RS-02（readLine API）, RS-07〜RS-09（リーダー内部動作）, SS-29（TableData内部処理） |

---

## 6. データモデル設計

変換ツールは以下の 3 層のデータモデルを使用する。

### 6.1 TestDataContainer

Excel ブック / YAML ディレクトリに相当するコンテナ。テストクラスと 1 対 1 に対応する。

```
TestDataContainer
  name: String                        // ブック名（拡張子なし）。例: "FooTest"
  sections: List<TestDataSection>     // セクション（読み込み単位）のリスト
```

### 6.2 TestDataSection

Excel シート / YAML ファイル 1 枚に相当する。NTF の読み込み単位。

```
TestDataSection
  name: String                        // シート名 / YAML ファイル名（拡張子なし）。例: "case01"
  blocks: List<TestDataBlock>         // データブロックのリスト
```

### 6.3 TestDataBlock

NTF の 1 データブロックに相当する。データブロック種別ごとにサブクラスを持つ。

```
TestDataBlock（抽象）
  dataType: DataType                  // データブロック種別（DataType 列挙値）
  groupId: String                     // groupId（省略時は空文字）
  identifier: String                  // 識別子の値（テーブル名・ファイルパス・LIST_MAP の ID 等）
```

#### 6.3.1 TableDataBlock（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

```
TableDataBlock extends TestDataBlock
  columnNames: List<String>           // カラム名リスト（マーカーカラムを含む）
  rows: List<List<String>>            // データ行のリスト（null・空文字を区別して保持）
```

#### 6.3.2 ListMapBlock（LIST_MAP）

```
ListMapBlock extends TestDataBlock
  columnNames: List<String>           // カラム名リスト
  rows: List<List<String>>            // データ行のリスト
```

#### 6.3.3 FileDataBlock（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）

```
FileDataBlock extends TestDataBlock
  fileType: FileType                  // FIXED / VARIABLE（SETUP_FIXED/EXPECTED_FIXED → FIXED、SETUP_VARIABLE/EXPECTED_VARIABLE → VARIABLE）
  directives: Map<String, String>     // ディレクティブ（キー → 値）
  records: List<RecordLayout>         // レコードレイアウトのリスト
```

`fileType` は `dataType` から一意に決定できるが、YAML Writer が SETUP/EXPECTED を問わず「FIXED か VARIABLE か」だけを見て type フィールドを出力するために正規化フィールドとして保持する。

```
RecordLayout
  recordType: String                  // レコード種別名
  fields: List<FieldDef>              // フィールド定義リスト
  rows: List<List<String>>            // データ行のリスト
```

```
FieldDef
  name: String      // フィールド名
  type: String      // データ型記号（"X", "N", "Z" 等）
  length: String    // フィールド長（固定長のみ。可変長は null。YAML 出力時は null の場合 length キーを省略する）
```

#### 6.3.4 MessageDataBlock（MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES）

```
MessageDataBlock extends TestDataBlock
  fwHeaderFields: Map<String, String>   // FW 制御ヘッダフィールド（FW_HEADER レコード）
  records: List<RecordLayout>           // レコードレイアウトのリスト（FieldDef は name のみ）
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
 * テストデータを読み込んで {@link TestDataContainer} に変換するインターフェース。
 */
public interface TestDataFormatReader {

    /**
     * 指定されたパスを読み込み、TestDataContainer として返す。
     *
     * @param sourcePath 読み込み元パス（Excel ファイル / YAML ディレクトリ）
     * @return 変換結果の TestDataContainer
     */
    TestDataContainer read(Path sourcePath);
}
```

#### TestDataFormatWriter

形式に依存しない書き込みインターフェース。

```java
package nablarch.test.core.reader.converter;

import java.nio.file.Path;

/**
 * {@link TestDataContainer} を指定された形式で書き出すインターフェース。
 */
public interface TestDataFormatWriter {

    /**
     * TestDataContainer を指定されたパスに書き出す。
     *
     * @param container  書き出す TestDataContainer
     * @param outputPath 書き出し先の基底パス（Excel ファイル / YAML ディレクトリの親）
     * @param overwrite  既存ファイルを上書きするか
     */
    void write(TestDataContainer container, Path outputPath, boolean overwrite);
}
```

### 7.3 実装クラス

#### XlsFormatReader

Apache POI を使用して `.xls` ファイルを読み込み、`TestDataContainer` に変換する。

**責務**

- `.xls` ファイルを開き、全シートを走査する
- 各シートを行 × 列の文字列リストとして読む（POI の `PoiXlsReader` の動作に相当）
- セル書式・色・結合セル・コメントポップアップは無視する
- 先頭セルが `//` で始まる行はコメント行としてスキップし、コメント行数を集計して警告ログに出力する（HC-05）
- 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨てる（HC-06）
- 全セルが空の行はスキップする（HC-07）
- データブロック識別行（DataType の前方一致 + `[groupId]=identifier` 形式）を検出し、各データブロックを適切な `TestDataBlock` サブクラスに変換する

#### XlsFormatWriter

Apache POI を使用して `TestDataContainer` を `.xls` ファイルとして書き出す。

**責務**

- `TestDataContainer` の各 `TestDataSection` をシートとして書き出す
- 全セルを文字列書式で書き出す（NTF の動作保証条件に合わせる）
- データブロック識別行（`SETUP_TABLE=USER_MASTER` 等）を先頭行に書き出す
- テーブルデータのカラム名行・データ行を書き出す
- ファイルデータブロックのディレクティブ行・フィールド名行・データ型行・フィールド長行・データ行を正しい順序で書き出す（SS-08）
- ファイルデータブロックのデータ行は先頭セルを空にして書き出す（SS-13）
- メッセージングデータブロックの FW ヘッダ行（ディレクティブと同じ位置）を書き出す
- 既存ファイルが存在し `overwrite=false` の場合は `IllegalStateException` をスローする

#### YamlFormatReader

SnakeYAML Engine を使用して `.yaml` ファイルを読み込み、`TestDataContainer` に変換する。

**責務**

- YAML ディレクトリ内の全 `.yaml` ファイルをファイル名アルファベット昇順で走査する
- 各 `.yaml` ファイルをトップレベル Map として読み込む
- `YamlSection` の定数（`KEY_SETUP_TABLES` 等）を使ってデータブロックキーを識別する
- 各エントリを適切な `TestDataBlock` サブクラスに変換する
- `TestDataContainer` の `name` にディレクトリ名を設定する

**注意**: 既存の `YamlSection.dataTypeToSectionKey()` はメッセージ系 DataType（`MESSAGE`、`EXPECTED_REQUEST_*`、`RESPONSE_*`）のみ対応しており、テーブル系・ファイル系 DataType では `IllegalArgumentException` をスローする。`YamlFormatReader` は `YamlSection.dataTypeToSectionKey()` に依存せず、以下の変換ツール独自のマッピングテーブルを使用する。

DataType 列は `DataType` enum の定数名（コード上の識別子）を示す。`DataType.getName()` が返す文字列（Excel/YAML のデータブロック識別名）は別であることに注意（例: `SETUP_TABLE_DATA` の `getName()` は `"SETUP_TABLE"`）。

| YAML キー | DataType（enum 定数名） | `getName()` 値 | TestDataBlock サブクラス |
|---|---|---|---|
| `setup_tables` | `SETUP_TABLE_DATA` | `"SETUP_TABLE"` | `TableDataBlock` |
| `expected_tables` | `EXPECTED_TABLE_DATA` | `"EXPECTED_TABLE"` | `TableDataBlock` |
| `expected_complete_tables` | `EXPECTED_COMPLETED` | `"EXPECTED_COMPLETE_TABLE"` | `TableDataBlock` |
| `list_maps` | `LIST_MAP` | `"LIST_MAP"` | `ListMapBlock` |
| `setup_files` + `type: fixed` | `SETUP_FIXED` | `"SETUP_FIXED"` | `FileDataBlock` |
| `setup_files` + `type: variable` | `SETUP_VARIABLE` | `"SETUP_VARIABLE"` | `FileDataBlock` |
| `expected_files` + `type: fixed` | `EXPECTED_FIXED` | `"EXPECTED_FIXED"` | `FileDataBlock` |
| `expected_files` + `type: variable` | `EXPECTED_VARIABLE` | `"EXPECTED_VARIABLE"` | `FileDataBlock` |
| `messages` | `MESSAGE` | `"MESSAGE"` | `MessageDataBlock` |
| `expected_request_header_messages` | `EXPECTED_REQUEST_HEADER_MESSAGES` | `"EXPECTED_REQUEST_HEADER_MESSAGES"` | `MessageDataBlock` |
| `expected_request_body_messages` | `EXPECTED_REQUEST_BODY_MESSAGES` | `"EXPECTED_REQUEST_BODY_MESSAGES"` | `MessageDataBlock` |
| `response_header_messages` | `RESPONSE_HEADER_MESSAGES` | `"RESPONSE_HEADER_MESSAGES"` | `MessageDataBlock` |
| `response_body_messages` | `RESPONSE_BODY_MESSAGES` | `"RESPONSE_BODY_MESSAGES"` | `MessageDataBlock` |

#### YamlFormatWriter

SnakeYAML Engine を使用して `TestDataContainer` を YAML ファイル群として書き出す。

**責務**

- `TestDataContainer` の各 `TestDataSection` を `{containerName}/{sectionName}.yaml` として書き出す
- `YamlSection` の定数を使って各データブロックを正しいキーで書き出す
- テーブルデータの `rows:` は `{カラム名: "値"}` 形式で書き出す。`table:` キーを必ず出力する（RS-10）
- ファイルデータの `fields:` は `{name: X, type: Y, length: Z}` 形式で書き出す。`path:` キーを必ず出力する（RS-11）
- ファイルデータの `rows:` は配列形式 `["値1", "値2"]` で書き出す
- 同一 YAML ファイル内にトップレベルの重複キーを出力しない（RS-22）
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
- 除外パターン（`template.xls`、`MASTER_DATA.xls` 等）に合致するファイルをスキップする
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
- 除外パターン（絶対パス末尾一致）に合致するファイルをスキップする。除外対象は 4.2 節の一覧に定義する
- Excel 読み込み時は `.xls` ファイルを、YAML 読み込み時は YAML ディレクトリ（`.yaml` ファイルを含む最下位ディレクトリ）を列挙する

#### ConverterPathResolver

入力パスと出力パスの対応関係を計算するユーティリティクラス。

**責務**

- Excel ファイルパスから YAML 出力ディレクトリパスを計算する
- YAML ディレクトリパスから Excel 出力ファイルパスを計算する
- 入力パスと出力パスのルートを考慮した相対パス計算を行う

---

## 8. 変換ルール詳細

### 8.1 データブロック識別行

#### Excel → YAML

Excel シートを走査し、データブロック識別行（セル値が `DataType.getName()` で前方一致する行）を検出する。

```
SETUP_TABLE=USER_MASTER           → setup_tables: [{table: "USER_MASTER", ...}]
SETUP_TABLE[case01]=USER_MASTER   → setup_tables: [{group_id: "case01", table: "USER_MASTER", ...}]
```

識別行検出のロジック:
1. 行の先頭セルの値を取得する
2. `DataType` の全列挙値の `getName()` と前方一致（`startsWith`）で比較する
3. 合致した場合、`[groupId]=identifier` を解析して `dataType`・`groupId`・`identifier` を抽出する

#### YAML → Excel

YAML のトップレベルキーから `DataType` を逆引きし、Excel のデータブロック識別行を生成する。

```
setup_tables: [{table: "USER_MASTER", ...}]                      → SETUP_TABLE=USER_MASTER
setup_tables: [{group_id: "case01", table: "USER_MASTER", ...}]  → SETUP_TABLE[case01]=USER_MASTER
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

ファイルデータブロックの Excel 構造は以下の順序で読む。

1. **データブロック識別行**: 先頭セルが `SETUP_FIXED=パス` 等の形式
2. **ディレクティブ行**（0 行以上）: 先頭セルがレコード種別名でなく、2 列目以降が値の行
3. **フィールド名行**: 先頭セル = レコード種別名、2 列目以降 = フィールド名
4. **データ型行**: 先頭セルが空、2 列目以降 = データ型記号
5. **フィールド長行**（固定長のみ）: 先頭セルが空、2 列目以降 = フィールド長（数値または `"-"`）
6. **データ行**（1 行以上）: 先頭セルが空、2 列目以降 = フィールド値

ディレクティブ行とフィールド名行の区別: 先頭セルが DataType の名前で始まらない非空セルである行はディレクティブ行とみなす。フィールド名行はデータ型行（2列目以降が型記号）が後続するものとして状態機械で解析する。

**ファイルデータブロック解析の状態遷移**

| 状態 | 遷移条件 | 遷移先 |
|---|---|---|
| `BLOCK_START`（識別行直後） | 先頭セルが非空かつ DataType 名で始まらない | `DIRECTIVE`（ディレクティブ行として読む） |
| `BLOCK_START` | 先頭セルが非空かつ DataType 名で始まらない → 次行が型記号行 | `FIELD_NAMES`（フィールド名行として読む） |
| `BLOCK_START` / `DIRECTIVE` | 先頭セルが非空かつ DataType 名で始まらない | `DIRECTIVE` 継続 |
| `DIRECTIVE` | 先頭セルが非空、かつ翌行の先頭が空（型記号行相当） | `FIELD_NAMES` |
| `FIELD_NAMES` | 先頭セルが空、2 列目以降が型記号 | `DATA_TYPES` |
| `DATA_TYPES` | 先頭セルが空、固定長の場合 | `FIELD_LENGTHS` |
| `DATA_TYPES` | 先頭セルが空、可変長の場合（長さ行スキップ） | `DATA` |
| `FIELD_LENGTHS` | 先頭セルが空 | `DATA` |
| `DATA` | 先頭セルが空 | `DATA` 継続（次のデータ行） |
| `DATA` | 先頭セルが非空（新レコード種別名）→ 次行が型記号行 | `FIELD_NAMES`（新 `RecordLayout` 追加） |
| いずれかの状態 | 次の DataType 識別行を検出 | 新データブロック開始 |

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

YAML のキー（`setup_files` / `expected_files`）は DataType を問わず共通。逆変換時は `type:` フィールドを参照して `SETUP_FIXED` か `SETUP_VARIABLE` かを決定する。

```
setup_files   + type: fixed     → SETUP_FIXED
setup_files   + type: variable  → SETUP_VARIABLE
expected_files + type: fixed    → EXPECTED_FIXED
expected_files + type: variable → EXPECTED_VARIABLE
```

#### 複数レコードレイアウト

Excel でデータ行の後に新たなフィールド名行が来る場合、新しいレコードレイアウトとして `RecordLayout` を追加する。YAML の `records:` 配列に複数の要素として出力される。

#### 空ファイル表現

ディレクティブのみのファイルデータブロック（レコード定義なし）は `records: []` として出力する。逆変換時は `records:` が空配列の場合、ディレクティブ行のみを書き出す。

#### `"-"` フィールド長の変換（SS-17）

Excel のフィールド長行で `"-"` が記述されている場合、YAML の `length:` フィールドにも文字列 `"-"` としてそのまま出力する。逆変換も同様。NTF 実行時の自動拡張（最大バイト長への伸張）は変換ツールの責務外であり、変換ツールは値を保持するだけでよい。

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

### 8.8 ディレクティブ値の変換ルール（DR-09, DR-10）

ディレクティブ値は原則として文字列としてそのまま変換する。ただし以下の特殊値は変換ツールが両方向で保持すること。

#### `field-separator`（DR-09）

| Excel / YAML 値 | 変換ツールの扱い |
|---|---|
| `","` | 文字列としてそのまま変換 |
| `"\\t"` | 文字列 `"\\t"` としてそのまま変換（タブ文字への変換は NTF 実行時の動作） |

**注意**: `"\\t"` をタブ文字に変換するのは NTF 実行時の責務であり、変換ツールはリテラル文字列 `"\\t"` をそのまま YAML に出力する。

#### `record-separator`（DR-10）

| Excel / YAML 値 | 変換ツールの扱い |
|---|---|
| `NONE` / `CR` / `LF` / `CRLF` | 文字列としてそのまま変換 |
| 任意のリテラル文字列 | 文字列としてそのまま変換 |

ディレクティブ値の有効性検証（未知のキー・不正な値）は変換ツールの責務外。NTF 実行時に検出される。

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
  -Dexec.args="--from xls --to yaml <入力パス> <出力パス>"
```

- デフォルトでは既存 `.yaml` ファイルがあればエラー（`--overwrite` で上書き許可）

#### 上書き許可での変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite <入力パス> <出力パス>"
```

#### 変換後に元 Excel を削除

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite --delete-source <入力パス> <出力パス>"
```

#### YAML → Excel 逆変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.core.reader.converter.TestDataConverter \
  -Dexec.args="--from yaml --to xls <入力パス> <出力パス>"
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
| データブロック識別行の書式が不正 | エラーとして記録し、対象ファイルをスキップして続行 |
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
