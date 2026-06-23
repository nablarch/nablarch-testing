# NTF テストデータ変換ツール 再構築フェーズ（設計書 6.3 到達）

ブランチ: `convert-testdata-excel-to-text`
ドラフト PR: [lovaizu/nablarch-testing#1](https://github.com/lovaizu/nablarch-testing/pull/1)

**設計書 `docs/pr75/docs/testdata-converter-design.md` が唯一の正。**

---

# Goal

Excel↔YAML テストデータ変換ツールを設計書通りに作り直し、品質担保 Level1〜3（単体テスト・往復変換・既存テスト全件 PASS）を達成する。

**→ 達成済み。** Phase 1〜6（#1〜#20、J-4/J-5）全タスク完了。リポジトリ分割フェーズへ移行。

---

# Rules

- **1 task = 1 commit・push 必須**
- released NTF 本体プロダクションコードの変更は**事前にユーザー相談**
- `pom.xml` の `M` 差分（parent `6-NEXT-SNAPSHOT`→`6u3`）は既知のローカル変更。コミット不要
- テスト実行は必ず `LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o test`
- 残存 4E（`MockHttpRequestTest`/`MockServletExecutionContextTest`）は PR75 非起因の既知事象

---

# State

- **Status**: paused — 本体後始末完了・ユーザーレビュー承認待ち
- **Date**: 2026-06-23
- **Last completed**: 本体を develop へ戻し（reset --hard origin/develop）、キャッシュTemplate Method集約5ファイルのみ再適用。テスト840件緑、C0/C1カバレッジ実質100%、mvn install BUILD SUCCESS。
- **Next**: ユーザー承認後 → このブランチのPR作業完了
- **Notes**: |
    - ブランチ状態: origin/develop と完全一致 + 5ファイル差分のみ（pom.xml 差分なし）
    - 5ファイル: DataFileParser.java / ListMapParser.java / TableDataParser.java / TestDataParsingTemplate.java / TestDataParsingTemplateTest.java
    - 未カバー箇所はすべて意図的: TestDataParsingTemplate のデフォルトno-op実装、DataFileParser の到達不能 default ブランチ（コメント入り）
    - mvn install 済み（6u3 ローカルリポジトリへインストール完了）
    - reset --hard により docs/ が消えたため steering.md を再作成。checks/ 配下のファイルも消失済み。
    - 次のアクション: ユーザー承認 → 完了

---

# 成果物

| 種別 | ファイル |
|---|---|
| **変換ツール設計書（正）** | docs/testdata-converter-design.md |
| **読み込み機構の解説** | docs/ntf-testdata-loading.md |
| **NTF テストデータ解説書** | docs/ntf-testdata-doc.md |
| **後片付け手順（yaml）** | divide/cc2-hontai-cleanup.md |
| **後片付け手順（converter）** | divide/cc2-converter-cleanup.md |
