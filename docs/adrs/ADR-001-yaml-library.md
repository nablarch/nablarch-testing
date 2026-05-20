# ADR-001: YAMLパーサライブラリの選定

- **日付**: 2026-05-20
- **ステータス**: 承認待ち

## コンテキスト

`YamlTestDataReader` を実装するにあたり、YAMLファイルをJavaオブジェクト（Map/List）に変換するライブラリが必要になった。
既存の `pom.xml` にはYAML系ライブラリが存在しない。

## 検討候補

| ライブラリ | ライセンス | JARサイズ | CVE安全性 | 速度 | 備考 |
|---|---|---|---|---|---|
| SnakeYAML 1.x | Apache 2.0 | 340 KB | 危険（CVE-2022-1471 等複数） | 基準 | 新規採用禁止 |
| SnakeYAML 2.x | Apache 2.0 | 340 KB | 危険APIが残存（使いにくくしただけ） | 基準 | 最新 2.3 |
| SnakeYAML Engine | Apache 2.0 | 95 KB | 危険な機能が設計上存在しない（CVEゼロ） | 約10〜20%速い | 最新 2.9（2025-01） |
| Jackson YAML | Apache 2.0 | 重い | SnakeYAML依存 | — | Jackson本体も必要で過剰 |

## 決定

**`org.snakeyaml:snakeyaml-engine:2.9`** を採用する。

## 理由

- SnakeYAML 1.x のCVEはすべて「YAMLから任意Javaクラスをデシリアライズできる機能」が根本原因。Engine はその機能が設計上存在しないため、同じ問題が原理的に起きない
- JARサイズが95KBと小さい（SnakeYAML 2.x の340KBの約1/3）
- 作者が「新規コードはEngineを推奨」と明言している
- 今回の用途（Map/List/String/null/Boolean/Integer への変換）に必要十分

## 影響

- `pom.xml` に依存を1件追加する（スコープは ADR-002 参照）
