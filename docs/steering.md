# NTF テストデータ YAML 実装フェーズ

ブランチ: `convert-testdata-excel-to-text`

## 背景・品質要求

Nablarch は銀行・保険・官公庁等のミッションクリティカルな大規模基幹系システムで使われるフレームワークである。
NTF（Nablarch Testing Framework）はそのテスト基盤であり、NTF 自体のバグが顧客システムの品質を直接損なうリスクがある。

**設計・実装・テスト・レビューのすべてにおいて、ミッションクリティカルな基幹系システムと同等の高品質を要求する。**

具体的には以下を意味する。

- テストは「通った」だけでは不十分。境界値・異常系・仕様の端点を網羅し、意図が明確であること
- レビューは「問題なさそう」ではなく、仕様の全IDに対して根拠を持って充足を確認すること
- QAエンジニアレビューは独立した立場で厳格に実施し、本質的な懸念があれば必ず指摘すること
- 「動く」と「正しい」は別物。正しさを根拠で説明できない実装・テストは完了とみなさない

---

## 目的

YAMLスキーマ設計フェーズ（完了済み）で固めたスキーマを実際に動かす。
目的は2つ。

1. 設計したYAMLスキーマがNTF仕様を満たしていることを検証する
2. YAMLスキーマでNTFを動かす（TDDベース）

「実装した」「テストが通った」だけでは不十分。
「NTF仕様の全IDに対してテストが1対1で対応しており、カバー漏れゼロである」ことを第三者に根拠で説明できる状態を目指す。

---

## 作業ルール（全作業共通）

- **全体整合確認**: ファイルを変更する際はパッチあてに留まらず、ファイル全体を見て不要・矛盾・重複がないか確認してから変更する
- **コミット単位**: ファイルを変更したら目的単位でコミット＆プッシュする
- **プッシュ必須**: ファイルを変更したらコミット後に必ずプッシュする
- **環境変更は事前確認必須**: ライブラリ追加・ツールインストール等、環境に対する変更が必要になった場合はユーザーに確認を取ってから実施する。勝手にインストール・追加しない

---

## タスク定義ルール

新しいタスクを定義・追加する際は以下のフォーマットと要件を守ること。

### タスクフォーマット

```markdown
### {タスクID}: {タスク名}

**目的**: このタスクで何を達成するか、1〜2文で明記する。

**前提**: このタスクを開始するために完了していなければならない前提タスクを列挙する。前提なしの場合は「なし」と記載する。

**作業内容**:
- [ ] 具体的な作業ステップ1
- [ ] 具体的な作業ステップ2
- [ ] ...
- [ ] セルフチェック（チェック結果: `docs/checks/{タスクID}.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] （ソースコード変更のタスクの場合）{対象言語}エンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] （ソースコード変更のタスクの場合）ソフトウエアエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- 完了を客観的に判定できる基準を1件ずつ箇条書きで記載する
- 「〜されていること」「〜が確認できること」など判定可能な表現で書く
- あいまいな表現（「適切に」「正しく」等）は使わない
```

### タスク定義の要件

- **目的を1文で言える粒度にする**: 作業が膨らみそうなら複数タスクに分割する
- **作業ステップは具体的にする**: 「実装する」ではなく「`ClassName` に `methodName()` を実装する」のように書く
- **完了条件は客観的にする**: 第三者が判定できる基準のみ記載する。「理解した」「把握した」は完了条件にしない
- **前提タスクを明記する**: 依存関係が不明だと並行着手の可否が判断できない

---

## タスク完了プロセス（全タスク共通）

各タスクの作業内容の最後に必ず以下のステップを実施する。ソースコード変更を含むタスクは5ステップ、それ以外は3ステップ。

### レビューはサブエージェントで実施する（バイアス排除）

**QAエンジニアレビュー・対象言語エキスパートレビュー・ソフトウエアエンジニアレビューは、いずれもサブエージェント（Agent ツール）を使って実施すること。**

理由: メインエージェントは実装の詳細を把握しているためバイアスがかかりやすい。サブエージェントは会話コンテキストを引き継がず独立した立場でレビューできるため、見落としや甘い判定を防ぐことができる。

サブエージェントへの指示には以下を含めること:
- レビュー対象ファイルのパス一覧
- レビューの役割（QAエンジニア / 対象言語エキスパート / ソフトウエアエンジニア）
- 評価観点（本セクションに記載の観点を全文コピーして渡す）
- 「本質的な指摘がなくなるまで改善→再レビューを繰り返す」旨

### レビュー指摘への対応方針

- **指摘は原則として全件対応すること。** 「軽微」「優先度低」を理由にスキップしない
- 対応しない指摘がある場合は、**ユーザーに確認を取ってから判断すること**。勝手に対応不要と判断しない
- 明らかに誤った指摘（事実誤認・前提が異なる等）の場合のみ、その根拠を明記して対応不要と判断できる

### ソースコード変更タスクにおけるカバレッジ確認

意味のあるテストの網羅性を担当者が確認できるよう、JaCoCo を使ったカバレッジレポートを生成すること。

- `pom.xml` に JaCoCo の設定がない場合は、ユーザーに追加可否を確認してから設定する
- `mvn test` 実行後に `target/site/jacoco/index.html` を確認し、行カバレッジ・分岐カバレッジの未達箇所をチェックする
- カバレッジ未達箇所はテスト追加の検討対象として担当者セルフチェックに記録する

### 全タスク共通（3ステップ）

1. **担当者セルフチェック**: 完了条件を1件ずつ確認し、判定（OK/NG）と根拠を記録する
2. **QAエンジニアレビュー**（サブエージェントで実施）: QAエキスパートとして以下の観点を網羅的に評価し、改善案を出す。本質的なFBがなくなるまで修正→レビューを繰り返す
   - 目的に対して意味のあるテストまたは動作確認が実施されているか？（テストが「通った」だけでなく、仕様の意図を検証しているか）
   - エッジケース（境界値・異常系・空入力・最大値・型変換の端点等）が漏れなくテストまたは動作確認されているか？
3. **ユーザーレビュー**: 担当者・QA両方がパスした後にユーザーへ確認依頼する。OKが出るまで改善を繰り返す

### ソースコード変更を含むタスク（5ステップ）

上記3ステップの2と3の間に以下を実施する（合計5ステップ）。

1. **担当者セルフチェック**（同上）
2. **QAエンジニアレビュー**（同上・サブエージェントで実施）
3. **対象言語エキスパートレビュー**（サブエージェントで実施）: 対象プログラミング言語のエキスパートとして以下の観点を網羅的に評価し、改善案を出す。本質的なFBがなくなるまで修正→レビューを繰り返す
   - ベストプラクティスに従って設計・実装できているか？（命名・例外処理・nullの扱い・スレッドセーフ性等、言語固有の慣例）
   - 同じリポジトリ内の他のソースコード・テストコードとコードの書き方を合わせているか？（Javadoc・`@Override`・型引数・アクセス修飾子等）
   - テストコードはGWT（Given/When/Then）形式でテスト内容が分かるようになっているか？
4. **ソフトウエアエンジニアレビュー**（サブエージェントで実施）: ソフトウエアエンジニアとして以下の観点を網羅的に評価し、改善案を出す。本質的なFBがなくなるまで修正→レビューを繰り返す
   - 設計の責務分離が適切か？（1クラス・1メソッドの責務が明確か）
   - 変更がシステム全体の整合性を壊していないか？（インタフェース契約・既存APIとの互換性）
   - 保守性・拡張性の観点で問題のある実装パターンがないか？（重複・深いネスト・マジックナンバー等）
5. **ユーザーレビュー**（同上）

チェック結果は `docs/checks/{タスクID}.md` に出力する。

### チェックファイルフォーマット

```markdown
# {タスクID} 完了条件チェック

## 完了条件チェックリスト

| 完了条件 | 担当者判定 | 担当者根拠 | QA判定 | QA根拠 |
|---|---|---|---|---|
| （完了条件の文章） | OK / NG | （確認した内容・証跡） | OK / NG | （QAが確認した内容・懸念点） |

## QAエンジニアレビュー

| 観点 | 判定 | 根拠・改善案 |
|---|---|---|
| 目的に対して意味のあるテスト・動作確認が実施されているか | OK / NG | |
| エッジケースが漏れなくテスト・動作確認されているか | OK / NG | |

## エキスパートレビュー（ソースコード変更タスクのみ）

### 対象言語エキスパートレビュー

| 観点 | 判定 | 根拠・改善案 |
|---|---|---|
| ベストプラクティス準拠 | OK / NG | |
| 既存コードスタイル統一 | OK / NG | |
| テストコードのGWT形式 | OK / NG | |

### ソフトウエアエンジニアレビュー

| 観点 | 判定 | 根拠・改善案 |
|---|---|---|
| 責務分離の適切さ | OK / NG | |
| システム全体の整合性 | OK / NG | |
| 保守性・拡張性 | OK / NG | |

## 総合判定

- 担当者: OK / NG
- QA: OK / NG
- 対象言語エキスパート: OK / NG / 該当なし（ソースコード変更なし）
- ソフトウエアエンジニア: OK / NG / 該当なし（ソースコード変更なし）
- ユーザーレビュー可否: 可 / 不可（理由）
```

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
- [x] `docs/ntf-coverage-spec-mapping.md` の仕様ID（DT-xx, SS-xx, RS-xx, HC-xx, IV-xx, DR-xx, MS-xx）を全件棚卸し
- [x] 調査で判明したギャップ E-1〜E-9 について、仕様IDとして昇格するか否かを判断し文書に明記する。昇格しない場合は除外理由を記載する
  - E-1: YAML ネイティブ型→文字列化の変換漏れリスク
  - E-2: 末尾空要素の扱い（Excel は null→"" 補完、YAML は末尾省略されやすい）
  - E-3: `readLine() == null` 終了判定タイミングのずれによる最終セクションデータ欠落リスク
  - E-4: `startsWith` 前方一致マッチングの挙動（YAML schema validation とは独立）
  - E-5: sendSyncTestData のディレクトリ配置規則はYAMLスキーマ外
  - E-6: `defaultDirectives` のDI設定は SystemRepository XML の問題でありYAMLファイルとは独立
  - E-7: `EXPECTED_REQUEST_HEADER/BODY_MESSAGES` の行数一致チェックはランタイムのみ
  - E-8: `BasicDefaultValues` の DATE カラムのTZハザード（JSTとUTCで値が変わる）
  - E-9: `BasicJapaneseCharacterInterpreter` の「スルー vs 例外」条件の誤記（design.md D-6）
- [x] 仕様を2つに分類する（テストデータ構造 / 実装内部ロジック）
- [x] 出力: `docs/ntf-impl-spec-list.md`（仕様ID / 概要 / 分類 の3列）
- [x] セルフチェック（チェック結果: `docs/checks/I-1.md`）
- [x] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [x] ユーザーレビュー依頼・OK取得

**完了条件**:
- 全仕様IDに分類が付いていること
- E-1〜E-9 について「仕様IDとして昇格」または「除外・理由付き」がそれぞれ記載されていること
- 抜け漏れがないことを確認した旨が記載されていること

---

### I-2: 仕様ID × 既存テストメソッドのマッピング

**目的**: 既存テストのどのメソッドがどの仕様IDを検証しているかを明示し、カバーゼロの仕様IDを特定する。

**前提**: I-1 完了

**作業内容**:
- [x] I-1 の仕様ID一覧に対して、以下のテストクラスのテストメソッドをマッピングする
  - `BasicTestDataParserTest`（16メソッド確認済み）
  - `MessageParserTest`
  - `FileSupportTest`
  - `SendSyncMessageParserTest`（現状17行のみ、MS-04〜MS-07 は実質未テスト）
  - reader/ パッケージのその他テストクラス
- [x] マッピングされない仕様ID（カバーゼロ）を「テスト追加必要」として明記する
  - D-14（複数レコードレイアウトの連続記述）: `BasicTestDataParserTest` に専用テストなし
  - MS-04〜MS-07（errorMode/NO列/グループメッセージ）: `SendSyncMessageParserTest` が17行しかない
- [x] 出力: `docs/ntf-impl-spec-list.md` に列「既存テストメソッド or テスト追加必要」を追加
- [x] セルフチェック（チェック結果: `docs/checks/I-2.md`）
- [x] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [x] ユーザーレビュー依頼・OK取得

**完了条件**: 全仕様IDに「対応テストメソッド名」または「テスト追加必要（理由付き）」が記載されること。

---

### I-3: 仕様ID × YAMLスキーマ記述のマッピング

**目的**: YAMLスキーマのどのキー/定義が、どの仕様IDを表現しているかを明示する。

**前提**: I-1 完了

**作業内容**:
- [x] I-1 の**全仕様ID**（分類問わず）に対して以下のいずれかを記載する
  - 「テストデータ構造」分類: `ntf-testdata-yaml-schema.json` / `ntf-testdata-yaml-design.md` のどのセクション/キーが対応するかを記載
  - 「実装内部ロジック」分類: 「スキーマ外・パーサ実装で担保」と明記
  - スキーマで表現できない仕様（E-4の前方一致、E-5の配置規則、E-7の行数一致チェック等）: 「スキーマ外仕様・テストで担保する方針」と明記し、後続 R-3 でテスト作成することを記載
- [x] 出力: `docs/ntf-impl-spec-list.md` に列「スキーマ根拠 or スキーマ外理由」を追加
- [x] セルフチェック（チェック結果: `docs/checks/I-3.md`）
- [x] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [x] ユーザーレビュー依頼・OK取得

**完了条件**: 全仕様IDに対して「スキーマ根拠箇所」または「スキーマ外理由」が記載されること（分類を問わず全件）。

---

## Ph-2: YAMLリーダー実装（TDDベース）

**前提**: Ph-1（I-1/I-2/I-3）全完了

### C-1: JaCoCo カバレッジレポート設定

**目的**: `mvn test` 実行時に行・分岐カバレッジの HTML レポートが生成されるようにし、担当者がテストの網羅性をローカルで確認できるようにする。

**前提**: なし（他タスクと独立して実施可能）

**作業内容**:
- [ ] `pom.xml` に JaCoCo Maven プラグインを追加する（`prepare-agent` + `report` ゴール）
- [ ] `mvn test` 実行後に `target/site/jacoco/index.html` が生成されることを確認する
- [ ] `YamlTestDataReader` および `yaml` パッケージの行カバレッジ・分岐カバレッジを確認し、未達箇所を記録する
- [ ] セルフチェック（チェック結果: `docs/checks/C-1.md`）
- [ ] QAエンジニアレビュー（サブエージェントで実施）
- [ ] Javaエキスパートレビュー（サブエージェントで実施）
- [ ] ソフトウエアエンジニアレビュー（サブエージェントで実施）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `mvn test` 実行後に `target/site/jacoco/index.html` が生成されること
- `YamlTestDataReader` および `nablarch.test.core.reader.yaml` パッケージの行カバレッジ・分岐カバレッジが HTML レポートで確認できること
- カバレッジ未達の行・分岐が存在する場合、その箇所と理由が `docs/checks/C-1.md` に記録されていること

---

### R-1: `TestDataReader` インタフェースの YAML実装クラス作成

**目的**: `PoiXlsReader` と同一インタフェースで YAML を読む `YamlTestDataReader` を実装する。

**作業内容**:
- [x] `TestDataReader` インタフェースを実装
- [x] `open(path, dataName)` の呼び出し規約を実装: `dataName` = `"ファイル名（拡張子なし）"` → `{dataName}.yaml` を探す
- [x] `readLine()` の返却仕様を実装（全てExcelの挙動に合わせる）
  - YAML ネイティブ `null` → 文字列 `"null"` として返す（E-1）
  - YAML ネイティブ boolean (`true`/`false`) → 文字列 `"true"/"false"` として返す（E-1）
  - YAML ネイティブ integer/float → 数字文字列として返す（E-1）
  - 末尾空要素は `""` として補完する（E-2）
  - 文書終端で `null` を返す。`null` を返した直前のセクションデータが欠落しないことを保証する（E-3）
- [x] `isDataExisting` / `isResourceExisting` を実装
- [x] TDD: テストクラス `YamlTestDataReaderTest` を先に書いてから実装する
- [x] **テスト実行・グリーン確認**
- [x] セルフチェック（チェック結果: `docs/checks/R-1.md`）
- [x] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [x] Javaエキスパートレビュー（既存スタイル準拠・ベストプラクティス確認）
- [x] テストコードレビュー（GWT構造・仕様IDリンク・エッジケース網羅）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `YamlTestDataReaderTest` が全グリーン
- YAML ネイティブ型の文字列化（E-1）の境界値テスト（null/true/false/integer/float各型、科学表記を含む）が含まれること
- 末尾空要素補完（E-2）のテストが含まれること（末尾省略・中間省略の両ケース）
- `readLine()` が `null` を返した後、直前のセクションデータが欠落しないことを検証するテストが含まれること（E-3）（具体的な値でアサートすること）
- 実装コードが既存コード（`PoiXlsReader` 等）のスタイルに準拠していること（Javadoc・`@Override`・型引数等）
- テストコードに GWT（Given/When/Then）コメントが記載されていること
- テストコードのコメントに仕様ID（RS-xx）と参照先（`docs/ntf-impl-spec-list.md`）が明記されていること
- Javaエキスパートによるレビューが完了し、本質的な指摘がなくなっていること

---

### R-2: 既存テスト（BasicTestDataParserTest）のYAMLリーダー版作成

**目的**: 既存のExcelベーステストと同一結果をYAMLリーダーで再現し、「ExcelとYAMLが等価である」ことを証明する。

**前提**: R-1 完了

**作業内容**:
- [ ] `BasicTestDataParserTest.xls` の内容を YAML に変換し `BasicTestDataParserTest.yaml` として配置
- [ ] `BasicTestDataParserTestYaml` を作成し、`TestDataParser` に `YamlTestDataReader` を差し込んで同一アサーションを実行
- [ ] 既存16テストメソッド全件をYAML版で実行し、差異がある場合は原因を文書に明記する
- [ ] セルフチェック（チェック結果: `docs/checks/R-2.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `BasicTestDataParserTestYaml` の16メソッド全グリーン
- `BasicTestDataParserTest`（Excel版）と `BasicTestDataParserTestYaml`（YAML版）の対応するメソッドが、同一入力データ・同一アサーション内容でグリーンになること
- 差異が生じた場合は原因を文書に明記すること（差異の存在自体は許容するが、隠蔽は不可）

---

### R-3: カバーゼロ仕様の新規テスト作成

**目的**: I-2 で「テスト追加必要」とされた27件の仕様IDに対してテストを作成し、カバーゼロを解消する。

**前提**: R-1 完了、I-2/I-3 完了

**テスト追加対象一覧**（I-2 確定・27件）:

| 仕様ID | 概要 | テスト追加方針 |
|---|---|---|
| DT-03 | DataType 前方一致（`startsWith`）判定 | `DataType#getType()` の前方一致動作を直接検証するテストを追加（`DataTypeTest` または新クラス） |
| DT-07 | RESPONSE_HEADER/BODY_MESSAGES の GroupData 経路 | `GroupMessageParser` 経由の GroupData 取得をテスト |
| SS-04 | SETUP_TABLE 主キーカラム省略不可 | 主キー省略時に INSERT が失敗または意図しないデフォルト値になることを検証 |
| SS-05 | EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE の混在 | 同一ファイル内で混在させた場合に後半データが欠落することを検証 |
| SS-11 | 複数レコードレイアウト連続記述（旧D-14） | `DataFileParser` に複数 record fragment を持つ YAML テストデータでシナリオテストを追加 |
| SS-19 | `testShots` LIST_MAP 予約ID | バッチリクエスト単体テストで `testShots` が自動読み込みされることを検証 |
| HC-06 | 行内コメント（先頭以外の `//` 以降切り捨て） | 行内コメントが正しく切り捨てられることを `TestDataParsingTemplate` で検証 |
| HC-07 | 空行スキップ | 全要素 null/空文字の行がスキップされることを検証 |
| IV-03 | `DateTimeInterpreter` 完全一致制約 | `${systemTime}` 等の完全一致のみ変換され部分文字列は変換されないことを検証（独立テストクラス作成） |
| IV-09 | 日付型カラム記述形式の境界値 | `yyyyMMddHHmmssSSS`（17文字）・後置0埋め・JDBC エスケープ形式の各パターンを `TableData` で検証 |
| IV-10 | Timestamp 型期待値の末尾 `.0` 必須 | `.0` がない期待値と `.0` がある期待値の比較挙動を検証 |
| IV-11 | バイナリデータの `0x` プレフィクス記法 | `0x` 付き16進数と `0x` なし文字列の扱いの違いを検証 |
| IV-15 | X9/SX9 型フィールドの実値記述 | パディング文字・符号を含む実値で固定長フィールドが正しく読み書きされることを検証 |
| DR-03 | 可変長ディレクティブキー制限 | 無効なディレクティブキーで例外が発生することを `VariableLengthFileParser` で検証 |
| DR-04 | `defaultDirectives` DI（旧E-6） | SystemRepository の `defaultDirectives` キーで設定したディレクティブが YAML テストデータに適用されることを検証。XML設定の正しさはテスト対象外と明記 |
| DR-05 | `fixedLengthDirectives` DI | 固定長専用デフォルトディレクティブの YAML 適用を検証 |
| DR-06 | `variableLengthDirectives` DI | 可変長専用デフォルトディレクティブの YAML 適用を検証 |
| MS-04 | `errorMode:timeout`/`msgException` 特殊値 | `SendSyncMessageParser` に対して YAML テストデータで errorMode 特殊値のパースを検証 |
| MS-05 | HEADER/BODY MESSAGES 行数一致必須（旧E-7） | 行数不一致時に `IllegalStateException` が発生することを YAML テストデータで検証 |
| MS-06 | `GroupMessageParser` 複数メッセージ収集 | 同一 groupId の複数メッセージプール収集を YAML テストデータで検証 |
| MS-07 | `sendSyncTestData` 配置規則（旧E-5） | `sendSyncTestData/{requestId}/message` の配置規則が YAML でも機能することを検証 |
| MS-08 | ステータスコード列なし時のデフォルト "200" | ステータスコード列が存在しない YAML テストデータで "200" が使われることを検証 |
| MS-09 | マルチレコード送信の行数一致 | N回送信で各 N 行記述する規約を YAML テストデータで検証 |
| MS-10 | no 値による複数回送信順序 | `no` 値を変えた連続記述で複数回送信が正しく動作することを検証 |
| MS-11 | HTTP同期応答ボディ行長制約 | `response_body_messages` の各行長が同一であることを YAML テストデータで検証 |
| MS-12 | フォーマット定義ファイル命名規則 | `{requestId}_RECEIVE` / `{requestId}_SEND` 命名で正しく解決されることを検証 |
| MS-13 | `messaging.assertAsMapFileType` キー切り替え | SystemRepository 設定値に応じてアサート方式が切り替わることを検証 |

**作業手順**:
- [ ] 上記27件を対象テストクラス別に整理し、既存テストクラスへの追加か新規クラス作成かを決定する
- [ ] 各テストを YAML テストデータを使う形式で実装する（R-1 完了後に着手）
- [ ] SS-18（DATE型TZハザード・旧E-8）: `EXPECTED_COMPLETE_TABLE` の DATE カラムデフォルト値が CI 環境 TZ で動作することを確認。TZ依存が解消できない場合は制約事項として SS-18 の注記と D-1 に明記する
- [ ] セルフチェック（チェック結果: `docs/checks/R-3.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- 上記27件すべてに対応するテストが全グリーン
- SS-18（TZハザード）について「TZ依存解消済み」または「制約事項として D-1 に記載済み」のいずれかが確認できること

---

## Ph-3: 既存ExcelテストのYAML版並走と差分ゼロ確認

**前提**: R-1 完了

### V-1: 全Excelテストファイルの YAML変換と並走実行

**目的**: リポジトリ内の全59 Excelファイルに対してYAML版を作成し、ExcelリーダーとYAMLリーダーの等価性を確認する。

**作業内容**:
- [ ] 変換方針を決定する: `nablarch-test-data-converter` を使用するか手動変換するかを明記する
- [ ] 全59の `.xls`/`.xlsx` ファイルを `.yaml` に変換する
- [ ] 各テストクラスに YAML版テストを作成する（またはリーダーを差し替えて実行する方式でも可）
- [ ] 差分が生じた場合の対処方針を明記する: 修正して差分解消するのか、除外して理由を記録するのか
- [ ] セルフチェック（チェック結果: `docs/checks/V-1.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- 全テストが Excel/YAML どちらでも同一結果でグリーンであること
- 差分が生じたファイルがある場合、ファイル名・差分内容・原因・対処（修正 or 除外理由）を一覧で記録すること

---

## Ph-4: 仕様カバレッジ根拠文書

**前提**: Ph-2/Ph-3 完了

### D-1: カバレッジマトリクスの完成

**目的**: 「YAMLスキーマがNTF仕様を100%カバーする」ことを第三者に説明できる根拠ドキュメントを完成させる。

**作業内容**:
- [ ] `docs/ntf-impl-spec-list.md`（Ph-1 で作成）に以下の列を追加して完成させる
  - 仕様ID / 概要 / 分類 / スキーマ根拠 or スキーマ外理由 / 既存テストメソッド / 追加テストメソッド / カバー状況
- [ ] E-6/E-8 について「TZ依存」「DI設定はXML外」の制約事項欄を設ける
- [ ] 出力: `docs/ntf-impl-coverage-matrix.md`
- [ ] セルフチェック（チェック結果: `docs/checks/D-1.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件（主完了条件）**: 全仕様IDのカバー状況が「済」であること。
**完了条件（許容除外）**: 意図的に除外した仕様IDがある場合は除外理由が明記されていること。
「除外理由なし・カバー状況空欄」は完了とみなさない。

---

## 現在の状態（2026-05-21時点）

- **ブランチ**: `convert-testdata-excel-to-text`（ローカル・リモートともにクリーン）
- **完了済みフェーズ**: スキーマ設計フェーズ全完了、Ph-1 I-1/I-2/I-3 完了
- **進行中フェーズ**: Ph-2 R-1 エキスパートレビュー指摘対応中
- **次の着手**: R-1 の指摘を全件対応 → 再レビュー → ユーザーレビュー OK → C-1 と R-2/R-3 を並行着手
- **未着手タスク**: C-1（JaCoCo設定・他タスクと並行可）、R-2/R-3（並行可） → V-1 → D-1

### 環境情報

- **Java**: Eclipse Temurin 17（`update-alternatives` で切り替え済み）
- **Maven settings**: `~/.m2/settings.xml` に社内 Nexus リポジトリ設定済み（`nablarch-parent:6-NEXT-SNAPSHOT` 解決済み）
- **ビルド確認**: `mvn test -Dtest="YamlTestDataReaderTest,YamlValueConverterTest,RecordRowBuilderTest,TableSectionConverterTest,ListMapSectionConverterTest,FileSectionConverterTest,MessageSectionConverterTest,GroupMessageSectionConverterTest,YamlRowBuilderTest"` で63件グリーン確認済み

### Ph-1 完了状況

**I-1:**
- **成果物**: `docs/ntf-impl-spec-list.md`（仕様ID 80件: DT-01〜DT-07 / SS-01〜SS-20 / RS-01〜RS-08 / HC-01〜HC-07 / IV-01〜IV-15 / DR-01〜DR-10 / MS-01〜MS-13）
- **チェック結果**: `docs/checks/I-1.md`（担当者 OK・QA OK・ユーザーレビュー OK）

**I-2:**
- **成果物**: `docs/ntf-impl-spec-list.md` に列「既存テストメソッド or テスト追加必要」追加（80件全件）
- 既存テストあり 45件 / テスト追加必要 35件（RS 全8件は YamlTestDataReader 未実装として記録済み）
- **チェック結果**: `docs/checks/I-2.md`（担当者 OK・QA OK・ユーザーレビュー OK）

**I-3:**
- **成果物**: `docs/ntf-impl-spec-list.md` に列「スキーマ根拠 or スキーマ外理由」追加（80件全件）
- スキーマ根拠あり 43件 / スキーマ外 37件
- **チェック結果**: `docs/checks/I-3.md`（担当者 OK・QA OK・ユーザーレビュー OK）

### Ph-2 R-1 状況（エキスパートレビュー指摘対応中）

**成果物:**
- `src/main/java/nablarch/test/core/reader/YamlTestDataReader.java`（ファイルI/O・委譲のみ）
- `src/main/java/nablarch/test/core/reader/yaml/` パッケージ（10クラス）:
  - `YamlRowBuilder`（public）、`SectionConverter`（interface）、`TableSectionConverter`、`ListMapSectionConverter`、`FileSectionConverter`、`MessageSectionConverter`、`GroupMessageSectionConverter`、`RecordRowBuilder`、`YamlValueConverter`、`package-info`
- `src/test/java/nablarch/test/core/reader/YamlTestDataReaderTest.java`（17件・RS-01〜RS-08 全網羅）
- `src/test/java/nablarch/test/core/reader/yaml/` テスト8クラス（46件・各クラス単体検証）
- テストデータ YAML 3件（`YamlTestDataReaderTestData.yaml`・`YamlNativeTypesTestData.yaml`・`YamlTrailingNullTestData.yaml`）
- **チェック結果**: `docs/checks/R-1.md`（担当者 OK・旧レビュー OK・エキスパートレビュー指摘対応待ち）

**ADR（設計判断記録）:**
- `docs/adrs/ADR-001-yaml-library.md`: SnakeYAML 2.6 採用の根拠
- `docs/adrs/ADR-002-yaml-dependency-scope.md`: compile スコープ採用の根拠

**エキスパートレビュー指摘一覧（全件対応が必要。対応不要な場合はユーザー確認を取ること）:**

| 指摘ID | 区分 | 概要 | 対応状況 |
|---|---|---|---|
| QA-1 | QA | `open_loadsYamlFile` のアサートが弱い（`notNullValue()` のみ） | 未対応 |
| QA-2 | QA | 科学表記の文字列表現がJVM実装依存・SnakeYAML 経由の境界テストが統合テストのみ | 未対応 |
| QA-3 | QA | `close()` 後の `readLine()` が `null` を返すことのテストがない | 未対応 |
| QA-4 | QA | `open()` 再呼び出し（再オープン）の動作テストがない | 未対応 |
| QA-5 | QA | `readLine_lastSectionNotLost` が具体値に依存しすぎで保守性が低い | 未対応 |
| QA-6 | QA | `YamlRowBuilderTest` の LIST_MAP 順序アサートが弱い | 未対応 |
| QA-7 | QA | `TableSectionConverter` に null 値を含む行の変換テストがない | 未対応 |
| QA-8 | QA | `FileSectionConverter` の `expected/fixed` 組み合わせテストがない | 未対応 |
| QA-9 | QA | `FileSectionConverter` の `setup/variable` 組み合わせテストがない | 未対応 |
| QA-10 | QA | `RecordRowBuilder` でフィールド0件のエッジケーステストがない | 未対応 |
| QA-11 | QA | `YamlValueConverter.asMapList()` のテストが存在しない | 未対応 |
| QA-12 | QA | `open()` の `path=null` ケースのテストがない | 未対応 |
| QA-13 | QA | `isDataExisting` のテストが `isResourceExisting` と非対称（存在しないディレクトリケース漏れ） | 未対応 |
| QA-14 | QA | `ListMapSectionConverter` に null 値を含む行の変換テストがない | 未対応 |
| QA-15 | QA | `GroupMessageSectionConverter` のテストで型行・長さ行のアサートが不完全 | 未対応 |
| QA-16 | QA | `YamlRowBuilderTest` に setup_files / messages 等6セクションの確認テストがない | 未対応 |
| JAVA-1 | Java | `YamlRowBuilder` の空コンストラクタが不要 | 未対応 |
| JAVA-2 | Java | `new ArrayList<>(Arrays.asList(...))` の二重ラップが不要 | 未対応 |
| JAVA-3 | Java | 型パラメータ明示（`new ArrayList<String>()` 等）がやや古い（プロジェクト方針次第） | 未対応 |
| JAVA-4 | Java | `singletonRow` メソッドが5クラスに重複定義 | 未対応 |
| JAVA-5 | Java | `FileSectionConverter` のフィールド名 `yamlKey` が役割と不一致 | 未対応 |
| JAVA-6 | Java | `open()` の `path` パラメータに null チェックがない | 未対応 |
| JAVA-7 | Java | Javadoc スタイルが既存クラスと不統一（`<br/>` の有無） | 未対応 |
| JAVA-8 | Java | テストデータのパスが `src/test/java` 直下でリソースとコードが混在 | 未対応 |
| JAVA-9 | Java | テストでマジックナンバーによる行スキップ（`for (int i = 0; i < 10; i++)`） | 未対応 |
| JAVA-10 | Java | `toCell_isMissing_returnsEmpty` で1メソッドに複数アサートが混在 | 未対応 |
| SWE-1 | SWE | `isResourceExisting` と `isDataExisting` の実装が同一なのに共通化されていない | 未対応 |
| SWE-2 | SWE | `singletonRow` が5クラスに重複コピー（JAVA-4 と同内容） | 未対応 |
| SWE-3 | SWE | `TableSectionConverter` と `ListMapSectionConverter` のデータ行生成ロジックが重複 | 未対応 |
| SWE-4 | SWE | `FileSectionConverter` のコンストラクタ設計が他コンバータと不統一 | 未対応 |
| SWE-5 | SWE | `open()` 重複呼び出し時の挙動が Javadoc に未記載 | 未対応 |
| SWE-6 | SWE | `SECTION_ENTRIES` が static フィールドでコンバータをシングルトン共有（将来の状態追加時のリスク） | 未対応 |
| SWE-7 | SWE | `asString` と `toCell` の null 扱いが非対称・`RecordRowBuilder` で null 混入リスク | 未対応 |
| SWE-8 | SWE | `YamlRowBuilder` だけ `public` で Javadoc と矛盾 | 未対応 |

### 再開手順

1. `git checkout convert-testdata-excel-to-text` でブランチを確認
2. `git status` でクリーンであることを確認
3. R-1 の指摘を上表に従って全件対応する（対応不要と判断する場合はユーザー確認を取ること）
4. 対応完了後にサブエージェントで再レビューを実施し、本質的な指摘がなくなったらユーザーレビューを依頼する
5. ユーザーレビュー OK → C-1（JaCoCo設定）と R-2/R-3 を並行着手

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
