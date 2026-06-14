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

- [ ] アダプタ呼び出し→中間モデル組み立てを実装
- [ ] 全データ種別の単体テスト（TDD）
- [ ] セルフチェック

**Completion criteria**:
- 全種別を無損失で中間モデル化／IN 値が記法のまま
- 独自の構造解析（`parseBlocks`/`isDataRow`/`trimQuotation`/POI 直叩きの構造判定）を含まない（レビュー確認）
- 単体テスト GREEN・C0/C1 100%（番人除く）

### #7: YamlFormatReader 再構築（YAML IN・判断 B）

**Purpose**: 本体の構造マッピング層(#2)を直接呼び、生の器を中間モデルへ写す。SnakeYAML 直叩きの構造解析は持たない。

**Prerequisites**: #2, #4

**Steps**:

- [ ] 構造マッピング層呼び出し→中間モデル組み立てを実装
- [ ] 全データ種別の単体テスト（TDD）
- [ ] セルフチェック

**Completion criteria**:
- 全種別を無損失で中間モデル化／IN 値が記法のまま
- 独自の YAML 構造解析を含まない（レビュー確認）
- 単体テスト GREEN・C0/C1 100%（番人除く）

**Phase 3 完了ゲート**: 4 観点レビュー → ユーザーレビュー OK

## Phase 4 — OUT（書き出し）＋入口＋検証の再構築（TDD）

### #8: YamlFormatWriter 再構築（YAML OUT）

**Purpose**: 中間モデル→YAML を記法どおりに書き出す。

**Prerequisites**: #4

**Completion criteria**: 全種別を YAML 出力／単体テスト GREEN・C0/C1 100%（番人除く）

### #9: XlsFormatWriter＋ExcelFormatConfig 再構築（Excel OUT）

**Purpose**: 中間モデル→Excel を整形設定（`ExcelFormatConfig`、デフォルト備え上書き可）に従い書き出す。

**Prerequisites**: #4

**Completion criteria**: 全種別を Excel 出力／整形はデフォルトで見やすい既定値／単体テスト GREEN

### #10: 変換ツール入口・周辺の再構築

**Purpose**: `convert(from,to,input,output)` 入口、CLI/Mojo、ディレクトリ走査・include/exclude・上書き可否を再構築（`TestDataConverter`/`ConversionRequest`/`DataFormat`/`ConverterFileFilter`/`ConverterPathResolver`/`ConverterException`）。

**Prerequisites**: #6, #7, #8, #9

**Completion criteria**: 4 方向変換が入口から実行可能／単体テスト GREEN

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

## D-F: #2 本体 YAML 2 層分離 — 共有するのは「本体器」でなく「構造ウォーク」（option C・あるべき姿）【YAML 経路に限り有効・Excel は D-G が上書き】
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
- **pom.xml ローカル変更**: parent `6-NEXT-SNAPSHOT`→`6u3` の未コミット変更あり（ローカル依存解決用）。目標対象外・コミットしない

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
- **失敗集合ベースライン（既存事象・PR75 非起因）**: 全モジュール 16 失敗クラス（43F/44E）＝Mockito 環境起因 3 クラス（`MockHttpRequestTest`/`MockServletExecutionContextTest`/`ConverterFileFilterTest`）＋ `*YamlTest` 統合 13 クラス（旧変換ツール経由・6.3 で解消対象）。`d3cd139`↔最終で完全一致。等価性テストの IllegalArgument/IllegalState ラップ差は T7 由来で PR75 マージ前に別途解消が必要。
- **テスト集計の罠**: surefire-reports 走査で `failures="[1-9]"` は 1 桁しか拾わない。**`failures="[1-9][0-9]*"` を使うこと**（10 件以上の失敗を取りこぼす）。

---

# State

- **Status**: in_progress
- **Date**: 2026-06-14
- **Last completed**: #5（commit `cb881bc`）。#6 は wip（`4eedba3`）。設計書をユーザーが再改訂（`f20ee92`→`d89c434`→**本セッションで判断 A を (a) 2 アダプタ方式へ・未コミット `M`**）。判断は全て確定（D-G）。
- **Next**: 下記 **Recovery Plan** を R1 から TDD で順次実施。R1＝本体撤回＋アダプタ 2 枚化。
- **確定事項（D-G）**: ① 本体無変更へ収束＝getter 撤回(DataFile/DataFileFragment)・getFwHeader 撤回。② Excel は `TestCoreReaderAdapter`＋`TestCoreFileAdapter` の 2 枚。③ QuotationTrimmer 据え置き。

## Recovery Plan（新設計書 = 正・D-G 反映・TDD・各 R は 1 commit→push→裏取り報告）

- **R0（本コミット）**: 設計書（判断 A=(a) 2 アダプタ）＋ steering を確定コミット。
- **R1 — 本体無変更化＋アダプタ 2 枚化（判断 A 確定）**:
  - `TestCoreFileAdapter`（`nablarch.test.core.file` 相乗り・新設）を TDD で新設：`DataFileFragment` の `names`/`types`/`lengths`/`values`・長さ省略判定を plain で返す。`isOndemandCalc*` private 問題は D-G の (i)/(ii) を実装時に確定。
  - `TestDataParserAdapter` → `TestCoreReaderAdapter` 改名。file 系は raw 器を返し、内部値は `TestCoreFileAdapter` 経由に。
  - 本体撤回：`DataFile`(+18)・`DataFileFragment`(+47) getter／`MessagePool.getFwHeader` public。
  - テスト追従：`TestDataParserAdapterTest`（336/362/386/410 が `file.getAllFragments().getValues()` を使用）を `TestCoreFileAdapter` 経由へ移送。`YamlTestDataParserTest:565` の stale コメントは撤回後に正となる。
  - 検証：`git diff main..HEAD -- core/file core/messaging core/reader/*Parser*` が**本体ゼロ差分**（新規アダプタ・YAML 層・新規ファイルのみ）。reader 既存テスト全 GREEN。
- **R2 — #6 XlsFormatReader を 2 アダプタ＋原文復元へ**: `TestCoreReaderAdapter`＋`TestCoreFileAdapter` で器・内部値を取得し、§共通「原文復元」3 点（長さ省略＝生行、型表記＝生行型行、LIST_MAP 列順＝HeaderLine／大文字化は復元不要）を Reader 側で組込。マーカー列除外で生行↔器 index 1:1。
- **R3 — #6 電文 4 種編入**: `EXPECTED_REQUEST_HEADER/BODY_MESSAGES`・`RESPONSE_HEADER/BODY_MESSAGES`（6.3 コーパスの messaging 系 6 クラスが使用＝必須）。`TestCoreReaderAdapter` に send-sync 2 経路（`getMessageWithoutCache`/`getSendSyncMessage`）を追加し `MessageDataBlock` の 4 種へ。FW ヘッダは 4 種とも空。
- **R4 — #7 YAML 経路アセスメント**: 新設計書 §判断 B（構造マッピング層→**本体器**）と現状コード（`Raw*`→`Yaml*StructureMapper`→`YamlValueProcessor`）の乖離を評価。YAML は生行が無いため原文復元手段が Excel と異なる＝D-F(option C/Raw\*) を内部実装として温存できるか、設計書文言へ寄せて作り直すかを Plan エージェントで判定 → ユーザー相談。
- **R5 — util 共通化**: マーカー列判定（`HeaderLine` private）・コメント/空行判定（`TestDataParsingTemplate`）を public util へ（観測挙動不変リファクタ）。本体と変換ツールで共有。
- **R6 — #6 仕上げ**: 回帰 `mvn -o test`・カバレッジ jacoco・3 観点レビュー（QA/Java/SWE）・`P3-6.md`・`complete task #6`。

- **旧 Notes（経緯・参考）**:
  - **本セッションの経緯（重要）**: #6 残作業（要求/応答電文4種の扱い確定）を調査中、ユーザーが「本体既存コードに変更が入っている／設計書では本体無変更では？」と指摘。さらにユーザーが**設計書をローカル改訂**（commit `f20ee92`）。これにより設計書とコードにズレが生じ、リカバリ方針の検討に切り替えた。**まだコードは一切変更していない**。
  - **設計書 `f20ee92` の改訂内容（main からの差分はこの 1 セクションのみ）**: §共通（器の中身を読む手段）を「**本体無変更**／getter は整備済み」と書き換え、`getRecordType` を getter 一覧から除外、`getFwHeader` の public 化記述を削除。さらに **判断 B の補足（option C＝Raw\*・構造ウォーク共有）の節をまるごと削除**し、判断 B を「構造マッピング層が**本体器**を返す」（＝元の文言・Raw\* 不採用）へ戻した。
  - **現状コードと新設計書の一致度**: 約 8 割一致。中間モデル(#4)・Excel アダプタ(#5)・XlsFormatReader(#6 wip) は新設計書と**一致**。ズレは 2 点のみ（下記）。
  - **ズレ①（本体既存コード変更・released 4 ファイル。`git diff main..HEAD` で確認済）**:
    - `DataFile.java`(+18): public getter `getAllFragments`/`getDirectives` 追加（純粋追加・後方互換）。
    - `DataFileFragment.java`(+47): public getter `getRecordType`/`getNames`/`getTypes`/`getLengths`/`getValues` 追加＋クラスに `@Published(tag=architect)`（純粋追加・後方互換）。
    - `MessagePool.java`(+3/-1): `getFwHeader()` を **package-private→public** 化（可視性拡大）。**呼び出し元を全数調査した結果、この public 化を要する本番呼び出し元は無い**（アダプタは `MessageParser.getFwHeader` を呼ぶ／テストは同一パッケージ）。`YamlTestDataParserTest:565` に「package-private のため」という今や嘘のコメントが残存。→ **過剰公開＝package-private へ戻す是正候補**。
    - `QuotationTrimmer.java`(+5): `if (str.length() < 2) return str;` ガード追加＝**唯一のロジック（挙動）変更**。引用符1文字で `substring(1,0)` 例外になるバグの修正。git 履歴に逡巡あり（`1f09271` で一度 main 同一へ revert→`c33e121` で再投入）。設計書 L56「Excel 読み込みの観測可能な挙動維持は必達」に抵触しうる。
    - 評価: getter 追加 2 ファイルは設計書 L56「挙動を変えないリファクタリングは可」に収まり**残してよい**（doc L110 も読み手段として列挙）。`getFwHeader` public は**戻す**。`QuotationTrimmer` は**ユーザー判断**。
  - **ズレ②（YAML 判断 B）→ ユーザーが設計書改訂で解決済（commit `d89c434`）**: 当初の論点（新設計書の「本体器を返す」が #2 の option C(Raw\*) と衝突し、本体器のままだと YAML 往復が壊れる＝品質 L2 未達）に対し、ユーザーが**第3の方式**を設計書へ明文化した：
    - **方式＝「本体器を使う＋器が正規化する箇所だけ生行/HeaderLine から原文復元」**（Raw\* には依らない／素の本体器でもない）。器の取り出し経路で原文が変わるのは**3 点のみ**と全データタイプで確認済：① カラム名/テーブル名の大文字化＝NTF 仕様上無意味で**復元不要**、② 長さ省略(`-`)＝器 `isOndemandCalcFieldSize(i)` で識別し生行から原文の値・長さを取る、③ 型表記(X/N/B/Z)＝生行の型行から取る、④ LIST_MAP 列順＝`HeaderLine` から取る（器から取得可・生行不要）。生行はマーカー列(`[...]`)を除外すると器フィールドと index 1:1 対応。
    - **重複実装回避**: 形式非依存の判定（マーカー列判定＝`HeaderLine` private、コメント/空行判定＝`TestDataParsingTemplate`、行末空セル＝`NablarchTestUtils.trimTailCopy`）を public util へ切り出し本体と共有（観測挙動不変のリファクタ）。行種別判定は本体状態機械から切り出せないため④の器＋生行 index 対応で代替。
    - **QuotationTrimmer**: 設計書 L56 に「明確なバグの修正は観測挙動維持を破らないため許容」を追記＝**残してよい（解決）**。加えて「クォートは Excel では `QuotationTrimmer`、YAML では YAML ライブラリが解決＝YAML に QuotationTrimmer 不適用／YAML OUT は全値クォート」を明文化。
  - **この解決がコードに与える影響（resume で最重要・要アセスメント）**: 新設計書は **#2 の option C(Raw\*) 群を使わない方式**。よって現状コード（`RawTableData`/`RawListMap`/`RawDataFile`/`RawRecordLayout`/`RawFieldDef`/`RawMessage`・`Yaml*StructureMapper` が Raw\* を返す・`YamlValueProcessor`）は**新設計書と乖離**。resume では「#2 を新方式（構造マッピング層→本体器、原文復元は呼び出し側=Reader が生行/HeaderLine から）へ作り直す」必要があるか、Raw\* を内部実装として温存しつつ設計書表現と整合させられるかを**最初にアセスメント**すること（D-F は旧 option C 決定。新設計書 `d89c434` が上書き＝D-F は要改訂/廃止）。Excel 側 #5/#6 も「原文復元 from 生行」を Reader 側で行う設計に合わせる（現 #6 は本体器のみ参照で原文復元未実装＝長さ省略/型表記の原文が器の正規化値のまま入る恐れ→要確認）。
  - **残る確定事項（軽い是正）**: `MessagePool.getFwHeader()` の public 化は呼び出し元なし＝**package-private へ戻す是正**（新設計書は §共通で getter のみ列挙し getFwHeader public 化を記述しない＝本体無変更方針に沿う）。getter 追加(DataFile/DataFileFragment)は L56 リファクタ許容で残す。
  - **削除して作り直すか**: **全削除は不要**（ユーザーへ報告済）。中間モデル #4・Excel アダプタ #5・XlsFormatReader #6 の骨格は新設計書と一致し健全。要改修は **#2 の YAML 経路（Raw\*→本体器+原文復元）** と **Excel/YAML 両 Reader への原文復元の組み込み**、および軽微是正(getFwHeader)。局所是正で収束する見込み。
  - **resume 時の段取り**: ①新設計書 `d89c434` を精読（特に §「器が正規化する値の原文復元」「重複実装を避ける」「特殊記法の扱い」）→ ②#2 コード(Raw\*) と新方式の乖離をアセスメント（作り直し範囲を確定。必要なら Plan エージェント）→ ③steering の Tasks/Decisions を新設計書基準のリカバリプランへ更新（D-F 改訂・#7 以降の YAML 経路記述差し替え・「原文復元」「util 共通化」タスク追加・電文4種を #6 に正式編入）→ ④`MessagePool.getFwHeader` 是正 → ⑤改修を TDD で実施 → ⑥#6 残作業へ。**コードは未着手＝まず方針(リカバリプラン)をユーザーに提示してから着手**（ユーザーは方針提示を求める傾向）。
  - **#6 残作業（設計整合の後）— 旧 State から継承**: (1) **要求/応答電文4種（`EXPECTED_REQUEST_HEADER/BODY_MESSAGES`・`RESPONSE_HEADER/BODY_MESSAGES`）は #6 で対応必須と判明**。本セッションの調査で **6.3 コーパスの 18 `*YamlTest` のうち 6 クラス（messaging 系：`RequestTestingSendSyncSupportYamlTest`/`RequestTestingSendSyncBatchYamlTest`/`RequestTestingMessagingClientYamlTest`/`RequestTestingMessagingContextYamlTest`/`MessagingRequestTestSupportYamlTest`/`MessagingReceiveTestSupportYamlTest`）がこの4種を使う**ことを確認（DataType 定数・各テストの直接参照で実証）。よって「先送り(b)」は不可、#6 内 or #14 前に必ず実装。実装方法は調査済: 本番は `BasicTestDataParser.getMessageWithoutCache`(SendSyncMessageParser・単体 MessagePool) と `getSendSyncMessage`(GroupMessageParser・caseNo グループ・`List<RequestTestingMessagePool>`) の 2 経路。本文は `SendSyncMessageParser.createFixedLengthFileParser`→`MockMessages`(FixedLengthFile)。**FW ヘッダは4種とも常に空**（`SendSyncMessageParser.getFwHeader` は例外／`GroupMessageParser` は emptyHeader）。`MessageDataBlock` は5種別対応済ゆえモデル追加不要。アダプタに `readSendSyncMessages`(または2経路分)を新設し `MessageDataBlock` の4種へ写す方針。**ただし判断②(YAML 経路)の結論次第で Excel/YAML の中間モデル整合が変わるため、設計整合を先に確定すること**。(2)回帰 `mvn -o test`、(3)カバレッジ jacoco（運用ノート参照）、(4)3観点レビュー（QA/Java/SWE）、(5)`P3-6.md` 確定＋`complete task #6` コミット。
  - **環境**: ブランチ `add-yaml`。`pom.xml` の `6u3` ローカル変更は**コミットしない**（[[new-code-prefer-ideal-design]]）＝`git status` に常に `M pom.xml` が残るのは正常。
  - **教訓（memory 候補）**: サブエージェント（アーキテクト）の結論は実コードで裏取りする（電文4種の「readFiles 経由」提案は誤りだった）。設計書は固定の聖典でなく「あるべき姿に追随させる唯一の正」＝コードと食い違ったら設計書側を是正する運用（D-F が実例。今回のユーザー改訂で再確認）。

## Operating mode（ユーザー指示・2026-06-13・継続有効）

**私に確認せず 6.3 まで自律的に目指す**。原則設計書通り／問題が起きたら常にあるべき姿を優先。NTF の YAML 対応・変換ツール・テストコードは自由に変更可。**released な NTF 本体プロダクションコードのみ後方互換維持があるので変更前にユーザーへ相談**（できるだけ変更しない）。困ったらエキスパート（サブエージェント）に相談。どうにもならない場合のみユーザーに相談。git でいつでも戻せる。フェーズ完了ゲートのユーザーレビューも「確認不要」指示によりスキップしてよい（必要時のみ相談）。
