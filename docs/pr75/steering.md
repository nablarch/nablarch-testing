Rn version: 0.8.0

# NTF テストデータ変換ツール 再構築フェーズ（設計書 6.3 到達）

ブランチ: `convert-testdata-excel-to-text`
ドラフト PR: [lovaizu/nablarch-testing#1](https://github.com/lovaizu/nablarch-testing/pull/1)

**設計書 `docs/pr75/docs/testdata-converter-design.md` が唯一の正。**

---

# Goal

Excel↔YAML テストデータ変換ツールを設計書通りに作り直し、品質担保 Level1〜3（単体テスト・往復変換・既存テスト全件 PASS）を達成する。

**→ 達成済み。** Phase 1〜6（#1〜#20、J-4/J-5）全タスク完了。リポジトリ分割フェーズへ移行。

**追加フェーズ**：`nablarch-document`（`ntf-yaml-support`ブランチ）の解説書作り直しの過程で判定を依頼された、テスティングフレームワーク本体の4事象のうち、不具合と判定された事象3、および対応可と判定された事象4のYAML対応を、TDDで実装する。

- 事象3: YAML形式のテストデータで `SendSyncSupport` の再読み込み（タイムスタンプ変更検知）が働かない
- 事象4: `MasterDataSetUpper` がYAML形式のマスタデータファイルに対応していない

事象1（解説書側の対応のみ）・事象2（現状維持）・事象4の「無言で0件になる」挙動（YAML導入前からの既存仕様として現状維持）は判定済みでスコープ外。

---

# Acceptance criteria

- YAML形式相当の構成（`sendSyncTestData`に拡張子未設定）で、テストデータ更新後に次回読み出しで再読み込みされる（事象3）
- YAML形式相当のマスタデータファイルが `MasterDataSetUpper` で投入できる（事象4後半）
- Excel形式の既存動作・既存テストに変化がない（後方互換維持、#21・#22とも）
- 事象3・事象4の対応が、それぞれ独立した「再現テストのみのコミット」→「修正コミット」に分かれている（事象をまたいで束ねられていない）

# Assumptions

- 事象1・事象2・事象4前半（無言0件の挙動）は判定済みでこの追加フェーズのスコープ外。判定を覆す新事実が出た場合はこの前提が崩れる
- `nablarch-core`（`FilePathSetting`）・`nablarch-testing-yaml` 側の変更は不要という前提で#21・#22を設計している。実装時にこの前提が崩れた場合はユーザーに相談する

---

# Rules

- **1 task = 1 commit・push 必須**
- released NTF 本体プロダクションコードの変更は**事前にユーザー相談**
- `pom.xml` の `M` 差分（parent `6-NEXT-SNAPSHOT`→`6u3`）は既知のローカル変更。コミット不要
- **Java/Maven 環境**: OpenJDK 17 (Temurin-17.0.19) + Maven 3.9.9。`compile`・`test`・`install` はすべて Java 17 で実行する
- テスト実行は必ず `LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o test`
- 残存 4E（`MockHttpRequestTest`/`MockServletExecutionContextTest`）は PR75 非起因の既知事象
- **カバレッジ取得**: 親 POM に JaCoCo Offline Instrumentation 設定済み。`pom.xml` 変更不要。
  ```
  mvn clean jacoco:instrument test jacoco:restore-instrumented-classes jacoco:report \
    [-Dtest="YamlTestDataReaderTest,..."]
  mvn jacoco:report -Djacoco.dataFile=/home/tie303177/work/nablarch-testing/jacoco.exec
  ```
  出力先は `${user.dir}/jacoco.exec`（プロジェクトルート）。`target/site/jacoco/` に HTML 生成。
- **追加フェーズ（事象3・4対応）専用ルール**:
  - 事象ごとに再現テスト→修正の単位でコミットを分ける。事象をまたいで1コミットに束ねない
  - 実装修正は、失敗する再現テストをコミットした後にのみ行う。再現テスト未コミットの状態で実装を直さない
  - 後方互換維持: Excel形式の既存動作・既存テストへの影響がないことを確認してから修正コミットする
  - `nablarch-core`（`FilePathSetting`）には変更を加えない
  - 各タスクの着手は、ユーザーの明示的な合図を待ってから開始する

---

# 追加フェーズのタスク（事象3・4対応）

### #21: SendSyncSupport のYAML形式再読み込み対応

**Purpose**: `sendSyncTestData`に拡張子を設定しない構成（YAML形式相当）で、テストデータ更新後に再読み込みされるようにする。Excel形式の既存動作は変えない。

**Prerequisites**: none

**Steps**:

- [ ] 失敗する再現テストを追加する（リクエストID同名ディレクトリ配下のファイルを書き換え後、2回目の読み出しで1件目の応答電文に戻らないことを示す）
- [ ] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [ ] `SendSyncSupport.createTestDataInfo` を修正する（`file.isDirectory()`の場合、配下ファイルの最大`lastModified()`を実効タイムスタンプとする）
- [ ] 再現テストが通ることを確認する
- [ ] 既存テスト（`MockMessagingContextTest#test6`含む）が壊れていないことを確認する
- [ ] 修正をコミットする

**Completion criteria**:

- YAML形式相当の構成（`sendSyncTestData`に拡張子未設定）で、配下ファイルを更新すると次回読み出しで1件目から応答電文が返る（再読み込みされる）ことが自動テストで示されている
- Excel形式（`sendSyncTestData`に拡張子設定あり）の既存挙動に変化がなく、既存テストが全件成功する
- 修正が`SendSyncSupport.createTestDataInfo`内で完結し、`FilePathSetting`（`nablarch-core`）に変更がない

### #22: MasterDataSetUpper のYAML形式マスタデータファイル対応

**Purpose**: マスタデータファイルの拡張子がExcel（`.xls`/`.xlsx`）以外の場合、POIによるシート列挙を経由せず`TestDataParser`へ1リソースとして直接問い合わせることで、YAML形式のマスタデータ投入を可能にする。Excel形式の既存動作は変えない。

**Prerequisites**: #21（依頼順に従い先に着手。技術的には独立）

**Steps**:

- [ ] 失敗する再現テストを追加する（YAML形式相当のマスタデータファイルを投入し、現状ではテーブルデータが得られない/例外になることを示す）
- [ ] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [ ] `MasterDataSetUpper.getAllTableData` を修正する（マスタデータファイルの拡張子で分岐し、Excel以外は`parser.getSetupTableData(dir, masterFileName)`を1回呼ぶ）
- [ ] 再現テストが通ることを確認する
- [ ] 既存のExcel形式マスタデータ投入テストが壊れていないことを確認する
- [ ] 修正をコミットする

**Completion criteria**:

- YAML形式相当のマスタデータファイルを指定した場合、そのファイル内の複数テーブルデータが投入されることが自動テストで示されている
- Excel形式（`.xls`）のマスタデータファイルは、シート列挙を含め既存と同じコードパスで処理され、既存テストに変化がない
- Excel形式＋YAML用パーサという取り違えケースの挙動（無言0件）は変更されていない（事象4の当該部分の判定は現状維持のため）

---

# State

- **Status**: paused
- **Date**: 2026-08-18
- **Last completed**: 期待値0件テーブル検証の偽陰性（新事象）の対応方針を検討し、`docs/pr75/docs/ntf-empty-table-assertion.md` に整理（steering未起票）
- **Next**: 同文書の内容にユーザーの合意を得る。合意後、#23（問題1: `TableData#loadData` が0件カラムでもDBを読む）・#24（問題2: 表形式リーダが識別子行をカラム名行として食わない）を起票してから、#21 の再現テスト作成に着手する
- **Notes**: ブランチ `convert-testdata-excel-to-text` / ドラフトPR lovaizu/nablarch-testing#1。Phase 1〜6 はユーザー承認済みでマージ待ち。#21・#22 は解説書チームからの4事象判定依頼のうち事象3・4のTDD対応で、着手はユーザーの明示的な合図待ち。**未決事項3件**: (1) `ntf-empty-table-assertion.md` の内容合意、(2) 同文書を上記「成果物」表に追加するか、(3) #23・#24 の起票可否。新事象は #21・#22 とは別系統（後方互換影響あり）で、問題1は既存テストが新たにFAILし得るため段階投入の要否も要判断。解説書チームへの回答報告（4事象＋新事象）は本steeringのスコープ外・別途必要。

---

# 成果物

| 種別 | ファイル |
|---|---|
| **変換ツール設計書（正）** | docs/testdata-converter-design.md |
| **読み込み機構の解説** | docs/ntf-testdata-loading.md |
| **NTF テストデータ解説書** | docs/ntf-testdata-doc.md |
| **後片付け手順（yaml）** | divide/cc2-hontai-cleanup.md |
| **後片付け手順（converter）** | divide/cc2-converter-cleanup.md |
