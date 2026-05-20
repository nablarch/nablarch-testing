# NTF テストデータ YAML 実装フェーズ

ブランチ: `convert-testdata-excel-to-text`

## 目的

YAMLスキーマ設計フェーズ（完了済み）で固めたスキーマを実際に動かす。
目的は2つ。

1. 設計したYAMLスキーマがNTF仕様を満たしていることを検証する
2. YAMLスキーマでNTFを動かす（TDDベース）

「実装した」「テストが通った」だけでは不十分。
「NTF仕様の全IDに対してテストが1対1で対応しており、カバー漏れゼロである」ことを第三者に根拠で説明できる状態を目指す。

---

## フェーズ概要

| フェーズ | 目的 | 前提 | 完了条件 |
|---|---|---|---|
| Ph-1 | NTF仕様一覧 × 既存テスト × YAMLスキーマの三角マッピング確立 | なし | I-1/I-2/I-3 全完了。`ntf-impl-spec-list.md` の全仕様IDに「分類・スキーマ根拠またはスキーマ外理由・既存テストメソッドまたはテスト追加必要」が記載されること |
| Ph-2 | YAMLリーダー実装（TDDベース） | Ph-1 完了 | 全仕様IDに対応するテストがグリーンであること |
| Ph-3 | 既存Excelテストの YAML版並走と差分ゼロ確認 | Ph-2（R-1）完了 | ExcelリーダーとYAMLリーダーで全テストが同一結果でグリーンであること |
| Ph-4 | 仕様カバレッジ根拠文書の作成 | Ph-2/Ph-3 完了 | 全仕様IDのカバー状況が「済」または「意図的除外（理由付き）」で埋まること |

---

## Ph-1: 三角マッピング確立

### I-1: 仕様ID一覧の確定と棚卸し

**目的**: 後続タスク全体の基準となる「NTFテストデータ仕様ID一覧」を確定する。

**作業内容**:
- `docs/ntf-coverage-spec-mapping.md` の仕様ID（DT-xx, SS-xx, RS-xx, HC-xx, IV-xx, DR-xx, MS-xx）を全件棚卸し
- 調査で判明したギャップ E-1〜E-9 について、仕様IDとして昇格するか否かを判断し文書に明記する。昇格しない場合は除外理由を記載する
  - E-1: YAML ネイティブ型→文字列化の変換漏れリスク
  - E-2: 末尾空要素の扱い（Excel は null→"" 補完、YAML は末尾省略されやすい）
  - E-3: `readLine() == null` 終了判定タイミングのずれによる最終セクションデータ欠落リスク
  - E-4: `startsWith` 前方一致マッチングの挙動（YAML schema validation とは独立）
  - E-5: sendSyncTestData のディレクトリ配置規則はYAMLスキーマ外
  - E-6: `defaultDirectives` のDI設定は SystemRepository XML の問題でありYAMLファイルとは独立
  - E-7: `EXPECTED_REQUEST_HEADER/BODY_MESSAGES` の行数一致チェックはランタイムのみ
  - E-8: `BasicDefaultValues` の DATE カラムのTZハザード（JSTとUTCで値が変わる）
  - E-9: `BasicJapaneseCharacterInterpreter` の「スルー vs 例外」条件の誤記（design.md D-6）
- 仕様を2つに分類する
  - **テストデータ構造**: YAMLファイルの書き方に直接影響する仕様（スキーマ設計の対象）
  - **実装内部ロジック**: パーサ・コンバータ内部の挙動であり、テストデータ構造に影響しない仕様
- 出力: `docs/ntf-impl-spec-list.md`（仕様ID / 概要 / 分類 の3列）

**完了条件**:
- 全仕様IDに分類が付いていること
- E-1〜E-9 について「仕様IDとして昇格」または「除外・理由付き」がそれぞれ記載されていること
- 抜け漏れがないことを確認した旨が記載されていること

---

### I-2: 仕様ID × 既存テストメソッドのマッピング

**目的**: 既存テストのどのメソッドがどの仕様IDを検証しているかを明示し、カバーゼロの仕様IDを特定する。

**前提**: I-1 完了

**作業内容**:
- I-1 の仕様ID一覧に対して、以下のテストクラスのテストメソッドをマッピングする
  - `BasicTestDataParserTest`（16メソッド確認済み）
  - `MessageParserTest`
  - `FileSupportTest`
  - `SendSyncMessageParserTest`（現状17行のみ、MS-04〜MS-07 は実質未テスト）
  - reader/ パッケージのその他テストクラス
- マッピングされない仕様ID（カバーゼロ）を「テスト追加必要」として明記する
- 特記すべき既知のカバーゼロ仕様:
  - D-14（複数レコードレイアウトの連続記述）: `BasicTestDataParserTest` に専用テストなし
  - MS-04〜MS-07（errorMode/NO列/グループメッセージ）: `SendSyncMessageParserTest` が17行しかない
- 出力: `docs/ntf-impl-spec-list.md` に列「既存テストメソッド or テスト追加必要」を追加

**完了条件**: 全仕様IDに「対応テストメソッド名」または「テスト追加必要（理由付き）」が記載されること。

---

### I-3: 仕様ID × YAMLスキーマ記述のマッピング

**目的**: YAMLスキーマのどのキー/定義が、どの仕様IDを表現しているかを明示する。

**前提**: I-1 完了

**作業内容**:
- I-1 の**全仕様ID**（分類問わず）に対して以下のいずれかを記載する
  - 「テストデータ構造」分類: `ntf-testdata-yaml-schema.json` / `ntf-testdata-yaml-design.md` のどのセクション/キーが対応するかを記載
  - 「実装内部ロジック」分類: 「スキーマ外・パーサ実装で担保」と明記
  - スキーマで表現できない仕様（E-4の前方一致、E-5の配置規則、E-7の行数一致チェック等）: 「スキーマ外仕様・テストで担保する方針」と明記し、後続 R-3 でテスト作成することを記載
- 出力: `docs/ntf-impl-spec-list.md` に列「スキーマ根拠 or スキーマ外理由」を追加

**完了条件**: 全仕様IDに対して「スキーマ根拠箇所」または「スキーマ外理由」が記載されること（分類を問わず全件）。

---

## Ph-2: YAMLリーダー実装（TDDベース）

**前提**: Ph-1（I-1/I-2/I-3）全完了

### R-1: `TestDataReader` インタフェースの YAML実装クラス作成

**目的**: `PoiXlsReader` と同一インタフェースで YAML を読む `YamlTestDataReader` を実装する。

**作業内容**:
- `TestDataReader` インタフェースを実装
- `open(path, dataName)` の呼び出し規約: `dataName` = `"ファイル名（拡張子なし）"` → `{dataName}.yaml` を探す
- `readLine()` の返却仕様（全てExcelの挙動に合わせる）:
  - YAML ネイティブ `null` → 文字列 `"null"` として返す（E-1）
  - YAML ネイティブ boolean (`true`/`false`) → 文字列 `"true"/"false"` として返す（E-1）
  - YAML ネイティブ integer/float → 数字文字列として返す（E-1）
  - 末尾空要素は `""` として補完する（E-2）
  - 文書終端で `null` を返す。`null` を返した直前のセクションデータが欠落しないことを保証する（E-3）
- `isDataExisting` / `isResourceExisting` を実装
- TDD: テストを先に書いてから実装する
- テストクラス: `YamlTestDataReaderTest`

**完了条件**:
- `YamlTestDataReaderTest` が全グリーン
- YAML ネイティブ型の文字列化（E-1）の境界値テスト（null/true/false/integer/float各型）が含まれること
- 末尾空要素補完（E-2）のテストが含まれること
- `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを検証するテストが含まれること（E-3）

---

### R-2: 既存テスト（BasicTestDataParserTest）のYAMLリーダー版作成

**目的**: 既存のExcelベーステストと同一結果をYAMLリーダーで再現し、「ExcelとYAMLが等価である」ことを証明する。

**前提**: R-1 完了

**作業内容**:
- `BasicTestDataParserTest.xls` の内容を YAML に変換し `BasicTestDataParserTest.yaml` として配置
- `BasicTestDataParserTestYaml` を作成し、`TestDataParser` に `YamlTestDataReader` を差し込んで同一アサーションを実行
- 対象: 既存16テストメソッド全件

**完了条件**:
- `BasicTestDataParserTestYaml` の16メソッド全グリーン
- `BasicTestDataParserTest`（Excel版）と `BasicTestDataParserTestYaml`（YAML版）の対応するメソッドが、同一入力データ・同一アサーション内容でグリーンになること
- 差異が生じた場合は原因を文書に明記すること（差異の存在自体は許容するが、隠蔽は不可）

---

### R-3: カバーゼロ仕様の新規テスト作成

**目的**: I-2 で「テスト追加必要」とされた仕様IDと、スキーマ外仕様（E-4/E-5/E-7）のランタイム担保テストを作成し、カバーゼロを解消する。

**前提**: R-1 完了、I-2/I-3 完了

**作業内容**:
- D-14（複数レコードレイアウト）: `DataFileParser` に対して複数 record fragment を持つ YAML テストデータを使ったシナリオテストを追加する（`BasicTestDataParserTest` ではなく `DataFileParser` 直接テスト）
- MS-04〜MS-07（errorMode/NO列/グループメッセージ）: `SendSyncMessageParser` / `GroupMessageParser` に対して YAML テストデータを使ったテストを追加する
- E-4（startsWith 前方一致）: `DataType` 名の前方一致マッチングが YAML セクションキーで正しく機能することを検証するテストを追加する
- E-5（sendSyncTestData 配置規則）: `sendSyncTestData/{requestId}/message` の配置規則が YAML でも機能することを確認するテストを追加する
- E-7（行数一致チェック）: `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数不一致時に `IllegalStateException` が発生することを YAML テストデータで確認するテストを追加する
- E-6（defaultDirectives の DI）: SystemRepository の `defaultDirectives` キーで設定されたデフォルトディレクティブが YAML テストデータにも正しく適用されることを確認するテストを追加する。ただし「DI設定はYAMLファイル外の問題」であるため、テスト対象は「DI設定済みの状態で YAML テストデータが正しく動作すること」に限定し、XML設定の正しさはテスト対象外と明記する
- E-8（DATE型TZハザード）: `EXPECTED_COMPLETE_TABLE` の DATE カラムデフォルト値が CI 環境と同一TZ（JST前提か否か）で動作することを確認する。TZ依存が解消できない場合は制約事項として D-1 に明記する
- E-9（BasicJapaneseCharacterInterpreter誤記）: `design.md` D-6 の「スルー vs 例外」条件の誤記を修正する（テスト実装ではなくドキュメント修正）

**完了条件**:
- D-14/MS-04〜MS-07/E-4/E-5/E-7/E-6/E-8 に対応するテストが全グリーン
- E-9 の `design.md` 誤記が修正されていること
- E-8 について「TZ依存解消済み」または「制約事項として D-1 に記載済み」のいずれかが確認できること

---

## Ph-3: 既存ExcelテストのYAML版並走と差分ゼロ確認

**前提**: R-1 完了

### V-1: 全Excelテストファイルの YAML変換と並走実行

**目的**: リポジトリ内の全59 Excelファイルに対してYAML版を作成し、ExcelリーダーとYAMLリーダーの等価性を確認する。

**作業内容**:
- 変換方針を先に決定する: `nablarch-test-data-converter` を使用するか手動変換するかを明記する
- 全59の `.xls`/`.xlsx` ファイルを `.yaml` に変換する
- 各テストクラスに YAML版テストを作成する（またはリーダーを差し替えて実行する方式でも可）
- 差分が生じた場合の対処方針を明記する: 修正して差分解消するのか、除外して理由を記録するのか

**完了条件**:
- 全テストが Excel/YAML どちらでも同一結果でグリーンであること
- 差分が生じたファイルがある場合、ファイル名・差分内容・原因・対処（修正 or 除外理由）を一覧で記録すること

---

## Ph-4: 仕様カバレッジ根拠文書

**前提**: Ph-2/Ph-3 完了

### D-1: カバレッジマトリクスの完成

**目的**: 「YAMLスキーマがNTF仕様を100%カバーする」ことを第三者に説明できる根拠ドキュメントを完成させる。

**作業内容**:
- `docs/ntf-impl-spec-list.md`（Ph-1 で作成）に以下の列を追加して完成させる:
  - 仕様ID / 概要 / 分類 / スキーマ根拠 or スキーマ外理由 / 既存テストメソッド / 追加テストメソッド / カバー状況
- E-6/E-8 について「TZ依存」「DI設定はXML外」の制約事項欄を設ける
- 出力: `docs/ntf-impl-coverage-matrix.md`

**完了条件（主完了条件）**: 全仕様IDのカバー状況が「済」であること。
**完了条件（許容除外）**: 意図的に除外した仕様IDがある場合は除外理由が明記されていること。
「除外理由なし・カバー状況空欄」は完了とみなさない。

---

## 現在の状態（2026-05-20時点）

- **完了済みフェーズ**: スキーマ設計フェーズ全完了
- **次の着手**: I-1（仕様ID一覧の確定）

---

## 完了済みタスク要約（スキーマ設計フェーズ）

| 完了タスク群 | 概要 |
|---|---|
| P0〜P3 + レビュー5回 | スキーマバグ修正・仕様曖昧箇所確定・ドキュメント補強。専門家4名×5回レビューで全員合格 |
| P4-0〜P4-4 | 仕様網羅性の根拠確立。src/main/java 29クラスを全行走査。未反映仕様 S-1〜S-5 / D-1〜D-16 / E-1〜E-4 を全反映 |
| D-5 | 公式解説書（nablarch-document）との照合。17件の未反映仕様を全反映 |
| E-1, E-2 + 実装例評価 | 実装例リポジトリ評価。複数シート方針を1シート1ファイル分割に確定。`"?"` プレフィックス記法は本リポジトリ外の慣習と確認 |
| C-1 | nablarch-test-data-converter との比較。16件調査・1件（マーカーカラム除外）反映 |

**設計フェーズ成果物（全て完成）**:

| ファイル | 内容 |
|---|---|
| `docs/ntf-testdata-yaml-schema.json` | JSON Schema（第5回レビュー対応済み） |
| `docs/ntf-testdata-yaml-design.md` | 設計解説ドキュメント（第5回レビュー対応済み） |
| `docs/ntf-testdata-yaml-examples.yaml` | 使用例（第5回レビュー対応済み） |
| `docs/ntf-testdata-structure.md` | コード調査報告 |
| `docs/ntf-coverage-class-list.md` | 対象クラス一覧（src/main + src/test 両方） |
| `docs/ntf-coverage-spec-mapping.md` | 仕様マッピング（29クラス全行走査済み） |
| `docs/ntf-yaml-impl-evaluation.md` | 実装例リポジトリ評価レポート |
| `docs/ntf-coverage-doc-check.md` | 公式解説書 × スキーマ 照合チェック（17件反映済み） |
| `docs/ntf-schema-accuracy-basis.md` | スキーマ正確性の根拠資料 |
| `docs/ntf-converter-comparison.md` | nablarch-test-data-converter 比較（16件調査・1件反映済み） |
