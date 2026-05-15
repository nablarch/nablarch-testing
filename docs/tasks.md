# NTF テストデータ YAML スキーマ設計 タスクリスト

ブランチ: `convert-testdata-excel-to-text`

## 目的

NTFのテストデータをExcelからYAMLに移行するためのスキーマ設計・ドキュメント整備。
専門家レビューで本質的な指摘がなくなるまで、修正→レビューを繰り返す。

---

## タスク一覧

### P0（バグ・即修正）

- [x] P0-1: `ntf-testdata-yaml-schema.json` のディレクティブキー大量欠落を修正
  - 固定長用: `positive-zone-sign-nibble`, `negative-zone-sign-nibble`, `positive-pack-sign-nibble`, `negative-pack-sign-nibble`, `required-decimal-point`, `fixed-sign-position`, `required-plus-sign`
  - 可変長用: `field-separator`, `quoting-delimiter`, `ignore-blank-lines`, `requires-title`, `max-record-length`, `title-record-type-name`
- [x] P0-2: `ntf-testdata-yaml-examples.yaml` のマーカーカラム構文バグ修正
  - `[MARKER_COL]: X` → `"[MARKER_COL]": X`

### P1（仕様曖昧・要確定）

- [x] P1-1: `null` 表現の仕様を確定してスキーマ・examples・design.md に統一明記
  - 方針: YAMLネイティブ `null` を正式採用（パーサがnullとして受け取る）
  - 文字列 `"null"` は NullInterpreter 経由（後方互換）として明記

### P2（整合性修正）

- [x] P2-1: `message_data` / `group_message_data` の `records` を `required` に追加、`minItems: 1` を設定
- [x] P2-2: `field_def.type` を `enum` で制約（`X`,`N`,`XN`,`Z`,`SZ`,`P`,`SP`,`X9`,`SX9`,`B`）
- [x] P2-3: `field_def.length` の `oneOf` → `anyOf` に変更

### P3（ドキュメント補強）

- [x] P3-1: `design.md` に Excel→YAML 変換ビフォーアフター例を追加
- [x] P3-2: `design.md` に段階的移行戦略セクションを追加
- [x] P3-3: `design.md` に AI向けプロンプト補助情報セクションを追加
- [x] P3-4: `examples.yaml` に特殊値インライン例を追加
- [x] P3-5: `examples.yaml` に数値クォートのNG/OKアンチパターン例を追加
- [x] P3-6: `examples.yaml` の `record-separator` エスケープ仕様をコメント明記
- [x] P3-7: `group_message_data.id` の description を改善（GroupDataはgroupIdでフィルタする旨を明記）

### P4（仕様網羅性の根拠確立）

テストデータ仕様の「塗りつぶし」 — 「レビューした」ではなく「全クラスを確認済み」という根拠を作る。

- [ ] P4-0: 調査リポジトリの範囲確認（前提検証）
  - 「このリポジトリだけ見ればよい」という前提自体を検証する
  - pom.xml の依存ライブラリを確認し、テストデータ仕様に関わる外部依存（nablarch-core-dataformat 等）を特定
  - 各外部依存について「どの仕様がこのリポジトリ外で定義されているか」を整理
  - 外部依存の仕様をどこで・どうやって確認するかの方針を決める
  - 出力: `docs/ntf-coverage-class-list.md` の前置セクションとして記載

- [ ] P4-1: 対象クラス一覧の作成
  - リポジトリ内のテストデータ関連クラスを全列挙
  - 各クラスについて「対象（スキーマに影響する）」「対象外（理由付き）」を分類
  - 出力: `docs/ntf-coverage-class-list.md`

- [ ] P4-2: 対象クラス毎の仕様抜き出しと行カバレッジ
  - 対象クラスの各メソッド・分岐について仕様を抜き出す
  - YAMLスキーマ・design.md・examples.yaml のどの記述が対応するかをマッピング
  - 未カバーの仕様があれば「未反映」として記録
  - 出力: `docs/ntf-coverage-spec-mapping.md`

- [ ] P4-3: テストコードから仕様を補完
  - 既存テストコード（`src/test/`）からテストデータ仕様の暗黙ルールを抜き出す
  - ソースコードだけでは読み取れない境界値・エラーケースをP4-2に補足
  - 出力: P4-2 ドキュメントへの追記

- [ ] P4-4: JavaエキスパートとQAエキスパートによるレビュー（サブエージェント並列）
  - Javaエキスパート: P4-1/P4-2 の分類・マッピングの正確性をコードと照合
  - QAエキスパート: 未カバー仕様の洗い出し・テスト観点の欠落確認
  - 本質的な指摘がなくなるまで P4-2 修正→レビューを繰り返す

### レビューループ

- [x] 第1回専門家レビュー（4名並列）実施済み
- [x] 第1回指摘を修正（P0〜P3）
- [x] 第2回専門家レビュー（4名並列）実施
- [x] 第2回指摘に基づく修正
  - field_def.type を enum → pattern: "^[A-Z][A-Z0-9]*$" に変更（カスタム型拡張対応）
  - record_fragment.rows に minItems: 1 を追加
  - group_message_data.group_id を required に追加
  - record-separator description にシンボル形式（CRLF/LF等）を追記
  - file-type description に「通常は記述不要（自動設定）」を追記
  - examples.yaml の固定長ファイル rows からパディング除去（自動付与される仕様）
  - examples.yaml の冒頭コメントにメッセージ系・expected_complete_tables を追記
  - design.md に変換ビフォーアフター（グループIDなし例）を追加
  - design.md にExcelとYAMLの並存説明・数値セル注意・複数シート方針を追加
  - AI向けプロンプト補助情報にboolean値クォート不要・record-separator罠・列順ミス検出タイミング・SingleData id一意制約を追記
- [x] 第3回専門家レビュー（4名並列）実施
- [x] 第3回指摘に基づく修正
  - design.md: 固定長ビフォーアフター例のパディングを除去（examples.yamlとの矛盾解消）
  - design.md: 「ExcelとYAMLの並存」重複セクションを統合・削除
  - examples.yaml: 残存パディングを全て除去（SEARCH_KEY, RESULT_COUNT/DATA 等）
  - schema.json: field_def.type の pattern を ^[A-Z][A-Z0-9_]*$ に緩和（TEST_ プレフィクス型対応）
  - schema.json: record_fragment.rows の minItems: 1 を削除（空ファイル検証ユースケース対応）
  - design.md §AI向け: expected_complete_tables 使い分け・quoting-delimiter 記述例を追記
- [x] 第4回専門家レビュー（4名並列）実施 → 全員「合格」。本質的な問題なし（後に問題発覚）
- [x] 独立再レビュー実施 → group_message_data.group_id が必須設定されている重大バグを発見
- [x] 第5回修正
  - schema.json: group_message_data の required から group_id を削除（MockMessagingContext/Client 経路は group_id 不要）
  - schema.json: group_message_data.description に2つのアクセスパス（A/B）を詳述
  - schema.json: table_data.rows の description に rows:[] 全件削除セマンティクスを追記
  - design.md: §9 を「2つのアクセスパス」として書き直し
  - examples.yaml: 特殊値インライン例をコメントアウトから有効 YAML に昇格
  - examples.yaml: NG コメントに YAML 1.1/1.2 バージョン依存性を明記
- [x] 第5回専門家レビュー（4名並列）実施
- [x] 第5回フォローアップ修正
  - examples.yaml: expected_request_header_messages の例を追加（13 DataType 全網羅）
  - examples.yaml: BasicJapaneseCharacterInterpreter の 14 文字種トークン一覧を追加
  - examples.yaml: SendSyncMessageParser の errorMode（timeout/msgException）説明・例示
  - examples.yaml: "null" クォート付き NG 例の注記を追加
  - design.md: §5 に field_def.type と BasicDataTypeMapping の関係（identity mapping 要件）を追記
  - design.md: §11 に MessageParser が record_type を "default" に置換する仕様を記録
  - design.md: null テーブルに "null"（クォート付き）NG 例の行を追加
  - design.md: AI向けプロンプト補助情報に 14 文字種トークン・record_type 注意・errorMode を追記
  - design.md: setCellType(STRING) を DataFormatter API に訂正（POI 4.x 以降）
  - design.md: dataName/resourceName のシート概念消滅に関する注意事項を追記
  - schema.json: group_id に minLength: 1 を追加（空文字による誤マッチ防止）
  - ntf-testdata-structure.md §3.3: FixedLengthDirective を 11 キーに拡充
- [x] 最終コミット・プッシュ

---

## 現在の状態（2026-05-15時点）

- **ブランチ**: `convert-testdata-excel-to-text`（ローカル・リモートともにクリーン）
- **完了済み**: P0〜P3 すべて、レビューループ第1〜5回
- **次のタスク**: P4-0（調査リポジトリの範囲確認）から順に着手

**P4 着手前の注意**:
- P4-1/P4-2 の調査はセッション内で試行したが、結果はコミットされていない（作業ログのみ）
- P4-0 の pom.xml 確認 → P4-1 クラス一覧作成 → P4-2 仕様マッピングの順で進めること
- P4 の出力ファイル（`docs/ntf-coverage-class-list.md`、`docs/ntf-coverage-spec-mapping.md`）はまだ存在しない

---

## 成果物ファイル

| ファイル | 状態 |
|---|---|
| `docs/ntf-testdata-structure.md` | 完成（コード調査報告） |
| `docs/ntf-testdata-yaml-schema.json` | 完成（第5回レビュー対応済み） |
| `docs/ntf-testdata-yaml-examples.yaml` | 完成（第5回レビュー対応済み） |
| `docs/ntf-testdata-yaml-design.md` | 完成（第5回レビュー対応済み） |
| `docs/tasks.md` | 本ファイル |
| `docs/ntf-coverage-class-list.md` | **未作成**（P4-0/P4-1 で作成予定） |
| `docs/ntf-coverage-spec-mapping.md` | **未作成**（P4-2/P4-3 で作成予定） |

---

## 再開手順

1. ブランチをチェックアウト: `git checkout convert-testdata-excel-to-text`
2. 本ファイルで「現在の状態」と未完了タスク（P4-0〜）を確認
3. P4-0 から順に着手:
   - pom.xml の外部依存を確認し、テストデータ仕様に関わるリポジトリ外の仕様範囲を特定
   - 「このリポジトリだけ見ればよいか」を明文化してから P4-1 へ進む

---

## 第1回レビュー指摘サマリー（根拠）

### 重大

| ID | 指摘 | レビュアー |
|---|---|---|
| R1-1 | `directives` に固定長用7キー・可変長用6キーが欠落。`additionalProperties: false` のためバリデーションエラー | 実装整合性 |
| R1-2 | `[MARKER_COL]: X` がYAMLフロー配列として誤解釈される | JSON Schema品質 |

### 軽微

| ID | 指摘 | レビュアー |
|---|---|---|
| R2-1 | `message_data`・`group_message_data` の `records` が `required` に未含有 | 実装整合性・JSON Schema品質 |
| R2-2 | `field_def.type` を `enum` で制約可能 | JSON Schema品質 |
| R2-3 | `oneOf` → `anyOf`（意味論的正確性） | JSON Schema品質 |
| R2-4 | `group_message_data.id` の description が GroupData のフィルタ動作を未記載 | 実装整合性 |

### 設計・ドキュメント

| ID | 指摘 | レビュアー |
|---|---|---|
| R3-1 | `null` 表現の未確定（ネイティブ vs 文字列） | 全員 |
| R3-2 | テーブル系とファイル系で `rows` の形式が異なる点を強調すべき | AI可読性 |
| R3-3 | 段階的移行戦略の記載なし | 開発者UX |
| R3-4 | 変換ビフォーアフター例なし | 開発者UX |
| R3-5 | 特殊値インライン例・NGアンチパターン例が不足 | AI可読性 |
| R3-6 | `record-separator` エスケープ仕様が不明確 | JSON Schema品質・開発者UX |
