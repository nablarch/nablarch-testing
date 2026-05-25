# NTF テストデータ仕様書

- **対象**: Nablarch Testing Framework（NTF）が読み込むテストデータの構造・ルール・制約
- **形式非依存**: 本書は論理仕様を記述します。Excel・YAML のどちらで記述する場合も同じルールが適用されます
- **記述例**: 各節末尾のリンクから Excel 表と YAML コードブロックの対比例を参照できます

---

## 目次

1. [概要](#1-概要)
   - [1.1 NTF テストデータとは](#11-ntf-テストデータとは)
   - [1.2 テストデータの基本構造](#12-テストデータの基本構造)
2. [セクション識別](#2-セクション識別)
3. [テストケース定義](#3-テストケース定義)
4. [テーブルデータ](#4-テーブルデータ)
5. [ファイルデータ](#5-ファイルデータ)
6. [メッセージングテストデータ](#6-メッセージングテストデータ)
7. [特殊値・インタープリタ](#7-特殊値インタープリタ)
8. [ディレクティブ](#8-ディレクティブ)
9. [ヘッダ・コメント・空エントリ](#9-ヘッダコメント空エントリ)

---

## 1. 概要

### 1.1 NTF テストデータとは

NTF テストデータファイルには、次の3種類のデータを記述します。

**テストケース**  
テストの実行条件を1エントリ1ケースで定義します。各エントリが1テストケースを表します。リクエスト単体テスト（ウェブアプリケーション）なら「ユーザ ID・期待ステータスコード・期待フォワード先 URI」など、リクエスト単体テスト（バッチ処理）なら「リクエストパス・ユーザ ID・DI コンフィグ・期待ステータスコード」などを列挙します。

**セットアップ**  
テスト実行前に投入するデータです。DB テーブルへの INSERT データ、固定長・可変長ファイルの入力データなどを定義します。

**検証**  
テスト後の検証に使うデータです。DB の期待値、出力ファイルの期待値、電文の期待値、ログや検索結果等の期待値などを定義します。

これらは**セクション**という単位で管理され、DataType 名と識別子の値の組み合わせで区別されます。1つのファイルに複数種別のセクションを共存させることができます。セクションの記述順序は問いません。

→ [Excel / YAML Example](ntf-spec-examples-overview.md#overview)

---

### 1.2 テストデータの基本構造

テストデータはテストクラスと1対1で対応します。

**Excel** では、テストクラスと同名の1つのブック（`.xls` ファイル）にすべてのテストデータを格納します。シートを分割単位とし、1シートが1つの読み込み単位になります。

**YAML** では、テストクラスと同名のディレクトリを作成し、その下にファイルを配置します。1ファイルが1つの読み込み単位になり、Excelの1シートに相当します。

```
【Excel】
src/test/java/com/example/
  FooTest.xls          ← テストクラス FooTest に対応する1ブック
    ├── case01         ← シート（読み込み単位）
    └── case02         ← シート（読み込み単位）

【YAML】
src/test/java/com/example/
  FooTest/             ← テストクラス FooTest に対応するディレクトリ
    ├── case01.yaml    ← ファイル（読み込み単位）= Excelのcase01シートに相当
    └── case02.yaml    ← ファイル（読み込み単位）= Excelのcase02シートに相当
```

読み込み単位（Excelの1シート / YAMLの1ファイル）の中に、テストケース・セットアップ・検証の複数セクションを共存させて記述します。

---

## 2. セクション識別

### 2.1 セクション識別の構成要素

各セクションは以下の3要素で識別されます。

- **DataType 名**: 後述する14種類のいずれか（例: `SETUP_TABLE`）
- **groupId**: セクションをグループ化するための識別子。省略可能で、省略時は空文字扱いです
- **識別子の値**: テーブル名・ファイルパス・IDなどセクション種別ごとの識別子

#### Excel での記述

Excel ではセクション先頭セルに `DataType名=識別子の値` 形式で記述します。DataType 名で始まれば合致します（前方一致）。

groupId なし:
```
SETUP_TABLE=USER_MASTER
```

groupId あり（DataType 名の直後に `[groupId]`）:
```
SETUP_TABLE[case01]=USER_MASTER
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

groupId なし:
```yaml
setup_tables:
  - table: USER_MASTER
    rows: ...
```

groupId あり（`group_id:` フィールドで指定）:
```yaml
setup_tables:
  - group_id: case01
    table: USER_MASTER
    rows: ...
```

- 完全なセクションキーを使用するため前方一致は発生しません
- YAMLでは同一ファイル内のトップレベルキーの重複は禁止です（`IllegalStateException` がスローされます）。同種のデータは同一キーにリストとして並べて記述します

→ [Excel / YAML Example](ntf-spec-examples-overview.md#section-identifier)

### 2.2 DataType の種類

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
| `EXPECTED_REQUEST_HEADER_MESSAGES` | 要求電文ヘッダの期待値 | GroupData または SingleData |
| `EXPECTED_REQUEST_BODY_MESSAGES` | 要求電文ボディの期待値 | GroupData または SingleData |
| `RESPONSE_HEADER_MESSAGES` | 応答電文ヘッダデータ | GroupData または SingleData |
| `RESPONSE_BODY_MESSAGES` | 応答電文ボディデータ | GroupData または SingleData |
| `DEFAULT` | フレームワーク内部用（通常使用しません） | — |

### 2.3 GroupData と SingleData

セクションの収集方式は DataType によって異なります。

- **GroupData**: 同一 groupId を持つセクションをすべて収集します。ファイル全体を最後まで読み込みます（`SETUP_TABLE`、`EXPECTED_TABLE`、ファイル系など）
- **SingleData**: 最初に一致したセクション1件だけを取得して停止します（`LIST_MAP`、`MESSAGE` など）

`LIST_MAP` で同一 ID のエントリが複数ある場合、2件目以降は黙って無視されます。

### 2.4 groupId の制約

- 省略時は空文字扱いです
- groupId の指定は1件のみ有効です。2件以上指定すると `IllegalArgumentException` がスローされます
- **Excel**: DataType 名の直後に `[case01]` のように角括弧で囲んで記述します（例: `SETUP_TABLE[case01]=テーブル名`）
- **YAML**: `group_id: case01` フィールドで指定します

バッチ固有の動作として、groupId に `"default"` を指定するとグループ ID なし扱いと同等になります。

### 2.5 RESPONSE_HEADER/BODY_MESSAGES の2経路

`RESPONSE_HEADER_MESSAGES` と `RESPONSE_BODY_MESSAGES` は、以下の2つの経路でアクセスできます。

- **経路A（GroupData）**: groupId を指定して収集する経路
- **経路B（SingleData）**: ID で一致する経路

---

## 3. テストケース定義

### 3.1 testShots

`testShots` はテストケース定義の予約IDです。フレームワークがこの ID を自動的に読み込み、各エントリを1テストケースとして実行します。旧ID `testCases` は後方互換性のためフォールバックとして残存します。

テストが実行されるためには `testShots` に1件以上のエントリが必要です。0件の場合は例外がスローされます。

- **Excel**: `LIST_MAP=testShots` セクションに記述します
- **YAML**: `list_maps:` 下の `id: testShots` エントリに記述します

→ [Excel / YAML Example](ntf-spec-examples-overview.md#test-shots)

### 3.2 リクエスト単体テスト（ウェブアプリケーション）の testShots カラム

リクエスト単体テスト（ウェブアプリケーション）での必須カラムは以下のとおりです。

| カラム名 | 説明 |
|---|---|
| `no` | テストケース番号 |
| `description` | テストケースの説明（旧名 `case` も可） |
| `isValidToken` | トークン制御フラグ |
| `expectedStatusCode` | 期待する HTTP ステータスコード |
| `forwardUri` | 期待するフォワード先 URI |
| `context` | リクエスト ID・ユーザ・HTTP メソッドを記載した `LIST_MAP` 名 |

主なオプションカラムは以下のとおりです。

| カラム名 | 説明 | 空の場合 |
|---|---|---|
| `setUpTable` | ケース固有の DB セットアップグループ ID | スキップ |
| `expectedTable` | テーブル期待値のグループ ID | スキップ |
| `expectedSearch` | 検索結果期待値のグループ ID | スキップ |
| `expectedMessageId` | 期待するメッセージ ID（カンマ区切りで複数指定可） | スキップ |
| `requestParams` | HTTP リクエストパラメータの `LIST_MAP` 名 | — |
| `cookie` | Cookie 値の `LIST_MAP` 名 | Cookie なし |
| `queryParams` | クエリパラメータの `LIST_MAP` 名 | パラメータなし |
| `HTTP_METHOD` | HTTP メソッド | `"POST"` |
| `expectedContentLength` | 期待する Content-Length | スキップ |
| `expectedContentType` | 期待する Content-Type | スキップ |
| `expectedContentFileName` | 期待する Content-Disposition ファイル名 | スキップ |
| `expectedMessage` | 同期応答メッセージ送信の要求電文グループ ID | スキップ |
| `responseMessage` | 同期応答メッセージ送信の応答電文グループ ID | スキップ |
| `expectedMessageByClient` | HTTP 同期応答メッセージ送信の要求電文グループ ID | スキップ |
| `responseMessageByClient` | HTTP 同期応答メッセージ送信の応答電文グループ ID | スキップ |

`context` LIST_MAP は1エントリのみ有効です。`REQUEST_ID` が空の場合は例外がスローされます。

### 3.3 リクエスト単体テスト（バッチ処理）の testShots カラム

リクエスト単体テスト（バッチ処理）での必須カラムは以下のとおりです。

| カラム名 | 説明 |
|---|---|
| `no` | テストケース番号 |
| `description` | テストケースの説明 |
| `expectedStatusCode` | 期待するステータスコード |
| `diConfig` | DI コンポーネント設定ファイルパス |
| `requestPath` | リクエストパス |
| `userId` | 実行ユーザ ID |

主なオプションカラムは以下のとおりです。

| カラム名 | 説明 | 空の場合 |
|---|---|---|
| `setUpTable` | ケース固有の DB セットアップグループ ID | スキップ |
| `expectedTable` | テーブル期待値のグループ ID | スキップ |
| `setUpFile` | 入力ファイル準備グループ ID | スキップ |
| `expectedFile` | 出力ファイル期待値グループ ID | スキップ |
| `expectedLog` | 期待ログの `LIST_MAP` 名 | スキップ |
| `args[0]`, `args[1]`, ... | コマンドライン引数 | — |
| その他任意カラム | コマンドラインオプション | — |

### 3.4 DB 共通セットアップデータ

`setUpDb` はテストメソッド共通の DB 初期化データを定義する予約 ID です。テストメソッド開始時に1度だけ `SETUP_TABLE` データが投入されます。

---

## 4. テーブルデータ

### 4.1 データの形式

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

→ [Excel / YAML Example](ntf-spec-examples-table.md#table-data)

### 4.2 SETUP_TABLE

DB への INSERT 用データです。

- 各エントリのカラム名と値を記述します
- **主キーカラムは省略不可**です。省略するとデフォルト値（`"0"` やスペース等）が INSERT されます

### 4.3 EXPECTED_TABLE

テスト後の DB 状態と比較するデータです。

- **省略したカラムは比較対象外**になります。検証したいカラムだけを列挙できます

### 4.4 EXPECTED_COMPLETE_TABLE

省略カラムにデフォルト値を補完してから比較するデータです。

- 省略カラムに `BasicDefaultValues` のデフォルト値が自動補完されます
- デフォルト値は以下のとおりです

| カラム型 | デフォルト値 |
|---|---|
| 数値型 | `"0"` |
| 固定長文字列型（CHAR, NCHAR） | 半角スペース × カラム長 |
| 可変長文字列型（VARCHAR 等） | `" "`（半角スペース1文字） |
| 日付型 | `"1970-01-01 09:00:00.0"`（JVM タイムゾーン依存） |
| バイナリ型 | 10バイトのゼロバイト列の HexString |
| Boolean 型 | `"false"` |

**注意**: DATE カラムのデフォルト値は JVM のタイムゾーン設定に依存します。JST 環境と UTC 環境では値が異なります。

**Excel 混在禁止**: Excel では `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一シート内で混在させると、後半のデータが読み込まれません。同じ種別のセクションをまとめて記述してください。YAML では `expected_tables` と `expected_complete_tables` は別キーのため混在可能です。

→ [Excel / YAML Example](ntf-spec-examples-table.md#expected-complete-table)

### 4.5 LIST_MAP

キーバリュー形式の汎用データです。テストケース定義（`testShots`）・リクエストパラメータ・期待値オブジェクト・期待ログなど、様々な用途で使用されます。

- ID は完全一致で検索されます
- 同一ファイル内で同一 ID の重複エントリは先着一致で、2件目以降は無視されます

主な予約IDは [3章](#3-テストケース定義) を参照してください。

→ [Excel / YAML Example](ntf-spec-examples-table.md#list-map)

---

## 5. ファイルデータ

### 5.1 固定長・可変長の統合

`SETUP_FIXED` と `SETUP_VARIABLE` は `getSetupFile()` でまとめて返されます。`EXPECTED_FIXED` / `EXPECTED_VARIABLE` も同様です。ファイル種別はセクション内の属性（固定長 or 可変長）で区別します。

### 5.2 ファイルセクションの構造

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

→ [Excel / YAML Example](ntf-spec-examples-file.md#file-data)

### 5.3 固定長ファイル固有の仕様

- フィールド名称・データ型・フィールド長の3リストが同サイズで必須です
- ファイル内の全フラグメントは同一レコード長でなければなりません。違反時は `IllegalStateException` がスローされます
- フィールド値がフィールド長を超えた場合は `IllegalStateException` がスローされます

### 5.4 可変長ファイル固有の仕様

- フィールド名称・データ型の2リストが同サイズで必須です。フィールド長は不要です
- **空エントリの動作**: 可変長ファイルの空エントリはスキップされず、全フィールドが `""` のレコードとして保持されます。固定長ファイルの空エントリはスペースパディングされた定長レコードとして書き出されます

### 5.5 複数レコードレイアウト

1ファイルセクション内に複数のレコードレイアウトを連続して記述できます。データの後ろに新たなレコード種別とフィールド名称を書くと、新しいレコードレイアウトとして扱われます。

→ [Excel / YAML Example](ntf-spec-examples-file.md#multi-record)

### 5.6 空ファイル

0バイトの空ファイルを表現するには、ディレクティブのみを記述してレコード定義を省略します。

→ [Excel / YAML Example](ntf-spec-examples-file.md#empty-file)

### 5.7 `"-"` 長フィールド

フィールド長に `"-"` を指定すると、追加された全レコードの最大バイト長に自動拡張されます。値は改行コードと前後空白が除去されます。

### 5.8 異常系

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

## 6. メッセージングテストデータ

### 6.1 sendSyncTestData の配置規則

テストデータファイルは `sendSyncTestData` ベースパス下にリクエスト ID と同名のファイルとして配置します。

```
sendSyncTestData/{requestId}/message
```

### 6.2 FW 制御ヘッダフィールド

デフォルトの FW 制御ヘッダフィールドは以下の4種類です。`reader.fwHeaderfields` キーで変更できます。

- `requestId`
- `userId`
- `resendFlag`
- `resultCode`

### 6.3 HEADER / BODY MESSAGES の構造と件数制約

- `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` のエントリ数（rows 合計）は一致が必須です。不一致の場合は `IllegalStateException` がスローされます
- HTTP 同期応答メッセージ（`response_body_messages`）の各データエントリは文字列長が同一である必要があります

### 6.4 no カラムと errorMode

- `no` カラム（先頭カラム）はフレームワークが除去し、データとして保存されません
- `errorMode` の値はカラム番号1に格納されます
- `errorMode:timeout` および `errorMode:msgException` は特殊値です。これらが指定されたエントリでは他フィールドはパースされません

### 6.5 複数回送信

N 回送信する場合は、ヘッダ件数とボディ件数をともに N 件ずつ記述します。同一リクエスト ID で複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させます。

### 6.6 GroupMessageParser

同一 groupId の複数メッセージプールを収集します。識別子の値をリクエスト ID として使用します。

### 6.7 ステータスコード

ステータスコードカラムがない場合はデフォルト値 `"200"` が使用されます。

### 6.8 フォーマット定義ファイルの命名規則

- 応答電文: `{requestId}_RECEIVE`
- 要求電文: `{requestId}_SEND`

### 6.9 アサート方式の切り替え

SystemRepository の `messaging.assertAsMapFileType` キーの設定値に応じてアサート方式が切り替わります。未設定時のデフォルトは `"Fixed"` 形式（項目単位アサート）です。

### 6.10 record_type の扱い

`MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は、内部で常に `"default"` に置き換えられます。任意の値を記述できます（装飾的なメタデータとして扱われます）。

→ [Excel / YAML Example](ntf-spec-examples-messaging.md#messaging)

---

## 7. 特殊値・インタープリタ

### 7.1 インタープリタチェーンの仕組み

テストデータの値はパース時にインタープリタチェーンを通過し、変換されます。DI 設定で注入されたインタープリタが順番に適用されます。

### 7.2 インタープリタ一覧

| インタープリタ | 変換内容 |
|---|---|
| `NullInterpreter` | `null` / `NULL` / `Null`（大文字小文字不問）→ Java null |
| `QuotationTrimmer` | 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去 |
| `DateTimeInterpreter` | `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換 |
| `LineSeparatorInterpreter` | `\\r` → CR（0x0D）、`\\n` → LF（0x0A）に変換 |
| `BinaryFileInterpreter` | `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換 |
| `BasicJapaneseCharacterInterpreter` | `${文字種,文字数}` 形式で文字列生成 |
| `CompositeInterpreter` | 文字列中の `${...}` 要素を個別解釈して置換 |

### 7.3 DateTimeInterpreter の完全一致制約

`DateTimeInterpreter` は完全一致のみ変換します。部分文字列は変換されません。文字列中の `${...}` を置換するには `CompositeInterpreter` との組み合わせが必要です。

### 7.4 BasicJapaneseCharacterGenerator の有効文字種

14種類の文字種が使用できます: 半角英字 / 半角数字 / 半角記号 / 半角カナ / 全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 / 中国語 / サロゲートペア / 改行 / 外字

未知の文字種を指定すると `IllegalArgumentException` がスローされます。

### 7.5 QuotationTrimmer によるスペース値明示記法

空白値を可視化して記述するための記法です。

| 記述 | 結果 |
|---|---|
| `" "` | 半角スペース1文字 |
| `"""` | ダブルクォート1文字 |

### 7.6 日付型カラムの記述形式と境界値

有効な記述形式は以下のとおりです。

- `yyyyMMddHHmmssSSS`（17文字）
- 後置0埋め短縮形
- JDBC タイムスタンプエスケープ形式（5文字目が `-`）

`java.sql.Timestamp` 型カラムの期待値は末尾 `.0` が必須です（例: `"2010-01-01 12:34:56.0"`）。末尾 `.0` がないとアサートが失敗します。

→ [Excel / YAML Example](ntf-spec-examples-special.md#datetime)

### 7.7 バイナリデータの記述

`0x` プレフィクス付き16進数で記述できます。`0x` がない場合は文字列としてエンコードされます。

### 7.8 X9/SX9 型フィールドの記述

パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記述します。

### 7.9 データ型マッピング

`BasicDataTypeMapping` のデフォルトマッピング22種が使用できます。未知の型記号を指定すると `IllegalArgumentException` がスローされます。

`TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用されます。

---

## 8. ディレクティブ

### 8.1 ディレクティブの構成

ディレクティブは「キー名・値」の2要素で記述します（最低2要素必要）。

- **Excel**: ファイルセクションの先頭（レコード定義より前）に `| キー名 | 値 |` の形で記述します
- **YAML**: `directives:` オブジェクトに `key: value` 形式で記述します

### 8.2 固定長ファイルのディレクティブ

固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Fixed"`）。通常は記述不要です |
| `record-length` | フィールド長合計から自動計算。通常は記述不要です |
| `text-encoding` | ファイルの文字エンコーディング |
| `positive-zone-sign-nibble` | ゾーン10進数の正符号ニブル |
| その他 | `FixedLengthDirective` 列挙型の定義を参照してください |

### 8.3 可変長ファイルのディレクティブ

可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Variable"`）。通常は記述不要です |
| `field-separator` | フィールド区切り文字。デフォルトは `","` です。`"\\t"` 指定でタブ文字になります。**1文字のみ有効**（2文字以上は `IllegalArgumentException`） |
| `record-separator` | レコード区切り。`NONE` / `CR` / `LF` / `CRLF` または任意リテラル文字列が有効です |
| `quoting-delimiter` | クォート文字 |
| その他 | `VariableLengthDirective` 列挙型の定義を参照してください |

### 8.4 デフォルトディレクティブの DI 設定

SystemRepository への DI 設定で、全ファイル共通または種別専用のデフォルトディレクティブを一括設定できます。

| DI キー | 適用対象 |
|---|---|
| `defaultDirectives` | 全ファイル共通のデフォルト |
| `fixedLengthDirectives` | 固定長ファイル専用。`defaultDirectives` より後に上書き適用されます |
| `variableLengthDirectives` | 可変長ファイル専用 |

→ [Excel / YAML Example](ntf-spec-examples-special.md#directive)

---

## 9. ヘッダ・コメント・空エントリ

### 9.1 ヘッダの構造

ヘッダにはカラム名を列挙します。

- ヘッダ末尾の空カラムは除去されます（末尾カラムの省略が可能です）
- データエントリがヘッダより少ない場合、不足分は空文字 `""` で補完されます

### 9.2 マーカーカラム

カラム名が `[カラム名]` 形式（角括弧で囲まれた名前）のカラムはマーカーカラムとして扱われ、DB 操作から除外されます。

### 9.3 エントリ単位のコメント

エントリをコメントとしてマークすると、そのエントリ全体がスキップされます。

- **Excel**: 先頭要素が `//` で始まる行はスキップされます
- **YAML**: `#` がコメント記号です（行頭・行末どちらにも使えます）

### 9.4 要素途中からのコメント（Excel 固有）

Excel では、エントリ内の先頭以外の要素をコメントとしてマークすると、その要素以降が切り捨てられます。YAML では標準のコメント構文（`#`）を使って同等の記述ができます。

- **Excel**: 先頭以外の要素が `//` で始まる場合、その要素以降が切り捨てられます
- **YAML**: `#` を行末に書いて同等の記述ができます（例: `NUMBER_COL: "100"  # 数値カラム`）

### 9.5 空エントリのスキップ

全要素が null または空文字のエントリは読み飛ばされます。

---
