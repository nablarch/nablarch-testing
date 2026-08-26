# Step 4-01 — nablarch-testing を解説書に合わせる作業記録

指示書: `nablarch-document` `ntf-yaml-support` `87a21d6`:`.rn/20260724-ntf-yaml-support/ntf-step4-01-nablarch-testing.md`
（`e634ffd` の版は破棄済み。差分は「`src/main` を変更しない」「§4-2 では `src/test` も変更しない」= 不一致は
どちらの側が誤りでも直さず報告して止める。user 判断 2026-08-26）

## 参照点（ピン）

| 対象 | リポジトリ / ブランチ | SHA |
|---|---|---|
| 解説書（SSoT） | `nablarch-document` / `ntf-yaml-support` | `40b9c52` |
| 本モジュール | `nablarch-testing` / `convert-testdata-excel-to-text` | `3c4bd2a` |

本記録で `解説書:<path>:<行>` と書いたものは `git show 40b9c52:ja/development_tools/testing_framework/<path>` の行番号、
`<path>:<行>` と書いたものは `git show 3c4bd2a:<path>` の行番号である。

作業ツリーの `src/` は `3c4bd2a` と同一であることを実測済み（`git diff --stat 3c4bd2a HEAD` の差分は `docs/pr75/steering.md` のみ）。

---

## §4-1 論点4 — フィールド数を超えた値を無警告で捨てる挙動

### 判定: **踏まない＝仕様**（利用者影響なし）

判定基準は「解説書のとおりに正しく書こうとした利用者が、この挙動を踏むか」。踏まない。

### 根拠 — 値が `addValue` に届くまでの経路を末端まで追った結果

切り捨ての実体は `src/main/java/nablarch/test/core/file/DataFileFragment.java:105`（`addValue`）と
`:173`（`addValueWithId`）の `for (int i = 0; i < names.size(); i++)`。
`names` は `setNames`（同 `:190`）で設定され、設定元は
`src/main/java/nablarch/test/core/reader/DataFileParser.java:259-263`（`createNewFragment`）の
`setNames(tail(fieldNamesLine))`、すなわち**フィールド名称行**である。

データ行が `addValue` に届くまでに、行は次の3段階を通る。

| # | 処理 | 実物 | 効果 |
|---|---|---|---|
| 1 | Excel の1行を読む | `PoiXlsReader.java:119-130` | `row.getLastCellNum()` までを読む。行ごとに長さが異なる（ragged） |
| 2 | コメントの切り落とし | `TestDataParsingTemplate.java:179` → `cutComment`（同 `:390-399`） | `//` で始まるセル以降を捨てる。全データタイプに共通 |
| 3 | 末尾の空要素の除去 | `DataFileParser.java:68` → `NablarchTestUtils.trimTailCopy`（`NablarchTestUtils.java:273-279` → `trimTail` 同 `:251-263`） | 末尾の空要素（`null` または空文字）を除去する |
| 4 | 先頭要素の除去 | `DataFileParser.java:197` の `tail(line)`（同 `:272-277`） | 先頭のラベル列／レコード種別列を落とす |

3 により、`addValue` に渡る `line` は**末尾が必ず非空**である。
したがって `line.size() > names.size()` が成り立つのは、
**最後のフィールド名称より右の列に非空の値が書かれている場合に限られる**。

解説書はこの書き方を認めていない。

- 解説書:`implementation/testdata_notation.rst:1055` — フィールド名称は「フィールドの数だけ記載する」
- 解説書:`implementation/testdata_notation.rst:882` — 「フィールドの数だけ値を並べる必要はない。**末尾のフィールドの値を書かなければ**、そのフィールドは `""` として扱われる」（少ない側のみ）
- 解説書:`implementation/testdata_notation.rst:1155` — メッセージボディも同文（少ない側のみ）
- 解説書:`implementation/testdata_notation.rst:786` — テーブルデータも「データ行のセル数がヘッダ行より少ない場合」のみ

多い側の記述は解説書に存在しない（指示書のディレクター実測と、本作業での `testdata_notation.rst` 全1,502行の通読の両方で確認）。

データの右側に何かを書く**正規の記法**は2つあるが、いずれも上記 2・3 で落ちるため
`names.size()` の上限に到達しない。

- 末尾の空セル（罫線・書式の都合で `getLastCellNum()` が伸びた場合を含む）→ 3 で除去
- 末尾のコメントセル（解説書:`implementation/testdata_notation.rst:1453`・`:1480`）→ 2 で除去

呼び出し元4箇所のうち `SendSyncMessageParser.java:129` の `addValue(list)` は
要素1個のリストを渡す障害系（`errorMode:`）の経路であり、少ない側（空文字補完）にしか進まない。
同 `:134` の `addValueWithId(temp, temp.remove(0))` は先頭のラベル列（電文の連番。
解説書:`implementation/testdata_notation.rst:1266`）を除いた残りを渡すため、
フィールド名称行と同じ整列になる。

### 処置 — 現行挙動を固定する特性テストを追加した

| テスト | 場所 | 押さえるもの |
|---|---|---|
| `testAddValueDiscardsValuesBeyondNames` | `src/test/java/nablarch/test/core/file/FixedLengthFileFragmentTest.java` | `addValue` の超過分切り捨て |
| `testAddValueWithIdDiscardsValuesBeyondNames` | 同上 | `addValueWithId` の超過分切り捨て |
| `testAddValuePadsMissingValuesWithEmptyString` | 同上 | 少ない側（解説書:`testdata_notation.rst:882`）との対 |
| `testValuesBeyondFieldNamesAreDiscarded` | `src/test/java/nablarch/test/core/reader/FixedLengthFileParserTest.java` | パーサ経由（公開 API `DataFile#toDataRecords()`）での超過分切り捨て |
| `testTrailingEmptyCellsAreNotTreatedAsExcess` | 同上 | 末尾の空セルが超過分にならないこと（経路3） |
| `testTrailingCommentCellIsNotTreatedAsExcess` | 同上 | 末尾のコメントセルが超過分にならないこと（経路2） |

### 負のテスト（完了条件2）

`DataFileFragment.java:105` と `:173` のループ上限を `names.size()` → `line.size()` に変えて実行した。

```
$ sed -i 's|for (int i = 0; i < names.size(); i++) {|for (int i = 0; i < line.size(); i++) {|g' \
    src/main/java/nablarch/test/core/file/DataFileFragment.java
$ LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o test \
    -Dtest="FixedLengthFileFragmentTest,FixedLengthFileParserTest" -DfailIfNoTests=false
```

```
[ERROR] Tests run: 20, Failures: 1, Errors: 2, Skipped: 0 - in nablarch.test.core.file.FixedLengthFileFragmentTest
[ERROR] Tests run: 4, Failures: 0, Errors: 1, Skipped: 0 - in nablarch.test.core.reader.FixedLengthFileParserTest
[ERROR]   FixedLengthFileFragmentTest.testAddValuePadsMissingValuesWithEmptyString:373
[ERROR]   FixedLengthFileFragmentTest.testAddValueDiscardsValuesBeyondNames:311 » ArrayIndexOutOfBounds
[ERROR]   FixedLengthFileFragmentTest.testAddValueWithIdDiscardsValuesBeyondNames:340 » ArrayIndexOutOfBounds
[ERROR]   FixedLengthFileParserTest.testValuesBeyondFieldNamesAreDiscarded:60 » IllegalState
[ERROR] Tests run: 24, Failures: 1, Errors: 3, Skipped: 0
[INFO] BUILD FAILURE
```

失敗の中身:

```
[ERROR] testAddValuePadsMissingValuesWithEmptyString  <<< FAILURE!
java.lang.AssertionError:
Expected: is <3>
     but: was <1>
	at ...FixedLengthFileFragmentTest.testAddValuePadsMissingValuesWithEmptyString(FixedLengthFileFragmentTest.java:373)

[ERROR] testAddValueWithIdDiscardsValuesBeyondNames  <<< ERROR!
java.lang.ArrayIndexOutOfBoundsException: Index 2 out of bounds for length 2
	at ...FixedLengthFileFragmentTest.testAddValueWithIdDiscardsValuesBeyondNames(FixedLengthFileFragmentTest.java:340)
```

超過分を押さえる3件（`testAddValueDiscardsValuesBeyondNames`・
`testAddValueWithIdDiscardsValuesBeyondNames`・`testValuesBeyondFieldNamesAreDiscarded`）が
すべて落ちる。少ない側の `testAddValuePadsMissingValuesWithEmptyString` も落ちる。

経路2・経路3 を押さえる2件（`testTrailingEmptyCellsAreNotTreatedAsExcess`・
`testTrailingCommentCellIsNotTreatedAsExcess`）はこの変異では落ちない。
これらは切り捨てそのものではなく、切り捨てに**到達しない**ことを押さえるテストであるため、
検知対象が異なる。超過分の挙動を押さえているのは上記3件である。

変異を戻したあとの再実行:

```
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 - in nablarch.test.core.file.FixedLengthFileFragmentTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 - in nablarch.test.core.reader.FixedLengthFileParserTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## §4-2 解説書との全件突合

### 母集合（先に固定した）

`git show 40b9c52` の `ja/development_tools/testing_framework/` 配下の `.rst` を機械的に列挙した結果、
**38ファイル・9,822行**。指示書の母集合と一致する。

```
$ git ls-tree -r 40b9c52 --name-only ja/development_tools/testing_framework/ | grep '\.rst$' | wc -l
38
$ (各ファイルの wc -l の総和)
9822
```

全件表は下記に1ページずつ追記する。

### 全件表

（着手順に追記）

#### `about/index.rst`（112行）— 全行通読

| 行 | 記述の要旨 | 対象／対象外 | 実装での成否 | 根拠 | 判定 | 処置 |
|---|---|---|---|---|---|---|
| :14 | クラス単体／リクエスト単体／取引単体の3粒度。トランザクション制御・システム日時設定のAPIも提供する | 対象 | 一致 | `DbAccessTestSupport.java:96,132,139,147`（`beginTransactions`／`commitTransactions`／`rollbackTransactions`／`endTransactions`）、`FixedSystemTimeProvider.java:20`。3粒度は `HttpRequestTestSupport.java`・`EntityTestSupport.java`・`IntegrationTestSupport.java` の存在 | 一致 | なし |
| :20 | リクエスト単体テストはハンドラキューを通して実行される | 対象 | 一致 | `HttpRequestTestSupport.java:334-336`（`controller.getHandlerQueue()` → `prepareHandlerQueue` → `server.setHandlerQueue`） | 一致 | なし |
| :24 | テストロジックはFWが提供し、利用者はテストデータを別ファイルに宣言的に書く | 対象 | 一致 | `TestSupport.java`・`DbAccessTestSupport.java:299-357`（`assertTableEquals` 群）・`DbAccessTestSupport.java:165-175`（`setUpDb`） | 一致 | なし |
| :28 | テストデータはExcel形式とYAML形式のいずれかで書け、相互に変換できる | 一部対象外 | 一致（Excel側） | Excel側は `PoiXlsReader.java:30`。YAML固有の記法・パーサは `nablarch-testing-yaml` 担当、相互変換は `nablarch-testing-converter` 担当のため、その2点は対象外 | 一致 | なし |
| :32・:112 | JUnit 4を基盤とする | 対象 | 一致 | `pom.xml:151-154`（`junit:junit:4.13.1`、`scope=compile`） | 一致 | なし |
| :34-44 | テストメソッドは `@Test` を付与して作る | 対象 | 一致 | 同上（JUnit 4 のアノテーションをそのまま使う） | 一致 | なし |
| :48 | `@Before`・`@After` も使用できる | 対象 | 一致 | 同上 | 一致 | なし |
| :64-75 | 3種類のテストの実行方法・範囲の表 | 対象 | 一致 | `HttpRequestTestSupport.java`（リクエスト単体）・`EntityTestSupport.java`（クラス単体）・`IntegrationTestSupport.java`（取引単体） | 一致 | なし |
| :77 | 取引単体テストは、ウェブは手動、RESTfulウェブサービスとバッチはリクエスト単体テストの連続実行でJUnit自動実行できる | 対象 | 一致 | `IntegrationTestSupport.java`（本体側の起点）。RESTful固有の `SimpleRestTestSupport` 派生は `nablarch-testing-rest` 担当 | 一致 | なし |
| :86-91 | リクエスト単体テストは処理方式で6つに分かれる | 対象 | 一致 | ウェブ=`HttpRequestTestSupport.java`、バッチ=`BatchRequestTestSupport.java`、MOM／HTTPメッセージング=`MessagingRequestTestSupport.java`・`MessagingReceiveTestSupport.java`、テーブルキュー=バッチと同じ経路（`BatchRequestTestSupport.java`）。RESTfulは `nablarch-testing-rest` 担当のため対象外 | 一致 | なし |
| :96 | Jakarta Batch 準拠のバッチアプリケーションは対象外 | 対象 | 一致 | `git grep -rln "jsr352\|jakarta.batch\|javax.batch" 3c4bd2a -- src` が0件 | 一致 | なし |
| :100 | マルチスレッド機能を使うアプリケーションは対象外 | 対象 | 一致 | `git grep -rln "MultiThread\|multiThread" 3c4bd2a -- src/main/java` が0件 | 一致 | なし |
| :106 | テストクラスはテスト対象を直接呼び出し、テストデータをFW経由で読み取る | 対象 | 一致 | `TestSupport.java`（読み込みの入口）・`BasicTestDataParser.java` | 一致 | なし |

対象外と判定した記述と理由:

- :28 の「YAML形式で記述できる」— YAML固有の記法・パーサは `nablarch-testing-yaml` の指示書が担当する
- :28 の「両形式は相互に変換できる」— 変換ツールは `nablarch-testing-converter` の指示書が担当する
- :87 の「リクエスト単体テスト（RESTfulウェブサービス）」— `RestTestSupport`・`SimpleRestTestSupport` 固有の記述は `nablarch-testing-rest` の指示書が担当する

集計: 対象 12件（うち一部対象外を含む行1件）／全件一致／不一致（解説書が正）0件／不一致（解説書側の誤りの疑い）0件。


#### `setup/index.rst`（21行）— 全行通読

| 行 | 記述の要旨 | 対象／対象外 | 実装での成否 | 根拠 | 判定 | 処置 |
|---|---|---|---|---|---|---|
| :1 | 参照ラベル `_testing_framework_setup:` | 対象外 | — | 解説書内の相互参照ラベル | — | なし |
| :3-4 | 見出し「テスティングフレームワークの導入と設定」 | 対象外 | — | 見出し文字列 | — | なし |
| :6-21 | `toctree`（`common` 以下13エントリ） | 対象外 | — | 13エントリすべて `40b9c52` に実在（`git cat-file -e 40b9c52:ja/development_tools/testing_framework/setup/<name>.rst` が13件とも成功） | — | なし |

対象外と判定した記述と理由:

- :1 の参照ラベル — 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない
- :3-4 の見出し — 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない
- :6-21 の `toctree` — 解説書のページ構成であり、`nablarch-testing` の実装で成否が決まらない（参照先の実在は上表のとおり確認済み）

集計: 対象 0件／一致 0件／不一致（解説書が正）0件／不一致（解説書側の誤りの疑い）0件。
本ページは `toctree` のみで構成されており、実装で成否が決まる記述は1件もない。

#### `setup/common.rst`（259行）— 全行通読

| 行 | 記述の要旨 | 対象／対象外 | 実装での成否 | 根拠 | 判定 | 処置 |
|---|---|---|---|---|---|---|
| :13 | 共通設定で扱うのは、依存関係の追加・読み込み先の変更・システム日時の固定・シーケンス採番のテーブル採番への置き換え・同期応答／HTTPメッセージ送信のテストデータ読み込み | 対象 | 一致（採番置換の帰属は :126 の行で判定） | 依存関係=`pom.xml:7-9`、読み込み先=`TestSupport.java:30,33,356-361`、日時固定=`FixedSystemTimeProvider.java:20,42-55`、同期応答=`SendSyncSupport.java:49,346,473`。採番置換だけは本モジュールに実装がない | 一致 | なし |
| :20-29 | テスティングフレームワークは `nablarch-testing` として提供され、`test` スコープで依存関係に追加する | 対象 | 一致 | `pom.xml:7-9`（`groupId=com.nablarch.framework`／`artifactId=nablarch-testing`）。JUnit を `compile` スコープで持つ（`pom.xml:150-155`）ため、利用側は `test` スコープで足りる | 一致 | なし |
| :33 | 処理方式によっては専用モジュールを使う。専用モジュールが `nablarch-testing` に依存するなら個別追加は不要 | 対象外 | — | 他モジュールの `pom.xml` が決める | — | なし |
| :39 | テストデータはデフォルトでは Excel 形式で読み込まれる。Excel 形式なら設定は不要 | 一部対象外 | 一致（Excel を読むクラスの提供） | Excel を読むクラスは本モジュールが提供する（`PoiXlsReader.java:30`）。ただし既定の `testDataParser` コンポーネント設定は本モジュールに同梱されていない（`git ls-tree -r 3c4bd2a --name-only -- src/main/resources` は `BatchSample.sql`・`template.xls` の2件のみ。未設定時は `TestSupport.java:404-407` が `IllegalStateException` を送出する）ため、「デフォルト」の成否はブランクプロジェクト側の設定が決める | 一致 | なし |
| :41 | YAML 形式のテストデータを解析するクラスは `nablarch-testing-yaml` が提供する | 対象外 | — | 本モジュールに `Yaml*` クラスは存在しない（`git ls-tree -r 3c4bd2a --name-only -- src/main/java \| grep -i yaml` が0件）。提供側の確認は `nablarch-testing-yaml` 担当 | — | なし |
| :52 | テストデータを解析するコンポーネントの名前は `testDataParser` | 対象 | 一致 | `TestSupport.java:404`・`MasterDataSetUpper.java:188` がともに `SystemRepository.get("testDataParser")` | 一致 | なし |
| :57-68 | `yamlInterpreters` に `DateTimeInterpreter`（`systemTimeProvider` プロパティ）・`CompositeInterpreter`（`interpreters` プロパティ）・`BasicJapaneseCharacterInterpreter` を指定する | 対象 | 一致 | 3クラスとも本モジュールが提供（`git ls-tree -r 3c4bd2a --name-only -- src/main/java \| grep interpreter`）。プロパティは `DateTimeInterpreter.java:70`（`setSystemTimeProvider`）・`CompositeInterpreter.java:61`（`setInterpreters(List<TestDataInterpreter>)`）・`BasicJapaneseCharacterInterpreter.java:18` | 一致 | なし |
| :71-75 | テストデータ解析コンポーネントは `dbInfo`・`interpreters` プロパティを持つ | 対象 | 一致 | `TestDataParser.java:100`（`setDbInfo`）・`:107`（`setInterpreters`）。実装は `BasicTestDataParser.java:221,230` | 一致 | なし |
| :77 | Excel 形式で必要な `NullInterpreter`・`QuotationTrimmer`・`LineSeparatorInterpreter` は YAML 形式では指定しない | 一部対象外 | 一致（3クラスの所属と Excel 側での使用） | 3クラスとも本モジュールが提供（`NullInterpreter.java:8`・`QuotationTrimmer.java:9`・`LineSeparatorInterpreter.java:28`）。Excel 形式の設定例で実際に使われている（`src/test/resources/unit-test.xml:30,33,40`）。「YAML のパーサが構文として解釈するから不要」の部分は `nablarch-testing-yaml` 担当 | 一致 | なし |
| :81 | `NullInterpreter` を指定すると、文字列として書いた `"null"` も Java の `null` になり区別できなくなる | 対象 | 一致 | `NullInterpreter.java:15-16`（`equalsIgnoreCase` で判定し `null` を返す）。実行して確認（下記「動かして確かめた内容」[G]）：`"null"`→`null`、`"NULL"`→`null`、`QuotationTrimmer`＋`NullInterpreter` に `"null"`（ダブルクォート付き）を通しても `null` | 一致 | なし |
| :83 | `testDataReader` は指定しない（`YamlTestDataParser` は YAML ファイルを直接読むため使用しない） | 対象外 | — | `testDataReader` というプロパティ名の出所は本モジュール（`TestDataParser.java:92`）だが、`YamlTestDataParser` が使わないことの確認は `nablarch-testing-yaml` 担当 | — | なし |
| :85 | テストデータの記法は別ページを参照 | 対象外 | — | 解説書内の相互参照 | — | なし |
| :89 | テストデータはデフォルトでは `src/test/java` 配下から読み込まれる。変更する場合に `nablarch.test.resource-root` を設定する。値はカレントディレクトリからの相対パス | 対象 | **不一致** | 下記「不一致の詳細 :89」参照 | **不一致（解説書側の誤りの疑い）** | 報告のみ（実装・テストとも変更しない） |
| :95-99 | 読み込み先はセミコロン（`;`）区切りで複数指定できる | 対象 | 一致 | `TestSupport.java:42`（`PATH_SEPARATOR = ";"`）・`:329`（`split(PATH_SEPARATOR)`）・`:339-346`。既存テスト `TestSupportTest.java:594-604` が `"java;resources"` → `["java/nablarch/test", "resources/nablarch/test"]` を検証。実行して緑を確認（下記 [T]） | 一致 | なし |
| :103 | 同名のテストデータが複数のディレクトリに存在する場合、最初に見つかったものが読み込まれる | 対象 | 一致 | `TestSupport.java:308-315`（候補パスを先頭から走査し、最初にヒットしたものを返す）。既存テスト `TestSupportTest.java:606-613` が候補順どおりの結果を検証。実行して緑を確認（下記 [T]） | 一致 | なし |
| :107 | 一時的な変更は `-Dnablarch.test.resource-root=...` をシステムプロパティで指定してもよい | 対象 | 一致 | `TestSupport.java:357` が `SystemRepository.get("nablarch.test.resource-root")` で読むだけなので、`SystemRepository` が解決した値がそのまま効く。システムプロパティによる上書き機構そのものは `nablarch-core` の担当 | 一致 | なし |
| :111 | テスティングフレームワークはシステム日時として固定値を返す機能を提供する | 対象 | 一致 | `FixedSystemTimeProvider.java:20`（`implements SystemTimeProvider`）・`:75-89` | 一致 | なし |
| :113-120 | `SystemTimeProvider` の実装を `nablarch.test.FixedSystemTimeProvider` に差し替え、`fixedDate` プロパティに固定したい日時を指定する | 対象 | 一致 | `FixedSystemTimeProvider.java:20,29,42`（`setFixedDate(String)`＝`fixedDate` プロパティ）。パッケージも `nablarch.test`（同 `:1`） | 一致 | なし |
| :122 | `fixedDate` は `yyyyMMddHHmmss`（14桁）または `yyyyMMddHHmmssSSS`（17桁）に合致する文字列 | 対象 | 一致 | `FixedSystemTimeProvider.java:23,26,42-53`（`SHORTEST_FORMAT.length()`=14／`LONGEST_FORMAT.length()`=17、それ以外は `IllegalArgumentException`）。実行して確認（下記 [H]）：14桁→`2010-09-14 12:34:56.0`、17桁→`2010-09-14 12:34:56.789`、13桁・15桁→`IllegalArgumentException`。なお Javadoc（同 `:35-36`）は「12桁」「15桁」と書いており誤っているが、解説書の記述は正しい | 一致 | なし |
| :126 | テスティングフレームワークは、シーケンスオブジェクトを使用した採番処理をコンポーネント設定ファイルの変更だけでテーブル採番に置き換える機能を提供する | 対象 | **不一致** | 下記「不一致の詳細 :126」参照 | **不一致（解説書側の誤りの疑い）** | 報告のみ（実装・テストとも変更しない） |
| :128-154 | `OracleSequenceIdGenerator`／`FastTableIdGenerator` の設定例（`idTable`・`tableName`・`idColumnName`・`noColumnName`・`dbTransactionManager`） | 対象外 | — | `FastTableIdGenerator` は `nablarch-common-idgenerator-jdbc` が提供（全ローカルjar走査で `nablarch-common-idgenerator-jdbc-6-NEXT-SNAPSHOT.jar` にのみ `FastTableIdGenerator.class` が存在）。`OracleSequenceIdGenerator` はプロジェクト側クラス（`com.example.common.idgenerator`）。いずれも `nablarch-testing` の実装で成否が決まらない | — | なし |
| :156-160 | `IdGenerator` の参照リンク・テストデータ記述例へのリンク | 対象外 | — | 解説書内の相互参照 | — | なし |
| :166 | 応答電文の読み込みにはベースディレクトリとテストデータ解析コンポーネントの設定が必要で、どちらもテスティングフレームワークのデフォルト設定には含まれない。設定していない場合はテストの実行時に例外が発生する | 対象 | 一致 | 既定のコンポーネント設定は本モジュールに同梱されていない（`src/main/resources` は `BatchSample.sql`・`template.xls` の2件のみ）。実行して確認（下記 [1][2]）：`sendSyncTestData` 未設定→`IllegalArgumentException: Unknown basePathName: sendSyncTestData`（`SendSyncSupport.java:346` → `FilePathSetting.java:143-151`）、`messagingTestDataParser` 未設定→`IllegalStateException: can't get TestDataParser. check configuration.`（`SendSyncSupport.java:473-476`） | 一致 | なし |
| :168 | ベースディレクトリはファイルパス管理の `sendSyncTestData` キー、フォーマット定義ファイルは `format` キー、解析コンポーネントは `messagingTestDataParser` という名前 | 対象 | 一致 | `SendSyncSupport.java:49`（`SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData"`）・`:346,348` で使用、`:473`（`SystemRepository.get("messagingTestDataParser")`）。`format` は `MockMessagingClient.java:166,196`・`RequestTestingMessagingClient.java:539`。リソース名は `<リクエストID>/message`（`SendSyncSupport.java:46-47,347`） | 一致 | なし |
| :170 | テストデータの記法を解釈するクラスは Excel 形式と YAML 形式で共通である | 対象 | 一致 | 解説書の両ブロック（`:220`・`:252`）がともに `messagingTestInterpreters` を参照しており、含まれる3クラスはいずれも本モジュールが提供する。`interpreters` プロパティは `TestDataParser.java:107` に定義され、`YamlTestDataParser` は `BasicTestDataParser` を継承している（`nablarch-testing-yaml` `05ada91`:`src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:43`）ので同じプロパティが使える | 一致 | なし |
| :174-185 | `messagingTestInterpreters` に `NullInterpreter`・`QuotationTrimmer`・`CompositeInterpreter`（配下に `BasicJapaneseCharacterInterpreter`）を指定する | 対象 | 一致 | 4クラスとも本モジュールが提供（`NullInterpreter.java:8`・`QuotationTrimmer.java:9`・`CompositeInterpreter.java:15,61`・`BasicJapaneseCharacterInterpreter.java:18`） | 一致 | なし |
| :176 と :252 の組み合わせ | YAML 形式の `messagingTestDataParser` にも `NullInterpreter` を含む `messagingTestInterpreters` を指定する | 対象 | **不一致** | 下記「不一致の詳細 :176/:252」参照 | **不一致（解説書側の誤りの疑い）** | 報告のみ（実装・テストとも変更しない） |
| :187 | ベースディレクトリの指定と解析コンポーネントの設定はテストデータの形式によって異なる | 対象 | 一致 | 実行して確認（下記 [D]）：`fileExtensions` に `sendSyncTestData` を設定するかどうかで `FilePathSetting#getFileIfExists` の解決先がファイル／ディレクトリに変わる | 一致 | なし |
| :191 | ベースディレクトリは `classpath:` ではなく `file:` で指定することを推奨する。ファイルシステムのパスならアプリケーションサーバ起動中にテストデータを編集してテストを続けられる | 対象 | 一致 | `SendSyncSupport.java:359,362-375`（呼び出しのたびにテストデータの最終更新日時スナップショットを採り、変化していれば再読み込みする）。`classpath:` を指定した場合はベースパスの URL が設定時に解決される（`FilePathSetting.java:229-267`）ため、編集対象がデプロイ済みのリソースになる | 一致 | なし |
| :195-212 | Excel 形式：`filePathSetting` に `basePathSettings`（`sendSyncTestData`・`format`）と `fileExtensions`（`sendSyncTestData=xlsx`・`format=fmt`）を設定する | 対象 | 一致 | `FilePathSetting.java:192,309`（`basePathSettings`／`fileExtensions` プロパティ）。キー名の出所は上記 :168。実行して確認（下記 [D-xlsx]）：`RB11AC0200.xlsx` が `getFileIfExists("sendSyncTestData","RB11AC0200")` で解決される | 一致 | なし |
| :214-223 | Excel 形式：`messagingTestDataParser` は `BasicTestDataParser`、`testDataReader` に `PoiXlsReader`、`interpreters` に前掲の `messagingTestInterpreters` | 対象 | 一致 | `BasicTestDataParser.java:32,216,230`、`PoiXlsReader.java:30`（`implements TestDataReader`）。`SendSyncSupport.java:473` は取得結果を `BasicTestDataParser` 型の変数へ代入するため、`BasicTestDataParser`（またはその派生）であることが必要 | 一致 | なし |
| :225 | `fileExtensions` の `sendSyncTestData` には実際に配置するファイルの拡張子（`xlsx` または `xls`）を指定する。指定した拡張子と一致しないファイルは読み込まれない。リクエストIDごとに1つのファイルを置く | 対象 | 一致 | `FilePathSetting.java:176-184`（拡張子が設定されていればファイル名に結合する）→ `:78-82`（`getFileIfExists`）→ `SendSyncSupport.java:348-353`（`null` なら `IllegalStateException`）。実行して確認（下記 [D-xls]）：実ファイルが `.xlsx` のとき `fileExtensions=xls` では `null` が返る。リクエストIDごとに1ファイルである点は `PoiXlsReader.java:55-65`（リソース名 `<リクエストID>/message` を分解して `<ベース>/<リクエストID>.xls(x)` のシート `message` を開く） | 一致 | なし |
| :227 | ベースディレクトリ配下の構成図（画像） | 対象外 | — | 解説書の画像アセット。図が表す構成（リクエストIDごとに1ファイル）の成否は :225 の行で判定済み | — | なし |
| :231-247 | YAML 形式：`filePathSetting` の `fileExtensions` には `format` だけを設定する | 対象 | 一致 | 実行して確認（下記 [D-noext]）：`sendSyncTestData` に拡張子を設定しない場合、`getFileIfExists("sendSyncTestData","RB11AC0100")` はディレクトリ `RB11AC0100` を返す（`FilePathSetting.java:176-184` で拡張子を結合しないため） | 一致 | なし |
| :249-255 | YAML 形式：`messagingTestDataParser` は `YamlTestDataParser`、`interpreters` は前掲と共通、`testDataReader` は指定しない | 一部対象外 | 一致（本モジュール側の受け口） | `SendSyncSupport.java:473` の代入先が `BasicTestDataParser` 型であるため、`YamlTestDataParser` が `BasicTestDataParser` を継承していることが必要。継承していることを実測（`nablarch-testing-yaml` `05ada91`:`src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:43`）。`testDataReader` を使わないことの確認は `nablarch-testing-yaml` 担当 | 一致 | なし |
| :257-259 | `fileExtensions` に `sendSyncTestData` を設定しない。設定するとテストデータが見つからず、テストの実行時に例外が発生する | 対象 | 一致 | 実行して確認（下記 [3]）：ディレクトリ `RB11AC0100` を置いたうえで `fileExtensions.sendSyncTestData=xlsx` を設定すると `IllegalStateException: test data file was not found. request id=[RB11AC0100], ...`（`SendSyncSupport.java:350-353`） | 一致 | なし |

対象外と判定した記述と理由:

- :33 の tip「専用のモジュールが `nablarch-testing` に依存する場合は個別に追加しなくてよい」— 他モジュールの `pom.xml` が決めることであり、`nablarch-testing` の実装で成否が決まらない
- :39 の「デフォルトでは Excel 形式で読み込まれる／設定は不要である」— `nablarch-testing` は既定のコンポーネント設定ファイルを同梱していないため、「デフォルト」の成否はブランクプロジェクト側の設定が決める（Excel を読むクラスの提供は対象として判定済み）
- :41 の「YAML 形式のテストデータを解析するクラスはこのモジュールが提供する」— `YamlTestDataParser` は `nablarch-testing-yaml` の指示書が担当する
- :43-50 の `nablarch-testing-yaml` の依存関係定義 — 同上
- :71-72 の `class="nablarch.test.core.reader.YamlTestDataParser"` — 同上（`dbInfo`・`interpreters` のプロパティ名は対象として判定済み）
- :77 の「null・空文字・ダブルクォート・改行文字は YAML のパーサが構文として解釈する」— YAML パーサの挙動であり `nablarch-testing-yaml` の指示書が担当する
- :83 の「`YamlTestDataParser` は YAML ファイルを直接読み込むためこの設定を使用しない」— 同上
- :85・:158・:160 のページ間リンク — 解説書内の相互参照であり、`nablarch-testing` の実装で成否が決まらない
- :107 の「システムプロパティを使って環境依存値を上書きする」機構そのもの — `nablarch-core` の `SystemRepository` が担当する（`nablarch-testing` 側は読み出すだけで、その点は対象として判定済み）
- :128-154 の採番設定例に出てくるクラス（`OracleSequenceIdGenerator`・`FastTableIdGenerator`）とそのプロパティ — 前者はプロジェクト側クラス、後者は `nablarch-common-idgenerator-jdbc` が提供するクラスであり、`nablarch-testing` の実装で成否が決まらない
- :227 の構成図（画像） — 解説書の画像アセット。図が表す構成の成否は :225 の行で判定済み
- :255 の「`testDataReader` は指定しない」— `YamlTestDataParser` の挙動であり `nablarch-testing-yaml` の指示書が担当する

##### 不一致の詳細 :89

1. **解説書の逐語**（`40b9c52`:`setup/common.rst:89`）
   「テストデータは、デフォルトでは ``src/test/java`` 配下から読み込まれる。プロジェクトのディレクトリ構成に合わせて読み込み先を変更する場合は、環境設定ファイルに ``nablarch.test.resource-root`` を設定する。値には、テスト実行時のカレントディレクトリからの相対パスを指定する。」
2. **実装での実測**（`3c4bd2a`）
   `TestSupport.java:30` — `private static final String DEFAULT_RESOURCE_ROOT = "test/java/";`
   `TestSupport.java:356-361` — `SystemRepository.get("nablarch.test.resource-root")` が `null` のとき `DEFAULT_RESOURCE_ROOT` を返す。
   実行結果（手順は下記「動かして確かめた内容」[A][B]）:

   ```
   [A] default resource root = [test/java/]
   [A] cwd = /home/tie303177/work/nablarch/nablarch-testing/.
   [A] exists(<cwd>/test/java) = false
   [A] exists(<cwd>/src/test/java) = true
   [B] default testDataPaths = [test/java//]
   ```

   すなわち `nablarch.test.resource-root` を設定しない場合、読み込み先は
   カレントディレクトリ配下の `test/java/` であって `src/test/java` ではない。
   本リポジトリ自身も `src/test/resources/unit-test.config:8` で
   `nablarch.test.resource-root=src/test/java` を明示的に設定している。
3. **どちらが誤っていると考えるか**: **解説書側の誤りの疑い**。
   解説書は「変更する場合は設定する」と書いており、設定しなければ `src/test/java` から
   読まれると読める。しかし `nablarch-testing` の実装既定値は `test/java/` である。
   ブランクプロジェクトの環境設定ファイルが `src/test/java` を設定している可能性はあるが、
   ブランクプロジェクトは本タスクの参照点に含まれないため**未確認**であり、
   解説書 `40b9c52` 内で `resource-root` に言及しているのは本ページだけ
   （`git grep -n "resource-root" 40b9c52 -- ja` の結果は `setup/common.rst:89,93,99,107` の4件のみ）。
   実装を `src/test/java` に変えると既存利用者の読み込み先が変わるため、実装側は変更できない。

##### 不一致の詳細 :126

1. **解説書の逐語**（`40b9c52`:`setup/common.rst:126`）
   「テスティングフレームワークは、シーケンスオブジェクトを使用した採番処理を、コンポーネント設定ファイルの変更だけでテーブル採番に置き換える機能を提供する。」
2. **実装での実測**（`3c4bd2a`）
   `git grep -rin "idgenerator" 3c4bd2a -- src/main` が0件。`git grep -rn "採番" 3c4bd2a -- src/main` も0件。
   解説書が置き換え先として挙げる `nablarch.common.idgenerator.FastTableIdGenerator`
   （`setup/common.rst:149`）は `nablarch-common-idgenerator-jdbc` が提供する
   （ローカル `~/.m2` の全 jar を走査し `FastTableIdGenerator.class` を含むのは
   `nablarch-common-idgenerator-jdbc-*.jar` のみ）。
3. **どちらが誤っていると考えるか**: **解説書側の誤りの疑い**（記述の帰属の問題であり、手順そのものは成立する）。
   置き換えの手順（コンポーネント設定の上書き）は `nablarch-core` のリポジトリ機能で成立し、
   置き換え先のクラスは `nablarch-common-idgenerator-jdbc` が提供する。
   `nablarch-testing` はこの機能に対して一切のコードを持たない。
   「テスティングフレームワークが提供する」という帰属だけが実装と合っていない。

##### 不一致の詳細 :176/:252

1. **解説書の逐語**
   `40b9c52`:`setup/common.rst:81` — 「``NullInterpreter``\ を指定してはならない。指定すると、文字列として記述した ``"null"``\ も\ Java\ の\ null\ になり、両者を区別できなくなる。」
   `40b9c52`:`setup/common.rst:176` — `<component class="nablarch.test.core.util.interpreter.NullInterpreter"/>`（`messagingTestInterpreters` の先頭要素）
   `40b9c52`:`setup/common.rst:250-252` — YAML 形式の `messagingTestDataParser`（`class="nablarch.test.core.reader.YamlTestDataParser"`）の `interpreters` に、その `messagingTestInterpreters` を指定している。
2. **実装での実測**（`3c4bd2a`）
   `NullInterpreter.java:14-19` — 値が大文字小文字を問わず `null` と等しければ Java の `null` を返す。
   実行結果（下記 [G]）:

   ```
   [G] NullInterpreter("null")  -> null
   [G] NullInterpreter("NULL")  -> null
   [G] Quotation+Null("\"null\"") -> null
   ```

   `QuotationTrimmer`（`:176-177` で `NullInterpreter` の直後に指定される）を通した場合も
   ダブルクォートが外れたうえで `null` になる。
3. **どちらが誤っていると考えるか**: **解説書側の誤りの疑い**。
   同じページの :81 が「YAML 形式では `NullInterpreter` を指定してはならない」と明記しているのに、
   :176 と :252 の組み合わせは YAML 形式の `messagingTestDataParser` に `NullInterpreter` を
   指定させている。:81 の理由（文字列 `"null"` と Java の `null` を区別できなくなる）は
   `NullInterpreter` 自体の挙動に由来するもので、上の実測のとおり
   `messagingTestDataParser` 経由でも同じく成り立つ。
   ただし YAML パーサが引用符付きの `"null"` をどう渡すかは `nablarch-testing-yaml` 担当のため
   **その先の影響は未確認**であり、本表では解説書内の記述同士が矛盾している事実までを記録する。

##### 動かして確かめた内容

`src/main`・`src/test` を変更しないため、検証コードはスクラップパッド配下に置いて実行した
（`/tmp/claude-1000/-home-tie303177-work-nablarch-nablarch-testing/92111fc8-7ec9-430c-96c2-1a8e7db53a6f/scratchpad/probe/`）。
クラスパスは `mvn -o dependency:build-classpath` の出力＋`target/classes`（`mvn -o compile` でビルド）。

`[A][B][D][E][G][H]`（`Probe.java`）:

```
[A] default resource root = [test/java/]
[A] cwd = /home/tie303177/work/nablarch/nablarch-testing/.
[A] exists(<cwd>/test/java) = false
[A] exists(<cwd>/src/test/java) = true
[B] default testDataPaths = [test/java//]
[D-noext] getFileIfExists(dir RB11AC0100) = <scratchpad>/msgbase/RB11AC0100
[D-noext] getFileIfExists(file RB11AC0200) = null
[D-xlsx] getFileIfExists(dir RB11AC0100) = null
[D-xlsx] getFileIfExists(file RB11AC0200) = <scratchpad>/msgbase/RB11AC0200.xlsx
[D-xls]  getFileIfExists(file RB11AC0200) = null
[E] java.lang.IllegalArgumentException: Unknown basePathName: sendSyncTestData
[F-format] java.lang.IllegalArgumentException: Unknown basePathName: format
[G] NullInterpreter("null")  -> null
[G] NullInterpreter("NULL")  -> null
[G] Quotation+Null("\"null\"") -> null
[H] 14digits -> 2010-09-14 12:34:56.0
[H] 17digits -> 2010-09-14 12:34:56.789
[H] 13digits -> java.lang.IllegalArgumentException: datetime string 2010091412345
[H] 15digits -> java.lang.IllegalArgumentException: datetime string 201009141234561
```

`[1][2][3]`（`Probe2.java`。`SendSyncSupport#getResponseMessageByRequestId` を実際に呼んだ結果）:

```
[1] sendSyncTestData未設定 -> java.lang.IllegalArgumentException: Unknown basePathName: sendSyncTestData
[2] messagingTestDataParser未設定 -> java.lang.IllegalStateException: can't get TestDataParser. check configuration.
[3] YAML(dir)にxlsx指定 -> java.lang.IllegalStateException: test data file was not found. request id=[RB11AC0100], base path=[<scratchpad>/msgbase], resource name=[RB11AC0100/message], absolute base path=[<scratchpad>/msgbase].
```

`[T]`（既存テストの実行。`;` 区切りと「最初に見つかったものが読み込まれる」の確認）:

```
$ LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o test -Dtest=TestSupportTest -DfailIfNoTests=false
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 - in nablarch.test.TestSupportTest
[INFO] BUILD SUCCESS
```

集計: 対象 29件（うち一部対象外を含む行3件: :39・:77・:249-255）／一致 26件／不一致（解説書が正）0件／不一致（解説書側の誤りの疑い）3件（:89・:126・:176/:252）。対象外 7行（:33・:41・:83・:85・:128-154・:156-160・:227）。
