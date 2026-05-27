# ADR-001: YAMLパーサライブラリの選定

- **日付**: 2026-05-20
- **ステータス**: 更新済み（2026-05-27 変更: SnakeYAML 2.6 → SnakeYAML Engine 3.0.1 に切替）

## コンテキスト

`YamlTestDataReader` を実装するにあたり、YAMLファイルをJavaオブジェクト（Map/List）に変換するライブラリが必要になった。
既存の `pom.xml` にはYAML系ライブラリが存在しない。

## 検討候補

| ライブラリ | ライセンス | JARサイズ | CVE安全性 | 速度 | 備考 |
|---|---|---|---|---|---|
| SnakeYAML 1.x | Apache 2.0 | 340 KB | 危険（CVE-2022-1471 等複数） | 基準 | 新規採用禁止 |
| SnakeYAML 2.x | Apache 2.0 | 340 KB | 2.0 で全CVEに対処済み。危険APIは残るが使用しない限り安全 | 基準 | 最新 2.10 |
| SnakeYAML Engine 3.x | Apache 2.0 | 95 KB | 危険な機能が設計上存在しない（CVEゼロ）。YAML 1.2 Core Schema でデフォルト動作 | 約10〜20%速い | 最新 3.0.1（2025） |
| Jackson YAML | Apache 2.0 | 重い | SnakeYAML依存 | — | Jackson本体も必要で過剰 |

## 決定

**`org.snakeyaml:snakeyaml-engine:3.0.1`** を採用する。

## 理由

当初は `org.yaml:snakeyaml:2.6` を採用していたが、以下の問題が顕在化したため切り替えた。

- **YAML 1.1 Norway Problem**: SnakeYAML 2.x はデフォルトで YAML 1.1 仕様に従い、`no`/`yes`/`on`/`off` を Boolean として解釈する。テストデータの `no:` キーが `false` に変換されるという根本的なバグが発生した
- **SnakeYAML Engine はデフォルトで YAML 1.2 Core Schema**: `no`/`yes`/`on`/`off` は文字列として扱われ、Norway Problem が設計上発生しない
- **使用 API は1ファイルに完全隔離**: `YamlLoader.java` のみで使用し、`SafeConstructor` → `Load(LoadSettings)` の等価な API 移行が可能
- JARサイズが小さく（95 KB）、CVE がゼロの点でも SnakeYAML Engine が優る

## 影響

- `pom.xml` の依存を `org.yaml:snakeyaml:2.6` → `org.snakeyaml:snakeyaml-engine:3.0.1` に変更する（スコープは ADR-002 参照）
- `YamlLoader.java` の API を `Yaml(SafeConstructor)` から `Load(LoadSettings)` に移行する
