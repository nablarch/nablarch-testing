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
- [ ] 第4回専門家レビュー（4名並列）実施
- [ ] 第4回指摘に基づく修正（本質的指摘がなくなるまで繰り返す）
- [ ] 最終コミット・プッシュ

---

## 成果物ファイル

| ファイル | 状態 |
|---|---|
| `docs/ntf-testdata-structure.md` | 完成（調査報告） |
| `docs/ntf-testdata-yaml-schema.json` | 修正中 |
| `docs/ntf-testdata-yaml-examples.yaml` | 修正中 |
| `docs/ntf-testdata-yaml-design.md` | 修正中 |
| `docs/tasks.md` | 本ファイル |

---

## 再開手順

1. このブランチをチェックアウト: `git checkout convert-testdata-excel-to-text`
2. 本ファイル (`docs/tasks.md`) でチェック済み/未着手タスクを確認
3. 未完了タスクから作業を再開する

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
