Rn version: 0.8.0

# Goal

`nablarch-document`（`ntf-yaml-support`ブランチ）の解説書作り直しの過程で判定を依頼された、テスティングフレームワーク本体の4事象のうち、不具合と判定された事象3、および対応可と判定された事象4のYAML対応を、TDDで実装する。

- 事象3: YAML形式のテストデータで `SendSyncSupport` の再読み込み（タイムスタンプ変更検知）が働かない
- 事象4: `MasterDataSetUpper` がYAML形式のマスタデータファイルに対応していない

事象1（解説書側の対応のみ）・事象2（現状維持）・事象4の「無言で0件になる」挙動（YAML導入前からの既存仕様として現状維持）は、いずれも判定済みでこのセッションのスコープ外。

# Acceptance criteria

- 事象3が、失敗する再現テスト→テストのみコミット→実装修正→修正コミットの順で修正されている
- 事象4のYAML形式マスタデータ対応が、同様の順で実装されている
- 事象3・事象4それぞれ、Excel形式の既存動作・既存テスト（`MockMessagingContextTest#test6`等）に影響がない（後方互換維持）
- 事象3と事象4のコミットが明確に分かれている。同一コミットに複数事象の変更を含まない
- `nablarch-core`（`FilePathSetting`）に変更がない

# Assumptions

- 事象1・2・4「無言で0件」は対応不要。判定確定済み（このセッションの会話履歴が根拠。解説書チームへの回答としては別途report要）
- `nablarch-testing-yaml`（別リポジトリ、`nablarch-testing`に依存）側の実装変更は不要という前提。`SendSyncSupport`・`MasterDataSetUpper`の修正は`nablarch-testing`内で完結する設計としたため
- 事象3の再現テストは、本物のYAMLパーサ（`YamlTestDataParser`、現ブランチ未収録）を使わず、`sendSyncTestData`に拡張子を設定しない構成＋テスト専用の最小限`TestDataParser`実装で再現する — 未確認: 着手時に実際に組んでみて妥当性を確認する
- 事象4の再現テストは、実ファイル拡張子（`.yaml`等）による分岐なので、既存の`TestDataParser`実装（テスト専用のものでよい）で再現可能と見込む — 未確認: 同上

# Rules

- 事象ごとに再現テスト→修正の単位でコミットを分ける。事象をまたいで1コミットに束ねない
- 実装修正は、失敗する再現テストをコミットした後にのみ行う。再現テスト未コミットの状態で実装を直さない
- 後方互換維持: Excel形式の既存動作・既存テストへの影響がないことを確認してから修正コミットする
- released NTF本体プロダクションコードの変更は事前にユーザー相談（`nablarch-core`は対象外、`nablarch-testing`のみ）
- 各タスクの着手は、ユーザーの明示的な合図を待ってから開始する

# Tasks

### #1: SendSyncSupport のYAML形式再読み込み対応

**Purpose**: `sendSyncTestData`に拡張子を設定しない構成（YAML形式相当）で、テストデータ更新後に再読み込みされるようにする。Excel形式の既存動作は変えない。

**Prerequisites**: none

**Steps**:

- [ ] 失敗する再現テストを追加する（リクエストID同名ディレクトリ配下のファイルを書き換え後、2回目の読み出しで1件目の応答電文に戻らないことを示す）
- [ ] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [ ] `SendSyncSupport.createTestDataInfo` を修正する（`file.isDirectory()`の場合、配下ファイルの最大`lastModified()`を実効タイムスタンプとする）
- [ ] 再現テストが通ることを確認する
- [ ] 既存テスト（`MockMessagingContextTest#test6`含む）が壊れていないことを確認する
- [ ] 修正をコミットする
- [ ] self-check（OK/NG、`checks/task-1.md`に記録）
- [ ] QA expert review（subagent）
- [ ] Craft expert review（subagent）
- [ ] Verification expert review（subagent）

**Completion criteria**:

- YAML形式相当の構成（`sendSyncTestData`に拡張子未設定）で、配下ファイルを更新すると次回読み出しで1件目から応答電文が返る（再読み込みされる）ことが自動テストで示されている
- Excel形式（`sendSyncTestData`に拡張子設定あり）の既存挙動に変化がなく、既存テストが全件成功する
- 修正が`SendSyncSupport.createTestDataInfo`内で完結し、`FilePathSetting`（`nablarch-core`）に変更がない

### #2: MasterDataSetUpper のYAML形式マスタデータファイル対応

**Purpose**: マスタデータファイルの拡張子がExcel（`.xls`/`.xlsx`）以外の場合、POIによるシート列挙を経由せず`TestDataParser`へ1リソースとして直接問い合わせることで、YAML形式のマスタデータ投入を可能にする。Excel形式の既存動作は変えない。

**Prerequisites**: none（#1と技術的には独立。依頼順に従い#1の後に着手）

**Steps**:

- [ ] 失敗する再現テストを追加する（YAML形式相当のマスタデータファイルを投入し、現状ではテーブルデータが得られない/例外になることを示す）
- [ ] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [ ] `MasterDataSetUpper.getAllTableData` を修正する（マスタデータファイルの拡張子で分岐し、Excel以外は`parser.getSetupTableData(dir, masterFileName)`を1回呼ぶ）
- [ ] 再現テストが通ることを確認する
- [ ] 既存のExcel形式マスタデータ投入テストが壊れていないことを確認する
- [ ] 修正をコミットする
- [ ] self-check（OK/NG、`checks/task-2.md`に記録）
- [ ] QA expert review（subagent）
- [ ] Craft expert review（subagent）
- [ ] Verification expert review（subagent）

**Completion criteria**:

- YAML形式相当のマスタデータファイルを指定した場合、そのファイル内の複数テーブルデータが投入されることが自動テストで示されている
- Excel形式（`.xls`）のマスタデータファイルは、シート列挙を含め既存と同じコードパスで処理され、既存テストに変化がない
- Excel形式＋YAML用パーサという取り違えケースの挙動（無言0件）は変更されていない（事象4の当該部分の判定は現状維持のため）

# State

- **Status**: paused
- **Date**: 2026-08-18
- **Last completed**: (未着手)
- **Next**: #1 の再現テスト作成から開始（ユーザーの明示的な合図待ち）
- **Notes**: 4事象の判定はユーザーとの会話で確定済み（事象1=仕様/解説書側対応、事象2=現状維持、事象3=不具合/このsteeringで対応、事象4前半=仕様/現状維持、事象4後半のYAML対応=このsteeringで対応）。解説書チームへの回答報告はこのsteeringのスコープ外・別途必要。着手はユーザーの「進めて」等の明示的指示を待つ。
