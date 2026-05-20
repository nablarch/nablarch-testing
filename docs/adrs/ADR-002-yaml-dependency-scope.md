# ADR-002: snakeyaml-engine の依存スコープ

- **日付**: 2026-05-20
- **ステータス**: 承認済み

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

**`compile`（スコープ省略）** で追加する。POI と同じ扱い。

```xml
<dependency>
    <groupId>org.snakeyaml</groupId>
    <artifactId>snakeyaml-engine</artifactId>
    <version>2.9</version>
</dependency>
```

## 理由

- 既存の `poi-ooxml` がスコープ省略（compile）で追加されており、それと一貫した方針を採る
- 構造変更なしで済む
- モジュール分割・リポジトリ分割は管理コストが大きく今回の規模に見合わない

## 影響

- `nablarch-testing` に依存する全プロジェクトに `snakeyaml-engine` が推移的に入る（POI と同様）
