# NTF テストデータ形式間変換ツール 設計書

- **作成日**: 2026-05-27
- **更新日**: 2026-05-27（C-1-7: フェーズ定義章を廃止し設計方針に統合）
- **対象ブランチ**: convert-testdata-excel-to-text

---

## 目次

1. [目的・スコープ](#1-目的スコープ)
2. [設計方針](#2-設計方針)
3. [データモデルとファイル構造の対応](#3-データモデルとファイル構造の対応)
4. [対応 NTF 仕様 ID](#4-対応-ntf-仕様-id)
5. [データモデル設計](#5-データモデル設計)
6. [クラス設計](#6-クラス設計)
7. [形式別 IN/OUT 仕様](#7-形式別-inout-仕様)
8. [実行方法](#8-実行方法)
9. [エラー処理方針](#9-エラー処理方針)

---

## 1. 目的・スコープ

### 1.1 目的

NTF（Nablarch Testing Framework）のテストデータを特定の形式に依存させず、Excel（`.xls`）と YAML（`.yaml`）を相互に変換可能にする。

これにより以下のような運用を選択できる。

- **全面 YAML 移行**: 既存の Excel テストデータを YAML に一括変換し、以降は YAML だけで運用する。AI によるテストデータ生成・編集が容易になる
- **Excel / YAML 並走**: 人間は Excel で編集し、AI は YAML を参照・生成する。両形式を相互変換しながら共存させる

変換ツールは「どちらの形式で管理するか」の選択を開発チームに委ねる。形式の優劣を決めるものではなく、形式の壁をなくすことが目的である。

### 1.2 スコープ

**変換ツールのスコープ**

変換ツールが対応する NTF 仕様 ID の全一覧は `docs/pr75/ntf-impl-spec-list.md` の「変換ツール対象」列を参照すること。仕様リスト全 145 件のうち「対象」と記載された仕様が変換ツールの実装範囲である（[4章](#4-対応-ntf-仕様-id) 参照）。

**変換ツールがカバーすること**

- Excel（`.xls`）→ YAML（`.yaml`）への変換
- YAML（`.yaml`）→ Excel（`.xls`）への変換

**変換ツールがカバーしないこと**

- テストの実行・検証（NTF 本体の責務）
- 仕様リストで「対象外」と記載された NTF 仕様（実行時動作・入力値検証・内部実装）

**前提条件**

- 入力 Excel ファイルは全セルが**文字列書式**で記述されていること。数値書式・日付書式のセルが含まれる場合、POI の `Cell.toString()` が `"001"` を `"1.0"` 等に変換するため、変換等価性を保証しない（警告を出力して処理は継続する）

---

## 2. 設計方針

### 2.1 データモデル中心設計

変換ツールは形式間の直接変換ではなく、**NTF 仕様に基づく中間データモデル（`TestDataContainer`）を起点**として設計する。Excel と YAML は NTF テストデータを表現する「形式の一手段」にすぎない。

- **IN（形式 → モデル）**: 各形式の Reader が NTF 仕様に従ってモデルに変換する。形式固有の情報（NTF 仕様外）はモデルに乗らない
- **OUT（モデル → 形式）**: 各形式の Writer が出力ルールに従ってモデルから形式に変換する。「ロスト」という概念はなく、出力ルールが出力内容を決める

```
Excel → [XlsFormatReader]  → TestDataContainer → [YamlFormatWriter] → YAML
YAML  → [YamlFormatReader] → TestDataContainer → [XlsFormatWriter]  → Excel
```

将来 CSV・JSON 等の新形式を追加しても既存の Reader/Writer を変更せずに済む。

### 2.2 NTF 内部クラス非依存と整合性の検知

変換ツールは NTF テストデータのパース処理（`BasicTestDataParser`、`TableData`、`DataFile` 等）を再利用しない。これらは「テストデータを読み込んでテストを実行する」という別の責務を持っており、変換ツールの「形式間でデータを忠実に変換する」責務とは異なる。変換ツールは独立したデータモデルを持つ。

**整合性の担保は統合テストで行う。** 変換ツールが NTF と静かにズレていくことを防ぐため、以下の統合テストを実装する。

```
元の Excel を BasicTestDataParser で読んだ結果
    ==
変換ツールで Excel → YAML に変換し YamlTestDataParser で読んだ結果
```

NTF 側の仕様変更（新 DataType の追加、YAML キーの変更等）があった場合、この統合テストが壊れることで検知できる。コードの独立性を保ちつつ、テストが整合性の番人になる。

### 2.3 上書き禁止デフォルト

既に変換先ファイルが存在する場合、デフォルト動作は上書きせずにエラーとして扱う（終了コード 1）。明示的に `--overwrite` オプションを指定した場合のみ上書きを許可する。誤操作による既存データの消失を防ぐ。

### 2.4 変換等価性の定義

変換における「等価」とは「NTF が読み込んだとき同じデータオブジェクトが生成されること」と定義する。

### 2.5 モデルに乗らない情報の扱い

#### IN（形式 → モデル）

どの形式でも、NTF 仕様としてモデルに乗せられない情報が存在する。これらは IN 時に検出し、ユーザーに通知・対応依頼する。

**Excel IN の場合**

Excel は NTF 仕様外のあらゆる情報（色・書式・結合セル・コメントポップアップ・NTF 仕様外のセル内容等）を含められる。これらはモデルに乗らない。モデルに乗らなかった情報を検出し、以下の形式でテキストファイルに出力してユーザーに通知する。

```
FooTest.xls
  Sheet: case01, Cell: B5, Value: "001", Background: FF0000
  Sheet: case01, Cell: C5, Value: "taro", Font-Color: FF0000
```

なお、コメント行（`//` 始まりの行）は NTF が読み捨てる仕様のためモデルに乗らない。変換実行時にコメント行数を警告として標準エラー出力する。

#### OUT（モデル → 形式）

OUT は出力ルールに従ってモデルの内容を形式に変換するだけであり、「ロスト」という概念はない。

**Excel OUT の場合**

色・書式はモデルが持たないため、出力ルールに従って新規に付与する。出力ルールはカスタマイズ可能とする。

デフォルトの出力ルール（仮説: Example アプリの調査で確認されたパターンに基づく）:

| 行の種類 | 判定方法 | デフォルト色 |
|---|---|---|
| DataType 識別行（`SETUP_TABLE=...` 等） | DataType 種別ごとに先頭セルが DataType 名で始まる | DataType 種別ごとに色を割り当て |
| カラム名行 | 識別行の直後の行 | 水色 |
| コメント行（`//`） | 先頭セルが `//` で始まる | 濃紺背景・白文字 |

**YAML OUT の場合**

出力ルール（カスタマイズ可能）: インデント幅・文字列クォートスタイル・データブロック間の空行

---

## 3. データモデルとファイル構造の対応

### 3.1 データモデルとファイルの対応

変換ツールの中間データモデル（5章）は、形式に依存せず以下の意味を持つ。

| データモデル | 意味 | 対応するNTFの読み込み単位 |
|---|---|---|
| `TestDataContainer` | 1 テストクラス分のテストデータ全体 | `TestDataParser` に渡す 1 つのリソース（ファイルまたはディレクトリ） |
| `TestDataSection` | 1 読み込み単位のテストデータ | `TestDataReader.open(path, dataName)` の `dataName` 1 件 |
| `TestDataBlock` | 1 データブロック（DataType + identifier + 行データ） | `BasicTestDataParser.getSetupTableData()` 等が返す個々のデータオブジェクト |

各形式がこのデータモデルにどのように対応するかは形式ごとに定める（7章参照）。

### 3.2 include / exclude パターン

変換対象ファイルは `--include` / `--exclude` オプションで制御する。どちらも**ファイル名に対するグロブパターン**（`*` = 任意の文字列、`?` = 任意の 1 文字）で指定する。ディレクトリパスは評価しない。

**評価ルール**:

1. `--include` が 1 件以上指定されている場合、いずれかの include パターンに合致するファイルのみを候補とする（指定がなければ全ファイルが候補）
2. 候補のうち、いずれかの `--exclude` パターンに合致するファイルをスキップする
3. `--include` と `--exclude` の両方に合致する場合は `--exclude` が優先される

**例**:

```bash
# MASTER_DATA*.xls と template.xls を除外する
--exclude "MASTER_DATA*.xls" --exclude "template.xls"

# FooTest.xls と BarTest.xls だけを対象にする
--include "FooTest.xls" --include "BarTest.xls"

# テスト系ファイルのみ対象にして、テンプレートを除外する
--include "*Test.xls" --exclude "template.xls"
```

デフォルトは include / exclude なし（全対象ファイルが候補）。プロジェクト固有の除外ファイル（DB 初期データ、HTTP ダンプテンプレート等）はツール側で決め打ちせず、実行者が `--exclude` で明示的に指定する。

### 3.3 形式ごとのファイル構造（詳細）

各形式がデータモデルにどのようなファイル構造で対応するかを定める。

#### XLS 形式

| データモデル | XLS での対応 |
|---|---|
| `TestDataContainer` | `.xls` ブック 1 ファイル |
| `TestDataSection` | ブック内のシート 1 枚（シート名 = セクション名） |
| `TestDataBlock` | シート内のデータブロック（識別行から始まる行群） |

`TestDataContainer` の名前はファイル名（拡張子なし）。例: `FooTest.xls` → `name = "FooTest"`

#### YAML 形式

| データモデル | YAML での対応 |
|---|---|
| `TestDataContainer` | YAML ディレクトリ 1 つ |
| `TestDataSection` | ディレクトリ内の `.yaml` ファイル 1 枚（ファイル名（拡張子なし）= セクション名） |
| `TestDataBlock` | YAML ファイル内のトップレベルキー配下の各エントリ |

`TestDataContainer` の名前はディレクトリ名。例: `FooTest/` → `name = "FooTest"`

**YAML ディレクトリの定義**: 直下に `.yaml` ファイルを 1 件以上含み、かつ `.yaml` ファイルを含むサブディレクトリを持たないディレクトリ（最下位の `.yaml` 保有ディレクトリ）を 1 つの変換単位とする。

- `A/B/C/` に `.yaml` があり `A/B/` に `.yaml` がない場合: `A/B/C/` が変換単位
- `A/B/` にも `.yaml` があり `A/B/C/` にも `.yaml` がある場合: `A/B/C/` のみが変換単位（`A/B/` は `.yaml` 含むサブディレクトリを持つため対象外）
- `A/B/C/` と `A/B/D/` の両方に `.yaml` がある場合: それぞれ独立した変換単位

**セクション順序の制限**: YAML 形式ではセクション（ファイル）の順序はファイル名のアルファベット昇順になる。XLS 形式のシート順序を保持したい場合は、YAML ファイル名に連番プレフィクス（例: `01_case01.yaml`）を付けること。

### 3.4 resourceName の対応

NTF は形式によって異なる resourceName で識別する。

| 形式 | resourceName の形式 | 例 |
|---|---|---|
| XLS | `ファイル名/シート名`（拡張子なし） | `FooTest/case01` |
| YAML | `ディレクトリ名/ファイル名`（拡張子なし） | `FooTest/case01` |

変換後も resourceName が変わらないよう、ファイル名・シート名・ディレクトリ名・YAML ファイル名を対応させる。

---

## 4. 対応 NTF 仕様 ID

変換ツールが正しく動作するために準拠する NTF 仕様 ID の網羅的な一覧は `docs/pr75/ntf-impl-spec-list.md` の「変換ツール対象」列を参照すること。同列が `対象` となっている仕様 ID が変換ツールの実装範囲である。

### 4.1 対象仕様の分類サマリー

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

### 4.2 対象外仕様の理由区分

「対象外」と分類した仕様は以下のいずれかに該当する。

| 区分 | 意味 | 例 |
|---|---|---|
| `対象外（実行時）` | NTF がデータを読み込んだ後に実行する処理。変換ツールは文字列として保持すれば等価性が保たれる | DT-04〜DT-05（GroupData/SingleData収集）, IV-01〜IV-16（インタープリタ）, TS-01〜TS-34（テストサポート層） |
| `対象外（検証）` | NTF が実行時に行う入力値の検証。変換ツールは検証を行わずエラーはNTF実行時に検出される | SS-14（フィールド名重複）, SS-16（レコード長一致）, DR-02〜DR-03（ディレクティブキー検証） |
| `対象外（内部）` | NTF の内部実装・APIであり変換ツールが依存しない | RS-02（readLine API）, RS-07〜RS-09（リーダー内部動作）, SS-29（TableData内部処理） |

---

## 5. データモデル設計

変換ツールは以下の 3 層のデータモデルを使用する。

### 5.1 TestDataContainer

Excel ブック / YAML ディレクトリに相当するコンテナ。テストクラスと 1 対 1 に対応する。

```
TestDataContainer
  name: String                        // ブック名（拡張子なし）。例: "FooTest"
  sections: List<TestDataSection>     // セクション（読み込み単位）のリスト
```

### 5.2 TestDataSection

Excel シート / YAML ファイル 1 枚に相当する。NTF の読み込み単位。

```
TestDataSection
  name: String                        // シート名 / YAML ファイル名（拡張子なし）。例: "case01"
  blocks: List<TestDataBlock>         // データブロックのリスト
```

### 5.3 TestDataBlock

NTF の 1 データブロックに相当する。データブロック種別ごとにサブクラスを持つ。

```
TestDataBlock（抽象）
  dataType: DataType                  // データブロック種別（DataType 列挙値）
  groupId: String                     // groupId（省略時は空文字）
  identifier: String                  // 識別子の値（テーブル名・ファイルパス・LIST_MAP の ID 等）
```

#### 5.3.1 ColumnRowDataBlock（テーブル・LIST_MAP の共通基底）

`TableDataBlock` と `ListMapBlock` はカラム名リストとデータ行リストを共有するため、共通フィールドを抽象クラスに括り出す。

```
ColumnRowDataBlock extends TestDataBlock（抽象）
  columnNames: List<String>           // カラム名リスト（マーカーカラムを含む）
  rows: List<List<String>>            // データ行のリスト（null・空文字を区別して保持）
```

#### 5.3.2 TableDataBlock（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

```
TableDataBlock extends ColumnRowDataBlock
  （追加フィールドなし）
```

#### 5.3.3 ListMapBlock（LIST_MAP）

```
ListMapBlock extends ColumnRowDataBlock
  （追加フィールドなし）
```

`TableDataBlock` と `ListMapBlock` は `dataType` フィールド（`TestDataBlock` が保持）で区別する。

#### 5.3.4 FileDataBlock（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）

```java
/** ファイルデータブロックの種別。SETUP/EXPECTED を問わず固定長か可変長かを区別する。 */
enum FileType { FIXED, VARIABLE }
```

```
FileDataBlock extends TestDataBlock
  fileType: FileType                  // FIXED / VARIABLE（SETUP_FIXED/EXPECTED_FIXED → FIXED、SETUP_VARIABLE/EXPECTED_VARIABLE → VARIABLE）
  directives: Map<String, String>     // ディレクティブ（キー → 値）。Excel の行順を保持するため LinkedHashMap を使用する
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
  type: String      // データ型記号（"X", "N", "Z" 等）。可変長 FW_HEADER では null
  length: String    // フィールド長（固定長のみ。可変長は null。YAML 出力時は null の場合 length キーを省略する）
```

`length` を `String` 型にする理由: `"-"`（SS-17: 自動拡張指示）や `null`（可変長の長さなし）を区別せずにリテラルとして保持するため、数値型への変換は行わない。NTF 実行時に数値解釈が行われる。`FieldDef` は不変オブジェクトとして扱い、全フィールドを `final` で宣言する。

#### 5.3.5 MessageDataBlock（MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES）

```
MessageDataBlock extends TestDataBlock
  fwHeaderFields: Map<String, String>   // FW 制御ヘッダフィールド（FW_HEADER レコード）。Excel の行順を保持するため LinkedHashMap を使用する
  records: List<RecordLayout>           // レコードレイアウトのリスト（FieldDef は name のみ）
```

---

## 6. クラス設計

### 6.1 パッケージと配置

**パッケージ**

```
nablarch.test.tool.converter
```

**ソースディレクトリ**

変換ツールは `src/main/java` に配置する。`nablarch.test.tool` 配下には既存のツール群（`htmlcheck`・`sanitizingcheck` 等）が置かれており、変換ツールもその一つとして位置づける。

### 6.2 インターフェース

#### ConverterException

変換ツール専用の検査例外。IO エラー・書式エラー・上書き禁止エラーなど、変換処理で発生する全ての回復可能なエラーをこの例外でラップして伝播させる。`TestDataConverter` が catch して「エラーとして記録・スキップして続行」する基点となる。

```java
package nablarch.test.tool.converter;

/**
 * テストデータ変換ツール専用の検査例外。
 */
public class ConverterException extends Exception {
    public ConverterException(String message) { super(message); }
    public ConverterException(String message, Throwable cause) { super(message, cause); }
}
```

#### TestDataFormatReader

形式に依存しない読み込みインターフェース。

```java
package nablarch.test.tool.converter;

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
     * @throws ConverterException IO エラーまたは書式エラーが発生した場合
     */
    TestDataContainer read(Path sourcePath) throws ConverterException;
}
```

#### TestDataFormatWriter

形式に依存しない書き込みインターフェース。

```java
package nablarch.test.tool.converter;

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
     * @throws ConverterException IO エラーまたは上書き禁止エラーが発生した場合
     */
    void write(TestDataContainer container, Path outputPath, boolean overwrite) throws ConverterException;
}
```

### 6.3 実装クラス

各実装クラスの詳細な IN/OUT 仕様は 7 章に定める。本節ではクラスの役割と使用ライブラリを記載する。

#### XlsFormatReader

Apache POI を使用して `.xls` ファイルを読み込み、`TestDataContainer` を生成する（IN仕様: 7.1節）。

#### XlsFormatWriter

Apache POI の `HSSFWorkbook` を使用して `TestDataContainer` を `.xls` ファイルとして書き出す（OUT仕様: 7.2節）。NTF の既存テストデータは全て `.xls` 形式のため `.xlsx` 変換は本ツールのスコープ外とする。HSSF の制約として 1 ブック最大 65535 行・256 シートがあるが、NTF テストデータのサイズでは超過しない前提とする。既存ファイルが存在し `overwrite=false` の場合は `ConverterException` をスローする。

#### YamlFormatReader

SnakeYAML Engine を使用して YAML ディレクトリ内の `.yaml` ファイル群を読み込み、`TestDataContainer` を生成する（IN仕様: 7.3節）。`YamlSection.dataTypeToSectionKey()` に依存せず、7.3.1節のマッピングテーブルを使用する。

#### YamlFormatWriter

SnakeYAML Engine を使用して `TestDataContainer` を YAML ファイル群として書き出す（OUT仕様: 7.4節）。出力先ディレクトリが存在しない場合は自動生成する。既存ファイルが存在し `overwrite=false` の場合は `ConverterException` をスローする。

### 6.4 エントリポイント

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
- `System.exit()` は `main()` メソッドのみから呼び出す。内部ロジックは終了コードを `int` で返す `run(String[])` メソッドに分離し、テスト時は `run()` を直接呼び出して終了コードを検証する
- `run()` メソッドは各ファイルに対して `reader.read()` および `writer.write()` を `try-catch(ConverterException)` で囲む。`ConverterException` をキャッチした場合はエラー件数を加算してファイルをスキップし、次のファイルの処理を継続する。全ファイルの処理完了後にエラー件数 > 0 であれば終了コード 1 を返す

**引数仕様**

```
TestDataConverter --from <形式> --to <形式> [options] <入力パス> <出力パス>
```

| 引数 | 必須 | 説明 |
|---|---|---|
| `--from <形式>` | 必須 | 入力形式。`xls` または `yaml`。`--to` と同一形式は不可（終了コード 2） |
| `--to <形式>` | 必須 | 出力形式。`xls` または `yaml`。`--from` と異なる形式を指定すること |
| `--include <パターン>` | 任意（複数可） | 変換対象に含めるファイル名グロブパターン（3.2 節参照）。複数指定可 |
| `--exclude <パターン>` | 任意（複数可） | 変換対象から除外するファイル名グロブパターン（3.2 節参照）。複数指定可 |
| `--overwrite` | 任意 | 既存ファイルを上書きする（デフォルト: 上書き禁止） |
| `--delete-source` | 任意 | 変換成功後に入力ファイルを削除する |
| `<入力パス>` | 必須 | 変換対象のルートディレクトリ |
| `<出力パス>` | 必須 | 変換結果の出力先ルートディレクトリ |

### 6.5 ユーティリティクラス

#### ConverterFileFilter

変換対象ファイルの列挙・除外判定を担当する。

**責務**

- 指定ルートディレクトリを再帰走査して変換対象ファイルを列挙する
- `--include` / `--exclude` で指定されたファイル名グロブパターン（3.2 節参照）に従って変換対象を絞り込む。グロブ評価には `java.nio.file.PathMatcher`（`glob:` 構文）をファイル名部分に適用する
- Excel 読み込み時は `.xls` ファイルを、YAML 読み込み時は YAML ディレクトリ（3.3 節「YAML ディレクトリの定義」参照: 直下に `.yaml` ファイルを 1 件以上含み、`.yaml` ファイルを含むサブディレクトリを持たない最下位ディレクトリ）を列挙する

#### ConverterPathResolver

入力パスと出力パスの対応関係を計算するユーティリティクラス。

**責務**

- Excel ファイルパスから YAML 出力ディレクトリパスを計算する
- YAML ディレクトリパスから Excel 出力ファイルパスを計算する
- 入力パスと出力パスのルートを考慮した相対パス計算を行う

---

## 7. 形式別 IN/OUT 仕様

各形式の Reader（IN: ファイル → `TestDataContainer`）と Writer（OUT: `TestDataContainer` → ファイル）の仕様を形式ごとに独立して定める。変換は Reader + Writer の組み合わせであり、本章は組み合わせに依存しない。

---

### 7.1 XLS 形式 IN 仕様（`XlsFormatReader`）

`.xls` ファイルを読み込んで `TestDataContainer` を生成する。

#### 7.1.1 セル値の読み取り規則

- 全セルを `Cell.toString()` で文字列化する（数値書式・日付書式セルは変換精度が落ちる場合がある。1.2節「前提条件」参照）
- `null` セル（空セル）は空文字 `""` として扱う
- 先頭セルが `//` で始まる行はコメント行としてスキップし、コメント行数を集計して警告出力する（HC-05）
- 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨てる（HC-06）。**注意**: 既存の `PoiXlsReader` は先頭カラムが `//` の場合のみ break する実装で、先頭以外のセルの切り捨ては行っていない。しかし NTF の `TestDataParsingTemplate.cutComment()` が最終的に行内コメント切り捨てを担うため、変換ツールは HC-06 仕様（先頭以外のセルも切り捨て）を実装することで変換等価性を保つ
- 全セルが空の行はスキップする（HC-07）

#### 7.1.2 データブロック識別行の解析

シートを走査し、先頭セルが `DataType.getName()` で前方一致する行をデータブロック識別行として検出する。

識別行検出のロジック:
1. 行の先頭セルの値を取得する
2. `DataType` の全列挙値の `getName()` と前方一致（`startsWith`）で比較する。ただし `DataType.DEFAULT`（`getName()` が `"DEFAULT"`）は対象外とする。先頭セルが `"DEFAULT"` で始まる行が出現した場合はエラーとして記録してスキップする
3. 合致した場合、`[groupId]=identifier` 形式を解析して `dataType`・`groupId`・`identifier` を抽出する

#### 7.1.3 テーブルデータブロックの解析（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

識別行の直後の行をヘッダ行（カラム名リスト）、それ以降の行をデータ行として解析する。

```
行1: SETUP_TABLE=USER_MASTER  [空]   [空]
行2: USER_ID                  NAME   AGE       ← ヘッダ行
行3: 001                      taro   20        ← データ行
行4: 002                      jiro   30        ← データ行
```

解析ルール:
- ヘッダ末尾の空カラムは除去する（HC-03）
- データ行がヘッダより短い場合、不足分は空文字 `""` として補完する（HC-04）
- マーカーカラム（`[カラム名]` 形式）は `[` `]` を含めてそのまま `columnNames` に保持する（HC-01）

`TableDataBlock` に格納:

```
TableDataBlock {
  dataType = SETUP_TABLE_DATA
  identifier = "USER_MASTER"
  columnNames = ["USER_ID", "NAME", "AGE"]
  rows = [["001", "taro", "20"], ["002", "jiro", "30"]]
}
```

#### 7.1.4 LIST_MAP ブロックの解析

テーブルデータブロックと同じ解析規則。`ListMapBlock` に格納する。

#### 7.1.5 ファイルデータブロックの解析（SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE）

識別行の後に続く行を以下の順序で解析する。

1. **ディレクティブ行**（0 行以上）: 先頭セルが非空かつ DataType 名で始まらない行の中で、次行の先頭セルも非空なもの
2. **フィールド名行**: 先頭セルが非空かつ DataType 名で始まらない行の中で、次行の先頭セルが空なもの（先頭セル = レコード種別名、2列目以降 = フィールド名）
3. **データ型行**: 先頭セルが空、2列目以降 = データ型記号
4. **フィールド長行**（固定長のみ）: 先頭セルが空、2列目以降 = フィールド長（数値または `"-"`）
5. **データ行**（1行以上）: 先頭セルが空、2列目以降 = フィールド値

ディレクティブ行とフィールド名行の判別は**1行先読み**で行う。次行の先頭セルが空 → フィールド名行、非空 → ディレクティブ行。

**状態遷移**

| 状態 | 現在行の条件 | 遷移先 |
|---|---|---|
| `BLOCK_START` / `DIRECTIVE` | 先頭セルが非空かつ DataType 名で始まらない、かつ次行の先頭セルが非空 | `DIRECTIVE`（ディレクティブ行として読む） |
| `BLOCK_START` / `DIRECTIVE` | 先頭セルが非空かつ DataType 名で始まらない、かつ次行の先頭セルが空 | `FIELD_NAMES`（フィールド名行として読む） |
| `FIELD_NAMES` | 先頭セルが空（直後の型記号行） | `DATA_TYPES` |
| `DATA_TYPES` | 先頭セルが空、固定長の場合 | `FIELD_LENGTHS` |
| `DATA_TYPES` | 先頭セルが空、可変長の場合（長さ行スキップ） | `DATA` |
| `FIELD_LENGTHS` | 先頭セルが空 | `DATA` |
| `DATA` | 先頭セルが空 | `DATA` 継続（次のデータ行） |
| `DATA` | 先頭セルが非空かつ次行の先頭セルが空（新レコード種別名） | `FIELD_NAMES`（新 `RecordLayout` を追加） |
| いずれかの状態 | DataType 識別行を検出 | 新データブロック開始 |
| `BLOCK_START` / `DIRECTIVE` / `FIELD_NAMES` / `DATA_TYPES` / `FIELD_LENGTHS` | EOF | ブロック解析完了。`FIELD_NAMES` / `DATA_TYPES` / `FIELD_LENGTHS` 状態でのEOFはエラーとして記録 |
| `DATA` | EOF | データブロック解析を正常に完了する |

入力例（固定長・エンコーディング付き）:

```
行1: SETUP_FIXED=input/data.dat  [空]    [空]    [空]
行2: text-encoding               MS932  [空]    [空]
行3: DATA                        USER_ID AMOUNT [空]
行4: [空]                        X       Z      [空]
行5: [空]                        10      10     [空]
行6: [空]                        001     5000   [空]
```

`FileDataBlock` に格納:

```
FileDataBlock {
  dataType = SETUP_FIXED
  fileType = FIXED
  identifier = "input/data.dat"
  directives = {"text-encoding": "MS932"}
  records = [
    RecordLayout {
      recordType = "DATA"
      fields = [FieldDef{name="USER_ID", type="X", length="10"}, FieldDef{name="AMOUNT", type="Z", length="10"}]
      rows = [["001", "5000"]]
    }
  ]
}
```

**空ファイル表現**: レコード定義なし（ディレクティブのみ）の場合、`records` は空リスト。

**`"-"` フィールド長（SS-17）**: `"-"` はリテラル文字列として `FieldDef.length` に格納する。NTF実行時の自動拡張は変換ツールの責務外。

#### 7.1.6 メッセージングデータブロックの解析（MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES）

ファイルデータブロック（7.1.5節）と同じ構造で解析するが、FW 制御ヘッダ行の扱いが異なる。

- **FW 制御ヘッダ行**: ディレクティブ行として読み込む（先頭セル = フィールド名、2列目 = 値）。これを `MessageDataBlock.fwHeaderFields` に格納する
- **`no` 列**: フィールド名行の先頭セルが空（`no` フィールドはフィールド名から省略されている）

```
MESSAGE=sendSyncTestData/REQ001/message
requestId  REQ001                          ← FW制御ヘッダ行
userId     usr001                          ← FW制御ヘッダ行
[空]  FIELD1  FIELD2                       ← フィールド名行（先頭セル空 = no列）
[空]  X       X                            ← データ型行
[空]  req1    data1                        ← データ行
```

`MessageDataBlock` に格納:

```
MessageDataBlock {
  dataType = MESSAGE
  identifier = "sendSyncTestData/REQ001/message"
  fwHeaderFields = {"requestId": "REQ001", "userId": "usr001"}  ← LinkedHashMap（行順保持）
  records = [
    RecordLayout {
      recordType = "default"
      fields = [FieldDef{name="FIELD1", type="X"}, FieldDef{name="FIELD2", type="X"}]
      rows = [["req1", "data1"]]
    }
  ]
}
```

**FW ヘッダフィールド名の判定**: NTF の `MessageParser` / `YamlMessageBuilder` は `SystemRepository` の `reader.fwHeaderfields` でどのフィールドが FW ヘッダかを判定するが、変換ツールは SystemRepository から独立して動作する。変換ツールは Excel のディレクティブ行（次行先頭セルが非空の行）を全て FW ヘッダとして扱う（デフォルト4フィールドの場合と同じ結果）。`reader.fwHeaderfields` をカスタム設定している場合の変換等価性は保証しない。

---

### 7.2 XLS 形式 OUT 仕様（`XlsFormatWriter`）

`TestDataContainer` を `.xls` ファイルとして書き出す。POI の `HSSFWorkbook` を使用する。

#### 7.2.1 セル値の書き出し規則

- 全セルを文字列書式で書き出す（NTF の動作保証条件に合わせる）
- `null` 値はセルに文字列 `"null"` と書き出す
- 空文字 `""` はセルを空（書き込まない）にする

#### 7.2.2 データブロック識別行の生成

`TestDataBlock` の `dataType`・`groupId`・`identifier` から識別行を生成する。

```
groupId が空文字 → SETUP_TABLE=USER_MASTER
groupId が "case01" → SETUP_TABLE[case01]=USER_MASTER
```

#### 7.2.3 テーブルデータブロックの書き出し

識別行 → ヘッダ行（`columnNames`）→ データ行（`rows` の各行）の順で書き出す。

- マーカーカラム（`[カラム名]` 形式）は `[` `]` を含めてそのままヘッダ行に書き出す（HC-01）

#### 7.2.4 ファイルデータブロックの書き出し

識別行 → ディレクティブ行群 → レコードレイアウト群（フィールド名行 → データ型行 → フィールド長行（固定長のみ）→ データ行群）の順で書き出す（SS-08）。

- データ行の先頭セルは空にする（SS-13）
- 可変長の場合はフィールド長行を省略する
- `records` が空リストの場合はディレクティブ行のみを書き出す

#### 7.2.5 メッセージングデータブロックの書き出し

識別行 → FW ヘッダ行群（`fwHeaderFields` の各エントリを `fieldName | value` 形式で書き出す）→ レコードレイアウト群の順で書き出す。

---

### 7.3 YAML 形式 IN 仕様（`YamlFormatReader`）

YAML ディレクトリ内の `.yaml` ファイル群を読み込んで `TestDataContainer` を生成する。

#### 7.3.1 トップレベルキーと DataType の対応

| YAML キー | `YamlSection` 定数名 | DataType（enum 定数名） | TestDataBlock サブクラス |
|---|---|---|---|
| `setup_tables` | `KEY_SETUP_TABLES` | `SETUP_TABLE_DATA` | `TableDataBlock` |
| `expected_tables` | `KEY_EXPECTED_TABLES` | `EXPECTED_TABLE_DATA` | `TableDataBlock` |
| `expected_complete_tables` | `KEY_EXPECTED_COMPLETE_TABLES` | `EXPECTED_COMPLETED` | `TableDataBlock` |
| `list_maps` | `KEY_LIST_MAPS` | `LIST_MAP` | `ListMapBlock` |
| `setup_files` + `type: fixed` | `KEY_SETUP_FILES` | `SETUP_FIXED` | `FileDataBlock` |
| `setup_files` + `type: variable` | `KEY_SETUP_FILES` | `SETUP_VARIABLE` | `FileDataBlock` |
| `expected_files` + `type: fixed` | `KEY_EXPECTED_FILES` | `EXPECTED_FIXED` | `FileDataBlock` |
| `expected_files` + `type: variable` | `KEY_EXPECTED_FILES` | `EXPECTED_VARIABLE` | `FileDataBlock` |
| `messages` | `KEY_MESSAGES` | `MESSAGE` | `MessageDataBlock` |
| `expected_request_header_messages` | `KEY_EXPECTED_REQUEST_HEADER_MESSAGES` | `EXPECTED_REQUEST_HEADER_MESSAGES` | `MessageDataBlock` |
| `expected_request_body_messages` | `KEY_EXPECTED_REQUEST_BODY_MESSAGES` | `EXPECTED_REQUEST_BODY_MESSAGES` | `MessageDataBlock` |
| `response_header_messages` | `KEY_RESPONSE_HEADER_MESSAGES` | `RESPONSE_HEADER_MESSAGES` | `MessageDataBlock` |
| `response_body_messages` | `KEY_RESPONSE_BODY_MESSAGES` | `RESPONSE_BODY_MESSAGES` | `MessageDataBlock` |

**注意**: 既存の `YamlSection.dataTypeToSectionKey()` はメッセージ系 DataType のみ対応し、テーブル系・ファイル系では `IllegalArgumentException` をスローする。`YamlFormatReader` はこのメソッドに依存せず、上記マッピングテーブルを使用する。

#### 7.3.2 値の読み取り規則

- SnakeYAML Engine は YAML 1.2 Core Schema に従い、`null`/`NULL`/`Null`/`~` を Java null に変換する。Java null は `TestDataBlock` の行データで `null` として保持する
- 文字列値（ダブルクォートあり）はそのまま Java String として保持する
- `group_id:` フィールドが存在する場合、`TestDataBlock.groupId` に設定する。なければ空文字

#### 7.3.3 ファイルデータブロックの解析

- `type: fixed` → `FileType.FIXED`、`type: variable` → `FileType.VARIABLE`
- `setup_files` / `expected_files` のリスト要素順序は `TestDataSection.blocks` への格納順として保持する

#### 7.3.4 メッセージングデータブロックの解析

- `record_type: FW_HEADER` のレコードの `fields` × `rows[0]` から `fwHeaderFields`（LinkedHashMap）を構築する
- フィールド名が `fwHeaderFields`（SystemRepository 設定）に含まれるかの検証は行わない

---

### 7.4 YAML 形式 OUT 仕様（`YamlFormatWriter`）

`TestDataContainer` を YAML ファイル群として書き出す。SnakeYAML Engine を使用する。

#### 7.4.1 値の書き出し規則

| `TestDataBlock` の値 | YAML 出力 |
|---|---|
| `null`（Java null） | アンクォートの `null` |
| `""` （空文字列） | `""` （ダブルクォートで出力する） |
| `"null"` / `"Null"` / `"NULL"` | `"null"`（ダブルクォートあり。YAML 1.2 の null と区別するため） |
| `"true"` / `"false"` | `"true"` / `"false"`（ダブルクォートあり） |
| `"001"` 等の先頭ゼロ付き数値文字列 | `"001"`（ダブルクォートあり） |
| その他の文字列 | ダブルクォートで出力する |

#### 7.4.2 テーブルデータブロックの書き出し

```yaml
setup_tables:
  - table: "USER_MASTER"          # groupId なしの場合
    rows:
      - USER_ID: "001"
        NAME: "taro"
        "[FLAG]": "X"             # マーカーカラムはそのまま
```

`group_id` が空文字でない場合は `group_id: "case01"` を `table:` の前に出力する。

#### 7.4.3 ファイルデータブロックの書き出し

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

- `records` が空リストの場合、`records: []` として出力する
- 可変長の `FieldDef.length` が `null` の場合、`length` キーを省略する

#### 7.4.4 メッセージングデータブロックの書き出し

`fwHeaderFields` を `record_type: FW_HEADER` のレコードとして出力する。

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

`FW_HEADER` の `fields` には `name` のみを出力する（`YamlMessageBuilder` が `type`/`length` を参照しないため）。

---

### 7.5 groupId の表現（全形式共通）

| `TestDataBlock.groupId` | XLS 識別行 | YAML フィールド |
|---|---|---|
| `""` （空文字） | `SETUP_TABLE=USER_MASTER` | `group_id` キーなし |
| `"case01"` | `SETUP_TABLE[case01]=USER_MASTER` | `group_id: "case01"` |

---

### 7.6 ディレクティブ値の特殊文字（DR-09, DR-10）

ディレクティブ値は原則として文字列としてそのまま保持する。変換ツールは値の意味解釈を行わない。

| ディレクティブキー | 値の例 | 変換ツールの扱い |
|---|---|---|
| `field-separator` | `","` / `"\\t"` | 文字列としてそのまま保持。タブ文字への変換は NTF 実行時の責務 |
| `record-separator` | `NONE` / `CR` / `LF` / `CRLF` | 文字列としてそのまま保持 |

ディレクティブ値の有効性検証（未知のキー・不正な値）は変換ツールの責務外。NTF 実行時に検出される。

---

## 8. 実行方法

### 8.1 pom.xml 設定

`exec-maven-plugin` を `pom.xml` に追加する。

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <configuration>
    <mainClass>nablarch.test.tool.converter.TestDataConverter</mainClass>
    <classpathScope>compile</classpathScope>
  </configuration>
</plugin>
```

`TestDataConverter` クラスは `src/main/java` に配置するため（6.1 節参照）、`classpathScope` は `compile` とする。

### 8.2 コマンド例

#### Excel → YAML 変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.tool.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml <入力パス> <出力パス>"
```

- デフォルトでは既存 `.yaml` ファイルがあればエラー（`--overwrite` で上書き許可）

#### 上書き許可での変換

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.tool.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite <入力パス> <出力パス>"
```

#### 変換後に元 Excel を削除

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.tool.converter.TestDataConverter \
  -Dexec.args="--from xls --to yaml --overwrite --delete-source <入力パス> <出力パス>"
```

#### YAML → Excel

```bash
mvn exec:java \
  -Dexec.mainClass=nablarch.test.tool.converter.TestDataConverter \
  -Dexec.args="--from yaml --to xls <入力パス> <出力パス>"
```

### 8.3 引数仕様（再掲）

```
TestDataConverter --from <形式> --to <形式> [--include <パターン>]... [--exclude <パターン>]... [--overwrite] [--delete-source] <入力パス> <出力パス>
```

| 引数 | 値 | 説明 |
|---|---|---|
| `--from` | `xls` / `yaml` | 入力形式（`--to` と同一形式は不可） |
| `--to` | `xls` / `yaml` | 出力形式（`--from` と異なる形式を指定） |
| `--include` | グロブパターン（複数可） | 変換対象に含めるファイル名パターン（3.2 節参照） |
| `--exclude` | グロブパターン（複数可） | 変換対象から除外するファイル名パターン（3.2 節参照） |
| `--overwrite` | フラグ | 既存ファイルを上書きする |
| `--delete-source` | フラグ | 変換成功後に入力ファイルを削除する |
| `<入力パス>` | パス文字列 | 変換対象のルートディレクトリ |
| `<出力パス>` | パス文字列 | 変換結果の出力先ルートディレクトリ |

### 8.4 終了コード

| 終了コード | 意味 |
|---|---|
| `0` | 全ファイルの変換が成功した |
| `1` | 1 件以上の変換エラーが発生した（未変換ファイルあり） |
| `2` | 引数エラー（`--from` / `--to` の値が不正、必須引数の欠落等） |

---

## 9. エラー処理方針

### 9.1 基本方針

- 1 ファイルのエラーで全体を停止しない。エラーが発生したファイルをスキップして次のファイルの変換を継続する
- 全ファイルの処理完了後にサマリーを出力し、エラーがあれば終了コード 1 で終了する
- エラーメッセージにはファイルパスと原因を含める

### 9.2 エラーケースと対処

| エラーケース | 対処 |
|---|---|
| 入力ファイルが存在しない | エラーとして記録し、スキップして続行 |
| 入力ファイルが読み取れない（IO エラー・破損） | エラーとして記録し、スキップして続行 |
| 変換先ファイルが存在し `--overwrite` 未指定 | エラーとして記録し、スキップして続行 |
| データブロック識別行の書式が不正 | エラーとして記録し、対象ファイルをスキップして続行 |
| フィールド名/型/長さリストのサイズ不一致 | エラーとして記録し、対象ファイルをスキップして続行 |
| YAML の `records:` 内で `rows:` 要素数と `fields:` 件数が不一致 | エラーとして記録し、対象ファイルをスキップして続行 |
| 引数が不正（`--from` の値が `xls`/`yaml` 以外、`--from` と `--to` が同一形式等） | 即時終了コード 2 で終了。ヘルプメッセージを出力する |

### 9.3 警告ケースと対処

| 警告ケース | 対処 |
|---|---|
| コメント行（`//`）が存在する | 標準エラー出力に警告を出力し、コメント行を読み捨てて処理を継続する |
| `--exclude` パターンに合致するファイル（または `--include` パターンに合致しないファイル） | 標準出力にスキップメッセージを出力し、スキップして続行 |
| 数値書式・日付書式セルが検出された（1.2 節「前提条件」違反） | 標準エラー出力に警告を出力する（セル値は POI の `Cell.toString()` 結果をそのまま使用し、処理は継続する） |
| データブロックが 0 件の TestDataSection（空シート / コメント行のみのシート） | 標準エラー出力に警告を出力し、YAML ファイルの生成をスキップする |

### 9.4 変換サマリー出力例

```
=== TestDataConverter 変換サマリー ===
変換成功: 59 件
スキップ: 2 件（除外パターン合致）
エラー:   0 件
コメント行ロスト: 12 行（3 ファイル）
```
