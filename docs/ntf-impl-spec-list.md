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

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| DT-01 | DataType 列挙値: `DEFAULT` / `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` / `LIST_MAP` / `SETUP_FIXED` / `EXPECTED_FIXED` / `SETUP_VARIABLE` / `EXPECTED_VARIABLE` / `MESSAGE` / `EXPECTED_REQUEST_HEADER_MESSAGES` / `EXPECTED_REQUEST_BODY_MESSAGES` / `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` の14種 | テストデータ構造 | `DataType.java` 行10-56 |
| DT-02 | セクション識別行の書式: `<DataType名>[groupId]=<値>` (`=` が必須区切り文字。groupId は省略可) | テストデータ構造 | `TestDataParsingTemplate.java` 行244-253 |
| DT-03 | DataType 判定は前方一致（`startsWith`）: セル値が DataType の name で始まれば合致。識別キー＋追加文字のセル値でも認識される | テストデータ構造 | `TestDataParsingTemplate.java` 行221-242（旧E-4） |
| DT-04 | GroupData系（SETUP_TABLE 等）は同一 groupId のセクションを全部収集し続ける（`shouldStopOnNextOne() = false`） | テストデータ構造 | `GroupDataParsingTemplate.java` 行45-53 |
| DT-05 | SingleData系（LIST_MAP / MESSAGE 等）は最初に合致したセクション1つだけを取得して停止する（`shouldStopOnNextOne() = true`） | テストデータ構造 | `SingleDataParsingTemplate.java` 行43-53 |
| DT-06 | groupId 書式: `[groupId]`（省略時は空文字扱い。要素数1時のみ有効・2以上は `IllegalArgumentException`）。バッチ固有: `group_id: "default"` はグループIDなし扱いと同等になる | テストデータ構造 | `BasicTestDataParser.java` 行243-266、公式解説書 batch.rst（Doc-5） |
| DT-07 | `RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` は GroupData（groupId 必須）経路と SingleData（id 一致）経路の2つが存在する | テストデータ構造 | `BasicTestDataParser.java` 行104-117、`design.md §10` |

---

### SS: テーブル・ファイル構造

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| SS-01 | テーブルデータ行の形式: カラム名をキーとするオブジェクト形式。省略されたカラムにはデフォルト値が INSERT 時に補完される | テストデータ構造 | `TableData.java`、`design.md §1/§4` |
| SS-02 | `EXPECTED_TABLE`: 省略されたカラムは比較対象外になる（カラム列挙は任意） | テストデータ構造 | `BasicTestDataParser.java` 行170-181、公式解説書 02_DbAccessTest.rst |
| SS-03 | `EXPECTED_COMPLETE_TABLE`: 省略されたカラムに `BasicDefaultValues` のデフォルト値を補完してから比較する | テストデータ構造 | `BasicTestDataParser.java` 行170-181 (`fillDefaultValues()` 呼び出し) |
| SS-04 | `SETUP_TABLE` では主キーカラムは省略不可（省略するとデフォルト値が INSERT される） | テストデータ構造 | 公式解説書 02_DbAccessTest.rst（Doc-2） |
| SS-05 | `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を同一ファイル内で混在させると後半データが読み込まれない（まとめて記述が必要） | テストデータ構造 | 公式解説書 01_Abstract.rst（Doc-4） |
| SS-06 | `LIST_MAP=id` セクション: id は完全一致。同一ファイル内で同一 id の重複エントリは後続が黙って無視される（先着一致） | テストデータ構造 | `SingleDataParsingTemplate.java`、`design.md §9` |
| SS-07 | `SETUP_FIXED` と `SETUP_VARIABLE` は `BasicTestDataParser#getSetupFile()` でまとめて返される。`EXPECTED_FIXED`/`EXPECTED_VARIABLE` も同様 | テストデータ構造 | `BasicTestDataParser.java` 行66-80 |
| SS-08 | ファイルセクションの行順序: ディレクティブ行（0行以上） → フィールド名行 → データ型行 → [フィールド長行（固定長のみ）] → データ行 | テストデータ構造 | `DataFileParser.java` 行38-49（`Status` 遷移） |
| SS-09 | 固定長フラグメント: `names` / `types` / `lengths` の3リストが同サイズで必須 | テストデータ構造 | `FixedLengthFileFragment.java` 行140-144 |
| SS-10 | 可変長フラグメント: `names` / `types` の2リストが同サイズで必須。`lengths` は不要（型行読み取り後に直接 READING_VALUES へ遷移） | テストデータ構造 | `VariableLengthFileParser.java` 行40-46 |
| SS-11 | 1ファイルセクション内に複数レコードレイアウトを連続記述可能: データ行の後ろに新たなフィールド名行を書くと新レコードレイアウトとして扱われる | テストデータ構造 | `DataFileParser.java` 行177-191（旧D-14） |
| SS-12 | フィールド名行の構造: 先頭列 = レコード種別名、2列目以降 = フィールド名の列挙 | テストデータ構造 | `DataFileParser.java` 行243-252 |
| SS-13 | データ行の先頭セルは必ず空（null または空文字）にする | テストデータ構造 | `DataFileParser.java` 行193-210 |
| SS-14 | 同一レコード種別内のフィールド名は重複不可（`IllegalArgumentException`）。異なる種別間は重複可 | テストデータ構造 | `DataFileFragment.java` 行185-194、348-362（Doc-9） |
| SS-15 | 空ファイル（0バイト）表現: ディレクティブ行のみ記述してレコード定義を省略する。`records` の `minItems: 0` が必要 | テストデータ構造 | 公式解説書 03_Tips.rst（Doc-10） |
| SS-16 | 固定長ファイルは全フラグメントで同一レコード長が必須（違反時 `IllegalStateException`） | テストデータ構造 | `FixedLengthFile.java` 行94-117 |
| SS-17 | `"-"` 長フィールド: 追加された全レコードの最大バイト長に自動拡張。値は改行コードと前後空白が除去される | テストデータ構造 | `DataFileFragment.java` 行129-161（旧D-16） |
| SS-18 | `BasicDefaultValues` のデフォルト値: 数値型=`"0"`、CHAR/NCHAR=スペース×カラム長、VARCHAR等=半角スペース1文字、DATE=`"1970-01-01 09:00:00.0"`（JVM タイムゾーン依存）、バイナリ=10バイトゼロHexString、Boolean=`"false"` | テストデータ構造 | `BasicDefaultValues`、`design.md §4` |
| SS-19 | `testShots` は LIST_MAP の予約ID: バッチリクエスト単体テストでフレームワークがテストケース一覧として自動読み込みする | テストデータ構造 | 公式解説書 batch.rst（Doc-16） |
| SS-20 | ファイル系空行の動作差異: 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される | テストデータ構造 | `design.md §AI向けプロンプト ファイル系の空行動作`（旧D-10） |

---

### RS: YAMLリーダー実装仕様

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| RS-01 | `open(path, dataName)` 規約: `dataName` に対して `{dataName}.yaml` ファイルを検索する | 実装内部ロジック | `TestDataReader` インタフェース（設計方針） |
| RS-02 | `readLine()` は文書終端で `null` を返す | 実装内部ロジック | `TestDataReader` インタフェース（既存 Excel 実装との整合） |
| RS-03 | YAML ネイティブ `null`（アンクォート）は文字列 `"null"` として返す（旧E-1） | 実装内部ロジック | `design.md §7`、YAML native type conversion |
| RS-04 | YAML ネイティブ boolean (`true`/`false`) は文字列 `"true"`/`"false"` として返す（旧E-1） | 実装内部ロジック | `design.md §7` |
| RS-05 | YAML ネイティブ integer/float は数字文字列として返す（旧E-1） | 実装内部ロジック | `design.md §7` |
| RS-06 | 末尾の空要素（null や省略）は空文字 `""` で補完して返す（旧E-2） | 実装内部ロジック | `HeaderLine.java` 行69-85 の末尾省略仕様と整合 |
| RS-07 | `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを保証する（旧E-3） | 実装内部ロジック | `TestDataParsingTemplate.java` 行187-219 の parse ロジック |
| RS-08 | `isDataExisting(directory, resource)` / `isResourceExisting(directory, resource)` の実装（リソース存在確認） | 実装内部ロジック | `BasicTestDataParser.java` 行267-271 |

---

### HC: ヘッダ行・カラム処理

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| HC-01 | マーカーカラムの書式: `[カラム名]`（`[` で始まり `]` で終わる） | テストデータ構造 | `HeaderLine.java` 行87-96 |
| HC-02 | マーカーカラムは DB 操作から除外される（データとして格納されない） | テストデータ構造 | `HeaderLine.java` 行53-85、`TableDataParser.java` 行74-82 |
| HC-03 | ヘッダ行末尾の空カラムは除去される（末尾カラム省略可） | テストデータ構造 | `HeaderLine.java` 行27-42（`trimTailCopy()`） |
| HC-04 | データ行がヘッダより短い場合、不足分は空文字 `""` で補完される | テストデータ構造 | `HeaderLine.java` 行69-85 |
| HC-05 | コメント行: 先頭セルが `//` で始まる行は行ごとスキップ | テストデータ構造 | `TestDataParsingTemplate.java` 行268-291 |
| HC-06 | 行内コメント: 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨て | テストデータ構造 | `TestDataParsingTemplate.java` 行292-308 |
| HC-07 | 空行スキップ: 全要素が null または空文字の行は読み飛ばす | テストデータ構造 | `TestDataParsingTemplate.java` 行310-318 |

---

### IV: インタープリタ・特殊値

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| IV-01 | `NullInterpreter`: `null`/`NULL`/`Null`（大文字小文字不問）を Java null に変換 | テストデータ構造 | `NullInterpreter.java` 行10-19 |
| IV-02 | `QuotationTrimmer`: 半角または全角ダブルクォートで前後が囲まれた場合のみ外側1層を除去。片側のみはスルー | テストデータ構造 | `QuotationTrimmer.java` 行18-30 |
| IV-03 | `DateTimeInterpreter`: `${systemTime}` / `${updateTime}` / `${setUpTime}` の完全一致のみ変換。部分文字列は変換されない（`CompositeInterpreter` との組み合わせが必要） | テストデータ構造 | `DateTimeInterpreter.java` 行48-94 |
| IV-04 | `LineSeparatorInterpreter`: `\\r` → CR(0x0D)（デフォルト）、`\\n` → LF(0x0A) に変換 | テストデータ構造 | `LineSeparatorInterpreter.java`、公式解説書 01_Abstract.rst（Doc-7） |
| IV-05 | `BinaryFileInterpreter`: `${binaryFile:パス}` でファイル内容をバイナリ読み込みし HexString に変換。YAML ファイルが基準ディレクトリになる | テストデータ構造 | `BinaryFileInterpreter.java` 行34-65 |
| IV-06 | `BasicJapaneseCharacterInterpreter`: `${文字種,文字数}` 形式で文字列生成。書式完全一致のみ動作、文字種未知の場合は `IllegalArgumentException`（書式ミスはスルー） | テストデータ構造 | `BasicJapaneseCharacterInterpreter.java` 行22-45 |
| IV-07 | `BasicJapaneseCharacterGenerator` 有効文字種14種: 半角英字/半角数字/半角記号/半角カナ/全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角記号その他/中国語/サロゲートペア/改行/外字 | テストデータ構造 | `BasicJapaneseCharacterGenerator.java` 行40-56 |
| IV-08 | `CompositeInterpreter`: 文字列中の `${...}` 要素を個別解釈して置換。`${...}` がない場合は次のインタープリタに委譲 | テストデータ構造 | `CompositeInterpreter.java` 行22-42 |
| IV-09 | 日付型カラムの記述形式: `yyyyMMddHHmmssSSS`（17文字）、後置0埋め短縮形、JDBC タイムスタンプエスケープ形式（5文字目が `-`）等が有効 | テストデータ構造 | `TableData.java` 行214-273、`design.md §7` |
| IV-10 | `Timestamp` 型カラムの期待値は末尾 `.0` が必要（例: `"2010-01-01 12:34:56.0"`） | テストデータ構造 | 公式解説書 02_DbAccessTest.rst（Doc-3） |
| IV-11 | バイナリデータの直接記述: `0x` プレフィクス付き16進数で記述可能。`0x` がない場合は文字列としてエンコード | テストデータ構造 | 公式解説書 batch.rst（Doc-11） |
| IV-12 | `BasiDataTypeMapping` デフォルトマッピング22種（`半角英字`→`X` 等）。未知の型記号は `IllegalArgumentException` | 実装内部ロジック | `BasicDataTypeMapping.java` 行30-73 |
| IV-13 | `TEST_` プレフィクス型の自動優先選択: `TEST_{baseType}` 名のデータ型が存在する場合、自動的に優先使用される | 実装内部ロジック | `DataFileFragment.java` 行211-245 |
| IV-14 | `QuotationTrimmer` によるスペース値明示記法: `'"⊔"'` → 半角スペース、`'"""'` → ダブルクォート1文字。ダブルクォートで囲むことで空白値を可視化して記述できる | テストデータ構造 | `design.md §7`、公式解説書 01_Abstract.rst（Doc-8） |
| IV-15 | X9/SX9 型フィールドの記述方法: パディング文字・符号を含めた実際のバイト列表現（固定長フォーマットの実値）をそのまま記載する必要がある | テストデータ構造 | 公式解説書 batch.rst（Doc-12）、`design.md §26` |

---

### DR: ディレクティブ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| DR-01 | ディレクティブ行の構成: 先頭列 = キー名、2列目 = 値（最低2列必要） | テストデータ構造 | `DataFileParser.java` 行212-232 |
| DR-02 | 固定長ファイルで有効なディレクティブキーは `FixedLengthDirective` 列挙型の定義に限定される | テストデータ構造 | `FixedLengthFileParser.java` 行34-38 |
| DR-03 | 可変長ファイルで有効なディレクティブキーは `VariableLengthDirective` 列挙型の定義に限定される | テストデータ構造 | `VariableLengthFileParser.java` 行34-38 |
| DR-04 | `defaultDirectives` DI: SystemRepository のこのキーで全ファイル共通デフォルトディレクティブを一括設定できる | 実装内部ロジック | `DataFile.java` 行59-93（旧E-6） |
| DR-05 | `fixedLengthDirectives` DI: 固定長ファイル専用デフォルトディレクティブ（`defaultDirectives` より後に上書き適用） | 実装内部ロジック | `FixedLengthFile.java` 行16-27 |
| DR-06 | `variableLengthDirectives` DI: 可変長ファイル専用デフォルトディレクティブ | 実装内部ロジック | `VariableLengthFile.java` 行19-31 |
| DR-07 | `file-type` ディレクティブはサブクラス（固定長=`"Fixed"`、可変長=`"Variable"`）が自動設定するため通常は記述不要 | テストデータ構造 | `DataFile.java` 行83-101、`FixedLengthFile.java` 行29-36 |
| DR-08 | `record-length` ディレクティブはフィールド長合計から自動計算されるため通常は記述不要 | テストデータ構造 | `FixedLengthFile.java` 行60-92 |
| DR-09 | `field-separator`: 可変長ファイルのデフォルトは `","``。`"\\t"` 指定でタブ文字に変換。値は1文字のみ有効 | テストデータ構造 | `VariableLengthFile.java` 行16-82 |
| DR-10 | `record-separator`: `NONE`/`CR`/`LF`/`CRLF` または任意リテラル文字列が有効 | テストデータ構造 | `LineSeparator.java`、`DataFile.java` 行318-334 |

---

### MS: メッセージングテストデータ

| 仕様ID | 概要 | 分類 | 根拠（コード/ドキュメント） |
|---|---|---|---|
| MS-01 | FW 制御ヘッダフィールドのデフォルト4種: `requestId` / `userId` / `resendFlag` / `resultCode`。`reader.fwHeaderfields` キーで変更可能 | テストデータ構造 | `MessageParser.java` 行95-110 |
| MS-02 | `no` 列（先頭列、列番号0）はフレームワークが除去し、データとして保存されない。`errorMode` 値は列番号1に格納される | テストデータ構造 | `SendSyncMessageParser.java` 行94-134 |
| MS-03 | `MESSAGE` / `EXPECTED_REQUEST_*_MESSAGES` の `record_type` 値は常に内部で `"default"` に置き換えられる（装飾的なメタデータとして任意の値を書いてよい） | テストデータ構造 | `MessageParser.java` 行60-67 |
| MS-04 | `errorMode:timeout` および `errorMode:msgException` は `no` 列の次（列番号1）に配置する特殊値。他フィールドはパースされない | テストデータ構造 | `SendSyncMessageParser.java` 行18-44、116-132 |
| MS-05 | `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（rows 合計）は一致が必須。不一致は `IllegalStateException`（旧E-7） | テストデータ構造 | `RequestTestingMessagingClient.java` 行294-443 |
| MS-06 | `GroupMessageParser`: 同一 groupId の複数メッセージプールを収集。セクション識別子 `=` 以降をリクエストIDとして使用 | テストデータ構造 | `GroupMessageParser.java` 行48-65 |
| MS-07 | `sendSyncTestData/{requestId}/message` の配置規則: テストデータファイルは `sendSyncTestData` ベースパス下にリクエストIDと同名ファイルとして配置する（旧E-5） | テストデータ構造 | `SendSyncSupport.java` 行39-49 |
| MS-08 | ステータスコード列がない場合はデフォルト `"200"` が使用される | テストデータ構造 | `RequestTestingMessagingClient.java` 行124-204 |
| MS-09 | マルチレコード送信時: ヘッダ行数とボディ行数を一致させる。N 回送信の場合は各 N 行記述（公式解説書 Doc-13） | テストデータ構造 | 公式解説書 send_sync.rst |
| MS-10 | `no` 列と複数回送信: 同一リクエストIDで複数回送信する場合は `no` 値を変えて連続記述し、送信順序と `no` 値を一致させる（公式解説書 Doc-14） | テストデータ構造 | 公式解説書 send_sync.rst |
| MS-11 | HTTP同期応答メッセージ送信処理のボディ行長制約: `response_body_messages` の各データ行の文字列長が同一であることが必要（JSON/XML形式使用時の制約） | テストデータ構造 | 公式解説書 http_send_sync.rst（Doc-15）、`design.md §11` |
| MS-12 | フォーマット定義ファイルの命名規則: 応答電文は `{requestId}_RECEIVE`、要求電文は `{requestId}_SEND` | テストデータ構造 | `RequestTestingMessagingClient.java` 行75-79、`design.md §20` |
| MS-13 | `messaging.assertAsMapFileType` キー: SystemRepository から未設定時はデフォルト `"Fixed"` 形式で項目単位アサート。値により文字列全体アサートに切り替え可能 | テストデータ構造 | `RequestTestingMessagingClient.java` 行81-83、`design.md §19` |

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
| SS | 20件（SS-01〜SS-20） | 20件 | 0件 |
| RS | 8件（RS-01〜RS-08） | 0件 | 8件 |
| HC | 7件（HC-01〜HC-07） | 7件 | 0件 |
| IV | 15件（IV-01〜IV-15） | 14件 | 1件（IV-12）|
| DR | 10件（DR-01〜DR-10） | 8件 | 2件（DR-04〜DR-06）|
| MS | 13件（MS-01〜MS-13） | 13件 | 0件 |
| **合計** | **80件** | **69件** | **11件** |

---

## 抜け漏れ確認

本仕様一覧は以下の3つの調査結果を統合して作成した。全件をカバーしていることを確認した。

| 調査元 | 仕様数 | 取り込み状況 |
|---|---|---|
| `ntf-coverage-spec-mapping.md`（コード全行走査 29クラス） | S-1〜S-5 / D-1〜D-16 / E-1〜E-4 | 全件取り込み済み。D-10→SS-20 として追加（QA指摘NG-1対応） |
| `ntf-coverage-doc-check.md`（公式解説書照合 13本） | Doc-1〜Doc-17（うち反映対象17件） | 全件取り込み済み。Doc-5→DT-06、Doc-8→IV-14、Doc-12→IV-15、Doc-15→MS-11（QA指摘対応） |
| `ntf-testdata-yaml-design.md`（スキーマ設計・設計上の注意点） | 27項目（§1〜§27） | 全件取り込み済み。§19→MS-13、§20→MS-12 として追加（QA指摘対応） |
| E-1〜E-9 | 9件 | 全件処置済み（8件昇格・1件ドキュメント修正のみ） |
