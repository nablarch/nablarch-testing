# NTF テストデータ YAML スキーマ — 正確性の根拠

- **対象スキーマ**: `ntf-testdata-yaml-schema.json`
- **関連文書**: `ntf-testdata-yaml-design.md` / `ntf-testdata-yaml-examples.yaml`
- **作成日**: 2026-05-15

---

## 1. 概要

このスキーマは「コードを読む → 解説書と照合する → 専門家レビューを受ける」という3段階の検証を経て作成した。  
本資料はその根拠を記録し、将来の改訂者がスキーマの信頼性を評価できるようにすることを目的とする。

---

## 2. 根拠の全体構造

```
┌─────────────────────────────────────────────────────────┐
│  根拠1: ソースコード全行走査（P4-0〜P4-4）               │
│  nablarch-testing の src/main/java 29クラスを全行走査    │
│  各行を「仕様あり / 対象外」に分類し、スキーマとマッピング│
├─────────────────────────────────────────────────────────┤
│  根拠2: 公式解説書との照合（D-5）                        │
│  nablarch-document の RST 13ファイルを読み込み           │
│  コード調査では判明しない「ユーザ向け仕様」を補完         │
├─────────────────────────────────────────────────────────┤
│  根拠3: 専門家レビュー（5回のレビューループ）             │
│  実装整合性・JSON Schema品質・AI可読性・開発者UX の       │
│  4観点から独立した複数レビュアーが査読                   │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 根拠1 — ソースコード全行走査

### 3.1 調査の方法と証跡

`nablarch-testing` リポジトリの `src/main/java` から「テストデータ構造に直接影響するクラス」29件を選定し、各クラスを全行走査した。

- **選定根拠**: `docs/ntf-coverage-class-list.md`（P4-0/P4-1）
- **走査記録**: `docs/ntf-coverage-spec-mapping.md`（P4-2）— クラスごとに行番号付きで「仕様あり / 対象外（理由）」を記録
- **走査対象29クラスの完全一覧**:

| パッケージ | クラス数 | 代表クラス |
|---|---|---|
| `nablarch.test.core.reader` | 14 | `DataType`, `TestDataParsingTemplate`, `GroupDataParsingTemplate`, `SingleDataParsingTemplate`, `HeaderLine`, `TableDataParser`, `ListMapParser`, `MessageParser`, `SendSyncMessageParser`, `GroupMessageParser`, `DataFileParser`, `FixedLengthFileParser`, `VariableLengthFileParser`, `BasicTestDataParser` |
| `nablarch.test.core.file` | 9 | `DataFile`, `DataFileFragment`, `FixedLengthFile`, `FixedLengthFileFragment`, `VariableLengthFile`, `VariableLengthFileFragment`, `BasicDataTypeMapping`, `LineSeparator`, `MockMessages` |
| `nablarch.test.core.messaging` | 3 | `RequestTestingMessagingClient`, `SendSyncSupport`, `MockMessagingClient` |
| `nablarch.test.core.db` | 1 | `TableData` |
| `nablarch.test.core.util.interpreter` | 7 | `NullInterpreter`, `QuotationTrimmer`, `DateTimeInterpreter`, `LineSeparatorInterpreter`, `BinaryFileInterpreter`, `BasicJapaneseCharacterInterpreter`, `CompositeInterpreter` |
| `nablarch.test.core.util.generator` | 2 | `BasicJapaneseCharacterGenerator`, `JapaneseCharacterSet` |

### 3.2 「全行走査」がなぜ必要だったか

初期版のスキーマ（P4-2 以前）は「目立つメソッドを拾う方式」で作成しており、全行を見たという証明がなかった。  
そのため第2回レビュー後に P4-1/P4-2/P4-3 を再実施し、全行を漏れなくカバーした形で作り直した。

旧方式で見逃していた仕様の例:
- `record-length` の自動計算（`FixedLengthFile#createLayout()`）
- `field-separator: "\\t"` のタブ変換（`VariableLengthFile#convertDirectiveValue()`）
- フィールド名重複チェック（`DataFileFragment#setNames()`）
- `"-"` 長フィールドの最大バイト長決定（`DataFileFragment#addValue()`）

### 3.3 スキーマ各要素のコードへの対応

主要なスキーマ要素とそのコード上の根拠を示す。

| スキーマ要素 | 根拠クラス・メソッド | 内容 |
|---|---|---|
| トップレベルキー名（`setup_tables` 等） | `DataType.java`（全 enum 値） | 13種のセクション識別キー名を完全列挙 |
| GroupData系 vs SingleData系の区別 | `GroupDataParsingTemplate.java:26-43` / `SingleDataParsingTemplate.java:27-41` | GroupData: 同一groupIdのセクションを全件収集; SingleData: 最初の1件のみ取得 |
| `group_id` の書式制約（`minLength: 1`） | `GroupDataParsingTemplate#isTargetType()` | `groupId` が空文字でも `TYPE_NAME=value` とマッチしてしまう誤マッチ防止 |
| マーカーカラム `"[COLNAME]"` 構文 | `HeaderLine.java:87-96`（`MARKER_COLUMN_CONDITION`） | `[` で始まり `]` で終わるカラム名をマーカーとして除外 |
| テーブル系 `rows` がオブジェクト配列 | `TableDataParser.java:84-98` | カラム名 → 値 の Map として格納 |
| ファイル系 `rows` が配列の配列 | `DataFileParser`（状態機械） | `fields` と同順の値配列として格納 |
| 可変長では `field_def.length` 省略可 | `VariableLengthFileParser`（長さ行スキップ実装） | 可変長は長さ行を読まない |
| `null` はネイティブ YAML null を使用 | `NullInterpreter.java`（`equalsIgnoreCase("null")`） | 文字列 `"null"` を渡した場合も Java null に変換される（NullInterpreter の変換は YAML でも再現可能） |
| `record_type` の値は messaging では無視 | `MessageParser.java`（`onReadingNames()` オーバーライド） | 先頭セルを常に `"default"` に置き換え。`record_type` は識別用のみ |
| `directives.field-separator: "\\\\t"` | `VariableLengthFile#convertDirectiveValue()` | `"\\t"` 2文字文字列をタブ文字 U+0009 に変換 |
| `directives.record-separator` のシンボル形式 | `LineSeparator.java`（enum）+ `LineSeparator.evaluate()` | `"CRLF"` / `"LF"` / `"CR"` / `"NONE"` を有効値として列挙 |
| `field_def.type` の pattern（`^[A-Z][A-Z0-9_]*$`） | `BasicDataTypeMapping.java`（22種の型記号一覧） | 標準型 10種 + カスタム型（`TEST_X9` 等のアンダースコアを含む型も許容） |
| `field_def.length: "-"` | `DataFileFragment.java`（`ONDEMAND_CALC_FIELD_SIZE`定数） | 全レコード中の最大バイト長で動的決定 |
| `BasicJapaneseCharacterInterpreter` の14トークン | `JapaneseCharacterSet.java`（enum全値） | `半角英字` 〜 `外字` の14種が有効 |

---

## 4. 根拠2 — 公式解説書との照合

### 4.1 照合の目的

コード調査はフレームワーク内部の動作を明らかにするが、「ユーザが使うべき記法」「ユーザが踏みやすい罠」はコードから読み取りにくい。  
公式解説書は「Nablarch が意図した使われ方」の記述であるため、コード調査と合わせて照合した。

### 4.2 照合したドキュメント（13ファイル）

| ファイル | 関連度 | 主な情報 |
|---|---|---|
| `06_TestFWGuide/01_Abstract.rst` | 高 | Excel 命名規約・シート構造・データタイプ一覧・特殊値記法 |
| `06_TestFWGuide/02_DbAccessTest.rst` | 高 | SETUP/EXPECTED_TABLE の記述方法・デフォルト値・Timestamp 書式 |
| `06_TestFWGuide/03_Tips.rst` | 高 | グループID・LIST_MAP・特殊値・空ファイル定義 |
| `06_TestFWGuide/RequestUnitTest_batch.rst` | 高 | 固定長/可変長ファイルのデフォルトディレクティブ・空ファイル定義 |
| `06_TestFWGuide/RequestUnitTest_send_sync.rst` | 高 | EXPECTED_REQUEST_HEADER/BODY_MESSAGES の Excel 書式 |
| `05_UnitTestGuide/02_RequestUnitTest/send_sync.rst` | 高 | no 列・フィールド名重複禁止・マルチレコード送信 |
| `05_UnitTestGuide/02_RequestUnitTest/http_send_sync.rst` | 高 | file-type によるアサート方式切り替え・HTTP 行長制約 |
| `05_UnitTestGuide/02_RequestUnitTest/batch.rst` | 高 | 0x プレフィクスバイナリ・X9/SX9 型の記述注意 |
| （その他5ファイル） | 低〜中 | テスト実行方法など（テストデータ記述仕様への直接影響なし） |

- **照合チェックリスト**: `docs/ntf-coverage-doc-check.md`

### 4.3 照合で発見した仕様（コード調査で見えにくかった点）

解説書からのみ判明した仕様 17件（Doc-1〜Doc-17）を発見し、全件をスキーマ・設計文書・examples.yaml に反映した。

代表的なもの:

| ID | 仕様 | 反映先 |
|---|---|---|
| Doc-3 | `java.sql.Timestamp` の期待値は末尾 `.0` が必須（例: `"2010-01-01 12:34:56.0"`） | design.md §7 |
| Doc-4 | 同一ファイル内で `expected_tables` と `expected_complete_tables` を混在させると後半が読み込まれない | design.md §4 |
| Doc-10 | 空ファイル（0バイト）は `records: []` で表現。`minItems` を 0 に変更が必要 | schema.json |
| Doc-11 | `"0x4AD"` 形式でバイナリ値を直接記述可能 | examples.yaml |
| Doc-13 | マルチレコード送信テストではヘッダと本文の行数を一致させる必要がある | design.md §11 |

### 4.4 コード調査と解説書の不一致

照合の結果、1件の意図的な不一致を確認した:

- **`BasicDefaultValues` のデフォルト日付値**: 解説書は `1970-01-01 00:00:00.0`（UTC基準の記載）。  
  コードは `JVM タイムゾーン依存`（JST 環境では `1970-01-01 09:00:00.0`）。  
  → **design.md の記載（JVM タイムゾーン依存）が正確**。解説書の記載は UTC 環境での実行値を例示したと解釈する。

---

## 5. 根拠3 — 専門家レビュー（5回のレビューループ）

スキーマ・設計文書・examples.yaml に対し、独立した複数の観点から計5回のレビューを実施した。  
「本質的な指摘がなくなるまで修正 → レビューを繰り返す」という方針で進めた。

### 5.1 レビューの観点

| 観点 | 主な確認内容 |
|---|---|
| **実装整合性** | コードの動作とスキーマ定義が矛盾していないか |
| **JSON Schema 品質** | 型定義・required・additionalProperties・enum/pattern が適切か |
| **AI 可読性** | AI がスキーマを参照してテストデータを生成できるか |
| **開発者 UX** | 移行手順・ビフォーアフター例が実務で使えるか |

### 5.2 各レビュー回の主要指摘と対応

| 回 | 主な発見・指摘 | 対応 |
|---|---|---|
| 第1回 | `directives` に固定長用7キー・可変長用6キーが欠落（`additionalProperties: false` のためバリデーションエラー） | 全ディレクティブキーを schema.json に追加 |
| 第2回 | `field_def.type` を enum で制約可能 / `group_message_data.group_id` を required に追加 | type を enum に変更（後に pattern に再変更）|
| 第3回 | 固定長ビフォーアフター例にパディングが含まれており examples.yaml と矛盾 | design.md・examples.yaml 両方からパディングを除去 |
| 第4回 | 独立再レビューにより `group_message_data.group_id` が required になっている重大バグを発見（MockMessagingContext/Client 経路では group_id 不要） | required から group_id を削除し、2つのアクセスパス（A/B）を設計文書で明示 |
| 第5回 | `expected_request_header_messages` の13 DataType 全網羅例が不足・`errorMode` の説明が不十分 | examples.yaml・design.md に詳細例と説明を追加 |

### 5.3 レビュー後の収束確認

第5回レビュー後、全レビュアーが「本質的な問題なし（合格）」と判定した。  
その後 P4-4（Java/QA エキスパート）レビューでも同様に「本質的な問題なし」を確認。

---

## 6. 成果物と根拠の対応マトリクス

| スキーマ要素 | コード走査 | 解説書照合 | レビュー |
|---|:---:|:---:|:---:|
| トップレベルキー（13種）の名称と区別 | ✓ | ✓ | ✓ |
| GroupData vs SingleData の取得方式 | ✓ | — | ✓ |
| `group_id` の必須/任意・`minLength: 1` | ✓ | — | ✓（第4回で重大バグ修正） |
| マーカーカラム `"[COLNAME]"` 構文 | ✓ | ✓ | ✓ |
| テーブル系 rows: オブジェクト配列 | ✓ | ✓ | ✓ |
| ファイル系 rows: 配列の配列 | ✓ | ✓ | ✓ |
| `directives` の全有効キー | ✓ | ✓ | ✓（第1回で欠落を検出） |
| `field_def.type` の pattern | ✓ | ✓ | ✓ |
| `file_data.records: minItems: 0` | — | ✓（Doc-10） | — |
| `null` の仕様 | ✓ | ✓ | ✓ |
| 特殊値記法（`${...}` 等） | ✓ | ✓ | ✓ |
| `java.sql.Timestamp` 末尾 `.0` | — | ✓（Doc-3） | — |
| QuotationTrimmer の `"""`・スペース記法 | ✓ | ✓（Doc-8） | — |
| データタイプ混在禁止 | — | ✓（Doc-4） | — |
| マルチレコード送信のヘッダ繰り返し | — | ✓（Doc-13） | — |
| `0x` プレフィクスバイナリ直接記述 | — | ✓（Doc-11） | — |
| BasicJapaneseCharacterInterpreter 14トークン | ✓ | ✓（解説書は11種のみ記載） | ✓ |

---

## 7. 残存する不確実性

正確性の根拠を示す一方で、以下の限界・不確実性を認識している。

### 7.1 nablarch-core-dataformat への依存

ディレクティブの動作仕様（`FixedLengthDataRecordFormatter` 等）は `nablarch-core-dataformat` で定義されており、本スキーマはこのライブラリのソースコードを直接調査していない。  
スキーマ上の記述は `nablarch-testing` 側のコードが参照している enum 値・Javadoc・定数名から推定したものである。  
将来バージョンで `nablarch-core-dataformat` のディレクティブ仕様が変更された場合はスキーマの更新が必要になる。

### 7.2 nablarch-example-*-ntf-yaml との差異

先行実装例（javajavawhale 氏の `nablarch-example-{batch,web,rest}-ntf-yaml`）はフラット変換方式（Excel の列をそのまま YAML に展開）を採用しており、本スキーマの構造化方式とは異なる。  
どちらが「正しい」かではなく、本スキーマはパーサクラスの入力構造を忠実に表現することを目的として設計した。

### 7.3 テストカバレッジのない仕様

解説書・コードから導いた仕様の一部は、実際に YAML パーサを実装して検証したものではない。  
スキーマ設計は「仕様の宣言」であり、YAML アダプタ実装時に追加の不整合が判明する可能性がある。

---

## 8. 主要ドキュメント一覧

| ドキュメント | 内容 | 根拠層 |
|---|---|---|
| `ntf-testdata-yaml-schema.json` | JSON Schema 定義（成果物） | — |
| `ntf-testdata-yaml-design.md` | 設計判断・トレードオフ・注意事項（成果物） | — |
| `ntf-testdata-yaml-examples.yaml` | 各データ種別の YAML 記述例（成果物） | — |
| `ntf-testdata-structure.md` | Phase 1 コード調査報告（全クラスの構造解析） | 根拠1 |
| `ntf-coverage-class-list.md` | 対象クラス一覧・分類（P4-0/P4-1） | 根拠1 |
| `ntf-coverage-spec-mapping.md` | 全行走査・仕様マッピング（P4-2、29クラス） | 根拠1 |
| `ntf-coverage-doc-check.md` | 公式解説書 × スキーマ 照合チェックリスト（D-5、17件） | 根拠2 |
| `ntf-yaml-impl-evaluation.md` | 先行実装例リポジトリ評価レポート | 参考 |
