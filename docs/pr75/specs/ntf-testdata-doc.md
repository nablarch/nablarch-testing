# NTF テストデータ解説書

- **対象**: Nablarch Testing Framework（NTF）が読み込むテストデータの書き方・構造・ルール
- **形式非依存**: Excel・YAML のどちらで記述する場合にも共通して適用されるルールを説明します
- **記述例**: 各節末尾のリンクから Excel 表と YAML コードブロックの対比例を参照できます

---

## 目次

1. [NTF テストデータとは](#1-ntf-テストデータとは)
2. [テストデータの基本構造](#2-テストデータの基本構造)
3. [データブロック識別](#3-データブロック識別)
4. [テストケース定義](#4-テストケース定義)
5. [テーブルデータ](#5-テーブルデータ)
6. [ファイルデータ](#6-ファイルデータ)
7. [メッセージングテストデータ](#7-メッセージングテストデータ)
8. [値の書き方](#8-値の書き方)
9. [ディレクティブ](#9-ディレクティブ)
10. [ヘッダ・コメント・空エントリ](#10-ヘッダコメント空エントリ)

---


## 1. NTF テストデータとは

NTF（Nablarch Testing Framework）では、テストを実行するために必要なデータを専用のファイルに記述します。テストコード（Java）からこのファイルを読み込むことで、DB へのデータ投入・入力ファイルの配置・期待値との比較が行われます。

テストデータファイルには、次の3種類のデータを記述します。

**テストケース**  
テストの実行条件を1エントリ1ケースで定義します。各エントリが1テストケースを表します。リクエスト単体テスト（ウェブアプリケーション）なら「ユーザ ID・期待ステータスコード・期待フォワード先 URI」など、リクエスト単体テスト（バッチ処理）なら「リクエストパス・ユーザ ID・DI コンフィグ・期待ステータスコード」などを列挙します。

**セットアップ**  
テスト実行前に投入するデータです。DB テーブルへの INSERT データ、固定長・可変長ファイルの入力データなどを定義します。

**検証**  
テスト後の検証に使うデータです。DB の期待値、出力ファイルの期待値、電文の期待値、ログや検索結果等の期待値などを定義します。

これらは**データブロック**という単位で管理されます。データブロックの種別（例: DB 投入用・ファイル期待値用）と識別子（テーブル名・ファイルパス等）の組み合わせで区別します。1つのファイルに複数種別のデータブロックを共存させることができます。データブロックの詳細は [3章](#3-データブロック識別) で説明します。

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

読み込み単位（Excel の1シート / YAML の1ファイル）の中に、テストケース・セットアップ・検証の複数データブロックを共存させて記述します。

YAML ファイルは **YAML 1.2** に準拠して記述します。YAML 1.1 との主な違いとして、`yes` / `no` / `on` / `off` は真偽値ではなく文字列として扱われます。

**ファイルの読み込みルール**

| 項目 | Excel | YAML |
|---|---|---|
| ファイルなし時の動作 | ファイルが存在しない場合はエラーになる | ファイルが存在しない、またはパースに失敗した場合はエラーになる |
| 空ファイル時の動作 | 空シートは存在しないシート扱いとなる | 空ファイル（0バイト）は空データとして扱われる（エラーにはならない） |
| 値の書き方 | セルは必ず**文字列書式**で記述すること。数値・日付書式の場合の動作は保証しない | 値は必ず**ダブルクォートで囲んで**ください |

---

## 3. データブロック識別

### 3.1 データブロック識別の構成要素

各データブロックは **データブロック種別** と **識別子の値** の2要素で識別されます。

- **データブロック種別**: 後述する14種類のいずれか（`SETUP_TABLE` / `EXPECTED_TABLE` など）
- **識別子の値**: テーブル名・ファイルパス・ID などデータブロック種別ごとの識別子


#### Excel での記述

Excel ではデータブロック先頭セルに `データブロック種別=識別子の値` 形式で記述します。データブロック種別名で始まれば合致します（前方一致）。

```
SETUP_TABLE=USER_MASTER
```

#### YAML での記述

YAML ではデータブロック種別ごとに専用のトップレベルキーを使用します。

| データブロック種別 | YAML キー |
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

- 完全なデータブロックキーを使用するため前方一致は発生しません
- YAML では同一ファイル内のトップレベルキーの重複は禁止です。同種のデータは同一キーにリストとして並べて記述します（重複した場合はエラーになります）
- Excel では同一シート内に同種データブロックを複数記述できます。DataType によって全件収集または先着一致のどちらかで収集されます（詳細は [3.3節](#33-同一ファイルシート内に複数のデータブロックを書く場合の注意) を参照）

### 3.2 データブロック種別の一覧

テストデータで使用できるデータブロック種別は以下の14種類です。

| データブロック種別 | 用途 | 同一 ID が複数ある場合 |
|---|---|---|
| `SETUP_TABLE` | INSERT 用テーブルデータ | 同じグループに属するものをすべて収集 |
| `EXPECTED_TABLE` | 比較用テーブルデータ（省略カラムは比較対象外） | 同じグループに属するものをすべて収集 |
| `EXPECTED_COMPLETE_TABLE` | 比較用テーブルデータ（省略カラムにデフォルト値補完） | 同じグループに属するものをすべて収集 |
| `LIST_MAP` | キーバリュー形式の汎用データ（テストケース定義・期待値等） | 最初の1件のみ有効（2件目以降は無視） |
| `SETUP_FIXED` | 固定長ファイルの入力データ | 同じグループに属するものをすべて収集 |
| `EXPECTED_FIXED` | 固定長ファイルの期待値データ | 同じグループに属するものをすべて収集 |
| `SETUP_VARIABLE` | 可変長ファイルの入力データ | 同じグループに属するものをすべて収集 |
| `EXPECTED_VARIABLE` | 可変長ファイルの期待値データ | 同じグループに属するものをすべて収集 |
| `MESSAGE` | メッセージング電文データ | 最初の1件のみ有効（2件目以降は無視） |
| `EXPECTED_REQUEST_HEADER_MESSAGES` | 要求電文ヘッダの期待値 | groupId 指定時は全件収集、ID 直接指定時は最初の1件 |
| `EXPECTED_REQUEST_BODY_MESSAGES` | 要求電文ボディの期待値 | groupId 指定時は全件収集、ID 直接指定時は最初の1件 |
| `RESPONSE_HEADER_MESSAGES` | 応答電文ヘッダデータ | groupId 指定時は全件収集、ID 直接指定時は最初の1件 |
| `RESPONSE_BODY_MESSAGES` | 応答電文ボディデータ | groupId 指定時は全件収集、ID 直接指定時は最初の1件 |
| `DEFAULT` | フレームワーク内部用（通常使用しません） | — |

### 3.3 同一ファイル（シート）内に複数のデータブロックを書く場合の注意

**複数テーブルを INSERT したい場合**: `SETUP_TABLE` などの全件収集タイプのデータブロックは、同一 ID（groupId）のものをすべて収集します。複数のテーブルデータを並べて記述できます。

**同一種別のデータブロックは連続して記述してください**: データブロックを読み込む際、別の種別のデータブロック（別の DataType）が現れると、そこで読み込みを終了します。同じ種別のデータブロックを別の種別で挟んで書くと、後半が読み込まれません。

**`LIST_MAP` や `MESSAGE` の重複 ID**: 同一 ID のエントリが複数ある場合、最初の1件のみ有効です。2件目以降は無視されます。

グループの指定方法（groupId）については [4.3 データブロックのグループ化](#43-データブロックのグループ化groupid) を参照してください。

---

## 4. テストケース定義

### 4.1 testShots

`testShots` はテストケース定義の予約 ID です。フレームワークがこの ID を自動的に読み込み、各エントリを1テストケースとして実行します。旧称 `testCases` も動作しますが、新規作成では `testShots` を使用してください。

テストが実行されるためには `testShots` に1件以上のエントリが必要です。0件の場合はエラーになります。

- **Excel**: `LIST_MAP=testShots` データブロックに記述します
- **YAML**: `list_maps:` 下の `id: testShots` エントリに記述します

### 4.2 testShots のカラム仕様

testShots の各カラムは処理方式によって異なります。各処理方式の詳細は以下を参照してください。

- [ウェブアプリケーション（HttpRequestTestSupport）](ntf-testdata-doc-examples-testshots.md#web)
- [バッチ処理（BatchRequestTestSupport）](ntf-testdata-doc-examples-testshots.md#batch)
- [メッセージング（MessagingRequestTestSupport）](ntf-testdata-doc-examples-testshots.md#messaging)
- [エンティティバリデーション（EntityTestSupport）](ntf-testdata-doc-examples-testshots.md#entity)

### 4.3 データブロックのグループ化（groupId）

複数のテストケースで異なるセットアップデータや期待値を使い分けたい場合、データブロックに **groupId** を付加してグループ化します。`testShots` の各カラム（`setUpTable` / `expectedTable` / `setUpFile` / `expectedFile` 等）に groupId の値を指定すると、そのテストケースでは対応する groupId を持つデータブロックだけが収集されます。

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

- `testShots` の各カラム（`setUpTable` 等）で groupId を省略すると、groupId なしのデータブロック（= デフォルトグループ）が収集されます
- バッチ固有の動作として、groupId に `"default"` を指定すると groupId なし扱いと同等になります（HTTP テスト・メッセージングテストではこの動作は適用されません）

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

**YAML 記述の必須キー**: `setup_tables` / `expected_tables` / `expected_complete_tables` の各エントリには `table` キーが必須です。省略するとエラーになります。

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#table-data)

### 5.2 SETUP_TABLE

DB への INSERT 用データを記述します。

- 各エントリのカラム名と値を記述します
- **主キーカラムは省略しないでください**。省略すると型に応じたデフォルト値（数値型は `"0"`、文字型はスペース等）が INSERT されます

**null 値・空文字の動作**:

| 値の指定 | Excel | YAML |
|---|---|---|
| null（Java null） | セルに `null`（大文字小文字不問）と記述 | アンクォートの `null` を記述（`"null"` でも同じ結果） |
| 空文字 | セルを空にする | `""` と記述 |
| 日付型カラムの空文字 | セルを空にする → `null` 扱い | `""` → `null` 扱い |

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#setup-table)

### 5.3 EXPECTED_TABLE

テスト後の DB 状態と比較するデータを記述します。

- **省略したカラムは比較対象外**になります。検証したいカラムだけを列挙できます

→ [Excel / YAML Example](ntf-testdata-doc-examples-table.md#expected-complete-table)

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

**Excel 混在禁止**: Excel では `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一シート内で混在させると、後半のデータが読み込まれません。同じ種別のデータブロックをまとめて記述してください。YAML では `expected_tables` と `expected_complete_tables` は別キーのため混在可能です。

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

セットアップ用のファイルデータ（`SETUP_FIXED` / `SETUP_VARIABLE`）は、固定長・可変長の区別なくまとめて収集されます。期待値ファイル（`EXPECTED_FIXED` / `EXPECTED_VARIABLE`）も同様です。固定長か可変長かはデータブロック内の記述で区別されます。

**YAML 記述の必須キー**: `setup_files` / `expected_files` の各エントリには `path` キーが必須です。省略するとエラーになります（`table` キーと同様）。

### 6.2 ファイルデータブロックの構造

ファイルデータブロックは以下の順序で記述します。

1. **ディレクティブ**（0件以上）: エンコーディング等のファイル属性を指定します
2. **レコード種別とフィールド名称**: 先頭要素 = レコード種別、以降 = フィールド名称
3. **データ型**（各フィールドのデータ型記号）
4. **フィールド長**（固定長のみ）: 各フィールドのバイト長
5. **データ**（1件以上）: 実データ

**Excel 固有の制約**: データの先頭要素は必ず空（null または空文字）にする必要があります。YAML にはこの制約はありません。

**Excel の記述例**（ディレクティブ → レコード種別+フィールド名称 → データ型 → フィールド長 → データ）:

セルをそのまま示します（各セルを `|` で区切って表示）。

```
行1: SETUP_FIXED=work/input.txt  [空]     [空]
行2: text-encoding               MS932   [空]
行3: DATA                        USER_ID  AMOUNT
行4: [空]                        X        Z
行5: [空]                        10       10
行6: [空]                        001      5000
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

- `fields:` の各要素は `{name: フィールド名, type: データ型, length: バイト長}` の形式で記述します
- `length` の値は整数（例: `length: 10`）または文字列（例: `length: "10"`）どちらでも有効です。変換ツールが生成した YAML は文字列形式（`"10"`）になります
- `rows:` の各行は配列形式で、`fields:` と**同じ順序・同じ件数**で値を並べます
- `rows:` 内の値はダブルクォートで囲んでください（[8章](#8-値の書き方) 参照）

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#file-data)

### 6.3 固定長ファイル固有の仕様

- フィールド名称・データ型・フィールド長の3リストが同サイズで必須です
- 1ファイルデータブロック内の全レコード定義は同一レコード長でなければなりません。違反した場合はエラーになります
- フィールド値がフィールド長を超えた場合はエラーになります

### 6.4 可変長ファイル固有の仕様

- フィールド名称・データ型の2リストが同サイズで必須です。フィールド長は不要です
- **空エントリの動作**: ファイルデータの空エントリ（先頭フィールドが空の行）はデータ行として扱われます。可変長ファイルの場合は全フィールドが `""` のレコードとして保持され、固定長ファイルの場合はスペースパディングされた定長レコードとして書き出されます（テーブルデータの空行スキップとは異なる動作です。テーブルデータの空行スキップは [10.5節](#105-空エントリのスキップ) を参照）

### 6.5 複数レコードレイアウト

1ファイルデータブロック内に複数のレコードレイアウトを連続して記述できます。データの後ろに新たなレコード種別とフィールド名称を書くと、新しいレコードレイアウトとして扱われます。

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#multi-record)

### 6.6 空ファイル

0バイトの空ファイルを表現するには、ディレクティブのみを記述してレコード定義を省略します。

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#empty-file)

### 6.7 `"-"` 長フィールド

フィールド長に `"-"` を指定すると、追加された全レコードの最大バイト長に自動拡張されます。値は改行コードと前後空白が除去されます。

### 6.8 エラーになるケース

- 同一レコード種別内でフィールド名称が重複している
- フィールド名称リストまたはデータ型リストが未指定または空
- フィールド名称・データ型・フィールド長リストのサイズが一致していない
- 存在しないフィールド名称を指定している
- データ要素数が不正
- ディレクティブまたはレコード種別/フィールド名称定義の要素数が2未満
- ファイルの読み込みに失敗した（IO エラー）
- 日付型カラムの値が日付として解析できない

---

## 7. メッセージングテストデータ

### 7.1 sendSyncTestData の配置規則

テストデータファイルは `sendSyncTestData/{requestId}/message` というパスに配置します（末尾の `message` は固定のパスセグメントです）。

- **Excel**: `MESSAGE=sendSyncTestData/{requestId}/message` をデータブロック識別子として記述します
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

- `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` のエントリ数（rows 合計）は一致が必須です。不一致の場合はエラーになります
- HTTP 同期応答メッセージ（`response_body_messages`）の各データエントリは文字列長が同一である必要があります

### 7.4 no カラムと errorMode

- **Excel**: `no` カラム（先頭カラム）はフレームワークが除去し、データとして保存されません。フィールド名称行の先頭セルは空にします
- **YAML**: `no` フィールドは `rows:` のリスト要素に含めます。フレームワークが除去します
- `errorMode` の値は先頭から2番目のカラム（1始まりで番号1）に格納されます
- `errorMode:timeout` および `errorMode:msgException` は特殊値です。これらが指定されたエントリでは他フィールドはパースされません

### 7.5 複数回送信

N 回送信する場合は、ヘッダ件数とボディ件数をともに N 件ずつ記述します。同一リクエスト ID で複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させます。

### 7.6 メッセージの groupId 収集

同一 groupId を持つ複数のメッセージプールを収集します。識別子の値をリクエスト ID として使用します。

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
| 改行文字（CR） | `\\r` | `"\\r"` | LineSeparatorInterpreter が変換（デフォルト設定は CR のみ） |

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
| `LineSeparatorInterpreter` | `\\r` → CR（0x0D）に変換（デフォルト設定）。`setMatchPattern` / `setLineSeparator` で変換対象・変換後の改行コードを変更可能 |
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

フィールドのデータ型は以下の日本語型名称で指定します。使用できない型名称を指定するとエラーになります。

| 型名称 | 型記号 | 用途 |
|---|---|---|
| `半角英字` / `半角数字` / `半角記号` / `半角カナ` / `半角英数字` / `半角英数字記号` / `半角` | `X` | 半角文字 |
| `全角英字` / `全角数字` / `全角ひらがな` / `全角カタカナ` / `全角漢字` / `全角` | `N` | 全角文字 |
| `全半角` | `XN` | 全角・半角混在 |
| `数値` / `符号無ゾーン10進数` | `Z` | ゾーン10進数（符号なし） |
| `符号付ゾーン10進数` | `SZ` | ゾーン10進数（符号あり） |
| `符号無パック10進数` | `P` | パック10進数（符号なし） |
| `符号付パック10進数` | `SP` | パック10進数（符号あり） |
| `符号無数値` | `X9` | バイナリ表現の数値（符号なし） |
| `符号付数値` | `SX9` | バイナリ表現の数値（符号あり） |
| `バイナリ` | `B` | バイナリデータ |

`TEST_{型名称}` という名前のデータ型を定義すると、同名の基底型より優先して使用されます（テスト専用の型定義に使います）。

---

## 9. ディレクティブ

### 9.1 ディレクティブの構成

ディレクティブは「キー名・値」の2要素で記述します（最低2要素必要）。

- **Excel**: ファイルデータブロックの先頭（レコード定義より前）に `| キー名 | 値 |` の形で記述します
- **YAML**: `directives:` オブジェクトに `key: value` 形式で記述します

### 9.2 固定長ファイルのディレクティブ

固定長ファイルで有効なディレクティブキーは以下に限定されます。無効なキーを指定するとエラーになります。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Fixed"`）。通常は記述不要です |
| `text-encoding` | ファイルの文字エンコーディング |
| `record-length` | フィールド長合計から自動計算。通常は記述不要です |
| `record-separator` | レコード区切り文字 |
| `positive-zone-sign-nibble` | ゾーン10進数の正符号ニブル |
| `negative-zone-sign-nibble` | ゾーン10進数の負符号ニブル |
| `positive-pack-sign-nibble` | パック10進数の正符号ニブル |
| `negative-pack-sign-nibble` | パック10進数の負符号ニブル |
| `required-decimal-point` | 小数点を必須とするか（`true` / `false`） |
| `fixed-sign-position` | 符号を固定位置に置くか（`true` / `false`） |
| `required-plus-sign` | 正符号を出力するか（`true` / `false`） |

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#file-data)

### 9.3 可変長ファイルのディレクティブ

可変長ファイルで有効なディレクティブキーは以下に限定されます。無効なキーを指定するとエラーになります。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Variable"`）。通常は記述不要です |
| `text-encoding` | ファイルの文字エンコーディング |
| `record-separator` | レコード区切り。`NONE` / `CR` / `LF` / `CRLF` または任意リテラル文字列が有効です |
| `field-separator` | フィールド区切り文字。デフォルトは `","` です。`"\\t"` 指定でタブ文字になります。**1文字のみ有効**（2文字以上はエラーになります） |
| `quoting-delimiter` | クォート文字 |
| `ignore-blank-lines` | 空行を無視するか |
| `requires-title` | タイトル行の有無 |
| `max-record-length` | レコードの最大長 |
| `title-record-type-name` | タイトルレコードの種別名 |

→ [Excel / YAML Example](ntf-testdata-doc-examples-file.md#file-data)

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
- **YAML**: `setup_tables` / `expected_tables` / `list_maps` すべてでマーカーカラムが除外されます

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

→ [Excel / YAML Example](ntf-testdata-doc-examples-special.md#header-comment)
