# NTF テストデータ 実装仕様一覧（ntf-impl-spec-list.md）

- **作成日**: 2026-05-20（I-1 タスク）
- **参照元**: `ntf-coverage-spec-mapping.md`（コード全行走査）、`ntf-coverage-doc-check.md`（公式解説書照合）、`ntf-testdata-yaml-design.md`（スキーマ設計）
- **目的**: Ph-1 三角マッピングの基準となる仕様IDを確定する。後続タスク（I-2/I-3/Ph-2）の全件を本文書に基づいて追跡する。

---

## 仕様ID体系

| プレフィクス | カテゴリ | 対応コード領域 |
|---|---|---|
| DT | セクション識別・DataType | `DataType`, `TestDataParsingTemplate`, `GroupDataParsingTemplate`, `SingleDataParsingTemplate` |
| SS | テーブル・ファイル構造 | `TableData`, `ListMapParser`, `DataFileParser`, `DataFile`, `DataFileFragment`, `BasicTestDataParser` |
| RS | YAMLリーダー実装仕様 | `TestDataReader` インタフェース（実装: `YamlTestDataReader`）|
| HC | ヘッダ行・カラム処理 | `HeaderLine`, `TestDataParsingTemplate` |
| IV | インタープリタ・特殊値 | interpreter / generator パッケージ全クラス |
| DR | ディレクティブ | `DataFile`, `FixedLengthFile`, `VariableLengthFile`, ディレクティブ列挙体 |
| MS | メッセージングテストデータ | `MessageParser`, `SendSyncMessageParser`, `GroupMessageParser`, `SendSyncSupport`, `RequestTestingMessagingClient` |

---

## 仕様一覧

### DT: セクション識別・DataType

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| DT-01 | DataType 列挙値: `DEFAULT` / `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` / `LIST_MAP` / `SETUP_FIXED` / `EXPECTED_FIXED` / `SETUP_VARIABLE` / `EXPECTED_VARIABLE` / `MESSAGE` / `EXPECTED_REQUEST_HEADER_MESSAGES` / `EXPECTED_REQUEST_BODY_MESSAGES` / `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` の14種 | テストデータ構造 | `DataType.java` 行10-56 | `DataTypeTest#testGetName`, `DataTypeTest#testGetType`（DataType列挙値の存在確認） | スキーマ根拠: `ntf-test-data.schema.json` の最上位 `properties` キー（`setup_tables`, `expected_tables`, ..., `response_body_messages`）が 14 DataType を網羅 |
| DT-02 | セクション識別行の書式: `<DataType名>[groupId]=<値>` (`=` が必須区切り文字。groupId は省略可) | テストデータ構造 | `TestDataParsingTemplate.java` 行244-253 | `TestDataParsingTemplateTest#testParseFail`（parse内部でセクション識別を使用）、`BasicTestDataParserTest#testExpectedGetTableData`（EXPECTED_TABLE セクション識別の間接テスト） | スキーマ根拠: 各 `$defs` オブジェクトの `group_id` + `id`/`table`/`path` 構造が `=` 区切り書式を YAML で表現 |
| DT-03 | DataType 判定は前方一致（`startsWith`）: セル値が DataType の name で始まれば合致。識別キー＋追加文字のセル値でも認識される | テストデータ構造 | `TestDataParsingTemplate.java` 行221-242（旧E-4） | テスト追加必要（`StartsWithTest#testStartsWith` は DataType の `startsWith` とは別クラス。`DataType#getType()` の前方一致動作を直接テストするテストが存在しない） | スキーマ外・パーサ実装で担保（YAML キーは完全なセクション名を使用するため前方一致は発生しない。既存 Excel 互換性のための実装内部仕様） |
| DT-04 | GroupData系（SETUP_TABLE 等）は同一 groupId のセクションを全部収集し続ける（`shouldStopOnNextOne() = false`） | テストデータ構造 | `GroupDataParsingTemplate.java` 行45-53 | `TestDataParsingTemplateTest#testGroupDataWithNullInterpreter`（GroupData収集の停止しない動作）、`BasicTestDataParserTest#testGetExpectedTableDataWithGroupId`（複数グループの収集） | スキーマ根拠: `setup_tables`/`expected_tables` 等が `type: array` で複数エントリを許容（GroupData の全件収集を表現） |
| DT-05 | SingleData系（LIST_MAP / MESSAGE 等）は最初に合致したセクション1つだけを取得して停止する（`shouldStopOnNextOne() = true`） | テストデータ構造 | `SingleDataParsingTemplate.java` 行43-53 | `SingleDataParsingTemplateTest#testParseSingleData`（SingleData先着一致）、`TestDataParsingTemplateTest#testSingleDataWithNullInterpreter` | スキーマ根拠: `list_maps` / `messages` の各エントリが `id` キーを持ち、パーサが最初の一致のみを取得（スキーマは構造を定義、先着一致はパーサ実装） |
| DT-06 | groupId 書式: `[groupId]`（省略時は空文字扱い。要素数1時のみ有効・2以上は `IllegalArgumentException`）。バッチ固有: `group_id: "default"` はグループIDなし扱いと同等になる | テストデータ構造 | `BasicTestDataParser.java` 行243-266、公式解説書 batch.rst（Doc-5） | `BasicTestDataParserTest#testFormatGroupId`, `BasicTestDataParserTest#testFormatGroupIdFail` | スキーマ根拠: `table_data.$defs.group_id` の `minLength: 1` 制約（空文字禁止）。`design.md §8` グループIDなしの場合 |
| DT-07 | `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` は GroupData（groupId 必須）経路と SingleData（id 一致）経路の2つが存在する | テストデータ構造 | `BasicTestDataParser.java` 行104-117、`design.md §10` | テスト追加必要（`RequestTestingSendSyncSupportTest#testGetExpectedRequestMessageWithoutCache` はアクセスパスBの間接テストのみ。GroupData経路（パスA）のテストなし） | スキーマ根拠: `response_header_messages`/`response_body_messages` が `group_message_data` を参照し、`group_id` 有無で両経路を表現（`design.md §10`） |

---

### SS: テーブル・ファイル構造

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| SS-01 | テーブルデータ行の形式: カラム名をキーとするオブジェクト形式。省略されたカラムにはデフォルト値が INSERT 時に補完される | テストデータ構造 | `TableData.java`、`design.md §1/§4` | `BasicTestDataParserTest#testGetSetupTableData`（テーブルデータ行の読み取り） | スキーマ根拠: `$defs.table_data.properties.rows` の `additionalProperties: {type: ["string","null"]}` がカラム=値の対応を表現 |
| SS-02 | `EXPECTED_TABLE`: 省略されたカラムは比較対象外になる（カラム列挙は任意） | テストデータ構造 | `BasicTestDataParser.java` 行170-181、公式解説書 02_DbAccessTest.rst | `BasicTestDataParserTest#testExpectedGetTableData`（カラム省略が比較対象外になること） | スキーマ根拠: `expected_tables` の `table_data.rows` でカラムを省略可能（`additionalProperties` 方式） |
| SS-03 | `EXPECTED_COMPLETE_TABLE`: 省略されたカラムに `BasicDefaultValues` のデフォルト値を補完してから比較する | テストデータ構造 | `BasicTestDataParser.java` 行170-181 (`fillDefaultValues()` 呼び出し) | `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithoutId`, `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithId` | スキーマ根拠: `expected_complete_tables` の `table_data` 構造は `expected_tables` と同一だが、パーサが `fillDefaultValues()` を呼ぶ点はスキーマ外 |
| SS-04 | `SETUP_TABLE` では主キーカラムは省略不可（省略するとデフォルト値が INSERT される） | テストデータ構造 | 公式解説書 02_DbAccessTest.rst（Doc-2） | テスト追加必要（主キー省略時の動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（主キーカラム省略の検出はスキーマでは困難。INSERT 時のランタイム制約） |
| SS-05 | `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一ファイル内で混在させると後半データが読み込まれない（まとめて記述が必要） | テストデータ構造 | 公式解説書 01_Abstract.rst（Doc-4） | テスト追加必要（EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE 混在時の動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（混在時の後半データ欠落はパーサのランタイム動作。YAML ファイルを分割して記述することを設計で推奨） |
| SS-06 | `LIST_MAP=id` セクション: id は完全一致。同一ファイル内で同一 id の重複エントリは後続が黙って無視される（先着一致） | テストデータ構造 | `SingleDataParsingTemplate.java`、`design.md §9` | `SingleDataParsingTemplateTest#testParseSingleData`（先着一致） | スキーマ根拠: `$defs.list_map_data.properties.id` が識別子を表現。先着一致はスキーマ外（パーサ実装） |
| SS-07 | `SETUP_FIXED` と `SETUP_VARIABLE` は `BasicTestDataParser#getSetupFile()` でまとめて返される。`EXPECTED_FIXED`/`EXPECTED_VARIABLE` も同様 | テストデータ構造 | `BasicTestDataParser.java` 行66-80 | `BasicTestDataParserTest#testGetSetupTableData`（getSetupFile 間接テスト）、`FileSupportTest#testSetUpFixedLengthFile`（固定長ファイル） | スキーマ根拠: `setup_files.type` フィールドの `enum: ["fixed","variable"]` で SETUP_FIXED/VARIABLE を統合表現（`design.md §3`） |
| SS-08 | ファイルセクションの行順序: ディレクティブ行（0行以上） → フィールド名行 → データ型行 → [フィールド長行（固定長のみ）] → データ行 | テストデータ構造 | `DataFileParser.java` 行38-49（`Status` 遷移） | `FileSupportTest#testSetUpFixedLengthFile`, `FileSupportTest#testSetUpVariableLengthFile`（ファイルセクション行順序） | スキーマ根拠: `$defs.file_data` の `directives`（0以上）→ `records[].fields`（名前/型/長さ統合）→ `records[].rows` 構造が行順序を表現 |
| SS-09 | 固定長フラグメント: `names` / `types` / `lengths` の3リストが同サイズで必須 | テストデータ構造 | `FixedLengthFileFragment.java` 行140-144 | `FileSupportTest#testSetUpFixedLengthFile`（固定長 names/types/lengths 3リスト） | スキーマ根拠: `$defs.record_fragment.fields` の `items: {$ref: field_def}` と `field_def.length` 必須（固定長では実質必須） |
| SS-10 | 可変長フラグメント: `names` / `types` の2リストが同サイズで必須。`lengths` は不要（型行読み取り後に直接 READING_VALUES へ遷移） | テストデータ構造 | `VariableLengthFileParser.java` 行40-46 | `FileSupportTest#testSetUpVariableLengthFile`（可変長 names/types 2リスト） | スキーマ根拠: `field_def.length` が `anyOf` でオプション（可変長では省略可） |
| SS-11 | 1ファイルセクション内に複数レコードレイアウトを連続記述可能: データ行の後ろに新たなフィールド名行を書くと新レコードレイアウトとして扱われる | テストデータ構造 | `DataFileParser.java` 行177-191（旧D-14） | テスト追加必要（複数レコードレイアウトの連続記述を明示するテストなし） | スキーマ根拠: `$defs.file_data.records` の `minItems: 0` と複数 `record_fragment` が連続記述を表現（`design.md §24`） |
| SS-12 | フィールド名行の構造: 先頭列 = レコード種別名、2列目以降 = フィールド名の列挙 | テストデータ構造 | `DataFileParser.java` 行243-252 | `FileSupportTest#testSetUpFixedLengthFile`（先頭セル=レコード種別名） | スキーマ根拠: `$defs.record_fragment.record_type` フィールドが先頭セル（レコード種別名）を表現 |
| SS-13 | データ行の先頭セルは必ず空（null または空文字）にする | テストデータ構造 | `DataFileParser.java` 行193-210 | `FileSupportTest#testSetUpFixedLengthFile`（データ行先頭セル空） | スキーマ外・パーサ実装で担保（YAML では行概念なく `rows` 配列の各要素が `fields` に対応。先頭セル空の制約なし） |
| SS-14 | 同一レコード種別内のフィールド名は重複不可（`IllegalArgumentException`）。異なる種別間は重複可 | テストデータ構造 | `DataFileFragment.java` 行185-194、348-362（Doc-9） | `FileSupportTest#testSetUpFixedWithDuplicateName`, `FileSupportTest#testAssertFixedWithDuplicateName`, `FileSupportTest#testSetUpVariableWithDuplicateName`, `FileSupportTest#testAssertVariableWithDuplicateName` | スキーマ根拠: `$defs.record_fragment.fields` の `items` で `name` ユニーク制約は JSON Schema では表現困難。スキーマ外・パーサ実装で担保（`IllegalArgumentException`） |
| SS-15 | 空ファイル（0バイト）表現: ディレクティブ行のみ記述してレコード定義を省略する。`records` の `minItems: 0` が必要 | テストデータ構造 | 公式解説書 03_Tips.rst（Doc-10） | `FileSupportTest#testAssertEmptyVariableFile`, `FileSupportTest#testAssertFixedActuallyEmpty`, `FileSupportTest#testAssertVariableActuallyEmpty` | スキーマ根拠: `$defs.file_data.records` の `minItems: 0`（空配列許容）（`design.md §25`） |
| SS-16 | 固定長ファイルは全フラグメントで同一レコード長が必須（違反時 `IllegalStateException`） | テストデータ構造 | `FixedLengthFile.java` 行94-117 | `FixedLengthFileParserTest#testInvalidDirectives`（異なるレコード長で IllegalStateException） | スキーマ外・パーサ実装で担保（フラグメント間のレコード長一致はランタイムチェック） |
| SS-17 | `"-"` 長フィールド: 追加された全レコードの最大バイト長に自動拡張。値は改行コードと前後空白が除去される | テストデータ構造 | `DataFileFragment.java` 行129-161（旧D-16） | `FileSupportTest#testVariation`（"-" 長フィールドの動作） | スキーマ根拠: `$defs.field_def.length` の `anyOf` に `{type: "string", const: "-"}` を含む（`design.md §27`） |
| SS-18 | `BasicDefaultValues` のデフォルト値: 数値型=`"0"`、CHAR/NCHAR=スペース×カラム長、VARCHAR等=半角スペース1文字、DATE=`"1970-01-01 09:00:00.0"`（JVM タイムゾーン依存）、バイナリ=10バイトゼロHexString、Boolean=`"false"` | テストデータ構造 | `BasicDefaultValues`、`design.md §4` | `BasicTestDataParserTest#testGetExpectedTableDataCompletedWithoutId`（EXPECTED_COMPLETE_TABLE でデフォルト値補完の間接テスト） | スキーマ外・テストで担保する方針（BasicDefaultValues のデフォルト値はパーサ実装。TZ依存（E-8）は制約事項として注記） |
| SS-19 | `testShots` は LIST_MAP の予約ID: バッチリクエスト単体テストでフレームワークがテストケース一覧として自動読み込みする | テストデータ構造 | 公式解説書 batch.rst（Doc-16） | テスト追加必要（`testShots` の予約ID動作を明示するテストなし） | スキーマ外仕様・テストで担保する方針（`testShots` は LIST_MAP の予約ID。YAML では `list_maps` の `id: testShots` エントリとして記述） |
| SS-20 | ファイル系空行の動作差異: 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される | テストデータ構造 | `design.md §AI向けプロンプト ファイル系の空行動作`（旧D-10） | `FileSupportTest#testSetUpVariableEmptyLine`, `FileSupportTest#testSetUpVariableEmptyLine2`, `FileSupportTest#testAssertEmptyLineVariable`, `FileSupportTest#testAssertEmptyLineFixed` | スキーマ外・パーサ実装で担保（空行の扱いはパーサのランタイム動作） |
| SS-21 | セクション未存在またはデータ未存在時の返却値: `getSetupTableData`/`getSetupFile` で `isDataExisting()` が false のとき空リスト（`Collections.emptyList()`）を返す。`getListMap` で指定 ID が見つからないときも空リストを返す | 実装内部ロジック | `BasicTestDataParser.java` 行53-56（`isDataExisting` false → `emptyList()`）、`SingleDataParsingTemplate.java` 行43-53（先着一致・見つからなければ空リスト） | `BasicTestDataParserTest#testGetTableDataNotExist`（存在しないグループID → 空リスト）、`YamlTableDataBuilderTest#testBuildTableDataList_sectionNotExists`、`YamlTableDataBuilderTest#testBuildTableDataList_emptyRowsExcluded`、`YamlTableDataBuilderTest#testBuildListMapRows_idNotFound`、`YamlFileBuilderTest#testBuildFileList_sectionNotExists` | スキーマ外・パーサ実装で担保（セクション/ID 未存在時の空リスト返却はパーサ実装。呼び出し元が空リストを想定した処理をする責務を持つ） |
| SS-22 | `DataFileParser` のディレクティブ行/フィールド名行が2列未満のとき `IllegalStateException("directive or data names row must have two columns at least. ...")` をスロー | 実装内部ロジック | `DataFileParser.java` 行222-224（`processDirectives` 内の列数チェック） | テスト追加必要（ディレクティブ行/フィールド名行が2列未満のときの例外を明示するテストなし） | スキーマ外・パーサ実装で担保（YAML ではディレクティブをオブジェクトで記述するため行列数の概念がない。列数チェックは Excel/TSV 形式固有のランタイムチェック） |
| SS-23 | `DataFileFragment.setNames()` に null または空リストを渡すと `IllegalArgumentException("names must not be null or empty.")` をスロー | 実装内部ロジック | `DataFileFragment.java` 行326-329（`setNames` の null/空チェック） | テスト追加必要（`setNames()` に null/空を渡したときの例外を明示するテストなし） | スキーマ外・パーサ実装で担保（YAML ではフィールド定義を `fields` 配列で記述するため null/空リストはスキーマバリデーションで排除される） |
| SS-24 | `DataFileFragment.setTypes()`/`setLengths()` のリストサイズが `names` と異なるとき `IllegalArgumentException("field name size is ... but types/lengths size is ...")` をスロー | 実装内部ロジック | `DataFileFragment.java` 行339-346（`assertSameSizeAsNames` による共通チェック） | テスト追加必要（`setTypes()`/`setLengths()` のサイズ不一致時の例外を明示するテストなし） | スキーマ外・パーサ実装で担保（YAML では `fields` 配列内で各フィールド定義を1オブジェクトとして記述するため names/types/lengths の個別リスト形式は存在しない） |
| SS-25 | `DataFileFragment.getIndexOf()` に存在しないフィールド名を渡すと `IllegalArgumentException("no such field [...]. ...")` をスロー（アサート時のフィールド名指定ミス等） | 実装内部ロジック | `DataFileFragment.java` 行443-447（`names.indexOf(fieldName) == -1` のとき例外スロー） | テスト追加必要（存在しないフィールド名を指定したときの例外を明示するテストなし） | スキーマ外・パーサ実装で担保（フィールド名の存在チェックはランタイムチェック） |

---

### RS: YAMLリーダー実装仕様

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| RS-01 | `open(path, dataName)` 規約: `dataName` に対して `{dataName}.yaml` ファイルを検索する | 実装内部ロジック | `TestDataReader` インタフェース（設計方針） | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（`YamlTestDataReader.open()` の実装仕様。Ph-2 R-1 で実装） |
| RS-02 | `readLine()` は文書終端で `null` を返す | 実装内部ロジック | `TestDataReader` インタフェース（既存 Excel 実装との整合） | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（`readLine()` の終端返却仕様） |
| RS-03 | YAML ネイティブ `null`（アンクォート）は Java `null` として返す（旧E-1） | 実装内部ロジック | `design.md §7`（SnakeYAML が Java null に変換し、パーサがそのまま返す） | `testRs03_yamlNativeNullIsJavaNull`（`YamlTestDataParserTest`） | スキーマ外・パーサ実装で担保（YAML ネイティブ null は Java null として返す） |
| RS-04 | YAML ネイティブ boolean (`true`/`false`) は文字列 `"true"`/`"false"` として返す（旧E-1） | 実装内部ロジック | `design.md §7` | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（YAML ネイティブ boolean の文字列化） |
| RS-05 | YAML ネイティブ integer/float は数字文字列として返す（旧E-1） | 実装内部ロジック | `design.md §7` | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（YAML ネイティブ数値の文字列化） |
| RS-06 | 末尾の空要素（YAML ネイティブ null または省略）は Java `null` として返す（旧E-2） | 実装内部ロジック | Excel 実装（`HeaderLine.java`）が `""` 補完するのに対し、YAML 実装は RS-03 仕様により Java null を返す。これは設計上の決定であり `design.md §7` に明記 | `testRs06_trailingNativeNullIsJavaNull` / `testRs06_trailingKeyOmittedIsNull`（`YamlTestDataParserTest`） | スキーマ外・パーサ実装で担保（末尾空要素は Java null として返す） |
| RS-07 | `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを保証する（旧E-3） | 実装内部ロジック | `TestDataParsingTemplate.java` 行187-219 の parse ロジック | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（null 返却後の最終セクション欠落防止） |
| RS-08 | `isDataExisting(directory, resource)` / `isResourceExisting(directory, resource)` の実装（リソース存在確認） | 実装内部ロジック | `BasicTestDataParser.java` 行267-271 | テスト追加必要（YamlTestDataReader 未実装。Ph-2 R-1 で実装・テスト作成予定） | スキーマ外・パーサ実装で担保（isDataExisting/isResourceExisting の実装） |
| RS-09 | `setup_tables`/`expected_tables` 等のテーブルエントリに `table` キーが欠如している場合、`IllegalStateException("table key is missing. section=[...] file=[...] ")` をスローする | 実装内部ロジック | `YamlTableDataBuilder.java`（R-1-refactor で実装: E-1 対応）| `YamlTableDataBuilderTest#testBuildTableDataList_missingTableThrowsException`（`table` キー欠如 → `IllegalStateException`。セクション名・ファイルパスがメッセージに含まれること） | スキーマ外・YAML パーサ実装で担保（既存 Excel 実装には対応なし。`table` キー欠如はスキーマバリデーション外のランタイムチェック） |
| RS-10 | `setup_files`/`expected_files` 等のファイルエントリに `path` キーが欠如している場合、`IllegalStateException("path key is missing. section=[...] group=[...]")` をスローする | 実装内部ロジック | `YamlFileBuilder.java`（R-1-refactor で実装: E-2 対応）| `YamlFileBuilderTest#testBuildFileList_missingPathThrowsException`（`path` キー欠如 → `IllegalStateException`。セクション名・グループIDがメッセージに含まれること） | スキーマ外・YAML パーサ実装で担保（既存 Excel 実装には対応なし。`path` キー欠如はスキーマバリデーション外のランタイムチェック） |
| RS-11 | メッセージデータの FW_HEADER エントリの `rows` が List 形式以外（Map 等）のとき、`IllegalStateException("FW_HEADER rows must be a list. section=[...] id=[...]")` をスローする | 実装内部ロジック | `YamlMessageBuilder.java`（R-1-refactor で実装: E-3 対応）| `YamlMessageBuilderTest#testBuildMessagePool_malformedFwHeaderRowsThrowsException`（`rows` が Map 形式 → `IllegalStateException`。セクションキー・ID がメッセージに含まれること） | スキーマ外・YAML パーサ実装で担保（既存 Excel 実装には対応なし。YAML 型チェックはランタイムチェック） |
| RS-12 | FW_HEADER エントリの `rows` が空リストのとき、例外なく空の fwHeader（空 Map）を持つ `MessagePool` が返る | 実装内部ロジック | `YamlMessageBuilder.java`（R-1-refactor で実装）| `YamlMessageBuilderTest#testBuildMessagePool_emptyFwHeaderRows`（FW_HEADER rows 空リスト → 例外なし・空 fwHeader の `MessagePool` が返ること） | スキーマ外・YAML パーサ実装で担保（既存 Excel 実装では FW ヘッダ行が空なら空 Map となる動作と同等） |
| RS-13 | messaging 系以外の `DataType` を `YamlSection.dataTypeToSectionKey()` に渡すと `IllegalArgumentException("Unsupported DataType: ...")` をスローする | 実装内部ロジック | `YamlSection.java`（R-1-refactor で実装）| `YamlMessageBuilderTest#testDataTypeToSectionKey_unsupportedDataTypeThrowsException`（messaging 系以外の `DataType` → `IllegalArgumentException`） | スキーマ外・YAML パーサ実装で担保（既存 Excel 実装には対応なし。DataType → sectionKey 変換はYAML固有の処理） |

---

### HC: ヘッダ行・カラム処理

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| HC-01 | マーカーカラムの書式: `[カラム名]`（`[` で始まり `]` で終わる） | テストデータ構造 | `HeaderLine.java` 行87-96 | `BasicTestDataParserTest#testGetListMapIgnoredColumn`, `BasicTestDataParserTest#testGetExpectedTableIgnoredColumn`, `BasicTestDataParserTest#testGetSetupTableIgnoredColumn`（マーカーカラム書式） | スキーマ根拠: `design.md §6` マーカーカラムの扱い。YAML では `[COLNAME]` 形式カラムを出力しない（変換ルール） |
| HC-02 | マーカーカラムは DB 操作から除外される（データとして格納されない） | テストデータ構造 | `HeaderLine.java` 行53-85、`TableDataParser.java` 行74-82 | `BasicTestDataParserTest#testGetListMapIgnoredColumn`（DB操作から除外） | スキーマ外・パーサ実装で担保（マーカーカラム除外はパーサ実装） |
| HC-03 | ヘッダ行末尾の空カラムは除去される（末尾カラム省略可） | テストデータ構造 | `HeaderLine.java` 行27-42（`trimTailCopy()`） | `BasicTestDataParserTest#testGetListMapWithInvisibleTail`, `BasicTestDataParserTest#testGetTableDataWithInvisibleTail`（末尾空カラム除去） | スキーマ外・パーサ実装で担保（末尾空カラム除去は `HeaderLine.java` の実装） |
| HC-04 | データ行がヘッダより短い場合、不足分は空文字 `""` で補完される | テストデータ構造 | `HeaderLine.java` 行69-85 | `BasicTestDataParserTest#testGetListMapWithInvisibleTail`（データ行がヘッダより短い場合の補完） | スキーマ根拠: `$defs.record_fragment.rows` の各配列が `fields` と同順・同件数を要求（補完はパーサ実装） |
| HC-05 | コメント行: 先頭セルが `//` で始まる行は行ごとスキップ | テストデータ構造 | `TestDataParsingTemplate.java` 行268-291 | `TestDataParsingTemplateTest#testIsCommentRow`（コメント行判定） | スキーマ外・パーサ実装で担保（コメント行はパーサが `//` 先頭を検出してスキップ。YAML では行コメント `#` を使用） |
| HC-06 | 行内コメント: 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨て | テストデータ構造 | `TestDataParsingTemplate.java` 行292-308 | テスト追加必要（行内コメント（先頭以外の `//` 以降切り捨て）を明示するテストなし） | スキーマ外・パーサ実装で担保（行内コメント切り捨てはパーサ実装。YAML では行末コメント `#` で同等機能） |
| HC-07 | 空行スキップ: 全要素が null または空文字の行は読み飛ばす | テストデータ構造 | `TestDataParsingTemplate.java` 行310-318 | テスト追加必要（空行スキップの明示的テストなし） | スキーマ外・パーサ実装で担保（空行スキップはパーサ実装。YAML では空行は存在しない） |

---

### IV: インタープリタ・特殊値

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| IV-01 | `NullInterpreter`: `null`/`NULL`/`Null`（大文字小文字不問）を Java null に変換 | テストデータ構造 | `NullInterpreter.java` 行10-19 | `NullInterpreterTest#testInterpretNullLowerCase`, `NullInterpreterTest#testInterpretNullUpperCase`, `NullInterpreterTest#testInterpretNullCapitalized`, `NullInterpreterTest#testInterpretNotNullValue` | スキーマ根拠: `$defs.table_data.rows.items.additionalProperties` の `type: ["string","null"]` で null 値を許容。`design.md §7` 特殊値の表現 |
| IV-02 | `QuotationTrimmer`: 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去。片側のみはスルー | テストデータ構造 | `QuotationTrimmer.java` 行18-30 | `QuotationTrimmerTest#testInterpretHalfWidthQuotation`, `QuotationTrimmerTest#testInterpretFullWidthQuotation`, `QuotationTrimmerTest#testInterpretNotQuoted` | スキーマ根拠: `design.md §7` 特殊値の表現（クォーティング記法） |
| IV-03 | `DateTimeInterpreter`: `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換。部分文字列は変換されない（`CompositeInterpreter` との組み合わせが必要） | テストデータ構造 | `DateTimeInterpreter.java` 行48-94 | テスト追加必要（`DateTimeInterpreter` の完全一致制約を明示するテストなし。実装はあるが独立したテストクラスが見当たらない） | スキーマ根拠: `design.md §22` DateTimeInterpreter の完全一致制約 |
| IV-04 | `LineSeparatorInterpreter`: `\\r` → CR(0x0D)（デフォルト）、`\\n` → LF(0x0A) に変換 | テストデータ構造 | `LineSeparatorInterpreter.java`、公式解説書 01_Abstract.rst（Doc-7） | `LineSeparatorInterpreterTest#testConvertBackR`, `LineSeparatorInterpreterTest#testDoNotConvertCR`, `LineSeparatorInterpreterTest#testDoNotConvert` | スキーマ根拠: `design.md §7` 特殊値の表現（`\\n`/`\\r` 記法） |
| IV-05 | `BinaryFileInterpreter`: `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換。YAML ファイルが基準ディレクトリになる | テストデータ構造 | `BinaryFileInterpreter.java` 行34-65 | `BinaryFileInterpreterTest#testOk`, `BinaryFileInterpreterTest#testNotApplicable`, `BinaryFileInterpreterTest#testFileNotFound` | スキーマ根拠: `design.md §21` BinaryFileInterpreter のパス基準 |
| IV-06 | `BasicJapaneseCharacterInterpreter`: `${文字種,文字数}` 形式で文字列生成。書式完全一致のみ動作、文字種未知の場合は `IllegalArgumentException`（書式ミスはスルー） | テストデータ構造 | `BasicJapaneseCharacterInterpreter.java` 行22-45 | `BasicJapaneseCharacterInterpreterTest#testInterpret`, `BasicJapaneseCharacterInterpreterTest#testInterpretUnknownType`, `BasicJapaneseCharacterInterpreterTest#testInterpretNotResponsible` | スキーマ根拠: `design.md §7` / `ntf-testdata-yaml-design.md §BasicJapaneseCharacterInterpreter の有効トークン（14種）` |
| IV-07 | `BasicJapaneseCharacterGenerator` 有効文字種14種: 半角英字/半角数字/半角記号/半角カナ/全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角記号その他/中国語/サロゲートペア/改行/外字 | テストデータ構造 | `BasicJapaneseCharacterGenerator.java` 行40-56 | `BasicJapaneseCharacterGeneratorTest#testGenerate`, `BasicJapaneseCharacterGeneratorTest#testGenerateWithUnknownType` | スキーマ根拠: `design.md §BasicJapaneseCharacterInterpreter の有効トークン（14種）` |
| IV-08 | `CompositeInterpreter`: 文字列中の `${...}` 要素を個別解釈して置換。`${...}` がない場合は次のインタープリタに委譲 | テストデータ構造 | `CompositeInterpreter.java` 行22-42 | `CompositeInterpreterTest#testExpression`, `CompositeInterpreterTest#testCombinationOfNotations`, `CompositeInterpreterTest#testCombinationOfInterpreters`, `CompositeInterpreterTest#testLiteral` | スキーマ根拠: `design.md §23` CompositeInterpreter の DI 設定 |
| IV-09 | 日付型カラムの記述形式: `yyyyMMddHHmmssSSS`（17文字）、後置0埋め短縮形、JDBC タイムスタンプエスケープ形式（5文字目が `-`）等が有効 | テストデータ構造 | `TableData.java` 行214-273、`design.md §7` | テスト追加必要（日付型カラムの記述形式の境界値テストなし） | スキーマ外・パーサ実装で担保（日付型変換は `TableData.java` のランタイム処理） |
| IV-10 | `Timestamp` 型カラムの期待値は末尾 `.0` が必要（例: `"2010-01-01 12:34:56.0"`） | テストデータ構造 | 公式解説書 02_DbAccessTest.rst（Doc-3） | テスト追加必要（Timestamp 型の `.0` 必須を明示するテストなし） | スキーマ外仕様・テストで担保する方針（Timestamp 末尾 `.0` は期待値記述ルール。YAML でも文字列として記述） |
| IV-11 | バイナリデータの直接記述: `0x` プレフィクス付き16進数で記述可能。`0x` がない場合は文字列としてエンコード | テストデータ構造 | 公式解説書 batch.rst（Doc-11） | テスト追加必要（バイナリデータの `0x` プレフィクス記法を明示するテストなし） | スキーマ外仕様・テストで担保する方針（`0x` プレフィクス記法は値記述ルール。YAML でも文字列として記述） |
| IV-12 | `BasiDataTypeMapping` デフォルトマッピング22種（`半角英字`→`X` 等）。未知の型記号は `IllegalArgumentException` | テストデータ構造 | `BasicDataTypeMapping.java` 行30-73 | `BasicDataTypeMappingTest#testConvertToFrameworkExpression`, `BasicDataTypeMappingTest#testConvertToFrameworkExpressionFail`, `BasicDataTypeMappingTest#testConvertToFrameworkExpressionNull`, `BasicDataTypeMappingTest#testSetMappingTable` | スキーマ根拠: `$defs.field_def.type` の `pattern: "^[A-Z][A-Z0-9_]*$"` と `design.md §5` DataTypeMapping |
| IV-13 | `TEST_` プレフィクス型の自動優先選択: `TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用される | テストデータ構造 | `DataFileFragment.java` 行211-245 | `FileSupportTest#testVariation`（TEST_ プレフィクス型の動作を間接的にテスト） | スキーマ根拠: `$defs.field_def.type` のパターン（`TEST_` プレフィクスも `[A-Z][A-Z0-9_]*` に合致）。`design.md §16` TEST_ プレフィクス型の自動昇格 |
| IV-14 | `QuotationTrimmer` によるスペース値明示記法: `'"⊔"'` → 半角スペース、`'"""'` → ダブルクォート1文字。ダブルクォートで囲むことで空白値を可視化して記述できる | テストデータ構造 | `design.md §7`、公式解説書 01_Abstract.rst（Doc-8） | `QuotationTrimmerTest#testInterpretHalfWidthQuotation`（スペース値明示記法） | スキーマ根拠: `design.md §7` 特殊値の表現（`'"""'`/`'"⊔"'` 記法） |
| IV-15 | X9/SX9 型フィールドの記述方法: パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記載する必要がある | テストデータ構造 | 公式解説書 batch.rst（Doc-12）、`design.md §26` | テスト追加必要（X9/SX9 型の実値記述を直接テストするものなし） | スキーマ根拠: `design.md §26` X9/SX9 型フィールドの記述方法 |
| IV-16 | 日付カラムの変換失敗時に `RuntimeException("invalid date format. tableName=...:rowNo=...:columnName=...:value=...")` をスロー。値が null または空文字のときは `null` を返す（エラーなし） | 実装内部ロジック | `TableData.java` 行204-209（`ParseException` → `RuntimeException` ラップ）、行225-227（null/空文字 → null 返却） | テスト追加必要（日付カラムの変換失敗時の例外メッセージを明示するテストなし。null/空文字の null 返却を明示するテストなし） | スキーマ外・パーサ実装で担保（日付変換はランタイム処理） |

---

### DR: ディレクティブ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| DR-01 | ディレクティブ行の構成: 先頭列 = キー名、2列目 = 値（最低2列必要） | テストデータ構造 | `DataFileParser.java` 行212-232 | `FileSupportTest#testSetUpFixedLengthFile`（ディレクティブ行読み取り） | スキーマ根拠: `$defs.directives` オブジェクトが `key: value` 形式のディレクティブを表現 |
| DR-02 | 固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定される。許容外のキーは `DataFile.java` 行298-299 で `IllegalArgumentException("invalid directive found. [...]")` をスロー | テストデータ構造 | `FixedLengthFileParser.java` 行34-38（`FixedLengthDirective.valueOf()` によるキー検証）、`DataFile.java` 行294-299（`setDirective()` の `IllegalArgumentException`） | `FixedLengthFileParserTest#testInvalidDirectives`（固定長ディレクティブキーの制限） | スキーマ根拠: `$defs.directives.properties` に固定長専用キー（`record-length`, `positive-zone-sign-nibble` 等）を列挙（`additionalProperties: false`） |
| DR-03 | 可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定される。許容外のキーは `DataFile.java` 行298-299 で `IllegalArgumentException("invalid directive found. [...]")` をスロー | テストデータ構造 | `VariableLengthFileParser.java` 行34-38（`VariableLengthDirective.valueOf()` によるキー検証）、`DataFile.java` 行294-299（`setDirective()` の `IllegalArgumentException`） | テスト追加必要（可変長ディレクティブキー制限の明示テストなし） | スキーマ根拠: `$defs.directives.properties` に可変長専用キー（`field-separator`, `quoting-delimiter` 等）を列挙 |
| DR-04 | `defaultDirectives` DI: SystemRepository のこのキーで全ファイル共通デフォルトディレクティブを一括設定できる | 実装内部ロジック | `DataFile.java` 行59-93（旧E-6） | テスト追加必要（`defaultDirectives` DI 設定の YAML 適用確認テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（DI 設定はランタイム。`design.md §14` デフォルトディレクティブの DI） |
| DR-05 | `fixedLengthDirectives` DI: 固定長ファイル専用デフォルトディレクティブ（`defaultDirectives` より後に上書き適用） | 実装内部ロジック | `FixedLengthFile.java` 行16-27 | テスト追加必要（`fixedLengthDirectives` DI の明示テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（`fixedLengthDirectives` DI はランタイム設定） |
| DR-06 | `variableLengthDirectives` DI: 可変長ファイル専用デフォルトディレクティブ | 実装内部ロジック | `VariableLengthFile.java` 行19-31 | テスト追加必要（`variableLengthDirectives` DI の明示テストなし。R-3 で作成予定） | スキーマ外・パーサ実装で担保（`variableLengthDirectives` DI はランタイム設定） |
| DR-07 | `file-type` ディレクティブはサブクラス（固定長=`"Fixed"`、可変長=`"Variable"`）が自動設定するため通常は記述不要 | テストデータ構造 | `DataFile.java` 行83-101、`FixedLengthFile.java` 行29-36 | `FileSupportTest#testSetUpFixedLengthFile`（file-type 自動設定の間接確認） | スキーマ根拠: `$defs.directives.properties.file-type` に説明あり（自動設定のため通常記述不要） |
| DR-08 | `record-length` ディレクティブはフィールド長合計から自動計算されるため通常は記述不要 | テストデータ構造 | `FixedLengthFile.java` 行60-92 | `FileSupportTest#testSetUpFixedLengthFile`（record-length 自動計算の間接確認） | スキーマ根拠: `$defs.directives.properties.record-length` に説明あり（自動計算のため通常記述不要） |
| DR-09 | `field-separator`: 可変長ファイルのデフォルトは `","``。`"\\t"` 指定でタブ文字に変換。値は1文字のみ有効 | テストデータ構造 | `VariableLengthFile.java` 行16-82 | `FileSupportTest#testVariation`（field-separator の動作） | スキーマ根拠: `$defs.directives.properties.field-separator` の説明（省略時はカンマ、`\\t` でタブ変換）（`design.md §ディレクティブの field-separator`） |
| DR-10 | `record-separator`: `NONE`/`CR`/`LF`/`CRLF` または任意リテラル文字列が有効 | テストデータ構造 | `LineSeparator.java`、`DataFile.java` 行318-334 | `LineSeparatorTest#testToString`, `LineSeparatorTest#testEvaluate`（record-separator の評価） | スキーマ根拠: `$defs.directives.properties.record-separator` の説明（NONE/CR/LF/CRLF またはリテラル）（`design.md §ディレクティブの record-separator`） |
| DR-11 | `field-separator` ディレクティブに `\t`（タブ変換対象）以外の2文字以上の文字列を指定すると `IllegalArgumentException("field-separator must be one character. but was ...")` をスロー | 実装内部ロジック | `VariableLengthFile.java` 行76-80（`convertDirectiveValue` 内の長さチェック） | テスト追加必要（`field-separator` に2文字以上の文字列を指定したときの例外を明示するテストなし） | スキーマ外・パーサ実装で担保（ディレクティブ値の長さバリデーションはランタイム処理） |

---

### MS: メッセージングテストデータ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） | 既存テストメソッド or テスト追加必要 | スキーマ根拠 or スキーマ外理由 |
|---|---|---|---|---|---|
| MS-01 | FW 制御ヘッダフィールドのデフォルト4種: `requestId` / `userId` / `resendFlag` / `resultCode`。`reader.fwHeaderfields` キーで変更可能 | テストデータ構造 | `MessageParser.java` 行95-110 | `MessageParserTest#testParseRequestMessage`（FW制御ヘッダ4種） | スキーマ根拠: `$defs.message_data.records` の `record_fragment` 内のフィールドが FW ヘッダ含む構造。`design.md §1` Excel概念→YAML構造 |
| MS-02 | `no` 列（先頭列、列番号0）はフレームワークが除去し、データとして保存されない。`errorMode` 値は列番号1に格納される | テストデータ構造 | `SendSyncMessageParser.java` 行94-134 | `SendSyncMessageParserTest#testGetFwHeader`（no列とerrorMode列の扱い） | スキーマ外・パーサ実装で担保（no列除去とerrorMode解釈はパーサ実装。`design.md §18` SendSyncSupport の配置規則） |
| MS-03 | `MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は常に内部で `"default"` に置き換えられる（装飾的なメタデータとして任意の値を書いてよい） | テストデータ構造 | `MessageParser.java` 行60-67 | `MessageParserTest#testParseRequestMessage`（record_type を "default" に置き換え） | スキーマ根拠: `$defs.record_fragment.record_type` の説明（`design.md §12` MESSAGE系の record_type は装飾的） |
| MS-04 | `errorMode:timeout` および `errorMode:msgException` は `no` 列の次（列番号1）に配置する特殊値。他フィールドはパースされない | テストデータ構造 | `SendSyncMessageParser.java` 行18-44、116-132 | テスト追加必要（`SendSyncMessageParserTest` が `testGetFwHeader` 1メソッドしかなく、errorMode:timeout/msgException の具体的テストなし） | スキーマ外・パーサ実装で担保（errorMode 特殊値はパーサ実装） |
| MS-05 | `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（rows 合計）は一致が必須。不一致は `IllegalStateException`（旧E-7） | テストデータ構造 | `RequestTestingMessagingClient.java` 行294-443 | テスト追加必要（行数不一致の `IllegalStateException` を YAML テストデータで確認するテストなし） | スキーマ外仕様・テストで担保する方針（行数一致チェックはランタイム。`design.md §11`） |
| MS-06 | `GroupMessageParser`: 同一 groupId の複数メッセージプールを収集。セクション識別子 `=` 以降をリクエストIDとして使用 | テストデータ構造 | `GroupMessageParser.java` 行48-65 | テスト追加必要（`GroupMessageParser` の複数メッセージ収集を明示するテストなし） | スキーマ根拠: `$defs.group_message_data` の `group_id` フィールドが groupId 収集を表現 |
| MS-07 | `sendSyncTestData/{requestId}/message` の配置規則: テストデータファイルは `sendSyncTestData` ベースパス下にリクエストIDと同名ファイルとして配置する（旧E-5） | テストデータ構造 | `SendSyncSupport.java` 行39-49 | テスト追加必要（`sendSyncTestData/{requestId}/message` 配置規則の YAML 動作確認テストなし） | スキーマ外仕様・テストで担保する方針（配置規則はファイルシステムの話。`design.md §18`） |
| MS-08 | ステータスコード列がない場合はデフォルト `"200"` が使用される | テストデータ構造 | `RequestTestingMessagingClient.java` 行124-204 | テスト追加必要（ステータスコード列なし時のデフォルト "200" を明示するテストなし） | スキーマ外・パーサ実装で担保（ステータスコードデフォルト "200" はパーサ実装） |
| MS-09 | マルチレコード送信時: ヘッダ行数とボディ行数を一致させる。N 回送信の場合は各 N 行記述（公式解説書 Doc-13） | テストデータ構造 | 公式解説書 send_sync.rst | テスト追加必要（マルチレコード送信の行数一致を明示するテストなし） | スキーマ外仕様・テストで担保する方針（行数一致ルールは運用規約。`design.md §AI向けプロンプト補助情報 messaging の追加注意事項`） |
| MS-10 | `no` 列と複数回送信: 同一リクエストIDで複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させる（公式解説書 Doc-14） | テストデータ構造 | 公式解説書 send_sync.rst | テスト追加必要（no値変更による複数回送信を明示するテストなし） | スキーマ外仕様・テストで担保する方針（no値による複数回送信は運用規約） |
| MS-11 | HTTP同期応答メッセージ送信処理のボディ行長制約: `response_body_messages` の各データ行の文字列長が同一であることが必要（JSON/XML形式使用時の制約） | テストデータ構造 | 公式解説書 http_send_sync.rst（Doc-15）、`design.md §11` | テスト追加必要（HTTP同期応答ボディ行長制約を明示するテストなし） | スキーマ外仕様・テストで担保する方針（ボディ行長制約は運用制約。`design.md §11`） |
| MS-12 | フォーマット定義ファイルの命名規則: 応答電文は `{requestId}_RECEIVE`、要求電文は `{requestId}_SEND` | テストデータ構造 | `RequestTestingMessagingClient.java` 行75-79、`design.md §20` | テスト追加必要（フォーマット定義ファイル命名規則を直接テストするものなし） | スキーマ根拠: `design.md §20` フォーマット定義ファイルの命名規則 |
| MS-13 | `messaging.assertAsMapFileType` キー: SystemRepository から未設定時はデフォルト `"Fixed"` 形式で項目単位アサート。値により文字列全体アサートに切り替え可能 | テストデータ構造 | `RequestTestingMessagingClient.java` 行81-83、`design.md §19` | テスト追加必要（`messaging.assertAsMapFileType` キーの動作を明示するテストなし） | スキーマ外・パーサ実装で担保（`messaging.assertAsMapFileType` キー参照はパーサ実装。`design.md §19`） |
| MS-14 | `SendSyncMessageParser#getFwHeader()` 呼び出し禁止: このメソッドは `UnsupportedOperationException("unsupported method was called.")` をスロー。`SendSyncMessageParser` の利用者は `getFwHeader()` ではなく `getSendSyncMessageList()` を使う必要がある | 実装内部ロジック | `SendSyncMessageParser.java` 行43（`getFwHeader()` で `UnsupportedOperationException` スロー） | `SendSyncMessageParserTest#testGetFwHeader`（`getFwHeader()` で `UnsupportedOperationException` が発生すること） | スキーマ外・パーサ実装で担保（禁止メソッドの呼び出し防止はランタイムチェック） |
| MS-15 | メッセージ未存在時の `null` 返却: `getMessageWithoutCache()`/`getMessage()` で指定 ID のメッセージが見つからないとき `null` を返す。`buildSendSyncMessageList()` も groupId が見つからないとき `null` を返す。呼び出し元が null チェックの責任を持つ | 実装内部ロジック | `MessageParser.java` 行129（`delegate.getResult()` が空リスト → null 返却）、`YamlMessageBuilderTest#testBuildMessagePool_idNotFound`、`YamlMessageBuilderTest#testBuildSendSyncMessageList_groupIdNotFound`、`YamlMessageBuilderTest#testBuildMessageFile_idNotFound` | `YamlMessageBuilderTest#testBuildMessagePool_idNotFound`（ID未存在 → null 返却）、`YamlMessageBuilderTest#testBuildSendSyncMessageList_groupIdNotFound`（groupId未存在 → null 返却）、`YamlMessageBuilderTest#testBuildMessageFile_idNotFound`（ID未存在 → null 返却） | スキーマ外・パーサ実装で担保（メッセージ未存在時の null 返却はパーサ実装。Javadoc に null 返却時の呼び出し元責任を明記） |

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

| カテゴリ | 仕様ID数 | テストデータ構造 | 実装内部ロジック |
|---|---|---|---|
| DT | 7件（DT-01〜DT-07） | 7件 | 0件 |
| SS | 25件（SS-01〜SS-25） | 20件 | 5件（SS-21〜SS-25） |
| RS | 13件（RS-01〜RS-13） | 0件 | 13件 |
| HC | 7件（HC-01〜HC-07） | 7件 | 0件 |
| IV | 16件（IV-01〜IV-16） | 15件 | 1件（IV-16） |
| DR | 11件（DR-01〜DR-11） | 8件 | 4件（DR-04〜DR-06/DR-11） |
| MS | 15件（MS-01〜MS-15） | 13件 | 2件（MS-14〜MS-15） |
| **合計** | **94件** | **70件** | **25件** |

**注**: I-4 で異常系仕様 14件（SS-21〜25 / IV-16 / DR-11 / MS-14〜15 / RS-09〜13）を追加。総仕様ID数は 80件 → 94件に更新。

### I-2: 既存テストメソッドマッピング サマリー（I-4 更新版）

| カテゴリ | 仕様ID数 | 既存テストあり | テスト追加必要 |
|---|---|---|---|
| DT | 7件 | 5件（DT-01/02/04/05/06） | 2件（DT-03/07） |
| SS | 25件 | 17件（SS-01〜03/06〜10/12〜14/15〜18/20/21） | 8件（SS-04/05/11/19/22/23/24/25） |
| RS | 13件 | 7件（RS-03/06/09〜13） | 6件（RS-01/02/04/05/07/08、YamlTestDataReader 未実装） |
| HC | 7件 | 5件（HC-01〜05） | 2件（HC-06/07） |
| IV | 16件 | 10件（IV-01/02/04〜08/12〜14） | 6件（IV-03/09〜11/15/16） |
| DR | 11件 | 6件（DR-01/02/07〜10） | 5件（DR-03〜06/11） |
| MS | 15件 | 6件（MS-01〜03/14〜15） | 9件（MS-04〜13） |
| **合計** | **94件** | **56件** | **38件** |

### I-3: スキーマ根拠マッピング サマリー（I-4 更新版）

| カテゴリ | 仕様ID数 | スキーマ根拠あり | スキーマ外（パーサ実装/テスト担保） |
|---|---|---|---|
| DT | 7件 | 6件（DT-01/02/04/05/06/07） | 1件（DT-03） |
| SS | 25件 | 12件（SS-01〜03/06〜12/15/17） | 13件（SS-04/05/13/14/16/18〜25） |
| RS | 13件 | 0件 | 13件（全件スキーマ外） |
| HC | 7件 | 2件（HC-01/04） | 5件（HC-02/03/05〜07） |
| IV | 16件 | 12件（IV-01〜08/12〜15） | 4件（IV-09〜11/16） |
| DR | 11件 | 7件（DR-01〜03/07〜10） | 4件（DR-04〜06/11） |
| MS | 15件 | 4件（MS-01/03/06/12） | 11件（MS-02/04/05/07〜11/13〜15） |
| **合計** | **94件** | **43件** | **51件** |

---

## 抜け漏れ確認

本仕様一覧は以下の4つの調査結果を統合して作成した。全件をカバーしていることを確認した。

| 調査元 | 仕様数 | 取り込み状況 |
|---|---|---|
| `ntf-coverage-spec-mapping.md`（コード全行走査 29クラス） | S-1〜S-5 / D-1〜D-16 / E-1〜E-4 | 全件取り込み済み。D-10→SS-20 として追加（QA指摘NG-1対応） |
| `ntf-coverage-doc-check.md`（公式解説書照合 13本） | Doc-1〜Doc-17（うち反映対象17件） | 全件取り込み済み。Doc-5→DT-06、Doc-8→IV-14、Doc-12→IV-15、Doc-15→MS-11（QA指摘対応） |
| `ntf-testdata-yaml-design.md`（スキーマ設計・設計上の注意点） | 27項目（§1〜§27） | 全件取り込み済み。§19→MS-13、§20→MS-12 として追加（QA指摘対応） |
| E-1〜E-9 | 9件 | 全件処置済み（8件昇格・1件ドキュメント修正のみ） |
| I-4: 既存 Excel 系実装の異常系走査（`BasicTestDataParser` / `DataFileParser` / `TableData` / `DataFileFragment` / `FixedLengthFile` / `VariableLengthFile` / `MessageParser` / `SendSyncMessageParser`）および YAML 実装の異常系テスト（`YamlTableDataBuilderTest` / `YamlFileBuilderTest` / `YamlMessageBuilderTest`）| 13件（SS-21〜24 / IV-16 / DR-11 / MS-14〜15 / RS-09〜13） | 全件取り込み済み。R-1-refactor で追加した全テスト（table 欠如・path 欠如・FW_HEADER rows 型誤り・rows 空・ID/groupId 未存在・DataType 誤り）が仕様IDに対応づけられたことを確認済み |
