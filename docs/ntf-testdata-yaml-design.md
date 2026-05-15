# NTF テストデータ YAML スキーマ設計メモ

## Excel概念 → YAML構造 対応表

| Excel概念 | YAML構造 | 備考 |
|---|---|---|
| `.xls` / `.xlsx` ファイル | 1つの `.yaml` ファイル | Excelファイル1つが YAMLファイル1つに対応 |
| シート | ファイル内のトップレベルセクション（`setup_tables:` 等）にデータを記述 | シート名の概念は消滅。1ファイルに全種別データを共存可能 |
| データタイプ行（`SETUP_TABLE=...`） | セクションキー（`setup_tables`）+ 各要素の `table:` フィールド | 種別とテーブル名を分離して表現 |
| グループID（`[groupId]`） | `group_id:` フィールド（省略可） | 省略時はグループIDなし扱い |
| ヘッダ行（カラム名） | `rows` 内の各オブジェクトのキー | 各行ごとにキーを書くため冗長だが可読性高 |
| マーカーカラム（`[COLNAME]`） | `"[COLNAME]"` 形式のキー（ダブルクォートが必須） | YAMLで角括弧がフロー配列と誤解釈されないようクォートが必要 |
| ディレクティブ行（`key\|value`） | `directives:` オブジェクト | 構造化されて型安全 |
| フィールド名行・データ型行・フィールド長行（3行1組） | `fields:` 配列の1要素（`name`/`type`/`length`） | 行分割をなくし1フィールド1定義に統合 |
| データ行（先頭空の行） | `rows:` 配列内の値配列 | `fields` と同順 |
| レコード種別（先頭セルが種別名） | `record_type:` フィールド | `records:` 配列の1要素 |
| コメント行（`//`始まり） | YAMLコメント（`#`） | YAML標準のコメント構文を使用 |

---

## 変換ビフォーアフター（Excel → YAML）

> **注意**: Excel の `|` はセル境界を模した擬似表記です。実際のExcelでは各セルが独立しています。

### テーブルデータ（グループIDなし）

**Excel（シート上の表示）:**
```
行1: SETUP_TABLE=USER_TABLE
行2: USER_ID | USER_NAME
行3: 001     | 山田太郎
```

**YAML（変換後）:**
```yaml
setup_tables:
  - table: USER_TABLE       # group_id フィールドは省略
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
```

### テーブルデータ（グループID付き）

**Excel（シート上の表示）:**
```
行1: SETUP_TABLE[case1]=USER_TABLE
行2: USER_ID | USER_NAME | AGE | [MARKER]
行3: 001     | 山田太郎   | 30  | X
行4: 002     | 鈴木花子   | 25  | Y
```

**YAML（変換後）:**
```yaml
setup_tables:
  - group_id: case1
    table: USER_TABLE
    rows:
      - USER_ID: "001"
        USER_NAME: "山田太郎"
        AGE: "30"
        "[MARKER]": "X"
      - USER_ID: "002"
        USER_NAME: "鈴木花子"
        AGE: "25"
        "[MARKER]": "Y"
```

### 固定長ファイル（Excel 6行 → YAML records 1ブロック）

**Excel（シート上の表示）:**
```
行1: SETUP_FIXED[grp1]=input/data.dat
行2: text-encoding | MS932          ← ディレクティブ行
行3: DATA    | USER_ID | USER_NAME | AMOUNT    ← フィールド名行（先頭がレコード種別名）
行4:         | X       | N         | Z         ← データ型行（先頭空）
行5:         | 10      | 20        | 10        ← フィールド長行（先頭空）
行6:         | 001     | 山田太郎   | 0000005000 ← データ行（先頭空）
```

**YAML（変換後）:**
```yaml
setup_files:
  - group_id: grp1
    path: input/data.dat
    type: fixed
    directives:
      text-encoding: MS932
    records:
      - record_type: DATA
        fields:
          - {name: USER_ID,   type: X, length: 10}
          - {name: USER_NAME, type: N, length: 20}
          - {name: AMOUNT,    type: Z, length: 10}
        rows:
          - ["001", "山田太郎", "5000"]  # パディングは自動付与されるため不要
```

**変換のポイント:**
- Excel の3行（フィールド名・型・長さ）が `fields:` の1要素に横方向統合される
- `rows:` の各配列は `fields` と完全に同じ順序・件数で値を並べること（列順ミスはパーサがランタイムエラーで検出。JSONスキーマでは検出できない）
- **固定長ファイルの rows 値はパディング不要**: `FixedLengthDataRecordFormatter` がフィールド長に合わせて自動パディングを付与する（Excel セルに `001` と書くのと同様、YAML でも `"001"` と書けばよい）

---

## 設計上のトレードオフと注意点

### 1. テーブルデータの行表現: オブジェクト形式 vs 配列形式

**採用: オブジェクト形式**（`{USER_ID: "001", NAME: "太郎"}`）

| | オブジェクト形式（採用） | 配列形式 |
|---|---|---|
| 可読性 | 高い（カラム名が値に隣接） | 低い（カラム名と値が離れる） |
| AI書きやすさ | 高い（カラム名を都度確認不要） | 低い（列順を常に意識） |
| 冗長性 | 高い（カラム名が全行に繰り返される） | 低い |
| 一部カラム省略 | 自然（キーを書かなければ省略） | 不自然 |

カラム数が多い（15列以上）テーブルを大量行扱う場合はトークン消費が増えるが、可読性とAI利用を優先してオブジェクト形式を採用。

### 2. ファイルデータの値表現: 配列形式

**採用: 配列形式**（`["val1", "val2"]`）

固定長・可変長ファイルのレコード値は `fields` と同順の配列で表現。  
理由: フィールド名は `fields` セクションに定義済みのため、各データ行でキー名を繰り返すと冗長かつ長くなる。ファイルデータは行数が多い傾向があるため配列形式で圧縮。

**注意: テーブル系とファイル系で `rows` の形式が異なる**

| 種別 | `rows` の形式 | 例 |
|---|---|---|
| `setup_tables` / `expected_tables` / `list_maps` | **オブジェクト配列** | `[{COL: "val"}, ...]` |
| `setup_files` / `expected_files` / `messages` 等の `record_fragment` | **配列の配列** | `[["val1", "val2"], ...]` |

### 3. SETUP_FIXED と SETUP_VARIABLE の統合

Excelでは別のデータ種別だが、`BasicTestDataParser#getSetupFile()` が両者をまとめて返す実装に揃え、`type: fixed/variable` で区別する1つのセクションに統合した。`expected_files` も同様。

### 4. EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE の分離維持

両者は `getExpectedTableData()` でマージされて返されるが、`EXPECTED_COMPLETE_TABLE` が `fillDefaultValues()` を呼ぶかどうかの違いがある。YAMLでは `expected_tables` と `expected_complete_tables` を分けて保持し、変換時に呼び分けられるようにした。

#### BasicDefaultValues のデフォルト値一覧

`expected_complete_tables` で省略カラムに補完されるデフォルト値（`BasicDefaultValues` の実装）:

| カラム型 | デフォルト値 |
|---|---|
| 数値型（`java.lang.Number` のサブクラス） | `"0"` |
| 固定長文字列型（`CHAR`, `NCHAR` 等） | 半角スペース × カラム長 |
| 可変長文字列型（`VARCHAR` 等） | `" "`（半角スペース1文字） |
| 日付型（`java.sql.Date` 等） | `"1970-01-01 09:00:00.0"`（epoch、JST） |
| バイナリ型 | 10バイトのゼロバイト列の HexString |
| Boolean型 | `"false"` |

**注意**: `BasicDataTypeMapping` では「`半角数字`」は `X`（文字型）にマッピングされる（`Z`＝ゾーン10進数ではない）。設計書の「半角数字」フィールドを YAML に変換する際は `type: X` と書く。

なお、`SETUP_TABLE` / `EXPECTED_TABLE` でも各 `rows` オブジェクトに含まれないカラム（キーを省略したカラム）には INSERT 時に `DefaultValues` によるデフォルト値が補完される（`TableData#convert()` の動作）。省略カラムの補完は `EXPECTED_COMPLETE_TABLE` 専用ではない。

### 5. field_def.type と BasicDataTypeMapping の関係

**採用: YAMLにはフレームワーク型記号（`X`, `N`, `Z` 等）を記述する。**

`DataFileFragment#setTypes()` は内部で `DataTypeMapping#convertToFrameworkExpression()` を呼ぶ。  
デフォルトの `BasicDataTypeMapping` のキーは日本語設計表記（`"半角英字"`, `"全角"` 等）であるため、
YAMLパーサが `type: X` を直接 `setTypes()` に渡すと `IllegalArgumentException` が発生する。

YAML対応パーサの実装時は、`type` 値をそのままフレームワーク型記号として使用する独自の `DataTypeMapping`（identity mapping）を `SystemRepository` の `"dataTypeMapping"` キーで登録するか、パーサ側で `setTypes()` を迂回してフレームワーク型記号を直接設定する必要がある。  
この実装判断はスキーマ定義の範囲外だが、YAMLアダプタ実装時に必須の考慮事項として記録する。

### 6. マーカーカラムのキー名表現

Excel では `[COLNAME]` 形式のカラム名がマーカーとして扱われる（`HeaderLine` の規則）。  
YAMLでは `"[COLNAME]"` のようにダブルクォートで囲む必要がある。  
（クォートなしの `[COLNAME]: val` はYAMLパーサがフロー配列として誤解釈する）

### 7. 特殊値の表現と null の仕様

**null の仕様（確定）:** YAMLネイティブ `null`（アンクォート）を正式採用。

| 意図 | YAML記述 | 動作 |
|---|---|---|
| DBにNULL | `null` | YAMLパーサがJava nullとして渡す |
| DBにNULL（**NG例**） | `"null"` | QuotationTrimmerが外側クォートを除去し文字列 `null` を格納 ← 意図と逆 |
| DBに空文字 | `""` | 空文字列として渡す |
| 文字列 "null" をDBに格納（意図的） | `'"null"'` | QuotationTrimmerが外側クォートを除去して "null" を格納 |
| システム日時 | `"${systemTime}"` | DateTimeInterpreter が変換 |

**NullInterpreter は大文字小文字を区別しない**（`equalsIgnoreCase`）。`"NULL"`, `"Null"`, `"null"` のいずれも Java null に変換される。

**QuotationTrimmer は全角ダブルクォートにも対応**。半角ダブルクォート（`"..."` U+0022）だけでなく全角ダブルクォート（`"..."` U+201C/U+201D）で囲んだ値も前後1文字が除去される。クォート除去は**先頭と末尾の両方が同じクォート文字の場合のみ**適用される（片側のみはスルー）。`""abc""` → `"abc"`（最外側の1層のみ除去）。

**すべての値は文字列（クォート付き）で記述すること。** YAMLパーサが数値・真偽値として解釈するとスキーマバリデーション違反になる。

#### 日付型カラムの記述形式

テーブルデータの日付型カラムは以下の形式を受け付ける（`TableData#asYyyyMMddHHmmssSSS()`）。

| 形式 | 例 | 備考 |
|---|---|---|
| `yyyyMMddHHmmssSSS`（17文字） | `"20240101120000000"` | 標準形式 |
| 17文字未満（後置0埋め） | `"20240101"` | 後ろに `"00000000000000000"` を付加して前17文字を使用。`"20240101"` → `"20240101000000000"` |
| `yyyyMMdd HHmmss`（スペース区切り14文字） | `"20240101 120000"` | スペースを含む14文字形式 |
| `yyyyMMddHHmmssS`（ミリ秒1桁15文字） | `"200001011234560"` | ミリ秒が1桁の15文字形式 |
| JDBCタイムスタンプエスケープ（5文字目が `-`） | `"2024-01-01"`, `"2024-01-01 12:00:00.000"` | `isJdbcTimestampFormat()` で判定 |

```yaml
# NG
rows:
  - AGE: 30
    ACTIVE: true
# OK
rows:
  - AGE: "30"
    ACTIVE: "true"
```

### 8. グループIDなしの場合

Excel では `SETUP_TABLE=TABLE_NAME`（角括弧なし）がグループIDなしを意味する。  
YAMLでは `group_id:` フィールドを省略することで表現する。

### 9. SingleData系（LIST_MAP、MESSAGE）の制約

SingleData系は同一ファイル内でIDが一致した最初の1ブロックのみ取得する（`SingleDataParsingTemplate` の規則）。  
`id:` はファイル内でユニークにすることを推奨。同一 `id` の重複エントリはエラーにならず後続が黙って無視される。

また、存在しない `group_id` を `getTableData()` 等に指定した場合も例外はスローされず空リストが返る。groupId のタイプミスはランタイムエラーにならないため注意。

### 10. RESPONSE_HEADER_MESSAGES / RESPONSE_BODY_MESSAGES の2つのアクセスパス

`RESPONSE_HEADER_MESSAGES` / `RESPONSE_BODY_MESSAGES` には**2つの異なるアクセス経路**がある。

| 経路 | 呼び出し元 | パーサ | group_id |
|---|---|---|---|
| A | `RequestTestingSendSyncSupport` | `GroupMessageParser`（GroupData系） | **必須** |
| B | `MockMessagingContext` / `MockMessagingClient` | `SendSyncMessageParser`（SingleData系） | 不要 |

経路Aでは `GroupDataParsingTemplate#isTargetType()` が `group_id` でフィルタリングする。  
経路Bでは `SingleDataParsingTemplate#isTargetType()` が `id`（`=`以降の値）で照合する。  
Excel形式でいうと、経路Aは `RESPONSE_HEADER_MESSAGES[grp1]=id`、経路Bは `RESPONSE_HEADER_MESSAGES=id`。

YAMLでは `group_id` フィールドを省略した場合が経路B相当となる。

### 11. messaging テストデータの制約（RequestTestingMessagingClient）

- テストデータにステータスコード列がない場合、デフォルト `"200"` が自動使用される（明示的に記述しなくてよい）。
- `EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` の行数（records 配下の rows 合計数）は一致が必須。不一致は `IllegalStateException: "number of lines of header and body does not match."` が発生する。

### 12. MESSAGE系の record_type は装飾的（MessageParser の仕様）

`MessageParser` は内部で `FixedLengthFileParser#onReadingNames()` をオーバーライドし、先頭セル（レコード種別名）を常に固定文字列 `"default"` に置き換える（`MessageParser.java` 匿名クラス内）。  
このため `messages` / `expected_request_*_messages` の `record_type` 値（`"FW_HEADER"`, `"BODY"` 等）は識別・可読性のためだけであり、パーサの動作に影響しない。  
YAMLでは可読性のため任意の名前を書いてよいが、実行時に無視されることを認識すること。

**Excel との相違点（YAMLアダプタ実装時の注意）:** Excel では FW制御ヘッダフィールド（`requestId`, `userId` 等）は「フィールド名 | 値」の 2列ディレクティブ行形式で書かれ、`MessageParser#processDirectives()` が `isFrameworkHeader()` で判定して `fwHeader` Map に分離していた。YAMLでは通常の `fields` 配列の要素として記述し、YAMLアダプタ実装側でフィールド名を参照して `fwHeader` 分離を行う必要がある。

**`response_*_messages` での FW制御ヘッダ分離なし:** `SendSyncMessageParser`（`MockMessagingContext` / `MockMessagingClient` 経路）は `getFwHeader()` が `UnsupportedOperationException` を投げるため、FW制御ヘッダの分離は行われない。`response_*_messages` では FW_HEADER ブロックを `directives` ではなく `fields` として記述すること（`MessageParser` 経路と同一の構造にしてよい）。

### 13. Excel → YAML の行処理ルール（TestDataParsingTemplate）

- **コメント行**: 先頭セルが `//` で始まる行を行ごとスキップ（YAML では `#` コメントが同等）
- **行内コメント**: 先頭以外のセルが `//` で始まる場合、そのセル以降を切り捨て（`cutComment()`）。YAML では列の途中に `#` コメントを置くことで表現できる
- **空行スキップ**: 全セルが空（null または空文字）の行は読み飛ばされる（`isBlankLine()`）

### 14. デフォルトディレクティブの DI（拡張ポイント）

`SystemRepository` への DI でファイル種別ごとにデフォルトディレクティブを一括設定できる:

| SystemRepository キー | 適用範囲 | 根拠 |
|---|---|---|
| `"defaultDirectives"` | 全ファイル（固定長・可変長共通） | `DataFile` コンストラクタ |
| `"fixedLengthDirectives"` | 固定長ファイル専用 | `FixedLengthFile` コンストラクタ |
| `"variableLengthDirectives"` | 可変長ファイル専用 | `VariableLengthFile` コンストラクタ |

値は `Map<String, String>` で登録する。個別ファイルの `directives:` 指定がある場合はその値が優先される。

### 15. DataTypeMapping の優先検索順（拡張ポイント）

`DataFileFragment#setTypes()` は以下の優先順でマッピングを取得する:

1. `SystemRepository["dataTypeMapping_{エンコーディング名}"]`（例: `"dataTypeMapping_MS932"`）
2. `SystemRepository["dataTypeMapping"]`
3. `BasicDataTypeMapping`（デフォルト）

YAML アダプタ実装時は、フレームワーク型記号（`X`, `N` 等）を直接渡す identity mapping を `"dataTypeMapping"` キーで登録するか、パーサ側で `setTypes()` を迂回する（§5 参照）。未知の型記号は `BasicDataTypeMapping` が `IllegalArgumentException` をスローするため、identity mapping が必須。

### 16. TEST_ プレフィクス型の自動昇格

`"TEST_X9"` のように `TEST_` プレフィクスのデータ型が `ConvertorFactory` に登録されている場合、YAML に `type: X9` と書いてもパーサが `getTypeForTest()` で `TEST_X9` を自動優先選択する（`DataFileFragment`）。テスト専用の型シンボルを使いたい場合は `TEST_` プレフィクスで登録すると既存の type 記述を変えずに切り替えできる。

### 17. TestDataConverter 拡張点

`SystemRepository["TestDataConverter_" + file-type]`（例: `"TestDataConverter_Fixed"`）に `TestDataConverter` 実装を登録することで、レイアウト定義の生成（`createDefinition()`）とデータレコードの変換（`convertData()`）をカスタマイズできる（`FixedLengthFile` / `VariableLengthFile`）。

### 18. SendSyncSupport のテストデータ配置規則

`MockMessagingContext` / `MockMessagingClient` 経由の同期送信テスト（`SendSyncSupport`）では、以下の規則でデータファイルを配置する:

- ベースパス: `FilePathSetting["sendSyncTestData"]` で設定されるディレクトリ
- ファイル配置: `{ベースパス}/{requestId}/message.xlsx`（Excel時）→ YAML 移行後は `{ベースパス}/{requestId}/message.yaml` 等
- シート名（Excel）: `"message"` 固定（`SendSyncSupport.RESPONSE_MESSAGES_SHEET_NAME = "message"`）

呼び出し毎にレコードを順番に消費するキャッシュ機構がある（ファイルのタイムスタンプが変わらない限りキャッシュを使いまわし、内部カウンタで次レコードを返す）。

### 19. messaging.assertAsMapFileType によるアサート方式切り替え

`RequestTestingMessagingClient` は `SystemRepository["messaging.assertAsMapFileType"]`（デフォルト: `"Fixed"`）の値と一致するファイルタイプのメッセージを DataRecord 単位で検証する。一致しないファイルタイプは電文バイト列を文字列全体で比較する。

### 20. メッセージフォーマット定義ファイルの命名規則（RequestTestingMessagingClient）

HTTP系リクエスト単体テストでは、以下の規則でフォーマット定義ファイルを検索する:

- 送信電文フォーマット: `{requestId}_SEND`（`requestMessageFormatFileNamePattern`）
- 応答電文フォーマット: `{requestId}_RECEIVE`（`responseMessageFormatFileNamePattern`）

これらのファイルは `FilePathSetting["format"]` ベースパス配下に配置する。

### 21. BinaryFileInterpreter のパス基準

`${binaryFile:相対パス}` のファイルパスは、Excel ファイルのディレクトリを基準とした相対パスで解決される（`BinaryFileInterpreter` コンストラクタの `path` 引数）。YAML 移行後は YAML ファイルのディレクトリを基準とするか、絶対パスで解決するかをアダプタ実装時に統一すること。

### 22. DateTimeInterpreter の完全一致制約

`DateTimeInterpreter` は値が `${systemTime}`, `${setUpTime}`, `${updateTime}` と**完全一致**する場合のみ変換する（Map lookup）。`"${systemTime}_suffix"` のような部分文字列が含まれる複合式は、`CompositeInterpreter` の `${...}` セグメントとして分解してから渡す必要がある。

`${setUpTime}` の変換後の値は JDBC タイムスタンプ書式（`yyyy-MM-dd HH:mm:ss.SSS`）形式で設定する必要がある（`DateTimeInterpreter#setSetUpDateTime()` のバリデーション）。

### 23. CompositeInterpreter の DI 設定

`CompositeInterpreter` は `interpreters` プロパティに `TestDataInterpreter` のリストを DI しないと機能しない（デフォルトは空リスト）。`DateTimeInterpreter`, `BasicJapaneseCharacterInterpreter`, `BinaryFileInterpreter` 等を登録することで各 `${...}` セグメントの解釈が有効になる。

### 24. 1ファイルセクション内の複数レコードレイアウト（DataFileParser の状態機械）

1つの `record_fragment` ブロック（`records:` の1要素）がレコード種別1つに対応する。  
1つのファイルセクション（`file_data` 1件）内に複数の `record_fragment` を並べることで、複数レコードレイアウトを持つファイルを表現できる。

```yaml
setup_files:
  - path: input/multi_layout.dat
    type: fixed
    directives:
      text-encoding: MS932
    records:
      - record_type: HEADER        # レコード種別1
        fields:
          - {name: TYPE,    type: X, length: 4}
          - {name: DATE,    type: X, length: 8}
        rows:
          - ["HDR", "20240101"]
      - record_type: DATA          # レコード種別2（連続して記述）
        fields:
          - {name: ID,      type: X, length: 10}
          - {name: VALUE,   type: Z, length: 10}
        rows:
          - ["0000000001", "5000"]
          - ["0000000002", "9800"]
      - record_type: TRAILER       # レコード種別3
        fields:
          - {name: TYPE,    type: X, length: 4}
          - {name: COUNT,   type: Z, length: 6}
        rows:
          - ["TRL", "2"]
```

`DataFileParser` の状態機械は `READING_DIRECTIVES_AND_NAMES` → `READING_TYPES` → `READING_LENGTHS`（固定長のみ）→ `READING_VALUES` の順序を繰り返す。  
フィールド名行（先頭セルが非空・非ディレクティブ）を読むと `READING_TYPES` に遷移し、型行・長さ行・データ行を読んだ後、再びフィールド名行（= 次のレコード種別の先頭）が来ると次のブロックとして扱う。

### 25. `"-"` 長フィールドの最終サイズ決定ルール

`DataFileFragment` でフィールド長に `"-"` を指定した場合（`ONDEMAND_CALC_FIELD_SIZE`）、そのフィールドの最終的なバイト長は **そのフィールドに追加された全レコード中の最大バイト長** となる。

具体的には `addValue()` が呼ばれるたびに現在の最大バイト長と比較更新され、すべてのレコードが追加し終わった時点の最大値が使用される。  
また、`"-"` フィールドへ格納される値は `removeLineSeparatorWithTrim()` により**改行コードと前後空白が除去**されてから長さが計算される。

---

## 段階的移行戦略

### ExcelとYAMLの並存

現状のNTFパーサ（`PoiXlsReader` + `BasicTestDataParser`）はExcelのみを読み込む実装になっている。
YAML対応のパーサを追加実装する際は、`TestDataReader` インタフェースを実装したYAMLパーサを作成し、`BasicTestDataParser`（あるいはそのファクトリ）でファイル拡張子（`.yaml`/`.yml`）により `PoiXlsReader` と切り替えるロジックを追加する。NTF が Reader を DI で差し込む構造の場合は、コンポーネント設定ファイルの変更も必要。

段階的な移行手順:

1. **段階1: YAMLパーサの追加実装**  
   拡張子切り替えロジックを含め、既存 `PoiXlsReader` と共存させる。

2. **段階2: テストクラス単位での移行**  
   各テストクラスが参照するファイルをExcel→YAMLに1ファイルずつ変換する。  
   変換ツール（Excel→YAML変換スクリプト）を整備して機械的に移行。

3. **段階3: Excelの廃止**  
   全ファイルのYAML移行完了後、`PoiXlsReader` への依存を削除。

### 移行優先度の基準

以下の順で移行を優先することを推奨する。

- **優先度高**: 更新頻度が高いExcelファイル（手書きコストが高い）
- **優先度高**: テーブルデータのみで構成されるシンプルなExcel（変換が容易）
- **優先度低**: 固定長ファイル定義が複雑なExcel（変換スクリプトの作り込みが必要）
- **後回し可**: 更新頻度が低く安定しているExcel（移行コストに見合わない）

### 変換ツール方針

自動変換スクリプトの実装時には以下に注意する。

- テーブル名・カラム名は `toUpperCase()` されているため、YAML側では大文字で出力する
- マーカーカラム（`[COLNAME]`）はYAMLキーとして `"[COLNAME]"` にクォートする
- Excel のセル値が空（`""`）でも意図的に空文字として出力する（省略しない）
- `null` セルは `null` として出力する
- **Excelのセルが数値型で保存されている場合**（例: `001` が整数 `1` として格納）は、POI の `DataFormatter#formatCellValue(cell)` で文字列化してから取得する（`cell.setCellType(STRING)` は POI 4.x 以降で削除されたため使用不可）
- **複数シートのExcelファイルは1シート1YAMLファイルに分割する（選択肢A）**: `FooTest.xlsx` の各シート（`setUpDb`, `testMethod1` 等）をそれぞれ `FooTest.setUpDb.yaml`, `FooTest.testMethod1.yaml` として独立したファイルに出力する。1ファイル複数シート相当の構造をスキーマに追加する選択肢B は既存スキーマの破壊的変更が必要なため採用しない。先行実装例（nablarch-example-*-ntf-yaml）もフラット変換（1シート→1ファイル）方式を採用しており整合が取れる
- **`dataName`（リソース名）の形式変更に注意**: 既存テストでは `PoiXlsReader` が `"ファイル名/シート名"` 形式のキーでデータをキャッシュする。YAML移行後はシートの概念がなくなるため、YAMLパーサのキャッシュキー形式をプロジェクトルールで統一すること（例: `"ファイル名"` のみ、または `"ファイル名/default"` など）。テストクラスが参照するリソース名もあわせて変更が必要


---

## AI向けプロンプト補助情報

このスキーマをAIにテストデータ生成させる際に一緒に渡すべき補助情報:

```
# NTF テストデータ YAML 生成ルール

## rows の形式の区別
- テーブル系（setup_tables / expected_tables / expected_complete_tables / list_maps）の rows は
  オブジェクト配列: [{COL: "val"}, ...]
- ファイル系（setup_files / expected_files / messages /
  expected_request_header_messages / expected_request_body_messages /
  response_header_messages / response_body_messages）の record_fragment の rows は
  配列の配列: [["val1", "val2"], ...]

## expected_tables と expected_complete_tables の使い分け
- expected_tables: 記述したカラムのみを比較する
- expected_complete_tables: 記述していないカラムにも BasicTestDataParser#fillDefaultValues() で
  デフォルト値が補完され、全カラムを比較する。省略カラムが多い場合に使う

## 値の型ルール
- すべての値は文字列型（ダブルクォート）で記述すること
- 数値・真偽値もクォートする: "30", "true"
- DBにNULLを入れる場合: null （YAMLキーワード、クォートなし）
- DBに空文字を入れる場合: "" （ダブルクォート2つ）

## record_fragment の列順保証
- records[].rows の各配列は、同ブロックの fields 配列と完全に同じ順序・同じ件数で値を並べること

## group_id の省略ルール
- グループIDがない場合は group_id フィールド自体を省略すること（null や "" は不可）
- group_id に null や "" を指定すると空文字列のグループIDとして扱われ誤マッチが起きる

## SingleData 系の id 一意制約
- list_maps / messages / expected_request_header_messages / expected_request_body_messages は
  ファイル内で id がユニークでなければならない（重複時は最初の1件のみ取得）
- 同一テストシナリオで複数バリエーションが必要な場合は別の id を使うこと

## ディレクティブの field-separator
- タブ区切りを指定する場合: field-separator: "\\t"  （バックスラッシュ+t の2文字文字列。VariableLengthFile がタブ文字 U+0009 に変換する）
- field-separator は1文字のみ有効（"\\t" 変換後は1文字となるため "\\t" も有効）

## ディレクティブの quoting-delimiter
- ダブルクォート1文字を指定する場合: quoting-delimiter: '"'  （シングルクォートで囲む）
- "\""（バックスラッシュエスケープ）でも同じ結果だが '"' の方が可読性が高い

## ディレクティブの boolean 値はクォート不要
- required-decimal-point / fixed-sign-position / required-plus-sign /
  ignore-blank-lines / requires-title はスキーマで boolean 型として定義
- rows フィールドの値と異なり、true / false とクォートなしで記述すること
  （"true" や "false" ではなく true / false）

## ディレクティブの record-separator
- record-separator の値は YAML ダブルクォート文字列内でエスケープシーケンスを使う
- CRLF: "\r\n" （正しい）
- LF:   "\n"   （正しい）
- "\\r\\n" はバックスラッシュ+r+バックスラッシュ+n の4文字になるため誤り
- シンボル形式（"CRLF" / "LF" / "CR" / "NONE"）も有効

## 列順ミスはスキーマでは検出されない
- record_fragment の rows は fields の順序に対応するが、列ズレは JSON Schema で検出できない
- fields に定義した順序と rows の値の順序を必ず目視で確認すること
- 列順ミスはパーサのランタイムエラーまで発覚しない

## マーカーカラム
- キー名を "[COLNAME]" と角括弧で囲みダブルクォートする
- 値は任意の文字列（マーキング用途。DB操作から除外される）

## 特殊値
- null（DB NULL）: null  ← クォートなしの YAML キーワード。"null" と書くと文字列 null が格納される（意図と逆）
- 空文字: ""
- システム日時: "${systemTime}"
- セットアップ時刻: "${setUpTime}"
- 文字種生成（例）: "${全角英字, 10}"  ← BasicJapaneseCharacterInterpreter（14種のトークンが有効）
- バイナリファイル: "${binaryFile:path/to/file.bin}"
- CR文字: "\r"  ← ファイル系レコード値のみ有効
- 複合式: "${半角数字,4}-${半角数字,4}" は CompositeInterpreter が各 ${} を個別解釈して結合

## BasicJapaneseCharacterInterpreter の有効トークン（14種）
半角英字 / 半角数字 / 半角記号 / 半角カナ /
全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 /
中国語 / サロゲートペア / 改行 / 外字
- 書式 ${文字種,文字数} にマッチしない入力はスルーされる（例外なし）
- 書式はマッチするが文字種が未知の場合は IllegalArgumentException がスローされる（スキーマでは検出できないが実行時にエラー）
- ${半角記号} の生成には ", #, ,, \ は含まれない（JapaneseCharacterSet.ASCII_SYMBOL の除外リスト）

## ファイル系の空行動作
- 可変長ファイルの空行はスキップされない。全フィールドが "" のレコードとして保持される
  （ignore-blank-lines ディレクティブを true にすると空行をスキップできる）
- 固定長ファイルの空行はスペースパディングされた定長レコードとして書き出される（0バイト行にはならない）

## LIST_MAP 重複セクションの先着一致
- 同一 YAML ファイル内に同じ id を持つ list_maps エントリが複数存在する場合、最初の1件のみ読まれる
- 後続の同 id エントリは黙って無視される（エラーにはならない）

## group_id が存在しない場合の挙動
- 存在しない group_id を指定した場合、例外はスローされず空リストが返る
- テストが意図せず group_id をタイプミスした場合も例外で検出されないため注意

## messaging（RequestTestingMessagingClient）の注意事項
- テストデータにステータスコード列（_nbctlhdr.statusCode 等）がない場合、デフォルト "200" が自動使用される
- EXPECTED_REQUEST_HEADER_MESSAGES と EXPECTED_REQUEST_BODY_MESSAGES の行数（records 内の rows 数）は一致が必須
  行数不一致は IllegalStateException: "number of lines of header and body does not match." が発生する

## messages / expected_request_*_messages の record_type に注意
- MessageParser は record_type の値を無視し、内部的に "default" という固定名に置き換える
- record_type は識別用途のみ（FW_HEADER, BODY 等の名前を書いても動作に影響しない）
- フィールド定義（fields）の内容のみが実際の解析に使われる

## response_*_messages の errorMode（MockMessagingContext/Client 経路のみ）
- SendSyncMessageParser は rows 先頭値が "errorMode:timeout" または "errorMode:msgException"
  の場合、そのレコードをエラーモードマーカーとして扱い送受信エラーをシミュレートする
- RequestTestingSendSyncSupport 経路（GroupMessageParser）では errorMode は未使用
```

---

## 成果物ファイル一覧

| ファイル | 内容 |
|---|---|
| `ntf-testdata-structure.md` | Phase 1: コード調査報告（データ構造の完全な記述） |
| `ntf-testdata-yaml-schema.json` | Phase 2: JSON Schema定義 |
| `ntf-testdata-yaml-examples.yaml` | Phase 2: 各データ種別のYAML記述例 |
| `ntf-testdata-yaml-design.md` | Phase 2: 設計判断・トレードオフ（本ファイル） |
| `tasks.md` | 作業タスクリスト（中断・再開用） |
