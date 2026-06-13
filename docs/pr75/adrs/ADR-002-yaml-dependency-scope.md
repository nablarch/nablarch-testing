# ADR-002: snakeyaml-engine の依存スコープ

- **日付**: 2026-05-20
- **ステータス**: 承認済み

## 背景と決定

ADR-001 で選定した `snakeyaml-engine` を `pom.xml` に追加するスコープを決める。`nablarch-testing` は複数 PJ から依存されるテストサポートライブラリのため、YAML テストデータを使わない PJ にも `snakeyaml-engine` が推移的に入るかが論点になる。

**決定**: `compile`（スコープ省略）で追加する。既存の `poi-ooxml` と同じ扱い。

```xml
<dependency>
    <groupId>org.snakeyaml</groupId>
    <artifactId>snakeyaml-engine</artifactId>
    <version>2.9</version>
</dependency>
```

## 検討した選択肢

| スコープ | YAML 非利用 PJ への影響 | 構造変更 | 利用者の手間 | POI との一貫性 | 判定 |
|---|---|---|---|---|---|
| `compile`（省略） | 全 PJ に自動で入る | なし | なし | ○（POI と同じ） | **採用** |
| `optional` | 入らない（使う側が明示宣言） | なし | 使う側が pom に 1 行追加 | △（POI と異なる） | 却下 |
| モジュール分割（同リポジトリ） | 入らない | pom.xml 大改造・CI 変更 | 使う側が明示宣言 | — | 却下 |
| リポジトリ分割（別リポジトリ） | 入らない | 別リポジトリ作成・リリース管理 2 倍 | 使う側が明示宣言 | — | 却下 |

却下理由は以下のとおり。

- **`optional`**: YAML 非利用 PJ に入らない利点はあるが、既に `compile` で入っている `poi-ooxml` と扱いが分かれ、依存方針が一貫しない。
- **モジュール分割・リポジトリ分割**: いずれも非利用 PJ への流入を断てるが、pom.xml 改造・CI 変更・別リポジトリのリリース管理といった管理コストが今回の規模に見合わない。

## 結果

**良い点**

- `poi-ooxml`（compile）と一貫した依存方針になる。
- 構造変更が不要。

**悪い点**

- `nablarch-testing` に依存する全 PJ に `snakeyaml-engine` が推移的に入る（POI と同様）。YAML を使わない PJ にも入る。
