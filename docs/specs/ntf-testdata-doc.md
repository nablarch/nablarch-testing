# NTF テストデータ解説書

- **対象**: Nablarch Testing Framework（NTF）が読み込むテストデータの書き方・構造・ルール
- **形式非依存**: Excel・YAML のどちらで記述する場合にも共通して適用されるルールを説明します
- **記述例**: 各節末尾のリンクから Excel 表と YAML コードブロックの対比例を参照できます

---

## 目次

1. [NTF テストデータとは](#1-ntf-テストデータとは)
2. [テストデータの基本構造](#2-テストデータの基本構造)
3. [セクション識別](#3-セクション識別)
4. [テストケース定義](#4-テストケース定義)
5. [テーブルデータ](#5-テーブルデータ)
6. [ファイルデータ](#6-ファイルデータ)
7. [メッセージングテストデータ](#7-メッセージングテストデータ)
8. [値の書き方](#8-値の書き方)
9. [ディレクティブ](#9-ディレクティブ)
10. [ヘッダ・コメント・空エントリ](#10-ヘッダコメント空エントリ)
11. [DB アサート](#11-db-アサート)

---


## 1. NTF テストデータとは

NTF テストデータファイルには、次の3種類のデータを記述します。

**テストケース**  
テストの実行条件を1エントリ1ケースで定義します。各エントリが1テストケースを表します。リクエスト単体テスト（ウェブアプリケーション）なら「ユーザ ID・期待ステータスコード・期待フォワード先 URI」など、リクエスト単体テスト（バッチ処理）なら「リクエストパス・ユーザ ID・DI コンフィグ・期待ステータスコード」などを列挙します。

**セットアップ**  
テスト実行前に投入するデータです。DB テーブルへの INSERT データ、固定長・可変長ファイルの入力データなどを定義します。

**検証**  
テスト後の検証に使うデータです。DB の期待値、出力ファイルの期待値、電文の期待値、ログや検索結果等の期待値などを定義します。

これらは**セクション**という単位で管理され、DataType 名と識別子の値の組み合わせで区別されます。1つのファイルに複数種別のセクションを共存させることができます。セクションの記述順序は問いません。

→ [Excel / YAML Example](ntf-testdata-doc-examples-overview.md#overview)

---

## 2. テストデータの基本構造

テストデータはテストクラスと1対1で対応します。

**Excel** では、テストクラスと同名の1つのブック（`.xls` ファイル）にすべてのテストデータを格納します。シートを分割単位とし、1シートが1つの読み込み単位になります。

**YAML** では、テストクラスと同名のディレクトリを作成し、その下にファイルを配置します。1ファイルが1つの読み込み単位になり、Excel の1シートに相当します。

```
【Excel】
src/test/java/com/example/
  FooTest.xls          ← テストクラス FooTest に対応する1ブック
    ├── case01         ← シート（読み込み単位）
    └── case02         ← シート（読み込み単位）

【YAML】
src/test/java/com/example/
  FooTest/             ← テストクラス FooTest に対応するディレクトリ
    ├── case01.yaml    ← ファイル（読み込み単位）= Excel の case01 シートに相当
    └── case02.yaml    ← ファイル（読み込み単位）= Excel の case02 シートに相当
```

読み込み単位（Excel の1シート / YAML の1ファイル）の中に、テストケース・セットアップ・検証の複数セクションを共存させて記述します。

**ファイルの読み込みルール**

| 項目 | Excel | YAML |
|---|---|---|
| ファイルなし時の動作 | ファイルが存在しない場合はエラーになる | ファイルが存在しない、またはパースに失敗した場合はエラーになる |
| 空ファイル時の動作 | 空シートは存在しないシート扱いとなる | 空ファイル（0バイト）は空データとして扱われる（エラーにはならない） |
| キャッシュ | Workbook 単位で LRU キャッシュ（最大1件）で管理される | ファイル単位で LRU キャッシュ（最大8件）で管理される。テスト間のキャッシュ汚染を防ぐには `YamlTestDataParser.clearCacheForTest()` を呼び出す |
| セル書式 | セルは必ず**文字列書式**で記述すること。数値・日付書式の場合の動作は保証しない | 値の型変換ルールは [8章](#8-値の書き方) を参照 |

---

## 3. セクション識別

### 3.1 セクション識別の構成要素

各セクションは **DataType 名** と **識別子の値** の2要素で識別されます。

- **DataType 名**: 後述する14種類のいずれか（例: `SETUP_TABLE`）
- **識別子の値**: テーブル名・ファイルパス・ID などセクション種別ごとの識別子


#### Excel での記述

Excel ではセクション先頭セルに `DataType名=識別子の値` 形式で記述します。DataType 名で始まれば合致します（前方一致）。

```
SETUP_TABLE=USER_MASTER
```

#### YAML での記述

YAML ではセクション種別ごとに専用のトップレベルキーを使用します。

| 論理 DataType 名 | YAML キー |
|---|---|
| `SETUP_TABLE` | `setup_tables` |
| `EXPECTED_TABLE` | `expected_tables` |
| `EXPECTED_COMPLETE_TABLE` | `expected_complete_tables` |
| `LIST_MAP` | `list_maps` |
| `SETUP_FIXED` / `SETUP_VARIABLE` | `setup_files` |
| `EXPECTED_FIXED` / `EXPECTED_VARIABLE` | `expected_files` |
| `MESSAGE` | `messages` |
| `EXPECTED_REQUEST_HEADER_MESSAGES` | `expected_request_header_messages` |
| `EXPECTED_REQUEST_BODY_MESSAGES` | `expected_request_body_messages` |
| `RESPONSE_HEADER_MESSAGES` | `response_header_messages` |
| `RESPONSE_BODY_MESSAGES` | `response_body_messages` |

```yaml
setup_tables:
  - table: USER_MASTER
    rows: ...
```

- 完全なセクションキーを使用するため前方一致は発生しません
- YAML では同一ファイル内のトップレベルキーの重複は禁止です（`IllegalStateException` がスローされます）。同種のデータは同一キーにリストとして並べて記述します
- Excel では同一シート内に同種セクションを複数記述できます。GroupData は全件収集、SingleData は先着一致です

### 3.2 DataType の種類

テストデータで使用できる DataType は以下の14種類です。

| DataType名 | 用途 | 収集方式 |
|---|---|---|
| `SETUP_TABLE` | INSERT 用テーブルデータ | GroupData（全件収集） |
| `EXPECTED_TABLE` | 比較用テーブルデータ（省略カラムは比較対象外） | GroupData（全件収集） |
| `EXPECTED_COMPLETE_TABLE` | 比較用テーブルデータ（省略カラムにデフォルト値補完） | GroupData（全件収集） |
| `LIST_MAP` | キーバリュー形式の汎用データ（テストケース定義・期待値等） | SingleData（先着一致） |
| `SETUP_FIXED` | 固定長ファイルの入力データ | GroupData（全件収集） |
| `EXPECTED_FIXED` | 固定長ファイルの期待値データ | GroupData（全件収集） |
| `SETUP_VARIABLE` | 可変長ファイルの入力データ | GroupData（全件収集） |
| `EXPECTED_VARIABLE` | 可変長ファイルの期待値データ | GroupData（全件収集） |
| `MESSAGE` | メッセージング電文データ | SingleData（先着一致） |
| `EXPECTED_REQUEST_HEADER_MESSAGES` | 要求電文ヘッダの期待値 | GroupData（`testShots` の `expectedMessage` カラムで groupId 指定）または SingleData（ID 直接指定） |
| `EXPECTED_REQUEST_BODY_MESSAGES` | 要求電文ボディの期待値 | GroupData（`testShots` の `expectedMessage` カラムで groupId 指定）または SingleData（ID 直接指定） |
| `RESPONSE_HEADER_MESSAGES` | 応答電文ヘッダデータ | GroupData（`testShots` の `responseMessage` カラムで groupId 指定）または SingleData（ID 直接指定） |
| `RESPONSE_BODY_MESSAGES` | 応答電文ボディデータ | GroupData（`testShots` の `responseMessage` カラムで groupId 指定）または SingleData（ID 直接指定） |
| `DEFAULT` | フレームワーク内部用（通常使用しません） | — |

### 3.3 GroupData と SingleData

セクションの収集方式は DataType によって異なります。

- **GroupData**: 同じグループに属するセクションをすべて収集します。ファイル全体を最後まで読み込みます（`SETUP_TABLE`、`EXPECTED_TABLE`、ファイル系など）
- **SingleData**: 最初に一致したセクション1件だけを取得して停止します（`LIST_MAP`、`MESSAGE` など）。同一 ID のエントリが複数ある場合、2件目以降は無視されます

グループの指定方法（groupId）については [4.4 セクションのグループ化](#44-セクションのグループ化groupid) を参照してください。

---

## 4. テストケース定義

### 4.1 testShots

`testShots` はテストケース定義の予約 ID です。フレームワークがこの ID を自動的に読み込み、各エントリを1テストケースとして実行します。旧 ID `testCases` は後方互換性のためフォールバックとして残存します。

テストが実行されるためには `testShots` に1件以上のエントリが必要です。0件の場合は例外がスローされます。

- **Excel**: `LIST_MAP=testShots` セクションに記述します
- **YAML**: `list_maps:` 下の `id: testShots` エントリに記述します

→ [処理方式別 testShots カラム一覧](ntf-testdata-doc-examples-testshots.md)

### 4.2 testShots のカラム仕様

testShots の各カラムは処理方式（ウェブアプリケーション / バッチ / メッセージング / エンティティバリデーション）によって異なります。詳細は [処理方式別 testShots カラム一覧](ntf-testdata-doc-examples-testshots.md) を参照してください。

#### 全処理方式共通の注意事項

- `no` カラムが空の場合は `IllegalArgumentException` がスローされます
- `description` カラムと `case` カラムのどちらも未定義の場合は `IllegalStateException` がスローされます

#### 主なカラムの動作

| カラム名 | 対象処理方式 | 動作 |
|---|---|---|
| `no` | 全方式（必須） | テストケース番号 |
| `description` / `case` | 全方式（いずれか必須） | テストケースの説明。`case` は旧称で後方互換として残存 |
| `context` | HTTP（必須） | `REQUEST_ID`・`USER_ID` 等を含む `LIST_MAP` 名を指定します。1行のみ有効。`REQUEST_ID` が空の場合は `IllegalArgumentException` がスローされます |
| `setUpTable` | 全方式 | この値と同じ groupId を持つ `SETUP_TABLE` セクションを収集して INSERT します。空の場合はスキップされます |
| `expectedTable` | 全方式 | この値と同じ groupId を持つ `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` セクションで DB を検証します。空の場合はスキップされます |
| `setUpFile` | バッチ系 | この値と同じ groupId を持つ `SETUP_FIXED` / `SETUP_VARIABLE` セクションを入力ファイルとして配置します。空の場合はスキップされます |
| `expectedFile` | バッチ系 | この値と同じ groupId を持つ `EXPECTED_FIXED` / `EXPECTED_VARIABLE` セクションで出力ファイルを検証します。空の場合はスキップされます |
| `expectedLog` | バッチ系 | 期待ログの `LIST_MAP` 名を指定します。空の場合はスキップされます。指定した LIST_MAP が空の場合は `IllegalStateException` がスローされます |
| `requestParams` | HTTP | HTTP リクエストパラメータの予約 ID。対応する `LIST_MAP` からパラメータを読み込みます。`LIST_MAP` の行数がテストケース数より少ない場合は `IllegalArgumentException` がスローされます |
| `responseResult` | HTTP | HTTP レスポンス（リクエストスコープ）期待値の予約 ID |
| `params` | エンティティバリデーション | 入力パラメータ定義の予約 ID（`EntityTestSupport` 専用）。`testShots` の行数と一致が必須です（不一致で `IllegalArgumentException` がスローされます） |
| `title` | エンティティバリデーション（必須） | テストケースの説明 |
| `expectedMessageId1` | エンティティバリデーション（必須） | 期待するバリデーションメッセージ ID |
| `propertyName1` | エンティティバリデーション（必須） | バリデーション対象プロパティ名 |
| `cookie` | HTTP | Cookie 値の `LIST_MAP` 名を指定します。空の場合は Cookie なし。指定した LIST_MAP が空の場合は `IllegalArgumentException` がスローされます |
| `queryParams` | HTTP | クエリパラメータの `LIST_MAP` 名を指定します。空の場合はパラメータなし。指定した LIST_MAP が空の場合は `IllegalArgumentException` がスローされます |
| `HTTP_METHOD` | HTTP | HTTP メソッド。空の場合は `"POST"` が使用されます |
| `expectedContentLength` | HTTP | 期待する Content-Length。空の場合は検証をスキップします |
| `expectedContentType` | HTTP | 期待する Content-Type。空の場合は検証をスキップします |
| `expectedContentFileName` | HTTP | 期待する Content-Disposition ファイル名。空の場合は検証をスキップします |
| `args[0]`, `args[1]`, ... | バッチ | コマンドライン引数として渡されます |

### 4.3 DB 共通セットアップデータ

`setUpDb` はテストメソッド共通の DB 初期化データを定義する予約 ID です。テストメソッド開始時に1度だけ `SETUP_TABLE` データが投入されます。

### 4.4 セクションのグループ化（groupId）

複数のテストケースで異なるセットアップデータや期待値を使い分けたい場合、セクションに **groupId** を付加してグループ化します。`testShots` の各カラム（`setUpTable` / `expectedTable` / `setUpFile` / `expectedFile` 等）に groupId の値を指定すると、そのテストケースでは対応する groupId を持つセクションだけが収集されます。

#### Excel での記述

DataType 名の直後に `[groupId]` と記述します。

```
SETUP_TABLE[case01]=USER_MASTER
```

#### YAML での記述

`group_id:` フィールドで指定します。

```yaml
setup_tables:
  - group_id: case01
    table: USER_MASTER
    rows: ...
```

#### 制約

- 省略時は空文字扱いです（groupId なし = デフォルトグループ）
- groupId の指定は1件のみ有効です。2件以上指定すると `IllegalArgumentException` がスローされます

バッチ固有の動作として、groupId に `"default"` を指定するとグループ ID なし扱いと同等になります。

→ [Excel / YAML Example](ntf-testdata-doc-examples-overview.md#groupid)

---

## 5. テーブルデータ

### 5.1 データの形式

テーブルデータの各エントリはカラム名と値の組み合わせで記述します。省略したカラムには INSERT 時にデフォルト値が補完されます。

**Excel**: 1行目にカラム名、2行目以降にデータを記述します。

```
| SETUP_TABLE=テーブル名 | | |
| カラム1 | カラム2 | カラム3 |
| 値1     | 値2     | 値3     |
```

**YAML**: `rows:` 配列に各行をオブジェクトで記述します。

```yaml
setup_tables:
  - table: テーブル名
    rows:
      - カラム1: "値1"
        カラム2: "値2"
        カラム3: "値3"
```

**YAML 記述の必須キー**: `setup_tables` / `expected_tables` / `expected_complete_tables` の各エントリには `table` キーが必須です。省略すると `IllegalStateException` がスローされます。

**セットアップデータなし時の動作**: SETUP_TABLE のデータが存在しない場合、INSERT はスキップされエラーにはなりません。

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#table-data)

### 5.2 SETUP_TABLE

DB への INSERT 用データを記述します。

- 各エントリのカラム名と値を記述します
- **主キーカラムは省略不可**です。省略するとデフォルト値（`"0"` やスペース等）が INSERT されます

**null 値・空文字の動作**:

| 値の指定 | Excel | YAML |
|---|---|---|
| null（Java null） | セルに `null`（大文字小文字不問）と記述 | アンクォートの `null` を記述（`"null"` でも同じ結果） |
| 空文字 | セルを空にする | `""` と記述 |
| 日付型カラムの空文字 | セルを空にする → `null` 扱い | `""` → `null` 扱い |

### 5.3 EXPECTED_TABLE

テスト後の DB 状態と比較するデータを記述します。

- **省略したカラムは比較対象外**になります。検証したいカラムだけを列挙できます

### 5.4 EXPECTED_COMPLETE_TABLE

省略カラムにデフォルト値を補完してから比較するデータを記述します。

- 省略カラムにはデフォルト値が自動補完されます
- デフォルト値は以下のとおりです

| カラム型 | デフォルト値 |
|---|---|
| 数値型 | `"0"` |
| 固定長文字列型（CHAR, NCHAR） | 半角スペース × カラム長 |
| 可変長文字列型（VARCHAR 等） | `" "`（半角スペース1文字） |
| 日付型 | epoch 起点（JVM タイムゾーン依存。JST 環境では `"1970-01-01 09:00:00.0"`） |
| バイナリ型 | 10バイトのゼロバイト列の HexString |
| Boolean 型 | `"false"` |

**注意**: DATE カラムのデフォルト値は JVM のタイムゾーン設定に依存します。JST 環境と UTC 環境では値が異なります。

**Excel 混在禁止**: Excel では `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一シート内で混在させると、後半のデータが読み込まれません。同じ種別のセクションをまとめて記述してください。YAML では `expected_tables` と `expected_complete_tables` は別キーのため混在可能です。

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#expected-complete-table)

### 5.5 LIST_MAP

キーバリュー形式の汎用データです。テストケース定義（`testShots`）・リクエストパラメータ・期待値オブジェクト・期待ログなど、様々な用途で使用されます。

#### Excel での記述

```
| LIST_MAP=testShots | | |
| no | description | status |
| 1  | 正常系       | active |
| 2  | 異常系       | error  |
```

#### YAML での記述

```yaml
list_maps:
  - id: testShots
    rows:
      - no: "1"
        description: "正常系"
        status: "active"
      - no: "2"
        description: "異常系"
        status: "error"
```

- ID は完全一致で検索されます
- 同一ファイル内で同一 ID の重複エントリは先着一致で、2件目以降は無視されます
- 指定した ID のエントリが存在しない場合は空のデータとして扱われます（エラーにはなりません）

主な予約 ID は [4章](#4-テストケース定義) を参照してください。

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#list-map)

---

## 6. ファイルデータ

### 6.1 固定長・可変長の統合

セットアップ用のファイルデータ（`SETUP_FIXED` / `SETUP_VARIABLE`）は、固定長・可変長の区別なくまとめて収集されます。期待値ファイル（`EXPECTED_FIXED` / `EXPECTED_VARIABLE`）も同様です。固定長か可変長かはセクション内の記述で区別されます。

**YAML 記述の必須キー**: `setup_files` / `expected_files` の各エントリには `path` キーが必須です。省略するとエラーになります。

### 6.2 ファイルセクションの構造

ファイルセクションは以下の順序で記述します。

1. **ディレクティブ**（0件以上）: エンコーディング等のファイル属性を指定します
2. **レコード種別とフィールド名称**: 先頭要素 = レコード種別、以降 = フィールド名称
3. **データ型**（各フィールドのデータ型記号）
4. **フィールド長**（固定長のみ）: 各フィールドのバイト長
5. **データ**（1件以上）: 実データ

**Excel 固有の制約**: データの先頭要素は必ず空（null または空文字）にする必要があります。YAML にはこの制約はありません。

**Excel の記述例**（ディレクティブ → レコード種別+フィールド名称 → データ型 → フィールド長 → データ）:

```
| SETUP_FIXED=work/input.txt | | | |
| text-encoding | MS932 | | |
| DATA | USER_ID | AMOUNT | |
|      | X       | Z      | |
|      | 10      | 10     | |
|      | 001     | 5000   | |
```

**YAML の記述例**:

```yaml
setup_files:
  - path: work/input.txt
    type: fixed
    directives:
      text-encoding: MS932
    records:
      - record_type: DATA
        fields:
          - {name: USER_ID, type: X, length: 10}
          - {name: AMOUNT,  type: Z, length: 10}
        rows:
          - ["001", "5000"]
```

- YAML の `rows:` 内の各値はダブルクォートで囲んでください（テーブルデータと同じルール。値の書き方の詳細は [8章](#8-値の書き方) を参照）

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#file-data)

### 6.3 固定長ファイル固有の仕様

- フィールド名称・データ型・フィールド長の3リストが同サイズで必須です
- ファイル内の全フラグメントは同一レコード長でなければなりません。違反時は `IllegalStateException` がスローされます
- フィールド値がフィールド長を超えた場合は `IllegalStateException` がスローされます

### 6.4 可変長ファイル固有の仕様

- フィールド名称・データ型の2リストが同サイズで必須です。フィールド長は不要です
- **空エントリの動作**: 可変長ファイルの空エントリはスキップされず、全フィールドが `""` のレコードとして保持されます。固定長ファイルの空エントリはスペースパディングされた定長レコードとして書き出されます

### 6.5 複数レコードレイアウト

1ファイルセクション内に複数のレコードレイアウトを連続して記述できます。データの後ろに新たなレコード種別とフィールド名称を書くと、新しいレコードレイアウトとして扱われます。

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#multi-record)

### 6.6 空ファイル

0バイトの空ファイルを表現するには、ディレクティブのみを記述してレコード定義を省略します。

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#empty-file)

### 6.7 `"-"` 長フィールド

フィールド長に `"-"` を指定すると、追加された全レコードの最大バイト長に自動拡張されます。値は改行コードと前後空白が除去されます。

### 6.8 異常系

| 条件 | 例外 |
|---|---|
| 同一レコード種別内でフィールド名称が重複 | `IllegalArgumentException` |
| フィールド名称リストまたはデータ型リストが null/空 | `IllegalArgumentException` |
| フィールド名称・データ型・フィールド長リストのサイズ不一致 | `IllegalArgumentException` |
| 存在しないフィールド名称を指定 | `IllegalArgumentException` |
| データ要素数が不正 | `IllegalStateException` |
| ディレクティブまたはレコード種別/フィールド名称定義の要素数が2未満 | `IllegalStateException` |
| ファイル読み込み失敗（IO 例外） | `RuntimeException` |
| 日付型カラムの値が日付として解析できない | `RuntimeException` |

---

## 7. メッセージングテストデータ

### 7.1 sendSyncTestData の配置規則

テストデータファイルは `sendSyncTestData/{requestId}/message` というパスに配置します（末尾の `message` は固定のパスセグメントです）。

- **Excel**: `MESSAGE=sendSyncTestData/{requestId}/message` をセクション識別子として記述します
- **YAML**: `messages:` の `id:` に `sendSyncTestData/{requestId}/message` を指定します

```
sendSyncTestData/{requestId}/message
```

### 7.2 FW 制御ヘッダフィールド

デフォルトの FW 制御ヘッダフィールドは以下の4種類です。`reader.fwHeaderfields` キーで変更できます。

- `requestId`
- `userId`
- `resendFlag`
- `resultCode`

**Excel での記述**: フィールド名称行より前に `| フィールド名 | 値 |` の形式で記述します（ディレクティブ行と同じ位置）。
**YAML での記述**: `record_type: FW_HEADER` のレコードとして記述します。

### 7.3 HEADER / BODY MESSAGES の構造と件数制約

- `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` のエントリ数（rows 合計）は一致が必須です。不一致の場合は `IllegalStateException` がスローされます
- HTTP 同期応答メッセージ（`response_body_messages`）の各データエントリは文字列長が同一である必要があります

### 7.4 no カラムと errorMode

- **Excel**: `no` カラム（先頭カラム）はフレームワークが除去し、データとして保存されません。フィールド名称行の先頭セルは空にします
- **YAML**: `no` フィールドは `rows:` のリスト要素に含めます。フレームワークが除去します
- `errorMode` の値はカラム番号1に格納されます
- `errorMode:timeout` および `errorMode:msgException` は特殊値です。これらが指定されたエントリでは他フィールドはパースされません

### 7.5 複数回送信

N 回送信する場合は、ヘッダ件数とボディ件数をともに N 件ずつ記述します。同一リクエスト ID で複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させます。

### 7.6 GroupMessageParser

同一 groupId の複数メッセージプールを収集します。識別子の値をリクエスト ID として使用します。

### 7.7 ステータスコード

ステータスコードカラムがない場合はデフォルト値 `"200"` が使用されます。これは Excel・YAML 両方で共通の動作です。

### 7.8 フォーマット定義ファイルの命名規則

- 応答電文: `{requestId}_RECEIVE`
- 要求電文: `{requestId}_SEND`

### 7.9 アサート方式の切り替え

SystemRepository の `messaging.assertAsMapFileType` キーの設定値に応じてアサート方式が切り替わります。未設定時のデフォルトは `"Fixed"` 形式（項目単位アサート）です。

### 7.10 record_type の扱い

`MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は、内部で常に `"default"` に置き換えられます。

- **Excel**: フィールド名称行の先頭セルに任意の値を記述できます（装飾的なメタデータとして扱われます）
- **YAML**: `record_type:` に任意の値を記述できます。ただし `FW_HEADER` は FW 制御ヘッダ抽出に使用されるため、それ以外の用途には使用しないでください

→ [Excel / YAML Example](ntf-testdata-doc-examples-messaging.md#messaging)

---

## 8. 値の書き方

### 8.1 値の種類と Excel / YAML 対比

テストデータに指定できる値の種類と、各形式での記述方法は以下のとおりです。

| 値の種類 | Excel での記述 | YAML での記述 | 備考 |
|---|---|---|---|
| 通常の文字列 | `abc` | `"abc"` | YAML はクォート必須（型変換防止） |
| null（DB に null を格納） | `null`（大文字小文字不問） | `null`（クォートなし） | YAML の `"null"`（クォートあり）も同じ結果 |
| 空文字 | 空セル | `""` | |
| 先頭ゼロ付き数値 | `001` | `"001"` | YAML でクォートなしだと `1` に型変換される |
| `true` / `false`（文字列） | `true` | `"true"` | YAML でクォートなしだと真偽値に型変換される |
| 半角スペース1文字 | `" "`（セルに `"` space `"` と入力） | `" "` | 外側クォートが除去されてスペースになる |
| ダブルクォート1文字 | `"""`（セルに `"` `"` `"` と入力） | `'"'`（YAML シングルクォート） | |
| 日時プレースホルダ | `${systemTime}` | `"${systemTime}"` | 完全一致のみ変換。詳細は 8.4 を参照 |
| バイナリファイル参照 | `${binaryFile:path}` | `"${binaryFile:path}"` | パスはどちらもデータファイルのディレクトリ基準。詳細は 8.6 を参照 |
| 文字種生成 | `${半角英字,10}` | `"${半角英字,10}"` | 詳細は 8.5 を参照 |
| 改行文字（LF） | `\\n` | `"\\n"` | LineSeparatorInterpreter が変換 |
| 改行文字（CR） | `\\r` | `"\\r"` | LineSeparatorInterpreter が変換 |

**YAML のクォートルール**:
- `rows:` 内のすべてのデータ値は**必ずダブルクォートで囲んでください**。クォートなしだと SnakeYAML が数値・真偽値に型変換します
- `null` のみクォートなしで記述します（ただし `"null"` でも同じく Java null になります）
- `type:`, `record_type:`, `path:` 等のスキーマ構造値はクォート不要です

**Excel のセル書式**:
- セルは必ず**文字列書式**で記述してください。数値・日付書式の場合の動作は保証されません

### 8.2 インタープリタチェーンの仕組み

テストデータの値はパース時にインタープリタチェーンを通過し、変換されます。DI 設定で注入されたインタープリタが順番に適用されます。

### 8.3 インタープリタ一覧

| インタープリタ | 変換内容 |
|---|---|
| `NullInterpreter` | `null` / `NULL` / `Null`（大文字小文字不問）→ Java null |
| `QuotationTrimmer` | 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去 |
| `DateTimeInterpreter` | `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換 |
| `LineSeparatorInterpreter` | `\\r` → CR（0x0D）、`\\n` → LF（0x0A）に変換 |
| `BinaryFileInterpreter` | `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換。パスはデータファイル（Excel / YAML）のディレクトリからの相対パス |
| `BasicJapaneseCharacterInterpreter` | `${文字種,文字数}` 形式で文字列生成 |
| `CompositeInterpreter` | 文字列中の `${...}` 要素を個別解釈して置換 |

### 8.4 DateTimeInterpreter の完全一致制約

`DateTimeInterpreter` は完全一致のみ変換します。部分文字列は変換されません。文字列中の `${...}` を置換するには `CompositeInterpreter` との組み合わせが必要です。

### 8.5 文字種生成の有効文字種

14種類の文字種が使用できます: 半角英字 / 半角数字 / 半角記号 / 半角カナ / 全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 / 中国語 / サロゲートペア / 改行 / 外字

上記以外の文字種を指定するとエラーになります。

### 8.6 BinaryFileInterpreter のパス基準

`${binaryFile:パス}` のパスは、**テストデータファイルのディレクトリ**からの相対パスです。これは Excel・YAML 両方で同じ動作です。

| 形式 | 基準ディレクトリ |
|---|---|
| Excel | Excel ファイル（`.xls` / `.xlsx`）が置かれているディレクトリ |
| YAML | YAML ファイル（`.yaml`）が置かれているディレクトリ |

### 8.7 日付型カラムの記述形式と境界値

有効な記述形式は以下のとおりです。

- `yyyyMMddHHmmssSSS`（17文字）
- 後置0埋め短縮形
- JDBC タイムスタンプエスケープ形式（5文字目が `-`）

`java.sql.Timestamp` 型カラムの期待値は末尾 `.0` が必須です（例: `"2010-01-01 12:34:56.0"`）。末尾 `.0` がないとアサートが失敗します。

→ [Excel / YAML Example](ntf-testdata-doc-examples-special.md#datetime)

### 8.8 バイナリデータの記述

`0x` プレフィクス付き16進数で記述できます。`0x` がない場合は文字列としてエンコードされます。

### 8.9 X9/SX9 型フィールドの記述

パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記述します。

### 8.10 データ型マッピング

デフォルトで22種のデータ型記号が使用できます。使用できない型記号を指定するとエラーになります。

`TEST_{基底型名}` という名前のデータ型を定義すると、同名の基底型より優先して使用されます（テスト専用の型定義に使います）。

---

## 9. ディレクティブ

### 9.1 ディレクティブの構成

ディレクティブは「キー名・値」の2要素で記述します（最低2要素必要）。

- **Excel**: ファイルセクションの先頭（レコード定義より前）に `| キー名 | 値 |` の形で記述します
- **YAML**: `directives:` オブジェクトに `key: value` 形式で記述します

### 9.2 固定長ファイルのディレクティブ

固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Fixed"`）。通常は記述不要です |
| `record-length` | フィールド長合計から自動計算。通常は記述不要です |
| `text-encoding` | ファイルの文字エンコーディング |
| `positive-zone-sign-nibble` | ゾーン10進数の正符号ニブル |
| その他 | `FixedLengthDirective` 列挙型の定義を参照してください |

### 9.3 可変長ファイルのディレクティブ

可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Variable"`）。通常は記述不要です |
| `field-separator` | フィールド区切り文字。デフォルトは `","` です。`"\\t"` 指定でタブ文字になります。**1文字のみ有効**（2文字以上は `IllegalArgumentException` がスローされます） |
| `record-separator` | レコード区切り。`NONE` / `CR` / `LF` / `CRLF` または任意リテラル文字列が有効です |
| `quoting-delimiter` | クォート文字 |
| その他 | `VariableLengthDirective` 列挙型の定義を参照してください |

### 9.4 デフォルトディレクティブの DI 設定

SystemRepository への DI 設定で、全ファイル共通または種別専用のデフォルトディレクティブを一括設定できます。

| DI キー | 適用対象 |
|---|---|
| `defaultDirectives` | 全ファイル共通のデフォルト |
| `fixedLengthDirectives` | 固定長ファイル専用。`defaultDirectives` より後に上書き適用されます |
| `variableLengthDirectives` | 可変長ファイル専用 |

→ [Excel / YAML Example](ntf-testdata-doc-examples-special.md#directive)

---

## 10. ヘッダ・コメント・空エントリ

### 10.1 ヘッダの構造

ヘッダにはカラム名を列挙します。

- ヘッダ末尾の空カラムは除去されます（末尾カラムの省略が可能です）
- データエントリがヘッダより少ない場合、不足分は空文字 `""` で補完されます

### 10.2 マーカーカラム

カラム名が `[カラム名]` 形式（角括弧で囲まれた名前）のカラムはマーカーカラムとして扱われ、DB 操作から除外されます。

- **Excel**: `SETUP_TABLE` / `EXPECTED_TABLE` / `LIST_MAP` すべてでマーカーカラムが除外されます
- **YAML**: `list_maps:` ではマーカーカラムが除外されます。`setup_tables` / `expected_tables` ではマーカーカラムの除外は行われません

### 10.3 エントリ単位のコメント

エントリをコメントとしてマークすると、そのエントリ全体がスキップされます。

- **Excel**: 先頭要素が `//` で始まる行はスキップされます
- **YAML**: `#` がコメント記号です（行頭・行末どちらにも使えます）

### 10.4 要素途中からのコメント（Excel 固有）

Excel では、エントリ内の先頭以外の要素をコメントとしてマークすると、その要素以降が切り捨てられます。YAML では標準のコメント構文（`#`）を使って同等の記述ができます。

- **Excel**: 先頭以外の要素が `//` で始まる場合、その要素以降が切り捨てられます
- **YAML**: `#` を行末に書いて同等の記述ができます（例: `NUMBER_COL: "100"  # 数値カラム`）

### 10.5 空エントリのスキップ

全要素が null または空文字のエントリは読み飛ばされます。

- **Excel**: 行の全セルが空の場合にスキップされます
- **YAML**: `rows:` 内の要素が空マッピング（`{}`）またはすべての値が空文字の場合にスキップされます

---

## 11. DB アサート

### 11.1 テーブルアサート（assertTableEquals）

テーブルアサートは **主キーで突合**してレコードを比較します。レコードの**順序は問いません**。異なる順序でデータが返ってきてもアサートが成功します。

### 11.2 SQL 結果セットアサート（assertSqlResultSetEquals）

SQL 結果セットアサートは**順序厳格**な比較を行います。期待値と実際の結果セットでレコードの順序が異なる場合はアサートが失敗します。

### 11.3 DB アサートのオプション

- テーブルアサートで `failIfNoDataFound=false` を指定すると、DB にデータが存在しない場合に検証をスキップします
- パラメータ Map の取得でリストが0件の場合は空データとして扱われます。2件以上ある場合はエラーになります

---
