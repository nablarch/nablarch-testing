Rn version: 0.8.0

# NTF テストデータ変換ツール 再構築フェーズ（設計書 6.3 到達）

ブランチ: `convert-testdata-excel-to-text`
ドラフト PR: [nablarch/nablarch-testing#75](https://github.com/nablarch/nablarch-testing/pull/75)（head `convert-testdata-excel-to-text` / base `develop`。`origin` もこのリポジトリ。2026-08-21 `gh pr list --head` で実測）<br>※ 従来ここに書いていた `lovaizu/nablarch-testing#1` は head が `add-yaml` で本ブランチの PR ではない（同日 `gh pr view` で実測）

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
- 以前は残存 4E（`MockHttpRequestTest`/`MockServletExecutionContextTest`）を PR75 非起因の既知事象としていたが、2026-08-19 の全件実行では発生しない（`LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test` → `Tests run: 841, Failures: 0, Errors: 0, Skipped: 7`。実装エキスパートと Verification エキスパートが別々に実行し同結果）。**テストの失敗・エラーは原則すべて対処対象として扱う**
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

- [x] 失敗する再現テストを追加する（リクエストID同名ディレクトリ配下のファイルを書き換え後、2回目の読み出しで1件目の応答電文に戻らないことを示す）→ `df0ea24`
- [x] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）→ `df0ea24`
- [x] `SendSyncSupport.createTestDataInfo` を修正する（`file.isDirectory()`の場合、ディレクトリ自体と配下エントリの`lastModified()`を**順序非依存に畳み込んだ署名**を実効値とする。`listFiles()` の順序は保証されないため畳み込み前に `Arrays.sort` する。署名は `createTestDataInfo(DataType, String)` の冒頭で1度だけ、`getMessages` の**前**に採る）→ `15f4dbb`（最大値方式で実装）→ 方式変更コミットで署名方式へ差し替え
- [x] 再現テストが通ることを確認する
- [x] 既存テスト（`MockMessagingContextTest#test6`含む）が壊れていないことを確認する
- [x] 修正をコミットする → `15f4dbb`
- [x] ディレクトリ用タイムスタンプの実装方式を確定する（1ラウンド目 指摘A）→ 署名方式で確定（2026-08-19 ユーザー判断）→ 2026-08-20 のユーザー判断で **Map スナップショット方式（選択肢 C）へ再確定**
- [x] 1ラウンド目のレビュー指摘 B〜G を修正する → `b22e5b9`
- [x] 既存テスト全件が成功することを確認し、修正をコミットする → `b22e5b9`（`Tests run: 848, Failures: 0, Errors: 0, Skipped: 7`）
- [x] 修正後に QA・Craft・Verification を再実行する（2ラウンド目）→ QA=fail / Craft=fail / Verification=conditional pass。判定と根拠は `checks/21.md` に記録
- [x] 畳み込みの設計を A/B/C から確定する（下記「レビュー指摘（2ラウンド目）」H）→ **C を採用**（2026-08-20 ユーザー判断。A・B は採用しない）。`Map<相対パス, lastModified>` のスナップショットを `equals` 比較する方式に差し替え、`Arrays.sort` と畳み込みを削除
- [x] レビュー指摘 I〜T を修正する（下記の表）→ 12件すべて処置。採否と根拠は `checks/21.md`「修正ラウンド2」
- [x] 修正後に QA・Craft・Verification を再実行し、`checks/21.md` のレビュー欄を更新する（3ラウンド目。`71a60c3` に対し隔離 worktree で独立実行）→ **3者とも conditional pass**。指摘 U〜AH の14件は下記の表と `checks/21.md`「レビューラウンド3」
- [x] レビューラウンド3 の指摘 U〜AH を処置する（下記の表）→ 14件すべて処置。`src/main` の変更は Javadoc・コメントのみ。処置内容とゲート1〜10 の実測は `checks/21.md`「修正ラウンド3」
- [x] 既存テスト全件が成功することを確認し、修正をコミットする（`Tests run: 851, Failures: 0, Errors: 0, Skipped: 7`。基準 848 ＋ 追加3件。ゲート1〜10 の実測は `checks/21.md`）

**レビュー指摘（2ラウンド目。QA・Craft・Verification を `b22e5b9` に対して再実行）**

1ラウンド目の指摘 A〜G は `b22e5b9` で処置済み（判定と根拠は `checks/21.md`）。H〜T は 2026-08-20 の修正ラウンド2 で全件処置済み（ゲート1〜10 の実測を含む記録は `checks/21.md`「修正ラウンド2」）。

| # | 指摘 | 出所 | 対応 |
|---|---|---|---|
| H | `Arrays.sort(files)`（`SendSyncSupport.java:438`）を削除しても全848テストが通る。`listFiles()` は未変更ディレクトリに対し同一プロセス内で決定的に同じ順序を返すため、走査順非依存という主眼は原理的にテストで検出できない。かつ `SendSyncSupportTest.java:234` の Javadoc は検証していないことを断言している | QA・Craft・Verification（別 worktree で独立に実測、3者一致） | **C を採用**（2026-08-20 ユーザー判断。A・B は採用しない）。署名をやめ `Map<相対パス, lastModified>` のスナップショットを `equals` 比較する。`Arrays.sort` は削除（走査順非依存が Map の等価判定により構造的に保証され、テストで担保する必要自体が消えた） |
| I | `SendSyncSupport.java:441` の畳み込みは直下ファイル `f` とサブディレクトリ `s` の係数がどちらも 31 になり（`d*961 + 31f + 31s + n`）、両者の `lastModified` が入れ替わると署名が一致する。確率的ではなく**構造的な衝突クラス**。`:419` の「確率的に残る」は不正確 | Craft (Medium)（実測再現。コーディネーターが算術でも確認） | **修正不要**（C の採用により解消）。畳み込みを行わないため構造的衝突が原理的に存在しない。「確率的に残る」の記述も Javadoc ごと削除 |
| J | 「署名を `getMessages` の前に1度だけ採る」（2026-08-19 ユーザー制約3）が無検証。採取を読み込み後に戻しても全テストが通る | QA・Verification（独立に一致、実測） | **修正済み**。`testReloadWhenTestDataIsUpdatedWhileReading` を追加（読み込み中に mtime を動かすテスト用リーダを差し込む）。採取を読み込み後に移す変異で本テストが落ちることを実測（ゲート8） |
| K | `SendSyncSupport.java:52` の `/** Excel情報のキャッシュ */` が、ディレクトリ形式も入るようになった実態と不一致 | Craft (Low) | **修正済み**。`読み込んだテストデータ情報のキャッシュ` に改めた |
| L | `SendSyncSupportTest.java:306` が `setReadable(false)` を `Assume` より**前**に `assertTrue` しており、読み取り権限の概念がない環境ではスキップでなく失敗する。`FileUtilsTest.java:505-509` の `assumeNotWindows()` が確立した作法で、`:395-396` `:428-429` は権限操作の前に置いている | Craft (Medium)（前例をコーディネーターが実物で確認） | **修正済み**。`assumeNotWindows()` を権限操作の前に置き、`setReadable(false)` の結果は `Assume.assumeTrue` で受ける |
| M | `SendSyncSupportTest.java:315` の `finally` 内 `assertTrue` が本来の失敗を隠す。`restoreTestData`（`:97-106`）も同様にループ内 `assertTrue` で、1件目の復元失敗時に残りが未復元のまま終わる | Craft (Low)・QA | **修正済み**。`finally` は復元結果を受けるだけとし表明は try を抜けた後に置いた。`restoreTestData` は全件復元を試み失敗ファイルをまとめて1度だけ表明する |
| N | 却下した最大値方式を `SendSyncSupportTest.java:326-336` にテスト側で再実装し、`:290` でその性質を表明している。production コードを何も検証していない | Craft (Medium-Low) | **修正済み**。`maxLastModified` とそれを使う表明を削除し、production コードの戻り値の変化のみを表明する |
| O | 本ラウンドの主眼（最大値→畳み込み）が `getTimestampSignature` の直接検証1件でしか担保されておらず、E2E は素通りする | QA (Medium) | **修正済み**。`RM11AD0301/dummy.txt` を新設し `testReloadWhenFutureTimestampedFileExists` を追加。公開 API 経由で表明する |
| P | エントリの**追加・削除**のテストがない。ディレクトリ自身の mtime を畳み込みに含める設計意図（`long signature = lastModified`）は、空ディレクトリのテストに偶発的に固定されているだけ | Verification | **修正済み**。`testGetTimestampSnapshotChangesWhenEntryIsAddedOrRemoved` を追加。C ではキー集合の変化として構造的に現れる |
| Q | 非ディレクトリの早期 return（`:429-431`）は、`listFiles()` が非ディレクトリに null を返すため `files == null` 分岐と意味論的に等価で、削除しても全テストが通る。「テストで担保されている」体裁は誤解を招く | Verification (Medium)（実測） | **テストのみ修正**。`testGetTimestampSnapshotForNotDirectory` を `setLastModified` した値そのものとの比較に改め、冗長分岐である旨をコメントに明記。**分岐の冗長性は C を採っても消えなかった**（分岐を削除しても `SendSyncSupportTest` 11件が全通過することを実測。`checks/21.md`） |
| R | `testGetTimestampSignatureWhenFilesCanNotBeListed` の `Assume` スキップは root 実行の CI・Windows・WSL の DrvFs では無言で消え、その環境では `files == null` 分岐が未検証・未カバーになる | Verification (Low) | **記録済み**（`checks/21.md`「修正ラウンド2」R 欄・未カバー項目）。本実行環境では `Skipped: 0` で到達している |
| S | `SendSyncSupportTest.java:170` の期待値 `is("test2")` が `:159` と同値で、再読み込みの有無を区別できない。`FUTURE_OFFSET_MILLIS`（未来方向のオフセット）を `:248` で過去方向の刻み幅に流用している | Craft (Low) | **修正済み**。末尾に `assertResponseMessageNotExists(..., 2)` を追加し、刻み幅用に `STEP_MILLIS` を新設して `FUTURE_OFFSET_MILLIS` の流用を解消 |
| T | `getTimestampSignature` の Javadoc が15行のメソッドに26行（同ファイルの `createCacheKey`/`getMessages` は5〜7行）。`TsvTestDataReader` の open 失敗時の例外型が `PoiXlsReader.java:191`（`RuntimeException`）と不一致 | Craft (Low) | **修正済み**。Javadoc を11行に短縮し、例外型・メッセージを `PoiXlsReader` に合わせた |

**レビュー指摘（3ラウンド目。QA・Craft・Verification を `71a60c3` に対して再実行）**

`71a60c3` は方式そのものが `Map` スナップショットへ入れ替わったため、前2ラウンドの判定・根拠は流用していない（本ラウンドが初レビュー）。**3者とも「実装（`src/main`）に欠陥なし」で一致**し、指摘はテストの判別力とドキュメントの正確性に集中している。実測ログ・変異テストの全結果・性能の A/B は `checks/21.md`「レビューラウンド3」。

**U〜AH の14件は 2026-08-21 の修正ラウンド3 で全件処置済み**（処置内容とゲート1〜10 の実測は `checks/21.md`「修正ラウンド3」）。`src/main` の実行されるコードは1行も変えていない。下表の「対応」欄のうち U・V・AD・AG は作業指示でレビュー役が方針を上書きしており、上書きの内容と反映結果は `checks/21.md`「修正ラウンド3」を正とする。

| # | 指摘 | 重大度 | 出所 | 対応 |
|---|---|---|---|---|
| U | `testGetTimestampSnapshotIsStableForSameDirectory`（`SendSyncSupportTest.java:373-395`）に killer が1つも無い。同一実装の戻り値どうしを比べているだけで、実装を空洞化しても本テストだけ通過する。Javadoc が主張する「順序非依存」は `listFiles()` が決定的な順序を返す以上、原理的に表明できない | Medium | QA（14変異すべてで0件 kill） | **処置済み**（方針を上書き）。削除せず `testGetTimestampSnapshotContainsAllEntriesInTree` へ作り替え、期待するキーと値の一式との完全一致で表明する。空洞化変異で 10/11 → **11/11 kill**（ゲート3） |
| V | 非ルートのディレクトリエントリを採取する挙動が、どのテストでも固定されていない（`SendSyncSupport.java:428` を非ルートで `put` しない変異が11件全通過）。この採取が効くのは入れ子のディレクトリが `listFiles() == null` になる場合だが、既存テストは起点しか読み取り不可にしていない | Medium | Craft（0件 kill） | **処置済み**（方針を上書き）。U と同じ1つの表明で殺す（`:323` の `containsKey` は据え置き）。非ルートのディレクトリを `put` しない変異で **0件 → 1件 kill**（ゲート4） |
| W | `checks/21.md` の Completion Criteria 表・レビュー欄が `b22e5b9` のままで、削除済みメソッド名・陳腐化した行番号・旧件数（848）を Evidence として掲げている | Medium | Verification | **処置済み**。現行コードを出典とする Completion Criteria 表を `checks/21.md` 冒頭に新設し、旧表一式は末尾の「付録」へ退避（削除はしていない）。新表の識別子がすべて現行コードに存在することを確認（ゲート9。0件のものは無し） |
| X | `SendSyncSupport.java:439` のコメントが述べる `"/"` 固定の目的（実行環境をまたいだキーの一致）が実態と対応しない。スナップショットは static な `fileCache` 内で同一 JVM 内でしか比較されず、永続化・シリアライズの経路は存在しない | Low | Craft・QA（独立に一致） | **処置済み**。`/` 固定は維持し、コメントを「キーは同一JVM内のスナップショット同士の比較にしか使わないため、区切り文字は`"/"`に固定する」に改めた |
| Y | `SendSyncSupportTest.java:62-64`・`:467` 付近の「Unix環境では `lastModified()` が秒の精度でしか得られない」が実測に反する（JDK 17 で ms 精度が保持される）。実際のリスクは同一ミリ秒内の連続書き換えと `setLastModified` の FS 依存精度 | Low | QA・Verification（独立に一致） | **処置済み**。理由を「同一ミリ秒内の書き換えでは値が変化しない／`setLastModified` の保持精度はファイルシステム依存」に書き換えた。定数値 2000ms と `truncateToSecond` は変更していない |
| Z | `truncateToSecond` の Javadoc サマリ（`SendSyncSupportTest.java:467`）が「ミリ秒未満を切り捨てる」だが実装は秒未満を切り捨てる。`@return` とメソッド名は正しく、サマリ行だけが誤り | Low | Craft | **処置済み**。サマリ行を「秒未満を切り捨てる。」に修正 |
| AA | `getTimestampSnapshot` の Javadoc から、非ディレクトリ（Excel 経路）で `""` 1件だけを返すという説明が失われた。指摘 T の短縮で削りすぎている | Low | Craft | **処置済み**。「テストデータがファイルの場合（ベースパスに拡張子が設定されている場合）は、そのファイルの最終更新日時だけを起点自身のエントリとして採取する。」を戻した |
| AB | `testGetTimestampSnapshotForNotDirectory` の Javadoc（`SendSyncSupportTest.java:271`）が「Excel形式の場合」と述べるが、fixture は空の `message.xls` で Excel 経路は一切通らない。Given コメント（`:279`）は既に正しい | Low | QA | **処置済み**。Javadoc から「（Excel形式の場合）」を削除 |
| AC | `SendSyncSupportTest.java` で `import org.junit.Assume;` と `import static ...assumeThat;` が併存し、使用側も修飾形と非修飾形が混在。1ファイル内で両形式を使う前例はリポジトリに無い | Low | Craft | **処置済み**。`import org.junit.Assume;` を削除し `assumeTrue` を static import に統一（`FileUtilsTest.java:11` と同じ形） |
| AD | ユーザー制約3（採取タイミング）の killer が `testReloadWhenTestDataIsUpdatedWhileReading` の1件のみ。差し込みリーダと専用フィクスチャに依存する作り込みで、壊れると制約が無言で無防備になる | Low | Verification | **処置済み**（方針を上書き）。「唯一」は時間が経つと嘘になるため書かない。代わりに Javadoc へ「スナップショットの採取を `getMessages` の後へ移す変異は、本テストだけが検知する。採取のタイミングを変える改修を行う際は、本テストを削除・改変する前に同じ変異を試し、検知するテストが残ることを確かめること」と記載した |
| AE | mtime のみを見て内容を見ないため、mtime を保存する更新や同一ミリ秒内の連続書き換えは検知できない。Excel 形式でも従来から同じ性質 | Low | Verification | **一部処置済み**。`getTimestampSnapshot` の Javadoc に「検知の基準は最終更新日時であり、内容のハッシュではない」旨を追加。**検知基準そのものは直さない = (b) 直すと別の前提を壊す**（内容ハッシュ化は走査コストが跳ね上がり Excel 経路の既存挙動も変わる）。**本ラウンドで直さなかったのはこの1件のみ** |
| AF | リポジトリ内に深さ3以上のツリーのテストが無い（最深 fixture は深さ2）。「深さ3以上で打ち切る」変異が0件 kill。Verification は一時テストで深さ6まで正しく動くことを実測したが、回帰には残っていない | Low | QA・Verification | **処置済み**。当該テストの `TemporaryFolder` ツリーを `sub/deep/nested.txt`（深さ3）にした。深さ3以上で打ち切る変異で **0件 → 1件 kill**（ゲート5） |
| AG | `"/"` 固定の効果は Linux では検証できない（`File.separator == "/"` のため変異が0件 kill） | Low | QA | **X の処置により解消**（方針を上書き）。X を処置した結果、`SendSyncSupport.java:445` のコメントが約束していた「実行環境をまたいだキーの一致」という主張自体が無くなり、担保すべき claim が残らない。**「未カバー」ではない。** 事実として残すのは「区切り文字は `"/"` リテラルで固定しており、本実行環境（Linux）では `File.separator` と同値のため `File.separator` へ戻す変異では差が出ない」の1点のみ |
| AH | `checks/21.md:46` の「修正ラウンドは3回上限」に一次情報が無い（CC 自身が書いた1行のみ） | Low | Verification | **処置済み**。2026-08-20 ユーザー判断を出典として当該行に訂正注記を追加。回数の上限は無限ループを避ける歯止めであって品質の上限ではなく、指摘を落とす理由に使わない |

**Invalid と判定（修正しない。根拠は `checks/21.md`）**

- symlink 循環による `StackOverflowError` — QA・Craft がともに実測し発生しないことを確認（Linux の ELOOP で `isDirectory()` が false になり停止する）
- `target/test-classes` 配下を書き換えるテスト構成 — 既存の `MockMessagingContextTest.java:151-192` が同じ構成で、しかも未来 mtime を戻していない。新テストは `@After` で復元する分むしろ厳格
- Excel 経路でも署名の採取タイミングが変わる点 — 2026-08-19 のユーザー制約3そのもの
- `getTimestampSignature` を `private` へ戻す提案 — ユーザー制約5「Excel形式で今日と同一の値を返すことをテストで示す」が成立しなくなる
- `fileCache` を `@Before` でクリアしていない点 — QA が汚染なしを実測（`RM11AD0301`/`RM11AD0302` が本テスト専用であることをリポジトリ走査で確認）。既存の `MockMessagingContextTest` も同じ規約に依存しており、本変更に起因しない

**Completion criteria**:

- YAML形式相当の構成（`sendSyncTestData`に拡張子未設定）で、配下ファイルを更新すると次回読み出しで1件目から応答電文が返る（再読み込みされる）ことが自動テストで示されている
- Excel形式（`sendSyncTestData`に拡張子設定あり）の既存挙動に変化がなく、既存テストが全件成功する
- 修正が`SendSyncSupport.createTestDataInfo`内で完結し、`FilePathSetting`（`nablarch-core`）に変更がない

### #22: MasterDataSetUpper のYAML形式マスタデータファイル対応

**Purpose**: マスタデータファイルの拡張子がExcel（`.xls`/`.xlsx`）以外の場合、POIによるシート列挙を経由せず`TestDataParser`へ1リソースとして直接問い合わせることで、YAML形式のマスタデータ投入を可能にする。Excel形式の既存動作は変えない。

**Prerequisites**: #23（2026-08-20 ユーザー判断により、#23 を #22 より先に行う。技術的には独立）

**#21 の締め**: `#21` は `6007a17` で締め。4ラウンド目レビューは回さない（2026-08-21 ユーザー判断）。

**Steps**:

- [x] 失敗する再現テストを追加する（YAML形式相当のマスタデータファイルを投入し、現状ではテーブルデータが得られない/例外になることを示す）→ `a94fa08`
- [x] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）→ `a94fa08`。RED の例外は `RuntimeException: test data file open failed.`（cause `IllegalArgumentException: Your InputStream was neither an OLE2 stream, nor an OOXML stream`）
- [x] `MasterDataSetUpper.getAllTableData` を修正する（マスタデータファイルの拡張子で分岐し、Excel以外は`parser.getSetupTableData(dir, masterFileName)`を1回呼ぶ）→ `3de41ff`。**渡すリソース名は拡張子を除いた名前が正しい**（拡張子はリーダ／パーサ側が付与するため）
- [x] 再現テストが通ることを確認する → `Tests run: 7, Failures: 0, Errors: 0`
- [x] 既存のExcel形式マスタデータ投入テストが壊れていないことを確認する → 既存6件のソースに差分なし。変異2種で分岐の両方向を実測（`checks/22.md`「ゲート5」）
- [x] 修正をコミットする → `3de41ff`（`Tests run: 854, Failures: 0, Errors: 0, Skipped: 7`。基準 853 ＋ 追加1件。ゲート1〜8 の実測は `checks/22.md`）
- [x] `checks/22.md:152` の拡張子件数を実測値に直す → `ec1298f`。レビュー役の「`xls` 523件・`xlsx` 18件」はどの走査範囲でも再現しない誤りだったため、Git 追跡ファイル基準（`xls` 59件・`xlsx` 2件）と出典コマンド `git ls-files | grep -iE '\.(xls|xlsx|xlsm|xlsb)$'` に差し替えた。`xlsm`・`xlsb`・大文字拡張子0件は Git 追跡・作業ツリー（`target` 除く 295/10・含む 580/20）の3範囲すべてで一致し、本タスクの設計判断は変わらない

**Completion criteria**:

- YAML形式相当のマスタデータファイルを指定した場合、そのファイル内の複数テーブルデータが投入されることが自動テストで示されている
- Excel形式（`.xls`）のマスタデータファイルは、シート列挙を含め既存と同じコードパスで処理され、既存テストに変化がない
- Excel形式＋YAML用パーサという取り違えケースの挙動（無言0件）は変更されていない（事象4の当該部分の判定は現状維持のため）


### #23: 期待値0件テーブルでも DB の実データを読む（新事象・問題1）

**Purpose**: 期待値のカラム名が0件のとき `TableData#loadData()` が DB を読まずに0行を返し、DB に行が残っていても検証が必ず PASS してしまう偽陰性をなくす。カラム名の宣言は「検証対象カラムの絞り込み」であって「検証するかどうか」のスイッチではない。YAML の `rows: []` はカラム名を書く場所が構造上無いため必ずこの経路に落ちる。**不具合修正ではなく、YAML 対応に必要な本体変更**である（`ntf-empty-table-assertion.md` 3章）。

**Prerequisites**: #21（2026-08-20 ユーザー判断により、#22 より先に着手する。技術的には独立）

**Steps**:

- [x] 失敗する再現テストを追加する（DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を `Assertion.assertTableEquals` に渡し、現状では PASS してしまうことを示す）
- [x] テストが失敗することを確認し、テストのみをコミットする（コミットメッセージに「再現テストを追加する」旨を明記）
- [x] `TableData#loadData()`（`TableData.java:337-346`）の early return を削除し、`colNames.length == 0` のとき `dbInfo.getColumns(tableName)` を SELECT 対象カラムとする。`getColumnNames()` は変更しない（`ntf-empty-table-assertion.md` 4章）
- [x] 再現テストが通ることを確認する
- [x] 既存テストが壊れていないことを確認する（`mvn -o test` 全件。失敗・エラーは原則すべて対処対象）
- [x] 修正をコミットする
- [x] 「DBが空なら通る」側を守るテストを1件追加する（`AssertionTest#testAssertTableEqualsWithEmptyColumnNamesOnEmptyTable`。実装は変更しない）。ゲート1〜6 の実測は `checks/23.md`「追加作業（2026-08-21）」

**Completion criteria**:

- カラム名0件の期待値でも DB が読まれ、DB に行が残っていれば検証が失敗することが自動テストで示されている
- 修正が `loadData()` 内に閉じており、`getColumnNames()`・準備データ投入・マスタデータ投入・変換ツール経路に変更がない
- 現行動作へ戻すための設定が追加されていない
- 表形式リーダ（`TableDataParser`・`ListMapParser`・`TestDataParsingTemplate`）に変更が無い
- `nablarch-testing-yaml` の `@Ignore` 4件（`ntf-empty-table-assertion.md` 付録B.2）が解除待ちであることが、引き継ぎとして記録されている

---

# 仕上げフェーズのタスク（NTF 本体 YAML 対応の仕上げ）

**出典**: `/home/tie303177/work/cowork/nablarch/ntf-testing/指示/00-共通ルール.md` および
`/home/tie303177/work/cowork/nablarch/ntf-testing/指示/本体-あるべき姿とカバレッジ.md`（2026-08-21 着手）。
到達すべき状態は3つ。`@Ignore` 0件・`TODO`/`FIXME` 0件・**PR が base から変更した `src/main` の行／分岐**の C0/C1 100%。

### #24: install する（下流5リポジトリの前提）

**Purpose**: 修正後の本体を `~/.m2` へ入れ、下流リポジトリが着手できる状態にする。着手前の jar は 6/25 のもので `#21`・`#22`・`#23` がどれも入っていない。

**Prerequisites**: none

**Steps**:

- [x] `mvn -o clean install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true` を実行する → `BUILD SUCCESS`（2026-08-21 17:46:14）
- [x] `unzip -l` で `TestDataParsingTemplate`・`MasterDataSetUpper.class`・`SendSyncSupport.class` のタイムスタンプが install 実行時刻であることを確認する → 4クラスとも `2026-08-21 17:46`。jar 本体は `17:46:12.568`。**着手前の jar は指示書の言う 6/25 ではなく `2026-08-21 15:27:53` だった**（別セッションで install 済み）
- [x] 完了をユーザーに報告する（下流の並行着手の合図になる）
- [x] **`#25` 完了後に再 install する**（初回 install `17:46:12` は `#25` の `MockHttpRequest.java` 修正より前だった）→ `2026-08-21 18:28:54`。4クラスとも `18:28`。`javap -c nablarch/fw/web/MockHttpRequest.class` に `String.getBytes` 参照1件を確認。**下流が使うのはこの jar**

**Completion criteria**:

- 4クラスすべてのタイムスタンプが install 実行時刻である
- ファイル変更が無いためコミットは発生しない

### #25: 本体の TODO / FIXME を無くす（手順2）

**Purpose**: `src` 配下の `TODO`/`FIXME` を0件にする。実測2件（`MockHttpRequest.java:379` の `//FIXME`、`MockConnection.java:23` の `TODO write document comment.`）。**どちらも本 PR で入れたものではない。**

**Prerequisites**: #24

**Steps**:

- [x] 2件それぞれについて、いつ誰が入れたものかを `git log -L` 等で実測して報告する → **2件とも `eb0258d 2016-09-21 ko.i first commit`**（リポジトリ初回コミット）。本 PR で入れたものではない
- [x] `MockHttpRequest.java:379` — **推測で消さない。** まず再現テストを書き、マルチバイト本文で `Content-Length` がバイト数とずれるかを実測する → `c909181`。**ずれる**。ただしずれるのは**パラメータ名**がマルチバイトのときだけ。値は `:362` で `URLEncoder.encode(values[i], "UTF-8")` を通り必ず ASCII になるためずれない。名前は `:366` の `bodyBuffer.append(name)` でエンコードせず出力される。RED: 宣言 `Content-Length: 6` / 実体 `氏名=abc` = UTF-8 で 10 オクテット
- [x] 実測結果に応じて処置する（ずれる→直す／ずれない→なぜ問題ないかを1文のコメントにして `//FIXME` を消す）→ ずれたので直した。`bodyBuffer.toString().getBytes(StandardCharsets.UTF_8).length` を宣言する。UTF-8 を採るのは値のエンコード（`:362`）に合わせるため。**パラメータ名をURLエンコードしない件は変更していない**（別論点。往復が壊れるため。報告済み）
- [x] `MockConnection.java:23` — 実物を読み、このクラスが何をするものかを1〜3文のクラスコメントにして `TODO` を消す → 全メソッドが `null`/`false`/`0` を返すだけの `java.sql.Connection` 実装。唯一の利用箇所 `EntityDependencyParserTest.java:54` が `getMetaData()` だけを無名サブクラスで上書きしている
- [x] `grep -rn 'TODO\|FIXME\|@Ignore' src --include=*.java` が0件であることを確認し、コミット・push する → **0件**（exit=1）。全件 `Tests run: 855, Failures: 0, Errors: 0, Skipped: 7`（基準 854 ＋ 追加1件）

**Completion criteria**:

- `src` 配下の `TODO`/`FIXME`/`@Ignore` が0件
- `MockHttpRequest.java:379` の処置が、推測ではなく再現テストの実測に基づいている

### #26: 下流から他責先として指されている3件の行番号照合（手順3・報告のみ）

**Purpose**: 下流の指示書が出典として引いている行番号が実物と合っているかを確かめる。**3件とも 2026-08-21 に「対応不要」で決着済み。**

**Prerequisites**: none

**Steps**:

- [x] XLS-40 — `TableData.java:97` `:492` `:530` がテーブル名・カラム名を大文字化しているかを実物で確認する → **`:492` `:530` は1行ずれ。正しくは `:493` `:531`**。`:97` は正しい
- [x] YML-14 — `DataFileFragment.java:105-113` の `addValue` が `names.size()` ぶんしか読まないかを実物で確認する → **一致**。`addValue` の定義は `:102`、`for (int i = 0; i < names.size(); i++)` が `:105`、その閉じ括弧が `:113`
- [x] RepositoryInitializer — `RepositoryInitializer.java` の `static {}` と `MainForRequestTesting.java:24` の `finally { revertDefaultRepository(); }` を実物で確認する → **一致**。`static {}` は `:32`（指示書に行番号の記載なし）、`revertDefaultRepository()` の呼び出しは `:24`（`finally` キーワード自体は `:23`）
- [x] ずれていたら正しい行番号を報告する → XLS-40 の2件を報告（`:492`→`:493`、`:530`→`:531`）。**`src/main` は1行も変更していない**

**Completion criteria**:

- **`src/main` を1行も変更していない**
- 3件の行番号の照合結果が報告されている（コミットはしない）

### #27: 変更差分の C0/C1 を 100% にする（手順4）

**Purpose**: PR が `origin/develop` から変更した `src/main` の行・分岐について、C0（命令網羅）と C1（分岐網羅）を 100% にする。**モジュール全体ではなく変更差分が対象**（2026-08-21 ユーザー判断）。

**Prerequisites**: #25

**Steps**:

- [x] `git diff --stat origin/develop...HEAD -- src/main` と `git diff origin/develop...HEAD -- src/main` を採り直し、どの行が差分かを報告に貼る → **8ファイル `+272 / -43`**（`#25` で `MockHttpRequest.java` が加わり、着手前の7ファイル `+265/-42` から増えた）
- [x] `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes` → `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec` で実測する（**`pom.xml`・`argLine` は変更しない**）→ 実施。`jacoco.xml` の行単位データと `git diff -U0 origin/develop...HEAD -- src/main` の変更行を突き合わせて判定した（クラス単位の `jacoco.csv` では未変更行を含んでしまうため）
- [x] 未達の行・分岐の一覧を**まず報告する**（テストを足すのはその後。何件足すかが分かってからでないとユーザーが判断できない）→ **変更行のうち未達は4行・3か所**。

  | 場所 | 未達 | 内容 |
  | --- | --- | --- |
  | `MasterDataSetUpper.java:218` | `mb=1 cb=1` | `if (dotPosition < 0)` の true 側（拡張子なしファイル）が未到達 |
  | `MasterDataSetUpper.java:220` | `mi=2` | 同上の `return false;` |
  | `TestDataParsingTemplate.java:266` | `mi=2` | `tryLoadFromCache` の既定実装 `return false;` |
  | `TestDataParsingTemplate.java:277` | `mi=1` | `storeToCache` の既定実装（空） |

  `TestDataParsingTemplate` の2件は**プロダクションコードの経路からは到達しない**。`:204` `if (!cacheEnabled() || !saveCache)` により既定実装を呼ぶ `cachedParse`（`:228-233`）へ入るのは `cacheEnabled() == true` のサブクラスだけであり、該当する3クラスはいずれも既定実装を通らない（`ListMapParser.java:58,68` と `TableDataParser.java:78,88` は両メソッドを override、`DataFileParser.java:106` は `cachedParse` 自体を override）。`:246` の Javadoc が「キャッシュを持つサブクラスは `true` を返し、`tryLoadFromCache`・`storeToCache`・`prepareResult` を実装する」と規約を定めているため、既定実装に到達する構成は規約違反にあたる。**2026-08-21 ユーザー判断: 到達不能として報告のみとし、テストも `src/main` も足さない。** 中間クラスを挟めば未達3命令は構造的に消えるが、3命令のためにクラス階層を変えない判断をした
- [x] 意味のあるテストを追加する（担保内容を javadoc に1文で書く。**その行／分岐を壊す変更で落ちることを実測**し、コマンドと結果を報告に書く）→ **1件**。`MasterDataSetUpperTest#testGetAllTableDataFromFileWithNoExtension`。担保するのは「拡張子を持たないマスタデータファイルをExcel形式と誤判定せず、ファイル全体を1リソースとして `TestDataParser` へ問い合わせること」。フィクスチャのファイル名を `xls` にしているのは、早期return の有無を判別できる唯一の入力だから（`.` が無い名前は早期return が無いとそのまま拡張子として扱われ `xls` に一致してしまう。8種の名前で実測）。

  変異2種とも kill（`mvn -o test -Dtest=MasterDataSetUpperTest#testGetAllTableDataFromFileWithNoExtension`）。

  | ゲート | 変異 | 結果 |
  | --- | --- | --- |
  | A | `:220` の `return false;` を `return true;` にする | `Tests run: 1, Failures: 0, Errors: 1` |
  | B | `:218-221` の早期return をブロックごと削除する | `Tests run: 1, Failures: 0, Errors: 1` |
- [x] 到達不能と判断した行・分岐は、テストで埋めず**理由を行番号付きで報告する**（判断はユーザー）→ `TestDataParsingTemplate.java:266`（`mi=2`）と `:277`（`mi=1`）の2件。2026-08-21 ユーザー判断で報告のみとした（上表参照）
- [x] コミット・push する

**Completion criteria**:

- 差分に含まれる行・分岐の `INSTRUCTION_MISSED` と `BRANCH_MISSED` が 0（または到達不能として報告済み）→ **達成**。再実測で変更行の未達は `TestDataParsingTemplate.java:266` `:277` の2行のみ（いずれも到達不能として報告済み）。`Tests run: 856, Failures: 0, Errors: 0, Skipped: 7`
- 追加した各テストについて「壊す変更で落ちた」確認結果がある

### #28: 全テストを流して緑を確認する（手順5）

**Purpose**: `mvn -o clean test` 全件で緑を確認し、`Tests run` の行をそのまま報告する。

**Prerequisites**: #27

**Steps**:

- [x] `mvn -o clean test` を実行する → `BUILD SUCCESS`
- [x] `Tests run: N, Failures: F, Errors: E, Skipped: S` の行をそのまま転記する（要約しない）→ `Tests run: 856, Failures: 0, Errors: 0, Skipped: 7`
- [x] 着手前の基準 `Tests run: 854, Failures: 0, Errors: 0, Skipped: 7` と突き合わせる（`Skipped: 7` は `org.junit.Assume` 由来で `@Ignore` ではない）→ 854 ＋ 追加2件（`#25` の `testContentLengthIsOctetLengthOfBody`、`#27` の `testGetAllTableDataFromFileWithNoExtension`）＝ 856。`Skipped: 7` は据え置き。**内訳は指示書の記述（すべて `org.junit.Assume` の Java バージョン判定）とは一部異なる**。`@Ignore` が0件である点は指示書どおり。

  | クラス | 件数 | 由来 |
  | --- | --- | --- |
  | `HtmlConvertTest.java:89,109` | 2 | `Assume.assumeTrue` の Java バージョン判定（1.6以下／1.8） |
  | `RequestTestingMessagingClientTest.java:438,479` | 2 | `Assume.assumeThat` の Java バージョン判定（1.6／1.7） |
  | `DbAccessTestSupportTest.java:274,308` | 2 | **`@TargetDb(include = ORACLE / POSTGRE_SQL)`**。H2 で実行しているため `DatabaseTestRunner` がスキップする |
  | `FileUtilsTest.java:457` | 1 | `assumeThat(getOsName(), containsString("windows"))`。Linux 実行のためスキップ |
- [x] コミット・push する

**Completion criteria**:

- `Failures: 0, Errors: 0` → **達成**
- `Skipped` の内訳が `@Ignore` ではないことを確認済み → **達成**（上表。`grep -rn '@Ignore' src --include=*.java` も0件）

---

# State

(written by /rn:dn, read and reset to this placeholder by /rn:up. `Status` is `paused` while a
session is suspended — the signal /rn:up and /rn:dn search for — and resets to `not suspended` here,
so only a genuinely suspended session reads `paused`.)

- **Status**: not suspended
- **Date**: -
- **Last completed**: -
- **Next**: -
- **Notes**: -

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
