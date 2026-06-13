# NTF テストデータ仕様 カバレッジ スペックマッピング（P4-2 再実施版）

NTF 本体クラスを全行走査し、YAML スキーマ・設計文書へ未反映の仕様を洗い出した記録。読み手は反映作業の担当者と、反映の根拠を遡って検証する監査者。担当者は「どの未反映仕様を、どの優先度で、どの文書のどこに反映するか」を §1 で決め、各仕様の根拠（クラス名・行番号）は §4 のクラス別表を引いて確認する。通読より参照を前提とする。

- **作成日**: 2026-05-15（P4-2 再実施）
- **走査対象**: 29 クラス（`src/main/java` の直接影響クラス）
- **参照文書**: `ntf-coverage-class-list.md` §1

---

## 1. 反映すべき未反映仕様（推奨）

全行走査で確認した未反映仕様を、反映先と優先度で整理する。優先度の根拠は各行の「理由」列、仕様の根拠（クラス・行番号）は §4 の該当クラスを引く。各仕様の詳細は §3 を参照。

優先度の基準: 誤った記述でユーザーが誤動作を期待しうる／頻出のテストデータ記法／利用時に必須の情報を **高**、一般的ユースケースで挙動差が問題になるものを **中**、高度なカスタマイズ向けを **低** とする。

### schema.json への追加

| # | 優先度 | 追加箇所 | 内容 | 理由 |
|---|---|---|---|---|
| S-1 | 高 | `$defs.directives.properties.record-length` description | `record-length` は固定長ファイルのフィールド長合計から自動計算されるため**通常は記述不要** | 手動設定不要な旨が未記載 |
| S-2 | 中 | `$defs.directives.properties.field-separator` description | `"\\t"` を指定するとタブ文字（U+0009）に変換される。値は1文字のみ有効 | タブ区切りファイルは一般的なユースケース |
| S-5 | 中 | `$defs.table_data.properties.rows` description | `SETUP_TABLE` / `EXPECTED_TABLE` でも省略カラムには `DefaultValues` によるデフォルト値が INSERT 時に補完される | `SETUP_TABLE` でも補完されることを知らないと誤ったテストになる |
| S-3 | — | `$defs.record_fragment.properties.fields` description | 同一レコード種別内のフィールド名は重複不可 | — |
| S-4 | — | `$defs.field_def.properties.length` description（const:"-" 部分） | `"-"` を指定したフィールドの値は格納時に改行コードと前後空白が除去される | — |

### design.md への追加

| # | 優先度 | 追加箇所 | 内容 | 理由 |
|---|---|---|---|---|
| D-6 | 高 | AI向けプロンプト §BasicJapaneseCharacterInterpreter | 「書式 `${...,...}` にマッチしない場合はスルー。書式はマッチするが文字種が未知の場合は `IllegalArgumentException` がスローされる」に修正（旧: 「スペルミスは素通り」は不正確） | 現在の記述が誤っており、ユーザーが誤動作を期待する |
| D-3 | 高 | §7 または §4 日付フォーマット | 日付型カラムは17文字未満でも後置0埋めで処理される（例: `"20240101"` も有効）。JDBC タイムスタンプエスケープ形式（`"2024-01-01"` 等）も受け付ける。さらに `"yyyyMMdd HHmmss"`（スペース区切り14文字）および `"yyyyMMddHHmmssS"`（ミリ秒1桁15文字）も有効 | テストデータ作成時によく使われる書き方 |
| D-4 | 高 | §4 `expected_complete_tables` の説明 | `BasicDefaultValues` のデフォルト値一覧を表形式で追記。DATE のデフォルトは `new Timestamp(0L)` を JVM タイムゾーンで文字列化した値（JST 環境では `"1970-01-01 09:00:00.0"`、UTC 環境では `"1970-01-01 00:00:00.0"`）。`CHAR`/`NCHAR` はカラム長分スペース、`VARCHAR`/`NVARCHAR` は常に1スペース。`"半角数字"` → `X`（`Z` ではない）を注記 | `expected_complete_tables` 利用時に必須の情報 |
| D-7 | 中 | AI向けプロンプト §文字種トークン | `${半角記号}` 生成では `"`, `#`, `,`, `\` は含まれない | テストデータ生成で予期しない文字列になる |
| D-10 | 中 | §ファイル系 注意事項（新規追加） | 可変長ファイルの空行はスキップされず全フィールド `""` のレコードとして保持される。固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される | 固定長と可変長で挙動が異なることは重要 |
| D-14 | 中 | §ファイル系 注意事項（新規） | 1つのファイルセクション内にフィールド名行→型行→[長さ行]→データ行のブロックを複数連続して記述することで複数レコードレイアウトを表現できる | 1セクション内に複数レコードレイアウトを持つファイルの YAML 化方法が不明 |
| D-9 | 低 | 新節「デフォルトディレクティブの DI」 | SystemRepository キー `defaultDirectives`（全共通）、`fixedLengthDirectives`（固定長専用、後者が優先上書き）、`variableLengthDirectives`（可変長専用）でデフォルトディレクティブを一括設定できる | 高度なカスタマイズポイント。実用ユーザーの多くは不要 |
| D-1 | — | §7 特殊値 null テーブル | `NullInterpreter` は大文字小文字不問（`"NULL"` / `"Null"` も null になる） | — |
| D-2 | — | §7 特殊値 QuotationTrimmer | 全角ダブルクォート（U+201C/U+201D）でも外側クォートが除去される。半角は先頭・末尾が同じ `"` (U+0022) のペア、全角は先頭が `"` (U+201C) かつ末尾が `"` (U+201D) という**異なる文字のペア**で判定（片側のみはスルー）。`""abc""` → `"abc"` | — |
| D-5 | — | §11 MESSAGE 系 record_type 説明の近くに追記 | Excel 上の FW 制御ヘッダは「フィールド名｜値」の2列ディレクティブ行形式だったが YAML では通常の `fields` に統合される | — |
| D-8 | — | AI向けプロンプト §field-separator 追加 | `"\\t"` でタブ区切りを指定できる | — |
| D-11 | — | §LIST_MAP 注意事項 | 同一シート内に同じ `LIST_MAP=id` セクションが複数存在する場合、最初の1つのみが読まれる（後続は黙って無視） | — |
| D-12 | — | §9 group_id の説明に補足 | 存在しない groupId を指定した場合は例外でなく空リストが返る | — |
| D-13 | — | §11 messaging に追補 | テストデータにステータスコード列がない場合デフォルト `"200"` が使用される。EXPECTED_REQUEST_HEADER_MESSAGES と EXPECTED_REQUEST_BODY_MESSAGES の行数一致が必須 | — |
| D-15 | — | §特殊値 §DateTimeInterpreter | `${systemTime}` 等は完全一致のみ変換。部分文字列（例: `"${systemTime}_suffix"`）は変換されないため `CompositeInterpreter` との組み合わせが必要 | — |
| D-16 | — | §ファイル系 `"-"` 長フィールド | `"-"` 長フィールドの最終的な長さは、追加された全レコード中の**最大バイト長**になる（各レコード追加時にバイト長が比較更新される） | — |

### examples.yaml への追加

| # | 追加内容 |
|---|---|
| E-1 | `field-separator: "\\t"` を使ったタブ区切りファイルの directives 例 |
| E-2 | `type: B`（バイナリ型）の `field_def` 使用例（`${binaryFile:...}` との組み合わせ） |
| E-3 | JDBC タイムスタンプ形式の日付値の例（`"2024-01-01"` など） |
| E-4 | `response_*_messages` の通常データ行（errorMode なし）の例 |

> 優先度 — の行は §7 で優先順位を付与していない仕様。反映自体は必要。

---

## 2. 走査方法（評価基準）

旧版は「目立つメソッドのみ拾った」漏れがあった。これを解消するため、各クラスを全行走査し、行番号付きで「仕様あり」「対象外」を記録する。全行を漏れなく分類することで、未反映仕様の見落としがないことを担保する。

| 区分 | 定義 |
|---|---|
| 仕様あり | YAMLスキーマの構造・有効値・必須/任意・セクション識別・行順序・特殊値などに影響する行 |
| 対象外 | 内部実装・ログ・例外ハンドリング・getter/setter 等、スキーマ設計に直接影響しない行 |

§4 のクラス別表が走査の生データ（測定値）。§1・§3 の判断（反映先・優先度）はこの測定値から導く。

---

## 3. 未反映仕様の詳細

§1 の各仕様の根拠と注意点。クラス別の行レベル根拠は §4 を引く。

### 特殊値・インタープリタ

- **D-1 `NullInterpreter`**: `"null"`（半角小文字）を `equalsIgnoreCase` で比較するため `"NULL"` / `"Null"` も null になる。根拠 §4 NullInterpreter 10-11。
- **D-2 `QuotationTrimmer`**: 半角は先頭・末尾が同じ `"` (U+0022) のペア、全角は先頭 `"` (U+201C)・末尾 `"` (U+201D) という異なる文字のペアで判定。両立必須で片側のみはスルー。`""abc""` → `"abc"`（最外側1層のみ除去）。根拠 §4 QuotationTrimmer 18-30。
- **D-6 `BasicJapaneseCharacterInterpreter`**: 書式 `${文字種,文字数}` に完全一致しない場合は次のインタープリタへスルー。書式はマッチするが文字種が未知の場合は `BasicJapaneseCharacterGenerator` 側が `IllegalArgumentException` をスロー。旧記述「スペルミスは素通り」は不正確。根拠 §4 BasicJapaneseCharacterInterpreter 25-37。
- **D-7 `${半角記号}` の除外文字**: `ASCII_SYMBOL` はダブルクォート `"`・シャープ `#`・カンマ `,`・バックスラッシュ `\` の4文字を除外する。根拠 §4 JapaneseCharacterSet 22-36。
- **D-15 `DateTimeInterpreter`**: `${systemTime}` / `${updateTime}` / `${setUpTime}` は完全一致のみ変換。部分文字列は変換されないため `CompositeInterpreter` との組み合わせが必要。根拠 §4 DateTimeInterpreter 48-55。

### 日付・テーブル

- **D-3 日付フォーマット**: 17文字未満は後置0埋めで処理（`"20240101"` も有効）。JDBC タイムスタンプエスケープ形式（`"2024-01-01"` 等）、`"yyyyMMdd HHmmss"`（スペース区切り14文字）、`"yyyyMMddHHmmssS"`（ミリ秒1桁15文字）も有効。根拠 §4 TableData 214-273。
- **D-4 `BasicDefaultValues` デフォルト値**: DATE は `new Timestamp(0L)` を JVM タイムゾーンで文字列化（JST `"1970-01-01 09:00:00.0"`、UTC `"1970-01-01 00:00:00.0"`）。`CHAR`/`NCHAR` はカラム長分スペース、`VARCHAR`/`NVARCHAR` は常に1スペース。`"半角数字"` → `X`（`Z` ではない）。根拠 §4 TableData 701-745、BasicDataTypeMapping 30-56。[要確認] `BasicDefaultValues` クラスは本走査の29クラスに含まれず、デフォルト値一覧の全項目は未確認。
- **S-5 省略カラムのデフォルト補完**: `SETUP_TABLE` / `EXPECTED_TABLE` でも省略カラムに `DefaultValues` が INSERT 時に補完される。根拠 §4 TableData 180-212、BasicTestDataParser 170-181。
- **D-12 存在しない groupId**: 例外でなく空リストが返る。根拠 §4 BasicTestDataParser 49-57（`isDataExisting` = false で空リスト）。

### ファイル系

- **S-1 `record-length` 自動計算**: 固定長ファイルのフィールド長合計から自動計算されるため通常は記述不要。根拠 §4 FixedLengthFile 60-92。
- **S-2 / S-8 `field-separator: "\\t"`**: `"\\t"` でタブ文字（U+0009）に変換。1文字のみ有効（違反は `IllegalArgumentException`）。根拠 §4 VariableLengthFile 62-82。
- **S-3 フィールド名重複不可**: 同一レコード種別内のフィールド名は重複不可。根拠 §4 DataFileFragment 185-194, 348-362。
- **S-4 / D-16 `"-"` 長フィールド**: 値は格納時に改行コードと前後空白を除去（S-4）。最終的な長さは追加された全レコード中の最大バイト長になる（D-16）。根拠 §4 DataFileFragment 97-115, 129-152, 154-161。
- **D-9 デフォルトディレクティブ DI**: SystemRepository キー `defaultDirectives`（全共通）→ `fixedLengthDirectives` / `variableLengthDirectives`（ファイル種別専用、後者が優先上書き）の順で適用。根拠 §4 DataFile 59-81、FixedLengthFile 19-27、VariableLengthFile 22-31。
- **D-10 空行の挙動差**: 可変長はスキップされず全フィールド `""` のレコードとして保持、固定長はスペースパディングされた定長レコードとして書き出される。[要確認] この挙動差の根拠は §4 の走査範囲（Parser 層）だけでは確定できず、書き出し処理側の確認が必要。
- **D-14 複数レコードレイアウトの連続記述**: 1セクション内にフィールド名行→型行→[長さ行]→データ行のブロックを複数連続して記述できる。根拠 §4 DataFileParser 177-191。

### メッセージ系

- **D-5 FW 制御ヘッダ**: Excel では「フィールド名｜値」の2列ディレクティブ行形式だったが YAML では通常の `fields` に統合される。根拠 §4 MessageParser 29-30, 77-92。
- **D-11 LIST_MAP の重複セクション**: 同一シート内に同じ `LIST_MAP=id` が複数あると最初の1つのみ読まれる（後続は無視）。根拠 §4 ListMapParser 15、SingleDataParsingTemplate 43-53。
- **D-13 messaging**: ステータスコード列がない場合デフォルト `"200"`。EXPECTED_REQUEST_HEADER_MESSAGES と EXPECTED_REQUEST_BODY_MESSAGES の行数一致が必須。根拠 §4 RequestTestingMessagingClient 124-204, 294-443。

---

## 4. クラス別 全行走査結果（根拠）

§1・§3 の各仕様の行レベル根拠。クラスを引いて該当行を確認する。

### 4.1 reader パッケージ

#### DataType（92行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1 | 対象外 | パッケージ宣言 |
| 3-7 | 対象外 | クラス Javadoc・author |
| 8 | 対象外 | enum 宣言 |
| 10-11 | 仕様あり | `DEFAULT`（値 `"DEFAULT"`）: どのタイプにも属さないデフォルト値 |
| 13-14 | 仕様あり | `SETUP_TABLE`（値 `"SETUP_TABLE"`）: 事前準備用テーブルデータのセクション識別キー |
| 16-17 | 仕様あり | `EXPECTED_TABLE`（値 `"EXPECTED_TABLE"`）: 期待値テーブルデータのセクション識別キー |
| 19-23 | 仕様あり | `EXPECTED_COMPLETE_TABLE`（値 `"EXPECTED_COMPLETE_TABLE"`）: 更新用期待値テーブル。省略カラムにはデフォルト値が設定される |
| 25-29 | 仕様あり | `LIST_MAP`（値 `"LIST_MAP"`）: `List<Map<String,String>>` 形式データ |
| 31-32 | 仕様あり | `SETUP_FIXED`（値 `"SETUP_FIXED"`）: 事前準備用固定長ファイルのセクション識別キー |
| 34-35 | 仕様あり | `EXPECTED_FIXED`（値 `"EXPECTED_FIXED"`）: 期待値固定長ファイルのセクション識別キー |
| 37-38 | 仕様あり | `SETUP_VARIABLE`（値 `"SETUP_VARIABLE"`）: 事前準備用可変長ファイルのセクション識別キー |
| 40-41 | 仕様あり | `EXPECTED_VARIABLE`（値 `"EXPECTED_VARIABLE"`）: 期待値可変長ファイルのセクション識別キー |
| 43-44 | 仕様あり | `MESSAGE`（値 `"MESSAGE"`）: メッセージセクション識別キー |
| 46-47 | 仕様あり | `EXPECTED_REQUEST_HEADER_MESSAGES`（値 `"EXPECTED_REQUEST_HEADER_MESSAGES"`）: 要求電文ヘッダ期待値セクション |
| 49-50 | 仕様あり | `EXPECTED_REQUEST_BODY_MESSAGES`（値 `"EXPECTED_REQUEST_BODY_MESSAGES"`）: 要求電文本文期待値セクション |
| 52-53 | 仕様あり | `RESPONSE_HEADER_MESSAGES`（値 `"RESPONSE_HEADER_MESSAGES"`）: 応答電文ヘッダセクション |
| 55-56 | 仕様あり | `RESPONSE_BODY_MESSAGES`（値 `"RESPONSE_BODY_MESSAGES"`）: 応答電文本文セクション |
| 58-73 | 対象外 | フィールド宣言・コンストラクタ（内部実装） |
| 75-91 | 対象外 | `getType()` / `getName()` getter（定型コード） |
| 92 | 対象外 | クラス終端 |

#### TestDataParsingTemplate（337行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-14 | 対象外 | パッケージ宣言・import文 |
| 16-22 | 対象外 | クラス Javadoc・abstract クラス宣言 |
| 24-47 | 対象外 | フィールド宣言（reader, interpreters, targetType, キャッシュ Map, testData, index, directory, resource） |
| 49-53 | 対象外 | abstract `onReadLine()` シグネチャ |
| 55-60 | 対象外 | abstract `onTargetTypeFound()` シグネチャ |
| 62-70 | 仕様あり | abstract `isTargetType(line, id)`: 行が対象 DataType かつ ID が一致するかを判定するポリシー。サブクラスが単一/グループ取得の差異を実装する |
| 72-77 | 仕様あり | abstract `shouldStopOnNextOne()`: 次の対象セクション検出で停止するか否かのポリシー（単一取得 vs 複数取得の分岐点） |
| 79-84 | 対象外 | abstract `getResult()` シグネチャ |
| 86-97 | 対象外 | コンストラクタ（内部実装） |
| 99-106 | 対象外 | `getTargetType()` getter |
| 108-158 | 対象外 | `parse(directory, resource, id)` / `parse(..., saveCache)`: キャッシュ付き読み込み委譲（内部実装） |
| 160-186 | 仕様あり | `readTestData()`: (1) 行172-174: 先頭セルが `//` で始まる行はコメント行としてスキップ（行コメント仕様）; (2) 行175: `cutComment(line)` — 先頭以外のセルが `//` で始まる場合そのセル以降を切り捨て（行内コメント仕様）; (3) 行176-178: `isBlankLine(line)` — 全要素が null または空文字の行はスキップ（空行スキップ仕様）; (4) 行179: `interpret(line)` — インタープリタによる特殊値展開 |
| 187-219 | 仕様あり | `parse(id)`: (1) 行198-199: 先頭セルで DataType を判定; (2) 行201-205: `isTargetType` 真 → `onTargetTypeFound` 呼び出し、`shouldStopOnNextOne` 真なら停止（単一取得の停止条件）; (3) 行207-210: DataType が DEFAULT（データ行）かつ読み込み中なら `onReadLine` 呼び出し; (4) 行212-216: 別セクション開始検出で読み込みを終了（セクション境界は次セクション開始行が自動的に区切り） |
| 221-242 | 仕様あり | `getDataType(dataTypeCell)`: セル値が DataType の `getName()` で **前方一致**（`startsWith`）するかどうかで型を決定。null は `DEFAULT` を返す（前方一致のためセル値は識別キー + 追加文字でも認識される） |
| 244-253 | 仕様あり | `getTypeValue(dataTypeRow)`: 先頭セルの `=` 以降の文字列をID値として取得。セクション識別子の書式 `<DataTypeName>[groupId]=<value>` を前提とする |
| 254-266 | 対象外 | `readLine()`: テストデータインデックス管理（内部実装） |
| 268-291 | 仕様あり | `COMMENT_EXPRESSION = "//"` 定数・`isCommentRow()` / `isComment()`: 先頭セルが `//` で始まる行をコメント行とみなすルール |
| 292-308 | 仕様あり | `cutComment(src)`: 1行データを走査し `//` で始まるセルが出現した時点でそれ以降を切り捨てて返す（行内コメント切り捨て仕様） |
| 310-318 | 仕様あり | `isBlankLine(line)`: 全要素が null または空文字かを判定し空行とみなす |
| 319-335 | 対象外 | `interpret()`: インタープリタ委譲（内部実装） |
| 336-337 | 対象外 | クラス終端 |

#### GroupDataParsingTemplate（55行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-5 | 対象外 | パッケージ宣言・import文 |
| 7-13 | 対象外 | クラス Javadoc・クラス宣言 |
| 14-24 | 対象外 | コンストラクタ（内部実装） |
| 26-43 | 仕様あり | `isTargetType(line, groupId)`: 先頭セルが `<DataTypeName><groupId>=` で**前方一致**する場合に真。セクション識別子の書式: `SETUP_TABLE<groupId>=<テーブル名>` のように DataType名 + groupId + `=` の連結。`=` 以降は何でもよい |
| 45-53 | 仕様あり | `shouldStopOnNextOne()` が常に `false`: 同一 groupId のセクションが複数存在しても全部収集し続ける（複数テーブルを1シートに並べられる） |
| 54-55 | 対象外 | クラス終端 |

#### SingleDataParsingTemplate（55行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-5 | 対象外 | パッケージ宣言・import文 |
| 7-14 | 対象外 | クラス Javadoc・クラス宣言 |
| 15-25 | 対象外 | コンストラクタ（内部実装） |
| 27-41 | 仕様あり | `isTargetType(line, id)`: DataType が一致 **かつ** `getTypeValue(line)` 取得値（`=` 以降の文字列）が id と **完全一致** する場合に真。書式: `<DataTypeName>=<id>` |
| 43-53 | 仕様あり | `shouldStopOnNextOne()` が常に `true`: 最初の一致セクションを読み終えたら次の同型セクションが現れた時点で停止（同一シート内に同じID のセクションが複数あっても最初の1つのみ読まれる） |
| 54-55 | 対象外 | クラス終端 |

#### HeaderLine（97行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-13 | 対象外 | パッケージ宣言・import文 |
| 15-16 | 対象外 | クラス Javadoc・クラス宣言 |
| 18-25 | 対象外 | フィールド宣言（keys, markerIndices, effectiveColumnNames） |
| 27-42 | 仕様あり | コンストラクタ: (1) 行33: `trimTailCopy(headerLine)` — 末尾の空要素（null または空文字）を除去してキーリスト構築（**末尾カラム省略可**の仕様); (2) 行34-36: `null` 返却時は空リストで代替（ヘッダ行自体が null/空でも安全に処理）; (3) 行40: マーカーカラムのインデックスを収集; (4) 行41: マーカーカラムを除外した有効カラム名リストを生成 |
| 44-51 | 対象外 | `getEffectiveColumnNames()` getter |
| 53-67 | 仕様あり | `getMapExcludingMarkerColumns(line)`: マーカーカラムを除外したカラムと値の Map を返す（データ行のマーカーカラム除外仕様） |
| 69-85 | 仕様あり | `excludeMarkerColumns(line)`: マーカーカラムに対応するインデックスをスキップ。行81: データ行がヘッダより短い場合は不足分を空文字 `""` で補完（**右端カラムの値省略が可能**） |
| 87-96 | 仕様あり | `MARKER_COLUMN_CONDITION`: `[` で始まり `]` で終わるカラム名をマーカーカラムとして扱う（マーカーカラムの書式仕様: `[カラム名]` 形式） |
| 97 | 対象外 | クラス終端 |

#### TableDataParser（107行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-17 | 対象外 | パッケージ宣言・import文・Javadoc |
| 18 | 仕様あり | `GroupDataParsingTemplate<List<TableData>>` を継承。グループIDによる複数テーブル収集が可能 |
| 20-37 | 対象外 | フィールド宣言（result, DbInfo, DefaultValues, targetDataType, HeaderLine, 処理中 TableData） |
| 38-57 | 対象外 | コンストラクタ（引数受け渡し・内部初期化） |
| 59-72 | 対象外 | LRU キャッシュ定数・`parse()` キャッシュ制御（内部実装） |
| 74-82 | 仕様あり | `onReadLine`: `header.excludeMarkerColumns(line)` でマーカーカラム（`[xxx]` 形式の列）を除外した行のみを `TableData` に追加。マーカー列はデータとして格納されない |
| 84-98 | 仕様あり | `onTargetTypeFound`: (1) 先頭列の `=` 以降をテーブル名として取得; (2) 直後の次行をカラム名ヘッダ行として読み込む; (3) マーカーカラムを除外した有効カラム名で `TableData` を生成してリストに追加 |
| 100-107 | 対象外 | `getResult()` 定型コード |

#### ListMapParser（79行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-14 | 対象外 | パッケージ宣言・import文・Javadoc |
| 15 | 仕様あり | `SingleDataParsingTemplate<List<Map<String,String>>>` を継承。**1セクションにつき1リストマップのみ**取得（IDが完全一致必須、最初に見つかったら終了） |
| 17-21 | 対象外 | フィールド宣言（result, header） |
| 23-31 | 仕様あり | コンストラクタで `DataType.LIST_MAP` を固定指定。YAMLセクション種別は `LIST_MAP` 固定 |
| 33-53 | 対象外 | LRU キャッシュ定数・`parse()` キャッシュ制御（内部実装） |
| 55-65 | 仕様あり | `onTargetTypeFound`: セクション行（`LIST_MAP=<id>` 行）の**直後の1行をヘッダ行（キー名一覧）として読み込む** |
| 67-72 | 仕様あり | `onReadLine`: `header.getMapExcludingMarkerColumns(line)` でマーカーカラムを除外した `Map<String,String>` を生成してリストに追加。`LIST_MAP` でもマーカーカラムは除外される |
| 73-79 | 対象外 | `getResult()` 定型コード |

#### MessageParser（150行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-23 | 対象外 | パッケージ宣言・import文・Javadoc |
| 24 | 仕様あり | `SingleDataParsingTemplate<MessagePool>` を継承。1セクションで1つのメッセージプールを解析 |
| 26-27 | 対象外 | `delegate` フィールド |
| 29-30 | 仕様あり | `fwHeader` フィールド: FW制御ヘッダのキー→値 Map。Excel では「フィールド名 ｜ 値」の2列ディレクティブ行形式だったが YAML では通常の `fields` に統合される |
| 32-33 | 仕様あり | `FW_HEADER_KEY = "reader.fwHeaderfields"`: SystemRepository にこのキーでカンマ区切り文字列を設定することで FW 制御ヘッダフィールド名をカスタマイズ可能 |
| 35-45 | 対象外 | コンストラクタ（delegate の生成） |
| 60-67 | 仕様あり | `onReadingNames` オーバーライド: フィールド名行の**先頭列（NO列相当）を無条件に `"default"` に書き換えてから**親クラスに渡す。YAMLでは `record_type` が常に `"default"` に固定される仕様 |
| 69-75 | 仕様あり | `onReadingValues` オーバーライド: 空行は無視。データ行は `tail(line)` で**先頭列（NO列）を除去してから値を格納**（NO列はデータとして保存されない） |
| 77-92 | 仕様あり | `processDirectives` オーバーライド: `isFrameworkHeader(fieldName)` が真の場合に `fwHeader` マップへ格納して `true` を返す。**FW制御ヘッダは通常フィールドとは別のマップに分離保存される** |
| 95-110 | 仕様あり | `fwHeaderFields`: デフォルトは `{"requestId", "userId", "resendFlag", "resultCode"}` の4フィールド。SystemRepository の `reader.fwHeaderfields` キーで上書き可能 |
| 112-122 | 対象外 | `onReadLine` / `onTargetTypeFound` 委譲（定型コード） |
| 124-133 | 仕様あり | `getResult`: delegate の結果が空の場合は `null` を返却。非空の場合は先頭要素（index=0）の `FixedLengthFile` を body とし `fwHeader` と組み合わせて `RequestTestingMessagePool` を生成。**1セクションにつき先頭の FixedLengthFile のみが本文として使用される** |
| 135-141 | 対象外 | `getDelegate()` accessor |
| 143-149 | 仕様あり | `getFwHeader()`: FWヘッダマップを返却（`SendSyncMessageParser` でオーバーライドされ使用禁止になる） |

#### SendSyncMessageParser（145行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-15 | 対象外 | パッケージ宣言・import文・Javadoc |
| 16 | 仕様あり | `MessageParser` を継承。同期送信メッセージ専用パーサ |
| 18-19 | 仕様あり | `ERROR_MODE_TIMEOUT = "errorMode:timeout"`: タイムアウトを表す特殊文字列リテラル（YAMLスキーマの有効値） |
| 21-22 | 仕様あり | `ERROR_MODE_MSG_EXCEPTION = "errorMode:msgException"`: メッセージ例外を表す特殊文字列リテラル（YAMLスキーマの有効値） |
| 24-33 | 対象外 | コンストラクタ（親クラスに委譲） |
| 35-44 | 仕様あり | `getFwHeader()` オーバーライド: 必ず `UnsupportedOperationException` をスロー。`SendSyncMessageParser` では FW 制御ヘッダ機能は使用不可 |
| 45-91 | 仕様あり | `ErrorMode` enum: `TIMEOUT`（値=`errorMode:timeout`）と `MSG_EXCEPTION`（値=`errorMode:msgException`）の2値。`isErrorMode(String)` でエラーモード文字列かどうかを判定 |
| 94-96 | 仕様あり | `ERROR_MODE_COLUMN_NUMBER = 1`: エラーモード値が格納される列番号は **1** （0番は NO 列） |
| 98-99 | 仕様あり | `NO_COLUMN_NUMBER = 0`: NO列は列番号0固定 |
| 101-115 | 対象外 | `createFixedLengthFileParser` オーバーライドの外枠（内部実装） |
| 116-118 | 仕様あり | 空行は無視（MessageParser と同様） |
| 120-132 | 仕様あり | **エラーモード行の処理**: 列1にエラーモード文字列が存在する場合、その1値だけ `currentFragment.addValue(list)` する（他フィールドはパースしない）。YAMLでは `errorMode` の特殊値として扱う |
| 133-134 | 仕様あり | **通常データ行の処理**: `temp.remove(NO_COLUMN_NUMBER)` で NO 列（列0）を除去し、`currentFragment.addValueWithId(temp, <NO列の値>)` で NO 列の値を**レコード ID として活用**しながら残りのデータを格納 |
| 137-142 | 仕様あり | `createNewFile` オーバーライド: `FixedLengthFile` でなく `MockMessages` を生成。`errorMode:*` 値に対してパディング除去処理をスキップする実装 |
| 143-145 | 対象外 | クラス終端 |

#### GroupMessageParser（67行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-15 | 対象外 | パッケージ宣言・import文・Javadoc |
| 16 | 仕様あり | `GroupDataParsingTemplate<List<RequestTestingMessagePool>>` を継承。グループID（`RESPONSE_BODY_MESSAGES<groupId>=<名前>` 形式）で複数メッセージプールをまとめて収集できる |
| 18-19 | 仕様あり | `delegate` フィールドに `SendSyncMessageParser` を保持。行の読み込み・処理は `SendSyncMessageParser` に委譲（エラーモード対応・NO列のID化を含む） |
| 21-32 | 対象外 | `onReadLine` / `onTargetTypeFound` 委譲（定型コード） |
| 34-44 | 仕様あり | コンストラクタ: delegate として `SendSyncMessageParser` を生成。グループメッセージパーサの実際の解析ロジックは `SendSyncMessageParser` と同じ |
| 48-65 | 仕様あり | `getResult`: 各 `FixedLengthFile` に対して `emptyMap()` を FWヘッダとして（= FWヘッダなし） `RequestTestingMessagePool` を生成。`messagePoolEx.setRequestId(data.getPath())` で**ファイルパス（セクション識別子 `=` 以降）をリクエストIDとして設定**する |
| 53-54 | 仕様あり | データリストが空の場合は `null` を返却 |
| 57-58 | 仕様あり | `emptyMap()` を FWヘッダとして使用: GroupMessageParser では FW 制御ヘッダは一切使用されない |
| 66-67 | 対象外 | クラス終端 |

#### DataFileParser（268行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-19 | 対象外 | パッケージ宣言・import文・Javadoc |
| 20 | 対象外 | 抽象クラス宣言（型パラメータ T extends DataFile） |
| 22-35 | 対象外 | インスタンス変数（result, currentFile, currentFragment, status, targetType） |
| 38-49 | 仕様あり | **行処理ステータス列挙型 `Status`**: `NONE` → `READING_DIRECTIVES_AND_NAMES`（ディレクティブ＋フィールド名行）→ `READING_TYPES`（型行）→ `READING_LENGTHS`（フィールド長行）→ `READING_VALUES`（データ行）の順に遷移。この遷移順がファイルセクションの行並び順仕様を確定する |
| 51-61 | 対象外 | コンストラクタ（reader, interpreters, targetType 受け取り） |
| 64-87 | 仕様あり | `onReadLine`: 各 status に応じてコールバックを呼び分け。`READING_DIRECTIVES_AND_NAMES`→`onReadingDirectives`、`READING_TYPES`→`onReadingTypes`、`READING_LENGTHS`→`onReadingLengths`、`READING_VALUES`→`onReadingValues` の行順序が確定 |
| 89-109 | 対象外 | LRU キャッシュ定数・キャッシュ付き `parse()` メソッド（内部実装） |
| 111-119 | 仕様あり | `onTargetTypeFound`: セクション識別行（例: `SETUP_FIXED[id]=ファイルパス`）の `=` 以降をファイルパスとして取得し新規ファイルオブジェクトを生成。セクション識別行の構文 `DataType名[groupId]=ファイルパス` が確定 |
| 121-133 | 対象外 | `getResult` / `createNewFile` 抽象メソッド宣言 |
| 135-145 | 仕様あり | `onReadingDirectives`: 先頭列がディレクティブキーであればディレクティブとして処理し、そうでなければフィールド名行として処理。**ディレクティブ行は0行以上、フィールド名行の直前に置く** |
| 147-155 | 仕様あり | `onReadingNames`: 先頭列をレコード種別名、2列目以降をフィールド名リストとして `createNewFragment` に渡す。ステータスを `READING_TYPES` へ遷移。フィールド名行は1行のみ |
| 157-165 | 仕様あり | `onReadingTypes`: 先頭列を除いた列をフィールドデータ型リストとして設定。ステータスを `READING_LENGTHS` へ遷移。型行は1行のみ（固定長の場合） |
| 167-175 | 仕様あり | `onReadingLengths`: 先頭列を除いた列をフィールド長リストとして設定。ステータスを `READING_VALUES` へ遷移。フィールド長行は1行のみ（固定長のみ存在） |
| 177-191 | 仕様あり | `onReadingValues`: 先頭列が空またはリスト自体が空（空行）の場合をデータ行と判断し、先頭列を除いた列をフィールド値として追加。先頭列が非空の場合は新しいフィールド名行（新レコードレイアウト）として扱う。**1セクション内に複数レコードレイアウトを連続記述可能** |
| 193-210 | 仕様あり | `isDataRow`: (1) 行が空、(2) 先頭列が null または空文字 → データ行と判定。**データ行の先頭セルは必ず空にする**という記述ルール |
| 212-232 | 仕様あり | `processDirectives`: 行は最低2列必要（列数 < 2 は例外）。先頭列がディレクティブキーと一致する場合、2列目の値をディレクティブ値として設定。**ディレクティブは `列0=キー名、列1=値` の2列構成** |
| 234-240 | 対象外 | `isDirective` 抽象メソッド宣言 |
| 243-252 | 仕様あり | `createNewFragment`: 先頭列をレコード種別名、2列目以降をフィールド名として設定。**フィールド名行の構造: 先頭列 = レコード種別名、2列目以降 = フィールド名の列挙** |
| 254-267 | 対象外 | `tail()` ユーティリティ（先頭要素除去） |

#### FixedLengthFileParser（39行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-26 | 対象外 | パッケージ宣言・import文・Javadoc・コンストラクタ・`createNewFile` |
| 34-38 | 仕様あり | `isDirective`: `FixedLengthDirective.VALUES` に含まれるキーのみがディレクティブとして有効。固定長セクションで記述できるディレクティブキーは `FixedLengthDirective` 列挙体の定義に限定される |

#### VariableLengthFileParser（47行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-32 | 対象外 | パッケージ宣言・import文・Javadoc・コンストラクタ・`createNewFile` |
| 34-38 | 仕様あり | `isDirective`: `VariableLengthDirective.VALUES` に含まれるキーのみがディレクティブとして有効。可変長セクションで記述できるディレクティブキーは `VariableLengthDirective` 列挙体の定義に限定される |
| 40-46 | 仕様あり | `onReadingTypes` オーバーライド: 型行読み取り後に `READING_LENGTHS` をスキップして直接 `READING_VALUES` へ遷移。**可変長ファイルにはフィールド長行が存在しない** |
| 47 | 対象外 | クラス終端 |

#### BasicTestDataParser（272行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-47 | 対象外 | パッケージ宣言・import文・Javadoc・フィールド宣言 |
| 49-57 | 仕様あり | `getSetupTableData`: リソースが存在しない場合（`isDataExisting` = false）は空リストを返す（空シートを省略可能）。DataType = `SETUP_TABLE` |
| 59-64 | 仕様あり | `getListMap`: DataType = `LIST_MAP`。ID は `[グループID]` 形式で指定 |
| 66-72 | 仕様あり | `getSetupFile`: `SETUP_FIXED` と `SETUP_VARIABLE` の両 DataType を走査しマージ。**1つのリソースに固定長・可変長を混在記述可能** |
| 74-80 | 仕様あり | `getExpectedFile`: `EXPECTED_FIXED` と `EXPECTED_VARIABLE` をマージ。期待値ファイルも混在可能 |
| 81-86 | 仕様あり | `getMessage`: DataType = `MESSAGE` |
| 88-103 | 仕様あり | `getMessageWithoutCache`: `saveCache=false` でキャッシュを回避して取得 |
| 104-117 | 仕様あり | `getSendSyncMessage`: `GroupMessageParser` を使用。groupId を引数で受け取り DataType も外部から渡す |
| 119-167 | 対象外 | `getFixedLengthFile` / `getVariableLengthFile` / `getFile` ヘルパーメソッド（内部実装） |
| 170-181 | 仕様あり | `getExpectedTableData`: `EXPECTED_TABLE` と `EXPECTED_COMPLETE_TABLE` の両 DataType を収集。後者には `fillDefaultValues()` を呼び出してから（省略カラムにデフォルト値が埋まる）マージ |
| 183-198 | 対象外 | `getTableData` ヘルパーメソッド（内部実装） |
| 200-213 | 仕様あり | `addBinaryFileInterpreter`: `BinaryFileInterpreter` をインタープリタリストの**先頭**に追加。バイナリファイル解釈が他のインタープリタより高優先度で実行される |
| 215-241 | 対象外 | setter 群（`setTestDataReader`, `setDbInfo`, `setInterpreters`, `setDefaultValues`） |
| 243-266 | 仕様あり | `formatGroupId`: (1) null または要素数0 → 空文字（グループIDなし）; (2) 要素数1 → `[グループID]` 形式に変換; (3) 要素数2以上 → `IllegalArgumentException`。セクション識別行のグループID書式: `[groupId]`（省略時は空文字） |
| 267-271 | 仕様あり | `isResourceExisting`: testDataReader に委譲してリソース存在確認を行う |
| 272 | 対象外 | クラス終端 |

### 4.2 file パッケージ

#### DataFile（366行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-27 | 対象外 | パッケージ宣言・import文 |
| 28-43 | 仕様あり | クラス Javadoc: `DataFile` はファイル全体のディレクティブを保持し、`DataFileFragment` の集合体として構成される（ファイル全体ディレクティブとフラグメントの二層構造） |
| 44-51 | 対象外 | アノテーション・クラス宣言・ロガー定義 |
| 50-57 | 仕様あり | `all`（フラグメントリスト）、`path`（ファイルパス）、`directives`（ディレクティブ Map）フィールド |
| 59-60 | 仕様あり | `DEFAULT_DIRECTIVES = "defaultDirectives"`: SystemRepository から全ファイル共通デフォルトディレクティブを DI するキー名 |
| 62-81 | 仕様あり | `prepareDefaultDirectives(String key)`: SystemRepository から指定キーで `Map<String,String>` を取得し一括設定。未設定時は空扱い（デフォルトディレクティブ DI 仕様） |
| 83-93 | 仕様あり | コンストラクタ: 初期化時に `"defaultDirectives"` キーのデフォルトディレクティブを読み込み、`"file-type"` ディレクティブをサブクラスの `getFileType()` 戻り値で**自動設定**する |
| 95-101 | 仕様あり | `getFileType()` 抽象メソッド: サブクラスがファイルタイプ文字列を返す（`file-type` ディレクティブの値に使用） |
| 103-123 | 対象外 | `write()` ファイル書き込み実装 |
| 125-137 | 仕様あり | `getNewFragment()`: 新しいフラグメントを生成して `all` リストに追加（フラグメントは `all` リストで順序管理される） |
| 139-145 | 仕様あり | `createNewFragment()` 抽象メソッド: サブクラスがファイル種別に対応するフラグメントを生成 |
| 147-161 | 仕様あり | `toDataRecords()`: 全フラグメントの DataRecord を結合して返す（フラグメント順序で全レコードが連結される） |
| 163-253 | 対象外 | `read()` 系メソッド群（内部実装） |
| 254-284 | 仕様あり | `createLayout()` / `createLayout(DataFileFragment...)`: ディレクティブ Map とフラグメントのレコード定義から `LayoutDefinition` を構築する |
| 286-306 | 仕様あり | `setDirective(String, String)`: ディレクティブ名称が許容リスト外の場合 `IllegalArgumentException`（無効ディレクティブ拒否）。`text-encoding` 設定時はエンコーディングを内部保持 |
| 308-316 | 対象外 | `getPath()` getter |
| 318-334 | 仕様あり | `convertDirectiveValue()`: `record-separator` は `LineSeparator.evaluate()` で変換、それ以外はディレクティブ許容型に変換（ディレクティブ値の型変換仕様） |
| 336-342 | 仕様あり | `valueOf(String)` 抽象メソッド: サブクラスがディレクティブ名から `Directive` を解決（ファイル種別ごとに許容ディレクティブが異なる） |
| 344-365 | 対象外 | `getEncodingFromDirectives()` / `createFormatter()` 内部ヘルパー |

#### DataFileFragment（608行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-33 | 対象外 | パッケージ宣言・import文・Javadoc |
| 34 | 対象外 | クラス宣言 |
| 36-55 | 仕様あり | フィールド定義: `container`（親 DataFile）、`DATATYPE_MAPPING = "dataTypeMapping"`（システムリポジトリキー）、`names` / `types` / `lengths`（フィールド定義の3要素）、`isOndemandCalcFieldSizeList`（`"-"` 長フラグ）、`recordType`（レコード種別名）、`values`（複数レコードデータ） |
| 57-70 | 仕様あり | `FIRST_FIELD_NO = "DataFileFragment:firstFieldKey"`（No.列対応の特殊キー）、`TEST_SYMBOL_PREFIX = "TEST_"`（テスト用データ型プレフィクス）、`ONDEMAND_CALC_FIELD_SIZE = "-"`（オンデマンド計算フィールド長）、`REMOVE_LS_SP_PATTERN`（`"-"` 長フィールドの改行・空白除去パターン） |
| 72-86 | 対象外 | コンストラクタ（`container` 設定のみ） |
| 88-95 | 仕様あり | `setRecordType(String)`: レコード種別を文字列で設定 |
| 97-115 | 仕様あり | `addValue(List<String>)`: フィールド名をキーとしてレコードデータを追加。フィールド数より値が少ない場合は **空文字補完**（末尾フィールド省略可）。`"-"` 長フィールドは `removeLineSeparatorWithTrim` + `replaceFieldSize` を適用 |
| 117-127 | 対象外 | `isOndemandCalcFieldSize(int)` 内部ヘルパー |
| 129-152 | 仕様あり | `replaceFieldSize(int, String)`: `"-"` 長フィールドの場合、データのバイト長を計算して `lengths` を更新（既存値より大きい場合のみ → **最大バイト長に自動拡張**）。エンコーディングはファイルの `text-encoding` ディレクティブを使用 |
| 154-161 | 仕様あり | `removeLineSeparatorWithTrim(String)`: `\s*[\r\n]\s*` パターンで改行コードと前後空白を除去（`"-"` 長フィールドの正規化仕様） |
| 163-183 | 仕様あり | `addValueWithId(List<String>, String)`: `FIRST_FIELD_NO` キーで連番を先頭に追加してからフィールド値を格納（No.列付きレコード追加仕様） |
| 185-194 | 仕様あり | `setNames(List<String>)`: フィールド名は null/空不可。**重複不可**（`assertNotContainDuplicateNames` を呼び出す） |
| 196-209 | 仕様あり | `setTypes(List<String>)`: 要素数はフィールド名と同数でなければならない。各シンボルを `convertToFrameworkExpression()` でフレームワーク表現に変換 |
| 211-245 | 仕様あり | `getTypeForTest(int)`: `"TEST_" + baseType` という名前のデータ型が存在する場合、自動的にそちらを**優先選択**する（TEST_プレフィクス型の自動優先仕様） |
| 247-252 | 対象外 | `getConvertorFactorySupport()` 抽象メソッド宣言 |
| 254-278 | 仕様あり | `convertToFrameworkExpression(String)`: データ型変換の優先順位 — (1) `dataTypeMapping_{エンコーディング名}` → (2) `dataTypeMapping` → (3) `BasicDataTypeMapping.getDefault()` の順で SystemRepository から取得 |
| 280-293 | 仕様あり | `setLengths(List<String>)`: 要素数はフィールド名と同数でなければならない。`"-"` 要素に対して `isOndemandCalcFieldSizeList` フラグを設定 |
| 295-347 | 対象外 | `getRecordLength()` / `calcRecordLength()` / バリデーション系（内部実装） |
| 348-362 | 仕様あり | `assertNotContainDuplicateNames()`: 同一レコード種別内のフィールド名重複は `IllegalArgumentException`（重複フィールド名禁止仕様） |
| 364-407 | 対象外 | `extractDuplicateElement()` / `toDataRecords()` / `toDataRecord()` 系（内部実装） |
| 408-424 | 仕様あり | `convertForDataRecord()` / `convertValue()` 抽象メソッド: サブクラスが文字列値を DataRecord 用 Object 値に変換する（固定長/可変長で実装が異なる） |
| 426-530 | 対象外 | `getTypeOf()` / `getIndexOf()` / `getFieldDefinition()` / `removePadding()` / `getDataType()` / `getRecordDefinition()` 系（内部実装） |
| 531-539 | 仕様あり | `createFieldDefinition(int)` 抽象メソッド: サブクラスがフィールドインデックスから `FieldDefinition` を生成する |
| 541-554 | 仕様あり | `isSizeValid()` 抽象メソッド: サブクラスが names/types/lengths 各リストのサイズ整合性チェック条件を定義（固定長は3リスト必須、可変長は lengths 不要） |
| 556-608 | 対象外 | `toString()` / `writeWith()` / `getNumberOfRecords()` / `getLengthOf()` 系（内部実装） |

#### FixedLengthFile（159行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-13 | 対象外 | パッケージ宣言・import文・Javadoc |
| 14 | 対象外 | クラス宣言 |
| 16-17 | 仕様あり | `DEFAULT_DIRECTIVES = "fixedLengthDirectives"`: 固定長ファイル専用のデフォルトディレクティブ DI キー名 |
| 19-27 | 仕様あり | コンストラクタ: `"fixedLengthDirectives"` キーのデフォルトディレクティブを追加適用（適用順序: `defaultDirectives` → `fixedLengthDirectives`、後者が優先上書き） |
| 29-36 | 仕様あり | `getFileType()`: 固定長ファイルのファイルタイプ文字列は `"Fixed"` |
| 38-47 | 仕様あり | `createNewFragment()`: 固定長ファイルのフラグメントは `FixedLengthFileFragment` |
| 49-58 | 仕様あり | `valueOf(String)`: 固定長ファイルで許容されるディレクティブは `FixedLengthDirective` 列挙型の範囲に限定される |
| 60-92 | 仕様あり | `createLayout()` (書き込み用・読み込み用): フラグメント群のフィールド長合計から **`record-length` を自動計算**してディレクティブに設定（明示指定不要） |
| 94-117 | 仕様あり | `getRecordLength()`: 全フラグメントのレコード長が同一でなければ `IllegalStateException`（**固定長ファイルは全フラグメントで同一レコード長が必須**） |
| 119-133 | 仕様あり | `createDefinition(LayoutDefinition, DataRecord)`: `"TestDataConverter_{file-type}"` キーで SystemRepository から `TestDataConverter` を取得し、存在する場合はレイアウト定義をカスタマイズできる拡張ポイント |
| 135-149 | 仕様あり | `convertData(LayoutDefinition, DataRecord)`: 同 `TestDataConverter` 経由でテストデータ自体を変換できる拡張ポイント |
| 151-158 | 仕様あり | `getTestDataConverter()`: SystemRepository キー `"TestDataConverter_" + fileType` でコンバータを取得（キー名規則: `"TestDataConverter_Fixed"` / `"TestDataConverter_Variable"`） |

#### FixedLengthFileFragment（145行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-31 | 対象外 | パッケージ宣言・import文・Javadoc・コンストラクタ |
| 33-38 | 仕様あり | `bytePosition = 1`: フィールド定義時のバイト位置はレコード先頭 (1) から開始する（1始まり仕様） |
| 40-59 | 仕様あり | `convertForDataRecord()`: 固定長では値はパディング処理される。ダミーの `FixedLengthDataRecordFormatter` を使用してパディング除去後に DataRecord を構築 |
| 61-88 | 仕様あり | `convertValue(String, String)`: `Bytes` 型フィールドはバイト列に変換、それ以外は文字列のまま返す（固定長の `Bytes` 型対応） |
| 90-103 | 仕様あり | `createFieldDefinition(int)`: 固定長フィールド定義はバイト位置・名前・エンコーディング・型シンボル・長さ（必須）で構成。`getTypeForTest()` で `TEST_` プレフィクス型を優先選択し `bytePosition` をフィールド長分インクリメント |
| 105-109 | 仕様あり | `getConvertorFactorySupport()`: 固定長は `FixedLengthConvertorSetting` のコンバータファクトリを使用 |
| 111-138 | 仕様あり | `toBytes()`: 変換後バイト数がフィールド長未満 → 右ゼロ埋め、超過 → `IllegalStateException`（**Bytes 型フィールドの長さ制約**） |
| 140-144 | 仕様あり | `isSizeValid()`: 固定長では names・types・lengths の3リストがすべて同じサイズでなければならない（可変長と異なり lengths も必須） |
| 145 | 対象外 | クラス終端 |

#### VariableLengthFile（83行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-13 | 対象外 | パッケージ宣言・import文・Javadoc |
| 14 | 対象外 | クラス宣言 |
| 16-17 | 仕様あり | `TAB_EXPRESSION = "\\t"`: フィールド区切り文字のタブ指定は `\t`（バックスラッシュ+t の2文字）で記述する |
| 19-20 | 仕様あり | `DEFAULT_DIRECTIVES = "variableLengthDirectives"`: 可変長ファイル専用のデフォルトディレクティブ DI キー名 |
| 22-31 | 仕様あり | コンストラクタ: `field-separator` のデフォルト値として `","` （カンマ）を設定した後、`"variableLengthDirectives"` キーのデフォルトディレクティブを上書き適用（`variableLengthDirectives` に `field-separator` を設定することでカンマ以外のデフォルトに変更可能） |
| 33-40 | 仕様あり | `getFileType()`: 可変長ファイルのファイルタイプ文字列は `"Variable"` |
| 42-50 | 仕様あり | `createNewFragment()`: 可変長ファイルのフラグメントは `VariableLengthFileFragment` |
| 52-60 | 仕様あり | `valueOf(String)`: 可変長ファイルで許容されるディレクティブは `VariableLengthDirective` 列挙型の範囲に限定される |
| 62-82 | 仕様あり | `convertDirectiveValue()`: `field-separator` に `\t` が指定された場合はタブ文字 `"\t"` に変換。`field-separator` は**1文字のみ有効**（違反時 `IllegalArgumentException`） |
| 83 | 対象外 | クラス終端 |

#### VariableLengthFileFragment（71行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-24 | 対象外 | パッケージ宣言・import文・Javadoc・コンストラクタ |
| 26-27 | 仕様あり | `fieldPosition = 1`: フィールド定義時の順番位置はレコード先頭 (1) から開始する |
| 29-39 | 対象外 | `convertForDataRecord()`: 文字列のまま collect（内部実装） |
| 41-45 | 仕様あり | `convertValue(String, String)`: 可変長では値の型変換を行わず文字列のまま返す（固定長と対照的） |
| 47-58 | 仕様あり | `createFieldDefinition(int)`: 可変長フィールド定義は名前・順番位置・エンコーディング・型シンボルで構成。フィールド長は不要。`getTypeForTest()` で `TEST_` プレフィクス型を優先選択 |
| 60-65 | 仕様あり | `getConvertorFactorySupport()`: 可変長は `VariableLengthConvertorSetting` のコンバータファクトリを使用 |
| 66-70 | 仕様あり | `isSizeValid()`: 可変長では names と types が同じサイズであれば良く、lengths のサイズ一致は不要（固定長と異なり長さ指定は任意） |
| 71 | 対象外 | クラス終端 |

#### BasicDataTypeMapping（100行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-15 | 対象外 | パッケージ宣言・import文・Javadoc |
| 16 | 仕様あり | `DataTypeMapping` インタフェースを実装する公開クラス |
| 18-28 | 対象外 | static インスタンス保持と getter（内部実装） |
| 30-56 | 仕様あり | デフォルトマッピング表（22種）: `半角英字`→`X`、`半角数字`→`X`、`半角記号`→`X`、`半角カナ`→`X`、`半角英数字`→`X`、`半角英数字記号`→`X`、`半角`→`X`、`全角英字`→`N`、`全角数字`→`N`、`全角ひらがな`→`N`、`全角カタカナ`→`N`、`全角漢字`→`N`、`全角`→`N`、`全半角`→`XN`、`数値`→`Z`、`符号無ゾーン10進数`→`Z`、`符号付ゾーン10進数`→`SZ`、`符号無パック10進数`→`P`、`符号付パック10進数`→`SP`、`符号無数値`→`X9`、`符号付数値`→`SX9`、`バイナリ`→`B` |
| 58-73 | 仕様あり | `convertToFrameworkExpression(null)` は `IllegalArgumentException`。マッピング表に存在しないキーも `IllegalArgumentException`（identity mapping なし — 未知の型記号はエラー） |
| 75-90 | 仕様あり | `setMappingTable(Map)` でデフォルトマッピング表を外部から上書き可能。null を渡すと `IllegalArgumentException` |
| 92-100 | 対象外 | private getter（`mappingTable` null チェックしてデフォルト返却） |

#### LineSeparator（66行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-7 | 対象外 | パッケージ宣言・Javadoc |
| 8 | 仕様あり | `enum` として公開 |
| 10-17 | 仕様あり | 有効な列挙値: `NONE`（空文字）、`CR`（`\r`）、`LF`（`\n`）、`CRLF`（`\r\n`）の4種 |
| 19-39 | 対象外 | フィールド・コンストラクタ・`toString()` |
| 41-65 | 仕様あり | `evaluate(String expression)`: `NONE/CR/LF/CRLF` のいずれかに一致する場合は対応する改行コードを返す。一致しない場合は引数をそのまま返す（**任意文字列をリテラル改行コードとして使用可能**） |
| 66 | 対象外 | クラス終端 |

### 4.3 messaging パッケージ

#### RequestTestingMessagingClient（572行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-52 | 対象外 | パッケージ宣言・import文・クラス Javadoc |
| 53 | 仕様あり | `MessageSenderClient` インタフェースを実装（テスト時の差し替えクラス。本クラスを使用する場合、実際のメッセージ送信は行われない） |
| 55-73 | 仕様あり | `isMockEnable` フラグで機能の有効/無効を制御。`expectedRequestMessageId` と `responseMessageId` が**両方とも**空の場合のみモック無効のまま（片方だけ空でも初期化は実行される） |
| 75-76 | 仕様あり | 応答電文フォーマット定義ファイル名パターン: `{requestId}_RECEIVE` |
| 78-79 | 仕様あり | 要求電文フォーマット定義ファイル名パターン: `{requestId}_SEND` |
| 81-83 | 仕様あり | `messaging.assertAsMapFileType` キー: SystemRepository からアサート方式を切り替える。未設定時はデフォルトで `"Fixed"` 形式として DataRecord 単位にアサート |
| 84-111 | 仕様あり | `initializeForRequestUnitTesting()`: テストケースのクラス・シート名・テストケース番号・responseMessageId・expectedMessageId の5つを受け取って初期化 |
| 113-122 | 仕様あり | `clearSendingMessageCache()`: 要求電文キャッシュをクリアし `isMockEnable=false` にする（テスト後の後処理） |
| 124-204 | 仕様あり | `sendSync()`: `isMockEnable=false` の場合は即 `RuntimeException`。ステータスコードなしは `"200"` をデフォルト設定 |
| 207-287 | 対象外 | `createReceivedMessage()` 内部実装 / `assertSendingMessage()` 内部ロジック |
| 289-292 | 対象外 | 内部キー定数 `"header"` / `"body"` |
| 294-443 | 仕様あり | ヘッダ行数とボディ行数が不一致の場合は `IllegalStateException`（EXPECTED_REQUEST_HEADER_MESSAGES と EXPECTED_REQUEST_BODY_MESSAGES の行数一致が必須）。送信メッセージ数と期待値数が不一致の場合は `Assertion.fail()`。`assertAsMapFileType` に `"Fixed"` が含まれる場合は項目単位アサート、それ以外は電文全体を文字列としてアサート |
| 445-571 | 対象外 | 内部ヘルパーメソッド群 |
| 572 | 対象外 | クラス終端 |

#### SendSyncSupport（474行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-37 | 対象外 | パッケージ宣言・import文・クラス Javadoc |
| 39-49 | 仕様あり | `RESPONSE_MESSAGES_SHEET_NAME = "message"`: レスポンスメッセージシート名は定数 `"message"`。`SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData"`: テストデータディレクトリのベースパス名は定数 `"sendSyncTestData"` |
| 50-52 | 対象外 | キャッシュ用 Map フィールド |
| 55-270 | 対象外 | ログ出力系メソッド群（内部実装） |
| 271-309 | 仕様あり | `getResponseMessageBinaryByRequestId()`: レコードに `TIMEOUT` 値が含まれる場合は `null` を返却（タイムアウトをシミュレート）。`MSG_EXCEPTION` 値が含まれる場合は `MessagingException` をスロー |
| 310-333 | 対象外 | `getResponseMessageByRequestId()` 内部実装 |
| 335-403 | 仕様あり | `createTestDataInfo()`: `sendSyncTestData` ベースパス下のリクエストIDと同名ファイルが存在しない場合は `IllegalStateException`。`message` シート名からデータを取得。ファイルのタイムスタンプが変更された場合は再読込、変わっていない場合はキャッシュからインクリメント取得（**連続呼び出しで次のレコードを返す**） |
| 404-432 | 仕様あり | `getMessages()`: SystemRepository から `"messagingTestDataParser"` キーで `BasicTestDataParser` を取得。取得できない場合 `IllegalStateException`。対応メッセージが見つからない場合も `IllegalStateException` |
| 433-474 | 対象外 | `TestDataInfo` 内部クラス（内部実装） |

### 4.4 db パッケージ

#### TableData（745行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-37 | 対象外 | パッケージ宣言・import文・クラス宣言 |
| 39-40 | 仕様あり | デフォルト日付フォーマット: `"yyyyMMddHHmmssSSS"`（17文字） |
| 42-59 | 対象外 | フィールド宣言（`defaultValues` のデフォルト実装は `BasicDefaultValues`） |
| 61-89 | 対象外 | コンストラクタ群 |
| 91-98 | 仕様あり | テーブル名は `trim().toUpperCase()` で正規化される |
| 100-178 | 対象外 | DB 操作メソッド（`replaceData`, `deleteData`, `insertData` 系）（内部実装） |
| 180-212 | 仕様あり | カラム省略時はデフォルト値を使用。日付型カラムは `toTimestamp()` に変換 |
| 214-229 | 仕様あり | `toTimestamp()`: 空文字は `null` を返す。先頭4文字目が `'-'` の場合は JDBC タイムスタンプエスケープ形式と判定、それ以外は `yyyyMMddHHmmssSSS` 形式で解析 |
| 230-273 | 仕様あり | `asYyyyMMddHHmmssSSS()`: 入力値の後ろに `"00000000000000000"` を付加して17文字にトリム（**後置0埋め** → 短い日付文字列 `"20231001"` でも有効）。`"yyyyMMdd HHmmss"`（スペース区切り14文字）と `"yyyyMMddHHmmssS"`（ミリ秒1桁15文字）も有効。`asJdbcTimestampEscape()`: 時刻部分（`:`）がない場合は `" 00:00:00.000"` を付加して `Timestamp.valueOf()` で変換 |
| 274-362 | 対象外 | `getDefaultValue()` / `createInsertStatement()` / `getNonComputedColumns()` / `loadData()` 内部実装 |
| 363-396 | 仕様あり | `convertSqlRow()`: CLOB 型は文字列に変換。BigDecimal 型は末尾の0を削除（`DecimalFormat("#.#")` 使用） |
| 397-700 | 対象外 | その他 getter/setter/toString 等 |
| 701-745 | 仕様あり | `fillDefaultValues()`: テストデータで省略されたカラムに対してデフォルト値を補完。DB 上の全カラムを取得し、`columnNames` にないものにデフォルト値を設定し、`columnNames` を DB 全カラムに更新する |

### 4.5 interpreter / generator パッケージ

#### NullInterpreter（20行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1 | 対象外 | パッケージ宣言 |
| 2-7 | 仕様あり | Javadoc: 「半角 `null`（大文字、小文字は区別しない）の場合は null 値に置き換える」と明記 |
| 8 | 仕様あり | `TestDataInterpreter` インタフェース実装 |
| 10-11 | 仕様あり | 比較対象定数は `"null"`（半角小文字）で `equalsIgnoreCase()` で比較するため `"NULL"` / `"Null"` も有効 |
| 13-19 | 仕様あり | `equalsIgnoreCase` で一致すれば `null` を返却、不一致は次のインタープリタに委譲 |
| 20 | 対象外 | クラス終端 |

#### QuotationTrimmer（32行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-3 | 対象外 | パッケージ宣言 |
| 4-7 | 仕様あり | Javadoc に「半角・全角問わず」と明記。全角ダブルクォート（`"` と `"`）にも対応 |
| 9 | 仕様あり | `TestDataInterpreter` 実装 |
| 10-16 | 対象外 | `interpret()` 委譲 |
| 18-30 | 仕様あり | `trimQuotation()`: 半角ダブルクォートまたは全角ダブルクォートで前後が囲われている場合のみ除去（`startsWith` かつ `endsWith` の両立が必須）。**片側のみはスルー**。`""abc""` → `"abc"`（最外側の1層のみ除去） |
| 31-32 | 対象外 | クラス終端 |

#### DateTimeInterpreter（105行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-5 | 対象外 | パッケージ宣言・import文 |
| 6-45 | 仕様あり | Javadoc: `${systemTime}` → システム日時、`${setUpTime}` → DB セットアップ時の値、`${updateTime}` → DB 更新時の値（システム日時と同値） |
| 46 | 仕様あり | `TestDataInterpreter` 実装 |
| 48-55 | 仕様あり | 3つのキー定数: `"${systemTime}"` / `"${updateTime}"` / `"${setUpTime}"`。**完全一致のみ変換**（部分文字列は変換されない。`CompositeInterpreter` との組み合わせが必要） |
| 56-77 | 仕様あり | `setSystemTimeProvider()`: `systemTimeProvider.getTimestamp().toString()` の値を `${systemTime}` と `${updateTime}` の両方に設定 |
| 78-94 | 仕様あり | `setSetUpDateTime()`: null または正規表現 `\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d+` に合致しない場合は `IllegalArgumentException`。受け入れフォーマット: `"yyyy-mm-dd hh:mm:ss.f..."` （小数部は1桁以上の任意桁数） |
| 96-104 | 対象外 | `interpret()` マップルックアップ（内部実装） |
| 105 | 対象外 | クラス終端 |

#### LineSeparatorInterpreter（89行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-7 | 対象外 | パッケージ宣言・import文 |
| 8-27 | 仕様あり | Javadoc: Excel セル内で CR を記述できない問題への対処。デフォルトでは文字列中の `\r`（バックスラッシュ+r の2文字）が CR に置換される |
| 28 | 仕様あり | `TestDataInterpreter` 実装 |
| 30-34 | 仕様あり | デフォルトの置換対象パターンは `"\\\\r"`（正規表現で `\r` リテラル文字列に一致）、デフォルトの置換後改行コードは `LineSeparator.CR`（`\r` 単独）。**CRLF ではなく CR 単独がデフォルト** |
| 35-65 | 対象外 | `interpret()` / `replaceLineSeparator()` 適用ロジック（内部実装） |
| 66-77 | 仕様あり | `setLineSeparator(String expression)`: `LineSeparator.evaluate(expression)` を介して設定。有効値は `NONE/CR/LF/CRLF` またはリテラル文字列 |
| 78-88 | 仕様あり | `setMatchPattern(String pattern)`: Java 正規表現文字列を受け取り `Pattern.compile()` でコンパイル（カスタマイズ可能な拡張ポイント） |
| 89 | 対象外 | クラス終端 |

#### BinaryFileInterpreter（93行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-13 | 対象外 | パッケージ宣言・import文 |
| 14-31 | 仕様あり | Javadoc: `${binaryFile:ファイルパス}` と記述するとファイル内容をバイナリ読み込みして HexString に変換する。ファイルパスは Excel ファイルからの相対パスで記述する。本インタープリタは設定ファイルの `interpreters` リストに含める必要はない（`BasicTestDataParser.addBinaryFileInterpreter()` で自動追加） |
| 32 | 仕様あり | `TestDataInterpreter` 実装 |
| 34-36 | 仕様あり | 認識する記法の正規表現パターン: `\$\{binaryFile:(.+)\}`（`${binaryFile:...}` 形式） |
| 37-48 | 仕様あり | `path` フィールドはコンストラクタで設定され、**Excel ファイルの格納ディレクトリパス**（基準ディレクトリ）となる |
| 49-65 | 仕様あり | `getPath()`: `path + '/' + value` でフルパスを構築（Excel ファイルのディレクトリからの相対パス解決）。YAML 移行後の基準ディレクトリは YAML ファイルのディレクトリになることに注意 |
| 66-92 | 対象外 | `fileToHexString()` ファイル読み込みと Hex 変換（内部実装） |
| 93 | 対象外 | クラス終端 |

#### BasicJapaneseCharacterInterpreter（46行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-7 | 対象外 | パッケージ宣言・import文 |
| 8-17 | 仕様あり | Javadoc: `${文字種,文字数}` 形式。例: `${全角英字,10}` → 10文字の全角英字 |
| 18 | 仕様あり | `TestDataInterpreter` 実装。委譲先は `BasicJapaneseCharacterGenerator` |
| 19-21 | 対象外 | フィールド宣言 |
| 22-24 | 仕様あり | パターン定義: `\$\{(\W+)\s*,\s*([0-9]+)\}`（文字種は `\W+` = 非単語文字1文字以上、文字数は数字のみ） |
| 25-37 | 仕様あり | `interpret()`: パターンに**完全一致**する場合のみ `delegate.generate(type, length)` を呼び出す。**完全一致しない場合は次のインタープリタに委譲**（書式ミスはスルー）。文字種ミス（未知の文字種）は `BasicJapaneseCharacterGenerator` 側が例外をスロー |
| 38-45 | 仕様あり | `setCharacterGenerator(CharacterGenerator)`: 委譲先の文字生成クラスを外部から差し替え可能（カスタム文字種の拡張ポイント） |
| 46 | 対象外 | クラス終端 |

#### CompositeInterpreter（64行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-7 | 対象外 | パッケージ宣言・import文 |
| 8-13 | 仕様あり | Javadoc: `${半角数字,4}-${半角数字,4}` のような複数の `${...}` 要素の混在を解釈し、各要素を個別解釈した結果で置換 |
| 14 | 仕様あり | `TestDataInterpreter` 実装 |
| 15-21 | 対象外 | フィールド宣言（パターン `\$\{[^\}]+\}` で `${...}` にマッチ） |
| 22-42 | 仕様あり | `interpret()`: 文字列中に `${...}` 形式が1つ以上あれば各要素を解釈した結果で置換して返す。**`${...}` 形式が1つもなければ次のインタープリタに委譲** |
| 43-54 | 対象外 | `interpretElement()` 内部ヘルパー |
| 55-63 | 仕様あり | `setInterpreters(List<TestDataInterpreter>)`: 各 `${...}` 要素の解釈に使用するインタープリタリストを設定（DI が必要） |
| 64 | 対象外 | クラス終端 |

#### BasicJapaneseCharacterGenerator（63行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-37 | 対象外 | パッケージ宣言・static import文・Javadoc |
| 38 | 仕様あり | `CharacterGeneratorBase` を継承 |
| 40-56 | 仕様あり | `TYPE_CHARS_PAIRS` で文字種名と対応する文字集合の定義。有効な文字種トークン（14種）: `半角英字`、`半角数字`、`半角記号`、`半角カナ`、`全角英字`、`全角数字`、`全角ひらがな`、`全角カタカナ`、`全角漢字`、`全角記号その他`、`中国語`（`"你"` 1文字のみ）、`サロゲートペア`（`"𩸽𠮷"` の2文字）、`改行`（`"\r\n"` = CRLF）、`外字`（`"㈱"` 1文字のみ） |
| 58-63 | 対象外 | コンストラクタ（`super(TYPE_CHARS_PAIRS)` を呼び出すだけ） |

#### JapaneseCharacterSet（270行）

| 行番号 | 仕様あり/対象外 | 内容 |
|---|---|---|
| 1-7 | 対象外 | パッケージ宣言・Javadoc |
| 8 | 仕様あり | `final class`・パッケージプライベート（外部公開しない） |
| 10-36 | 仕様あり | 半角文字集合の定義: `NUMERIC` = `"0123456789"`、`LOWER_ALPHABET` = `a-z`、`UPPER_ALPHABET` = `A-Z` |
| 22-36 | 仕様あり | `ASCII_SYMBOL` の除外文字（Javadoc に明記）: ダブルクォート（`"`）、シャープ（`#`）、カンマ（`,`）、バックスラッシュ（`\`）の4文字。実際の `ASCII_SYMBOL` 値: `"!$%&'()*+-./:;<=>?@[]^_` + "`{|}~"` |
| 37 | 仕様あり | `HANKAKU_KANA_CHARS`: 半角カナ文字集合（`｡｢｣` から `ﾟ` まで） |
| 40-51 | 対象外 | 組み合わせ定数（`ALPHABET`, `ALPHA_NUMERIC`, `ASCII_CHARS`, `HANKAKU_CHARS`）は内部組み合わせ |
| 53-73 | 仕様あり | 全角文字集合: `ZENKAKU_NUM_CHARS`（全角数字）、`ZENKAKU_ALPHA_CHARS`（全角英字）、`ZENKAKU_HIRAGANA_CHARS`（`ー`（長音符）を末尾に含む）、`ZENKAKU_KATAKANA_CHARS`（`ー`・`ヴ`・`ヵ`・`ヶ` を含む） |
| 75-206 | 仕様あり | `LEVEL1_KANJI` / `LEVEL2_KANJI`: JIS 第1・第2水準漢字の全文字定義 |
| 208-265 | 仕様あり | 全角記号系文字集合: `JIS_SYMBOL_CHARS`、`ZENKAKU_GREEK_CHARS`、`ZENKAKU_KEISEN_CHARS`、`ZENKAKU_RUSSIAN_CHARS`、`NEC_EXTENDED_CHARS`（NEC選定IBM拡張）、`NEC_SYMBOL_CHARS`（NEC特殊）、`IBM_EXTENDED_CHARS`（IBM拡張）。組み合わせ: `ZENKAKU_KANJI`=第1+第2水準、`ZENKAKU_SYMBOL`=JIS記号+罫線、`GAIJI_CHARS`=IBM拡張+NEC拡張+NEC特殊 |
| 266-270 | 対象外 | プライベートコンストラクタ・クラス終端 |

---

## 5. 次の一歩

1. §1 の優先度 **高** の仕様（S-1・D-3・D-4・D-6）から反映先（schema.json / design.md）へ反映する。
2. §3 で `[要確認]` を付した D-4（`BasicDefaultValues` のデフォルト値一覧）・D-10（空行の書き出し挙動差）は、走査範囲外のクラス・処理を追加確認してから反映する。
3. examples.yaml の E-1〜E-4 を追加し、反映済み仕様の記法例を補う。
