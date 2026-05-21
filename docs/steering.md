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

### I-4: 異常系仕様の列挙と三角マッピングへの追加（I-1 やり直し）

**目的**: I-1 で正常系仕様のみを列挙し異常系（必須フィールド欠如・型誤り等の入力不正時の挙動）が抜けていたことが R-1-refactor の実装レビューで判明した。異常系仕様を `ntf-impl-spec-list.md` に追加し、I-2/I-3 のマッピングも完成させる。

**背景**:
R-1-refactor で「`table` キー欠如時に例外スロー」「`path` キー欠如時に例外スロー」「FW_HEADER rows 型誤り時に例外スロー」のテストを追加したが、これらに対応する仕様IDが `ntf-impl-spec-list.md` に存在しなかった。原因は I-1 が「パーサが正常に動作するときの仕様」しか列挙しておらず、「パーサが不正入力を受けたときに何をすべきか」という異常系仕様を仕様IDとして認識していなかったこと。

**前提**: I-1/I-2/I-3 完了（ただし本タスクはその不完全さを修正する）

**作業内容**:
- [ ] `YamlTestDataParser` および `reader.yaml` パッケージ全クラスのソースコードを走査し、異常系の挙動（例外スロー・null 返却・空リスト返却等）を全件列挙する
- [ ] 列挙した異常系挙動を仕様IDとして `ntf-impl-spec-list.md` に追加する（プレフィクスは RS-xx に続番、または新カテゴリとして判断する）
- [ ] 追加した仕様IDに対して I-2 相当（対応テストメソッド）・I-3 相当（スキーマ根拠またはスキーマ外理由）を記載する
- [ ] R-1-refactor で追加した既存テスト（table欠如・path欠如・FW_HEADER rows 型誤り・rows 空の各テスト）を追加仕様IDに対応づける
- [ ] セルフチェック（チェック結果: `docs/checks/I-4.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `ntf-impl-spec-list.md` の全仕様IDに異常系を含めた仕様が網羅されていること
- R-1-refactor で追加した全テストメソッドが、いずれかの仕様IDに対応づけられていること
- 「仕様IDのないテスト」が存在しないこと

---

## Ph-2: YAMLパーサー実装（TDDベース）

**前提**: Ph-1（I-1/I-2/I-3）全完了

### 実装方針（確定）

```
YAML → YamlTestDataParser（BasicTestDataParser を継承）→ TableData / DataFile / MessagePool
```

- `BasicTestDataParser` を継承し、getter を YAML から直接オーバーライドする
- `List<List<String>>` 中間フォーマットは使わない
- 公開API（`TestDataParser` インタフェース・`SendSyncSupport` 等）の変更は不要
- SnakeYAML は `pom.xml` に追加する（ADR-001/002 参照）

**根拠**:
- `TestDataParser` インタフェースは `@Published(tag="architect")` のため変更不可
- `SendSyncSupport` / `RequestTestingSendSyncSupport` が `BasicTestDataParser` 型に直接依存しているため、`implements TestDataParser` の独立実装への差し替えは不可（キャスト失敗）
- `BasicTestDataParser` は `public class`・`final` なし → 継承可能
- `extends BasicTestDataParser` のサブクラスであれば既存のキャストがすべて通る

---

### R-1: `YamlTestDataParser` 実装（`BasicTestDataParser` 継承）

**目的**: `BasicTestDataParser` を継承し、getter を YAML から直接オーバーライドする `YamlTestDataParser` を TDD で実装する。

**前提**: Ph-1 完了

**作業内容**:
- [ ] TDD: `YamlTestDataParserTest` を先に書いてから実装する（仕様ID RS-01〜RS-08 を網羅）
- [ ] `YamlTestDataParser extends BasicTestDataParser` を実装する
  - `getSetupTableData` / `getExpectedTableData` / `getListMap` / `getSetupFile` / `getExpectedFile` / `getMessage` / `getMessageWithoutCache` / `getSendSyncMessage` / `isResourceExisting` を `@Override`
  - `setTestDataReader` は `UnsupportedOperationException` で実装（YAML実装は `TestDataReader` を使わない）
  - `setDbInfo` / `setInterpreters` / `setDefaultValues` は `super` に委譲
  - SnakeYAML によるパース・キャッシュは `YamlTestDataParser` 内に閉じ込める
  - interpreter チェーン（`setInterpreters` で注入）を各 getter 内で値ごとに適用する
- [ ] `pom.xml` に SnakeYAML 依存を追加する
- [ ] **テスト実行・グリーン確認**
- [ ] セルフチェック（チェック結果: `docs/checks/R-1.md`）
- [ ] QAエンジニアレビュー（サブエージェントで実施）
- [ ] Javaエキスパートレビュー（サブエージェントで実施）
- [ ] ソフトウエアエンジニアレビュー（サブエージェントで実施）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `YamlTestDataParserTest` が全グリーン（RS-01〜RS-08 全網羅）
- `setTestDataReader` 呼び出し時に `UnsupportedOperationException` がスローされること
- DI 設定で `class="nablarch.test.core.reader.YamlTestDataParser"` に差し替えたとき `SendSyncSupport` / `RequestTestingSendSyncSupport` のキャストが通ること
- 実装コードが既存コードのスタイルに準拠していること（Javadoc・`@Override`・型引数等）
- テストコードに GWT（Given/When/Then）コメントと仕様ID（RS-xx）参照が記載されていること

---

### R-1-refactor: `YamlTestDataParser` のクラス分割リファクタリング（TDDベース）

**目的**: 828行のファットクラスを責務ごとに分割し、保守性・可読性・テスト網羅性の判断容易性を向上させる。

**前提**: R-1 完了（ユーザーレビュー OK 取得後に着手）

**設計方針**:

```
nablarch.test.core.reader
  └─ YamlTestDataParser（公開API・委譲のみ）

nablarch.test.core.reader.yaml（パッケージプライベート）
  ├─ YamlLoader          … YAMLロード・キャッシュ管理
  ├─ YamlTableDataBuilder … TableData 構築（setup_tables / expected_tables）
  ├─ YamlFileBuilder     … DataFile / Fragment 構築（setup_files / expected_files）
  ├─ YamlMessageBuilder  … MessagePool 構築（messages / *_messages）
  └─ YamlSection         … セクションキー定数・共通ヘルパー（getList / castMap 等）
```

- `YamlTestDataParser` は `reader` パッケージに残し `BasicTestDataParser` 継承を維持する（キャスト互換性のため）
- 各ビルダーはパッケージプライベート（外部APIは変えない）
- `util/interpreter/`・`util/generator/` と同様の慣例でサブパッケージに閉じ込める

**作業内容**:
- [ ] TDD: 各ビルダークラスのテストを先に書いてから実装する
  - `YamlLoaderTest` → `YamlLoader` 実装
  - `YamlTableDataBuilderTest` → `YamlTableDataBuilder` 実装
  - `YamlFileBuilderTest` → `YamlFileBuilder` 実装
  - `YamlMessageBuilderTest` → `YamlMessageBuilder` 実装
- [ ] `YamlTestDataParser` を各ビルダーへの委譲のみに書き換える
- [ ] `YamlTestDataParserTest`（既存37テスト）が引き続き全グリーンであることを確認する
- [ ] セルフチェック（チェック結果: `docs/checks/R-1-refactor.md`）
- [ ] QAエンジニアレビュー（サブエージェントで実施）
- [ ] Javaエキスパートレビュー（サブエージェントで実施）
- [ ] ソフトウエアエンジニアレビュー（サブエージェントで実施）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `YamlTestDataParser` の行数が 200行以内であること（委譲コードのみ）
- 各ビルダークラスが単一責務であること（1クラスの行数が 200行以内を目安）
- `YamlTestDataParserTest` の既存37テストが全グリーンであること
- 各ビルダーの単体テストが存在し、仕様IDとの対応が明確であること
- 既存の公開API（`getSetupTableData` 等）のシグネチャが変わっていないこと

---

### C-1: JaCoCo カバレッジレポート設定

**目的**: `mvn test` 実行時に行・分岐カバレッジの HTML レポートが生成されるようにし、担当者がテストの網羅性をローカルで確認できるようにする。

**前提**: なし（他タスクと独立して実施可能）

**作業内容**:
- [ ] `pom.xml` に JaCoCo Maven プラグインを追加する（`prepare-agent` + `report` ゴール）
- [ ] `mvn test` 実行後に `target/site/jacoco/index.html` が生成されることを確認する
- [ ] `YamlTestDataParser` の行カバレッジ・分岐カバレッジを確認し、未達箇所を記録する
- [ ] セルフチェック（チェック結果: `docs/checks/C-1.md`）
- [ ] QAエンジニアレビュー（サブエージェントで実施）
- [ ] Javaエキスパートレビュー（サブエージェントで実施）
- [ ] ソフトウエアエンジニアレビュー（サブエージェントで実施）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- `mvn test` 実行後に `target/site/jacoco/index.html` が生成されること
- `YamlTestDataParser` の行カバレッジ・分岐カバレッジが HTML レポートで確認できること
- カバレッジ未達の行・分岐が存在する場合、その箇所と理由が `docs/checks/C-1.md` に記録されていること

---

### R-2: 既存テスト（BasicTestDataParserTest）のYAML版作成

**目的**: 既存の Excel ベーステストと同一結果を `YamlTestDataParser` で再現し、「Excel と YAML が等価である」ことを証明する。

**前提**: R-1 完了

**作業内容**:
- [ ] `BasicTestDataParserTest.xls` の内容を YAML に変換し `BasicTestDataParserTest.yaml` として配置
- [ ] `BasicTestDataParserTestYaml` を作成し、`YamlTestDataParser` で同一アサーションを実行
- [ ] 既存16テストメソッド全件をYAML版で実行し、差異がある場合は原因を文書に明記する
- [ ] セルフチェック（チェック結果: `docs/checks/R-2.md`）
- [ ] QAエンジニアレビュー（サブエージェントで実施）
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
- [ ] 各テストを YAML テストデータを使う形式で実装する（R-1 の `YamlTestDataParser` を使う）
- [ ] SS-18（DATE型TZハザード・旧E-8）: `EXPECTED_COMPLETE_TABLE` の DATE カラムデフォルト値が CI 環境 TZ で動作することを確認。TZ依存が解消できない場合は制約事項として SS-18 の注記と D-1 に明記する
- [ ] セルフチェック（チェック結果: `docs/checks/R-3.md`）
- [ ] QAエンジニアレビュー（本質的なFBがなくなるまで改善）
- [ ] ユーザーレビュー依頼・OK取得

**完了条件**:
- 上記27件すべてに対応するテストが全グリーン
- SS-18（TZハザード）について「TZ依存解消済み」または「制約事項として D-1 に記載済み」のいずれかが確認できること

---

## Ph-3: 既存ExcelテストのYAML版並走と差分ゼロ確認

**前提**: R-1（YamlTestDataParser）完了

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
- **完了済みフェーズ**: スキーマ設計フェーズ全完了、Ph-1（I-1/I-2/I-3）完了（ただし I-4 で異常系仕様を追加する必要あり）
- **R-1-refactor 進捗**: **全レビュー通過済み・ユーザーレビュー待ち**（ユーザーレビューは I-4 完了後に実施）
- **タスク順序**: I-4 → R-1-refactor ユーザーレビュー → C-1（並行可）→ R-2/R-3 → V-1 → D-1

**I-4 追加の経緯**: R-1-refactor の実装レビューで「`table`/`path` キー欠如・FW_HEADER rows 型誤り時に例外スロー」のテストを追加したが、これらに対応する仕様IDが `ntf-impl-spec-list.md` に存在しなかった。I-1 が正常系仕様のみを列挙し異常系を仕様IDとして認識していなかったことが原因。

### 環境情報

- **Java**: Eclipse Temurin 17（`update-alternatives` で切り替え済み）
- **Maven settings**: `~/.m2/settings.xml` に社内 Nexus リポジトリ設定済み（`nablarch-parent:6-NEXT-SNAPSHOT` 解決済み）
- **注意**: `mvn clean package` は Javadoc プラグインが `JAVA_HOME` 未設定で `BUILD FAILURE` になるが、テスト自体は全グリーン。`Tests run:` 行と `Failures: 0, Errors: 0` で確認すること

### カバレッジ取得方法（pom.xml 変更不要）

親 POM に JaCoCo Offline Instrumentation が定義済みのため、以下の手順で取得できる。

```bash
# 1. テスト実行（jacoco.exec がプロジェクトルートに生成される）
mvn clean package -Dtest="対象テストクラス..."

# 2. レポート生成
mvn jacoco:report -Djacoco.dataFile=/path/to/nablarch-testing/jacoco.exec
# → target/site/jacoco/index.html で確認
```

`mvn test` だけでは `restore-instrumented-classes` が走らず（`prepare-package` フェーズにバインド）、
`jacoco:report` 時に「instrumented class」エラーになる。`package` まで実行すること。

### Ph-1 完了状況

**I-1:**
- **成果物**: `docs/ntf-impl-spec-list.md`（仕様ID 80件: DT-01〜DT-07 / SS-01〜SS-20 / RS-01〜RS-08 / HC-01〜HC-07 / IV-01〜IV-15 / DR-01〜DR-10 / MS-01〜MS-13）
- **チェック結果**: `docs/checks/I-1.md`（担当者 OK・QA OK・ユーザーレビュー OK）

**I-2:**
- **成果物**: `docs/ntf-impl-spec-list.md` に列「既存テストメソッド or テスト追加必要」追加（80件全件）
- 既存テストあり 45件 / テスト追加必要 35件（RS 全8件は `YamlTestDataParser` 未実装として記録）
- **チェック結果**: `docs/checks/I-2.md`（担当者 OK・QA OK・ユーザーレビュー OK）

**I-3:**
- **成果物**: `docs/ntf-impl-spec-list.md` に列「スキーマ根拠 or スキーマ外理由」追加（80件全件）
- スキーマ根拠あり 43件 / スキーマ外 37件
- **チェック結果**: `docs/checks/I-3.md`（担当者 OK・QA OK・ユーザーレビュー OK）

### R-1 進捗状況（参照用・ユーザーレビュー NG）

- コミット `e9a7432` 時点の実装（37テスト全グリーン）がベースとして存在する
- R-1-refactor でクラス分割後、既存37テストが引き続き通ることをリグレッション確認として使用する
- `docs/checks/R-1.md` に全レビュー指摘対応履歴を記録済み

### R-1-refactor 進捗状況（全レビュー通過済み・ユーザーレビュー待ち）

最終コミット `e0719bd` で以下の状態:
- `YamlTestDataParser`（188行）+ `reader.yaml` サブパッケージ5クラス
- テスト: `YamlTestDataParserTest`（37件）+ 各ビルダーテスト（42件）= 79件全グリーン
- `docs/checks/R-1-refactor.md`（担当者 OK・QA OK・Javaエキスパート OK・SE OK・ユーザーレビュー可）

**通過済みレビュー**:
- QAエンジニアレビュー 2回（QA-1〜QA-5 + 追加8件 全対応）
- Javaエキスパートレビュー（Javadoc修正・import整理・try-with-resources等 全対応）
- ソフトウエアエンジニアレビュー（applyDirectives集約・buildFragments統合・DEFAULT_RECORD_TYPE定数化等 全対応）

### ADR（設計判断記録）

- `docs/adrs/ADR-001-yaml-library.md`: SnakeYAML 2.6 採用の根拠
- `docs/adrs/ADR-002-yaml-dependency-scope.md`: compile スコープ採用の根拠

### 再開手順

1. `git checkout convert-testdata-excel-to-text` でブランチを確認し、`git status` でクリーンであることを確認
2. **I-4 を先に完了させる**
   - `YamlTestDataParser` および `reader.yaml` パッケージの異常系仕様を列挙し `ntf-impl-spec-list.md` に追加
   - R-1-refactor で追加した全テストを仕様IDに対応づける
   - セルフチェック → QAレビュー → ユーザーレビュー
3. I-4 完了後、**R-1-refactor のユーザーレビュー依頼・OK取得** を行う
   - チェックファイル `docs/checks/R-1-refactor.md` の内容を提示してユーザーの確認を得る
   - OK が出たら R-1-refactor 完了（タスク作業内容の最後のチェックボックスにチェック）
4. R-1-refactor 完了後、次タスクへ進む（**C-1 は R-1-refactor と並行可**）
   - C-1: JaCoCo カバレッジレポート設定
   - R-2: 既存テスト（BasicTestDataParserTest）の YAML 版作成

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
