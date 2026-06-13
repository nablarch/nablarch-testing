# NTF テストデータ YAML スキーマ — 正確性の根拠

このスキーマを設計・実装判断の根拠として信頼してよいかを判定するための評価記録。

**対象成果物**:
- スキーマ定義 → [`ntf-testdata-yaml-schema.json`](ntf-testdata-yaml-schema.json)
- スキーマ設計・判断根拠 → [`ntf-testdata-yaml-design.md`](ntf-testdata-yaml-design.md)
- 記述例 → [`ntf-testdata-yaml-examples.yaml`](ntf-testdata-yaml-examples.yaml)

---

## 結論

**信頼して使える。** ソースコード全行走査・公式解説書との照合・専門家レビューの3層で検証し、検出した未反映仕様・バグはすべて反映・修正済み。

---

## 評価基準

単一の確認手段では見落としが残る。コード読解はソースの裏取りになるが網羅性を保証しない。ドキュメント照合は仕様の網羅を補うがコードとの乖離を見抜けない。独立レビューは前二者の盲点を突く。三者が揃って初めて「漏れがない」と言える、という基準で検証層を構成した。先行実装例・既存変換ツールとの照合は、自分の判断が他実装と矛盾しないかの相互確認として加えた。

---

## 検証結果

| 検証層 | 規模 | 結果 |
|---|---|---|
| ソースコード全行走査 | `src/main/java` 直接影響クラス 29件、全行を「仕様あり / 対象外」に分類 | 未反映仕様 S-1〜S-5 / D-1〜D-16 / E-1〜E-4 を発見・反映 |
| 公式解説書との照合 | RST ファイル 13本 | 未反映仕様 17件（Doc-1〜Doc-17）を発見・全件反映 |
| 専門家レビュー | 4観点 × 5回ループ（本質的指摘がなくなるまで反復） | 第4回で `group_id` 必須バグ（重大）を検出・修正 |
| 先行実装例との照合 | nablarch-example-{batch,web,rest}-ntf-yaml（3リポジトリ） | 複数シート対応方針（1シート1ファイル分割）を確定 |
| 変換ツールとの照合 | nablarch-test-data-converter（社内GitLab） | マーカーカラム除外方針を採用。その他15件は取り込まず |

---

## 根拠（検証の一次記録）

- ソースコード走査 → [`ntf-coverage-class-list.md`](ntf-coverage-class-list.md)（クラス選定根拠）/ [`ntf-coverage-spec-mapping.md`](ntf-coverage-spec-mapping.md)（全行走査記録）
- 公式解説書との照合 → [`ntf-coverage-doc-check.md`](ntf-coverage-doc-check.md)（17件の差分リスト）
- レビュー経緯・各回の指摘と対応 → [`tasks.md`](tasks.md)（レビューループセクション）
- 先行実装例の評価 → [`ntf-yaml-impl-evaluation.md`](ntf-yaml-impl-evaluation.md)
- 変換ツールとの比較 → [`ntf-converter-comparison.md`](ntf-converter-comparison.md)
