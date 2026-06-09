# CC作業指示: T7 動的アプローチ実装（YAML版テストクラス追加方式）

この指示の通りに実装する。設計・修正方針は全て確定済み（試作で実証済み）。指示にない判断・追加・省略をしない。判断が必要に見える箇所があれば、実装せずユーザーに確認する。報告時は実際にプッシュし、実コードに反映してから行う（プッシュ前の完了報告は不可）。

## 目的と方式（確定）
NTF の YAML 対応を、利用者と同じ移行手順（Excel データを YAML に変換し、testDataParser を YAML 版に切替）を適用した状態で、既存テストロジックを再実行して品質保証する。

方式（試作で成立を実証済み）:
- 対象テストごとに「YAML 版テストクラス」を追加する。`XxxYamlTest extends XxxTest`。
- YAML 版は、既存テストのテストメソッドを継承で再利用する（テストロジックの重複を作らない）。
- 既存テスト（Excel 版）と製品コード（src/main）は、原則変更しない（例外は後述の YamlLoader バグ修正のみ）。

## 対象クラス（18クラス。この通り。判断不要）
TestDataParser を使う 18 クラス。各クラスに対応する YAML 版（`<クラス名>YamlTest`）を追加する。
1. AbstractHttpRequestTestTemplateTest
2. BatchRequestTestSupportTest
3. DBtoDBBatchSampleTest
4. DbAccessTestSupportTest
5. FileToFileBatchSampleTest
6. MessagingReceiveTestSupportTest
7. MessagingRequestTestSupportTest
8. SimpleBatchSampleTest
9. TestSupportTest
10. EntityTestSupportTest
11. FileSupportTest
12. FileSupportWithDbLessTestDataParserTest
13. RequestTestingMessagingClientTest
14. RequestTestingMessagingContextTest
15. RequestTestingSendSyncBatchTest
16. RequestTestingSendSyncSupportTest
17. TestBeanTest
18. TestEntityTest

対象外（YAML 版を作らない）: HttpRequestTestSupportTest（TestDataParser 非使用）、BasicTestDataParserTest / VariableLengthFileParserTest / MessageParserTest / PoiXlsReaderTest（Excel 実装の単体テスト）。

## 実装手順

### STEP A: YamlLoader のパス連結バグ修正（製品バグ修正・正当）
YamlLoader が basePath と resourceName を "/" なしで連結しており、resource-root が末尾 "/" なしで渡る正規経路でファイルを解決できない（Excel 側 PoiXlsReader は "/" を入れている）。これは YAML 対応の実装バグ。
- 対象: `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java` の `load`（filePath 生成）と `isResourceExisting` の 2 箇所。
- 修正: basePath と resourceName を、間に "/" を 1 つだけ挟んで連結する（basePath が末尾 "/" 付きの場合は足さない）。ヘルパーメソッドを 1 つ作り両箇所で使う。
- これはテスト都合でなく、YAML 対応のパス解決バグの修正である。
- 既存の YamlTestDataParserTest（basePath 末尾 "/" 付きで呼ぶ）も壊れないこと（"/" 二重化を避ける実装にする）。

### STEP B: YAML 版テストクラスの共通基底クラスを作る
YAML 生成と testDataParser 差し替えを 1 箇所に局所化するため、共通基底クラスを作る（クラス名は目的ベース）。各 YAML 版クラスはこれを継承して使う。基底クラスの責務:
1. YAML 生成（クラス単位、@BeforeClass 相当）:
   - 自クラス名・パッケージを `getClass()` から動的に取得する（ハードカ禁止）。
   - 変換元: 既存 Excel データのあるディレクトリ（src/test/java の当該パッケージ）。
   - 変換先: `target/generated-test-yaml/<パッケージ>/`。
   - 変換は STEP3 の共通入口 `TestDataConverter.convert(ConversionRequest)` を呼ぶ。
   - include 指定は拡張子込みグロブにする（例 `"FileSupportTest.xls"`。`"FileSupportTest"` ではマッチしない）。
   - 変換出力ディレクトリ名は元ブック名（元 Excel ファイル名）になる。テストは自クラス名（YAML 版クラス名）で探すため、変換後に「元ブック名ディレクトリ」を「自クラス名ディレクトリ」へ複製する。
   - テスト実行前に、生成先の自クラス名ディレクトリの古い YAML を削除してから生成・複製する（上書きでなく削除してから。古い YAML でテストが動くことを防ぐ）。
2. testDataParser 差し替え（テストメソッドごと、@Before 相当）:
   - `repositoryResource.addComponent("testDataParser", new YamlTestDataParser())`
   - `repositoryResource.addComponent("nablarch.test.resource-root", "target/generated-test-yaml")`
   - 注意: SystemRepositoryResource.before() が DI を再ロードするため、差し替えは @Before（before() の後）で行う。
   - 注意: repositoryResource は @Rule のため public 必須。既存テストで private の場合は、継承先から差し替えできるよう既存テスト側のアクセス修飾子を調整してよい（テストコード変更は可）。ただし @Rule フィールドは public を維持する。

### STEP C: 各 YAML 版クラス（18個）を追加
各対象クラスに対し `<クラス名>YamlTest extends <クラス名>` を作り、STEP B の基底クラスの仕組みを使う。テストメソッドは書かない（継承で再利用）。

### STEP D: YAML モードで失敗するテストの修正（YAML 対応バグ）
YAML 版を実行すると、YAML 対応の不具合で失敗するテストがある。これらは全て YAML 対応のバグとして原因を特定し修正する（先送り・対象除外しない）。試作で確認済みの代表:
- 空セルの表現バグ: Excel の空セルが、YAML 変換で空文字列でなく「ダブルクオート文字 2 個（`"\"\""`）」として出力される。変換ツール（YamlFormatWriter 等）の空文字列処理の不具合。空文字列として正しく出力する。
- 重複フィールド名の例外型差: 既存テストが Excel 経路特有の例外（IllegalStateException）を期待しているが、YAML 経路では下流の DataFileFragment まで進み IllegalArgumentException が出る。原因を特定し、YAML 対応として正しい挙動に揃える。期待値の差がテスト側の Excel 依存である場合の扱いは、原因特定後に判断が要るためユーザーに報告する。
- 上記以外の失敗も、原因を特定して YAML 対応の不具合として修正する。

### STEP E: 既存 Runner の削除
STEP5 で作成した NtfTestdataTestRunner は本方式では使わない。削除する。

## 完了条件
- YamlLoader のパス連結バグが修正され、既存 YamlTestDataParserTest も含めグリーン。
- 対象 18 クラスそれぞれに YAML 版クラスが追加され、Excel 版（既存）と YAML 版の両方が全テストグリーン。
- 既存テスト（Excel 版）のテストロジック・テストデータは変更していない（アクセス修飾子の調整は可、@Rule は public 維持）。
- 製品コード（src/main）の変更は、YamlLoader のパス連結バグ修正と、STEP D の YAML 対応バグ修正のみ（テスト専用メソッドの追加は禁止）。
- 生成 YAML は `target/generated-test-yaml/` 配下にのみ作られる。
- `target/generated-test-yaml/` を .gitignore に追加し、生成 YAML を git 管理対象外にする。
- NtfTestdataTestRunner が削除されている。
- mvn test 全クラスグリーン（既知の環境失敗を除く）。DB 必須クラスは DB 環境で確認する。

## このステップのルール（遵守）
- 着手前・完了時に mvn test 全クラスを実行し、ベースラインから新規失敗ゼロを確認する。環境要因（DB 等）で落ちるテストは対象から除外せず、必要な環境をユーザーに連絡する。
- 製品コード（src/main）には、テスト専用のメソッド・引数・クラスを追加しない。YAML 対応のバグ修正（パス連結・空セル・例外型等）は製品の正当な修正として可。
- クラス名・パッケージはハードコードせず getClass() から動的取得する。
- YAML 生成・parser 差し替えのロジックは共通基底クラス 1 箇所に局所化する。各 YAML 版クラスに散らさない。
- 生成 YAML はテスト前に削除してから生成する（古い YAML で動かさない）。
- 命名は目的ベース。手段・形式・現状の数に依存する名前を付けない。
- STEP D で判断が必要な失敗（例外型の揃え方など）は、実装せずユーザーに報告する。それ以外は判断余地なく本指示通り実装する。
