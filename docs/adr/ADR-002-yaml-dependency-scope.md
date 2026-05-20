# ADR-002: snakeyaml-engine の依存スコープ

- **日付**: 2026-05-20
- **ステータス**: 承認待ち

## コンテキスト

ADR-001 で選定した `snakeyaml-engine` を `pom.xml` に追加する際のスコープを決定する。
このリポジトリ（`nablarch-testing`）は複数のプロジェクトから依存されるテストサポートライブラリであり、
YAMLテストデータを使わないプロジェクトにも snakeyaml-engine が推移的に入るかどうかが論点になった。

## 検討候補

| スコープ | YAMLを使わないPJへの影響 | 構造変更 | 利用者の手間 | POIとの一貫性 |
|---|---|---|---|---|
| `compile`（省略） | 全PJに自動で入る | なし | なし | ○（POIと同じ） |
| `optional` | 入らない（使う側が明示宣言） | なし | 使う側が pom に1行追加 | △（POIと異なる） |
| モジュール分割（同リポジトリ） | 入らない | pom.xml 大改造・CI変更 | 使う側が明示宣言 | — |
| リポジトリ分割（別リポジトリ） | 入らない | 別リポジトリ作成・リリース管理2倍 | 使う側が明示宣言 | — |

## 決定

**`optional`** スコープで追加する。

```xml
<dependency>
    <groupId>org.snakeyaml</groupId>
    <artifactId>snakeyaml-engine</artifactId>
    <version>2.9</version>
    <optional>true</optional>
</dependency>
```

## 理由

- YAMLテストデータを使わないプロジェクトに不要なライブラリが入ることを避けたい
- モジュール分割・リポジトリ分割は利用者側の影響は同じだが、管理コストが大きく今回の規模に見合わない
- `compile` は構造変更不要だが、既存の POI（Excel不要なPJにも入っている）と同じ問題を踏襲することになり、新規追加で同じ轍を踏む理由がない

## 影響

- `YamlTestDataReader` を使うプロジェクトは `pom.xml` に `snakeyaml-engine` の依存を明示的に追加する必要がある
- `YamlTestDataReader` を使わないプロジェクトへの影響はゼロ
