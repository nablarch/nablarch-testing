# NTF 公式解説書 × スキーマ設計 照合チェック

- **照合日**: 2026-05-15
- **解説書リポジトリ**: nablarch/nablarch-document
- **照合対象スキーマ**: ntf-testdata-yaml-schema.json / ntf-testdata-yaml-design.md / ntf-testdata-yaml-examples.yaml

---

## 1. 読み込んだドキュメント一覧

| ファイルパス | 行数 | テストデータ仕様への関連度 | 概要 |
|---|---|---|---|
| 06_TestFWGuide/01_Abstract.rst | 739 | 中 | 自動テストフレームワークの概要・Excel命名規約・シート構造・データタイプ一覧・特殊記法・日付記述方法 |
| 06_TestFWGuide/02_DbAccessTest.rst | 554 | 高 | DBアクセステストの方法・SETUP_TABLE/EXPECTED_TABLE/EXPECTED_COMPLETE_TABLE/LIST_MAP の記述方法・デフォルト値仕様 |
| 06_TestFWGuide/02_RequestUnitTest.rst | 552 | 低 | リクエスト単体テスト（Web）の構造・設定値一覧。テストデータ記述仕様への直接言及なし |
| 06_TestFWGuide/03_Tips.rst | 831 | 高 | グループID・LIST_MAP・空行表現・特殊値・TestDataConverter・テストデータディレクトリ変更・空のファイル定義 |
| 06_TestFWGuide/04_MasterDataRestore.rst | 215 | 低 | マスタデータ復旧機能の説明。テストデータ記述仕様への直接言及なし |
| 06_TestFWGuide/RequestUnitTest_send_sync.rst | 156 | 高 | 同期応答メッセージ送信テスト：EXPECTED_REQUEST_HEADER/BODY_MESSAGES・RESPONSE_HEADER/BODY_MESSAGES の Excel 書式・errorMode の説明 |
| 06_TestFWGuide/RequestUnitTest_http_send_sync.rst | 23 | 中 | HTTP同期応答メッセージ送信テストの差分説明（send_sync に準拠） |
| 06_TestFWGuide/RequestUnitTest_batch.rst | 262 | 高 | バッチ用テストデータ：SETUP_FIXED/SETUP_VARIABLE/EXPECTED_FIXED/EXPECTED_VARIABLE の Excel 書式・日本語データ型・デフォルトディレクティブ設定・空ファイル定義・符号付数値型の注意 |
| 06_TestFWGuide/RequestUnitTest_rest.rst | 361 | 低 | RESTfulウェブサービスのリクエスト単体テスト。テストデータ記述仕様への直接言及なし |
| 05_UnitTestGuide/02_RequestUnitTest/send_sync.rst | 296 | 高 | 同期応答メッセージ送信テスト実施方法の詳細・識別子書式・no列・フィールド名重複禁止・マルチレコード時のヘッダ繰り返し記述 |
| 05_UnitTestGuide/02_RequestUnitTest/http_send_sync.rst | 164 | 高 | HTTP同期応答メッセージ送信テスト実施方法・file-type によるアサート方式切り替え・JSON/XML 制約 |
| 05_UnitTestGuide/02_RequestUnitTest/batch.rst | 619 | 高 | バッチ・リクエスト単体テスト実施方法の詳細・固定長/可変長ファイルの詳細記述・testShots LIST_MAP・0xプレフィクスバイナリ表記 |
| 05_UnitTestGuide/01_ClassUnitTest/index.rst | 7 | 低 | クラス単体テストの目次のみ |

---

## 2. 解説書に記載されているテストデータ仕様

各ドキュメントから読み取ったテストデータ仕様を列挙する。

### 2.1 テーブルデータ（SETUP_TABLE / EXPECTED_TABLE 等）

| 仕様 | 根拠（ドキュメント） | スキーマ対応状況 |
|---|---|---|
| `SETUP_TABLE=テーブル名` の書式（グループIDなし） | 01_Abstract.rst, 02_DbAccessTest.rst | 反映済み（`setup_tables[].table`） |
| `SETUP_TABLE[グループID]=テーブル名` の書式 | 03_Tips.rst | 反映済み（`setup_tables[].group_id`） |
| `EXPECTED_TABLE` は省略カラムを比較対象外にする | 02_DbAccessTest.rst, 03_Tips.rst | 反映済み（design.md §4） |
| `EXPECTED_COMPLETE_TABLE` は省略カラムにデフォルト値を補完して比較 | 02_DbAccessTest.rst | 反映済み（schema.json description） |
| デフォルト値（数値型=0, 文字列型=半角スペース, 日付型=`1970-01-01 00:00:00.0`） | 02_DbAccessTest.rst | **一部未反映**（解説書の日付デフォルト値が `1970-01-01 00:00:00.0` と UTC 表記。design.md §4 には JVM タイムゾーン依存の詳細は記載されているが、解説書は UTC 基準の単一値 `1970-01-01 00:00:00.0` を公式値として提示している） |
| `BasicDefaultValues` の設定項目（`charValue`, `numberValue`, `dateValue`）をコンポーネント設定ファイルで変更可能 | 02_DbAccessTest.rst | **未反映**（schema.json / design.md にデフォルト値のカスタマイズ設定方法が記載されていない） |
| 主キーカラムは省略不可（SETUP_TABLE） | 02_DbAccessTest.rst | **未反映**（schema.json の description に記載なし） |
| `assertTableEquals` はレコード順序に依存しない（主キーで突合） | 02_DbAccessTest.rst | 対象外（アサートAPI仕様。スキーマ設計の範囲外） |
| `assertSqlResultSetEquals` はレコード順序が異なる場合アサート失敗 | 02_DbAccessTest.rst | 対象外（アサートAPI仕様） |
| `java.sql.Timestamp` 型カラムの期待値表示形式: `2010-01-01 12:34:56.0`（末尾 `.0` が必要） | 02_DbAccessTest.rst | **未反映**（Timestamp 型の期待値記述時に末尾 `.0` が必要である旨が schema.json / design.md に記載されていない） |
| グループIDを使用する場合、同一データタイプごとにまとめて記述すること（混在禁止） | 01_Abstract.rst, 03_Tips.rst | **未反映**（schema.json / design.md に `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` を混在させてはならない制約の明示がない） |
| グループIDに `default` を指定するとグループIDなし扱いと同等になる（バッチ固有） | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | **未反映**（design.md に `default` グループIDの特殊扱いが記載されていない） |
| 日付フォーマット `yyyyMMddHHmmssSSS` および `yyyy-MM-dd HH:mm:ss.SSS` が有効 | 01_Abstract.rst | 反映済み（design.md §7、examples.yaml） |
| 日付フォーマットは時刻・ミリ秒省略可（`yyyyMMddHHmmss`, `yyyyMMdd`, `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd`） | 01_Abstract.rst | **一部未反映**（design.md には後置0埋め仕様として記載があるが、解説書は `yyyyMMddHHmmss`（12桁）も明示。design.md に12桁形式の明示なし） |
| セルの書式は文字列のみ使用すること | 01_Abstract.rst | 反映済み（examples.yaml の NG 例コメント） |
| `\\n` はセル内改行（LF）に変換される（`LineSeparatorInterpreter` 経由） | 01_Abstract.rst | **未反映**（examples.yaml の特殊値一覧に `\\n` → LF の変換が記載されていない。`\\r` → CR のみ記載） |

### 2.2 ファイルデータ（SETUP_FIXED / SETUP_VARIABLE 等）

| 仕様 | 根拠（ドキュメント） | スキーマ対応状況 |
|---|---|---|
| `SETUP_FIXED[グループID]=ファイルパス` の書式 | batch.rst（05_UnitTestGuide） | 反映済み（`setup_files[].group_id`, `path`） |
| ディレクティブ行はフィールド名行の直前に0行以上記述 | batch.rst（05_UnitTestGuide） | 反映済み（schema.json `directives`） |
| レコード種別 → フィールド名 → データ型 → フィールド長 → データ の順で記述 | batch.rst（05_UnitTestGuide） | 反映済み（schema.json `record_fragment`） |
| 可変長ファイルはフィールド長を記載しない | batch.rst（05_UnitTestGuide） | 反映済み（schema.json `field_def.length` 省略可） |
| データ型は日本語名称（`半角英字`, `数値` 等）で記述する | send_sync.rst、batch.rst（05_UnitTestGuide） | **未反映**（YAML では型記号 `X`, `Z` 等を直接記述する設計。ただし「日本語名称は Excel 向け」であり YAML 移行後の設計方針は design.md §5 に記載あり。解説書の「日本語名称で記述する」という説明が YAML スキーマと乖離している点の明示がない） |
| `file-type` ディレクティブは固定長テストデータでは記述不要 | batch.rst（05_UnitTestGuide）, send_sync.rst（05_UnitTestGuide） | 反映済み（schema.json `file-type` description に「自動設定のため通常は記述不要」と記載） |
| `record-length` ディレクティブはフィールド長合計から自動計算されるため記述不要 | batch.rst（05_UnitTestGuide）, send_sync.rst（05_UnitTestGuide） | 反映済み（schema.json `record-length` description） |
| デフォルトディレクティブ（`defaultDirectives`, `fixedLengthDirectives`, `variableLengthDirectives`）をコンポーネント設定ファイルで一括設定可能 | 06_TestFWGuide/RequestUnitTest_batch.rst | 反映済み（design.md §14） |
| フィールド名の重複は禁止（同一レコード種別内） | batch.rst（05_UnitTestGuide）, send_sync.rst（05_UnitTestGuide） | 反映済み（schema.json `fields` description） |
| 異なるレコード種別間では同一フィールド名が存在してもよい | batch.rst（05_UnitTestGuide） | **未反映**（schema.json / design.md に「同一レコード種別内で重複禁止だが、異なる種別間は許容」という明示がない） |
| `field-separator=\t` でタブ区切りを指定可能（可変長ファイル） | batch.rst（05_UnitTestGuide） | 反映済み（examples.yaml, schema.json） |
| 空のファイル（0バイト）を定義する場合、ディレクティブ行のみ記述しレコード定義を省略する | 03_Tips.rst, batch.rst（05_UnitTestGuide） | **未反映**（schema.json の `file_data.records` は `minItems: 1` のため空ファイルを表現できない。解説書には「ディレクティブ行のみ記述、レコード定義省略で空ファイル」と明示） |
| バイナリデータは `0x` プレフィクス付き16進数で記述（例: `0x4AD`）。`0x` がない場合は文字列として解釈 | batch.rst（05_UnitTestGuide） | **未反映**（examples.yaml では `${binaryFile:path}` による参照形式のみ記載。`0x` プレフィクス形式の16進数直接記述という別の記法が存在することが未記載） |
| 符号付/符号無数値型（`X9`/`SX9`）使用時はパディング・符号を含めた固定長フォーマットの実値をそのまま記載すること | batch.rst（05_UnitTestGuide） | **未反映**（design.md / schema.json に X9/SX9 フィールドへのデータ記述方法の注意事項がない） |
| 符号付/符号無数値型を使用する場合、`TEST_X9`/`TEST_SX9` コンバータ設定が必要 | batch.rst（05_UnitTestGuide） | 反映済み（design.md §16 TEST_ プレフィクス型の説明として記載） |

### 2.3 メッセージデータ（MESSAGE / RESPONSE_* 等）

| 仕様 | 根拠（ドキュメント） | スキーマ対応状況 |
|---|---|---|
| 識別子書式: `EXPECTED_REQUEST_HEADER_MESSAGES[グループID]=リクエストID` 等 | send_sync.rst（05_UnitTestGuide） | 反映済み（schema.json `expected_request_header_messages`, `group_id`, `id`） |
| `no` 列（先頭列）は Excel 上必須。フレームワークが除去してデータには含めない | send_sync.rst（05_UnitTestGuide） | 反映済み（design.md §12 の説明） |
| 複数レコード送信時にヘッダと本文データが交互に並ぶ必要がある（ヘッダの繰り返し記述） | send_sync.rst（05_UnitTestGuide） | **未反映**（schema.json / design.md に「マルチレコード送信時はヘッダとボディ行数が一致し、ヘッダを送信回数分繰り返す」制約が明示されていない） |
| `errorMode:timeout` / `errorMode:msgException` を最初のフィールド（`no` を除く先頭フィールド）に設定で障害系テスト可能 | send_sync.rst, http_send_sync.rst（05_UnitTestGuide） | 反映済み（examples.yaml, design.md §11） |
| 同一リクエストIDで複数回送信する場合、`no` の値を変えて連続記述する | send_sync.rst（05_UnitTestGuide） | **未反映**（design.md / examples.yaml に `no` 値と複数回送信の関連が記載されていない） |
| HTTP同期応答メッセージ送信処理では `file-type` の値により項目単位/バイト列一括のアサート方式が切り替わる | http_send_sync.rst（05_UnitTestGuide） | 反映済み（design.md §19） |
| HTTP送信のメッセージボディは各行の文字列長が同一であることが必要（JSON/XML制約） | http_send_sync.rst（05_UnitTestGuide） | **未反映**（schema.json / design.md に HTTP メッセージの行長統一制約が記載されていない） |
| FW制御ヘッダフィールドはデフォルト `requestId`, `userId`, `resendFlag`, `resultCode` の4つ | send_sync.rst | 反映済み（schema.json `message_data.records` description） |

### 2.4 その他（LIST_MAP、特殊値、ディレクティブ等）

| 仕様 | 根拠（ドキュメント） | スキーマ対応状況 |
|---|---|---|
| `LIST_MAP=ID` の書式。ID はシート内で一意 | 01_Abstract.rst, 03_Tips.rst | 反映済み（`list_maps[].id`） |
| `LIST_MAP` の `testShots` は バッチ単体テストのテストケース一覧として使用される特別 ID | batch.rst（05_UnitTestGuide） | **未反映**（`testShots` が LIST_MAP の特定用途として使われることが design.md / schema.json に記載されていない） |
| `null`（大文字/小文字不問）で DB NULL を表現 | 01_Abstract.rst | 反映済み（examples.yaml, design.md §7） |
| `"null"` でダブルクォート除去後に文字列 `null` を格納 | 01_Abstract.rst | 反映済み（examples.yaml） |
| `""` で空文字列を表現 | 01_Abstract.rst | 反映済み（examples.yaml） |
| `"⊔"` や `"△"` のようにダブルクォートでスペースを明示する記法 | 01_Abstract.rst | **未反映**（examples.yaml / design.md にスペース値の QuotationTrimmer 活用例が記載されていない） |
| `"""` でダブルクォート1文字を表現 | 01_Abstract.rst | **未反映**（examples.yaml / design.md に QuotationTrimmer によるダブルクォート1文字の表現方法が記載されていない） |
| `${systemTime}`, `${updateTime}`, `${setUpTime}` で日時特殊値 | 01_Abstract.rst | 反映済み（examples.yaml） |
| `${文字種,文字数}` で文字種生成（14種。解説書には中国語・サロゲートペア・改行・外字の4種は記載なし） | 01_Abstract.rst | **一部未反映**（解説書が列挙する有効文字種は11種のみ。design.md では14種を正確に記載しており差異あり。解説書が公式ドキュメントとして11種と記載している点の注記がない） |
| `${binaryFile:パス}` でバイナリファイルを BLOB に格納（パスは Excel ファイルからの相対パス） | 01_Abstract.rst | 反映済み（examples.yaml, design.md §21） |
| `\\r` で CR(0x0D)、`\\n` で LF(0x0A) に変換（`LineSeparatorInterpreter`） | 01_Abstract.rst | **一部未反映**（examples.yaml の特殊値一覧に `\\r` → CR は記載あり。`\\n` → LF の変換が記載されていない） |
| 可変長ファイルの空行をテストデータとして含めたい場合は `""` を左端セルに記述する | 03_Tips.rst | 反映済み（design.md §ファイル系注意事項の空行動作で言及） |
| テストデータ読み込みディレクトリは `nablarch.test.resource-root` プロパティで変更可能（セミコロン区切りで複数指定可） | 03_Tips.rst | 対象外（フレームワーク設定。スキーマ設計の範囲外） |
| `TestDataConverter_<データ種別>` キーで TestDataConverter を登録してデータ変換処理を追加可能 | 03_Tips.rst | 反映済み（design.md §17） |
| メッセージングテスト固有: テストデータファイルは `sendSyncTestData` ベースパス下のリクエスト ID と同名ファイルを使用 | design.md §18（コードから） | 反映済み（design.md §18 に記載済み） |

---

## 3. 未反映仕様まとめ

| # | 仕様 | 根拠ドキュメント | 追加すべき箇所 |
|---|---|---|---|
| Doc-1 | `BasicDefaultValues` の `charValue`, `numberValue`, `dateValue` プロパティをコンポーネント設定ファイルで変更可能（デフォルト値のカスタマイズ） | 06_TestFWGuide/02_DbAccessTest.rst | design.md §4（EXPECTED_COMPLETE_TABLE の説明） |
| Doc-2 | SETUP_TABLE では主キーカラムは省略不可。EXPECTED_TABLE では省略カラムは比較対象外になる（登録系テストでは全カラム記述が必要） | 06_TestFWGuide/02_DbAccessTest.rst | schema.json `table_data.rows` description / design.md 注意事項 |
| Doc-3 | `java.sql.Timestamp` 型カラムの期待値は末尾 `.0`（ゼロ）が必要（例: `2010-01-01 12:34:56.0`）。この形式でないとアサートが失敗する | 06_TestFWGuide/02_DbAccessTest.rst | design.md §7（日付型カラムの記述形式） / examples.yaml |
| Doc-4 | `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` は同一シート内で混在させると後半のデータが読み込まれない（データタイプごとにまとめて記述する必要あり） | 06_TestFWGuide/01_Abstract.rst（`auto-test-framework_multi-datatype` セクション） | design.md（注意事項として追加） |
| Doc-5 | グループID指定時に `default` という文字列を使用するとグループIDなし扱いと同等になり、グループIDなしデータと同時に使用可能 | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | design.md §8（グループIDなしの場合の説明） |
| Doc-6 | 日付フォーマットとして `yyyyMMddHHmmss`（12桁、ミリ秒省略）が有効（解説書に明示。design.md には後置0埋めとして間接的に含まれるが明示なし） | 06_TestFWGuide/01_Abstract.rst | design.md §7（日付型カラムの記述形式）に `yyyyMMddHHmmss` を明示追加 |
| Doc-7 | `\\n` はセル内の改行コード指定として LF(0x0A) に変換される（`LineSeparatorInterpreter`）。`\\r` は CR に変換されると examples.yaml に記載あるが `\\n` → LF が未記載 | 06_TestFWGuide/01_Abstract.rst | examples.yaml の特殊値一覧テーブル / design.md AI向けプロンプト |
| Doc-8 | ダブルクォートで囲むことでスペース値を明示できる（例: `"⊔"` → 半角スペース1文字、`"△△"` → 全角スペース2文字）。`"""` でダブルクォート1文字を格納可能 | 06_TestFWGuide/01_Abstract.rst（特殊記法テーブル） | design.md §7（特殊値の表現）/ examples.yaml の特殊値コメント |
| Doc-9 | 固定長/可変長ファイルデータにおいて、異なるレコード種別間では同一フィールド名が存在してもよい（同一種別内のみ禁止） | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | schema.json `fields` description / design.md |
| Doc-10 | 空のファイル（0バイトファイル）を定義するには、ディレクティブ行のみ記述してレコード定義を省略する。現行スキーマの `records: minItems: 1` では空ファイルを表現できない | 06_TestFWGuide/03_Tips.rst, batch.rst（05_UnitTestGuide） | schema.json `file_data.records` の `minItems` を 0 に変更（設計変更）/ design.md に空ファイル表現方法を追記 |
| Doc-11 | バイナリデータの直接記述: `0x` プレフィクス付き16進数（例: `0x4AD`）でバイナリ値を記述可能。`0x` がない場合は文字列としてエンコードされる | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | examples.yaml の バイナリ型フィールドの例 / design.md |
| Doc-12 | 符号付/符号無数値型（`X9`/`SX9`）使用時の注意：固定長ファイルから入力/出力する値（パディング文字・符号を含めた実際のバイト列表現）をそのまま記載すること | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | design.md（ファイル系のデータ型説明）/ examples.yaml |
| Doc-13 | 複数回メッセージ送信テストでは、ヘッダと本文の行数を一致させ、送信回数分ヘッダを繰り返し記述する必要がある（マルチレコード時の制約） | 05_UnitTestGuide/02_RequestUnitTest/send_sync.rst | design.md §11（messaging の注意事項） |
| Doc-14 | `no` 列の値と複数回送信の対応関係：同一リクエストIDで複数回送信する場合は `no` の値を変えて連続記述し、送信順序と `no` 値の順番を一致させる | 05_UnitTestGuide/02_RequestUnitTest/send_sync.rst | design.md §18（SendSyncSupport の説明）/ examples.yaml の response_*_messages 例 |
| Doc-15 | HTTP同期応答メッセージ送信処理のボディ行長制約：各行の文字列長が同一であることが必要（JSON/XML データ形式使用時の制約） | 05_UnitTestGuide/02_RequestUnitTest/http_send_sync.rst | design.md §20（messaging のフォーマット定義）|
| Doc-16 | LIST_MAP の `testShots` ID は、バッチリクエスト単体テストでフレームワークが自動的にテストケース一覧として読み込む予約 ID | 05_UnitTestGuide/02_RequestUnitTest/batch.rst | design.md §9（SingleData 系の制約）|
| Doc-17 | 解説書の `${文字種,文字数}` 有効文字種は11種として記載（中国語・サロゲートペア・改行・外字の4種が欠如）。design.md は14種が正確だが、公式ドキュメントとの差異を注記する必要あり | 06_TestFWGuide/01_Abstract.rst | design.md AI向けプロンプトに「公式解説書では11種と記載されているが実装は14種有効」を追記 |

---

## 4. 総合評価

### 解説書から新たに判明した未反映仕様

今回の照合により、既存のスキーマ設計文書（コード調査で作成）に加えて、公式解説書から以下の追加仕様が判明した。

**スキーマ設計上の変更が必要なもの（Doc-10）:**

- 空ファイル（0バイト）表現のために `file_data.records` の `minItems: 1` を `minItems: 0` に変更する必要がある。解説書の「ディレクティブ行のみ記述、レコード定義省略で空ファイル」という仕様は、現行スキーマでは表現不可能。

**design.md への追記が必要なもの（中優先度）:**

- Doc-3: Timestamp 型期待値の末尾 `.0` 必須（アサート失敗の原因になる実務上重要な仕様）
- Doc-4: 同一シート内でのデータタイプ混在禁止（読み込みが途中で終わる罠）
- Doc-8: QuotationTrimmer によるスペース明示記法・ダブルクォート1文字の表現
- Doc-12: X9/SX9 型フィールドの記述方法（パディング込みの実値記載）
- Doc-13: マルチレコード送信時のヘッダ繰り返し記述制約

**examples.yaml への追記が必要なもの:**

- Doc-7: `\\n` → LF の特殊値変換例
- Doc-11: `0x` プレフィクス形式バイナリ記述例
- Doc-14: no 列と複数回送信の対応例

**比較的優先度が低いもの:**

- Doc-1: BasicDefaultValues カスタマイズ（高度な設定変更。通常ユーザには不要）
- Doc-5: `default` グループID の特殊扱い（バッチ固有の細かい仕様）
- Doc-15: HTTP メッセージの行長制約（HTTP 同期送信テスト固有の制約）
- Doc-16: `testShots` 予約 ID（バッチテスト固有）
- Doc-17: 文字種数の差異注記

### コードから導かれた仕様との整合性

公式解説書の内容はコード調査（P4-2）で判明した仕様と概ね整合している。主要な相違点：

1. **BasicDefaultValues のデフォルト日付値**: 解説書は `1970-01-01 00:00:00.0`（UTC基準）を記載。design.md は JVM タイムゾーン依存（JST: `1970-01-01 09:00:00.0`）を正確に記載しており、解説書の記載はやや不正確（UTC 環境での値を記載している可能性が高い）。YAML スキーマ的には design.md の記載が正確。
2. **日本語データ型名**: 解説書は Excel 記述用に日本語名称（`半角英字` 等）を使用すると説明。YAML スキーマ設計では型記号（`X`, `N` 等）を直接記述する設計であり、この違いは YAML 移行の意図的な変更として design.md §5 に記載済み。
3. **空ファイル表現**: 解説書に記載があるが現行スキーマでは `records: minItems: 1` のため未サポート（要設計変更）。
