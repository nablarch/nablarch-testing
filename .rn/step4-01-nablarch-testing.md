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
