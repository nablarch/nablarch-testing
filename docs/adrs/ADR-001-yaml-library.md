# ADR-001: YAMLパーサライブラリの選定

- **日付**: 2026-05-20
- **ステータス**: 承認済み（2026-05-20 変更: SnakeYAML Engine → SnakeYAML 2.x に変更）

## コンテキスト

`YamlTestDataReader` を実装するにあたり、YAMLファイルをJavaオブジェクト（Map/List）に変換するライブラリが必要になった。
既存の `pom.xml` にはYAML系ライブラリが存在しない。

## 検討候補

| ライブラリ | ライセンス | JARサイズ | CVE安全性 | 速度 | 備考 |
|---|---|---|---|---|---|
| SnakeYAML 1.x | Apache 2.0 | 340 KB | 危険（CVE-2022-1471 等複数） | 基準 | 新規採用禁止 |
| SnakeYAML 2.x | Apache 2.0 | 340 KB | 2.0 で全CVEに対処済み。危険APIは残るが使用しない限り安全 | 基準 | 最新 2.6（2026-02） |
| SnakeYAML Engine | Apache 2.0 | 95 KB | 危険な機能が設計上存在しない（CVEゼロ） | 約10〜20%速い | 最新 2.9（2025-01） |
| Jackson YAML | Apache 2.0 | 重い | SnakeYAML依存 | — | Jackson本体も必要で過剰 |

## 決定

**`org.yaml:snakeyaml:2.6`** を採用する。

## 理由

- SnakeYAML 2.x は 2.0 で全CVEに対処済みであり、最新版（2.6）を使う限り既知のCVEはない
- `Yaml.load()` を使わず `SafeConstructor` または `LoaderOptions` で型制限して使えば、残存する危険APIを踏まない
- SnakeYAML Engine より実績・情報量が多く、`Yaml.load()` が `Map<String, Object>` を返す API が直感的で扱いやすい
- 今回の用途（Map/List/String/null/Boolean/Integer への変換）に必要十分

## 影響

- `pom.xml` に依存を1件追加する（スコープは ADR-002 参照）
