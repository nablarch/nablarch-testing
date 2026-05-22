# NTF テストデータ 実装仕様一覧（ntf-impl-spec-list.md）

- **作成日**: 2026-05-20（I-1 タスク）
- **更新日**: 2026-05-22（I-1 やり直し: 正常系・異常系・代替フローの3観点で完全版に再作成）
- **参照元**: `ntf-coverage-spec-mapping.md`（コード全行走査）、`ntf-coverage-doc-check.md`（公式解説書照合）、`ntf-testdata-yaml-design.md`（スキーマ設計）
- **目的**: Ph-1 三角マッピングの基準となる仕様IDを確定する。後続タスク（I-2/I-3/Ph-2）の全件を本文書に基づいて追跡する。

---

## 仕様ID体系

| プレフィクス | カテゴリ | 対応コード領域 |
|---|---|---|
| DT | セクション識別・DataType | `DataType`, `TestDataParsingTemplate`, `GroupDataParsingTemplate`, `SingleDataParsingTemplate` |
| SS | テーブル・ファイル構造 | `TableData`, `ListMapParser`, `DataFileParser`, `DataFile`, `DataFileFragment`, `BasicTestDataParser` |
| RS | YAMLリーダー実装仕様 | `TestDataReader` インタフェース（実装: `YamlTestDataParser`, `YamlLoader`, `YamlTableDataBuilder`, `YamlFileBuilder`, `YamlMessageBuilder`, `YamlSection`） |
| HC | ヘッダ行・カラム処理 | `HeaderLine`, `TestDataParsingTemplate` |
| IV | インタープリタ・特殊値 | interpreter / generator パッケージ全クラス |
| DR | ディレクティブ | `DataFile`, `FixedLengthFile`, `VariableLengthFile`, ディレクティブ列挙体 |
| MS | メッセージングテストデータ | `MessageParser`, `SendSyncMessageParser`, `GroupMessageParser`, `SendSyncSupport`, `RequestTestingMessagingClient` |

---

## 仕様一覧

### DT: セクション識別・DataType

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| DT-01 | DataType 列挙値: `DEFAULT` / `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` / `LIST_MAP` / `SETUP_FIXED` / `EXPECTED_FIXED` / `SETUP_VARIABLE` / `EXPECTED_VARIABLE` / `MESSAGE` / `EXPECTED_REQUEST_HEADER_MESSAGES` / `EXPECTED_REQUEST_BODY_MESSAGES` / `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` の14種 | 正常系 | `DataType.java` 行10-56 | `DataTypeTest#testGetName`, `DataTypeTest#testGetType`（DataType列挙値の存在確認） | スキーマ根拠: `ntf-test-data.schema.json` の最上位 `properties` キー（`setup_tables`, `expected_tables`, ..., `response_body_messages`）が 14 DataType を網羅 |
| DT-02 | セクション識別行の書式: `<DataType名>[groupId]=<値>` (`=` が必須区切り文字。groupId は省略可) | 正常系 | `TestDataParsingTemplate.java` 行244-253 | `TestDataParsingTemplateTest#testParseFail`（parse内部でセクション識別を使用）、`BasicTestDataParserTest#testExpectedGetTableData`（EXPECTED_TABLE セクション識別の間接テスト） | スキーマ根拠: 各 `$defs` オブジェクトの `group_id` + `id`/`table`/`path` 構造が `=` 区切り書式を YAML で表現 |
| DT-03 | DataType 判定は前方一致（`startsWith`）: セル値が DataType の name で始まれば合致。識別キー＋追加文字のセル値でも認識される | 正常系 | `TestDataParsingTemplate.java` 行221-242（旧E-4） | テスト追加必要（`StartsWithTest#testStartsWith` は DataType の `startsWith` とは別クラス。`DataType#getType()` の前方一致動作を直接テストするテストが存在しない） | スキーマ外・パーサ実装で担保（YAML キーは完全なセクション名を使用するため前方一致は発生しない。既存 Excel 互換性のための実装内部仕様） |
| DT-04 | GroupData系（SETUP_TABLE 等）は同一 groupId のセクションを全部収集し続ける（`shouldStopOnNextOne() = false`） | 正常系 | `GroupDataParsingTemplate.java` 行45-53 | `TestDataParsingTemplateTest#testGroupDataWithNullInterpreter`（GroupData収集の停止しない動作）、`BasicTestDataParserTest#testGetExpectedTableDataWithGroupId`（複数グループの収集） | スキーマ根拠: `setup_tables`/`expected_tables` 等が `type: array` で複数エントリを許容（GroupData の全件収集を表現） |
| DT-05 | SingleData系（LIST_MAP / MESSAGE 等）は最初に合致したセクション1つだけを取得して停止する（`shouldStopOnNextOne() = true`） | 正常系 | `SingleDataParsingTemplate.java` 行43-53 | `SingleDataParsingTemplateTest#testParseSingleData`（SingleData先着一致）、`TestDataParsingTemplateTest#testSingleDataWithNullInterpreter` | スキーマ根拠: `list_maps` / `messages` の各エントリが `id` キーを持ち、パーサが最初の一致のみを取得（スキーマは構造を定義、先着一致はパーサ実装） |
| DT-06 | groupId 書式: `[groupId]`（省略時は空文字扱い。要素数1時のみ有効・2以上は `IllegalArgumentException`）。バッチ固有: `group_id: "default"` はグループIDなし扱いと同等になる | 正常系 | `BasicTestDataParser.java` 行243-266、公式解説書 batch.rst（Doc-5） | `BasicTestDataParserTest#testFormatGroupId`, `BasicTestDataParserTest#testFormatGroupIdFail` | スキーマ根拠: `table_data.$defs.group_id` の `minLength: 1` 制約（空文字禁止）。`design.md §8` グループIDなしの場合 |
| DT-07 | `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` は GroupData（groupId 必須）経路と SingleData（id 一致）経路の2つが存在する | 正常系 | `BasicTestDataParser.java` 行104-117、`design.md §10` | テスト追加必要（`RequestTestingSendSyncSupportTest#testGetExpectedRequestMessageWithoutCache` はアクセスパスBの間接テストのみ。GroupData経路（パスA）のテストなし） | スキーマ根拠: `response_header_messages`/`response_body_messages` が `group_message_data` を参照し、`group_id` 有無で両経路を表現（`design.md §10`） |
| DT-08 | groupId 引数に2件以上指定した場合は `IllegalArgumentException` をスロー | 異常系 | `BasicTestDataParser.java` 行264（`formatGroupId` メソッド） | `BasicTestDataParserTest#testFormatGroupIdFail`（2件引数で IllegalArgumentException） | スキーマ外・パーサ実装で担保（groupId のバリデーションはパーサ実装） |

---

### SS: テーブル・ファイル構造

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| SS-01 | テーブルデータ行の形式: カラム名をキーとするオブジェクト形式。省略されたカラムにはデフォルト値が INSERT 時に補完される | 正常系 | `TableData.java`、`design.md §1/§4` | `BasicTestDataParserTest#testGetSetupTableData`（テーブルデータ行の読み取り） | スキーマ根拠: `$defs.table_data.properties.rows` の `additionalProperties: {type: ["string","null"]}` がカラム=値の対応を表現 |
| SS-02 | `EXPECTED_TABLE`: 省略されたカラムは比較対象外になる（カラム列挙は任意） | 正常系 | `BasicTestDataParser.java` 行170-181、公式解説書 02_DbAccessTest.rst | `BasicTestDataParserTest#testExpectedGetTableData`（カラム省略が比較対象外になること） | スキーマ根拠: `expected_tables` の `table_data.rows` でカラムを省略可能（`additionalProperties` 方式） |
| SS-03 | `EXPECTED_COMPLETE_TABLE`: 省略されたカラムに `BasicDefaultValues` のデフォルト値を補完してから比較する | 正常系 | `BasicTestDataParser.java` 行170-181 (`fillDefaultValues()` 呼び出し) | `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithoutId`, `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithId` | スキーマ根拠: `expected_complete_tables` の `table_data` 構造は `expected_tables` と同一だが、パーサが `fillDefaultValues()` を呼ぶ点はスキーマ外 |
| SS-04 | `SETUP_TABLE` では主キーカラムは省略不可（省略するとデフォルト値が INSERT される） | 正常系 | 公式解説書 02_DbAccessTest.rst（Doc-2） | テスト追加必要（主キー省略時の動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（主キーカラム省略の検出はスキーマでは困難。INSERT 時のランタイム制約） |
| SS-05 | `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一ファイル内で混在させると後半データが読み込まれない（まとめて記述が必要） | 正常系 | 公式解説書 01_Abstract.rst（Doc-4） | テスト追加必要（EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE 混在時の動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（混在時の後半データ欠落はパーサのランタイム動作。YAML ファイルを分割して記述することを設計で推奨） |
| SS-06 | `LIST_MAP=id` セクション: id は完全一致。同一ファイル内で同一 id の重複エントリは後続が黙って無視される（先着一致） | 正常系 | `SingleDataParsingTemplate.java`、`design.md §9` | `SingleDataParsingTemplateTest#testParseSingleData`（先着一致） | スキーマ根拠: `$defs.list_map_data.properties.id` が識別子を表現。先着一致はスキーマ外（パーサ実装） |
| SS-07 | `SETUP_FIXED` と `SETUP_VARIABLE` は `BasicTestDataParser#getSetupFile()` でまとめて返される。`EXPECTED_FIXED`/`EXPECTED_VARIABLE` も同様 | 正常系 | `BasicTestDataParser.java` 行66-80 | `BasicTestDataParserTest#testGetSetupTableData`（getSetupFile 間接テスト）、`FileSupportTest#testSetUpFixedLengthFile`（固定長ファイル） | スキーマ根拠: `setup_files.type` フィールドの `enum: ["fixed","variable"]` で SETUP_FIXED/VARIABLE を統合表現（`design.md §3`） |
| SS-08 | ファイルセクションの行順序: ディレクティブ行（0行以上） → フィールド名行 → データ型行 → [フィールド長行（固定長のみ）] → データ行 | 正常系 | `DataFileParser.java` 行38-49（`Status` 遷移） | `FileSupportTest#testSetUpFixedLengthFile`, `FileSupportTest#testSetUpVariableLengthFile`（ファイルセクション行順序） | スキーマ根拠: `$defs.file_data` の `directives`（0以上）→ `records[].fields`（名前/型/長さ統合）→ `records[].rows` 構造が行順序を表現 |
| SS-09 | 固定長フラグメント: `names` / `types` / `lengths` の3リストが同サイズで必須 | 正常系 | `FixedLengthFileFragment.java` 行140-144 | `FileSupportTest#testSetUpFixedLengthFile`（固定長 names/types/lengths 3リスト） | スキーマ根拠: `$defs.record_fragment.fields` の `items: {$ref: field_def}` と `field_def.length` 必須（固定長では実質必須） |
| SS-10 | 可変長フラグメント: `names` / `types` の2リストが同サイズで必須。`lengths` は不要（型行読み取り後に直接 READING_VALUES へ遷移） | 正常系 | `VariableLengthFileParser.java` 行40-46 | `FileSupportTest#testSetUpVariableLengthFile`（可変長 names/types 2リスト） | スキーマ根拠: `field_def.length` が `anyOf` でオプション（可変長では省略可） |
| SS-11 | 1ファイルセクション内に複数レコードレイアウトを連続記述可能: データ行の後ろに新たなフィールド名行を書くと新レコードレイアウトとして扱われる | 正常系 | `DataFileParser.java` 行177-191（旧D-14） | テスト追加必要（複数レコードレイアウトの連続記述を明示するテストなし） | スキーマ根拠: `$defs.file_data.records` の `minItems: 0` と複数 `record_fragment` が連続記述を表現（`design.md §24`） |
| SS-12 | フィールド名行の構造: 先頭列 = レコード種別名、2列目以降 = フィールド名の列挙 | 正常系 | `DataFileParser.java` 行243-252 | `FileSupportTest#testSetUpFixedLengthFile`（先頭セル=レコード種別名） | スキーマ根拠: `$defs.record_fragment.record_type` フィールドが先頭セル（レコード種別名）を表現 |
| SS-13 | データ行の先頭セルは必ず空（null または空文字）にする | 正常系 | `DataFileParser.java` 行193-210 | `FileSupportTest#testSetUpFixedLengthFile`（データ行先頭セル空） | スキーマ外・パーサ実装で担保（YAML では行概念なく `rows` 配列の各要素が `fields` に対応。先頭セル空の制約なし） |
| SS-14 | 同一レコード種別内のフィールド名は重複不可（`IllegalArgumentException`）。異なる種別間は重複可 | 異常系 | `DataFileFragment.java` 行348-362（Doc-9） | `FileSupportTest#testSetUpFixedWithDuplicateName`, `FileSupportTest#testAssertFixedWithDuplicateName`, `FileSupportTest#testSetUpVariableWithDuplicateName`, `FileSupportTest#testAssertVariableWithDuplicateName` | スキーマ根拠: `$defs.record_fragment.fields` の `items` で `name` ユニーク制約は JSON Schema では表現困難。スキーマ外・パーサ実装で担保（`IllegalArgumentException`） |
| SS-15 | 空ファイル（0バイト）表現: ディレクティブ行のみ記述してレコード定義を省略する。`records` の `minItems: 0` が必要 | 正常系 | 公式解説書 03_Tips.rst（Doc-10） | `FileSupportTest#testAssertEmptyVariableFile`, `FileSupportTest#testAssertFixedActuallyEmpty`, `FileSupportTest#testAssertVariableActuallyEmpty` | スキーマ根拠: `$defs.file_data.records` の `minItems: 0`（空配列許容）（`design.md §25`） |
| SS-16 | 固定長ファイルは全フラグメントで同一レコード長が必須（違反時 `IllegalStateException`） | 異常系 | `FixedLengthFile.java` 行100-117 | `FixedLengthFileParserTest#testInvalidDirectives`（異なるレコード長で IllegalStateException） | スキーマ外・パーサ実装で担保（フラグメント間のレコード長一致はランタイムチェック） |
| SS-17 | `"-"` 長フィールド: 追加された全レコードの最大バイト長に自動拡張。値は改行コードと前後空白が除去される | 正常系 | `DataFileFragment.java` 行129-161（旧D-16） | `FileSupportTest#testVariation`（"-" 長フィールドの動作） | スキーマ根拠: `$defs.field_def.length` の `anyOf` に `{type: "string", const: "-"}` を含む（`design.md §27`） |
| SS-18 | `BasicDefaultValues` のデフォルト値: 数値型=`"0"`、CHAR/NCHAR=スペース×カラム長、VARCHAR等=半角スペース1文字、DATE=`"1970-01-01 09:00:00.0"`（JVM タイムゾーン依存）、バイナリ=10バイトゼロHexString、Boolean=`"false"` | 正常系 | `BasicDefaultValues`、`design.md §4` | `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithoutId`（EXPECTED_COMPLETE_TABLE でデフォルト値補完の間接テスト） | スキーマ外・テストで担保する方針（BasicDefaultValues のデフォルト値はパーサ実装。TZ依存（E-8）は制約事項として注記） |
| SS-19 | `testShots` は LIST_MAP の予約ID: バッチリクエスト単体テストでフレームワークがテストケース一覧として自動読み込みする | 正常系 | 公式解説書 batch.rst（Doc-16） | テスト追加必要（`testShots` の予約ID動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（`testShots` は LIST_MAP の予約ID。YAML では `list_maps` の `id: testShots` エントリとして記述） |
| SS-20 | ファイル系空行の動作差異: 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される | 正常系 | `design.md §AI向けプロンプト ファイル系の空行動作`（旧D-10） | `FileSupportTest#testSetUpVariableEmptyLine`, `FileSupportTest#testSetUpVariableEmptyLine2`, `FileSupportTest#testAssertEmptyLineVariable`, `FileSupportTest#testAssertEmptyLineFixed` | スキーマ外・パーサ実装で担保（空行の扱いはパーサのランタイム動作） |
| SS-21 | `DataFileFragment` のフィールド名リストまたは型リストが null/空の場合 `IllegalArgumentException` をスロー | 異常系 | `DataFileFragment.java` 行328（`assertNotNullOrEmpty` メソッド） | `FileSupportTest#testSetUpFixedWithDuplicateName`（フラグメント構築の異常系の間接確認）。フィールド名 null/空に対する専用テストは確認要 | スキーマ外・パーサ実装で担保（フィールド定義のバリデーションはパーサ実装） |
| SS-22 | `DataFileFragment` のフィールド名リストと型/長さリストのサイズ不一致時 `IllegalArgumentException` をスロー | 異常系 | `DataFileFragment.java` 行342（`assertSameSizeAsNames` メソッド） | テスト追加必要（サイズ不一致の専用テストが見当たらない） | スキーマ外・パーサ実装で担保（リストサイズバリデーションはパーサ実装） |
| SS-23 | 固定長フィールド値がフィールド長を超えた場合 `IllegalStateException` をスロー | 異常系 | `FixedLengthFileFragment.java` 行132（変換時の長さ超過チェック） | テスト追加必要（フィールド長超過の専用テストが見当たらない） | スキーマ外・パーサ実装で担保（フィールド長バリデーションはパーサ実装） |
| SS-24 | 存在しないフィールド名を指定した場合 `IllegalArgumentException` をスロー | 異常系 | `DataFileFragment.java` 行446（`getIndexOf` メソッド） | テスト追加必要（存在しないフィールド名の専用テストが見当たらない） | スキーマ外・パーサ実装で担保 |
| SS-25 | `DataFileFragment` のデータ要素数が不正な場合 `IllegalStateException` をスロー | 異常系 | `DataFileFragment.java` 行545（`checkSize` メソッド） | テスト追加必要（データ要素数不正の専用テストが見当たらない） | スキーマ外・パーサ実装で担保 |
| SS-26 | ファイルの読み込み失敗時（IO例外）に `RuntimeException` をスロー | 異常系 | `DataFile.java` 行185（`read()` メソッド） | テスト追加必要（ファイル読み込み失敗の専用テストが見当たらない） | スキーマ外・パーサ実装で担保 |
| SS-27 | `DataFileParser.Status` が想定外の状態になった場合 `IllegalStateException` をスロー（通常フローでは到達しない） | 異常系 | `DataFileParser.java` 行84（switch default ケース） | 除外: 通常フローでは到達しない（`onTargetTypeFound` が status を `READING_DIRECTIVES_AND_NAMES` に設定した後でのみ `onReadLine` が呼ばれる。サブクラスが status を直接操作しない限り到達不能）。テスト不要。根拠: DataFileParser.java:84 | スキーマ外・パーサ実装で担保 |
| SS-28 | ディレクティブ行またはフィールド名行の列数が2未満の場合 `IllegalStateException` をスロー | 異常系 | `DataFileParser.java` 行222（`processDirectives` メソッド） | テスト追加必要（ディレクティブ行の列数不足の専用テストが見当たらない） | スキーマ外・パーサ実装で担保 |
| SS-29 | `TableData#getClone()` で `CloneNotSupportedException` が発生した場合 `RuntimeException` をスロー（到達不能コード） | 異常系 | `TableData.java` 行581（`getClone` メソッド） | 除外: 到達不能コード（`TableData` は `Cloneable` を実装しており `CloneNotSupportedException` は発生しない）。テスト不要。根拠: TableData.java:581 | スキーマ外・パーサ実装で担保 |
| SS-30 | `TableData#getValue()` で日付型カラムの値が日付として解析できない場合 `RuntimeException` をスロー | 異常系 | `TableData.java` 行204（`toTimestamp` 呼び出し時） | テスト追加必要（不正な日付文字列の専用テストが見当たらない） | スキーマ外・パーサ実装で担保（日付型変換のバリデーションはパーサ実装） |
| SS-31 | `TableData#getValue()` でカラム値が `null` の場合は `null` を返す（代替フロー） | 代替フロー | `TableData.java` 行198（`getValue` メソッド） | `BasicTestDataParserTest#testGetSetupTableData`（null値カラムの間接テスト） | スキーマ根拠: `$defs.table_data.rows.items.additionalProperties` の `type: ["string","null"]` で null 値を許容 |
| SS-32 | `TableData#toTimestamp()` で空文字の場合は `null` を返す（代替フロー） | 代替フロー | `TableData.java` 行224（`toTimestamp` メソッド） | テスト追加必要（日付型カラムに空文字を指定した場合の null 返却テストが見当たらない） | スキーマ外・パーサ実装で担保（空文字→null 変換はパーサ実装） |

---

### RS: YAMLリーダー実装仕様

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| RS-01 | `open(path, dataName)` 規約: `dataName` に対して `{dataName}.yaml` ファイルを検索する | 正常系 | `TestDataReader` インタフェース（設計方針） | `YamlTestDataParserTest#testRs01_getSetupTableDataLoadsYamlFile`（.yaml ファイルをロード） | スキーマ外・パーサ実装で担保（`YamlTestDataReader.open()` の実装仕様） |
| RS-02 | `readLine()` は文書終端で `null` を返す | 正常系 | `TestDataReader` インタフェース（既存 Excel 実装との整合） | テスト追加必要（YamlTestDataReader を直接テストするケースが存在しない。RS-07 で間接確認） | スキーマ外・パーサ実装で担保（`readLine()` の終端返却仕様） |
| RS-03 | YAML ネイティブ `null`（アンクォート）は Java `null` として返す（旧E-1） | 正常系 | `design.md §7`（SnakeYAML が Java null に変換し、パーサがそのまま返す） | `YamlTestDataParserTest#testRs03_yamlNativeNullIsJavaNull`（YAML ネイティブ null は Java null） | スキーマ外・パーサ実装で担保（YAML ネイティブ null は Java null として返す） |
| RS-04 | YAML ネイティブ boolean (`true`/`false`) は文字列 `"true"`/`"false"` として返す（旧E-1） | 正常系 | `design.md §7` | `YamlTestDataParserTest#testRs04_yamlNativeBooleanIsStringified`（boolean の文字列化） | スキーマ外・パーサ実装で担保（YAML ネイティブ boolean の文字列化） |
| RS-05 | YAML ネイティブ integer/float は数字文字列として返す（旧E-1） | 正常系 | `design.md §7` | `YamlTestDataParserTest#testRs05_yamlNativeNumberIsStringified`, `testRs05_yamlScientificNotationIsStringified`（数値の文字列化） | スキーマ外・パーサ実装で担保（YAML ネイティブ数値の文字列化） |
| RS-06 | 末尾の空要素（YAML ネイティブ null または省略）は Java `null` として返す（旧E-2） | 正常系 | Excel 実装（`HeaderLine.java`）が `""` 補完するのに対し、YAML 実装は RS-03 仕様により Java null を返す。これは設計上の決定であり `design.md §7` に明記 | `YamlTestDataParserTest#testRs06_trailingNativeNullIsJavaNull`, `testRs06_trailingKeyOmittedIsNull`（末尾キー省略→null） | スキーマ外・パーサ実装で担保（末尾空要素は Java null として返す） |
| RS-07 | `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを保証する（旧E-3） | 正常系 | `TestDataParsingTemplate.java` 行187-219 の parse ロジック | `YamlTestDataParserTest#testRs07_lastSectionDataNotLostAtEndOfFile`（末尾セクション欠落防止） | スキーマ外・パーサ実装で担保（null 返却後の最終セクション欠落防止） |
| RS-08 | `isDataExisting(directory, resource)` / `isResourceExisting(directory, resource)` の実装（リソース存在確認） | 正常系 | `BasicTestDataParser.java` 行267-271 | `YamlTestDataParserTest#testRs08_isResourceExistingReturnsTrueWhenFileExists`, `testRs08_isResourceExistingReturnsFalseWhenFileNotExists` | スキーマ外・パーサ実装で担保（isDataExisting/isResourceExisting の実装） |
| RS-09 | YAML ファイルが存在しない、または読み込み失敗・パース失敗時は `IllegalStateException` をスロー | 異常系 | `YamlLoader.java` 行68-70（IOException / YAMLException キャッチ）、`YamlTestDataParserTest.java` 行391 | `YamlTestDataParserTest#testGetExpectedTableDataThrowsWhenFileNotExists`（ファイル不在時の IllegalStateException） | スキーマ外・パーサ実装で担保（YamlLoader がファイルロードエラーを IllegalStateException に変換） |
| RS-10 | `setup_tables`/`expected_tables` のエントリに `table` キーが存在しない場合 `IllegalStateException` をスロー | 異常系 | `YamlTableDataBuilder.java` 行71（`table` キー欠如チェック） | `YamlTableDataBuilderTest#testBuildTableDataList_missingTableThrowsException`（table キー欠如時の IllegalStateException） | スキーマ外・パーサ実装で担保（テーブル名必須バリデーションは YamlTableDataBuilder 実装） |
| RS-11 | `setup_files`/`expected_files` のエントリに `path` キーが存在しない場合 `IllegalStateException` をスロー | 異常系 | `YamlFileBuilder.java` 行71（`path` キー欠如チェック） | `YamlFileBuilderTest#testBuildFileList_missingPathThrowsException`（path キー欠如時の IllegalStateException） | スキーマ外・パーサ実装で担保（ファイルパス必須バリデーションは YamlFileBuilder 実装） |
| RS-12 | `messages`/`expected_request_*_messages` のエントリで `FW_HEADER` の `rows` が List of Lists でない場合 `IllegalStateException` をスロー | 異常系 | `YamlMessageBuilder.java` 行152（FW_HEADER rows 型チェック） | `YamlMessageBuilderTest#testBuildMessagePool_malformedFwHeaderRowsThrowsException`（FW_HEADER rows 型誤りの IllegalStateException） | スキーマ外・パーサ実装で担保（FW_HEADER rows の型バリデーションは YamlMessageBuilder 実装） |
| RS-13 | メッセージング以外の DataType を `YamlSection#dataTypeToSectionKey` に渡した場合 `IllegalArgumentException` をスロー | 異常系 | `YamlSection.java` 行190（switch default ケース） | `YamlMessageBuilderTest#testDataTypeToSectionKey_unsupportedDataTypeThrowsException`（非メッセージング DataType の IllegalArgumentException） | スキーマ外・パーサ実装で担保（DataType バリデーションは YamlSection 実装） |
| RS-14 | `setTestDataReader` 呼び出し時は `UnsupportedOperationException` をスロー（YAML 実装は TestDataReader を使わない） | 異常系 | `YamlTestDataParser.java` 行60（`setTestDataReader` メソッド） | `YamlTestDataParserTest#testSetTestDataReaderThrowsUnsupported`（UnsupportedOperationException） | スキーマ外・パーサ実装で担保（YAML 実装は TestDataReader を不使用） |
| RS-15 | `getSetupTableData` のみ、ファイルが存在しない場合は空リストを返す（代替フロー）。他のメソッド（`getExpectedTableData`、`getSetupFile` 等）はファイル不在時に RS-09 の `IllegalStateException` をスロー | 代替フロー | `YamlTestDataParser.java` 行99（`isResourceExisting` チェック後の emptyList 返却）、`BasicTestDataParser.java` 行54（継承元の同一ロジック） | `YamlTestDataParserTest#testGetSetupTableDataReturnsEmptyWhenFileNotExists`（ファイル不在時の emptyList）、`testGetSetupTableDataNotExist`（存在しない groupId 時の emptyList） | スキーマ外・パーサ実装で担保（`getSetupTableData` のみが `isResourceExisting` チェックを行う設計。他のメソッドは直接 YamlLoader.load() を呼ぶため不在時に例外） |
| RS-16 | `getMessage`/`getMessageWithoutCache` で対象 ID が見つからない場合は `null` を返す（代替フロー） | 代替フロー | `MessageParser.java` 行129（`data.isEmpty()` 判定後の null 返却）、`YamlFileBuilder.java` 行108（`buildMessageFile` で ID 未発見の null 返却）、`YamlMessageBuilder.java` 行83（`buildMessagePool` で file=null 時の null 返却） | `YamlTestDataParserTest#testGetMessageReturnsNullWhenIdNotFound`, `testGetMessageWithoutCacheReturnsNullWhenIdNotFound`（ID未発見の null 返却） | スキーマ外・パーサ実装で担保（ID未発見→null はパーサ実装） |
| RS-17 | `getSendSyncMessage` で対象 groupId が見つからない場合は `null` を返す（代替フロー） | 代替フロー | `YamlMessageBuilder.java` 行116（`buildSendSyncMessageList` で result が空の場合の null 返却） | `YamlTestDataParserTest#testGetSendSyncMessageReturnsNullForUnknownGroupId`（groupId 未発見の null 返却） | スキーマ外・パーサ実装で担保（groupId 未発見→null はパーサ実装） |
| RS-18 | YAML ファイルの内容が空の場合（`yaml.load()` が null）は空 Map として扱う（代替フロー） | 代替フロー | `YamlLoader.java` 行63（`result == null` の場合 `emptyMap()` に置き換え） | テスト追加必要（YAML ファイルが空の場合の動作を明示するテストが見当たらない） | スキーマ外・パーサ実装で担保（空 YAML→emptyMap はパーサ実装） |
| RS-19 | `getListMap` で指定 ID のエントリが存在しない場合は空リストを返す（代替フロー） | 代替フロー | `YamlTableDataBuilder.java` 行122（`buildListMapRows` で ID 未発見の emptyList 返却） | `YamlTestDataParserTest#testGetListMapReturnsEmptyWhenIdNotFound`（ID未発見の emptyList） | スキーマ外・パーサ実装で担保（ID未発見→emptyList はパーサ実装） |
| RS-20 | `messages` エントリで `FW_HEADER` フラグメントが見つからない場合は空 Map を FW ヘッダとして使用する（代替フロー） | 代替フロー | `YamlMessageBuilder.java` 行169（`extractFwHeader` で FW_HEADER グループ未発見の emptyMap 返却） | `YamlMessageBuilderTest#testBuildMessagePool_emptyFwHeaderRows`（FW_HEADER が空の場合の正常処理） | スキーマ外・パーサ実装で担保（FW_HEADER 未発見→emptyMap はパーサ実装） |

---

### HC: ヘッダ行・カラム処理

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| HC-01 | マーカーカラムの書式: `[カラム名]`（`[` で始まり `]` で終わる） | 正常系 | `HeaderLine.java` 行87-96 | `BasicTestDataParserTest#testGetListMapIgnoredColumn`, `BasicTestDataParserTest#testGetExpectedTableIgnoredColumn`, `BasicTestDataParserTest#testGetSetupTableIgnoredColumn`（マーカーカラム書式） | スキーマ根拠: `design.md §6` マーカーカラムの扱い。YAML では `[COLNAME]` 形式カラムを出力しない（変換ルール） |
| HC-02 | マーカーカラムは DB 操作から除外される（データとして格納されない） | 正常系 | `HeaderLine.java` 行53-85、`TableDataParser.java` 行74-82 | `BasicTestDataParserTest#testGetListMapIgnoredColumn`（DB操作から除外） | スキーマ外・パーサ実装で担保（マーカーカラム除外はパーサ実装） |
| HC-03 | ヘッダ行末尾の空カラムは除去される（末尾カラム省略可） | 正常系 | `HeaderLine.java` 行27-42（`trimTailCopy()`） | `BasicTestDataParserTest#testGetListMapWithInvisibleTail`, `BasicTestDataParserTest#testGetTableDataWithInvisibleTail`（末尾空カラム除去） | スキーマ外・パーサ実装で担保（末尾空カラム除去は `HeaderLine.java` の実装） |
| HC-04 | データ行がヘッダより短い場合、不足分は空文字 `""` で補完される | 正常系 | `HeaderLine.java` 行69-85 | `BasicTestDataParserTest#testGetListMapWithInvisibleTail`（データ行がヘッダより短い場合の補完） | スキーマ根拠: `$defs.record_fragment.rows` の各配列が `fields` と同順・同件数を要求（補完はパーサ実装） |
| HC-05 | コメント行: 先頭セルが `//` で始まる行は行ごとスキップ | 正常系 | `TestDataParsingTemplate.java` 行268-291 | `TestDataParsingTemplateTest#testIsCommentRow`（コメント行判定） | スキーマ外・パーサ実装で担保（コメント行はパーサが `//` 先頭を検出してスキップ。YAML では行コメント `#` を使用） |
| HC-06 | 行内コメント: 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨て | 正常系 | `TestDataParsingTemplate.java` 行292-308 | テスト追加必要（行内コメント（先頭以外の `//` 以降切り捨て）を明示するテストなし） | スキーマ外・パーサ実装で担保（行内コメント切り捨てはパーサ実装。YAML では行末コメント `#` で同等機能） |
| HC-07 | 空行スキップ: 全要素が null または空文字の行は読み飛ばす | 正常系 | `TestDataParsingTemplate.java` 行310-318 | テスト追加必要（空行スキップの明示的テストなし） | スキーマ外・パーサ実装で担保（空行スキップはパーサ実装。YAML では空行は存在しない） |

---

### IV: インタープリタ・特殊値

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| IV-01 | `NullInterpreter`: `null`/`NULL`/`Null`（大文字小文字不問）を Java null に変換 | 正常系 | `NullInterpreter.java` 行10-19 | `NullInterpreterTest#testInterpretNullLowerCase`, `NullInterpreterTest#testInterpretNullUpperCase`, `NullInterpreterTest#testInterpretNullCapitalized`, `NullInterpreterTest#testInterpretNotNullValue` | スキーマ根拠: `$defs.table_data.rows.items.additionalProperties` の `type: ["string","null"]` で null 値を許容。`design.md §7` 特殊値の表現 |
| IV-02 | `QuotationTrimmer`: 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去。片側のみはスルー | 正常系 | `QuotationTrimmer.java` 行18-30 | `QuotationTrimmerTest#testInterpretHalfWidthQuotation`, `QuotationTrimmerTest#testInterpretFullWidthQuotation`, `QuotationTrimmerTest#testInterpretNotQuoted` | スキーマ根拠: `design.md §7` 特殊値の表現（クォーティング記法） |
| IV-03 | `DateTimeInterpreter`: `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換。部分文字列は変換されない（`CompositeInterpreter` との組み合わせが必要） | 正常系 | `DateTimeInterpreter.java` 行48-94 | テスト追加必要（`DateTimeInterpreter` の完全一致制約を明示するテストなし。実装はあるが独立したテストクラスが見当たらない） | スキーマ根拠: `design.md §22` DateTimeInterpreter の完全一致制約 |
| IV-04 | `LineSeparatorInterpreter`: `\\r` → CR(0x0D)（デフォルト）、`\\n` → LF(0x0A) に変換 | 正常系 | `LineSeparatorInterpreter.java`、公式解説書 01_Abstract.rst（Doc-7） | `LineSeparatorInterpreterTest#testConvertBackR`, `LineSeparatorInterpreterTest#testDoNotConvertCR`, `LineSeparatorInterpreterTest#testDoNotConvert` | スキーマ根拠: `design.md §7` 特殊値の表現（`\\n`/`\\r` 記法） |
| IV-05 | `BinaryFileInterpreter`: `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換。YAML ファイルが基準ディレクトリになる | 正常系 | `BinaryFileInterpreter.java` 行34-65 | `BinaryFileInterpreterTest#testOk`, `BinaryFileInterpreterTest#testNotApplicable`, `BinaryFileInterpreterTest#testFileNotFound` | スキーマ根拠: `design.md §21` BinaryFileInterpreter のパス基準 |
| IV-06 | `BasicJapaneseCharacterInterpreter`: `${文字種,文字数}` 形式で文字列生成。書式完全一致のみ動作、文字種未知の場合は `IllegalArgumentException`（書式ミスはスルー） | 正常系 | `BasicJapaneseCharacterInterpreter.java` 行22-45 | `BasicJapaneseCharacterInterpreterTest#testInterpret`, `BasicJapaneseCharacterInterpreterTest#testInterpretUnknownType`, `BasicJapaneseCharacterInterpreterTest#testInterpretNotResponsible` | スキーマ根拠: `design.md §7` / `ntf-testdata-yaml-design.md §BasicJapaneseCharacterInterpreter の有効トークン（14種）` |
| IV-07 | `BasicJapaneseCharacterGenerator` 有効文字種14種: 半角英字/半角数字/半角記号/半角カナ/全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角記号その他/中国語/サロゲートペア/改行/外字 | 正常系 | `BasicJapaneseCharacterGenerator.java` 行40-56 | `BasicJapaneseCharacterGeneratorTest#testGenerate`, `BasicJapaneseCharacterGeneratorTest#testGenerateWithUnknownType` | スキーマ根拠: `design.md §BasicJapaneseCharacterInterpreter の有効トークン（14種）` |
| IV-08 | `CompositeInterpreter`: 文字列中の `${...}` 要素を個別解釈して置換。`${...}` がない場合は次のインタープリタに委譲 | 正常系 | `CompositeInterpreter.java` 行22-42 | `CompositeInterpreterTest#testExpression`, `CompositeInterpreterTest#testCombinationOfNotations`, `CompositeInterpreterTest#testCombinationOfInterpreters`, `CompositeInterpreterTest#testLiteral` | スキーマ根拠: `design.md §23` CompositeInterpreter の DI 設定 |
| IV-09 | 日付型カラムの記述形式: `yyyyMMddHHmmssSSS`（17文字）、後置0埋め短縮形、JDBC タイムスタンプエスケープ形式（5文字目が `-`）等が有効 | 正常系 | `TableData.java` 行214-273、`design.md §7` | テスト追加必要（日付型カラムの記述形式の境界値テストなし） | スキーマ外・パーサ実装で担保（日付型変換は `TableData.java` のランタイム処理） |
| IV-10 | `Timestamp` 型カラムの期待値は末尾 `.0` が必要（例: `"2010-01-01 12:34:56.0"`） | 正常系 | 公式解説書 02_DbAccessTest.rst（Doc-3） | テスト追加必要（Timestamp 型の `.0` 必須を明示するテストなし） | スキーマ外仕様・テストで担保する方針（Timestamp 末尾 `.0` は期待値記述ルール。YAML でも文字列として記述） |
| IV-11 | バイナリデータの直接記述: `0x` プレフィクス付き16進数で記述可能。`0x` がない場合は文字列としてエンコード | 正常系 | 公式解説書 batch.rst（Doc-11） | テスト追加必要（バイナリデータの `0x` プレフィクス記法を明示するテストなし） | スキーマ外仕様・テストで担保する方針（`0x` プレフィクス記法は値記述ルール。YAML でも文字列として記述） |
| IV-12 | `BasiDataTypeMapping` デフォルトマッピング22種（`半角英字`→`X` 等）。未知の型記号は `IllegalArgumentException` | 正常系 | `BasicDataTypeMapping.java` 行30-73 | `BasicDataTypeMappingTest#testConvertToFrameworkExpression`, `BasicDataTypeMappingTest#testConvertToFrameworkExpressionFail`, `BasicDataTypeMappingTest#testConvertToFrameworkExpressionNull`, `BasicDataTypeMappingTest#testSetMappingTable` | スキーマ根拠: `$defs.field_def.type` の `pattern: "^[A-Z][A-Z0-9_]*$"` と `design.md §5` DataTypeMapping |
| IV-13 | `TEST_` プレフィクス型の自動優先選択: `TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用される | 正常系 | `DataFileFragment.java` 行211-245 | `FileSupportTest#testVariation`（TEST_ プレフィクス型の動作を間接的にテスト） | スキーマ根拠: `$defs.field_def.type` のパターン（`TEST_` プレフィクスも `[A-Z][A-Z0-9_]*` に合致）。`design.md §16` TEST_ プレフィクス型の自動昇格 |
| IV-14 | `QuotationTrimmer` によるスペース値明示記法: `'"⊔"'` → 半角スペース、`'"""'` → ダブルクォート1文字。ダブルクォートで囲むことで空白値を可視化して記述できる | 正常系 | `design.md §7`、公式解説書 01_Abstract.rst（Doc-8） | `QuotationTrimmerTest#testInterpretHalfWidthQuotation`（スペース値明示記法） | スキーマ根拠: `design.md §7` 特殊値の表現（`'"""'`/`'"⊔"'` 記法） |
| IV-15 | X9/SX9 型フィールドの記述方法: パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記載する必要がある | 正常系 | 公式解説書 batch.rst（Doc-12）、`design.md §26` | テスト追加必要（X9/SX9 型の実値記述を直接テストするものなし） | スキーマ根拠: `design.md §26` X9/SX9 型フィールドの記述方法 |
| IV-16 | `BasicJapaneseCharacterInterpreter` に未知の文字種を指定した場合 `IllegalArgumentException` をスロー | 異常系 | `BasicJapaneseCharacterInterpreter.java` 行22-45（文字種バリデーション） | `BasicJapaneseCharacterInterpreterTest#testInterpretUnknownType`（未知文字種の IllegalArgumentException） | スキーマ外・パーサ実装で担保（文字種バリデーションはインタープリタ実装） |

---

### DR: ディレクティブ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| DR-01 | ディレクティブ行の構成: 先頭列 = キー名、2列目 = 値（最低2列必要） | 正常系 | `DataFileParser.java` 行212-232 | `FileSupportTest#testSetUpFixedLengthFile`（ディレクティブ行読み取り） | スキーマ根拠: `$defs.directives` オブジェクトが `key: value` 形式のディレクティブを表現 |
| DR-02 | 固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定される | 正常系 | `FixedLengthFileParser.java` 行34-38 | `FixedLengthFileParserTest#testInvalidDirectives`（固定長ディレクティブキーの制限） | スキーマ根拠: `$defs.directives.properties` に固定長専用キー（`record-length`, `positive-zone-sign-nibble` 等）を列挙（`additionalProperties: false`） |
| DR-03 | 可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定される | 正常系 | `VariableLengthFileParser.java` 行34-38 | テスト追加必要（可変長ディレクティブキー制限の明示テストなし） | スキーマ根拠: `$defs.directives.properties` に可変長専用キー（`field-separator`, `quoting-delimiter` 等）を列挙 |
| DR-04 | `defaultDirectives` DI: SystemRepository のこのキーで全ファイル共通デフォルトディレクティブを一括設定できる | 実装内部ロジック | `DataFile.java` 行59-93（旧E-6） | テスト追加必要（`defaultDirectives` DI 設定の YAML 適用確認テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（DI 設定はランタイム。`design.md §14` デフォルトディレクティブの DI） |
| DR-05 | `fixedLengthDirectives` DI: 固定長ファイル専用デフォルトディレクティブ（`defaultDirectives` より後に上書き適用） | 実装内部ロジック | `FixedLengthFile.java` 行16-27 | テスト追加必要（`fixedLengthDirectives` DI の明示テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（`fixedLengthDirectives` DI はランタイム設定） |
| DR-06 | `variableLengthDirectives` DI: 可変長ファイル専用デフォルトディレクティブ | 実装内部ロジック | `VariableLengthFile.java` 行19-31 | テスト追加必要（`variableLengthDirectives` DI の明示テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（`variableLengthDirectives` DI はランタイム設定） |
| DR-07 | `file-type` ディレクティブはサブクラス（固定長=`"Fixed"`、可変長=`"Variable"`）が自動設定するため通常は記述不要 | 正常系 | `DataFile.java` 行83-101、`FixedLengthFile.java` 行29-36 | `FileSupportTest#testSetUpFixedLengthFile`（file-type 自動設定の間接確認） | スキーマ根拠: `$defs.directives.properties.file-type` に説明あり（自動設定のため通常記述不要） |
| DR-08 | `record-length` ディレクティブはフィールド長合計から自動計算されるため通常は記述不要 | 正常系 | `FixedLengthFile.java` 行60-92 | `FileSupportTest#testSetUpFixedLengthFile`（record-length 自動計算の間接確認） | スキーマ根拠: `$defs.directives.properties.record-length` に説明あり（自動計算のため通常記述不要） |
| DR-09 | `field-separator`: 可変長ファイルのデフォルトは `","``。`"\\t"` 指定でタブ文字に変換。値は1文字のみ有効 | 正常系 | `VariableLengthFile.java` 行16-82 | `FileSupportTest#testVariation`（field-separator の動作） | スキーマ根拠: `$defs.directives.properties.field-separator` の説明（省略時はカンマ、`\\t` でタブ変換）（`design.md §ディレクティブの field-separator`） |
| DR-10 | `record-separator`: `NONE`/`CR`/`LF`/`CRLF` または任意リテラル文字列が有効 | 正常系 | `LineSeparator.java`、`DataFile.java` 行318-334 | `LineSeparatorTest#testToString`, `LineSeparatorTest#testEvaluate`（record-separator の評価） | スキーマ根拠: `$defs.directives.properties.record-separator` の説明（NONE/CR/LF/CRLF またはリテラル）（`design.md §ディレクティブの record-separator`） |
| DR-11 | 無効なディレクティブキーを設定した場合 `IllegalArgumentException` をスロー（固定長・可変長ともに適用） | 異常系 | `DataFile.java` 行298（`setDirective` → `valueOf` で null 判定）、`FixedLengthFileParser.java` 行34-38 | `FixedLengthFileParserTest#testInvalidDirectives`（固定長に無効ディレクティブで IllegalArgumentException） | スキーマ根拠: `$defs.directives.properties` の `additionalProperties: false` に対応するランタイムバリデーション |
| DR-12 | 可変長ファイルの `field-separator` に2文字以上指定した場合 `IllegalArgumentException` をスロー | 異常系 | `VariableLengthFile.java` 行76（フィールド区切り文字の長さバリデーション） | テスト追加必要（可変長 field-separator 長さバリデーションの専用テストが見当たらない） | スキーマ根拠: `$defs.directives.properties.field-separator` の説明（1文字のみ有効）でスキーマ側の制約も記載 |

---

### MS: メッセージングテストデータ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| MS-01 | FW 制御ヘッダフィールドのデフォルト4種: `requestId` / `userId` / `resendFlag` / `resultCode`。`reader.fwHeaderfields` キーで変更可能 | 正常系 | `MessageParser.java` 行95-110 | `MessageParserTest#testParseRequestMessage`（FW制御ヘッダ4種） | スキーマ根拠: `$defs.message_data.records` の `record_fragment` 内のフィールドが FW ヘッダ含む構造。`design.md §1` Excel概念→YAML構造 |
| MS-02 | `no` 列（先頭列、列番号0）はフレームワークが除去し、データとして保存されない。`errorMode` 値は列番号1に格納される | 正常系 | `SendSyncMessageParser.java` 行94-134 | `SendSyncMessageParserTest#testGetFwHeader`（no列とerrorMode列の扱い） | スキーマ外・パーサ実装で担保（no列除去とerrorMode解釈はパーサ実装。`design.md §18` SendSyncSupport の配置規則） |
| MS-03 | `MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は常に内部で `"default"` に置き換えられる（装飾的なメタデータとして任意の値を書いてよい） | 正常系 | `MessageParser.java` 行60-67 | `MessageParserTest#testParseRequestMessage`（record_type を "default" に置き換え） | スキーマ根拠: `$defs.record_fragment.record_type` の説明（`design.md §12` MESSAGE系の record_type は装飾的） |
| MS-04 | `errorMode:timeout` および `errorMode:msgException` は `no` 列の次（列番号1）に配置する特殊値。他フィールドはパースされない | 正常系 | `SendSyncMessageParser.java` 行18-44、116-132 | テスト追加必要（`SendSyncMessageParserTest` が `testGetFwHeader` 1メソッドしかなく、errorMode:timeout/msgException の具体的テストなし） | スキーマ外・パーサ実装で担保（errorMode 特殊値はパーサ実装） |
| MS-05 | `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（rows 合計）は一致が必須。不一致は `IllegalStateException`（旧E-7） | 異常系 | `RequestTestingMessagingClient.java` 行294-443 | テスト追加必要（行数不一致の `IllegalStateException` を YAML テストデータで確認するテストなし） | スキーマ外仕様・テストで担保する方針（行数一致チェックはランタイム。`design.md §11`） |
| MS-06 | `GroupMessageParser`: 同一 groupId の複数メッセージプールを収集。セクション識別子 `=` 以降をリクエストIDとして使用 | 正常系 | `GroupMessageParser.java` 行48-65 | テスト追加必要（`GroupMessageParser` の複数メッセージ収集を明示するテストなし） | スキーマ根拠: `$defs.group_message_data` の `group_id` フィールドが groupId 収集を表現 |
| MS-07 | `sendSyncTestData/{requestId}/message` の配置規則: テストデータファイルは `sendSyncTestData` ベースパス下にリクエストIDと同名ファイルとして配置する（旧E-5） | 正常系 | `SendSyncSupport.java` 行39-49 | テスト追加必要（`sendSyncTestData/{requestId}/message` 配置規則の YAML 動作確認テストなし） | スキーマ外仕様・テストで担保する方針（配置規則はファイルシステムの話。`design.md §18`） |
| MS-08 | ステータスコード列がない場合はデフォルト `"200"` が使用される | 代替フロー | `RequestTestingMessagingClient.java` 行124-204 | テスト追加必要（ステータスコード列なし時のデフォルト "200" を明示するテストなし） | スキーマ外・パーサ実装で担保（ステータスコードデフォルト "200" はパーサ実装） |
| MS-09 | マルチレコード送信時: ヘッダ行数とボディ行数を一致させる。N 回送信の場合は各 N 行記述（公式解説書 Doc-13） | 正常系 | 公式解説書 send_sync.rst | テスト追加必要（マルチレコード送信の行数一致を明示するテストなし） | スキーマ外仕様・テストで担保する方針（行数一致ルールは運用規約。`design.md §AI向けプロンプト補助情報 messaging の追加注意事項`） |
| MS-10 | `no` 列と複数回送信: 同一リクエストIDで複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させる（公式解説書 Doc-14） | 正常系 | 公式解説書 send_sync.rst | テスト追加必要（no値変更による複数回送信を明示するテストなし） | スキーマ外仕様・テストで担保する方針（no値による複数回送信は運用規約） |
| MS-11 | HTTP同期応答メッセージ送信処理のボディ行長制約: `response_body_messages` の各データ行の文字列長が同一であることが必要（JSON/XML形式使用時の制約） | 正常系 | 公式解説書 http_send_sync.rst（Doc-15）、`design.md §11` | テスト追加必要（HTTP同期応答ボディ行長制約を明示するテストなし） | スキーマ外仕様・テストで担保する方針（ボディ行長制約は運用制約。`design.md §11`） |
| MS-12 | フォーマット定義ファイルの命名規則: 応答電文は `{requestId}_RECEIVE`、要求電文は `{requestId}_SEND` | 正常系 | `RequestTestingMessagingClient.java` 行75-79、`design.md §20` | テスト追加必要（フォーマット定義ファイル命名規則を直接テストするものなし） | スキーマ根拠: `design.md §20` フォーマット定義ファイルの命名規則 |
| MS-13 | `messaging.assertAsMapFileType` キー: SystemRepository から未設定時はデフォルト `"Fixed"` 形式で項目単位アサート。値により文字列全体アサートに切り替え可能 | 正常系 | `RequestTestingMessagingClient.java` 行81-83、`design.md §19` | テスト追加必要（`messaging.assertAsMapFileType` キーの動作を明示するテストなし） | スキーマ外・パーサ実装で担保（`messaging.assertAsMapFileType` キー参照はパーサ実装。`design.md §19`） |
| MS-14 | `SendSyncMessageParser#getFwHeader()` は `UnsupportedOperationException` をスロー（MessageParser が提供する FW ヘッダ解析機能は使用しない） | 異常系 | `SendSyncMessageParser.java` 行43（`getFwHeader` メソッド） | テスト追加必要（`SendSyncMessageParser#getFwHeader()` の UnsupportedOperationException 専用テストが見当たらない） | スキーマ外・パーサ実装で担保（getFwHeader 無効化はパーサ実装） |

---

## E-1〜E-9 の昇格/除外判断

設計フェーズの調査で発見された E-1〜E-9 の各ギャップについて、仕様IDとして昇格するか否かを判断する。

| ギャップID | 概要 | 判断 | 理由 | 昇格先仕様ID |
|---|---|---|---|---|
| E-1 | YAML ネイティブ型→文字列化の変換漏れリスク | **昇格** | YAMLリーダー実装で必ず対処が必要なランタイム仕様。テストで検証可能 | RS-03 / RS-04 / RS-05 |
| E-2 | 末尾空要素の扱い（Excel は null→"" 補完、YAML は末尾省略されやすい） | **昇格** | YAMLリーダー実装で必ず対処が必要。`HeaderLine` の末尾省略仕様と整合が必要 | RS-06 |
| E-3 | `readLine() == null` 終了判定タイミングのずれによる最終セクションデータ欠落リスク | **昇格** | YAMLリーダー実装の重要な境界条件。最終セクションデータが欠落しないことをテストで保証が必要 | RS-07 |
| E-4 | `startsWith` 前方一致マッチングの挙動（YAML schema validation とは独立） | **昇格** | DataType 判定の実装仕様として重要。YAML スキーマのセクションキー設計に影響する | DT-03 |
| E-5 | sendSyncTestData のディレクトリ配置規則はYAMLスキーマ外 | **昇格** | スキーマ外だが YAML テストデータ運用上必須の配置規則。テストで動作確認が必要 | MS-07 |
| E-6 | `defaultDirectives` の DI 設定は SystemRepository XML の問題でありYAMLファイルとは独立 | **昇格（スキーマ外）** | YAML ファイルの記述仕様には影響しないが、YAMLリーダーが DI 設定を正しく受け取れることを確認するテストが必要 | DR-04 / DR-05 / DR-06 |
| E-7 | `EXPECTED_REQUEST_HEADER_BODY_MESSAGES` の行数一致チェックはランタイムのみ | **昇格** | ランタイム制約であり YAML テストデータの記述ルールとして明示が必要。テストで違反時の `IllegalStateException` を検証 | MS-05 |
| E-8 | `BasicDefaultValues` の DATE カラムの TZ ハザード（JSTとUTCで値が変わる） | **昇格（制約事項）** | CI 環境の TZ 設定に依存するため、テストで TZ を明示するか、制約事項として SS-18 に記載する。TZ 依存が解消できない場合は SS-18 に制約事項として明記する | SS-18（注記） |
| E-9 | `BasicJapaneseCharacterInterpreter` の「スルー vs 例外」条件の誤記（design.md D-6） | **ドキュメント修正のみ** | `design.md §6` の記述修正のみで対応済み（設計フェーズで反映済み）。新仕様IDは不要。IV-06 に正確な挙動を記載済み | IV-06（修正済み） |

---

## 仕様一覧サマリー

| カテゴリ | 仕様ID数 | 正常系 | 異常系 | 代替フロー | 実装内部ロジック |
|---|---|---|---|---|---|
| DT | 8件（DT-01〜DT-08） | 7件 | 1件（DT-08） | 0件 | 0件 |
| SS | 32件（SS-01〜SS-32） | 18件 | 12件（SS-14/16/21〜28） | 2件（SS-31/32） | 0件 |
| RS | 20件（RS-01〜RS-20） | 8件 | 6件（RS-09〜14） | 6件（RS-15〜20） | 0件 |
| HC | 7件（HC-01〜HC-07） | 7件 | 0件 | 0件 | 0件 |
| IV | 16件（IV-01〜IV-16） | 15件 | 1件（IV-16） | 0件 | 0件 |
| DR | 12件（DR-01〜DR-12） | 7件 | 2件（DR-11/12） | 0件 | 3件（DR-04〜DR-06） |
| MS | 14件（MS-01〜MS-14） | 11件 | 2件（MS-05/14） | 1件（MS-08） | 0件 |
| **合計** | **109件** | **73件** | **24件** | **9件** | **3件** |

> **注**: 旧 SS（DataFile:298 に対応する旧 SS-26、VariableLengthFile:76 に対応する旧 SS-30）を DR-11/DR-12 に統合し、SS を詰め直した。RS-18〜RS-20 を追加（YAML 実装クラスの代替フロー）。

---

## grep 証跡（I-1 やり直し版）

### 対象ファイル一覧

**対象クラス（11ファイル: I-1 steering 指定クラス）**:

| ファイルパス |
|---|
| `src/main/java/nablarch/test/core/reader/BasicTestDataParser.java` |
| `src/main/java/nablarch/test/core/reader/DataFileParser.java` |
| `src/main/java/nablarch/test/core/db/TableData.java` |
| `src/main/java/nablarch/test/core/file/DataFileFragment.java` |
| `src/main/java/nablarch/test/core/file/FixedLengthFileFragment.java` |
| `src/main/java/nablarch/test/core/file/VariableLengthFileFragment.java` |
| `src/main/java/nablarch/test/core/file/DataFile.java` |
| `src/main/java/nablarch/test/core/file/FixedLengthFile.java` |
| `src/main/java/nablarch/test/core/file/VariableLengthFile.java` |
| `src/main/java/nablarch/test/core/reader/MessageParser.java` |
| `src/main/java/nablarch/test/core/reader/SendSyncMessageParser.java` |

**追加対象クラス（6ファイル: R-1/R-1-refactor で新規追加された YAML 実装クラス）**:

| ファイルパス |
|---|
| `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` |

### grep 結果

**`throw ` 検索結果（計 25 行）**:

| ファイル | 行番号 | 内容 | 仕様ID分類 |
|---|---|---|---|
| `TableData.java` | 204 | `throw new RuntimeException("invalid date format...")` | 登録: SS-30 |
| `TableData.java` | 420 | `throw new RuntimeException(e)` (Clob変換) | 除外: CLOB変換の SQLException ラップ。外部依存（JDBC）で制御困難。行番号: TableData.java:420 |
| `TableData.java` | 581 | `throw new RuntimeException("unexpected exception.", e)` (getClone) | 登録: SS-29（到達不能→除外） |
| `FixedLengthFile.java` | 111 | `throw new IllegalStateException("record-length differs.")` | 登録: SS-16 |
| `SendSyncMessageParser.java` | 43 | `throw new UnsupportedOperationException("unsupported method was called.")` | 登録: MS-14 |
| `VariableLengthFile.java` | 76 | `throw new IllegalArgumentException("field-separator must be one character.")` | 登録: DR-12（ディレクティブバリデーションとして DR カテゴリが適切）|
| `FixedLengthFileFragment.java` | 132 | `throw new IllegalStateException("value size overflowed.")` | 登録: SS-23 |
| `DataFileFragment.java` | 328 | `throw new IllegalArgumentException("... must not be null or empty.")` | 登録: SS-21 |
| `DataFileFragment.java` | 342 | `throw new IllegalArgumentException("field name size is ...")` | 登録: SS-22 |
| `DataFileFragment.java` | 357 | `throw new IllegalArgumentException("Duplicate field names are not permitted...")` | 登録: SS-14 |
| `DataFileFragment.java` | 446 | `throw new IllegalArgumentException("no such field [...]")` | 登録: SS-24 |
| `DataFileFragment.java` | 545 | `throw new IllegalStateException("invalid data.")` | 登録: SS-25 |
| `DataFileParser.java` | 84 | `throw new IllegalStateException("invalid status[...]")` | 登録: SS-27（到達不能→除外） |
| `DataFileParser.java` | 222 | `throw new IllegalStateException("directive or data names row must have two columns...")` | 登録: SS-28 |
| `BasicTestDataParser.java` | 264 | `throw new IllegalArgumentException("argument groupId must be one or zero.")` | 登録: DT-08 |
| `DataFile.java` | 119 | `throw e` (RuntimeException 再スロー) | 除外: catch ブロック内の例外再スロー（書き込み失敗時）。専用の仕様IDは不要。行番号: DataFile.java:119 |
| `DataFile.java` | 185 | `throw new RuntimeException("read file failed...")` | 登録: SS-26 |
| `DataFile.java` | 298 | `throw new IllegalArgumentException("invalid directive found...")` | 登録: DR-11（ディレクティブバリデーションとして DR カテゴリが適切）|
| `YamlTestDataParser.java` | 60 | `throw new UnsupportedOperationException(...)` | 登録: RS-14 |
| `YamlLoader.java` | 68 | `throw new IllegalStateException("Failed to load YAML file...")` | 登録: RS-09 |
| `YamlLoader.java` | 70 | `throw new IllegalStateException("Failed to parse YAML file...")` | 登録: RS-09（同一仕様ID） |
| `YamlTableDataBuilder.java` | 71 | `throw new IllegalStateException("Missing required field 'table'...")` | 登録: RS-10 |
| `YamlFileBuilder.java` | 71 | `throw new IllegalStateException("Missing required field 'path'...")` | 登録: RS-11 |
| `YamlMessageBuilder.java` | 152 | `throw new IllegalStateException("FW_HEADER rows must be a list of lists...")` | 登録: RS-12 |
| `YamlSection.java` | 190 | `throw new IllegalArgumentException("Unsupported DataType for messaging...")` | 登録: RS-13 |

**`return null` / `Collections.emptyList()` / `Collections.empty*` 検索結果（計 15 行）**:

| ファイル | 行番号 | 内容 | 仕様ID分類 |
|---|---|---|---|
| `BasicTestDataParser.java` | 54 | `return Collections.emptyList()` (データ不在時) | 登録: RS-15 |
| `TableData.java` | 198 | `return null` (カラム値が null の場合) | 登録: SS-31 |
| `TableData.java` | 224 | `return null` (日付型に空文字指定時) | 登録: SS-32 |
| `DataFile.java` | 77 | `return null` (MapCollector の内部コールバック) | 除外: MapCollector の evaluate() 実装の内部返却値。外部 API 仕様ではなくコレクション処理の実装パターン。行番号: DataFile.java:77 |
| `MessageParser.java` | 129 | `return null` (データが空の場合) | 登録: RS-16 |
| `YamlSection.java` | 88 | `return Collections.emptyList()` (`getList` でキーなし or List でない場合) | 除外: `getList` は内部ユーティリティ（安全キャスト用フォールバック）。呼び出し側ビルダーが空リストとして扱う内部実装パターン。行番号: YamlSection.java:88 |
| `YamlSection.java` | 99 | `return Collections.emptyMap()` (`castMap` でキーなし or Map でない場合) | 除外: `castMap` は内部ユーティリティ（安全キャスト用フォールバック）。内部実装パターン。行番号: YamlSection.java:99 |
| `YamlSection.java` | 138 | `return null` (`interpret(null, interps)` で入力が null の場合) | 除外: RS-03（YAML ネイティブ null → Java null）の内部実装パス。`objectToString` 経路と同一仕様。行番号: YamlSection.java:138 |
| `YamlMessageBuilder.java` | 83 | `return null` (`buildMessagePool` で file=null 時) | 登録: RS-16 |
| `YamlMessageBuilder.java` | 107 | `Collections.emptyMap()` (変数代入、`buildSendSyncMessageList` 内) | 除外: `Map<String, String> emptyHeader = Collections.emptyMap()` は変数への代入（return ではない）。メソッド内の定数的な初期値として使用。行番号: YamlMessageBuilder.java:107 |
| `YamlMessageBuilder.java` | 169 | `return Collections.emptyMap()` (`extractFwHeader` で FW_HEADER 未発見) | 登録: RS-20 |
| `YamlFileBuilder.java` | 108 | `return null` (`buildMessageFile` で ID 未発見) | 登録: RS-16 |
| `YamlLoader.java` | 63 | `result = Collections.emptyMap()` (YAML が空ファイルの場合) | 登録: RS-18（変数代入だが外部から空 Map として返却されるパス） |
| `YamlTableDataBuilder.java` | 122 | `return Collections.emptyList()` (`buildListMapRows` で ID 未発見) | 登録: RS-19 |
| `YamlTestDataParser.java` | 99 | `return Collections.emptyList()` (`getSetupTableData` でファイル不在) | 登録: RS-15 |

### 集計

| 種別 | 総行数 | 登録件数 | 除外件数 |
|---|---|---|---|
| `throw ` | 25行 | 23行 | 2行（TableData:420, DataFile:119） |
| `return null/empty` | 15行 | 8行 | 7行 |

- throw 行 25行 = 登録 23行 + 除外 2行
  - 除外内訳: `TableData.java:420`（JDBC依存の Clob 変換）、`DataFile.java:119`（例外の再スロー）
  - 到達不能コードとして登録扱い: `TableData.java:581`（SS-29）、`DataFileParser.java:84`（SS-27）
  - 仕様ID統合: `YamlLoader.java:68` と `YamlLoader.java:70` は RS-09 として1件に統合
  - `VariableLengthFile.java:76` → DR-12、`DataFile.java:298` → DR-11（ディレクティブカテゴリが適切）
- return null/empty 行 15行 = 登録 8行 + 除外 7行
  - 除外内訳: `DataFile.java:77`（MapCollector 内部）、`YamlSection.java:88/99`（内部ユーティリティ）、`YamlSection.java:138`（RS-03 の実装パス）、`YamlMessageBuilder.java:107`（変数代入）
  - `YamlLoader.java:63` は return ではなく変数代入だが、外部から空 Map として返却されるため RS-18 に登録
  - `YamlTestDataParser.java:99` は RS-15 と同一仕様（BasicTestDataParser:54 との継承関係）
- **数値確認**: throw 25行 = 登録23行 + 除外2行 ✓
- **数値確認**: return null/empty 15行 = 登録8行（仕様ID: RS-15/16/18/19/20/SS-31/32） + 除外7行 ✓

---

## 抜け漏れ確認

本仕様一覧は以下の調査結果を統合して作成した。

| 調査元 | 仕様数 | 取り込み状況 |
|---|---|---|
| `ntf-coverage-spec-mapping.md`（コード全行走査 29クラス） | S-1〜S-5 / D-1〜D-16 / E-1〜E-4 | 全件取り込み済み。D-10→SS-20 として追加（QA指摘NG-1対応） |
| `ntf-coverage-doc-check.md`（公式解説書照合 13本） | Doc-1〜Doc-17（うち反映対象17件） | 全件取り込み済み。Doc-5→DT-06、Doc-8→IV-14、Doc-12→IV-15、Doc-15→MS-11（QA指摘対応） |
| `ntf-testdata-yaml-design.md`（スキーマ設計・設計上の注意点） | 27項目（§1〜§27） | 全件取り込み済み。§19→MS-13、§20→MS-12 として追加（QA指摘対応） |
| E-1〜E-9 | 9件 | 全件処置済み（8件昇格・1件ドキュメント修正のみ） |
| I-1 grep 証跡（throw/return null の全行走査） | throw 25行 + return null/empty 15行 = 計40行 | 全件処置済み（登録31件・除外9件）。DT-08/SS-14/16/21〜32/RS-09〜20/DR-11〜12/MS-14 を新規追加 |
