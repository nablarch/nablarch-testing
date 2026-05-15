# NTF テストデータ仕様 カバレッジ クラス一覧

## 0. 前提: 調査リポジトリの範囲（P4-0）

### 0.1 調査リポジトリ

- **リポジトリ**: `nablarch-testing`（`com.nablarch.framework:nablarch-testing`）
- **調査日**: 2026-05-15
- **ブランチ**: `convert-testdata-excel-to-text`

### 0.2 「このリポジトリだけを見ればよいか」の判断

**結論: このリポジトリだけでは仕様を完全に把握できない。`nablarch-core-dataformat` の参照も必要。**

ただし、YAML スキーマ設計の主目的である「テストデータの構造（どの項目をどう書くか）」については、  
このリポジトリ内のパーサ・インタープリタクラスを読むことで大半を把握できる。  
フォーマッタ本体（実際のバイト列変換ロジック）は外部にあるが、YAMLスキーマが扱うのは  
「フォーマット定義のテスト記述」であり、変換ロジックの詳細まで YAML に落とす必要はない。

### 0.3 外部依存の整理

| artifactId | scope | テストデータ仕様への関わり |
|---|---|---|
| `nablarch-core-dataformat` | compile（`nablarch-fw-web-extension` からは exclude 済み） | **重要**: 固定長・可変長フォーマットの定義・変換ロジック本体。`LayoutDefinition`, `FieldDefinition`, `DataRecordFormatter`, `FixedLengthDataRecordFormatter`, `VariableLengthDataRecordFormatter` 等を提供 |
| `nablarch-fw-messaging-mom` | provided | メッセージング系テストデータ（`MessagePool`, `MockMessagingClient` 等が依存） |
| `nablarch-fw-messaging-http` | provided | HTTP メッセージング系テストデータ |
| `nablarch-common-dao` | compile | DB テストデータ（`TableData` 等が依存） |
| `org.apache.poi:poi-ooxml:3.8` | compile | Excel 読み込み（`PoiXlsReader` が直接使用）。このリポジトリ内で完結 |

### 0.4 nablarch-core-dataformat への依存状況

このリポジトリの以下のクラスが `nablarch-core-dataformat` のクラスを直接 import して使用している:

**`nablarch.test.core.file` パッケージ（10クラス）**
- `DataFile`, `DataFileFragment`, `FixedLengthFile`, `FixedLengthFileFragment`
- `VariableLengthFile`, `VariableLengthFileFragment`, `FileSupport`
- `MockMessages`, `StringDataType`, `TestDataConverter`

**`nablarch.test.core.reader` パッケージ（2クラス）**
- `FixedLengthFileParser`, `VariableLengthFileParser`

**`nablarch.test.core.messaging` パッケージ（6クラス）**
- `MessagePool`, `MessagingRequestTestSupport`, `MockMessagingClient`
- `RequestTestingMessagePool`, `RequestTestingMessagingClient`, `RequestTestingMessagingProvider`, `SendSyncSupport`

**その他（1クラス）**
- `nablarch.test.Assertion`

使用される外部クラス（計23種）:
`DataRecord`, `DataRecordFormatter`, `DataRecordFormatterSupport`, `DataRecordFormatterSupport.Directive`,
`FieldDefinition`, `FileRecordWriter`, `FormatterFactory`, `LayoutDefinition`, `RecordDefinition`,
`FixedLengthDataRecordFormatter`, `FixedLengthDataRecordFormatter.FixedLengthDirective`,
`VariableLengthDataRecordFormatter`, `VariableLengthDataRecordFormatter.VariableLengthDirective`,
`InvalidDataFormatException`, `SimpleDataConvertResult`, `SimpleDataConvertUtil`,
`convertor.ConvertorFactorySupport`, `convertor.FixedLengthConvertorSetting`, `convertor.VariableLengthConvertorSetting`,
`convertor.datatype.ByteStreamDataSupport`, `convertor.datatype.Bytes`,
`convertor.datatype.CharacterStreamDataString`, `convertor.datatype.DataType`

### 0.5 このリポジトリ内で完結している仕様の範囲

- **Excel テストデータ読み込み**: `PoiXlsReader`（POI のみ依存、外部 Nablarch ライブラリ不要）
- **テストデータパーサの構造**: `BasicTestDataParser`, `TestDataReader` 等のインタフェースと実装
- **DB テストデータ処理**: `TableData`, `DbAccessTestSupport` 等（`nablarch-common-dao` への依存はあるが仕様の核心はこのリポジトリ内）
- **テストデータ値のインタープリタ**: `nablarch.test.core.util.interpreter` 配下（`NullInterpreter`, `BasicJapaneseCharacterInterpreter` 等）
- **メッセージング系テストデータの構造定義**: 各パーサクラスはこのリポジトリ内

### 0.6 nablarch-core-dataformat を参照すべき仕様の範囲

以下の仕様は `nablarch-core-dataformat` 側に定義があり、必要に応じて参照が必要:

- **固定長ファイルのディレクティブ一覧**: `FixedLengthDataRecordFormatter.FixedLengthDirective` で定義
- **可変長ファイルのディレクティブ一覧**: `VariableLengthDataRecordFormatter.VariableLengthDirective` で定義
- **フィールド型（DataType）の一覧**: `convertor.datatype.*` で実装
- **レコード構造の詳細**: `LayoutDefinition`, `FieldDefinition`, `RecordDefinition`
- **フォーマッタの実際の変換動作**: `DataRecordFormatter` の各実装

### 0.7 P4-1 以降の調査方針

P4-1（対象クラス一覧）はこのリポジトリ（`nablarch-testing`）の `src/main/java` を主たる調査対象とする。  
ディレクティブ・フィールド型など `nablarch-core-dataformat` 側の仕様については、  
現在設計済みのスキーマ（`ntf-testdata-yaml-schema.json`）・設計文書（`ntf-testdata-yaml-design.md`）・  
既存の構造解析文書（`ntf-testdata-structure.md`）に既に取り込まれている内容を参照することで補完する。

---

## 1. 対象クラス一覧（P4-1）

### 凡例

- **直接影響**: YAMLスキーマの構造・制約・有効値に直接関わる仕様を持つ
- **参照情報**: スキーマ設計の背景理解に有用だが、スキーマ項目の直接根拠にはならない
- **対象外**: テストデータ構造定義と無関係（テスト実行支援・HTTP処理・HTML検証等）

---

### 1.1 `nablarch.test.core.reader` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `DataType` | enum | **直接影響** | セクション識別キー（`SETUP_TABLE`, `EXPECTED_TABLE`, `EXPECTED_COMPLETE_TABLE`, `LIST_MAP`, `SETUP_FIXED`, `EXPECTED_FIXED`, `SETUP_VARIABLE`, `EXPECTED_VARIABLE`, `MESSAGE`, `EXPECTED_REQUEST_HEADER_MESSAGES`, `EXPECTED_REQUEST_BODY_MESSAGES`, `RESPONSE_HEADER_MESSAGES`, `RESPONSE_BODY_MESSAGES`）の完全一覧を定義 |
| `TestDataParsingTemplate` | abstract class | **直接影響** | コメント行（`//` 行スキップ）・セクション先頭一致マッチング規則を実装。値変換はインタープリタ群に委譲 |
| `GroupDataParsingTemplate` | abstract class | **直接影響** | グループID付きセクション識別構文 `TYPE_NAME[groupId]=value` の解析。グループID省略不可の根拠 |
| `SingleDataParsingTemplate` | abstract class | 参照情報 | 単一IDの完全一致参照方式の実装 |
| `HeaderLine` | class | **直接影響** | `[xxx]` 形式のマーカーカラムを除外してカラム名一覧を構築。`[MARKER_COL]` 構文の根拠 |
| `TableDataParser` | class | **直接影響** | `SETUP_TABLE` / `EXPECTED_TABLE` / `EXPECTED_COMPLETE_TABLE` セクション解析。セクション行→カラム名行→データ行の順序を確定 |
| `ListMapParser` | class | **直接影響** | `LIST_MAP` セクション解析。キー名行→データ行の構造を確定 |
| `MessageParser` | class | **直接影響** | `MESSAGE` セクション解析。`record_type` 先頭カラムを常に `"default"` へ強制置換。FWヘッダフィールド名（`requestId`, `userId`, `resendFlag`, `resultCode`）を定義 |
| `SendSyncMessageParser` | class | **直接影響** | `MessageParser` を継承。第2カラムに `errorMode:timeout` または `errorMode:msgException` という特殊値を解釈 |
| `GroupMessageParser` | class | **直接影響** | グループメッセージセクションを複数ブロックとして解析。`SendSyncMessageParser` に委譲 |
| `DataFileParser` | abstract class | **直接影響** | ファイルセクションの行順序（ディレクティブ/フィールド名行→型行→長さ行→値行）を状態機械で実装 |
| `FixedLengthFileParser` | class | **直接影響** | `SETUP_FIXED` / `EXPECTED_FIXED` セクション用。有効ディレクティブキーを `FixedLengthDirective` で判定 |
| `VariableLengthFileParser` | class | **直接影響** | `SETUP_VARIABLE` / `EXPECTED_VARIABLE` セクション用。**長さ行をスキップ**（フィールド長不要）。有効ディレクティブキーを `VariableLengthDirective` で判定 |
| `BasicTestDataParser` | class | 参照情報 | `TestDataParser` の主要実装。各セクションへの委譲構造の確認に利用 |
| `DbLessTestDataParser` | class | 対象外 | DBなし用パーサ。スキーマ構造に影響なし |
| `TestDataParser` | interface | 対象外 | 高レベル読み込みインタフェース。スキーマ構造に直接影響なし |
| `TestDataReader` | interface | 対象外 | 低レベル読み込みインタフェース。スキーマ構造に直接影響なし |
| `PoiXlsReader` | class | 対象外 | Excel読み込み実装。YAMLスキーマとは無関係 |

---

### 1.2 `nablarch.test.core.file` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `DataFile` | abstract class | **直接影響** | ファイル全体の基底。`file-type`, `record-separator`, `text-encoding` 等の共通ディレクティブ処理を担う |
| `DataFileFragment` | abstract class | **直接影響** | レコード種別ひとまとまりの基底。フィールド名/型/長さ/値の4要素を保持。フィールド長 `-`（`ONDEMAND_CALC_FIELD_SIZE`）特殊値の実装 |
| `FixedLengthFile` | class | **直接影響** | 固定長ファイル実体。`FixedLengthDirective` で有効ディレクティブを決定。レコード長を自動計算 |
| `FixedLengthFileFragment` | class | 参照情報 | 固定長レコード種別実体。バイナリ型フィールドのゼロ埋め処理の根拠 |
| `VariableLengthFile` | class | **直接影響** | 可変長ファイル実体。デフォルトフィールド区切り `,`。`\\t` → タブ文字変換を実装 |
| `VariableLengthFileFragment` | class | 参照情報 | 可変長レコード種別実体。長さ行不要の実装上の根拠 |
| `BasicDataTypeMapping` | class | **直接影響** | 設計書データ型記法→フレームワークシンボル変換のデフォルト実装。有効な設計書記法17種を定義（`半角英字`→`X`, `全角`→`N`, `数値`→`Z`, `符号付パック10進数`→`SP`, `バイナリ`→`B` 等） |
| `DataTypeMapping` | interface | 参照情報 | カスタムデータ型マッピングの拡張ポイント |
| `LineSeparator` | enum | **直接影響** | `record-separator` ディレクティブの有効値。`NONE` / `CR` / `LF` / `CRLF` のほか列挙名以外の文字列もリテラルとして使用可能 |
| `MockMessages` | class | 参照情報 | `FixedLengthFile` の同期送信テスト用サブクラス。`errorMode:*` 特殊値がパディング処理を受けない実装根拠 |
| `StringDataType` | class | 参照情報 | テスト用 `TEST_` プレフィクスシンボルの動作（パディングなし・サイズ不一致で例外）の根拠 |
| `TestDataConverter` | interface | 参照情報 | カスタム変換処理の拡張ポイント（`TestDataConverter_{file-type}` キーで登録） |
| `FileSupport` | class | 対象外 | テスト実行サポートユーティリティ。スキーマ構造に影響なし |

---

### 1.3 `nablarch.test.core.messaging` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `RequestTestingMessagingClient` | class | **直接影響** | `EXPECTED_REQUEST_HEADER_MESSAGES`, `EXPECTED_REQUEST_BODY_MESSAGES`, `RESPONSE_HEADER_MESSAGES`, `RESPONSE_BODY_MESSAGES` の4セクションを使用するHTTP系リクエスト単体テストのモック。送信電文のアサートと応答電文の返却を担う |
| `MockMessagingContext` | class | 参照情報 | MOMメッセージングのアクセスパスA（`group_message_data` 経由）。`requestId` フィールド必須の根拠 |
| `MockMessagingClient` | class | 参照情報 | HTTP系メッセージングのアクセスパスB。`statusCode` デフォルト `200` の根拠 |
| `RequestTestingMessagePool` | class | 参照情報 | `errorMode:timeout` → `null` 返却、`errorMode:msgException` → `MessagingException` スローの動作確認 |
| `SendSyncSupport` | class | **直接影響** | テストデータ配置規則：`sendSyncTestData` ベースパス配下の `{requestId}/message` シートからデータを取得。配置場所の仕様を確定 |
| `MessagePool` | class | 参照情報 | MESSAGEセクションデータの実体。テストショット毎のメッセージ管理 |
| `RequestTestingMessagingProvider` | class | 参照情報 | `RequestTestingMessagingClient` と同仕様のMOM系実装 |
| `MessagingRequestTestSupport` | class | 対象外 | テスト実行サポート。スキーマ構造に影響なし |
| `MessagingReceiveTestSupport` | class | 対象外 | 受信テストサポート。スキーマ構造に影響なし |
| `EmbeddedMessagingProvider` | class | 対象外 | 組み込みメッセージングプロバイダ。スキーマ構造に影響なし |
| `MQSupport` | class | 対象外 | MQサポートユーティリティ。スキーマ構造に影響なし |
| `MockMessagingProvider` | class | 対象外 | コンポーネント設定クラス。スキーマ構造に影響なし |
| `AsyncMessageSendActionForUt` | class | 対象外 | 非同期送信アクション。スキーマ構造に影響なし |
| `RequestTestingSendSyncSupport` | class | 対象外 | リクエストテスト用同期送信サポート。スキーマ構造に影響なし |

---

### 1.4 `nablarch.test.core.db` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `TableData` | class | **直接影響** | テーブルデータの実体。日付デフォルトフォーマット `yyyyMMddHHmmssSSS`。`fillDefaultValues()` で省略カラムにデフォルト値補完（`EXPECTED_COMPLETE_TABLE` 用）|
| `DbAccessTestSupport` | class | 対象外 | テスト実行サポート。スキーマ構造に影響なし |
| `BasicDefaultValues` | class | 対象外 | デフォルト値設定クラス |
| `DefaultValues` | interface | 対象外 | デフォルト値インタフェース |
| その他（`DbInfo`, `EntityDependencyParser` 等） | class | 対象外 | DB操作ユーティリティ群 |

---

### 1.5 `nablarch.test.core.util.interpreter` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `NullInterpreter` | class | **直接影響** | 文字列 `"null"`（大文字小文字不問）を Java の `null` へ変換。YAMLネイティブ `null` との使い分け仕様の根拠 |
| `QuotationTrimmer` | class | **直接影響** | 半角/全角ダブルクォートで囲まれた値の前後クォートを除去。Excelでの文字列エスケープ記法の根拠 |
| `DateTimeInterpreter` | class | **直接影響** | `${systemTime}` → システム時刻、`${setUpTime}` → DBセットアップ時刻、`${updateTime}` → DB更新時刻 へ変換 |
| `LineSeparatorInterpreter` | class | **直接影響** | デフォルト設定で `\\r` パターンを CR（`\r`）に置換。改行コードのエスケープ記法の根拠 |
| `BinaryFileInterpreter` | class | **直接影響** | `${binaryFile:相対パス}` 記法をファイルのバイナリ内容（16進数文字列）に変換 |
| `BasicJapaneseCharacterInterpreter` | class | **直接影響** | `${文字種,文字数}` 記法を指定文字種の文字列に変換。`BasicJapaneseCharacterGenerator` に委譲 |
| `CompositeInterpreter` | class | **直接影響** | 複数の `${...}` 記法を含む値（例: `${半角数字,4}-${半角数字,4}`）を分解・個別解釈・連結 |
| `TestDataInterpreter` | interface | 参照情報 | インタープリタ拡張ポイント |
| `InterpretationContext` | class | 対象外 | 内部実装クラス |

---

### 1.6 `nablarch.test.core.util.generator` パッケージ

| クラス | 種別 | 関連度 | 役割・スキーマへの影響 |
|---|---|---|---|
| `BasicJapaneseCharacterGenerator` | class | **直接影響** | `BasicJapaneseCharacterInterpreter` から利用される文字生成実装。サポートされる文字種トークンの完全一覧を定義 |
| `JapaneseCharacterSet` | enum | **直接影響** | 文字種トークンの enum。`半角英字`, `全角`, `半角数字` 等の有効トークン名を確定 |
| `CharacterGenerator` | interface | 参照情報 | 文字生成拡張インタフェース |
| `CharacterGeneratorBase` | abstract class | 参照情報 | 文字生成基底クラス |

---

### 1.7 対象外パッケージ（全クラス）

以下のパッケージはテストデータ構造定義とは無関係なため P4-2 の対象外とする:

| パッケージ | 理由 |
|---|---|
| `nablarch.fw.web` | HTTPモック・サーバ実装 |
| `nablarch.test.core.http` | HTTPリクエストテスト実行サポート |
| `nablarch.test.core.batch` | バッチリクエストテスト実行サポート |
| `nablarch.test.core.entity` | エンティティバリデーションテストサポート |
| `nablarch.test.core.integration` | 統合テストサポート |
| `nablarch.test.core.log` | ログ検証サポート |
| `nablarch.test.core.repository` | リポジトリ設定ブラウザ |
| `nablarch.test.core.standalone` | スタンドアロンテストサポート |
| `nablarch.test.tool.htmlcheck` | HTML構文チェックツール |
| `nablarch.test.tool.sanitizingcheck` | サニタイズチェックツール |
| `nablarch.test.event` | テストイベントリスナ |
| `nablarch.test`（ルート） | テスト基底ユーティリティ（`TestSupport`, `Assertion` 等） |

---

### 1.8 直接影響クラス 集計

| パッケージ | 直接影響クラス数 | 主要な仕様 |
|---|---|---|
| `reader` | 11 | セクション識別、行順序、record_type強制化、errorMode特殊値 |
| `file` | 6 | ディレクティブ有効値、データ型マッピング17種、record-separator有効値 |
| `messaging` | 2 | 4セクションの役割定義、配置規則 |
| `db` | 1 | 日付フォーマット |
| `interpreter` | 7 | null/クォート/日時/改行/バイナリ/文字生成の特殊値記法 |
| `generator` | 2 | 文字種トークン完全一覧 |
| **合計** | **29** | |
