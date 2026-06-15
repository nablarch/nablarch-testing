# NTF テストデータ変換ツール 再構築フェーズ（設計書 6.3 到達）

ブランチ: `add-yaml`
ドラフト PR（開発の最初に作成・常時更新）: [lovaizu/nablarch-testing#1](https://github.com/lovaizu/nablarch-testing/pull/1)

このファイルはプロジェクトの進行管理・経緯記録であり、**唯一の真実（single source of truth）**です。PR 本文は本ファイルへのリンクのみとし、内容を二重化しません（重複は形骸化・不整合の元になるため）。進捗・状況共有・相談はすべて本ファイルの更新を通じて行います。PR レビュアーは「成果物」までを参照してください。

---

## 背景・目的

Nablarch は銀行・保険・官公庁等のミッションクリティカルな大規模基幹系システムで使われるフレームワークである。NTF（Nablarch Testing Framework）はそのテスト基盤であり、NTF 自体のバグが顧客システムの品質を直接損なう。

**設計・実装・テスト・レビューのすべてに、ミッションクリティカルな基幹系システムと同等の高品質を要求する。**

- テストは「通った」だけでは不十分。境界値・異常系・仕様の端点を網羅し、意図が明確であること
- レビューは「問題なさそう」ではなく、仕様の全 ID に対して根拠を持って充足を確認すること
- 「動く」と「正しい」は別物。正しさを根拠で説明できない実装・テストは完了とみなさない

---

# Goal

このブランチで、Excel↔YAML テストデータ変換ツールを **変換ツール設計書（`docs/pr75/docs/testdata-converter-design.md`）の通りに作り直し**、設計書の品質担保 **6.3（既存の Excel 読み込みテストを YAML に変換しても全件 PASS ＝ 振る舞い不変）まで到達**させる。

背景として、現状は NTF 本体と変換ツールが読み込みロジックを**別々に実装**しており、6.3 のテストで両者の解釈不整合が露見した。これを解消するため、変換ツールは**本体の構造解析を再利用する設計**へ全面的に作り直す。具体的には：

- **① NTF 本体の YAML 対応**を設計書通りに改修する（YAML 読み込みを「構造マッピング層／値加工層」に明示分離し、Excel 再利用のための取り出し面を最小化する）
- **② 既存の独自実装の変換ツール**は main・テストとも全削除し、設計書通りにゼロから再構築する（本体パッケージ相乗りの `TestDataParserAdapter` 経由で Excel を、構造マッピング層経由で YAML を再利用）
- **③ 既存 Excel テストを YAML へ変換して実行するテスト基盤**を、再構築後の変換ツールへ繋ぎ直して全件 PASS させる

**変換ツール設計書を唯一の正**とし、旧 steering のタスク・設計は破棄する。全工程 TDD で進める。

## Verification（2 軸）

**目標整合（設計書通りに再利用しているか）**

- `XlsFormatReader` が独自 POI パース（`parseBlocks`／`parseRecordLayouts`／`isDataRow`／`trimQuotation` 等）を持たず、本体 Parser を `TestDataParserAdapter`（本体パッケージ相乗り 1 枚）経由で再利用していること
- `YamlFormatReader` が SnakeYAML 直叩きの構造解析を持たず、本体の**構造マッピング層**を直接呼ぶこと
- 本体への変更が「設計書の再利用に必要な最小集合」に収束していること（過剰公開は是正）

**品質（設計書 4 章 品質担保 Level1〜3）**

- **L1**: 変換ツール各クラスの単体テスト C0/C1 100%（番人コード除く）、全データ種別（FIXED／VARIABLE／TABLE／LIST_MAP／MESSAGE 系）網羅、IN 値が記法のまま未加工であることを実証
- **L2**: 往復変換（Excel→中間→Excel、YAML→中間→YAML）で NTF 仕様上の意味が不変
- **L3（＝6.3）**: nablarch-testing 既存テストを Excel データの一時 YAML 変換で実行し**全件 PASS**（Excel ベースラインと同一の `Tests run`／`Failures:0 Errors:0`、既知 Skip 除く）

# Assumptions

**事実（調査で確認済み）**

- 現ブランチは `add-yaml`。`pom.xml` に未コミットのローカル変更（parent `6-NEXT-SNAPSHOT`→`6u3`）あり＝ローカル依存解決用、目標対象外
- ②変換ツールは本体ロジックを**一切再利用せず 100% 独自実装**（本体約 2,800 行＋テスト約 6,600 行）。`XlsFormatReader` は POI 直叩き（約 500 行）、`YamlFormatReader` は SnakeYAML 直叩き（約 301 行）。`TestDataParserAdapter` は**存在しない**
- ①本体 YAML は builder 内で構造写しと値加工（`interpret`）が混在し、加工除外が「空 interpreters」の暗黙切替に依存（判断 B が廃すべきとする方式）。`getResult` protected 化・器 getter 追加（旧 Step1-2 相当）は merge 済み
- ③は `YamlModeTestBase.prepareYamlData()`（87 行目）が `TestDataConverter.convert()` を呼ぶ 1 点と `TestDataConverterTest` のみが変換ツールに依存。`*YamlTest` 18 クラスがそれを利用

**前提**

- 「6.3 まで」＝設計書 4 章 品質担保 **Level1〜3** を含む（L2 往復は全 4 方向の Reader/Writer を要する）。**Level4（サンプルアプリ）はリポジトリ分割後でスコープ外**
- 設計書が「本体無変更」と記す器 getter は、実際にはこのブランチで追加されたもの。設計書を正としつつ「再利用に必要な最小 getter」は本体変更として許容する（#1 で是正・収束）

# Rules

- **1 task = 1 commit**、コミット後**プッシュ必須**
- **全工程 TDD**（RED→GREEN）で進める
- 各タスクは担当者セルフチェック（完了条件を 1 件ずつ OK/NG＋根拠で `docs/pr75/checks/{タスクID}.md` に記録）
- **レビューはフェーズ単位**（下記「レビュープロセス」）。サブエージェントで 4 観点 → 全パス後にユーザーレビュー → OK で次フェーズへ
- 指摘は原則全件対応。対応しない場合はユーザー確認
- 環境変更（ライブラリ追加・ツールインストール等）は事前確認
- 設計書 `docs/pr75/docs/testdata-converter-design.md` が唯一の正。読み込み機構の解説は `docs/pr75/docs/ntf-testdata-loading.md`

## レビュープロセス（フェーズ単位）

各タスクは TDD ＋ 担当者セルフチェックで進める。**フェーズ完了時**に以下の 4 観点をサブエージェント（Agent ツール）で独立実施し、本質的な指摘がなくなるまで改善→再レビューを繰り返す。全観点パス後にユーザーレビューを受け、OK で次フェーズへ進む。

| 観点（サブエージェント） | 見るもの |
|---|---|
| **アーキテクト** | 設計書通りか（再利用構造・判断 A/B・層分離・中間モデル・依存方向 変換ツール→本体の一方向） |
| **ソフトウェアエンジニア** | ベストプラクティス（責務分離・命名・例外/null・重複・深いネスト・保守性・既存 API 互換） |
| **QA エンジニア** | 意味のある仕様テストの網羅（「通った」でなく仕様の意図を検証）・エッジケース（境界値・異常系・空入力・最大値・型変換端点） |
| **Java 言語エキスパート** | 言語イディオム・既存コードスタイル統一（Javadoc・`@Override`・sealed・型引数・アクセス修飾子）・テストの GWT（Given/When/Then）形式 |

サブエージェントへの指示には、対象ファイルのパス一覧・役割・上記観点の全文・「本質的な指摘がなくなるまで繰り返す」旨を含める。チェック結果は `docs/pr75/checks/{フェーズID}.md` に出力する。

# Tasks

## Phase 1 — 本体再利用基盤の整備（① 改修・TDD）

### #1: 本体再利用面の設計書照合・是正

**Purpose**: 設計書 判断 A・共通が要求する「本体の構造解析を再利用するための取り出し面」を設計書と照合し、過不足を最小集合へ収束させる。

**Prerequisites**: なし

**Steps**:

- [x] 設計書 判断 A（アダプタが空 interpreters で parse→getResult／相乗りはアダプタ 1 枚に閉じる）と「共通：器の中身を読む手段」表を精読
- [x] `git diff main..HEAD` で本体 reader/file/messaging の現変更（`getResult` 可視性・`DataFile`/`DataFileFragment`/`MessagePool` の getter・各 Parser コンストラクタ可視性）を棚卸し
- [x] アダプタを `nablarch.test.core.reader` 同一パッケージに置く前提で、真に必要な可視性・getter を確定。過剰公開（例: `getResult` が package-private で足りるなら戻す）は是正
- [x] reader パッケージの既存テスト全 GREEN（振る舞い不変）※残存 4 RED は YAML 値加工層の既存バグ（本タスク起因でなく #2 スコープ）— `docs/pr75/checks/P1-1.md` 参照
- [x] セルフチェック（`docs/pr75/checks/P1-1.md`）

**Completion criteria**:
- 本体 reader/file/messaging への変更が「再利用に必要な最小集合」に一致し、各変更の必要理由を 1 件ずつ列挙できる
- `git diff main..HEAD` の本体差分が設計書根拠つきで説明可能
- reader 既存テストが全 GREEN

### #2: 本体 YAML 読み込みの 2 層分離（判断 B）

**Purpose**: YAML 読み込みを「構造マッピング層（値未加工で本体器を返す）」と「値加工層（interpret・補完・マージ）」に明示分離し、変換ツールが構造マッピング層だけを呼べる public API を本体に設ける。

**Prerequisites**: なし

**Steps**（option C・D-F 参照）:

- [x] 現 builder（`YamlFileBuilder`/`YamlTableDataBuilder`/`YamlMessageBuilder`）の構造写し処理と値加工（interpret・補完・マージ・`-`長注入）箇所を特定
- [x] 生の構造レコード `Raw*`（`reader.yaml.model`：`RawTableData`/`RawListMap`/`RawDataFile`/`RawRecordLayout`/`RawFieldDef`/`RawMessage`）を新設（値未加工・マーカー保持・YAML 順・長さ省略 null・FW_HEADER 保持・大文字化なし）
- [x] 構造マッピング層 `Yaml{Table,File,Message}StructureMapper`（Map→`Raw*`）を新設（変換ツールも呼ぶ public API）
- [x] 値加工＋組み立て層 `YamlValueProcessor`（`Raw*`→本体器。interpret・BinaryFileInterpreter・`-`長注入・fillDefaultValues・マーカー除外・グループ絞り込み）を新設
- [x] `YamlTestDataParser` を「structureMapper → valueProcessor」明示 2 呼び出しへ再配線。「空 interpreters 素通し」暗黙切替を廃止。旧 builder 3 本を削除
- [x] 既存 Yaml 系本体テスト全 GREEN（振る舞い不変）。builder テスト 73 件を valueProcessor+mapper 経由へ移送＋構造層テスト新設。レビュー対応で File/Message 構造層直接テストを追加し **reader-YAML 171 件全 GREEN**（Parser43/Equivalence16/TableStruct5/FileStruct6/MessageStruct6/FileVP14/MessageVP29/TableVP30/Loader11/Section10/Schema1）
- [x] 要確認 2 件: list_maps は valueProcessor 出力を TreeMap 維持（本体不変）・RawListMap は YAML 順保持／fw_header は非 interpret（objectToString のみ）を維持。いずれも GREEN で確認
- [x] 全モジュールのフルテスト実行（reader 以外への波及がないこと）— ベースライン(`d3cd139`)比較で失敗集合完全一致（最終 1490 件・43F/44E・既存事象のみ）＝波及ゼロを実証
- [x] セルフチェック（`docs/pr75/checks/P1-2.md`）— 4 観点レビュー（イテレーション 2）全 PASS まで反映

**Completion criteria**:
- 構造マッピング層が `${...}`・`null`・`""`・マーカー・長さ省略・大文字小文字を記法のまま保持した `Raw*` を返すことをテストで実証
- `YamlTestDataParser` が両層経由で従来と同一結果（既存テスト全 GREEN）
- 変換ツールから呼べる public 構造マッピング API（`Yaml*StructureMapper`＋`Raw*`）が存在する

**Phase 1 完了ゲート**: ✅ **通過**（4 観点サブエージェントレビュー＝アーキ/SWE/QA/Java をイテレーション 2 まで実施し全 PASS・`docs/pr75/checks/P1-2.md` 記録。ユーザーレビューは Operating mode により不要）。残 nice-to-have: `isMarker` null 列分岐の明示カバレッジ（値加工層テスト・#7 以降で必要に応じ）。

## Phase 2 — 変換ツールの全削除（②）

### #3: 変換ツール一括削除＋テスト基盤一時無効化

**Purpose**: 独自再実装の変換ツールを main・test とも一括削除し、設計書通りの再構築に向けて白紙化する。削除で壊れる③テスト基盤は一時無効化してビルドを維持する。

**Prerequisites**: なし

**Steps**:

- [x] `src/main/java/nablarch/test/tool/converter/` 配下を全削除（24 ファイル）
- [x] `src/test/java/nablarch/test/tool/converter/` 配下を全削除（9 ファイル・全 .java）
- [x] `YamlModeTestBase` と 18 `*YamlTest` を一時無効化（base は `UnsupportedOperationException` スタブ化＝ユーティリティゆえ @Ignore 不可、18 テストは クラスレベル `@Ignore`）。一覧と復帰方針を `docs/pr75/checks/P2-3.md` に記録
- [x] `mvn -o test-compile` 成功・`mvn -o test` で Failures=0（残 Error は既存 Mockito 環境起因 2 クラスのみ・#3 非起因）＝Excel 経路 GREEN
- [x] セルフチェック（`docs/pr75/checks/P2-3.md`）

**Completion criteria**:
- `tool/converter/` 配下が main/test とも存在しない
- プロジェクトがコンパイル成功
- Excel 経路の既存テストが従来 GREEN
- 無効化した 18 テストの一覧と復帰方針が記録済み

**Phase 2 完了ゲート**: ✅ **通過**（アーキテクト PASS・SWE PASS〔イテレーション 2〕。`docs/pr75/checks/P2-3.md` 記録。ユーザーレビューは Operating mode によりスキップ）。残存: `pom.xml` exec mainClass が削除クラスを指す（非ビルド経路・#10 で CLI/Mojo 再構築時に是正）。

## Phase 3 — 中間モデル＋IN（読み込み）の再構築（TDD）

### #4: 中間モデルの再構築

**Purpose**: 設計書 3 章の中間モデルを新規作成する。

**Prerequisites**: #3

**Steps**:

- [x] `TestDataContainer` / `TestDataSection` / `TestDataBlock`(sealed; permits `ColumnRowDataBlock`/`FileDataBlock`/`MessageDataBlock`) / `ColumnRowDataBlock`(sealed; permits `TableDataBlock`/`ListMapBlock`) / `TableDataBlock` / `ListMapBlock` / `FileDataBlock` / `MessageDataBlock` / `RecordLayout` / `FieldDef` を作成（`nablarch.test.tool.converter.model`・本体 `DataType` 再利用・`KNOWN_DIRECTIVE_NAMES` 非搭載）
- [x] 各モデルの単体テスト（TDD・RED→GREEN）。8 クラス 27 件 GREEN・model C0/C1 100%（JaCoCo 実測）
- [x] セルフチェック（`docs/pr75/checks/P3-4.md` 記載済。4 観点レビュー＝QA イテレ2／Java／SWE を本セッションで再実施し全 **PASS**。QA イテレ1 FAIL（全 13 DataType 網羅漏れ＋エッジ不足）はテスト 20→27 件で解消確認）

**Completion criteria**:
- 設計書 3 章のクラス図と継承/sealed 関係が一致
- 単体テスト全 GREEN・C0/C1 100%（番人除く）

### #5: TestDataParserAdapter 新設（判断 A）

**Purpose**: `nablarch.test.core.reader` に薄いアダプタを 1 枚新設し、本体 Parser を空 interpreters＋スタブ `DbInfo` で配線して parse→getResult で生の器を取り出す。MESSAGE 本文は `MessageParser.getDelegate()` の `FixedLengthFile` を再利用。

**Prerequisites**: #1, #4

**Steps**:

- [x] `readFiles`/`readTables`/`readListMap`/`readMessage` を実装（本体器を返す。`TestDataParserAdapter` 1 枚・本体パッケージ相乗り）
- [x] スタブ `DbInfo`（読み込み経路で唯一呼ばれる `getColumnType`→VARCHAR のみ実値・他 9 メソッドは番人＝`UnsupportedOperationException`）を構成
- [x] 空 interpreters で `${...}`/`null`/`""` が未加工・補完/マージが起きないことをテストで実証（後処理なしは番人発火＋カラム数非拡張で識別的に証明）
- [x] セルフチェック（`docs/pr75/checks/P3-5.md`）— 3 観点レビュー（QA イテレ2／Java／SWE）全 **PASS**。adapter C0/C1 100%・テスト 15 件 GREEN・reader 回帰なし（33 件 GREEN）

**Completion criteria**:
- 各メソッドが本体器（`DataFile`/`TableData`/`MessagePool`/`List<Map>`）を返す
- IN 値が記法のまま（未加工）であることをテストで実証
- 相乗りがこのアダプタ 1 クラスに限局

### #6: XlsFormatReader 再構築（Excel IN）

**Purpose**: アダプタ経由で本体器を受け取り中間モデルへ写す。独自 POI パースは持たない。

**Prerequisites**: #4, #5

**Steps**:

- [x] アダプタ呼び出し→中間モデル組み立てを実装（R1+R2 `6c5ef7a` で全種別＋原文復元・本体無変更／R3 `ed313b7` で送信系電文4種編入）
- [x] 全データ種別の単体テスト（TDD）— XlsFormatReaderTest 18・TestCoreReaderAdapterTest 22・TestCoreFileAdapterTest 7＝47 件 GREEN
- [x] セルフチェック（`docs/pr75/checks/P3-6.md`）— 3 観点レビュー（QA/Java/SWE）全 PASS まで反映。jacoco 番人除く C0/C1 100%・全モジュール回帰 1111 件 新規失敗ゼロ

**Completion criteria**:
- 全種別を無損失で中間モデル化／IN 値が記法のまま ✅（送信系4種含む全5群。先送りなし）
- 独自の構造解析（`parseBlocks`/`isDataRow`/`trimQuotation`/POI 直叩きの構造判定）を含まない（レビュー確認）✅（本体 `getDataType`/`getTypeValue`/`TestDataParsingTemplate` 再利用）
- 単体テスト GREEN・C0/C1 100%（番人除く）✅

### #7: YamlFormatReader 再構築（YAML IN・判断 B / D-H）

**Purpose**: YAML IN を Excel(#6) と**対称**の方式で再構築する＝**本体器**で構造を得て、**`YamlLoader.load` が返す順序保持 Map で原文を復元**し中間モデルへ写す。SnakeYAML 直叩きの独自構造解析は持たない。[[D-H]] に従い #2 の `Raw*`/StructureMapper/ValueProcessor は破棄する。

**Prerequisites**: #2, #4

**Steps**（D-H「次セッションの段取り」準拠）:

- [x] 本体 YAML 読み込みを本体器生成へ作り直す（`YamlTableDataBuilder`/`YamlFileBuilder`/`YamlMessageBuilder` が YAML Map を走査し `TableData`/`DataFile`/`MessagePool` を返す。`YamlTestDataParser` を再配線）。`Raw*`(6本)・`Yaml{Table,File,Message}StructureMapper`・`YamlValueProcessor` を削除、構造マッパテスト3本も削除、値加工テスト73件をビルダテストへ移送。reader-YAML **154件 GREEN**（Parser43/Equivalence16/TableBuilder30/FileBuilder14/MessageBuilder29/Loader11/Section10/Schema1）＝振る舞い不変・本体器ゼロ差分
- [x] 変換ツール側 YAML アダプタ新設（`nablarch.test.core.reader.YamlTestCoreAdapter`・`TestCoreReaderAdapter` と対称）。空インタープリタ・補完なしで本体ビルダを配線し生器（`TableData`/`DataFile`/`MessageContent`/送信系 `FixedLengthFile`）を返す＋`loadRawMap`=`YamlLoader.load` 透過 seam。**バイナリ注入を呼び出し側へ移管**＝新設 `InterpreterResolver`（`withBinaryFile`/`raw`）をビルダ ctor に注入（旧 `YamlSection.addBinaryFileInterpreter` の内蔵を廃止）→ `raw` で `${binaryFile:}` も未解決のまま取得。`StubDbInfo` を両アダプタ共有の package-private 型へ抽出。`YamlMessageBuilder` に `buildMessageContent`/`buildSendSyncBodies`/`MessageContent` 追加（`MessagePool.getSource` 不可視回避＝本文 `FixedLengthFile` 直接取得）。adapter 15件 GREEN・reader-YAML/Excel アダプタ回帰 209件 0F/0E（Equivalence16+Parser43 緑＝振る舞い不変）。stale fixture/Javadoc 是正
- [x] `tool/converter/yaml/YamlFormatReader` を Excel と対称に新設（本体器で構造・`loadRawMap` Map で原文〔カラム名・YAML 列順・値・型表記・長さ省略・fw_header〕復元 → 中間モデル）。`read` はトップレベル Map を記述順走査→既知セクションをハンドラへ（未知キー無視）。TABLE=器のみ・LIST_MAP=器値＋Map 列順（マーカー除外）・FILE=器と Map エントリを zip・MESSAGE=fw_header 原文＋FW_HEADER レコード除外・送信系=生値グループで zip＋整形 groupId 格納＋"no" 保持。fail-fast 2 種（器↔エントリ数／器断片↔原文レコード数）。本体ゼロ差分
- [x] 全データ種別の単体テスト（TDD・RED→GREEN）。`YamlFormatReaderTest` 15 件 GREEN（in-memory `loadRawMap` 差し替えで実ビルダ統合）。網羅: TABLE(setup/group×2/complete)・LIST_MAP(列順/マーカー/null)・FILE(fixed 型長省略/variable)・MESSAGE(fw_header 原文/FW_HEADER 除外/空本文/null skip)・送信系(同一グループ複数 id/4 種/"no")・未加工(${}/null/"")・混在＋未知キー・コンテナ名・fail-fast 2 種。reader/adapter/converter 回帰 136 件 0F/0E
- [x] 設計書 §判断 B 据え置き（決定は維持）・§共通 原文復元を「Excel=生行 / YAML=YamlLoader Map」へ一般化是正。あわせて D-H 反映で stale 化していた箇所を是正：§判断 B 本文（構造マッピング層/値加工層 2 層分割→本体器を空インタープリタで取得・Raw* 却下は経緯として保持）・IN 概要・クラス図（`YamlTestCoreAdapter` 追加・`YamlReader`→`YamlBuilders`）・特殊記法節。設計書＝唯一の正をコード実態へ追随（D-A・教訓）
- [x] セルフチェック（`docs/pr75/checks/P3-7.md`）＋ 4 観点レビュー（アーキ/QA/Java/SWE＝Phase 3 完了ゲート）— 全 PASS（must-fix なし）。Step5 で QA 提案 2 本＋カバレッジ穴 2 本＝計 +4 テスト追加し YamlFormatReader を instr/branch 99/99%（残 2 は番人＝本番 ctor・null ディレクティブ値）へ。全モジュール回帰 1128 件 0F/4E（P3-6 ベースライン一致＝新規失敗ゼロ）。本体ゼロ差分。

**Completion criteria**:
- 全種別を無損失で中間モデル化／IN 値が記法のまま
- 独自の YAML 構造解析を含まない・`Raw*` 系が消えている（レビュー確認）
- 本体 YAML テスト全 GREEN（振る舞い不変）・変換ツール単体テスト GREEN・C0/C1 100%（番人除く）

**Phase 3 完了ゲート**: ✅ **通過**（#4〜#7 完了。#7 Step5 で 4 観点サブエージェントレビュー＝アーキ/QA/Java/SWE を最終成果物に実施し全 PASS・`docs/pr75/checks/P3-7.md` 記録。ユーザーレビューは Operating mode によりスキップ）。

## Phase 4 — OUT（書き出し）＋入口＋検証の再構築（TDD）

### #8: YamlFormatWriter 再構築（YAML OUT）

**Purpose**: 中間モデル→YAML を記法どおりに書き出す。#7 IN リーダと記法対称（L2 往復(#12)の片側）。

**Prerequisites**: #4

**Steps**（TDD・RED→GREEN）:

- [x] `TestDataFormatWriter` インタフェース新設（`write(container, basePath)`。Reader と対称・本体非依存）
- [x] `YamlFormatWriter implements TestDataFormatWriter` 新設。`serialize(TestDataSection)`＝純粋な中間モデル→YAML テキスト（テスト容易化）、`write`＝各セクションを `<basePath>/<section名>.yaml` へ出力。設計書 OUT 方針＝**全値ダブルクォート**（null はアンクォート `null`・`""`/`${...}` は記法保持）・キーは原則無クォート（YAML 特殊文字含む場合のみクォート）。`DataType`→セクションキー対応は writer 内 `sectionKey`（全13種・`dataTypeToSectionKey` は messaging 専用ゆえ流用不可）
- [x] 全種別を #7 と対称の記法で出力：TABLE3（`table:`＋`rows:` map・group_id 整形 `[x]`→生値）・LIST_MAP（`id:`＋マーカー除外列）・FILE（`path:`/`type:`/`directives:`/`records:`〔`record_type` 省略時 null は出力なし・`fields:` flow `{name,type,length}`・length/type null は省略・`rows:` flow list〕）・MESSAGE（`id:`＋`fw_header:`＋FW_HEADER レコード非出力）・送信系4種（`group_id:` 必須＋`id:`＋fw_header なし＋"no" 保持）。セクションは初出順でグルーピング
- [x] 全データ種別の単体テスト（TDD）。`serialize` 直アサート＋クォート/エスケープ（`"`/`\`/改行・制御文字・null・空文字）＋ `write` の I/O（`TemporaryFolder`）＋往復（モデル→write→実 `YamlFormatReader` で読み戻し同値・TABLE/固定長FILE/MESSAGE/送信系の4経路）。`YamlFormatWriterTest` 30件 GREEN・JaCoCo instr 99%/branch 96%（未到達は番人4＝sealed default・no-parent・制御文字キー・不均衡角括弧）
- [x] セルフチェック（`docs/pr75/checks/P4-8.md`）＋ QA/Java/SWE 3観点サブエージェントレビュー（Phase 4 コードタスク手順）＝**全 PASS**（must-fix なし）。指摘は原則全件対応：QA→往復テスト2件追加（前後空白・null/"null"/数値の往復区別を実 Reader で実証）／Java→`@After clearLoaderCache()`＋FQCN import 化／SWE→`Seq.childLevel()` 抽出で `level+2` 散在集約。`YamlFormatWriterTest` 32件 GREEN・JaCoCo instr 99%/branch 96%（番人4枝のみ未到達）・本体ゼロ差分

### #9: XlsFormatWriter＋ExcelFormatConfig 再構築（Excel OUT）✅ 完了

**Purpose**: 中間モデル→Excel を整形設定（`ExcelFormatConfig`、デフォルト備え上書き可）に従い書き出す。

**Prerequisites**: #4

**Completion criteria**: 全種別を Excel 出力／整形はデフォルトで見やすい既定値／単体テスト GREEN

- [x] 全種別 Excel 出力（TABLE3/LIST_MAP/FILE(FIXED,VARIABLE)/MESSAGE/送信系4種）＝build 検証＋実 `XlsFormatReader` 往復 8 経路
- [x] 整形デフォルト＝`ExcelFormatConfig.defaults()`（ヘッダ淡青/マーカー淡橙/列幅自動/外枠細線/ブロック間1空行・`with*` 上書き可）
- [x] 単体テスト 33 件 GREEN・JaCoCo **C0/C1 100%（番人除く＝parent==null／sealed throw／isMarkerColumn null の 3 枝のみ未到達）**。ExcelFormatConfig 100%
- [x] セルフチェック（`docs/pr75/checks/P4-9.md`）＋ 3 観点レビュー（QA イテレ2／Java／SWE）全 **PASS**。指摘対応: ① recordType=null×複数レコードの版面破壊→`appendRecords` 番人で早期失敗＋異常系/正常系テスト固定 ② null セル非可逆往復を実 Reader で固定 ③ 未使用 import 削除。全モジュール回帰 1192 件 0F/4E（P3-7 ベースライン一致＝新規失敗ゼロ）・本体ゼロ差分

### #10: 変換ツール入口・周辺の再構築 ✅ 完了

**Purpose**: `convert(from,to,input,output)` 入口、ディレクトリ走査・include/exclude・上書き可否を再構築（`TestDataConverter`/`ConversionRequest`/`DataFormat`/`ConverterFileFilter`/`ConverterPathResolver`/`ConverterException`）。

**Prerequisites**: #6, #7, #8, #9

- [x] main 6 クラス新設（`DataFormat`／`ConverterException`〔非検査〕／`ConversionRequest`+Builder〔source/target/in/out/overwrite/include/exclude・同形式許容〕／`ConverterPathResolver`〔4 方向出力 basePath・純関数〕／`ConverterFileFilter`〔`findXlsFiles`/`findYamlDirs`・glob include/exclude・決定性ソート〕／`TestDataConverter`〔`convert(ConversionRequest)`＋`convert(from,to,input,output)` ファサード〕）。入口が粒度差（Excel ブック=複数シート／YAML ディレクトリ=複数ファイル）を 1 コンテナへ集約。シート列挙は本体 `PoiXlsReader.getSheetNames`（順不同）→ 辞書順ソート。本体ゼロ差分
- [x] 単体テスト 38 件 GREEN（DataFormat5/ConverterException2/ConversionRequest6/ConverterPathResolver6/ConverterFileFilter8/TestDataConverter11）。4 方向 E2E は実 Reader/Writer 往復で中間モデル一致を実証
- [x] JaCoCo C0/C1 **100%（番人除く＝private ctor の AssertionError・IOException catch 再ラップ・到達不能な `parent==null`/`dot<0` 防御枝のみ未到達）**
- [x] 全モジュール回帰 `mvn -o test` = 1231 件 **0F/4E**（4E は既存 Mockito 環境起因 `MockHttpRequestTest`/`MockServletExecutionContextTest`＝PR75 非起因・ベースライン一致／新規失敗ゼロ）。18 `*YamlTest` は @Ignore で Skip
- [x] セルフチェック（`docs/pr75/checks/P4-10.md`）＋ 3 観点レビュー（QA/Java/SWE）全 **PASS（must-fix なし）**。SWE 指摘①`stripExtension` 重複を `ConverterPathResolver` へ集約済み。残 nice-to-have（QA 空文字往復・YAML 複数ファイル集約 E2E・include E2E／Java Predicate import・PathMatcher 事前コンパイル・glob 区切り Javadoc／SWE 拡張子定数集約）は次セッション繰延・must-fix ではない

**スコープ（ユーザー決定 2026-06-15・[[D-I]]）**: **Maven プラグイン（`ConverterMojo`）はリポジトリ分割後に実施＝#10 スコープ外**。今は **6.3 のテスト（#13 `YamlModeTestBase`）からテストコードで `convert(ConversionRequest)` を呼べれば十分**。CLI（`main`/`run`）も 6.3 に不要なら最小限／後回しでよい。pom への `maven-plugin-api` 依存追加（環境変更）は行わない。`exec-maven-plugin` mainClass 是正もリポジトリ分割後の CLI/Mojo 整備時に回す。

**Completion criteria**: 4 方向変換が入口（`TestDataConverter.convert`）から実行可能／テストコードから呼べる／単体テスト GREEN

### #11: YamlTestDataValidator 再構築（検証モード）

**Purpose**: リンタ（列数一致・構造境界・スキーマ適合＋V-FNAME/V-DKEY/V-MSGROW 等）を再構築。

**Prerequisites**: #4, #7

**Completion criteria**: 各検証ルールをテストで実証／`KNOWN_DIRECTIVE_NAMES` が本体ディレクティブと一致する整合テストを含む

**Phase 4 完了ゲート**: 4 観点レビュー → ユーザーレビュー OK

## Phase 5 — 往復変換の確認（品質 Level2）

### #12: 往復変換の可逆性確認

**Purpose**: Excel→中間→Excel、YAML→中間→YAML で NTF 仕様上の意味が不変であることを検証する。

**Prerequisites**: #6, #7, #8, #9, #10

**Completion criteria**: 全データ種別で往復が意味不変／色・書式・コメント等は対象外と明記

**Phase 5 完了ゲート**: 4 観点レビュー → ユーザーレビュー OK

## Phase 6 — 6.3 達成（品質 Level3）

### #13: 6.3 テスト基盤の再接続

**Purpose**: `YamlModeTestBase` を再構築後の `TestDataConverter.convert` API へ再接続し、18 `*YamlTest` を再有効化する。

**Prerequisites**: #10

**Completion criteria**: `YamlModeTestBase` が新 API で Excel→一時 YAML 変換／18 テスト再有効化

### #14: 6.3 全件 PASS 確認

**Purpose**: nablarch-testing 既存テストを Excel データの一時 YAML 変換で実行し全件 PASS（振る舞い不変）を確認する。

**Prerequisites**: #2, #13, 全 Reader/Writer

**Completion criteria**: 全件 GREEN（Excel ベースラインと同一の `Tests run`／`Failures:0 Errors:0`、既知 Skip 除く）／ベースライン数値を記録

**Phase 6 完了ゲート**: 4 観点レビュー → ユーザーレビュー OK（＝ゴール達成）

# Decisions

## D-A: 変換ツール設計書を唯一の正とする
- **Conclusion**: 旧 steering のタスク・設計（実装ステップ計画・C-1/V-1/T7/T1〜T6 等）は破棄し steering から削除。`docs/pr75/docs/testdata-converter-design.md` を唯一の正とする
- **Rationale**: 旧プランが残ると混乱の元になるとユーザー判断
- **Evidence**: 旧プランの Step4（YAML を Excel の④へ合流）は設計書 判断 B と非整合

## D-B: 変換ツールは全削除→ゼロから再構築
- **Conclusion**: `tool/converter/` を main＋テスト一括全削除し、設計書通りに再構築する。部分流用・切り分けはしない
- **Rationale**: 既存は本体を一切再利用しない 100% 独自実装で、これが 6.3 の不整合の原因。部分流用は混乱を招く
- **Evidence**: 調査で `XlsFormatReader`=POI 直叩き、`YamlFormatReader`=SnakeYAML 直叩き、`TestDataParserAdapter` 不在を確認

## D-C: YAML 経路は判断 B（構造マッピング層分離）に従う
- **Conclusion**: 本体 YAML 読み込みを「構造マッピング層／値加工層」に明示分離し、変換ツールは構造マッピング層を直接呼ぶ。旧「④へ合流」案は不採用
- **Rationale**: 設計書 判断 B が唯一の正

## D-D: 「6.3 まで」＝品質担保 Level1〜3
- **Conclusion**: Level1（単体）・Level2（往復）・Level3（6.3 全件 PASS）を含む。Level4（サンプルアプリ）はリポジトリ分割後でスコープ外
- **Rationale**: L2 往復は全 4 方向の Reader/Writer を要するため再構築範囲に含む

## D-E: steering は docs/pr75/steering.md を継続更新
- **Conclusion**: 本ファイルを継続更新。旧プラン部は削除して新プランへ置換
- **Rationale**: checks/・決定事項・PR 参照が docs/pr75 に集約され単一の真実として扱える

## D-G: 判断 A を「器のパッケージごとに同一パッケージ相乗りアダプタ 2 枚」へ確定（本体無変更・ユーザー決定 (a)・2026-06-14）
- **Conclusion**: 本体（`DataFile`/`DataFileFragment`/`MessagePool`）は**無変更**へ収束。Excel 経路の可視性の壁は**器のパッケージごとに薄い抽出アダプタ 1 枚ずつ**で越える。
  - `TestCoreReaderAdapter`（`nablarch.test.core.reader` 相乗り）＝旧 `TestDataParserAdapter` を改名。Parser を空 `interpreters` で `parse → getResult` し生の器を取り出す。MESSAGE 本文は `MessageParser.getDelegate()` の `FixedLengthFile`、FW ヘッダは同一パッケージの `MessageParser.getFwHeader()`。
  - `TestCoreFileAdapter`（`nablarch.test.core.file` 相乗り・**新設**）＝`DataFileFragment` の `names`/`types`/`lengths`/`values`・長さ省略判定を同一パッケージから読んで plain で返す。
- **本体撤回（確定）**: ① `DataFile.java` の `getAllFragments`/`getDirectives`（+18）と `DataFileFragment.java` の `getRecordType`/`getNames`/`getTypes`/`getLengths`/`getValues`＋`@Published`（+47）を**撤回**（`TestCoreFileAdapter` が同一パッケージで読むため不要）。② `MessagePool.getFwHeader` の public 化（+3/-1）を **package-private へ撤回**（本番呼び出し元なし・アダプタは `MessageParser.getFwHeader` を使用・テストは同一パッケージ＝裏取り済）。
- **QuotationTrimmer は据え置き（確定）**: 設計書 §制約 L56「明確なバグ修正は許容」＋§特殊記法（Excel はクォート対書きでクラッシュ不発生／YAML は QuotationTrimmer 不適用）により**現状のまま**。バグでなく実害ゼロ。
- **(b) 却下理由（ユーザー）**: 行種別判定・レコード区切りが `DataFileParser` の状態機械に埋め込まれ切り出せず、(b)（生行のみで構造解析）は構造解析の二重実装になる。よって (a)。
- **正となる文書**: 設計書 `testdata-converter-design.md` 2 章 判断 A のアダプタ表・§共通「器の中身を読む手段」・3 章 IN 図（ユーザーが反映済）。
- **D-F（option C/Raw\*）への影響**: D-F は **YAML 経路の精緻化**として有効。判断 A の Excel 経路には Raw\* を使わない（本 D-G）。YAML 側 Raw\* が新設計書 §判断 B（構造マッピング層→本体器）とどう整合するかは **#7 で要アセスメント**（下記 Recovery Plan R4）。
- **未解決の実装メモ（#5/#6 で解消）**: `DataFileFragment.isOndemandCalcFieldSize`／`isOndemandCalcFieldSizeList` は **private**＝同一パッケージでも不可視。長さ省略判定は (i) `TestCoreFileAdapter` が読むため package-private 化（軽微・L56 リファクタだが「可視性拡大は不要」と衝突）か、(ii) `XlsFormatReader` が生行の長さ行セル `==-` で判定（本体完全無変更）か、実装時に確定。設計書 §共通の文言は「長さ省略判定を読む」＝(i) 寄り。released 本体に触れる場合は Operating mode に従いユーザー相談。

## D-F: #2 本体 YAML 2 層分離 — 共有するのは「本体器」でなく「構造ウォーク」（option C・あるべき姿）【⚠ SUPERSEDED by D-H（2026-06-14）— Raw* は破棄。以下は経緯参考】
- **Conclusion**: 新規追加の未リリースコードゆえ「安全（既存挙動維持）」は論点でなく**負債ゼロ**を優先（memory `new-code-prefer-ideal-design`）。当初案（構造マッピング層が本体器を返す＝判断 B 文言どおり）はエキスパート照合の結果、YAML 経路では非可逆で誤りと判明したため **option C** を採用。実装方針：
  1. **構造マッピング層**＝`Yaml*StructureMapper`（`YamlTableStructureMapper`/`YamlFileStructureMapper`/`YamlMessageStructureMapper`）。YAML Map → **生の構造レコード `Raw*`**（`nablarch.test.core.reader.yaml.model`：`RawTableData`/`RawListMap`/`RawDataFile`/`RawRecordLayout`/`RawFieldDef`/`RawMessage`）。値未加工・マーカー保持・YAML 順保持・長さ省略は `null`・FW_HEADER もデータ保持・**大文字化しない**。本体テストと変換ツールが共有する公開 API。
  2. **値加工＋組み立て層**＝`YamlValueProcessor`（仮）。`Raw*` を受け取り `interpret`＋`BinaryFileInterpreter`(basePath)＋メッセージ長 `-` 注入＋`fillDefaultValues` を施して**本体器を組み立てる**。本体テスト専用。構造層は interpret を一切知らない。
  3. `YamlTestDataParser` を「structureMapper → valueProcessor」の明示 2 呼び出しに再配線。`interpret()` の「interpreters 空なら素通し」暗黙切替を廃止。
  4. 既存 YAML テストを「構造層=生値／値加工層=解釈・組み立て」に整理しつつ全 GREEN 維持。
- **なぜ本体器を共有しないか（実コードで確認）**: `TableData#setColumnNames`/`addRow` がカラム名・テーブル名を**大文字化**（`TableData.java:489-494,96-98,530`）→ `emp_name`→`EMP_NAME` で往復破壊。メッセージは長さ省略に `-` を注入し `addValue`→`replaceFieldSize` が**実値バイト長で `-` を上書き破壊**（`DataFileFragment.java:111-114,140-154,289-296`）→「長さ省略」復元不能。マーカー `[COL]` は `TableData` 構築時に脱落。list_maps は `TreeMap` でキー順変化（`YamlTableDataBuilder.java:154`）。**決定打**: 変換ツールの現 `YamlFormatReader`(`tool/converter/yaml/`)は既に本体器を経由せず生 YAML Map を独自ウォーク（`:93-250`）→ 共有すべきは器でなくウォーク。
- **判断 A（Excel）は不変**: Excel は本体 Parser 再利用の都合上「本体器」が唯一の共有点ゆえ §共通 の getter 経由は正。option C は YAML 経路（判断 B）のみの精緻化。設計書 §判断 B の補足に反映済み。
- **#7 への波及**: 変換ツール `YamlFormatReader` の独自ウォーク（重複）は #7 で削除し `Yaml*StructureMapper` へ再接続（Raw*→中間モデル）。これが「本体/変換ツールの読み込み二重実装」解消＝6.3 不整合の根治。
- **着手前に要確認だった事項（解決済み）**: 公開 API 形 → ユーザー指示「設計書通り・あるべき姿優先・確認不要」により上記で確定。
- **実装中の要確認（2 件・潜在挙動）**: ① list_maps の `TreeMap` キー順変更が本体テスト assertion に影響しないか（構造層は YAML 順保持＝挙動が変わりうる）。② fw_header 値は現状 interpret されない（`YamlMessageBuilder.extractFwHeader` は素の `objectToString`）— 分離時に挙動を変えないこと。
- **#2 実装で確定した設計判断（reader 系 159 件 GREEN で実証済み・#7 へ波及）**:
  - ① 空マッピング `{}` 行は構造層が **空リスト `[]`** として保持（行の有無を後段が判別）。テーブルは値加工層でスキップ、list_maps は空行として残す（本体差異を再現）。
  - ② `fw_header` の「マップ検証」は **値加工層が読み出すメッセージに対してのみ遅延実行**（`RawMessage.fwHeader` は生 `Object` 保持）。同一ファイル内の誤記エントリが他エントリ読み出しを巻き添えにしない旧挙動を維持。
  - ③ `toTableDataList`/`toDataFileList` は例外メッセージ用に `sectionKey` 引数を取る（テストがセクション名を要求）。
  - ④ list_maps 出力は TreeMap 維持（本体不変）、`RawListMap` は YAML 順保持（変換ツール用）。

## D-H: YAML 経路は「本体器＋YamlLoader Map で原文復元」へ（Raw* 破棄・Excel と対称・ユーザー決定 2026-06-14）

- **Conclusion**: #7 YAML IN は Excel(#6) と**対称**の方式で作る＝**本体器**で構造を得て、**`YamlLoader.load` が返す Map で原文を復元**し中間モデルへ写す。#2 で導入した `Raw*`(6本)・`Yaml{Table,File,Message}StructureMapper`・`YamlValueProcessor` は**破棄**。D-F（option C/Raw*）は棄却。設計書 §判断 B（構造マッピング層→**本体器**）は**正しく据え置き**。§共通「器が正規化する値の原文復元」は「**Excel=生行 / YAML=YamlLoader Map**」と両形式へ一般化是正する。
- **決定的根拠（実コードで確認）**: 前回 Plan エージェントは「YAML には Excel の生行に相当する原文源が無い」を前提に Raw* 温存を推奨したが、これは**誤前提**。`YamlLoader.load`（`reader/yaml/YamlLoader.java`）は SnakeYAML Engine の `Load.loadFromInputStream` 結果（**順序保持 LinkedHashMap・原文値＝本体器の正規化前**）をそのまま返す。これが Excel の生行に相当する**原文源**。よって YAML も「器で構造（`TableData` 等が大文字化・列ソート等で正規化）＋ Map で原文（カラム名・YAML 列順・値・型表記・長さ省略 `-`）復元」が成立し、器を経由しない Raw* は不要。
- **なぜ Raw* が不要か（ユーザー指摘）**: 器の正規化は Excel 同様に起きるが、原文は YamlLoader Map（`firstRow.keySet()` が記述順のカラム名原文・値も解決後の原文）から取れる。Raw* は「器を経由しない並行構造」で重複・余剰。
- **次セッションの段取り**: ① 正しい前提で #7 を Plan 再設計（YAML 読み込みを本体器生成へ＝旧 `YamlTableDataBuilder`/`YamlFileBuilder`/`YamlMessageBuilder` 相当を復活/再構成、`YamlTestDataParser` を再配線）。② `Raw*`/StructureMapper/ValueProcessor とテストを除去/作り直し。③ `tool/converter` の YAML IN リーダ（旧名 `YamlFormatReader`・#3 で削除済）を Excel と対称に新設（器＋YamlLoader Map→中間モデル）。④ 設計書 §共通を両形式一般化へ是正。⑤ TDD・jacoco・3 観点レビュー・`P3-7.md`・`complete task #7`。
- **released 本体への影響**: 本方式は released 本体プロダクションコードの変更を要しない見込み（YAML 系は未リリース新規コード）。万一 released 本体に触れる必要が出たら**着手前にユーザー相談**（Operating mode）。

## D-I: 変換ツール入口 #10 は「テストコードから呼べる convert」までに絞る・Mojo はリポジトリ分割後（ユーザー決定 2026-06-15）

- **Conclusion**: #10 は `TestDataConverter.convert(ConversionRequest)` を中核に、4 方向変換を**テストコード（#13 `YamlModeTestBase`）から呼べる**ところまでを実装する。**Maven プラグイン `ConverterMojo` はリポジトリ分割後**に実施＝#10／本ブランチのスコープ外。CLI（`main`/`run`）は 6.3 達成に不要なら最小限または後回し。
- **Rationale**: `maven-plugin-api`/`maven-plugin-annotations` の追加と packaging 変更は環境変更であり、リポジトリ分割（NTF 本体と変換ツールの分離）後にまとめて整備する方が自然。6.3（Level3）の達成に必要なのは「Excel→一時 YAML 変換をテストコードから起動する」ことのみ。
- **影響**: `pom.xml` の `exec-maven-plugin` mainClass 是正（#3 ゲートの積み残し）もリポジトリ分割後の CLI/Mojo 整備時へ繰り延べ。#10 完了条件は「入口 API から 4 方向実行可能・テストから呼べる・単体 GREEN」へ収束。

## 既存の有効な決定事項（YAML 仕様・本体①に適用、継続有効）

判断に迷った場合は、対応する文書の該当章を正とする。

| 決定 | 内容 | 正となる文書 |
|---|---|---|
| 型記法 | フィールド型は日本語名称（`半角英字`/`全角`/`数値` 等）。記号（`X`/`N`/`Z`）・identity mapping は不採用 | 設計書 §5、スキーマ `$defs/field_def/type` |
| FW 制御ヘッダ | `messages`（MESSAGE）のみ `fw_header:` マップで表す。`expected_request_*`/`response_*` は `records` の `fields/rows` で定義。`record_type: FW_HEADER` 方式は廃止 | 設計書 §12、スキーマ |
| ランタイム FW 分離 | `getMessage`（messages）経路のみ `fw_header` を読む。`getMessageWithoutCache`（expected/response）経路は空 Map | 設計書 §12、仕様リスト MS-04 |
| ディレクティブ分離 | `text-encoding` 等は `directives:` に入れる。FW 制御ヘッダ・電文ボディに混入させない | 設計書 §12 |
| 電文構造 | 電文は ディレクティブ群 → FW 制御ヘッダ群 → `no` 行（フィールド名称行）→ 型 → 長さ → データ の順 | 設計書 §12、解説書 §7 |
| 混在順序 | `expected_tables` と `expected_complete_tables` の混在順序は自由（YAML はセクションキーで独立取得） | 設計書 §4、解説書 §3.3 |
| 数値書式セル | 数値/日付書式セルは `DataFormatter#formatCellValue(cell)` で文字列化。`cell.toString()` は使わない | 設計書 数値セル注記 |
| リソース名 | リソース名は `"ブック名/シート名"` を維持（ブック名→ディレクトリ、シート名→ファイル名） | 設計書 移行戦略 |

### ADR

- `docs/pr75/adrs/ADR-001-yaml-library.md`: SnakeYAML Engine 3.0.1 採用の根拠
- `docs/pr75/adrs/ADR-002-yaml-dependency-scope.md`: compile スコープ採用の根拠

---

## 成果物

| 種別 | ファイル | 内容 |
|---|---|---|
| **NTF 変換ツール設計書（正）** | [docs/testdata-converter-design.md](docs/testdata-converter-design.md) | Excel↔YAML 変換ツール設計書（唯一の正） |
| **読み込み機構の解説** | [docs/ntf-testdata-loading.md](docs/ntf-testdata-loading.md) | NTF 本体 Excel 読み込み 4 段階の解説 |
| **NTF テストデータ解説書** | [docs/ntf-testdata-doc.md](docs/ntf-testdata-doc.md) | YAML テストデータ記述仕様書 |
| **スキーマ** | [src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json](../../src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json) | JSON Schema 定義 |
| **仕様リスト** | [ntf-impl-spec-list.md](ntf-impl-spec-list.md) | 全 145 件（解説書 × 実装 × テストメソッド） |

---

## 環境情報

- **Java**: Eclipse Temurin 17（`update-alternatives` で切り替え済み）
- **Maven settings**: `~/.m2/settings.xml` に社内 Nexus リポジトリ設定済み
- **注意**: `mvn clean package` は Javadoc プラグインが `JAVA_HOME` 未設定で `BUILD FAILURE` になるが、テスト自体は全グリーン。`Tests run:` 行と `Failures: 0, Errors: 0` で確認すること
- **pom.xml ローカル変更**: parent `6-NEXT-SNAPSHOT`→`6u3` の未コミット変更あり（ローカル依存解決用）。目標対象外・コミットしない。**resume / dirty-tree チェックではこの `M pom.xml` を理由にユーザーへ確認しない＝既知の正常状態としてそのまま続行する**（ユーザー指示 2026-06-15）

### カバレッジ取得方法

```bash
# 1. テスト実行（jacoco.exec がプロジェクトルートに生成される）
mvn clean package -Dtest="対象テストクラス..."
# 2. レポート生成
mvn jacoco:report -Djacoco.dataFile=/path/to/nablarch-testing/jacoco.exec
# → target/site/jacoco/index.html で確認
```

`mvn test` だけでは `restore-instrumented-classes` が走らず `jacoco:report` でエラーになる。`package` まで実行すること。

### 運用ノート（恒久・過去タスクで確定）

- **オフライン/オンラインの可否**: オフライン `mvn -o test` は可。ただし `package`（jacoco.exec 生成に必要）は clean-plugin/git-commit-id-plugin 等が未キャッシュで**オフライン不可**＝**online `mvn package -Dmaven.javadoc.skip=true`** で jacoco.exec を生成する。`clean` ゴールもオフライン不可なので `rm -rf target` で代替。Javadoc プラグインは BUILD FAILURE になるがテストは GREEN。
- **失敗集合ベースライン（既存事象・PR75 非起因）**: `mvn -o test` の残存 Error は Mockito 環境起因 **2 クラス＝`MockHttpRequestTest`(2E)/`MockServletExecutionContextTest`(2E)＝計 4E** のみ（#10 時点 1231 件 0F/4E で確認）。`*YamlTest` 統合 13 クラスは #3 で @Ignore 化済＝Skip（6.3＝#13/#14 で再有効化・解消対象）。※旧 `ConverterFileFilterTest`（Mockito 起因）は #3 で削除済→ #10 で実 temp dir ベースの新 `ConverterFileFilterTest` を新設し GREEN（もはや失敗集合に含まれない）。等価性テストの IllegalArgument/IllegalState ラップ差は T7 由来で PR75 マージ前に別途解消が必要。
- **テスト集計の罠**: surefire-reports 走査で `failures="[1-9]"` は 1 桁しか拾わない。**`failures="[1-9][0-9]*"` を使うこと**（10 件以上の失敗を取りこぼす）。

---

# State

- **Status**: paused
- **Date**: 2026-06-15
- **Last completed**: **#10（変換ツール入口・周辺の再構築）= 本セッションで Verify 完了**（3 観点レビュー全 PASS・must-fix なし・`docs/pr75/checks/P4-10.md`・38 件 GREEN・C0/C1 100%〔番人除く〕・全モジュール回帰 1231 件 0F/4E＝新規失敗ゼロ・本体ゼロ差分）。`complete task #10` でコミット済。#9＝`a4c74ea`・#8＝`63e8dc7`。
- **Next**: **#11（YamlTestDataValidator 再構築・検証モード）**。Prerequisites #4/#7 完了済＝着手可。設計書 §リンタ／`KNOWN_DIRECTIVE_NAMES` が本体ディレクティブと一致する整合テストを含むこと。完了条件＝各検証ルール（列数一致・構造境界・スキーマ適合＋V-FNAME/V-DKEY/V-MSGROW 等）をテストで実証。**ただし #11 はゴール 6.3 達成の必須経路ではない**（6.3 は #13/#14＝Phase 6 で達成）。マージのキリどころを優先するなら #11 完了で **Phase 4 完了ゲート**に到達する（ユーザーと相談した「Phase 4 完了でマージ」の区切り）。
- **マージ方針メモ（本セッションのユーザー相談結果）**: PR #1（head `add-yaml` → base `convert-testdata-excel-to-text`・MERGEABLE・FF 可）。`add-yaml` は base に対し先行のみ・0 遅れ＝コンフリクトなし。「フェーズの切れ目がキリが良い」→ **直近の妥当な区切りは Phase 4 完了（#10＋#11）**。ただし end-to-end で動く＝6.3 緑になるのは Phase 6（#13/#14）まで。理想は #14（6.3 達成＝PR 本来の exit 条件）。main（リリース系列）へは入れない＝統合ブランチ止まり。
- **Notes（#11 着手用 ＋ #10 残 nice-to-have）**:
  - **#10 完了済の入口 API**（#11/#13 が利用）: `TestDataConverter.convert(ConversionRequest)` ＋ `convert(DataFormat from, DataFormat to, Path in, Path out)` ファサード。`ConversionRequest.Builder`（sourceFormat/targetFormat/inputPath/outputPath/overwrite/include/exclude・同形式許容）。`DataFormat`(XLS/YAML・`getArgument`/`fromArgument`)。`ConverterException`(非検査)。`ConverterPathResolver`(純関数・`outputBaseForYaml`/`outputBaseForXls`/package-private `stripExtension`)。`ConverterFileFilter`(`findXlsFiles`/`findYamlDirs`)。戻り値＝変換コンテナ件数(int)。上書き衝突は `ConverterException`。
  - **#13（Phase 6）への申し送り**: `YamlModeTestBase.prepareYamlData()` の再接続は `convert(ConversionRequest)` で Excel→一時 YAML（`overwrite(true)` 推奨）。入口は `inputPath`(Excel ルート)・`outputPath`(一時 dir)・`sourceFormat(XLS)`・`targetFormat(YAML)` を渡す。出力は `<out>/<ブック名>/<シート名>.yaml`（YamlFormatReader が読む単位と一致）。
  - **#10 残 nice-to-have（must-fix ではない・余裕があれば #11 と同時 or Phase 4 ゲート前に）**:
    - QA: ①空文字 `""` 往復端点テスト ②YAML 複数 .yaml の 1 コンテナ集約 E2E（Excel 複数シートと対称）③`convert` 経由の include E2E（現状 exclude のみ）
    - Java: ①`ConverterFileFilter.find` の `java.util.function.Predicate` を FQN→import ②`matchesAny` の `PathMatcher` をパターン毎に毎回生成→`find`/`findYamlDirs` 冒頭で事前コンパイルしてキャッシュ ③`ConversionRequest` の include/exclude Javadoc に glob 区切り規約（`/` 区切り）を明示
    - SWE: ②拡張子定数（`.yaml`/`.xlsx`）が `ConverterFileFilter`/`TestDataConverter`/各 Writer に散在→集約余地（③`isSendSync` 分類重複は #10 スコープ外＝リポジトリ分割後）
  - 設計の正＝設計書 §リンタ・[[D-H]]・[[D-I]]。TDD・jacoco（online `mvn package -Dmaven.javadoc.skip=true` → `jacoco:report`）・3 観点レビュー・`docs/pr75/checks/P4-11.md`・`complete task #11`。
  - **環境**: `M pom.xml`（parent `6-NEXT-SNAPSHOT`→`6u3`）は既知のローカル変更＝**コミット禁止・resume で確認不要**（環境情報 §参照）。Operating mode により確認不要で自律続行可。

## Operating mode（ユーザー指示・2026-06-13・継続有効）

**私に確認せず 6.3 まで自律的に目指す**。原則設計書通り／問題が起きたら常にあるべき姿を優先。NTF の YAML 対応・変換ツール・テストコードは自由に変更可。**released な NTF 本体プロダクションコードのみ後方互換維持があるので変更前にユーザーへ相談**（できるだけ変更しない）。困ったらエキスパート（サブエージェント）に相談。どうにもならない場合のみユーザーに相談。git でいつでも戻せる。フェーズ完了ゲートのユーザーレビューも「確認不要」指示によりスキップしてよい（必要時のみ相談）。
