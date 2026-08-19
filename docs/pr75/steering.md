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

**新事象（期待値0件テーブルの偽陰性）**：上記の判定作業の中で発見した事象のうち、**本体で修正するのは1件（#23）**。判断根拠・後方互換影響・実測結果・起票案は `docs/pr75/docs/ntf-empty-table-assertion.md` に記載。

- 問題1（#23・**本体で修正する**）: 期待値のカラム名が0件だと `TableData#loadData()` が DB を読まず、DB に行が残っていても検証が必ず PASS する（偽陰性）。YAML の `rows: []` はカラム名を書く場所が構造上無いため、必ずここに落ちる
- 問題2（**本体では修正しない**）: 表形式リーダが識別子行の次の行を無条件にカラム名行として読むため、識別子行が連続すると後続ブロックが消える。ただし Excel 記法はデータ行が0件でもカラム名の行を書くのが仕様であり（記載例・実データとも。同 3.1）、記法どおりに書けば発生しない。**本体は変更せず**、converter 側で「Excel 書き出し時にマーカーカラム行を出す」対応を行う（別リポジトリ・スコープ外。同 付録B.1）

---

# Acceptance criteria

- YAML形式相当の構成（`sendSyncTestData`に拡張子未設定）で、テストデータ更新後に次回読み出しで再読み込みされる（事象3）
- YAML形式相当のマスタデータファイルが `MasterDataSetUpper` で投入できる（事象4後半）
- Excel形式の既存動作・既存テストに変化がない（後方互換維持、#21・#22とも）
- 事象3・事象4の対応が、それぞれ独立した「再現テストのみのコミット」→「修正コミット」に分かれている（事象をまたいで束ねられていない）
- 期待値のカラム名が0件でも DB の実データが読まれ、DB に行が残っていれば検証が失敗する（新事象・問題1）
- 表形式リーダ（`TableDataParser`・`ListMapParser`・`TestDataParsingTemplate`）に変更が無い（問題2 は本体では修正しない）
- 新事象の対応に、現行動作へ戻すための設定は追加されていない

# Assumptions

- 事象1・事象2・事象4前半（無言0件の挙動）は判定済みでこの追加フェーズのスコープ外。判定を覆す新事実が出た場合はこの前提が崩れる
- `nablarch-core`（`FilePathSetting`）・`nablarch-testing-yaml` 側の変更は不要という前提で#21・#22を設計している。実装時にこの前提が崩れた場合はユーザーに相談する
- **#23 は不具合修正ではなく、YAML 対応に必要な本体変更として起票している**（`ntf-empty-table-assertion.md` 3.2）。Excel 記法だけの世界では記法どおりの書き方でこの経路に到達せず、不具合として顕在化したことがない。YAML の `rows: []` が正規の入力になったことで初めて通る
- **問題2 も不具合ではない**（同 3.1・6.1）。Excel 記法はデータ行が0件でもカラム名の行を書く。記載例（`testdata_examples.rst`「0件のテーブルデータを記述する」）がそうであり、本リポジトリの実データにも該当が5件ある（同 5.2）。この判定を覆す事実が出た場合は本前提が崩れる
- カラム名の行は表形式（Excel）の記法要素であって、データモデルの構成要素ではない。YAML はモデルを表す形式なので `rows: []` で0件を表せる。したがって本体の対応は #23（形式共通の `TableData`）に閉じる
- 対象PJのテストデータは未走査。#23 の修正で FAIL するデータが対象PJに無いという確証は無い（同 3.5・9章）。ただし #23 の分岐に入るのは記法違反のデータと「ヘッダ行がマーカーカラムのみ」の2形だけであり、記法どおりの0件テーブルは1件も挙動が変わらない（同 5.1）
- `nablarch-testing-yaml` の `@Ignore` 4件の解除と FIXME 削除は別リポジトリ・別セッションの作業であり、本 steering のスコープ外。#23 が本体に入り `install` された後に実施する
- `nablarch-testing-converter` のマーカーカラム行の出力対応（`XlsFormatWriter.java:233-240` の番人の置き換え）は問題2 への恒久対応であり、本 steering のスコープ外（同 付録B.1）。#23 とは独立に着手できるが、先に入れると変換後の Excel は #23 が入るまで偽陰性のままになる

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
- **追加フェーズ（事象3・4＋新事象対応）専用ルール**:
  - 事象・問題ごとに再現テスト→修正の単位でコミットを分ける。事象をまたいで1コミットに束ねない
  - 実装修正は、失敗する再現テストをコミットした後にのみ行う。再現テスト未コミットの状態で実装を直さない
  - 後方互換維持: Excel形式の既存動作・既存テストへの影響がないことを確認してから修正コミットする
  - `nablarch-core`（`FilePathSetting`）には変更を加えない
  - 各タスクの着手は、ユーザーの明示的な合図を待ってから開始する

---

# 追加フェーズのタスク（事象3・4＋新事象）

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


### #23: 期待値0件テーブルでも DB の実データを読む（新事象・問題1）

**Purpose**: 期待値のカラム名が0件のとき `TableData#loadData()` が DB を読まずに0行を返し、DB に行が残っていても検証が必ず PASS してしまう偽陰性をなくす。カラム名の宣言は「検証対象カラムの絞り込み」であって「検証するかどうか」のスイッチではない。YAML の `rows: []` はカラム名を書く場所が構造上無いため必ずこの経路に落ちる。**不具合修正ではなく、YAML 対応に必要な本体変更**である（`ntf-empty-table-assertion.md` 3章）。

**Prerequisites**: #22（後方互換に影響するため、依頼分を先に片付けてから着手する。技術的には独立）

**Steps**:

- [ ] 失敗する再現テストを追加する（DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を `Assertion.assertTableEquals` に渡し、現状では PASS してしまうことを示す）
- [ ] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [ ] `TableData#loadData()`（`TableData.java:337-346`）の early return を削除し、`colNames.length == 0` のとき `dbInfo.getColumns(tableName)` を SELECT 対象カラムとする。`getColumnNames()` は変更しない（`ntf-empty-table-assertion.md` 4章）
- [ ] 再現テストが通ることを確認する
- [ ] 既存テストが壊れていないことを確認する（`mvn -o test` 全件。残存4Eを除く）
- [ ] 修正をコミットする

**Completion criteria**:

- カラム名0件の期待値でも DB が読まれ、DB に行が残っていれば検証が失敗することが自動テストで示されている
- 修正が `loadData()` 内に閉じており、`getColumnNames()`・準備データ投入・マスタデータ投入・変換ツール経路に変更がない
- 現行動作へ戻すための設定が追加されていない
- 表形式リーダ（`TableDataParser`・`ListMapParser`・`TestDataParsingTemplate`）に変更が無い
- `nablarch-testing-yaml` の `@Ignore` 4件（`ntf-empty-table-assertion.md` 付録B.2）が解除待ちであることが、引き継ぎとして記録されている

---

# State

(written by /rn:dn, read and reset to this placeholder by /rn:up. `Status` is `paused` while a
session is suspended — the signal /rn:up and /rn:dn search for — and resets to `not suspended` here,
so only a genuinely suspended session reads `paused`.)

- **Status**: not suspended
- **Date**: YYYY-MM-DD
- **Last completed**: #N description
- **Next**: #N description
- **Notes**: bounded forward pointer — branch/PR, next concrete action, open blockers, user-deferred paths, open questions / pending decisions not yet captured in `design.md`; not a re-narration of the session (that lives in `git log`)

---

# 成果物

| 種別 | ファイル |
|---|---|
| **変換ツール設計書（正）** | docs/testdata-converter-design.md |
| **読み込み機構の解説** | docs/ntf-testdata-loading.md |
| **NTF テストデータ解説書** | docs/ntf-testdata-doc.md |
| **期待値0件テーブル偽陰性 — NTF 本体への変更提案（#23 の正・Nablarch 本体チーム宛）** | docs/ntf-empty-table-assertion.md |
| **後片付け手順（yaml）** | divide/cc2-hontai-cleanup.md |
| **後片付け手順（converter）** | divide/cc2-converter-cleanup.md |
