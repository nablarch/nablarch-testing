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
- **Java/Maven 環境**: OpenJDK 17 (Temurin-17.0.19) + Maven 3.9.9。`compile`・`test`・`install` はすべて Java 17 で実行する
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

- **Status**: paused
- **Date**: 2026-06-25
- **Last completed**: Java 17 (Temurin-17.0.19) + Maven 3.9.9 で `mvn clean install` を実行し、compile・test（840件 Failures:0 Errors:0）・install すべて BUILD SUCCESS を確認
- **Next**: ユーザーレビュー承認後 → PR 作業完了
- **Notes**: |
    - ブランチ状態: origin/convert-testdata-excel-to-text と同期済み（2a6a28a が最新）
    - 全タスク（Phase 1〜6、#1〜#20、J-4/J-5）達成済み
    - Java17 での動作確認完了。Skipped 7 件は既存の既知スキップ（PR75 非起因）
    - 次のアクション: ユーザー PR レビュー承認 → 完了

---

# 成果物

| 種別 | ファイル |
|---|---|
| **変換ツール設計書（正）** | docs/testdata-converter-design.md |
| **読み込み機構の解説** | docs/ntf-testdata-loading.md |
| **NTF テストデータ解説書** | docs/ntf-testdata-doc.md |
| **後片付け手順（yaml）** | divide/cc2-hontai-cleanup.md |
| **後片付け手順（converter）** | divide/cc2-converter-cleanup.md |
