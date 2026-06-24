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

- **Status**: paused — ドキュメント整合修正完了・ユーザーレビュー承認待ち
- **Date**: 2026-06-24
- **Last completed**: docs/pr75/docs/ と ntf-impl-spec-list.md を git 履歴から復元（f45c565）。その後、設計書・NTF解説書 examples・仕様リストの実装不整合を全件修正してプッシュ（d39f07e）。
- **Next**: ユーザーレビュー承認後 → PR 作業完了
- **Notes**: |
    - ブランチ状態: origin/convert-testdata-excel-to-text と同期済み（d39f07e が最新）
    - 本体差分: DataFileParser.java / ListMapParser.java / TableDataParser.java / TestDataParsingTemplate.java / TestDataParsingTemplateTest.java の5ファイルのみ（pom.xml 差分なし）
    - 修正済み不整合: 設計書4件（readシグネチャ・ConverterMojo→FormatHandler構成・クラス図フィールド追記・重複削除）、解説書2件（directives位置・type記法統一）、仕様リスト4件（testプレフィックス除去・メソッド名修正・未実装テスト訂正）
    - docs/ 復元元コミット: a5628c6（ntf-testdata-doc等）、16f59c9（ntf-impl-spec-list）
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
