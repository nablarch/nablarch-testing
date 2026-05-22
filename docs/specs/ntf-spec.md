# NTF テストデータ仕様書

- **対象**: Nablarch Testing Framework（NTF）が読み込むテストデータの構造・ルール・制約
- **対応仕様ID**: DT-01〜08 / SS-01〜32 / HC-01〜07 / IV-01〜16 / DR-01〜12 / MS-01〜14 / TS-01〜32
- **形式非依存**: 本書は論理仕様を記述します。Excel・YAML のどちらで記述する場合も同じルールが適用されます
- **記述例**: 各節末尾のリンクから Excel 表と YAML コードブロックの対比例を参照できます

---

## 目次

1. [概要](#1-概要)
2. [セクション識別](#2-セクション識別)
3. [テーブルデータ](#3-テーブルデータ)
4. [ファイルデータ](#4-ファイルデータ)
5. [メッセージングテストデータ](#5-メッセージングテストデータ)
6. [特殊値・インタープリタ](#6-特殊値インタープリタ)
7. [ディレクティブ](#7-ディレクティブ)
8. [ヘッダ行・コメント・空行](#8-ヘッダ行コメント空行)
9. [付録: 仕様ID索引](#9-付録-仕様id索引)

---

## 1. 概要

### 1.1 NTF テストデータとは

NTF テストデータファイルには、次の3種類のデータを記述します。

**テストケース**  
テストの実行条件を1エントリ1ケースで定義します。`LIST_MAP=testShots` に記述し、各エントリが1テストケースを表します。HTTP テストなら「リクエストユーザー・期待ステータスコード・期待フォワード先 URI」など、バッチテストなら「実行パス・ユーザー ID・DIコンフィグ・期待ステータスコード」などを列挙します。

**セットアップ**  
テスト実行前に投入するデータです。DB テーブルへの INSERT データ（`SETUP_TABLE`）、固定長・可変長ファイルの入力データ（`SETUP_FIXED` / `SETUP_VARIABLE`）などを定義します。

**期待値**  
テスト後に検証するデータです。DB の期待値（`EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE`）、出力ファイルの期待値（`EXPECTED_FIXED` / `EXPECTED_VARIABLE`）、電文の期待値（`MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` 等）、ログや検索結果等の期待値（`LIST_MAP`）などを定義します。

これらは**セクション**という単位で管理され、`DataType名=識別子` の形式で区別されます。セクションの詳細は [2章](#2-セクション識別)、各データの詳細は [3章](#3-テーブルデータ)〜[5章](#5-メッセージングテストデータ) で説明します。

### 1.2 テストデータファイルの全体構造

1つのテストデータファイルは、**セクション**の集まりで構成されます。各セクションは「データ種別（DataType）」と「識別子」の組み合わせで識別されます。

1つのファイルに複数種別のセクションを共存させることができます。

---

## 2. セクション識別

### 2.1 セクション識別行の書式（DT-02）

各セクションの先頭には識別行を記述します。書式は以下のとおりです。

```
<DataType名>[groupId]=<識別子の値>
```

- `DataType名`: 後述する14種類のいずれか（例: `SETUP_TABLE`）
- `[groupId]`: 省略可能です。省略時は空文字扱いになります
- `=`: 必須の区切り文字です
- `識別子の値`: テーブル名・ファイルパス・IDなどセクション種別ごとの識別子です

**Excel 固有の動作**: Excel 実装では DataType 判定に前方一致（`startsWith`）を使用します。セル値が DataType 名で始まれば合致します（DT-03）。YAML では完全なセクションキーを使用するため前方一致は発生しません。

→ [Excel / YAML Example](ntf-spec-examples.md#section-identifier)

### 2.2 DataType の種類（DT-01）

テストデータで使用できる DataType は以下の14種類です。

| DataType名 | 用途 | 収集方式 |
|---|---|---|
| `SETUP_TABLE` | INSERT 用テーブルデータ | GroupData（全件収集） |
| `EXPECTED_TABLE` | 比較用テーブルデータ（省略カラムは比較対象外） | GroupData（全件収集） |
| `EXPECTED_COMPLETE_TABLE` | 比較用テーブルデータ（省略カラムにデフォルト値補完） | GroupData（全件収集） |
| `LIST_MAP` | キーバリュー形式のデータ（バッチ等のパラメータ） | SingleData（先着一致） |
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

### 2.3 GroupData と SingleData（DT-04/05）

セクションの収集方式は DataType によって異なります。

- **GroupData**: 同一 groupId を持つセクションをすべて収集します。ファイル全体を最後まで読み込みます（`SETUP_TABLE`、`EXPECTED_TABLE`、ファイル系など）
- **SingleData**: 最初に一致したセクション1件だけを取得して停止します（`LIST_MAP`、`MESSAGE` など）

`LIST_MAP` で同一 ID のエントリが複数ある場合、2件目以降は黙って無視されます（SS-06）。

### 2.4 groupId の書式と制約（DT-06/08）

- 書式: `[groupId]`（角括弧で囲みます）
- 省略時は空文字扱いです
- groupId の指定は1件のみ有効です。2件以上指定すると `IllegalArgumentException` がスローされます

バッチ固有の動作として、`group_id: "default"` はグループIDなし扱いと同等になります。

### 2.5 RESPONSE_HEADER/BODY_MESSAGES の2経路（DT-07）

`RESPONSE_HEADER_MESSAGES` と `RESPONSE_BODY_MESSAGES` は、以下の2つの経路でアクセスできます。

- **経路A（GroupData）**: groupId を指定して収集する経路
- **経路B（SingleData）**: ID で一致する経路

---

## 3. テーブルデータ

### 3.1 データ行の形式（SS-01）

テーブルデータの各行は「カラム名=値」の形式で記述します。省略したカラムには INSERT 時にデフォルト値が補完されます。

→ [Excel / YAML Example](ntf-spec-examples.md#table-data)

### 3.2 SETUP_TABLE（SS-01/04）

DB への INSERT 用データです。

- 各行のカラム名と値を記述します
- **主キーカラムは省略不可**です。省略するとデフォルト値（`"0"` やスペース等）が INSERT されます

### 3.3 EXPECTED_TABLE（SS-02）

テスト後の DB 状態と比較するデータです。

- **省略したカラムは比較対象外**になります。検証したいカラムだけを列挙できます

### 3.4 EXPECTED_COMPLETE_TABLE（SS-03/18）

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

**注意**: DATE カラムのデフォルト値は JVM のタイムゾーン設定に依存します。JST 環境と UTC 環境では値が異なります（SS-18）。

**混在禁止（SS-05）**: `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一ファイル内で混在させると、後半のデータが読み込まれません。同じ種別のセクションをまとめて記述してください。

→ [Excel / YAML Example](ntf-spec-examples.md#expected-complete-table)

### 3.5 LIST_MAP（SS-06/19）

キーバリュー形式のデータです。

- ID は完全一致で検索されます
- `testShots` は予約 ID です。バッチリクエスト単体テストでフレームワークがテストケース一覧として自動読み込みします（SS-19）

→ [Excel / YAML Example](ntf-spec-examples.md#list-map)

---

## 4. ファイルデータ

### 4.1 固定長・可変長の統合（SS-07）

`SETUP_FIXED` と `SETUP_VARIABLE` は `getSetupFile()` でまとめて返されます。`EXPECTED_FIXED` / `EXPECTED_VARIABLE` も同様です。ファイル種別はセクション内の属性（固定長 or 可変長）で区別します。

### 4.2 ファイルセクションの行構造（SS-08/12/13）

ファイルセクションは以下の順序で記述します。

1. **ディレクティブ行**（0行以上）: エンコーディング等のファイル属性を指定します
2. **フィールド名行**: 先頭列 = レコード種別名、2列目以降 = フィールド名
3. **データ型行**: 各フィールドのデータ型記号（先頭列は空）
4. **フィールド長行**（固定長のみ）: 各フィールドのバイト長（先頭列は空）
5. **データ行**（1行以上）: 実データ（先頭列は空）

**Excel 固有の制約**: データ行の先頭セルは必ず空（null または空文字）にする必要があります（SS-13）。YAML にはこの制約はありません。

→ [Excel / YAML Example](ntf-spec-examples.md#file-data)

### 4.3 固定長ファイル固有の仕様（SS-09/16/23）

- `names` / `types` / `lengths` の3リストが同サイズで必須です（SS-09）
- ファイル内の全フラグメントは同一レコード長でなければなりません。違反時は `IllegalStateException` がスローされます（SS-16）
- フィールド値がフィールド長を超えた場合は `IllegalStateException` がスローされます（SS-23）

### 4.4 可変長ファイル固有の仕様（SS-10/20）

- `names` / `types` の2リストが同サイズで必須です。`lengths` は不要です（SS-10）
- **空行の動作**: 可変長ファイルの空行はスキップされず、全フィールドが `""` のレコードとして保持されます。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出されます（SS-20）

### 4.5 複数レコードレイアウト（SS-11）

1ファイルセクション内に複数のレコードレイアウトを連続して記述できます。データ行の後ろに新たなフィールド名行を書くと、新しいレコードレイアウトとして扱われます。

→ [Excel / YAML Example](ntf-spec-examples.md#multi-record)

### 4.6 空ファイル（SS-15）

0バイトの空ファイルを表現するには、ディレクティブ行のみを記述してレコード定義を省略します。

→ [Excel / YAML Example](ntf-spec-examples.md#empty-file)

### 4.7 `"-"` 長フィールド（SS-17）

フィールド長に `"-"` を指定すると、追加された全レコードの最大バイト長に自動拡張されます。値は改行コードと前後空白が除去されます。

### 4.8 異常系（SS-14/21〜28/30）

| 条件 | 例外 |
|---|---|
| 同一レコード種別内でフィールド名が重複 | `IllegalArgumentException` |
| フィールド名リストまたは型リストが null/空 | `IllegalArgumentException` |
| フィールド名リストと型/長さリストのサイズ不一致 | `IllegalArgumentException` |
| 存在しないフィールド名を指定 | `IllegalArgumentException` |
| データ要素数が不正 | `IllegalStateException` |
| ディレクティブ行またはフィールド名行の列数が2未満 | `IllegalStateException` |
| ファイル読み込み失敗（IO 例外） | `RuntimeException` |
| 日付型カラムの値が日付として解析できない | `RuntimeException` |

---

## 5. メッセージングテストデータ

### 5.1 sendSyncTestData の配置規則（MS-07）

テストデータファイルは `sendSyncTestData` ベースパス下にリクエスト ID と同名のファイルとして配置します。

```
sendSyncTestData/{requestId}/message
```

### 5.2 FW 制御ヘッダフィールド（MS-01）

デフォルトの FW 制御ヘッダフィールドは以下の4種類です。`reader.fwHeaderfields` キーで変更できます。

- `requestId`
- `userId`
- `resendFlag`
- `resultCode`

### 5.3 HEADER / BODY MESSAGES の構造と行数制約（MS-05/11）

- `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（rows 合計）は一致が必須です。不一致の場合は `IllegalStateException` がスローされます（MS-05）
- HTTP 同期応答メッセージ（`response_body_messages`）の各データ行は文字列長が同一である必要があります（MS-11）

### 5.4 no 列と errorMode（MS-02/04）

- `no` 列（先頭列）はフレームワークが除去し、データとして保存されません
- `errorMode` の値は列番号1に格納されます
- `errorMode:timeout` および `errorMode:msgException` は特殊値です。これらが指定された行では他フィールドはパースされません（MS-04）

### 5.5 複数回送信（MS-09/10）

N 回送信する場合は、ヘッダ行数とボディ行数をともに N 行ずつ記述します。同一リクエスト ID で複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させます。

### 5.6 GroupMessageParser（MS-06）

同一 groupId の複数メッセージプールを収集します。セクション識別子 `=` 以降をリクエスト ID として使用します。

### 5.7 ステータスコード（MS-08）

ステータスコード列がない場合はデフォルト値 `"200"` が使用されます。

### 5.8 フォーマット定義ファイルの命名規則（MS-12）

- 応答電文: `{requestId}_RECEIVE`
- 要求電文: `{requestId}_SEND`

### 5.9 アサート方式の切り替え（MS-13）

SystemRepository の `messaging.assertAsMapFileType` キーの設定値に応じてアサート方式が切り替わります。未設定時のデフォルトは `"Fixed"` 形式（項目単位アサート）です。

### 5.10 record_type の扱い（MS-03）

`MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は、内部で常に `"default"` に置き換えられます。任意の値を記述できます（装飾的なメタデータとして扱われます）。

→ [Excel / YAML Example](ntf-spec-examples.md#messaging)

---

## 6. 特殊値・インタープリタ

### 6.1 インタープリタチェーンの仕組み

テストデータの値はパース時にインタープリタチェーンを通過し、変換されます。DI 設定で注入されたインタープリタが順番に適用されます。

### 6.2 インタープリタ一覧（IV-01〜08）

| インタープリタ | 変換内容 |
|---|---|
| `NullInterpreter` | `null` / `NULL` / `Null`（大文字小文字不問）→ Java null（IV-01） |
| `QuotationTrimmer` | 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去（IV-02） |
| `DateTimeInterpreter` | `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換（IV-03） |
| `LineSeparatorInterpreter` | `\\r` → CR（0x0D）、`\\n` → LF（0x0A）に変換（IV-04） |
| `BinaryFileInterpreter` | `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換（IV-05） |
| `BasicJapaneseCharacterInterpreter` | `${文字種,文字数}` 形式で文字列生成（IV-06） |
| `CompositeInterpreter` | 文字列中の `${...}` 要素を個別解釈して置換（IV-08） |

### 6.3 DateTimeInterpreter の完全一致制約（IV-03）

`DateTimeInterpreter` は完全一致のみ変換します。部分文字列は変換されません。文字列中の `${...}` を置換するには `CompositeInterpreter` との組み合わせが必要です。

### 6.4 BasicJapaneseCharacterGenerator の有効文字種（IV-07）

14種類の文字種が使用できます: 半角英字 / 半角数字 / 半角記号 / 半角カナ / 全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 / 中国語 / サロゲートペア / 改行 / 外字

未知の文字種を指定すると `IllegalArgumentException` がスローされます（IV-16）。

### 6.5 QuotationTrimmer によるスペース値明示記法（IV-14）

空白値を可視化して記述するための記法です。

| 記述 | 結果 |
|---|---|
| `" "` | 半角スペース1文字 |
| `"""` | ダブルクォート1文字 |

### 6.6 日付型カラムの記述形式と境界値（IV-09/10）

有効な記述形式は以下のとおりです。

- `yyyyMMddHHmmssSSS`（17文字）
- 後置0埋め短縮形
- JDBC タイムスタンプエスケープ形式（5文字目が `-`）

`java.sql.Timestamp` 型カラムの期待値は末尾 `.0` が必須です（例: `"2010-01-01 12:34:56.0"`）。末尾 `.0` がないとアサートが失敗します（IV-10）。

→ [Excel / YAML Example](ntf-spec-examples.md#datetime)

### 6.7 バイナリデータの記述（IV-11）

`0x` プレフィクス付き16進数で記述できます。`0x` がない場合は文字列としてエンコードされます。

### 6.8 X9/SX9 型フィールドの記述（IV-15）

パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記述します。

### 6.9 データ型マッピング（IV-12/13）

`BasicDataTypeMapping` のデフォルトマッピング22種が使用できます。未知の型記号を指定すると `IllegalArgumentException` がスローされます。

`TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用されます（IV-13）。

---

## 7. ディレクティブ

### 7.1 ディレクティブ行の構成（DR-01）

ディレクティブ行は「先頭列 = キー名、2列目 = 値」の形式で記述します（最低2列必要）。

### 7.2 固定長ファイルのディレクティブ（DR-02）

固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます（DR-11）。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Fixed"`）。通常は記述不要です（DR-07） |
| `record-length` | フィールド長合計から自動計算。通常は記述不要です（DR-08） |
| `text-encoding` | ファイルの文字エンコーディング |
| `positive-zone-sign-nibble` | ゾーン10進数の正符号ニブル |
| その他 | `FixedLengthDirective` 列挙型の定義を参照してください |

### 7.3 可変長ファイルのディレクティブ（DR-03/09/10/12）

可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定されます。無効なキーを指定すると `IllegalArgumentException` がスローされます（DR-11）。

| ディレクティブキー | 説明 |
|---|---|
| `file-type` | 自動設定（`"Variable"`）。通常は記述不要です（DR-07） |
| `field-separator` | フィールド区切り文字。デフォルトは `","` です。`"\\t"` 指定でタブ文字になります。**1文字のみ有効**（2文字以上は `IllegalArgumentException`）（DR-09/12） |
| `record-separator` | レコード区切り。`NONE` / `CR` / `LF` / `CRLF` または任意リテラル文字列が有効です（DR-10） |
| `quoting-delimiter` | クォート文字 |
| その他 | `VariableLengthDirective` 列挙型の定義を参照してください |

### 7.4 デフォルトディレクティブの DI 設定（DR-04/05/06）

SystemRepository への DI 設定で、全ファイル共通または種別専用のデフォルトディレクティブを一括設定できます。

| DI キー | 適用対象 |
|---|---|
| `defaultDirectives` | 全ファイル共通のデフォルト（DR-04） |
| `fixedLengthDirectives` | 固定長ファイル専用。`defaultDirectives` より後に上書き適用されます（DR-05） |
| `variableLengthDirectives` | 可変長ファイル専用（DR-06） |

→ [Excel / YAML Example](ntf-spec-examples.md#directive)

---

## 8. ヘッダ行・コメント・空行

### 8.1 ヘッダ行の構造

ヘッダ行にはカラム名を列挙します。

- ヘッダ行末尾の空カラムは除去されます（末尾カラムの省略が可能です）（HC-03）
- データ行がヘッダより短い場合、不足分は空文字 `""` で補完されます（HC-04）

### 8.2 マーカーカラム（HC-01/02）

`[カラム名]` 形式（角括弧で囲まれた名前）のカラムはマーカーカラムとして扱われ、DB 操作から除外されます。

### 8.3 コメント行（HC-05）

先頭セルが `//` で始まる行は行ごとスキップされます。

**Excel 固有**: Excel では `//` 先頭のセルでコメント行を表現します。  
**YAML**: YAML では標準のコメント構文（`#`）を使用します。

### 8.4 行内コメント（HC-06）

先頭以外のセルが `//` で始まる場合、そのセル以降が切り捨てられます。

**Excel 固有**: Excel 実装の動作です。  
**YAML**: YAML では行末コメント（`#`）で同等の機能を実現できます。

### 8.5 空行スキップ（HC-07）

全要素が null または空文字の行は読み飛ばされます。

---

## 9. 付録: 仕様ID索引

| 仕様ID | 概要 | 分類 |
|---|---|---|
| DT-01 | DataType 列挙値14種 | 正常系 |
| DT-02 | セクション識別行の書式 `<DataType名>[groupId]=<値>` | 正常系 |
| DT-03 | DataType 判定は前方一致（Excel 固有・YAML 非適用） | 正常系 |
| DT-04 | GroupData 系は全件収集（`shouldStopOnNextOne() = false`） | 正常系 |
| DT-05 | SingleData 系は先着一致で停止（`shouldStopOnNextOne() = true`） | 正常系 |
| DT-06 | groupId 書式 `[groupId]`・省略時は空文字・バッチ固有 `"default"` 扱い | 正常系 |
| DT-07 | RESPONSE_HEADER/BODY_MESSAGES の GroupData 経路と SingleData 経路 | 正常系 |
| DT-08 | groupId 引数に2件以上指定で `IllegalArgumentException` | 異常系 |
| SS-01 | テーブルデータ行はカラム名=値形式。省略カラムにはデフォルト値補完 | 正常系 |
| SS-02 | EXPECTED_TABLE: 省略カラムは比較対象外 | 正常系 |
| SS-03 | EXPECTED_COMPLETE_TABLE: 省略カラムにデフォルト値補完してから比較 | 正常系 |
| SS-04 | SETUP_TABLE: 主キーカラムは省略不可 | 正常系 |
| SS-05 | EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE の混在で後半データ欠落 | 正常系 |
| SS-06 | LIST_MAP=id: id は完全一致・重複エントリは先着一致 | 正常系 |
| SS-07 | SETUP_FIXED と SETUP_VARIABLE は getSetupFile() でまとめて返される | 正常系 |
| SS-08 | ファイルセクションの行順序: ディレクティブ→フィールド名→型→長さ→データ | 正常系 |
| SS-09 | 固定長フラグメント: names/types/lengths の3リストが同サイズで必須 | 正常系 |
| SS-10 | 可変長フラグメント: names/types の2リストが同サイズで必須・lengths 不要 | 正常系 |
| SS-11 | 1ファイルセクション内に複数レコードレイアウトを連続記述可能 | 正常系 |
| SS-12 | フィールド名行: 先頭列=レコード種別名、2列目以降=フィールド名 | 正常系 |
| SS-13 | データ行の先頭セルは必ず空（Excel 固有・YAML 非適用） | 正常系 |
| SS-14 | 同一レコード種別内のフィールド名重複で `IllegalArgumentException` | 異常系 |
| SS-15 | 空ファイル表現: ディレクティブ行のみ・レコード定義省略 | 正常系 |
| SS-16 | 固定長ファイル: 全フラグメントで同一レコード長が必須 | 異常系 |
| SS-17 | `"-"` 長フィールド: 最大バイト長に自動拡張 | 正常系 |
| SS-18 | BasicDefaultValues のデフォルト値一覧（DATE は JVM TZ 依存） | 正常系 |
| SS-19 | testShots は LIST_MAP の予約 ID | 正常系 |
| SS-20 | 空行動作差異: 可変長は全フィールド `""` で保持・固定長はスペースパディング | 正常系 |
| SS-21 | フィールド名/型リストが null/空で `IllegalArgumentException` | 異常系 |
| SS-22 | フィールド名/型/長さリストのサイズ不一致で `IllegalArgumentException` | 異常系 |
| SS-23 | 固定長フィールド値がフィールド長超過で `IllegalStateException` | 異常系 |
| SS-24 | 存在しないフィールド名指定で `IllegalArgumentException` | 異常系 |
| SS-25 | データ要素数不正で `IllegalStateException` | 異常系 |
| SS-26 | ファイル読み込み失敗（IO 例外）で `RuntimeException` | 異常系 |
| SS-27 | DataFileParser.Status が想定外状態で `IllegalStateException`（通常到達不能） | 異常系 |
| SS-28 | ディレクティブ/フィールド名行の列数2未満で `IllegalStateException` | 異常系 |
| SS-29 | TableData#getClone() の CloneNotSupportedException（到達不能） | 異常系 |
| SS-30 | 日付型カラムの値が解析不可で `RuntimeException` | 異常系 |
| SS-31 | TableData#getValue() でカラム値が null の場合 null を返す | 代替フロー |
| SS-32 | TableData#toTimestamp() で空文字の場合 null を返す | 代替フロー |
| HC-01 | マーカーカラムの書式: `[カラム名]` | 正常系 |
| HC-02 | マーカーカラムは DB 操作から除外 | 正常系 |
| HC-03 | ヘッダ行末尾の空カラムは除去 | 正常系 |
| HC-04 | データ行がヘッダより短い場合、不足分は `""` で補完 | 正常系 |
| HC-05 | コメント行: 先頭セルが `//` で始まる行はスキップ | 正常系 |
| HC-06 | 行内コメント: 先頭以外のセルが `//` で始まる場合、以降を切り捨て | 正常系 |
| HC-07 | 空行スキップ: 全要素が null/空文字の行は読み飛ばす | 正常系 |
| IV-01 | NullInterpreter: null/NULL/Null → Java null | 正常系 |
| IV-02 | QuotationTrimmer: ダブルクォートで囲まれた場合のみ外側1層除去 | 正常系 |
| IV-03 | DateTimeInterpreter: ${systemTime} 等の完全一致のみ変換 | 正常系 |
| IV-04 | LineSeparatorInterpreter: `\\r`→CR、`\\n`→LF | 正常系 |
| IV-05 | BinaryFileInterpreter: ${binaryFile:パス} でバイナリ読み込み→HexString | 正常系 |
| IV-06 | BasicJapaneseCharacterInterpreter: ${文字種,文字数} 形式で文字列生成 | 正常系 |
| IV-07 | BasicJapaneseCharacterGenerator の有効文字種14種 | 正常系 |
| IV-08 | CompositeInterpreter: ${...} 要素を個別解釈して置換 | 正常系 |
| IV-09 | 日付型カラムの記述形式（17文字・後置0埋め・JDBCエスケープ形式） | 正常系 |
| IV-10 | Timestamp 型期待値は末尾 `.0` 必須 | 正常系 |
| IV-11 | バイナリデータ: `0x` プレフィクス付き16進数で記述可能 | 正常系 |
| IV-12 | BasicDataTypeMapping デフォルトマッピング22種 | 正常系 |
| IV-13 | TEST_ プレフィクス型が存在する場合は自動優先選択 | 正常系 |
| IV-14 | QuotationTrimmer によるスペース値明示記法 | 正常系 |
| IV-15 | X9/SX9 型フィールド: 実値をそのまま記述 | 正常系 |
| IV-16 | 未知の文字種指定で `IllegalArgumentException` | 異常系 |
| DR-01 | ディレクティブ行: 先頭列=キー名・2列目=値（最低2列） | 正常系 |
| DR-02 | 固定長ファイルのディレクティブキーは FixedLengthDirective 列挙型に限定 | 正常系 |
| DR-03 | 可変長ファイルのディレクティブキーは VariableLengthDirective 列挙型に限定 | 正常系 |
| DR-04 | defaultDirectives DI: 全ファイル共通デフォルトディレクティブ | 実装内部ロジック |
| DR-05 | fixedLengthDirectives DI: 固定長専用デフォルトディレクティブ | 実装内部ロジック |
| DR-06 | variableLengthDirectives DI: 可変長専用デフォルトディレクティブ | 実装内部ロジック |
| DR-07 | file-type ディレクティブはサブクラスが自動設定（通常記述不要） | 正常系 |
| DR-08 | record-length ディレクティブはフィールド長合計から自動計算（通常記述不要） | 正常系 |
| DR-09 | field-separator: デフォルト `","` ・`"\\t"` でタブ・1文字のみ有効 | 正常系 |
| DR-10 | record-separator: NONE/CR/LF/CRLF または任意リテラル文字列 | 正常系 |
| DR-11 | 無効なディレクティブキーで `IllegalArgumentException` | 異常系 |
| DR-12 | 可変長 field-separator に2文字以上で `IllegalArgumentException` | 異常系 |
| MS-01 | FW 制御ヘッダフィールドデフォルト4種（reader.fwHeaderfields で変更可） | 正常系 |
| MS-02 | no 列（先頭列）はフレームワークが除去・errorMode は列番号1 | 正常系 |
| MS-03 | MESSAGE 系の record_type は内部で常に `"default"` に置き換え | 正常系 |
| MS-04 | errorMode:timeout / msgException は特殊値・他フィールドはパース対象外 | 正常系 |
| MS-05 | HEADER と BODY MESSAGES の行数不一致で `IllegalStateException` | 異常系 |
| MS-06 | GroupMessageParser: 同一 groupId の複数メッセージプールを収集 | 正常系 |
| MS-07 | sendSyncTestData/{requestId}/message の配置規則 | 正常系 |
| MS-08 | ステータスコード列なし時のデフォルト `"200"` | 代替フロー |
| MS-09 | マルチレコード送信: ヘッダ・ボディ各 N 行ずつ記述 | 正常系 |
| MS-10 | no 値を変えた連続記述で複数回送信・送信順序と no 値を一致させる | 正常系 |
| MS-11 | HTTP 同期応答ボディ: 各データ行の文字列長は同一 | 正常系 |
| MS-12 | フォーマット定義ファイル命名規則: {requestId}_RECEIVE / {requestId}_SEND | 正常系 |
| MS-13 | messaging.assertAsMapFileType キーでアサート方式を切り替え | 正常系 |
| MS-14 | SendSyncMessageParser#getFwHeader() は UnsupportedOperationException | 異常系 |
| TS-01 | `LIST_MAP=testShots` はテストケース定義の予約ID（旧ID `testCases` は後方互換） | 正常系 |
| TS-02 | `LIST_MAP=requestParams` は HTTP リクエストパラメータの予約ID | 正常系 |
| TS-03 | `LIST_MAP=responseResult` は HTTP レスポンス期待値の予約ID | 正常系 |
| TS-04 | `LIST_MAP=params` はエンティティバリデーション入力パラメータの予約ID（EntityTestSupport 専用） | 正常系 |
| TS-05 | `setUpDb` は DB 共通初期化シートの予約シート名 | 正常系 |
| TS-06 | testShots の `context` カラムが指す LIST_MAP から REQUEST_ID・USER_ID を取得。1行のみ有効 | 正常系 |
| TS-07 | HTTP テストの testShots 必須カラム: `no`・`description`・`isValidToken`・`expectedStatusCode`・`forwardUri`・`context` | 正常系 |
| TS-08 | バッチテストの testShots 必須カラム: `no`・`description`・`expectedStatusCode`・`diConfig`・`requestPath`・`userId` | 正常系 |
| TS-09 | バッチテストの testShots オプションカラム: `setUpFile`・`expectedFile`（空でスキップ） | 正常系 |
| TS-10 | `setUpTable` カラムに値があればケース固有の DB 初期化を実行。空でスキップ | 正常系 |
| TS-11 | `expectedTable` カラムに値があればテーブル期待値を検証。空でスキップ | 正常系 |
| TS-12 | `expectedLog` カラムに値があればログ期待値を読み込む。空でスキップ | 正常系 |
| TS-13 | `cookie` カラムが空の場合 Cookie なし（null 返却） | 代替フロー |
| TS-14 | `queryParams` カラムが空の場合クエリパラメータなし（null 返却） | 代替フロー |
| TS-15 | `HTTP_METHOD` カラムが空の場合デフォルト `"POST"` | 代替フロー |
| TS-16 | `expectedContentLength`・`expectedContentType`・`expectedContentFileName` が空の場合各検証スキップ | 代替フロー |
| TS-17 | `args[n]` カラムはコマンドライン引数、その他の任意カラムはコマンドラインオプション（バッチテスト） | 正常系 |
| TS-18 | testShots が空の場合 `IllegalStateException` / `IllegalArgumentException` をスロー | 異常系 |
| TS-19 | sheetName が null または空の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-20 | context LIST_MAP の REQUEST_ID が null または空の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-21 | context LIST_MAP が1行でない場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-22 | requestParams の行数がテストケース番号より少ない場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-23 | testShots の `no` カラムが空の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-24 | `description` も `case` も未定義の場合 `IllegalStateException` をスロー | 異常系 |
| TS-25 | cookie LIST_MAP 名を指定したが対応 LIST_MAP が空の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-26 | queryParams LIST_MAP 名を指定したが対応 LIST_MAP が空の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-27 | バッチテストの必須カラムが欠けている場合検証エラー | 異常系 |
| TS-28 | `expectedLog` に値があるが対応 LIST_MAP が空の場合 `IllegalStateException` をスロー | 異常系 |
| TS-29 | EntityTestSupport の testShots 件数と params 件数が不一致の場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-30 | EntityTestSupport の testShots 必須カラムが欠けている場合 `IllegalArgumentException` をスロー | 異常系 |
| TS-31 | `getParamMap()` でリスト2件以上は `IllegalArgumentException`・0件は空 Map を返す | 異常系/代替フロー |
| TS-32 | `assertTableEquals(failIfNoDataFound=false)` でデータなしは検証スキップ・`true` の場合は `IllegalArgumentException` | 異常系/代替フロー |
