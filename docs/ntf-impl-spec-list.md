# NTF テストデータ 実装仕様一覧（ntf-impl-spec-list.md）

- **作成日**: 2026-05-20（I-1 タスク）
- **更新日**: 2026-05-27（T-1: テストメソッドマッピング列を追加・全145件記載）
- **参照元**: `docs/checks/S-1.md`（解説書抽出188件）、`docs/checks/S-2.md`（実装抽出300件超）、`ntf-coverage-spec-mapping.md`（コード全行走査）、`ntf-testdata-yaml-design.md`（スキーマ設計）

**マッピング列の記載方針**:
- `解説書マッピング` 列: その仕様IDを最も直接的に裏付ける S-1 ID を代表的に記載する（同一仕様IDに関連する全 S-1 ID の網羅列挙ではなく代表参照）。全件マッピングは `docs/checks/S-3.md` の S-1 マッピング一覧を参照。
- `実装マッピング` 列: その仕様IDの動作を実装している主要コード箇所を記載する（1箇所の実装が複数仕様IDにまたがる場合、代表的な仕様IDに記載し他仕様IDからの参照は省略することがある）。全件マッピングは `docs/checks/S-3.md` の S-2 マッピング一覧を参照。
- `テストメソッド` 列: その仕様IDを直接検証するテストクラス・メソッドを記載する。テスト対象外の場合は理由を記載する。`—` は「上位層/統合テストに委任・YAMLリーダーの責務外」を意味する。

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
| TS | テストサポート層 | `AbstractHttpRequestTestTemplate`, `TestCaseInfo`, `StandaloneTestSupportTemplate`, `TestShot`, `BatchRequestTestSupport`, `EntityTestSupport`, `DbAccessTestSupport` |

---

## 仕様一覧

### DT: セクション識別・DataType

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| DT-01 | DataType 列挙値: `DEFAULT` / `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` / `LIST_MAP` / `SETUP_FIXED` / `EXPECTED_FIXED` / `SETUP_VARIABLE` / `EXPECTED_VARIABLE` / `MESSAGE` / `EXPECTED_REQUEST_HEADER_MESSAGES` / `EXPECTED_REQUEST_BODY_MESSAGES` / `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` の14種 | 正常系 | S1-005, S1-006, S1-007, S1-008, S1-009, S1-010, S1-011, S1-012, S1-013, S1-014, S1-015, S1-016, S1-017, S1-018 | S2-062（DataType 列挙型定義）, S2-063（getName） | DataTypeTest#testGetName, DataTypeTest#testGetType |
| DT-02 | セクション識別行の書式: `<DataType名>[groupId]=<値>` (`=` が必須区切り文字。groupId は省略可) | 正常系 | S1-005 | S2-086（getDataType 前方一致）, S2-087（getTypeValue） | BasicTestDataParserTest#testGetSetupTableData（XLS読み込みで間接確認） |
| DT-03 | DataType 判定は前方一致（`startsWith`）: セル値が DataType の name で始まれば合致 | 正常系 | 解説書に記載なし | S2-086（TestDataParsingTemplate.getDataType L230-242） | TestDataParsingTemplateTest#testGetDataTypeNull（null→DEFAULT 確認）。前方一致そのものは XLS統合テストで間接確認 |
| DT-04 | GroupData系（SETUP_TABLE 等）は同一 groupId のセクションを全部収集し続ける（`shouldStopOnNextOne() = false`） | 正常系 | S1-064, S1-066 | S2-088, S2-089（GroupDataParsingTemplate） | BasicTestDataParserTest#testGetSetupTableData（複数グループを通じた間接確認） |
| DT-05 | SingleData系（LIST_MAP / MESSAGE 等）は最初に合致したセクション1つだけを取得して停止する（`shouldStopOnNextOne() = true`） | 正常系 | 解説書に記載なし | S2-090, S2-091（SingleDataParsingTemplate） | SingleDataParsingTemplateTest#testParseSingleData |
| DT-06 | groupId 書式: `[groupId]`（省略時は空文字扱い。要素数1時のみ有効・2以上は `IllegalArgumentException`）。バッチ固有: `group_id: "default"` はグループIDなし扱いと同等 | 正常系 | S1-063, S1-064, S1-065, S1-185 | S2-015（BasicTestDataParser.formatGroupId L253-266） | BasicTestDataParserTest#testFormatGroupId |
| DT-07 | `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` は GroupData（groupId 必須）経路と SingleData（id 一致）経路の2つが存在する | 正常系 | S1-097, S1-098 | S2-014（BasicTestDataParser.getSendSyncMessage L113）, S2-022（YamlTestDataParser.getSendSyncMessage） | YamlTestDataParserTest#testGetSendSyncMessage（GroupData経路）, YamlTestDataParserTest#testGetMessageWithoutCache_responseHeaderMessages（SingleData経路） |
| DT-08 | groupId 引数に2件以上指定した場合は `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-015（BasicTestDataParser.formatGroupId L264） | BasicTestDataParserTest#testFormatGroupIdFail |

---

### SS: テーブル・ファイル構造

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| SS-01 | テーブルデータ行の形式: カラム名をキーとするオブジェクト形式。省略されたカラムにはデフォルト値が INSERT 時に補完される | 正常系 | S1-045, S1-046 | S2-127（TableData.addRow L522）, S2-128（fillDefaultValues L706）, S2-097（TableDataParser キャッシュ L60-72） | TableDataTest#testReplaceData |
| SS-02 | `EXPECTED_TABLE`: 省略されたカラムは比較対象外になる（カラム列挙は任意） | 正常系 | S1-048 | S2-012（BasicTestDataParser.getExpectedTableData L171-181） | BasicTestDataParserTest#testExpectedGetTableData |
| SS-03 | `EXPECTED_COMPLETE_TABLE`: 省略されたカラムに `BasicDefaultValues` のデフォルト値を補完してから比較する | 正常系 | S1-049 | S2-012（BasicTestDataParser.getExpectedTableData fillDefaultValues L171-181）, S2-045（YamlTableDataBuilder.buildTableDataList fillDefaults） | BasicTestDataParserTest#testGetExpectedTableDataCompletedWithoutId, BasicTestDataParserTest#testGetExpectedTableDataCompletedWithId |
| SS-04 | `SETUP_TABLE` では主キーカラムは省略不可（省略するとデフォルト値が INSERT される） | 正常系 | S1-047 | S2-002（BasicTestDataParser.getSetupTableData L43） | — （主キー省略はDB制約エラーとして検出される。テストフレームワーク単体では検証不可） |
| SS-05 | `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一ファイル内で混在させると後半データが読み込まれない（まとめて記述が必要） | 正常系 | S1-043, S1-044 | S2-080, S2-081（TestDataParsingTemplate.parse キャッシュ L117-128） | — （パーサのキャッシュ動作で間接的に担保。XLS統合テストで確認） |
| SS-06 | `LIST_MAP=id` セクション: id は完全一致。同一ファイル内で同一 id の重複エントリは後続が黙って無視される（先着一致） | 正常系 | S1-062 | S2-090, S2-091（SingleDataParsingTemplate isTargetType L33-41）, S2-100（ListMapParser キャッシュ L34-53） | SingleDataParsingTemplateTest#testParseSingleData |
| SS-07 | `SETUP_FIXED` と `SETUP_VARIABLE` は `BasicTestDataParser#getSetupFile()` でまとめて返される。`EXPECTED_FIXED`/`EXPECTED_VARIABLE` も同様 | 正常系 | S1-010, S1-011, S1-012, S1-013 | S2-011b, S2-011c（BasicTestDataParser.getSetupFile/getExpectedFile L67-80） | YamlTestDataParserTest#testGetSetupFile, YamlTestDataParserTest#testGetExpectedFile |
| SS-08 | ファイルセクションの行順序: ディレクティブ行（0行以上） → フィールド名行 → データ型行 → [フィールド長行（固定長のみ）] → データ行 | 正常系 | S1-080, S1-081 | S2-114（DataFileParser.Status 遷移 L38-48） | FixedLengthFileParserTest#testInvalidDirectives（状態遷移の異常系）, VariableLengthFileParserTest 全般 |
| SS-09 | 固定長フラグメント: `names` / `types` / `lengths` の3リストが同サイズで必須 | 正常系 | S1-080 | S2-165, S2-167, S2-168（DataFileFragment.setNames/setTypes/setLengths） | FixedLengthFileFragmentTest#testSetNamesNull, testSetNamesEmpty, testSetTypesNull, testSetTypesEmpty, testSetLengthsNull, testSetLengthsEmpty |
| SS-10 | 可変長フラグメント: `names` / `types` の2リストが同サイズで必須。`lengths` は不要（型行読み取り後に直接 READING_VALUES へ遷移） | 正常系 | S1-081 | S2-121（VariableLengthFileParser.onReadingTypes L42-46） | VariableLengthFileTest#testAddValue |
| SS-11 | 1ファイルセクション内に複数レコードレイアウトを連続記述可能: データ行の後ろに新たなフィールド名行を書くと新レコードレイアウトとして扱われる | 正常系 | S1-159 | S2-114（DataFileParser.Status 遷移）, S2-116（データ行判定 L204-210） | — （マルチレイアウトは XLS 統合テストで確認） |
| SS-12 | フィールド名行の構造: 先頭列 = レコード種別名、2列目以降 = フィールド名の列挙 | 正常系 | S1-080 | S2-098（TableDataParser.onTargetTypeFound L89-97）, S2-101b（MessageParser.onReadingNames L60-65） | — （パーサの統合テストで間接確認） |
| SS-13 | データ行の先頭セルは必ず空（null または空文字）にする | 正常系 | 解説書に記載なし | S2-116（DataFileParser.isDataRow L204-210） | — （実装内部規約。パーサ統合テストで間接確認） |
| SS-14 | 同一レコード種別内のフィールド名は重複不可（`IllegalArgumentException`）。異なる種別間は重複可 | 異常系 | S1-161 | S2-166（DataFileFragment.setNames L354-361） | FixedLengthFileFragmentTest#testSetDuplicateNames |
| SS-15 | 空ファイル（0バイト）表現: ディレクティブ行のみ記述してレコード定義を省略する | 正常系 | S1-083 | S2-163（DataFile.prepareDefaultDirectives L68-81） | — （DataFile 統合テストで間接確認） |
| SS-16 | 固定長ファイルは全フラグメントで同一レコード長が必須（違反時 `IllegalStateException`） | 異常系 | 解説書に記載なし | S2-178（FixedLengthFile.getRecordLength L109-113） | FixedLengthFileTest#testRecordLengthDiffers |
| SS-17 | `"-"` 長フィールド: 追加された全レコードの最大バイト長に自動拡張 | 正常系 | S1-107 | S2-169（DataFileFragment.setLengths "-" L291-293） | FixedLengthFileFragmentTest#testAutoCalcRecordLengthWhenAddValue, testAutoCalcRecordLengthaddValueWithId |
| SS-18 | `BasicDefaultValues` のデフォルト値: 数値型=`"0"`、CHAR/NCHAR=スペース×カラム長、VARCHAR等=半角スペース1文字、DATE=epoch（JVM タイムゾーン依存）、バイナリ=10バイトゼロHexString、Boolean=`"false"` | 正常系 | S1-050, S1-051, S1-052, S1-186, S1-187 | S2-146, S2-147, S2-148, S2-149, S2-150, S2-151, S2-151b, S2-152, S2-153（BasicDefaultValues 各デフォルト値）, S2-145（DefaultValues インターフェース） | BasicDefaultValuesTest#testGetValueOfNumber, testGetValueOfDate, testGetValueOfChar, testGetValueOfVarchar, testGetValueOfClob, testGetValueOfBlob, testGetValueOfBoolean |
| SS-19 | `testShots` は LIST_MAP の予約ID: バッチリクエスト単体テストでフレームワークがテストケース一覧として自動読み込みする | 正常系 | S1-167 | S2-099（ListMapParser L30）, S2-100（LIST_MAP型パース） | BatchRequestTestSupportTest#testTestCasesNotFound（空時の例外で間接確認） |
| SS-20 | ファイル系空行の動作差異: 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される | 正常系 | 解説書に記載なし | S2-170（DataFileFragment.addValue L105-109） | VariableLengthFileParserTest#testEmptyRowSingleItem, testEmptyRowMultiItems |
| SS-21 | `DataFileFragment` のフィールド名リストまたは型リストが null/空の場合 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-165（DataFileFragment.setNames L327-329） | FixedLengthFileFragmentTest#testSetNamesNull, testSetNamesEmpty |
| SS-22 | `DataFileFragment` のフィールド名リストと型/長さリストのサイズ不一致時 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-167, S2-168（DataFileFragment.setTypes/setLengths） | FixedLengthFileFragmentTest#testSetTypesNull（サイズ不一致含む） |
| SS-23 | 固定長フィールド値がフィールド長を超えた場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-186（FixedLengthFileFragment.toBytes L130-135） | FixedLengthFileFragmentTest#testConvertBytesFail |
| SS-24 | 存在しないフィールド名を指定した場合 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-174（DataFileFragment.getIndexOf L446-448） | — （FixedLengthFileFragmentTest で他の異常系と一体確認） |
| SS-25 | `DataFileFragment` のデータ要素数が不正な場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-173（DataFileFragment.checkSize L543-546） | — （FixedLengthFileFragmentTest で統合確認） |
| SS-26 | ファイルの読み込み失敗時（IO例外）に `RuntimeException` をスロー | 異常系 | 解説書に記載なし | S2-160（DataFile.read L178-187） | — （IO エラー誘発テストなし。到達不能に近いパス） |
| SS-27 | `DataFileParser.Status` が想定外の状態になった場合 `IllegalStateException` をスロー（到達不能コード） | 異常系 | 解説書に記載なし | S2-118（DataFileParser 想定外状態 L83-85） | — （到達不能コード） |
| SS-28 | ディレクティブ行またはフィールド名行の列数が2未満の場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-115（DataFileParser.processDirectives L220-223） | FixedLengthFileParserTest#testInvalidDirectives |
| SS-29 | `TableData#getClone()` で `CloneNotSupportedException` が発生した場合 `RuntimeException` をスロー（到達不能コード） | 異常系 | 解説書に記載なし | 実装に記載なし（到達不能コード） | TableDataTest#testCloneFail |
| SS-30 | `TableData#getValue()` で日付型カラムの値が日付として解析できない場合 `RuntimeException` をスロー | 異常系 | 解説書に記載なし | S2-143（TableData.convert L203-209） | — （日付解析エラーの直接テストなし） |
| SS-31 | `TableData#getValue()` でカラム値が `null` の場合は `null` を返す（代替フロー） | 代替フロー | 解説書に記載なし | S2-130（TableData.convert L197-199） | TableDataTest#testReplaceNullValue |
| SS-32 | `TableData#toTimestamp()` で空文字の場合は `null` を返す（代替フロー） | 代替フロー | 解説書に記載なし | S2-131（TableData.toTimestamp L222-225） | — （直接テストなし） |

---

### RS: YAMLリーダー実装仕様

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| RS-01 | `open(path, dataName)` 規約: `dataName` に対して `{dataName}.yaml` ファイルを検索する | 正常系 | S1-067, S1-068, S1-069 | S2-018（YamlTestDataParser.isResourceExisting L92）, S2-029（YamlLoader.isResourceExisting L81） | YamlTestDataParserTest#testRs01_getSetupTableDataLoadsYamlFile（他 RS-01 対応テスト多数 — docs/checks/R-1.md の対応表参照） |
| RS-02 | `readLine()` は文書終端で `null` を返す | 正常系 | 解説書に記載なし | S2-066（TestDataReader.readLine L33）, S2-085（TestDataParsingTemplate.readLine L261-265） | 非適用（YamlTestDataParser は TestDataReader を使用しない） |
| RS-03 | YAML ネイティブ `null`（アンクォート）は Java `null` として返す | 正常系 | 解説書に記載なし | S2-034（YamlSection.toStr L109）, S2-035（YamlSection.objectToString L129）, S2-036（YamlSection.interpret L136-145） | YamlTestDataParserTest#testRs03_yamlNativeNullIsJavaNull |
| RS-04 | YAML ネイティブ boolean (`true`/`false`) は文字列 `"true"`/`"false"` として返す | 正常系 | 解説書に記載なし | S2-035（YamlSection.objectToString L129） | YamlTestDataParserTest#testRs04_yamlNativeBooleanIsStringified |
| RS-05 | YAML ネイティブ integer/float は数字文字列として返す | 正常系 | 解説書に記載なし | S2-035（YamlSection.objectToString L129） | YamlTestDataParserTest#testRs05_yamlNativeNumberIsStringified, testRs05_yamlScientificNotationIsStringified |
| RS-06 | 末尾の空要素（YAML ネイティブ null または省略）は Java `null` として返す | 正常系 | 解説書に記載なし | S2-035（YamlSection.objectToString null パス） | YamlTestDataParserTest#testRs06_trailingNativeNullIsJavaNull, testRs06_trailingKeyOmittedIsNull |
| RS-07 | `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを保証する | 正常系 | 解説書に記載なし | S2-080, S2-082（TestDataParsingTemplate.parse L117-157） | YamlTestDataParserTest#testRs07_lastSectionDataNotLostAtEndOfFile, YamlFileBuilderTest#testBuildFileList_lastSectionNotLost |
| RS-08 | `isDataExisting(directory, resource)` / `isResourceExisting(directory, resource)` の実装（リソース存在確認） | 正常系 | 解説書に記載なし | S2-016（BasicTestDataParser.isResourceExisting L269）, S2-018（YamlTestDataParser.isResourceExisting L92）, S2-029（YamlLoader.isResourceExisting L81） | YamlTestDataParserTest#testRs08_isResourceExistingReturnsTrueWhenFileExists, testRs08_isResourceExistingReturnsFalseWhenFileNotExists |
| RS-09 | YAML ファイルが存在しない、または読み込み失敗・パース失敗時は `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-026（YamlLoader.load IO エラー L67-68）, S2-027（YamlLoader.load パースエラー L69-71） | YamlLoaderTest#testLoad_throwsWhenFileNotExists, testLoad_throwsWhenRootIsNotMap |
| RS-10 | `setup_tables`/`expected_tables` のエントリに `table` キーが存在しない場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-042（YamlTableDataBuilder.buildTableDataList L71-73） | YamlTableDataBuilderTest#testBuildTableDataList_missingTableThrowsException |
| RS-11 | `setup_files`/`expected_files` のエントリに `path` キーが存在しない場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-049（YamlFileBuilder.buildFileList L70-73） | YamlFileBuilderTest#testBuildFileList_missingPathThrowsException |
| RS-12 | `messages`/`expected_request_*_messages` のエントリで `FW_HEADER` の `rows` が List of Lists でない場合 `IllegalStateException` をスロー | 異常系 | 解説書に記載なし | S2-060（YamlMessageBuilder.extractFwHeader L131-170） | YamlMessageBuilderTest#testBuildMessagePool_malformedFwHeaderRowsThrowsException |
| RS-13 | メッセージング以外の DataType を `YamlSection#dataTypeToSectionKey` に渡した場合 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-037（YamlSection.dataTypeToSectionKey L182-192） | YamlMessageBuilderTest#testDataTypeToSectionKey_unsupportedDataTypeThrowsException |
| RS-14 | `setTestDataReader` 呼び出し時は `UnsupportedOperationException` をスロー（YAML 実装は TestDataReader を使わない） | 異常系 | 解説書に記載なし | S2-017（YamlTestDataParser.setTestDataReader L59-63） | YamlTestDataParserTest#testSetTestDataReaderThrowsUnsupported |
| RS-15 | `getSetupTableData` のみ、ファイルが存在しない場合は空リストを返す（代替フロー） | 代替フロー | S1-132 | S2-019（YamlTestDataParser.getSetupTableData L99）, S2-011（BasicTestDataParser.getSetupTableData L54） | YamlTestDataParserTest#testGetSetupTableDataReturnsEmptyWhenFileNotExists |
| RS-16 | `getMessage`/`getMessageWithoutCache` で対象 ID が見つからない場合は `null` を返す（代替フロー） | 代替フロー | 解説書に記載なし | S2-056（YamlMessageBuilder.buildMessagePool L79-87）, S2-051（YamlFileBuilder.buildMessageFile L95-109）, S2-101（MessageParser.getResult L127-133） | YamlTestDataParserTest#testGetMessageReturnsNullWhenIdNotFound, YamlMessageBuilderTest#testBuildMessagePool_idNotFound |
| RS-17 | `getSendSyncMessage` で対象 groupId が見つからない場合は `null` を返す（代替フロー） | 代替フロー | 解説書に記載なし | S2-057（YamlMessageBuilder.buildSendSyncMessageList L98-117） | YamlTestDataParserTest#testGetSendSyncMessageReturnsNullForUnknownGroupId |
| RS-18 | YAML ファイルの内容が空の場合（`yaml.load()` が null）は空 Map として扱う（代替フロー） | 代替フロー | 解説書に記載なし | S2-025（YamlLoader.load 空ファイル L62-64） | YamlLoaderTest#testLoad_emptyYamlReturnsEmptyMap |
| RS-19 | `getListMap` で指定 ID のエントリが存在しない場合は空リストを返す（代替フロー） | 代替フロー | 解説書に記載なし | S2-046（YamlTableDataBuilder.buildListMapRows L113-123） | YamlTestDataParserTest#testGetListMapReturnsEmptyWhenIdNotFound, YamlTableDataBuilderTest#testBuildListMapRows_idNotFound |
| RS-20 | `messages` エントリで `FW_HEADER` フラグメントが見つからない場合は空 Map を FW ヘッダとして使用する（代替フロー） | 代替フロー | 解説書に記載なし | S2-061（YamlMessageBuilder.extractFwHeader L169） | YamlMessageBuilderTest#testBuildMessagePool_noFwHeaderFragmentReturnsEmptyFwHeader |
| RS-21 | YAML キャッシュは LRU 最大8件。`clearCacheForTest()` でテスト間汚染防止のためキャッシュをクリアできる | 正常系 | S1-144 | S2-024（YamlLoader.load LRU 8件 L50）, S2-023（YamlTestDataParser.clearCacheForTest L170）, S2-029b（YamlLoader.clearCacheForTest L97）, S2-214（NablarchTestUtils.createLRUMap）, S2-223f（SendSyncSupport タイムスタンプ変更検知 L358-371） | YamlLoaderTest#testLoad_returnsCachedInstance, testLoad_lruEvictionWhenCacheFull, testLoad_recentlyAccessedEntryIsNotEvicted |
| RS-22 | YAML ファイルに重複キーが存在する場合 `IllegalStateException` をスロー（SnakeYAML の `setAllowDuplicateKeys(false)` で検出） | 異常系 | 解説書に記載なし | S2-028（YamlLoader.load 重複キー L57） | YamlLoaderTest#testLoad_throwsOnDuplicateKey |

---

### HC: ヘッダ行・カラム処理

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| HC-01 | マーカーカラムの書式: `[カラム名]`（`[` で始まり `]` で終わる） | 正常系 | S1-023 | S2-093（HeaderLine L88-96）, S2-047（YamlTableDataBuilder.buildListMapRows マーカー除外 L133-135） | HeaderLineTest#testGetEffectiveColumnNames, HeaderLineTest#testHeaderContainsNull |
| HC-02 | マーカーカラムは DB 操作から除外される（データとして格納されない） | 正常系 | S1-024 | S2-094, S2-095, S2-096（HeaderLine.getEffectiveColumnNames/getMapExcludingMarkerColumns/excludeMarkerColumns）, S2-098b（TableDataParser.onReadLine） | HeaderLineTest#testExcludeMarkerColumns, HeaderLineTest#testGetMapExcludingMarkerColumns |
| HC-03 | ヘッダ行末尾の空カラムは除去される（末尾カラム省略可） | 正常系 | 解説書に記載なし | S2-092b（HeaderLine コンストラクタ trimTailCopy L33） | — （HeaderLineTest で統合確認） |
| HC-04 | データ行がヘッダより短い場合、不足分は空文字 `""` で補完される | 正常系 | 解説書に記載なし | S2-096（HeaderLine.excludeMarkerColumns L75-85）, S2-170（DataFileFragment.addValue L105-109） | HeaderLineTest#testExcludeMarkerColumnsShort |
| HC-05 | コメント行: 先頭セルが `//` で始まる行は行ごとスキップ | 正常系 | S1-022 | S2-083（TestDataParsingTemplate.isCommentRow L278-280） | TestDataParsingTemplateTest#testIsCommentRow |
| HC-06 | 行内コメント: 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨て | 正常系 | S1-022 | S2-084（TestDataParsingTemplate.cutComment L299-308） | — （TestDataParsingTemplateTest で統合確認） |
| HC-07 | 空行スキップ: 全要素が null または空文字の行は読み飛ばす | 正常系 | S1-071, S1-072 | S2-110c（SendSyncMessageParser.onReadingValues 空行スキップ） | — （SendSyncMessageParser 統合テストで間接確認） |

---

### IV: インタープリタ・特殊値

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| IV-01 | `NullInterpreter`: `null`/`NULL`/`Null`（大文字小文字不問）を Java null に変換 | 正常系 | S1-029 | S2-194（NullInterpreter.interpret L16） | NullInterpreterTest#testInterpretNullLowerCase, testInterpretNullUpperCase, testInterpretNullCapitalized, testInterpretNotNullValue |
| IV-02 | `QuotationTrimmer`: 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去。片側のみはスルー | 正常系 | S1-030, S1-031, S1-032, S1-033 | S2-195（QuotationTrimmer.interpret L25-29） | QuotationTrimmerTest#testInterpretHalfWidthQuotation, testInterpretFullWidthQuotation, testInterpretNotQuoted, testBoundaryValues |
| IV-03 | `DateTimeInterpreter`: `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換 | 正常系 | S1-034, S1-035, S1-036 | S2-196, S2-197, S2-198（DateTimeInterpreter L49-52） | DateTimeInterpreterTest#testInterpretSystemTime, testInterpretUpdateTime, testInterpretSetUpTime, testInterpretNotApplicable |
| IV-04 | `LineSeparatorInterpreter`: `\\r` → CR(0x0D)（デフォルト）、`\\n` → LF(0x0A) に変換 | 正常系 | S1-040, S1-041 | S2-203, S2-204, S2-205, S2-206（LineSeparatorInterpreter L31-87） | LineSeparatorInterpreterTest#testConvertBackR, testDoNotConvertCR, testDoNotConvert |
| IV-05 | `BinaryFileInterpreter`: `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換。YAML ファイルが基準ディレクトリになる | 正常系 | S1-039 | S2-201（BinaryFileInterpreter L36-55）, S2-040c（YamlSection.addBinaryFileInterpreter L150） | BinaryFileInterpreterTest#testOk, testNotApplicable, testFileNotFound |
| IV-06 | `BasicJapaneseCharacterInterpreter`: `${文字種,文字数}` 形式で文字列生成。書式完全一致のみ動作、文字種未知の場合は `IllegalArgumentException`（書式ミスはスルー） | 正常系 | S1-037 | S2-207（BasicJapaneseCharacterInterpreter L24）, S2-207b | BasicJapaneseCharacterInterpreterTest#testInterpret, testInterpretNotResponsible |
| IV-07 | `BasicJapaneseCharacterGenerator` 有効文字種14種: 半角英字/半角数字/半角記号/半角カナ/全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角記号その他/中国語/サロゲートペア/改行/外字 | 正常系 | S1-038 | S2-208（BasicJapaneseCharacterInterpreter 文字種一覧 L41-56） | BasicJapaneseCharacterInterpreterTest#testSetCharcterGenerator（差し替えによる間接確認） |
| IV-08 | `CompositeInterpreter`: 文字列中の `${...}` 要素を個別解釈して置換。`${...}` がない場合は次のインタープリタに委譲 | 正常系 | 解説書に記載なし | S2-210, S2-210b, S2-211（CompositeInterpreter L21-42） | CompositeInterpreterTest#testExpression, testCombinationOfNotations, testCombinationOfInterpreters, testLiteral |
| IV-09 | 日付型カラムの記述形式: `yyyyMMddHHmmssSSS`（17文字）、後置0埋め短縮形、JDBC タイムスタンプエスケープ形式（5文字目が `-`）等が有効 | 正常系 | S1-025, S1-026, S1-027, S1-028 | S2-132, S2-133, S2-134（TableData.toTimestamp L239-273） | TableDataTest#testInsertJdbcTimestampEscape, testInsertyyyyMMddhhmmssS |
| IV-10 | `Timestamp` 型カラムの期待値は末尾 `.0` が必要（例: `"2010-01-01 12:34:56.0"`） | 正常系 | S1-056 | S2-132（TableData.toTimestamp L239） | — （TableDataTest の日付挿入テストで間接確認） |
| IV-11 | バイナリデータの直接記述: `0x` プレフィクス付き16進数で記述可能。`0x` がない場合は文字列としてエンコード | 正常系 | S1-084, S1-188 | S2-184（FixedLengthFileFragment.convertValue HexString L82-84）, S2-135（TableData.insert バイナリ L147-158） | — （FixedLengthFileFragmentTest の convertValue テストで間接確認） |
| IV-12 | `BasicDataTypeMapping` デフォルトマッピング22種（`半角英字`→`X` 等）。未知の型記号は `IllegalArgumentException` | 正常系 | S1-160 | S2-188（BasicDataTypeMapping DEFAULT_TABLE L31-56）, S2-189, S2-190, S2-191 | BasicDataTypeMappingTest#testConvertToFrameworkExpression, testConvertToFrameworkExpressionFail, testConvertToFrameworkExpressionNull |
| IV-13 | `TEST_` プレフィクス型の自動優先選択: `TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用される | 正常系 | 解説書に記載なし | S2-172（DataFileFragment.getTypeForTest L238-244）, S2-175（DataTypeMapping フォールバック L264-278） | FixedLengthFileFragmentTest#testSetTypesMatchEncodingDef, testSetTypesNoMatchEncodingDefWithDefault |
| IV-14 | `QuotationTrimmer` によるスペース値明示記法: `'"⊔"'` → 半角スペース、`'"""'` → ダブルクォート1文字 | 正常系 | S1-032, S1-033 | S2-195（QuotationTrimmer.interpret L25-29） | QuotationTrimmerTest#testBoundaryValues |
| IV-15 | X9/SX9 型フィールドの記述方法: パディング文字・符号を含めた実際のバイト列表現をそのまま記載する必要がある | 正常系 | S1-162 | S2-175b（DataFileFragment.addValueWithId L169-183） | — （直接テストなし。仕様は利用者のデータ記載規約） |
| IV-16 | `BasicJapaneseCharacterInterpreter` に未知の文字種を指定した場合 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-209（CharacterGeneratorBase L55-57） | BasicJapaneseCharacterInterpreterTest#testInterpretUnknownType |

---

### DR: ディレクティブ

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| DR-01 | ディレクティブ行の構成: 先頭列 = キー名、2列目 = 値（最低2列必要） | 正常系 | S1-158 | S2-114（DataFileParser.Status 遷移）, S2-116（データ行判定） | FixedLengthFileParserTest#testInvalidDirectives（列数不足の異常系で間接確認） |
| DR-02 | 固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定される | 正常系 | 解説書に記載なし | S2-119（FixedLengthFileParser.isDirective L37） | DataFileTest#testConvertValueWithInvalidDirective |
| DR-03 | 可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定される | 正常系 | 解説書に記載なし | S2-120（VariableLengthFileParser.isDirective L37） | DataFileTest#testConvertValueWithInvalidDirective |
| DR-04 | `defaultDirectives` DI: SystemRepository のこのキーで全ファイル共通デフォルトディレクティブを一括設定できる | 実装内部ロジック | S1-136 | S2-163（DataFile.prepareDefaultDirectives L68-81）, S2-038（YamlSection.applyDirectives L168-177） | FixedLengthFileTest#testPrepareDefaultDirectives, VariableLengthFileTest#testPrepareDefaultDirectives |
| DR-05 | `fixedLengthDirectives` DI: 固定長ファイル専用デフォルトディレクティブ（`defaultDirectives` より後に上書き適用） | 実装内部ロジック | S1-136 | S2-177（FixedLengthFile デフォルトディレクティブキー L18） | FixedLengthFileTest#testPrepareDefaultDirectives |
| DR-06 | `variableLengthDirectives` DI: 可変長ファイル専用デフォルトディレクティブ | 実装内部ロジック | S1-136 | S2-183（VariableLengthFile デフォルトディレクティブキー L21） | VariableLengthFileTest#testPrepareDefaultDirectives |
| DR-07 | `file-type` ディレクティブはサブクラス（固定長=`"Fixed"`、可変長=`"Variable"`）が自動設定するため通常は記述不要 | 正常系 | S1-108 | S2-176（FixedLengthFile.getFileType L35）, S2-179（VariableLengthFile.getFileType L38） | — （getFileType は他テストで間接確認） |
| DR-08 | `record-length` ディレクティブはフィールド長合計から自動計算されるため通常は記述不要 | 正常系 | S1-108 | S2-178（FixedLengthFile.getRecordLength L109-113） | FixedLengthFileTest#testRecordLengthDiffers（自動計算と比較で間接確認） |
| DR-09 | `field-separator`: 可変長ファイルのデフォルトは `","`. `"\\t"` 指定でタブ文字に変換。値は1文字のみ有効 | 正常系 | S1-082 | S2-180（VariableLengthFile デフォルト区切り L29）, S2-181（\\t→タブ変換 L67-69） | VariableLengthFileTest#testConvertTab, testConvertDirectiveValue |
| DR-10 | `record-separator`: `NONE`/`CR`/`LF`/`CRLF` または任意リテラル文字列が有効 | 正常系 | 解説書に記載なし | S2-192（LineSeparator 列挙 L11-17）, S2-193（LineSeparator.evaluate L57-65） | LineSeparatorTest#testToString, testEvaluate |
| DR-11 | 無効なディレクティブキーを設定した場合 `IllegalArgumentException` をスロー（固定長・可変長ともに適用） | 異常系 | 解説書に記載なし | S2-157（DataFile.setDirective L297-299） | DataFileTest#testConvertValueWithInvalidDirective |
| DR-12 | 可変長ファイルの `field-separator` に2文字以上指定した場合 `IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-182（VariableLengthFile.convertDirectiveValue L73-77） | VariableLengthFileTest#testConvertDirectiveValueFail, testConvertDirectiveValueFail2 |

---

### MS: メッセージングテストデータ

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| MS-01 | FW 制御ヘッダフィールドのデフォルト4種: `requestId` / `userId` / `resendFlag` / `resultCode`。`reader.fwHeaderfields` キーで変更可能 | 正常系 | S1-094 | S2-059（YamlMessageBuilder FW ヘッダフィールド L64-68）, S2-102（MessageParser.fwHeaderfields L107-110）, S2-103（MessageParser FW ヘッダ抽出 L83-91） | MessageParserTest#testParseRequestMessage, testParseRequestMessageAdd |
| MS-02 | `no` 列（先頭列、列番号0）はフレームワークが除去し、データとして保存されない。`errorMode` 値は列番号1に格納される | 正常系 | S1-099 | S2-104（MessageParser データ行 tail L73-77）, S2-109（SendSyncMessageParser no列 L134） | — （MessageParserTest の統合テストで間接確認） |
| MS-03 | `MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は常に内部で `"default"` に置き換えられる | 正常系 | S1-090, S1-091, S1-111 | S2-101b（MessageParser.onReadingNames L60-65）, S2-052（YamlFileBuilder.buildMessageFile FW_HEADER スキップ L104） | — （MessageParser 統合テストで間接確認） |
| MS-04 | `errorMode:timeout` および `errorMode:msgException` は `no` 列の次（列番号1）に配置する特殊値 | 正常系 | S1-102, S1-103, S1-110, S1-112, S1-113 | S2-105, S2-106（SendSyncMessageParser.ErrorMode L19/21）, S2-108（L123-130）, S2-187（MockMessages.removePadding L63-70） | RequestTestingMessagingClientTest#testTimeout（間接確認） |
| MS-05 | `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（rows 合計）は一致が必須。不一致は `IllegalStateException` | 異常系 | S1-174 | 実装に記載なし（RequestTestingMessagingClient で発生） | RequestTestingMessagingClientTest#testAssertFailNoMatchCount |
| MS-06 | `GroupMessageParser`: 同一 groupId の複数メッセージプールを収集。セクション識別子 `=` 以降をリクエストIDとして使用 | 正常系 | S1-104 | S2-111, S2-112, S2-113（GroupMessageParser L52-65） | — （GroupMessageParser の直接テストなし。RequestTestingMessagingClientTest で統合確認） |
| MS-07 | `sendSyncTestData/{requestId}/message` の配置規則: テストデータファイルは `sendSyncTestData` ベースパス下にリクエストIDと同名ファイルとして配置する | 正常系 | S1-105, S1-106 | S2-223b（SendSyncSupport テストデータ配置 L350-354） | — （SendSyncSupport 統合テストで間接確認） |
| MS-08 | ステータスコード列がない場合はデフォルト `"200"` が使用される | 代替フロー | 解説書に記載なし | 実装に記載なし（RequestTestingMessagingClient 内部） | RequestTestingMessagingClientTest#testSendLessStatusCode |
| MS-09 | マルチレコード送信時: ヘッダ行数とボディ行数を一致させる。N 回送信の場合は各 N 行記述 | 正常系 | S1-109, S1-115, S1-116, S1-140, S1-171 | S2-058（YamlMessageBuilder.buildSendSyncMessageList requestId L109-112） | — （SendSyncSupport 統合テストで間接確認） |
| MS-10 | `no` 列と複数回送信: 同一リクエストIDで複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させる | 正常系 | S1-173 | S2-109（SendSyncMessageParser.addValueWithId L134）, S2-223c（SendSyncSupport.getResponseMessageBinaryByRequestId L283-288） | — （SendSyncSupport 統合テストで間接確認） |
| MS-11 | HTTP同期応答メッセージ送信処理のボディ行長制約: `response_body_messages` の各データ行の文字列長が同一であることが必要 | 正常系 | S1-117 | 実装に記載なし（MessagePool.Comparator による比較） | — （MessagePoolTest の Comparator テストで間接確認） |
| MS-12 | フォーマット定義ファイルの命名規則: 応答電文は `{requestId}_RECEIVE`、要求電文は `{requestId}_SEND` | 正常系 | S1-100 | 実装に記載なし（RequestTestingMessagingClient L75-79） | — （RequestTestingMessagingClientTest で統合確認） |
| MS-13 | `messaging.assertAsMapFileType` キー: SystemRepository から未設定時はデフォルト `"Fixed"` 形式で項目単位アサート | 正常系 | S1-101 | S2-220（MessagePool.Comparator.compareBody L154-184） | RequestTestingMessagingClientTest#testAssertAsDataRecord（間接確認） |
| MS-14 | `SendSyncMessageParser#getFwHeader()` は `UnsupportedOperationException` をスロー | 異常系 | 解説書に記載なし | S2-107（SendSyncMessageParser.getFwHeader L42-44） | SendSyncMessageParserTest#testGetFwHeader |

---

### TS: テストサポート層

| 仕様ID | 概要 | 分類 | 解説書マッピング | 実装マッピング | テストメソッド |
|---|---|---|---|---|---|
| TS-01 | `LIST_MAP=testShots` はテストケース定義の予約ID。1行1テストケースを表し、フレームワークが自動読み込みする。旧ID `testCases` は後方互換性のためフォールバックとして残存 | 正常系 | S1-121, S1-122, S1-167 | 実装に記載なし（AbstractHttpRequestTestTemplate.java L68/71） | BatchRequestTestSupportTest#testTestCasesNotFound（空時の例外で間接確認） |
| TS-02 | `LIST_MAP=requestParams` はHTTPリクエストパラメータの予約ID。testShots の行番号に対応する行が使用される | 正常系 | S1-086, S1-087 | S2-213g（TestSupport.splitWithComma カンマエスケープ L170-202） | — （AbstractHttpRequestTestTemplateTest 統合テストで間接確認） |
| TS-03 | `LIST_MAP=responseResult` はHTTPレスポンス（リクエストスコープ）期待値の予約ID | 正常系 | 解説書に記載なし | 実装に記載なし（AbstractHttpRequestTestTemplate.java L77） | — （AbstractHttpRequestTestTemplateTest 統合テストで間接確認） |
| TS-04 | `LIST_MAP=params` はエンティティバリデーションテストの入力パラメータ定義の予約ID（`EntityTestSupport` 専用）。`testShots` の行数と一致が必須 | 正常系 | S1-127 | 実装に記載なし（EntityTestSupport.java L56） | EntityTestSupportTest#testDataSizeDiffer（件数不一致の異常系で間接確認） |
| TS-05 | `setUpDb` はDB共通初期化シートの予約シート名。テストメソッド開始時（または各ショット毎）に1度だけ `SETUP_TABLE` データを投入する | 正常系 | S1-088 | 実装に記載なし（AbstractHttpRequestTestTemplate.java L65） | — （AbstractHttpRequestTestTemplateTest 統合テストで間接確認） |
| TS-06 | testShots の `context` カラムに指定した名前の `LIST_MAP` から `REQUEST_ID`・`USER_ID` を取得する。`context` LIST_MAP は1行のみ有効 | 正常系 | S1-073 | 実装に記載なし（TestCaseInfo.java L40/292-298/432） | — （TestCaseInfoTest 統合テストで間接確認） |
| TS-07 | HTTPテストの testShots 必須カラム: `no`・`description`（または `case`）・`isValidToken`・`expectedStatusCode`・`forwardUri`・`context` | 正常系 | S1-085 | 実装に記載なし（TestCaseInfo.java） | — （TestCaseInfoTest 統合テストで間接確認） |
| TS-08 | バッチ/スタンドアロンテストの testShots 必須カラム: `no`・`description`・`expectedStatusCode`・`diConfig`・`requestPath`・`userId` | 正常系 | S1-075 | 実装に記載なし（TestShot.java L384-387） | BatchRequestTestSupportTest#testTestCasesNotFound（間接確認） |
| TS-09 | バッチテストの testShots オプションカラム: `setUpFile`（入力ファイル準備）・`expectedFile`（出力ファイル検証）。空の場合はスキップ | 正常系 | S1-076 | 実装に記載なし（BatchRequestTestSupport.java L75-91） | — （BatchRequestTestSupportTest 統合テストで間接確認） |
| TS-10 | testShots の `setUpTable` カラムに値がある場合、対応グループIDで `setUpDb(sheetName, groupId)` を呼び出してケース固有のDB初期化を行う | 正常系 | S1-059 | 実装に記載なし（TestCaseInfo.java L374-378） | — （TestCaseInfoTest 統合テストで間接確認） |
| TS-11 | testShots の `expectedTable` カラムに値がある場合、対応グループIDでテーブル期待値を検証する | 正常系 | S1-060 | 実装に記載なし（TestCaseInfo.java L464-466） | — （TestCaseInfoTest 統合テストで間接確認） |
| TS-12 | testShots の `expectedLog` カラムに値がある場合、対応 LIST_MAP からログ期待値を読み込む | 正常系 | S1-079 | 実装に記載なし（TestShot.java L172-174） | — （BatchRequestTestSupportTest 統合テストで間接確認） |
| TS-13 | testShots の `cookie` カラムに値がある場合、対応 LIST_MAP から Cookie 値を読み込む | 代替フロー | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L316-319） | AbstractHttpRequestTestTemplateTest#testCookieNormal |
| TS-14 | testShots の `queryParams` カラムに値がある場合、対応 LIST_MAP からクエリパラメータを読み込む | 代替フロー | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L327-330） | AbstractHttpRequestTestTemplateTest#testQueryParamsNormal |
| TS-15 | testShots の `HTTP_METHOD` カラムが空の場合、デフォルトは `"POST"` | 代替フロー | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L307-309） | — （AbstractHttpRequestTestTemplateTest 統合テストで間接確認） |
| TS-16 | testShots の `expectedContentLength`・`expectedContentType`・`expectedContentFileName` が空の場合、各検証をスキップ | 代替フロー | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L492/513/530） | — （TestCaseInfoTest 統合テストで間接確認） |
| TS-17 | バッチテストの testShots で `args[n]`（`args[0]`, `args[1]`, ...）カラムはコマンドライン引数として渡される | 正常系 | S1-077, S1-078, S1-157 | 実装に記載なし（TestShot.java L255-271） | — （BatchRequestTestSupportTest 統合テストで間接確認） |
| TS-18 | testShots が空の場合、`IllegalStateException`（HTTPテスト）または `IllegalArgumentException`（バッチテスト）をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（AbstractHttpRequestTestTemplate.java L226-229） | BatchRequestTestSupportTest#testTestCasesNotFound |
| TS-19 | `sheetName` が null または空の場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-213j（TestSupport.getResourceName L391-394） | BatchRequestTestSupportTest#testExecuteNull |
| TS-20 | `context` LIST_MAP の `REQUEST_ID` が null または空の場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L293-298） | — （TestCaseInfoTest で間接確認） |
| TS-21 | `context` LIST_MAP が1行でない場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L432） | — （TestCaseInfoTest で間接確認） |
| TS-22 | `requestParams` の行数がテストケース番号より少ない場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | S2-213e（TestSupport.getMap データ行なし IllegalArgumentException L123-125） | — （AbstractHttpRequestTestTemplateTest で間接確認） |
| TS-23 | `testShots` の `no` カラムが空の場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L418-422） | — （TestCaseInfoTest で間接確認） |
| TS-24 | `description` カラムも `case` カラムも未定義の場合、`IllegalStateException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（TestCaseInfo.java L404-405） | — （TestCaseInfoTest で間接確認） |
| TS-25 | `cookie` カラムに LIST_MAP 名を指定したが対応 LIST_MAP が空の場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（AbstractHttpRequestTestTemplate.java L347-348） | AbstractHttpRequestTestTemplateTest#testCookieFailed |
| TS-26 | `queryParams` カラムに LIST_MAP 名を指定したが対応 LIST_MAP が空の場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（AbstractHttpRequestTestTemplate.java L357-359） | AbstractHttpRequestTestTemplateTest#testQueryParamsFailed |
| TS-27 | バッチテストの必須カラム（`no`・`description`・`expectedStatusCode`・`diConfig`・`requestPath`・`userId`）が欠けている場合、検証エラー | 異常系 | 解説書に記載なし | 実装に記載なし（TestShot.java L384-387） | — （BatchRequestTestSupportTest で統合確認） |
| TS-28 | `expectedLog` カラムに値があるが対応 LIST_MAP が空の場合、`IllegalStateException` をスロー | 異常系 | S1-164 | 実装に記載なし（TestShot.java L178-181） | BatchRequestTestSupportTest#testExpectedLogNotFound |
| TS-29 | `EntityTestSupport` の `testShots` 件数と `params` 件数が一致しない場合、`IllegalArgumentException` をスロー | 異常系 | 解説書に記載なし | 実装に記載なし（EntityTestSupport.java L223-228） | EntityTestSupportTest#testDataSizeDiffer |
| TS-30 | `EntityTestSupport` の testShots 必須カラム（`title`・`expectedMessageId1`・`propertyName1`）が欠けている場合、`IllegalArgumentException` をスロー | 異常系 | S1-126 | 実装に記載なし（EntityTestSupport.java L270-276） | EntityTestSupportTest#testRequiredColumnAbsent |
| TS-31 | `DbAccessTestSupport.getParamMap()` でリストが2件以上の場合、`IllegalArgumentException` をスロー。0件の場合は空 Map を返す | 異常系/代替フロー | 解説書に記載なし | 実装に記載なし（DbAccessTestSupport.java L280-288） | — （DbAccessTestSupportTest で統合確認） |
| TS-32 | `DbAccessTestSupport.assertTableEquals(failIfNoDataFound=false)` でデータなしの場合、検証をスキップ | 異常系/代替フロー | 解説書に記載なし | 実装に記載なし（DbAccessTestSupport.java L363-369） | — （DbAccessTestSupportTest で統合確認） |
| TS-33 | `assertTableEquals` はレコードの順番が異なっても主キーで突合して比較する（順序不問） | 正常系 | S1-053 | 実装に記載なし（Assertion.java L249-270） | AssertionTest#testAssertTableEqualsStringListOfTableData |
| TS-34 | `assertSqlResultSetEquals` はレコードの順序が異なる場合は等価でないとみなす（順序厳格） | 正常系 | S1-054 | 実装に記載なし（Assertion.java L116-120） | AssertionTest#testAssertSqlResultSetEquals |

---

## 仕様ID サマリー

| カテゴリ | 仕様ID数 |
|---|---|
| DT | 8件（DT-01〜DT-08） |
| SS | 32件（SS-01〜SS-32） |
| RS | 22件（RS-01〜RS-22） |
| HC | 7件（HC-01〜HC-07） |
| IV | 16件（IV-01〜IV-16） |
| DR | 12件（DR-01〜DR-12） |
| MS | 14件（MS-01〜MS-14） |
| TS | 34件（TS-01〜TS-34） |
| **合計** | **145件** |

> **注**: S-3 で RS-21（YAMLキャッシュ LRU/clearCacheForTest）と RS-22（YAML重複キーエラー）を新規追加（S-2 実装分析で判明した YAML 固有仕様）。TS-33（assertTableEquals 順序不問）と TS-34（assertSqlResultSetEquals 順序厳格）を追加（S1-053/054 の正確なマッピング先として TS-32 から分離）。

---

## S-1 / S-2 / 両方 の分類

| 分類 | 件数 |
|---|---|
| 解説書・実装両方に存在 | 60件 |
| 解説書のみに存在（S-1 only・実装に記載なし） | 18件 |
| 実装のみに存在（S-2 only・解説書に記載なし） | 49件 |
| 解説書・実装ともに記載なし（テストサポート層等の設計レベル仕様） | 18件 |
| **合計** | **145件** |

---

## テストメソッドマッピング サマリー（T-1）

| テスト状態 | 件数 | 内容 |
|---|---|---|
| 直接テストメソッドあり | 約80件 | 具体的なテストクラス・メソッド名を記載 |
| 間接確認（統合テスト・上位層テスト） | 約50件 | `—` で表記。テスト対象クラスを特定して間接的に確認 |
| テスト未作成（到達不能コード・利用者記載規約等） | 約15件 | `—` で表記。理由を記載 |
| 非適用（YAMLリーダー責務外） | 1件 | RS-02 |

**`—` の意味**: 「上位層/統合テストに委任・実装内部の到達不能コード・利用者向けデータ記載規約」のいずれかに該当し、YAMLリーダー単体テストでは検証対象外であることを意味する。根拠なしの「テスト漏れ」ではない。
