# CC作業指示: T7 再実装（動的アプローチ）

この指示の通りに実施する。各ステップに必要なルールを記載済み。指示にない判断・追加・省略をしない。各ステップ完了ごとにユーザーレビューを受ける。

---

## 全ステップ共通の絶対ルール（各ステップでも再掲する）

- 着手前に `mvn test`（スコープを絞らない）で全テストクラスを実行し、グリーン状態を記録する。これをベースラインとする。環境要因（DB等）で失敗するクラスがあれば、対象から除外せず、必要な環境をユーザーに連絡し、整備後に実行する。「環境が必要なので未実施」での完了は禁止。
- ステップ完了時も `mvn test` 全クラス実行し、ベースラインから新たな失敗を増やしていないこと（差分ゼロ）を確認する。
- 指示にない制約・検証・ルールを独自に追加しない。追加が必要と判断したら、実装せずユーザーに提案して指示を仰ぐ。
- 命名は目的・意図ベースにする。手段・形式・現状の数（dual 等）に依存する名前を付けない。
- TDD: テストコードを変更・追加するステップは RED（失敗を確認）→ GREEN の順で行う。
- 1ステップ = 1レビュー単位。セルフ/QA/言語/SWEレビューの後、ユーザーレビューを受けてから次へ進む。
- root 権限環境で `setReadable(false)`/`listFiles()==null` 系の3テストが偽陰性で失敗するのは既知。これは環境要因として扱い、CC の非root環境でグリーンなら問題なし。

---

## STEP 1: T7 等価照合実装を全リバート

### 作業
1. 現在の HEAD から、コミット `02ddd3e`（T7開始）以降の全 T7 コミットをリバートし、`afb866b`（T6完了時点）の状態に戻す。
2. リバートにより以下が消えることを確認する: `T7EquivalenceTest.java`、`T7MessagingEquivalenceTest.java`、`ExcelToYamlEquivalenceTest.java` への T7 追加分、T7 で生成した YAML テストデータ、`docs/pr75/checks/T7.md` の等価照合記述、steering の T7 完了記述。
3. 一部だけ残すリバート（特定行・特定ファイルだけ戻す）はしない。T7 範囲は全リバートでクリーンにする。NTF 本体の NPE 修正も一旦消えるが、STEP 2 で入れ直すため問題ない。

### 完了条件（チェック）
- HEAD が T6 完了時点（`afb866b`）相当の内容に戻っている。
- 上記ファイル・記述が消えている。
- `mvn test` 全クラスがベースライングリーン。

### このステップのルール
- 着手前・完了時に全クラス `mvn test`。
- リバートは全リバート。部分剥がし禁止。

---

## STEP 2: NPE 修正を TDD で再投入（独立コミット）

### 背景
messaging で length なしフィールドを持つ電文を YAML から読むと `YamlFileBuilder`/`YamlMessageBuilder` で NPE になる不具合がある（STEP 1 のリバートで一旦消える）。これを独立して入れ直す。

### 作業
1. RED: length なしフィールドを持つ messaging YAML を読み込むと NPE が再現することを示すテストを追加し、失敗を確認する。
2. GREEN: `YamlFileBuilder`/`YamlMessageBuilder` を修正し、length なしフィールドを正しく扱えるようにする。
3. T7（後続ステップ）とは独立したコミットにする。

### 完了条件（チェック）
- RED→GREEN を確認した。
- 追加テストを含め `mvn test` 全クラスグリーン。

### このステップのルール
- RED を先に確認してからGREEN。
- 着手前・完了時に全クラス `mvn test`。

---

## STEP 3: 変換ツールの共通入口を構造化インタフェースで切り出す

### 背景
現状 `TestDataConverter.run(String[] args)` が引数解析と変換ロジックを両方持つ。CLI / 将来の Maven プラグイン / 後続 STEP の Runner が共通で呼べる入口を作る。

### 作業
1. 変換ロジックの共通入口メソッドを新設する。引数は `String[]` ではなく、変換の意図を表す構造化された型にする（変換元形式・変換先形式・入力パス・出力パス・必要なオプションをフィールドに持つ型を定義し、それを受け取る）。型名・フィールド名は目的ベースで命名する。
2. CLI の `main`/`run(String[])` は「引数解析 → 構造化型を組み立て → 共通入口を呼ぶ」だけの薄いアダプタにする。変換ロジックは共通入口側に置く。
3. この共通入口は `nablarch/test/tool/converter/` 配下に置く。
4. 今回スコープ: 共通入口を作るところまで。Maven プラグイン（Mojo）本体は作らない。

### 完了条件（チェック）
- 変換ロジックの共通入口が、構造化型を引数に取るメソッドとして存在する（`String[]` を直接受け取らない）。
- CLI 経由の既存変換テストがグリーン。
- 共通入口を構造化型で直接呼ぶテストを追加し、グリーン。
- `mvn test` 全クラスグリーン。

### このステップのルール
- 共通入口の引数に `String[]` を使わない。構造化型にする。
- 命名は目的ベース。
- 着手前・完了時に全クラス `mvn test`。

---

## STEP 4: パッケージ整理（YAML対応の集約）

### 作業
1. 以下を `nablarch/test/core/reader/yaml/` に移動する:
   - `src/main` の `YamlTestDataParser.java`（現在 `core/reader/` 直下）
   - `src/test` の `YamlTestDataParserTest.java`、`YamlSchemaValidationTest.java`（現在 `core/reader/` 直下）
2. 移動に伴う package 宣言・import を修正する。
3. 既存 Excel 系テスト（`BasicTestDataParserTest` 等）は移動しない。
4. 変換ツール（`tool/converter/`）は現状の配置のまま。STEP 3・STEP 5 で追加するクラスもこの配下に置く。

### 完了条件（チェック）
- 上記3ファイルが `core/reader/yaml/` にあり、package 宣言が一致している。
- `mvn test` 全クラスグリーン。

### このステップのルール
- 移動対象は上記3ファイルのみ。それ以外を動かさない。
- 着手前・完了時に全クラス `mvn test`。

---

## STEP 5: テストデータ駆動テスト用 Runner の新規作成

詳細は **[CC作業指示-T7-STEP5.md](CC作業指示-T7-STEP5.md)** を参照する。

---

## STEP 6: 対象テストクラスへ Runner を適用

### 対象クラス（この一覧の通りにする。判断不要）

YAML 対応の検証対象＝「TestDataParser 経由でデータを読む業務テスト」。以下19クラス。

すでに `@RunWith(DatabaseTestRunner.class)` が付いている9クラス（→ STEP5 の Runner に差し替える）:
1. AbstractHttpRequestTestTemplateTest
2. BatchRequestTestSupportTest
3. DBtoDBBatchSampleTest
4. DbAccessTestSupportTest
5. FileToFileBatchSampleTest
6. MessagingReceiveTestSupportTest
7. MessagingRequestTestSupportTest
8. SimpleBatchSampleTest
9. TestSupportTest

`@RunWith` が付いていない10クラス（→ STEP5 の Runner を新規に付与する）:
10. EntityTestSupportTest
11. FileSupportTest
12. FileSupportWithDbLessTestDataParserTest
13. HttpRequestTestSupportTest
14. RequestTestingMessagingClientTest
15. RequestTestingMessagingContextTest
16. RequestTestingSendSyncBatchTest
17. RequestTestingSendSyncSupportTest
18. TestBeanTest
19. TestEntityTest

### 対象外クラス（Runner を付けない。判断不要）
以下4クラスは Excel 実装そのものを `new` して検証する単体テストであり、TestDataParser 境界を通らないため対象外。触らない。
- BasicTestDataParserTest（BasicTestDataParser 自体のテスト）
- VariableLengthFileParserTest（VariableLengthFileParser 自体のテスト）
- MessageParserTest（MessageParser 自体のテスト）
- PoiXlsReaderTest（PoiXlsReader 自体のテスト）

### 作業
1. 上記19クラスの `@RunWith` を STEP5 の Runner に設定する（9クラスは差し替え、10クラスは新規付与）。テストコード本体は変更しない。
2. 19クラスを実行し、Excel 入力・YAML 入力の両方で全テストがパスすることを確認する。
3. YAML 入力で失敗するテストがあれば、それは NTF の YAML 対応または変換ツールの不具合である。原因を特定して修正する。修正は先送りせず、このステップ内で完了させる。修正が大きい場合はユーザーに状況を報告して指示を仰ぐ。
4. 着手前に、STEP1 リバート後の状態でも対象19クラス・対象外4クラスが上記の通りであることを確認する。差異があればユーザーに報告する。

### 完了条件（チェック）
- 対象19クラスに Runner が設定されている。対象外4クラスは未変更。
- 対象19クラスが Excel 入力・YAML 入力の両方で全テストグリーン。
- 環境（DB等）が必要なクラスは、環境を整備した上でグリーンを確認（除外しない）。
- `mvn test` 全クラスグリーン。

### このステップのルール
- 対象は上記19クラスのみ。対象外4クラスを触らない。一覧外のクラスを勝手に追加しない。
- YAML 入力で落ちたら不具合として修正。先送り・対象除外をしない。
- 着手前・完了時に全クラス `mvn test`。

---

## STEP 7: 仕上げ

### 作業
1. `docs/pr75/checks/T7.md` を本作業（STEP1〜6）の完了条件で記入する。
2. steering の Ph-6 タスクリストを本作業の実態に合わせて更新する（旧 T7 等価照合の記述を本ステップ群に置換）。
3. 静的アプローチ（仕様リスト×テスト×実装マッピング）と動的アプローチ（本作業: 対象テストが Excel/YAML 両入力でパス）の両方が揃ったことを記録する。

### 完了条件（チェック）
- T7.md・steering が実態と一致している。
- `mvn test` 全クラスグリーン。

### このステップのルール
- 着手前・完了時に全クラス `mvn test`。
