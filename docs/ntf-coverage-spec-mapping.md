# NTF テストデータ仕様 カバレッジ スペックマッピング

## 概要

- **作成日**: 2026-05-15
- **調査クラス数**: 29クラス（直接影響クラス）
- **参照文書**: `ntf-coverage-class-list.md` §1

このドキュメントは、`nablarch-testing` リポジトリの各クラスから抜き出した仕様と、
現在の YAML スキーマ設計文書（`ntf-testdata-yaml-schema.json`, `ntf-testdata-yaml-design.md`, `ntf-testdata-yaml-examples.yaml`）との対応関係をまとめたものです。

---

## 1. reader パッケージ

### 1.1 DataType（セクション識別キー）

| 仕様 | 根拠（クラス/メソッド） | スキーマ対応 | 判定 |
|---|---|---|---|
| セクション識別キーは14種（`SETUP_TABLE`, `EXPECTED_TABLE`, `EXPECTED_COMPLETE_TABLE`, `LIST_MAP`, `SETUP_FIXED`, `EXPECTED_FIXED`, `SETUP_VARIABLE`, `EXPECTED_VARIABLE`, `MESSAGE`, `EXPECTED_REQUEST_HEADER_MESSAGES`, `EXPECTED_REQUEST_BODY_MESSAGES`, `RESPONSE_HEADER_MESSAGES`, `RESPONSE_BODY_MESSAGES`） | `DataType` enum | schema.json ルート properties（`SETUP_FIXED`/`EXPECTED_FIXED` は `setup_files`/`expected_files` に統合） | 反映済み |
| セクション識別は先頭一致マッチング | `TestDataParsingTemplate#isTargetSection()` | design.md §Excel概念→YAML対応表 | 反映済み |

### 1.2 TestDataParsingTemplate（パーシング共通）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 先頭セルが `//` で始まる行はコメント行としてスキップ | `isCommentRow()` | design.md §コメント行の扱い | 反映済み |
| 行内コメント: 先頭以外のセルが `//` で始まる場合、そのセル以降をすべて切り捨て | `cutComment()` | 未記載 | **未反映** |
| 全要素が空（null または空文字）の行は読み飛ばされる | `isBlankLine()` | 未記載 | **未反映** |
| セル値に TestDataInterpreter チェーンを適用（`${...}` 形式の特殊値展開） | `interpret()` | design.md §7 特殊値 / examples.yaml 特殊値節 | 反映済み |

### 1.3 GroupDataParsingTemplate（グループID）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| グループID付きセクション識別構文: `TYPE_NAME[groupId]=VALUE` 形式 | `isTargetType()` | schema.json `$defs.table_data.properties.group_id` / design.md §9 | 反映済み |
| 同一 groupId に一致する複数ブロックをすべて収集する | `shouldStopOnNextOne()=false` | schema.json の array 型定義 | 反映済み |

### 1.4 HeaderLine（マーカーカラム）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `[` で始まり `]` で終わるカラム名はマーカーカラムとして DB 操作から除外 | `isMarkerColumn()` | schema.json `$defs.table_data.properties.rows` / design.md §6 | 反映済み |
| ヘッダ行の後尾空要素は `trimTailCopy()` でトリムされる（末尾カラム省略可） | `trimTailCopy()` | 未記載 | **未反映** |

### 1.5 TableDataParser（テーブルセクション）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| テーブル名は `=` 以降の文字列として取得 | `getTypeValue()` | schema.json `$defs.table_data.properties.table` | 反映済み |
| セクション行→カラム名行→データ行の順序 | `onTargetTypeFound()` | design.md §変換ビフォーアフター | 反映済み |

### 1.6 ListMapParser（LIST_MAPセクション）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| SingleData系（同一IDの最初の1件のみ取得） | extends `SingleDataParsingTemplate` | schema.json `$defs.list_map_data` description | 反映済み |
| キー名行→データ行の構造 | `onTargetTypeFound()` | schema.json `$defs.list_map_data.properties.rows` | 反映済み |

### 1.7 MessageParser（MESSAGEセクション）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `record_type` 先頭カラムを常に `"default"` へ強制置換 | `onReadingNames()` のオーバーライド | design.md §11 / schema.json description | 反映済み |
| FW制御ヘッダフィールド（`requestId`, `userId`, `resendFlag`, `resultCode`）。SystemRepository `reader.fwHeaderfields` でカスタマイズ可能 | `isFrameworkHeader()` / `fwHeaderFields` | schema.json `$defs.message_data` description / design.md | 反映済み |
| Excel上のFW制御ヘッダは「フィールド名\|値」の2列ディレクティブ行形式だったが、YAMLでは通常の `fields` に統合される | `processDirectives()` + `isFrameworkHeader()` | 未記載（移行時の変換が必要なことが不明瞭） | **未反映** |
| データ行の先頭列（NO列）は `tail()` で除去して格納される | `onReadingValues()` → `tail(line)` | 未記載 | **未反映**（YAMLアダプタ実装時注意） |

### 1.8 SendSyncMessageParser（errorMode特殊値）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `errorMode:timeout` / `errorMode:msgException` という特殊値を2列目で認識 | `ERROR_MODE_COLUMN_NUMBER=1` / `ErrorMode` enum | design.md §AI向け / examples.yaml §errorMode節 | 反映済み |
| errorMode 行の `rows` には errorMode 値のみ格納（NO列除去なし） | `READING_VALUES` state の `if (errorMode != null)` 分岐 | examples.yaml の例（1要素配列）で正確に反映 | 反映済み |
| `response_*_messages` の通常データ行は先頭の NO 列を `remove(0)` して格納 | `addValueWithId()` 呼び出し前の `remove(NO_COLUMN_NUMBER)` | 未記載（通常データ行の例がない） | **未反映** |
| `getFwHeader()` が `UnsupportedOperationException` を投げる（FW制御ヘッダ分離なし） | `getFwHeader()` のオーバーライド | 未記載 | **未反映** |

### 1.9 DataFileParser（ファイルセクション行順序）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 状態機械による行順序: `READING_DIRECTIVES_AND_NAMES` → `READING_TYPES` → `READING_LENGTHS` → `READING_VALUES` | `Status` enum | design.md §変換ビフォーアフター（固定長） | 反映済み |
| ディレクティブ行と名前行の区別はその行の先頭セルが `isDirective()` かどうかで判定 | `isDirective()` 抽象メソッド | schema.json `$defs.directives` / `$defs.record_fragment` | 反映済み |
| 先頭要素が非空かつ非ディレクティブの行がフィールド名行として認識され、先頭要素が `record_type`、2要素目以降が `names` | `createNewFragment()` | schema.json `$defs.record_fragment.properties.record_type` / `fields[].name` | 反映済み |

### 1.10 FixedLengthFileParser（固定長ファイル）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 有効ディレクティブキーは `FixedLengthDirective` enum の値に限られる | `isDirective()` | schema.json `$defs.directives` の固定長専用キー | 反映済み |
| `file-type` は `"Fixed"` として自動設定（通常記述不要） | `FixedLengthFile` コンストラクタ | schema.json `directives.file-type` description | 反映済み |
| `record-length` はフィールド長の合計から自動計算（通常記述不要） | `FixedLengthFile#createLayout()` | 未記載（「通常は記述不要」の旨がない） | **未反映** |
| `SystemRepository["fixedLengthDirectives"]` でデフォルトディレクティブを DI 可能 | `prepareDefaultDirectives("fixedLengthDirectives")` | 未記載 | **未反映** |
| `TestDataConverter` は `SystemRepository["TestDataConverter_" + fileType]` で差し込め可能 | `getConverter()` | 未記載 | **未反映** |

### 1.11 VariableLengthFileParser（可変長ファイル）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 型行読み取り後に `READING_LENGTHS` をスキップして `READING_VALUES` へ直接遷移 → フィールド長行なし | `onReadingTypes()` のオーバーライド | schema.json `$defs.field_def.properties.length` description「可変長では省略可」 | 反映済み |
| `field-separator` のデフォルト値は `","` | `VariableLengthFile` コンストラクタ | schema.json `directives.field-separator` description | 反映済み |
| `field-separator` に `"\\t"` を指定するとタブ文字（U+0009）に変換される | `VariableLengthFile#convertDirectiveValue()` | 未記載 | **未反映** |
| `field-separator` は1文字のみ有効（`"\\t"` 変換後は1文字となるため有効） | `VariableLengthFile#setDirective()` の length check | 未記載 | **未反映** |
| `SystemRepository["variableLengthDirectives"]` でデフォルトディレクティブを DI 可能 | `prepareDefaultDirectives("variableLengthDirectives")` | 未記載 | **未反映** |

---

## 2. file パッケージ

### 2.1 DataFile（共通ディレクティブ）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `record-separator` の値は `LineSeparator.evaluate()` で処理（シンボル名またはリテラル） | `setDirective()` | schema.json `directives.record-separator` / design.md AI向け | 反映済み |
| `SystemRepository["defaultDirectives"]` で全ファイル共通のデフォルトディレクティブを DI 可能 | `DataFile` コンストラクタの `prepareDefaultDirectives("defaultDirectives")` | 未記載 | **未反映** |

### 2.2 DataFileFragment（フィールド定義）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| フィールド長 `"-"` でオンデマンド計算（実データのバイト数で動的決定、最大長を保持） | `ONDEMAND_CALC_FIELD_SIZE` / `addValue()` | schema.json `$defs.field_def.properties.length` oneOf `const: "-"` | 反映済み |
| `"-"` 長フィールドの値はインポート時に改行コードと前後空白が除去される | `removeLineSeparatorWithTrim()` | 未記載 | **未反映** |
| 同一レコード種別内のフィールド名は重複不可（重複で `IllegalArgumentException`） | `setNames()` の重複チェック | 未記載 | **未反映** |
| `dataTypeMapping_{エンコーディング名}` → `dataTypeMapping` → `BasicDataTypeMapping` の優先順でマッピングを取得 | `convertToFrameworkExpression()` | design.md §5 に基本は記載済み。文字コード別の優先検索は未記載 | **一部未反映** |
| `TEST_` プレフィクス型シンボルが存在する場合、`TEST_X` 等が自動優先選択される | `getTypeForTest()` | schema.json `$defs.field_def.properties.type` pattern 許容のみ。動作説明なし | **未反映** |

### 2.3 BasicDataTypeMapping（設計書記法マッピング）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| デフォルトマッピング21種（半角英字→X, 全角→N, 数値→Z, 符号付パック10進数→SP, バイナリ→B 等） | `BasicDataTypeMapping` の static 初期化 | design.md §5（YAMLではフレームワーク型記号を直接書く旨の記載あり） | 反映済み |
| `setMappingTable()` でカスタム全置換可能 | `setMappingTable()` | schema.json `$defs.field_def.properties.type` description | 反映済み |
| 未知の型記号は `IllegalArgumentException`（identity mapping なし） | `convertToFrameworkExpression()` | 未記載 | **未反映** |

### 2.4 LineSeparator（record-separator有効値）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `NONE`（空文字）/ `CR`（`\r`）/ `LF`（`\n`）/ `CRLF`（`\r\n`）の4シンボルが有効 | `LineSeparator` enum | schema.json `directives.record-separator` description | 反映済み |
| シンボル名以外の文字列はリテラルとして使用可能 | `evaluate()` | schema.json / examples.yaml で `"\r\n"` 使用例あり | 反映済み |

---

## 3. messaging パッケージ

### 3.1 RequestTestingMessagingClient（4セクション役割）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 4セクション（`EXPECTED_REQUEST_HEADER_MESSAGES`, `EXPECTED_REQUEST_BODY_MESSAGES`, `RESPONSE_HEADER_MESSAGES`, `RESPONSE_BODY_MESSAGES`）の役割と相互関係 | `sendSync()` 実装 | schema.json 各セクション description | 反映済み |
| `SystemRepository["messaging.assertAsMapFileType"]` でアサート方式（DataRecord vs 文字列）を切り替え可能 | `isAssertAsMap()` | 未記載 | **未反映** |
| 送信電文フォーマット定義ファイル命名規則: `{requestId}_SEND`, 応答電文: `{requestId}_RECEIVE` | `requestMessageFormatFileNamePattern` / `responseMessageFormatFileNamePattern` | 未記載 | **未反映** |

### 3.2 SendSyncSupport（ファイル配置規則）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| テストデータ配置: `FilePathSetting["sendSyncTestData"]` ベースパス配下の `{requestId}/message` シート | `SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData"` / `RESPONSE_MESSAGES_SHEET_NAME = "message"` | 未記載 | **未反映** |
| 呼び出し順にレコードを消費するキャッシュ機構（ファイルタイムスタンプ変化で無効化） | `fileCache` / `no` カウンタ | 未記載 | **未反映**（YAMLアダプタ実装時注意） |

---

## 4. db パッケージ

### 4.1 TableData（テーブルデータ）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| テーブル名・カラム名は `toUpperCase()` で正規化 | `setTableName()` / `setColumnNames()` | design.md §変換ツール方針 | 反映済み |
| 日付型カラムのデフォルトフォーマットは `yyyyMMddHHmmssSSS` | `DEFAULT_DATE_FORMAT` 定数 | examples.yaml コメント | 反映済み |
| 日付文字列が17文字未満でも後置0埋めで処理される（例: `"20240101"` も有効） | `asYyyyMMddHHmmssSSS()` の 後置`"00000000000000000"` | 未記載 | **未反映** |
| JDBC タイムスタンプエスケープ形式（`"2024-01-01"` / `"2024-01-01 12:00:00.000"`）も日付型カラムに記述可能 | `isJdbcTimestampFormat()` （5文字目が `-`） | 未記載 | **未反映** |
| `SETUP_TABLE` / `EXPECTED_TABLE` でも省略カラム（キーなし）には `DefaultValues` でデフォルト値が補完されて INSERT | `convert()` の `getDefaultValue()` | 未記載 | **未反映** |
| `EXPECTED_COMPLETE_TABLE` 専用の `fillDefaultValues()`: DB全カラムから省略カラムを `BasicDefaultValues` で補完 | `fillDefaultValues()` | design.md §4 | 反映済み |
| `BasicDefaultValues` のデフォルト値一覧（数値=`0`、文字列=スペース、日付=epoch、Boolean=`false`等） | `BasicDefaultValues.java` | 未記載 | **未反映** |

---

## 5. interpreter / generator パッケージ

### 5.1 NullInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `"null"`（大文字小文字不問）を Java `null` へ変換（`equalsIgnoreCase`） | `NullInterpreter#interpret()` | design.md §7 | 反映済み |
| `"NULL"`, `"Null"`, `"null"` すべて null に変換される（大文字小文字無視） | `equalsIgnoreCase` | 大文字小文字無視の旨が未記載 | **未反映** |

### 5.2 QuotationTrimmer

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 半角ダブルクォート（`"..."`)で囲まれた値の前後1文字を除去 | `QuotationTrimmer#interpret()` | design.md §7 `'"null"'` の例 | 反映済み |
| 全角ダブルクォート（`"..."` U+201C/U+201D）でも同様に前後1文字を除去 | `isQuotation()` が `“` / `”` を含む | 全角の場合が未記載 | **未反映** |

### 5.3 DateTimeInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `${systemTime}` → JDBC タイムスタンプ書式の現在時刻 | `dateTimeTable.put("${systemTime}", ...)` | design.md §7 / AI向けプロンプト | 反映済み |
| `${updateTime}` → `${systemTime}` と同値 | `dateTimeTable.put("${updateTime}", ...)` | design.md §7 / AI向けプロンプト | 反映済み |
| `${setUpTime}` → DB セットアップ時刻（JDBC タイムスタンプ書式で設定が必要） | `setSetUpDateTime()` | design.md §7 / AI向けプロンプト | 反映済み |
| 完全一致のみ変換（`"${systemTime}_suffix"` のような部分文字列は変換されない） | Map lookup による完全一致 | 未記載（CompositeInterpreter との組み合わせが必要な旨がない） | **未反映** |

### 5.4 LineSeparatorInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| デフォルトで `\\r`（バックスラッシュ+r の2文字）にマッチし CR（`\r`）へ置換 | `matchPattern = "\\\\r"` | design.md AI向けプロンプト / examples.yaml | 反映済み |
| `setMatchPattern()` / `setLineSeparator()` でカスタマイズ可能 | setter 定義 | 未記載 | **未反映**（拡張ポイント） |

### 5.5 BinaryFileInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `${binaryFile:相対パス}` をファイル内容の HexString に変換 | `BinaryFileInterpreter#interpret()` | design.md AI向けプロンプト / examples.yaml | 反映済み |
| ファイルパスは Excel ファイルのディレクトリを基準とした相対パス | `concat(path, '/', value)` | 未記載（YAML移行後の基準ディレクトリを明記する必要あり） | **未反映** |

### 5.6 BasicJapaneseCharacterInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `${文字種,文字数}` 記法でサポートされる文字種 14 種 | `BasicJapaneseCharacterGenerator#TYPE_CHARS_PAIRS` | design.md AI向けプロンプト / examples.yaml §文字種トークン | 反映済み |
| 未知トークンは `IllegalArgumentException`（「素通り」ではない） | `CharacterGeneratorBase#generate()` の例外 | design.md の「素通り」記述が**不正確**。要修正 | **不正確記述** |
| `setCharacterGenerator()` でカスタム文字生成クラスへ差し替え可能 | setter 定義 | 未記載 | **未反映**（拡張ポイント） |

### 5.7 CompositeInterpreter

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| 値中の `${...}` パターンを分解して個別解釈し結合（例: `${半角数字,4}-${半角数字,4}`） | `CompositeInterpreter#interpret()` | design.md AI向けプロンプト / examples.yaml | 反映済み |
| `${...}` が含まれない値は後続 Interpreter に委譲（`invokeNext()`） | 分岐ロジック | 未記載 | **未反映**（実用影響小） |
| `interpreters` プロパティに `TestDataInterpreter` のリストを DI する必要がある | `interpreters` フィールド | 未記載（利用者がコンポーネント設定方法を知る必要あり） | **未反映** |

### 5.8 JapaneseCharacterSet（文字種詳細）

| 仕様 | 根拠 | スキーマ対応 | 判定 |
|---|---|---|---|
| `${半角記号}` 生成では `"`, `#`, `,`, `\` が意図的に除外される | `JapaneseCharacterSet.ASCII_SYMBOL` の除外リスト | 未記載 | **未反映** |

---

## 6. 未反映仕様まとめ

### 6.1 schema.json への追加

| # | 追加箇所 | 追加内容 |
|---|---|---|
| S-1 | `$defs.directives.properties.record-length` description | `record-length` は固定長ファイルのフィールド長合計から自動計算されるため**通常は記述不要** |
| S-2 | `$defs.directives.properties.field-separator` description | `"\\t"` を指定するとタブ文字（U+0009）に変換される。値は1文字のみ有効（`"\\t"` 変換後1文字のため有効） |
| S-3 | `$defs.record_fragment.properties.fields` description | 同一レコード種別内のフィールド名は重複不可 |
| S-4 | `$defs.field_def.properties.length` description（const:"-" の説明部分） | `"-"` を指定したフィールドの値は格納時に改行コードと前後空白が除去される |
| S-5 | `$defs.table_data.properties.rows` description | `SETUP_TABLE` / `EXPECTED_TABLE` でも省略カラムには `DefaultValues` によるデフォルト値が INSERT 時に補完される |

### 6.2 design.md への追加

| # | 追加箇所 | 追加内容 |
|---|---|---|
| D-1 | §7 特殊値 null テーブル | `NullInterpreter` は大文字小文字不問（`"NULL"`, `"Null"` も null になる） |
| D-2 | §7 特殊値 QuotationTrimmer | 全角ダブルクォート（`"..."` U+201C/U+201D）での囲みでも外側クォートが除去される |
| D-3 | §7 または §4 | 日付型カラムは 17 文字未満でも後置 0 埋めで処理される（例: `"20240101"` も有効）。JDBC タイムスタンプエスケープ形式（`"2024-01-01"` 等）も受け付ける |
| D-4 | §4 `expected_complete_tables` の説明 | `BasicDefaultValues` のデフォルト値一覧を表形式で追記 |
| D-5 | §11 MESSAGE系 record_type 説明の近くに追記 | Excel 上の FW 制御ヘッダは「フィールド名\|値」の 2 列ディレクティブ行形式だったが YAML では通常の `fields` に統合される |
| D-6 | AI向けプロンプト §BasicJapaneseCharacterInterpreter | 「スペルミスは素通り」→「スペルミスは `IllegalArgumentException` がスローされる」に**修正** |
| D-7 | AI向けプロンプト §文字種トークン | `${半角記号}` 生成では `"`, `#`, `,`, `\` は含まれない |
| D-8 | AI向けプロンプト §field-separator 追加 | `"\\t"` でタブ区切りを指定できる |
| D-9 | 新節「デフォルトディレクティブの DI」 | SystemRepository キー `defaultDirectives`（全共通）、`fixedLengthDirectives`（固定長専用）、`variableLengthDirectives`（可変長専用）でデフォルトディレクティブを一括設定できる |

### 6.3 examples.yaml への追加

| # | 追加内容 |
|---|---|
| E-1 | `field-separator: "\\t"` を使ったタブ区切りファイルの directives 例 |
| E-2 | `type: B`（バイナリ型）の `field_def` 使用例（`${binaryFile:...}` との組み合わせ） |
| E-3 | JDBC タイムスタンプ形式の日付値の例（`"2024-01-01"` など） |
| E-4 | `response_*_messages` の通常データ行（errorMode なし）の例 |

---

## 8. P4-3 テストコード調査で補完された仕様

### 8.1 TableDataTest から

| 仕様 | 根拠（テストメソッド） | 判定 |
|---|---|---|
| BLOB フィールドで空文字 `""` を指定すると SQL NULL として格納される（Java null と同じ扱い） | `testSetValueBlobNull` / `testSetValueBlobEmpty` | **未反映** → D-4 に追記 |
| 数値列に指数表記 `"1.E-1"` を指定可能 → `BigDecimal("0.1")` として格納される | `testPutLargeScaleDecimalValue` | **未反映** → examples.yaml コメントへ |
| タイムスタンプ短縮形式: `"yyyyMMdd HHmmss"`（14文字、スペース区切り）および `"yyyyMMddHHmmssS"`（15文字、ミリ秒1桁）も有効 | `testTimestamp` | **未反映** → D-3 精度向上 |
| `BasicDefaultValues` の DATE/TIMESTAMP デフォルト値は JST エポック `"1970-01-01 09:00:00.0"`（UTC ではない） | `testFillDefaultValues` | **未反映** → D-4 に追記 |
| 数値文字列 `"01"` は Long 型として `1L` に変換されて格納される（ゼロパディング文字列は数値として解釈） | `testGetSetupTableData` | **未反映** → design.md 注意事項 |

### 8.2 BasicDefaultValuesTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| `CHAR`/`NCHAR` のデフォルト値はカラム長分のスペース（n文字）; `VARCHAR`/`NVARCHAR` のデフォルトは常に1文字スペース（長さによらず固定） | `testGetDefault_charType` | **未反映** → D-4 を精緻化 |
| サポート外 SQL 型（`ARRAY`, `DATALINK`, `DISTINCT`, `JAVA_OBJECT`, `NULL`, `OTHER`, `REF`, `STRUCT`）は `UnsupportedOperationException` | `testUnsupportedTypes` | **未反映**（参考情報） |
| `setCharValue` は正確に1文字のみ有効（0文字・2文字以上・null はいずれも `IllegalArgumentException`） | `testSetCharValue_*` | **未反映** → schema.json description |

### 8.3 QuotationTrimmerTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| クォート除去は「先頭と末尾が同じクォート文字である場合のみ」適用される（片側のみはスルー） | `testInterpret_noQuotation_*` | **未反映** → D-2 に追記 |
| `""abc""` → `"abc"`（最外側の1層のみ除去、二重クォートは1回で解消） | `testInterpret_doubleQuoted` | **未反映** → examples.yaml NG/OK例 |

### 8.4 BasicDataTypeMappingTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| `"半角数字"` は `X`（バイナリ/文字型）にマッピングされる（`Z`＝ゾーン10進数ではない） | `testConvertToFrameworkExpression` | **未反映** → D-4 マッピング表に注記 |
| `convertToFrameworkExpression(null)` → `IllegalArgumentException` | `testConvertToFrameworkExpression_null` | **未反映** → schema/design 参考情報 |

### 8.5 BasicJapaneseCharacterInterpreterTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| 書式 `${...,...}` にマッチしない入力（例: `"解釈できない形式"`）はスルーされる（例外なし）。一方、書式はマッチするが文字種が未知（例: `${不明,10}`）は `IllegalArgumentException` | `testInterpret_*` | **未反映** → D-6 の修正文に「書式不一致はスルー、型ミスは例外」として追記 |

### 8.6 DateTimeInterpreterTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| `setSetUpDateTime()` に受け付けるフォーマットは `"yyyy-MM-dd HH:mm:ss.S"`（例: `"2010-12-13 12:34:56.0"`） | `testSetSetUpDateTime` | **未反映** → design.md §7 に補足 |
| `setSetUpDateTime("invalid argument")` → `IllegalArgumentException` | `testSetSetUpDateTime_invalid` | **未反映**（参考情報） |
| `FixedSystemTimeProvider` への日時指定フォーマットは `"yyyyMMddHHmmss"`（14文字） | テスト初期化部分 | **未反映**（参考情報） |
| 不明な `${...}` トークン（例: `${hoge}`）はそのままスルーされる（例外なし） | `testInterpret_unknown` | 反映済み（design.md §7） |

### 8.7 TestDataParsingTemplateTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| `getDataType(null)` は `DataType.DEFAULT` を返す（null はデフォルト扱い） | `testGetDataType_null` | **未反映**（YAML移行では不要） |
| `NullInterpreter` が先頭セルを null に変換した行は**コメント行として扱われない**（`isCommentRow()` は null セルを無視） | `testIsCommentRow_null` | **未反映** → design.md 注意事項 |
| `NullInterpreter` が先頭セルを null に変換したグループID行は `TableDataParser` で黙って捨てられる（0件返却） | `testTableDataParser_nullGroupId` | **未反映** → design.md 注意事項 |

### 8.8 VariableLengthFileParserTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| 可変長ファイルの空行は**スキップされない**。全フィールドが `""` のレコードとして保持される | `testReadEmpty*` | **未反映** → design.md §ファイル系 注意事項 |

### 8.9 SingleDataParsingTemplateTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| 同一シート内に同じ `LIST_MAP=id` セクションが複数存在する場合、**最初の1つのみ**が読まれる（後続は黙って無視） | `testParse_duplicate` | **未反映** → design.md §LIST_MAP の注意事項 |

### 8.10 BasicTestDataParserTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| `formatGroupId` には 0 または 1 個の引数のみ有効（2個以上で `IllegalArgumentException`） | `testFormatGroupId_*` | **未反映**（スキーマ利用者には直接影響なし） |
| 存在しない groupId を指定すると空リストが返る（例外なし） | `testGetTableData_notExist` | **未反映** → design.md §9 に補足 |

### 8.11 FileSupportTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| 固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される（0バイト行にはならない） | `testSetUpFixedFile_emptyRow` | **未反映** → design.md §ファイル系 注意事項 |
| `DataRecord` のエントリが0件のレコードは「空ファイル」とみなされる（空リストと同等） | `testIsEmptyFile_*` | **未反映** → design.md §ファイル系 参考情報 |

### 8.12 RequestTestingMessagingClientTest から

| 仕様 | 根拠 | 判定 |
|---|---|---|
| テストデータにステータスコード列がない場合、デフォルトで `"200"` が使用される | `testSendLessStatusCode` | **未反映** → design.md §11 messaging |
| EXPECTED_REQUEST_HEADER_MESSAGES と EXPECTED_REQUEST_BODY_MESSAGES の行数は一致が必須（不一致で `IllegalStateException`） | `testAssertSendingMessage_countMismatch` | **未反映** → design.md §11 注意事項 |

### 8.13 E-2: `"?"` プレフィックス記法の調査結果

`src/test/` 配下の全 Java/Excel/YAML/CSV ファイルを調査した結果、`"?"` プレフィックスのフィールド名は**一切存在しない**。

**結論**: `"?"` プレフィックス記法は `nablarch-testing` リポジトリには存在せず、実装例リポジトリ（`nablarch-example-batch-ntf-yaml` 等）固有の慣習と推定される。`nablarch-testing` の YAML スキーマには影響しない。E-2 タスクは「本リポジトリのスキーマへの反映不要」として完了とする。

---

## 6. 未反映仕様まとめ（P4-3 追記版）

### 6.1 schema.json への追加

| # | 追加箇所 | 追加内容 |
|---|---|---|
| S-1 | `$defs.directives.properties.record-length` description | `record-length` は固定長ファイルのフィールド長合計から自動計算されるため**通常は記述不要** |
| S-2 | `$defs.directives.properties.field-separator` description | `"\\t"` を指定するとタブ文字（U+0009）に変換される。値は1文字のみ有効（`"\\t"` 変換後1文字のため有効）。空文字・2文字以上は `IllegalArgumentException` |
| S-3 | `$defs.record_fragment.properties.fields` description | 同一レコード種別内のフィールド名は重複不可 |
| S-4 | `$defs.field_def.properties.length` description（const:"-" の説明部分） | `"-"` を指定したフィールドの値は格納時に改行コードと前後空白が除去される |
| S-5 | `$defs.table_data.properties.rows` description | `SETUP_TABLE` / `EXPECTED_TABLE` でも省略カラムには `DefaultValues` によるデフォルト値が INSERT 時に補完される |

### 6.2 design.md への追加

| # | 追加箇所 | 追加内容 |
|---|---|---|
| D-1 | §7 特殊値 null テーブル | `NullInterpreter` は大文字小文字不問（`"NULL"`, `"Null"` も null になる） |
| D-2 | §7 特殊値 QuotationTrimmer | 全角ダブルクォート（`"..."` U+201C/U+201D）での囲みでも外側クォートが除去される。クォート除去は先頭・末尾の両方が同じクォート文字の場合のみ適用（片側のみはスルー）。`""abc""` → `"abc"` |
| D-3 | §7 または §4 | 日付型カラムは 17 文字未満でも後置 0 埋めで処理される（例: `"20240101"` も有効）。JDBC タイムスタンプエスケープ形式（`"2024-01-01"` 等）も受け付ける。さらに `"yyyyMMdd HHmmss"`（スペース区切り14文字）および `"yyyyMMddHHmmssS"`（ミリ秒1桁15文字）も有効 |
| D-4 | §4 `expected_complete_tables` の説明 | `BasicDefaultValues` のデフォルト値一覧を表形式で追記。DATE のデフォルトは JST エポック `"1970-01-01 09:00:00.0"`（UTC ではない）。`CHAR`/`NCHAR` はカラム長分スペース、`VARCHAR`/`NVARCHAR` は常に1スペース。BLOB は 10 ゼロバイト HEX 固定。`"半角数字"` → `X`（Z ではない）を注記 |
| D-5 | §11 MESSAGE系 record_type 説明の近くに追記 | Excel 上の FW 制御ヘッダは「フィールド名\|値」の 2 列ディレクティブ行形式だったが YAML では通常の `fields` に統合される |
| D-6 | AI向けプロンプト §BasicJapaneseCharacterInterpreter | 「スペルミスは素通り」→「書式 `${...,...}` にマッチしない場合はスルー。書式はマッチするが文字種が未知の場合は `IllegalArgumentException` がスローされる」に**修正** |
| D-7 | AI向けプロンプト §文字種トークン | `${半角記号}` 生成では `"`, `#`, `,`, `\` は含まれない |
| D-8 | AI向けプロンプト §field-separator 追加 | `"\\t"` でタブ区切りを指定できる |
| D-9 | 新節「デフォルトディレクティブの DI」 | SystemRepository キー `defaultDirectives`（全共通）、`fixedLengthDirectives`（固定長専用）、`variableLengthDirectives`（可変長専用）でデフォルトディレクティブを一括設定できる |
| D-10 | §ファイル系 注意事項（新規追加） | 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される |
| D-11 | §LIST_MAP 注意事項 | 同一シート内に同じ `LIST_MAP=id` セクションが複数存在する場合、**最初の1つのみ**が読まれる（後続は黙って無視） |
| D-12 | §9 group_id の説明に補足 | 存在しない groupId を指定した場合は例外でなく空リストが返る |
| D-13 | §11 messaging に追補 | テストデータにステータスコード列がない場合デフォルト `"200"` が使用される。ヘッダ行数とボディ行数は一致が必須 |

### 6.3 examples.yaml への追加

| # | 追加内容 |
|---|---|
| E-1 | `field-separator: "\\t"` を使ったタブ区切りファイルの directives 例 |
| E-2 | `type: B`（バイナリ型）の `field_def` 使用例（`${binaryFile:...}` との組み合わせ） |
| E-3 | JDBC タイムスタンプ形式の日付値の例（`"2024-01-01"` など） |
| E-4 | `response_*_messages` の通常データ行（errorMode なし）の例 |

---

## 7. 影響度別優先度

| 優先度 | 未反映仕様 | 理由 |
|---|---|---|
| **高** | D-6（`BasicJapaneseCharacterInterpreter` の「素通り」記述が不正確） | 現在の記述が誤っており、ユーザーが誤動作を期待する |
| **高** | D-3（日付型カラムの短縮形/JDBCエスケープ形式） | テストデータ作成時によく使われる書き方 |
| **高** | D-4（`BasicDefaultValues` のデフォルト値一覧） | `expected_complete_tables` 利用時に必須の情報 |
| **高** | S-1（`record-length` 自動計算） | examples.yaml で手動設定例があり誤解を招く |
| **中** | S-2（`field-separator: "\\t"` タブ変換と1文字制約） | タブ区切りファイルは一般的なユースケース |
| **中** | S-5（省略カラムのデフォルト補完） | SETUP_TABLE でも補完されることを知らないと誤ったテストになる |
| **中** | D-7（`${半角記号}` の除外文字） | テストデータ生成で予期しない文字列になる |
| **低** | D-9（デフォルトディレクティブ DI） | 高度なカスタマイズポイント。実用ユーザーの多くは不要 |
| **低** | その他の拡張ポイント（TestDataConverter 等） | カスタム実装者向け情報 |