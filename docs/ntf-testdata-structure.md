# NTFテストデータ構造 調査報告

根拠: コードおよびJavadocのみ。推測なし。

---

## 1. テストデータの論理単位

| レベル | Excel上の単位 | 備考 | 根拠クラス |
|---|---|---|---|
| ファイル | `.xls` / `.xlsx` ファイル | `.xls`が存在しない場合に`.xlsx`を試みる | `PoiXlsReader` |
| シート | 1シート = 1テストデータリソース | `dataName` = `"ファイル名/シート名"`（`/`区切り） | `PoiXlsReader` |
| データブロック | シート内の連続した行群 | データタイプ行（例: `SETUP_TABLE=...`）が起点 | `TestDataParsingTemplate` |
| 行 | 空行・コメント行はスキップ | 先頭セルが`//`始まりはコメント行 | `TestDataParsingTemplate` |
| セル | 全て文字列化 | `cell == null` → `""` | `PoiXlsReader` |

---

## 2. データ種別の完全な列挙

根拠: `nablarch.test.core.reader.DataType`（enum）

| enum定数 | Excel識別文字列 | 用途 | テンプレート種別 |
|---|---|---|---|
| `SETUP_TABLE_DATA` | `SETUP_TABLE` | DB事前準備用テーブルデータ | GroupData |
| `EXPECTED_TABLE_DATA` | `EXPECTED_TABLE` | 期待値テーブルデータ | GroupData |
| `EXPECTED_COMPLETED` | `EXPECTED_COMPLETE_TABLE` | 期待値テーブル（省略カラムにデフォルト値補完） | GroupData |
| `LIST_MAP` | `LIST_MAP` | `List<Map<String,String>>`形式データ | SingleData（ID一致で停止） |
| `SETUP_FIXED` | `SETUP_FIXED` | 事前準備用固定長ファイル | GroupData |
| `EXPECTED_FIXED` | `EXPECTED_FIXED` | 期待値固定長ファイル | GroupData |
| `SETUP_VARIABLE` | `SETUP_VARIABLE` | 事前準備用可変長ファイル | GroupData |
| `EXPECTED_VARIABLE` | `EXPECTED_VARIABLE` | 期待値可変長ファイル | GroupData |
| `MESSAGE` | `MESSAGE` | 要求電文（固定長ファイルとして処理） | SingleData |
| `EXPECTED_REQUEST_HEADER_MESSAGES` | `EXPECTED_REQUEST_HEADER_MESSAGES` | 要求電文ヘッダ期待値 | SingleData |
| `EXPECTED_REQUEST_BODY_MESSAGES` | `EXPECTED_REQUEST_BODY_MESSAGES` | 要求電文本文期待値 | SingleData |
| `RESPONSE_HEADER_MESSAGES` | `RESPONSE_HEADER_MESSAGES` | 応答電文ヘッダ | GroupData |
| `RESPONSE_BODY_MESSAGES` | `RESPONSE_BODY_MESSAGES` | 応答電文本文 | GroupData |

識別ロジック: `TestDataParsingTemplate#getDataType()` が `startsWith` で判定。  
グループID書式: `SETUP_TABLE[グループID]=テーブル名`（IDなしは `SETUP_TABLE=テーブル名`）

---

## 3. 各データ種別のフィールド構造

### 3.1 テーブルデータ（SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE）

根拠: `TableDataParser`、`HeaderLine`、`TableData`

```
行1: SETUP_TABLE[groupId]=テーブル名
行2: COL1 | COL2 | [MARKER] | COL3      ← ヘッダ行
行3: val1 | val2 | mark_val  | val3      ← データ行
```

- テーブル名・カラム名は `toUpperCase()` される（`TableData#setTableName()`、`setColumnNames()`）
- `[` 始まり `]` 終わりのカラムはマーカーカラム（DB操作から除外）（`HeaderLine`）
- `EXPECTED_COMPLETE_TABLE` は `fillDefaultValues()` で省略カラムにデフォルト値補完

### 3.2 LIST_MAP

根拠: `ListMapParser`（`SingleDataParsingTemplate` 継承）

```
行1: LIST_MAP=ID
行2: KEY1 | KEY2 | [MARKER]
行3: val1 | val2 | mark_val
```

- IDは `getTypeValue()`（`=`以降の文字列）で取得
- 結果は `List<Map<String,String>>`、マーカーカラム除外後

### 3.3 固定長ファイル（SETUP_FIXED / EXPECTED_FIXED）

根拠: `FixedLengthFileParser`、`DataFileParser`、`FixedLengthFileFragment`

```
行1: SETUP_FIXED[groupId]=ファイルパス
行2: text-encoding  | UTF-8           ← ディレクティブ行（key|value形式）
行3: レコード種別名  | FIELD1 | FIELD2  ← フィールド名行（先頭セルが種別名）
行4:                | X      | N       ← データ型行（先頭空）
行5:                | 10     | 5       ← フィールド長行（先頭空。"-"はオンデマンド計算）
行6:                | val1   | val2    ← データ行（先頭空）
```

有効なディレクティブ（`FixedLengthDirective`）:

| キー | 意味 |
|---|---|
| `text-encoding` | 文字エンコーディング |
| `record-separator` | レコード区切り文字 |
| `record-length` | レコード長 |
| `file-type` | ファイル種別 |

### 3.4 可変長ファイル（SETUP_VARIABLE / EXPECTED_VARIABLE）

根拠: `VariableLengthFileParser`

固定長ファイルと同構造だが**フィールド長行がない**（`onReadingTypes()` で `READING_LENGTHS` ステートをスキップ）。  
デフォルト区切り文字: `,`

### 3.5 メッセージ（MESSAGE / EXPECTED_REQUEST_*_MESSAGES）

根拠: `MessageParser`（`SingleDataParsingTemplate` + `FixedLengthFileParser`に委譲）

- 内部構造は固定長ファイルと同一
- FWヘッダフィールド（デフォルト: `requestId`, `userId`, `resendFlag`, `resultCode`）は `fwHeader` Mapに分離
- `SystemRepository` の `reader.fwHeaderfields` キーで上書き可能

### 3.6 グループメッセージ（RESPONSE_HEADER_MESSAGES / RESPONSE_BODY_MESSAGES）

根拠: `GroupMessageParser`（`GroupDataParsingTemplate` 継承）

- 固定長ファイルと同構造、複数件対応

---

## 4. 特殊値・変換ルール

根拠: `TestDataParsingTemplate#interpret()`、各 `Interpreter` 実装

| Excelセル値 | 変換後 | 根拠クラス |
|---|---|---|
| `null`（大文字小文字不問） | Java `null` | `NullInterpreter` |
| `"abc"` / `"abc"`（全半角ダブルクォート囲み） | `abc`（クォート除去） | `QuotationTrimmer` |
| `""` / `""` | 空文字 | `QuotationTrimmer` |
| `${systemTime}` | システム日時 | `DateTimeInterpreter` |
| `${updateTime}` | システム日時（`${systemTime}` と同値） | `DateTimeInterpreter` |
| `${setUpTime}` | DBセットアップ時刻（JDBCタイムスタンプ形式） | `DateTimeInterpreter` |
| `${文字種, 文字数}`（例: `${全角英字, 10}`） | 対応文字種の文字列 | `BasicJapaneseCharacterInterpreter` |
| `${binaryFile:パス}` | HexString | `BinaryFileInterpreter` |
| `\r`（文字列） | CR（0x0D） | `LineSeparatorInterpreter` |
| 複合式（`${...}-${...}`等） | 各部分を個別解釈して結合 | `CompositeInterpreter` |

日付フォーマット（`TableData` DB挿入時）:
- プライマリ: `yyyyMMddHHmmssSSS`（17桁、不足は末尾0補完）
- セカンダリ: `yyyy-MM-dd` / `yyyy-MM-dd HH:mm:ss[.SSS]`（4文字目が`-`で判定）

データ型記号（`BasicDataTypeMapping`）:

| 設計書表記 | 記号 |
|---|---|
| 半角英字/半角数字/半角記号/半角カナ/半角英数字等 | `X` |
| 全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字等 | `N` |
| 全半角 | `XN` |
| 数値/符号無ゾーン10進数 | `Z` |
| 符号付ゾーン10進数 | `SZ` |
| 符号無パック10進数 | `P` |
| 符号付パック10進数 | `SP` |
| 符号無数値 | `X9` |
| 符号付数値 | `SX9` |
| バイナリ | `B` |

---

## 5. データ種別間の関係

- `getSetupFile()` は `SETUP_FIXED` + `SETUP_VARIABLE` を1つの `List<DataFile>` にまとめて返す（`BasicTestDataParser`）
- `getExpectedTableData()` は `EXPECTED_TABLE` + `EXPECTED_COMPLETE_TABLE` をマージして返す
- `DataFile`（1ファイル）は複数の `DataFileFragment`（1レコード種別）を持つ（`all` フィールド）
- `DataFileFragment` は親 `DataFile` への参照でディレクティブを参照する
- GroupData系（SETUP_TABLE等）: 同一シートに複数グループ共存可能
- SingleData系（LIST_MAP、MESSAGE等）: ID一致で最初の1ブロックのみ取得

---

## 主要根拠ファイル

| クラス | パス |
|---|---|
| `DataType` | `src/main/java/nablarch/test/core/reader/DataType.java` |
| `PoiXlsReader` | `src/main/java/nablarch/test/core/reader/PoiXlsReader.java` |
| `BasicTestDataParser` | `src/main/java/nablarch/test/core/reader/BasicTestDataParser.java` |
| `TestDataParsingTemplate` | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java` |
| `GroupDataParsingTemplate` | `src/main/java/nablarch/test/core/reader/GroupDataParsingTemplate.java` |
| `SingleDataParsingTemplate` | `src/main/java/nablarch/test/core/reader/SingleDataParsingTemplate.java` |
| `TableDataParser` | `src/main/java/nablarch/test/core/reader/TableDataParser.java` |
| `HeaderLine` | `src/main/java/nablarch/test/core/reader/HeaderLine.java` |
| `ListMapParser` | `src/main/java/nablarch/test/core/reader/ListMapParser.java` |
| `FixedLengthFileParser` | `src/main/java/nablarch/test/core/reader/FixedLengthFileParser.java` |
| `VariableLengthFileParser` | `src/main/java/nablarch/test/core/reader/VariableLengthFileParser.java` |
| `MessageParser` | `src/main/java/nablarch/test/core/reader/MessageParser.java` |
| `TableData` | `src/main/java/nablarch/test/core/db/TableData.java` |
| `DataFile` | `src/main/java/nablarch/test/core/file/DataFile.java` |
| `BasicDataTypeMapping` | `src/main/java/nablarch/test/core/file/BasicDataTypeMapping.java` |
| `NullInterpreter` | `src/main/java/nablarch/test/core/util/interpreter/NullInterpreter.java` |
| `QuotationTrimmer` | `src/main/java/nablarch/test/core/util/interpreter/QuotationTrimmer.java` |
| `DateTimeInterpreter` | `src/main/java/nablarch/test/core/util/interpreter/DateTimeInterpreter.java` |
