# I-4 完了条件チェック

## 完了条件チェックリスト

| 完了条件 | 担当者判定 | 担当者根拠 | QA判定 | QA根拠 |
|---|---|---|---|---|
| 既存 Excel 系実装の異常系挙動が全件仕様IDとして登録されていること | OK | `BasicTestDataParser` / `DataFileParser` / `TableData` / `DataFileFragment` / `FixedLengthFile` / `VariableLengthFile` / `MessageParser` / `SendSyncMessageParser` の全 `throw` 文・null 返却条件を走査し、14件の仕様ID（SS-21〜25 / IV-16 / DR-11 / MS-14〜15 / RS-09〜13）として `ntf-impl-spec-list.md` に追加した。除外したのは `TableData#getClone()` の `CloneNotSupportedException` ラップ（通常到達不能パス）と `TableData#clob2String()` の `SQLException` ラップ（DBレイヤーの例外でテストデータ仕様外）と `DataFileFragment#checkSize()` の `IllegalStateException`（`setTypes()`/`setLengths()` のセッター段階でバリデーション済みのため通常到達不能）の3件のみで、いずれも理由を以下に明記する | - | - |
| R-1-refactor で追加した全テストメソッドが、いずれかの仕様IDに対応づけられていること | OK | R-1-refactor で追加した6件のテスト（`testBuildTableDataList_missingTableThrowsException` → RS-09、`testBuildFileList_missingPathThrowsException` → RS-10、`testBuildMessagePool_malformedFwHeaderRowsThrowsException` → RS-11、`testBuildMessagePool_emptyFwHeaderRows` → RS-12、`testDataTypeToSectionKey_unsupportedDataTypeThrowsException` → RS-13、`testBuildTableDataList_emptyRowsExcluded` → SS-21、`testBuildTableDataList_sectionNotExists` → SS-21、`testBuildListMapRows_idNotFound` → SS-21、`testBuildFileList_sectionNotExists` → SS-21、`testBuildMessagePool_idNotFound` → MS-15、`testBuildSendSyncMessageList_groupIdNotFound` → MS-15、`testBuildMessageFile_idNotFound` → MS-15）の全件が仕様IDに対応づけられた | - | - |
| YAML 実装の異常系挙動が既存 Excel 実装の仕様と一致していること（乖離がある場合は理由が明記されていること） | OK | RS-09（table 欠如 → `IllegalStateException`）/ RS-10（path 欠如 → `IllegalStateException`）/ RS-11（FW_HEADER rows 型誤り → `IllegalStateException`）は既存 Excel 実装に対応する explicit チェックがなく YAML 実装固有のバリデーション。これは「YAML は構造が明示的なためより厳密なエラー報告が可能」という設計判断で許容される乖離であり、仕様IDの「スキーマ外理由」欄に明記した | - | - |
| 「仕様IDのないテスト」が存在しないこと | OK | R-1-refactor で追加した全テストメソッドを仕様IDに対応づけた。`YamlTestDataParserTest` の RS-03/RS-06（既存）は既登録仕様IDに対応済み | - | - |

### 仕様IDとして登録しなかった異常系（除外理由）

| 対象コード | 異常系内容 | 除外理由 |
|---|---|---|
| `TableData#getClone()` 行580-582 | `CloneNotSupportedException` → `RuntimeException` ラップ | 通常到達不能パス（`TableData` は `Cloneable` を `implements` しており `clone()` は必ず成功する）。テスト対象として意味がない |
| `TableData#clob2String()` 行419-421 | `SQLException` → `RuntimeException` ラップ | JDBC CLOB アクセス時の DB レイヤー例外。テストデータ仕様（読み書き形式の仕様）には直接関係しない。DB 例外ハンドリングはアプリレベルの関心事 |
| `DataFileFragment#checkSize()` 行543-546 | `isSizeValid()` が true のとき `IllegalStateException("invalid data.")` | `checkSize()` は `prepareRecordDefinition()` から呼ばれるが、`setTypes()`/`setLengths()` のセッター段階（SS-24）でサイズ不一致をチェックし `IllegalArgumentException` をスローするため、`checkSize()` 実行時点ではサイズ不一致は発生しない。通常到達不能パス（SS-24 のバリデーションが適切にテストされることを保証条件とする） |

## QAエンジニアレビュー

| 観点 | 判定 | 根拠・改善案 |
|---|---|---|
| 目的に対して意味のある洗い出しが実施されているか | OK | 対象クラス全件の `throw` 文・null 返却を走査済み。漏れ2件（SS-25 / DR-02/DR-03 根拠コード）を指摘し対応済み。除外3件の理由も妥当と確認 |
| エッジケース（異常系挙動）が漏れなく登録されているか | OK | QA指摘3件（SS-25 追加・DR-02/DR-03 根拠コード補記・`checkSize()` 除外理由明記）を全件対応済み |

## 総合判定

- 担当者: OK
- QA: OK（QA指摘3件を全件対応済み）
- 対象言語エキスパート: 該当なし（ソースコード変更なし）
- ソフトウエアエンジニア: 該当なし（ソースコード変更なし）
- ユーザーレビュー可否: 可
