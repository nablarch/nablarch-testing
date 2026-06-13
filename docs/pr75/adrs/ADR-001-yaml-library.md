# ADR-001: YAMLパーサライブラリの選定

- **日付**: 2026-05-20
- **ステータス**: 更新済み（2026-05-27 変更: SnakeYAML 2.6 → SnakeYAML Engine 3.0.1 に切替）

## 背景と決定

`YamlTestDataReader` の実装で、YAMLファイルをJavaオブジェクト（Map/List）に変換するライブラリが必要になった。既存の `pom.xml` にYAML系ライブラリは存在しない。

**決定**: `org.snakeyaml:snakeyaml-engine:3.0.1` を採用する。

当初は `org.yaml:snakeyaml:2.6` を採用したが、後述の Norway Problem が顕在化したため切り替えた。決定を分けた決め手は、テストデータの `no:` キーが破壊されないこと（YAML 1.2 Core Schema がデフォルト）である。

## 検討した選択肢

却下理由が判断の主役。比較軸（ライセンス・JARサイズ・CVE安全性・速度）を中立に並べる。

| ライブラリ | ライセンス | JARサイズ | CVE安全性 | 速度 | 採否と理由 |
|---|---|---|---|---|---|
| SnakeYAML 1.x | Apache 2.0 | 340 KB | 危険（CVE-2022-1471 等複数） | 基準 | 却下。新規採用禁止 |
| SnakeYAML 2.x（当初採用 2.6） | Apache 2.0 | 340 KB | 2.0 で全CVEに対処済み。危険APIは残るが使用しない限り安全 | 基準 | 却下。Norway Problem で `no:` が `false` に化ける（下記） |
| SnakeYAML Engine 3.x | Apache 2.0 | 95 KB | 危険な機能が設計上存在しない（CVEゼロ）。YAML 1.2 Core Schema でデフォルト動作 | 約10〜20%速い | **採用（3.0.1）** |
| Jackson YAML | Apache 2.0 | 重い | SnakeYAML依存 | — | 却下。Jackson本体も必要で過剰 |

### SnakeYAML 2.x を却下した理由（当初採用からの切替）

- **Norway Problem**: SnakeYAML 2.x はデフォルトで YAML 1.1 仕様に従い、`no`/`yes`/`on`/`off` を Boolean として解釈する。テストデータの `no:` キーが `false` に変換される根本的なバグが発生した。
- **SnakeYAML Engine はデフォルトで YAML 1.2 Core Schema**: `no`/`yes`/`on`/`off` を文字列として扱い、Norway Problem が設計上発生しない。
- **移行コストは低い**: 使用 API は `YamlLoader.java` の1ファイルに完全隔離されており、`SafeConstructor` → `Load(LoadSettings)` の等価な API 移行で済む。
- JARサイズが小さく（95 KB）、CVE がゼロの点でも SnakeYAML Engine が優る。

## 影響

良い点・コストの両方を残す。

- **良い点**: Norway Problem が設計上発生しない。JARサイズが SnakeYAML 2.x の 340 KB から 95 KB に減り、CVE はゼロ。速度は基準比で約10〜20%速い。
- **コスト（変更作業）**:
  - `pom.xml` の依存を `org.yaml:snakeyaml:2.6` → `org.snakeyaml:snakeyaml-engine:3.0.1` に変更する（スコープは ADR-002 参照）。
  - `YamlLoader.java` の API を `Yaml(SafeConstructor)` から `Load(LoadSettings)` に移行する。
- **限界**: 影響範囲は `YamlLoader.java` の1ファイルに閉じる。これ以外のコードへの波及は確認されていない。
