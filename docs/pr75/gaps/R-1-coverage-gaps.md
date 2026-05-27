# R-1 カバレッジギャップ一覧

解説書・Example ファイルとテストメソッドのマッピングを全件確認した結果、以下の未テスト項目を特定した。

作成日: 2026-05-26

---

## YAMLパーサー層でテストすべき未テスト項目（6件）

| # | 対象節 | 内容 | 追加先テストクラス | 対応テストメソッド |
|---|---|---|---|---|
| G-1 | 8.1 / examples-special.md 8.2 | ダブルクォート1文字 `"\""` → `QuotationTrimmer` で `"` 1文字になること | `YamlTableDataBuilderTest` | `testBuildListMapRows_escapedDoubleQuoteIsDoubleQuoteChar` |
| G-2 | 8.1 / examples-special.md 8.1 | `"${updateTime}"` / `"${setUpTime}"` → `DateTimeInterpreter` でシステム時刻に変換されること | `YamlTableDataBuilderTest` | `testBuildListMapRows_updateTimeAndSetUpTimeConverted` |
| G-3 | 9.3 / examples-special.md 9.2 | 可変長ファイルの `field-separator: "\\t"` がタブ文字として設定されること | `YamlFileBuilderTest` | `testBuildFileList_tabFieldSeparatorBecomesTabChar` |
| G-4 | 7.3 / examples-messaging.md 7.3 | `messages` の `id` にパスセグメントを含む形式（`sendSyncTestData/REQ001/message`）が正しく取得できること | `YamlMessageBuilderTest` | `testBuildMessagePool_idWithPathSegments` |
| G-5 | 7.2 / examples-messaging.md 7.2 | `expected_request_header_messages` から `buildMessagePool` で正しく取得できること | `YamlMessageBuilderTest` | `testBuildMessagePool_expectedRequestHeaderMessages` |
| G-6 | 4章 / examples-testshots.md | `testShots` という予約 ID で `list_maps` が正しく取得でき、Web/Batch/Messaging 各カラムが保持されること | `YamlTableDataBuilderTest` | `testBuildListMapRows_testShotsReservedId` |

---

## スコープ外（テスト不要と判定した項目）

| 内容 | 理由 |
|---|---|
| 8.7 `java.sql.Timestamp` 末尾 `.0` 必須 | DB アサート層（`TableData.getValue()` 比較）の動作。YAML パーサーは値を文字列として素通しするのみ |
| 8.8 `0xCAFEBABE` バイナリ記述 | DB 格納層の動作。YAML パーサーは値を文字列として素通しするのみ |
| 8.9 X9/SX9 型フィールド | 固定長フォーマッタ層の動作。YAML パーサーはフィールド型文字列を素通しするのみ |
| 7.4 ステータスコードデフォルト `"200"` | `SendSyncMessageParser` / `MockMessagingClient` 層の動作。YAML ビルダーは値を保持するのみ |
| 10.3 `#` コメント構文 | SnakeYAML のネイティブ動作。パーサーが介在しないため YAML リーダーとしてのテスト対象外 |

---

## 対応完了後のアクション

- 全 G-1〜G-6 のテストがグリーンになったら `R-1-coverage-gaps.md` を更新（各行に対応テストメソッド名を追記）
- `docs/pr75/steering.md` の R-1 作業内容チェックリストを更新
