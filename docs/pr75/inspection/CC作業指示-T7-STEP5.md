# CC作業指示: T7 STEP5（テストデータ駆動テスト用 Runner の新規作成）

この指示の通りに実装する。設計判断は本指示で確定済み。指示にない判断・追加・省略をしない。判断が必要に見える箇所があれば、実装せずユーザーに確認する。

## 目的
テストデータを使う NTF 本体テストを、Excel 入力でも、その Excel から生成した YAML 入力でも、同じテストコードがそのまま通ることを保証する（NTF の YAML 対応の仕様）。本 STEP では、それを実現する Runner を1つ作る。STEP6 でこの Runner を対象テストに適用する。

## 確定済みの設計（この通りに作る）

### Runner クラス
- `nablarch.test.support.db.helper.DatabaseTestRunner` を継承した Runner を新規作成する。
- クラス名は目的ベース（テストデータを使うテスト向けであることを表す名前。例: `NtfTestdataTestRunner`）。配置は適切なテスト基盤パッケージ（既存テスト基盤と同じ階層）。
- `runChild(FrameworkMethod, RunNotifier)` をオーバーライドし、各テストメソッドを次の2回実行する。
  - 1回目（Excel入力）: 既存の設定のまま `super.runChild(...)` を呼ぶ。`testDataParser` は Excel パーサ（`BasicTestDataParser`）のまま。
  - 2回目（YAML入力）: 後述の前処理で YAML 入力に切り替えてから、同じテストメソッドを実行する（`super.runChild(...)` を再度呼ぶ）。実行後に後処理で元に戻す。
- DB 制御は親 `DatabaseTestRunner` を継承して温存する（`super.runChild`/`super.run` を活かす）。
- 比較はしない。各入力でテストの assert が通ればよい。

### 2回目（YAML入力）の前処理・後処理（確定）
1. 変換: 対象テストが使う Excel テストデータを、STEP3 の共通入口 `TestDataConverter.convert(ConversionRequest)` で YAML に変換する。
   - 変換元: テストの Excel データのルート（resource-root が指すディレクトリ配下）。
   - 変換先: `target/` 配下の専用ディレクトリ（例: `target/generated-test-yaml/`）。ソースツリー（`src/test`）には生成物を置かない。
   - ディレクトリ構造（resourceName = "book/sheet" の相対構造）は変換元と同じに保つ。同じ resourceName で拡張子が `.yaml` になるだけにする。
2. パーサ差し替え: `SystemRepository` の `testDataParser` を `YamlTestDataParser` に一時的に差し替える。
3. 入力ルート差し替え: `SystemRepository` の `nablarch.test.resource-root`（キー定数 `RESOURCE_ROOT_KEY`）を、変換先の `target/generated-test-yaml/` に一時的に差し替える。これにより resourceName・テストコードを変えずに YAML が読まれる。
4. テスト実行: `super.runChild(...)` を呼ぶ。
5. 後処理（必ず finally で実行）: `SystemRepository` の `testDataParser` と resource-root を、2回目実行前の値に復元する。Excel 入力（1回目）に影響を残さない。

### 局所化（確定）
- 変換・差し替え・復元のロジックは、この Runner クラス1箇所に閉じる。対象テスト側には一切書かない。
- 対象テストは `@RunWith` をこの Runner に変えるだけ（STEP6）。テストコード本体は変更しない。

### IDE 単体実行対応（確定）
- 変換・差し替えは Runner の `runChild` 内で行うため、IDE で単一テストクラスを実行しても2回実行される（Maven フェーズに依存しない）。

## やらないこと（確定）
- 生成 YAML をソースツリー（`src/test`）に置かない。
- テストコード本体を変更しない（STEP6 の `@RunWith` 変更を除く）。
- 比較ロジックを入れない（assert で判定）。
- 対象テストへの適用は STEP6 で行う。本 STEP では Runner 作成とサンプル1クラスでの動作確認まで。

## 実装上の確認事項（着手時に実コードで確認し、想定と違えばユーザーに報告）
- `SystemRepository` の値を一時差し替え・復元する正しい手段（既存テストの `SystemRepositoryResource` の仕組みに倣う）。差し替え後に確実に復元できる方法にする。
- `getPathResourceExisting` が複数候補パスを探索する作りのため、resource-root 差し替え時に YAML が確実に解決されること（変換先ディレクトリが候補パスの起点になること）を確認する。
- messaging のバイナリ等、Excel 以外に basePath 相対で読む付随ファイルがある場合、それも変換先に揃うか確認する。揃わない場合はユーザーに報告して指示を仰ぐ（勝手な回避をしない）。

## 完了条件（チェック）
- Runner が `DatabaseTestRunner` を継承している。
- サンプルのテストクラス1つ（テストデータを使うもの。例: `FileSupportTest`）にこの Runner を付けて実行すると、各テストメソッドが Excel 入力・YAML 入力の2回実行され、両方パスする（ログ等で2回実行を確認）。
- `mvn test -Dtest=サンプルクラス`（IDE 単体実行相当）でも2回実行される。
- 2回目の後、`testDataParser`・resource-root が復元され、他テストに影響しない。
- 生成 YAML は `target/` 配下にのみ作られ、`src/test` に生成物がない。
- `mvn test` 全クラスで、STEP4 完了時のベースラインから新規失敗がない（既知の環境失敗を除く）。

## このステップのルール（遵守）
- 着手前・完了時に `mvn test` 全クラスを実行し、ベースラインから新規失敗ゼロを確認する。環境要因（DB等）で失敗するクラスがあれば、対象から除外せず、必要な環境をユーザーに連絡し、整備後に実行する。「環境が必要なので未実施」での完了は禁止。
- Runner 名・メソッド名・定数名は目的ベース。手段・形式・現状の数（dual 等）に依存する名前を付けない。
- 変換・差し替え・復元ロジックは Runner 1箇所に集約。対象テストに散らさない。
- 比較ロジックを入れない。
- 指示にない制約・検証・回避策を独自に追加しない。判断が要るときは実装せずユーザーに確認する。
