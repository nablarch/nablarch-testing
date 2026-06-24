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
- **カバレッジ取得**: 親 POM に JaCoCo Offline Instrumentation 設定済み。`pom.xml` 変更不要。
  ```
  mvn clean jacoco:instrument test jacoco:restore-instrumented-classes jacoco:report \
    [-Dtest="YamlTestDataReaderTest,..."]
  mvn jacoco:report -Djacoco.dataFile=/home/tie303177/work/nablarch-testing/jacoco.exec
  ```
  出力先は `${user.dir}/jacoco.exec`（プロジェクトルート）。`target/site/jacoco/` に HTML 生成。

---

# State

- **Status**: paused — カバレッジ手順を Rules に追記・ユーザーレビュー承認待ち
- **Date**: 2026-06-24
- **Last completed**: Rules に JaCoCo Offline Instrumentation 手順を追記（pom.xml 変更不要・dataFile はプロジェクトルート）
- **Next**: ユーザーレビュー承認後 → PR 作業完了
- **Notes**: |
    - ブランチ状態: origin/convert-testdata-excel-to-text と同期済み（d39f07e が最新）
    - 本体差分: DataFileParser.java / ListMapParser.java / TableDataParser.java / TestDataParsingTemplate.java / TestDataParsingTemplateTest.java の5ファイルのみ（pom.xml 差分なし）
    - 修正済み不整合（前回）: 設計書4件・解説書2件・仕様リスト4件
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
