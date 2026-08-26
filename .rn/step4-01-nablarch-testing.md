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
| :1 | 参照ラベル `_testing_framework_setup:` | 対象外 | — | 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :3-4 | 見出し「テスティングフレームワークの導入と設定」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :6-21 | `toctree`（`common` 以下13エントリ） | 対象外 | — | 13エントリすべて `40b9c52` に実在（`git cat-file -e 40b9c52:ja/development_tools/testing_framework/setup/<name>.rst` が13件とも成功） | — | なし |

対象外と判定した記述と理由:

- :1 の参照ラベル — 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない
- :3-4 の見出し — 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない
- :6-21 の `toctree` — 解説書のページ構成であり、`nablarch-testing` の実装で成否が決まらない（参照先の実在は上表のとおり確認済み）
- 空行 :2・:5 — 記述がない

##### 行の網羅の検算

表の「行」欄と、対象外リストの「空行」の行番号を機械的に集め、
対象ページの 1..21 を隙間なく・重複なく覆っているかを検算する。
検算スクリプトは `<scratchpad>/fix1/verify_lines.py`（表の1つ目のセルが `（` で始まる横断行は分割の対象外として無視する）。

```
$ python3 <scratchpad>/fix1/verify_lines.py .rn/step4-01-nablarch-testing.md 'setup/index.rst' 21
section        : setup/index.rst
総行数         : 21
拾ったレンジ数 : 5（表 3 / 対象外リスト(空行) 2）
覆った行数     : 21
欠け           : なし
重複           : なし
範囲外         : なし
判定           : OK — 1..21 を隙間なく1回ずつ覆っている
$ echo $?
0
```


集計: 対象 0件／一致 0件／不一致（解説書が正）0件／不一致（解説書側の誤りの疑い）0件／判定保留 0件。対象外 3行＋空行2行。
本ページは `toctree` のみで構成されており、実装で成否が決まる記述は1件もない。

#### `setup/common.rst`（259行）— 全行通読

##### 保留中の論点 — デフォルト設定モジュールの扱い

`:39`・`:89`・`:166` の3行は、「テスティングフレームワークのデフォルト設定」が何を指すかによって判定が変わる。
§2 のピンは `nablarch-document`（`40b9c52`）と `nablarch-testing`（`3c4bd2a`）の2つだけで、
デフォルト設定モジュールはピンに入っていない。ピンをどう扱うかは user が決めるため、この3行は**判定保留**にする。

実物で確認した事実は次のとおり。

1. 解説書自身が「テスティングフレームワークのデフォルト設定」として
   `com.nablarch.configuration:nablarch-testing-default-configuration` を名指ししている。

   ```
   $ git show 40b9c52:ja/development_tools/testing_framework/setup/request_unit_test/rest.rst | sed -n '30,34p'
     <!-- テスティングフレームワークのデフォルト設定 -->
     <dependency>
       <groupId>com.nablarch.configuration</groupId>
       <artifactId>nablarch-testing-default-configuration</artifactId>
       <scope>test</scope>
   ```

2. そのモジュールが `nablarch.test.resource-root=src/test/java` を持つ。

   ```
   $ unzip -p ~/.m2/repository/com/nablarch/configuration/nablarch-testing-default-configuration/6-NEXT-SNAPSHOT/nablarch-testing-default-configuration-6-NEXT-20260327.002359-3.jar nablarch/test/test-data.config
   nablarch.test.resource-root=src/test/java
   ```

3. 同じモジュールが `testDataParser` を `BasicTestDataParser` ＋ `PoiXlsReader` として定義している。

   ```
   $ unzip -p <上と同じjar> nablarch/test/test-data.xml
   ...
     <!-- TestDataParser -->
     <component name="testDataParser" class="nablarch.test.core.reader.BasicTestDataParser">
       <property name="testDataReader">
         <component name="xlsReaderForPoi" class="nablarch.test.core.reader.PoiXlsReader"/>
       </property>
       <property name="dbInfo" ref="dbInfo"/>
       <property name="interpreters" ref="interpreters"/>
     </component>
   ```

   `interpreters` の中身は同モジュールの `nablarch/test/test-data-interpreter.xml` で、
   `NullInterpreter`・`QuotationTrimmer`・`DateTimeInterpreter`・`LineSeparatorInterpreter`・`CompositeInterpreter` の5件である。

4. 一方、同じモジュールに `sendSyncTestData`・`messagingTestDataParser`・採番関連は存在しない。

   ```
   $ unzip -q -o <上と同じjar> -d defcfg && cd defcfg
   $ grep -rn 'sendSyncTestData' . | wc -l
   0
   $ grep -rn 'messagingTestDataParser' . | wc -l
   0
   $ grep -rni 'idgenerator' . | wc -l
   0
   $ grep -rn '採番' . | wc -l
   0
   ```

これらから言えること:

- `:89`「デフォルトでは `src/test/java` 配下から読み込まれる」は**解説書側の誤りではない**。
  `TestSupport.java:30` の `test/java/` は、デフォルト設定を読み込まない場合のフォールバックである
  （前回の「不一致（解説書側の誤りの疑い）」は取り下げた。下記「取り下げた不一致候補 :89」）
- `:39`「Excel 形式で記述する場合、設定は不要である」を決めているのは、
  前回の根拠に書いた「ブランクプロジェクト側の設定」ではなく、このデフォルト設定モジュールである
- `:166` の根拠として前回書いた「本モジュールに同梱されていない（`src/main/resources` は2件）」は不十分である。
  正しい根拠は、デフォルト設定モジュール側にも `sendSyncTestData`／`messagingTestDataParser` が無いこと（上記4）

**保留の理由**: この3行を「一致」で確定させることは、`nablarch-testing-default-configuration` を
突合の参照点として認めることを意味する。§2 のピンに加えるかどうかは user の判断であり、こちらでは確定させない。

| 行 | 記述の要旨 | 対象／対象外 | 実装での成否 | 根拠 | 判定 | 処置 |
|---|---|---|---|---|---|---|
| :1 | 参照ラベル `_testing_framework_common:` | 対象外 | — | 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :3-4 | 見出し「共通設定」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :6-8 | `.. contents:: 目次`（`:depth: 3`・`:local:`） | 対象外 | — | Sphinx のディレクティブであり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :10-11 | 見出し「機能概要」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :13 | 共通設定で扱うのは、依存関係の追加・読み込み先の変更・システム日時の固定・シーケンス採番のテーブル採番への置き換え・同期応答／HTTPメッセージ送信のテストデータ読み込み | 対象 | 一致（採番置換の帰属は :126 の行で判定） | 依存関係=`pom.xml:7-9`、読み込み先=`TestSupport.java:30,33,356-361`、日時固定=`FixedSystemTimeProvider.java:20,42-55`、同期応答=`SendSyncSupport.java:49,346,473`。採番置換だけは本モジュールに実装がない | 一致 | なし |
| :15-16 | 見出し「使用方法」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :18-19 | 見出し「テスティングフレームワークを依存関係に追加する」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :20-29 | テスティングフレームワークは `nablarch-testing` として提供される。テストでのみ使用するため `test` スコープで依存関係に追加する | 一部対象外 | 一致（座標と「テストでのみ使用する」） | 座標は `pom.xml:7-8`（`groupId=com.nablarch.framework`・`artifactId=nablarch-testing`）。「テストでのみ使用する」は本モジュールの提供クラスの内訳で裏づけられる：`git ls-tree -r 3c4bd2a --name-only -- src/main/java` は187件で、うち179件が `nablarch/test/` 配下。残る8件も `nablarch/fw/web` のモック（`MockHttpRequest.java`・`MockHttpCookie.java`・`MockServletExecutionContext.java`・`TestServletContextCreator.java`・`package-info.java` 2件）と、単体テスト用の内蔵サーバ（`HttpServer.java:41` 「主に単体テスト時の画面確認や打鍵テストで使用することを想定した…軽量アプリケーションサーバ」・`HttpServerFactory.java`）であり、本番コードから使う API を提供していない。`<scope>test</scope>` と書くこと自体は利用側プロジェクトの `pom.xml` が決めるため対象外 | 一致 | なし |
| :31 | `.. tip::` のディレクティブ宣言行 | 対象外 | — | ディレクティブの宣言行であり、本文の成否は :33 の行で判定する | — | なし |
| :33 | 処理方式によっては専用モジュールを使う。専用モジュールが `nablarch-testing` に依存するなら個別追加は不要 | 対象外 | — | 他モジュールの `pom.xml` が決める | — | なし |
| :35 | 参照ラベル `_testing_framework_common-yaml_testdata:` | 対象外 | — | 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :37-38 | 見出し「テストデータの形式をYAMLに変更する」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :39 | テストデータはデフォルトでは Excel 形式で読み込まれる。Excel 形式で記述する場合、設定は不要である | 一部対象外 | 一致（Excel を読むクラスの提供）／「デフォルト」の成否は保留 | Excel を読むクラスは本モジュールが提供する（`PoiXlsReader.java:30`）。「デフォルト」を決めているのは `nablarch-testing-default-configuration` の `nablarch/test/test-data.xml`（`testDataParser` = `BasicTestDataParser` ＋ `PoiXlsReader`）と `nablarch/test/test-data-interpreter.xml`。前回書いた「ブランクプロジェクト側の設定が決める」は誤りだった。上記「保留中の論点」参照 | **判定保留（ピンの扱いについて user 判断待ち）** | 報告のみ。§2 のピンに `nablarch-testing-default-configuration` を加えるかどうかの user 判断待ち |
| :41 | YAML 形式のテストデータを解析するクラスは `nablarch-testing-yaml` が提供する | 対象外 | — | 本モジュールに `Yaml*` クラスは存在しない（`git ls-tree -r 3c4bd2a --name-only -- src/main/java` を `grep -i yaml` して0件）。提供側の確認は `nablarch-testing-yaml` 担当 | — | なし |
| :43 | `.. code-block:: xml` の宣言行 | 対象外 | — | ディレクティブの宣言行であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :45-50 | `nablarch-testing-yaml` の dependency 定義（groupId・artifactId・scope） | 対象外 | — | 他モジュールの座標であり、`nablarch-testing-yaml` の指示書が担当する | — | なし |
| :52 | テストデータを解析するコンポーネントの名前は `testDataParser` | 対象 | 一致 | `TestSupport.java:404`・`MasterDataSetUpper.java:188` がともに `SystemRepository.get("testDataParser")` | 一致 | なし |
| :54 | `.. code-block:: xml` の宣言行 | 対象外 | — | ディレクティブの宣言行であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :56 | コード例中のXMLコメント `<!-- YAML形式のテストデータ記法の解釈を行うクラス群 -->` | 対象外 | — | コメント文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :57-68 | `yamlInterpreters` に `DateTimeInterpreter`（`systemTimeProvider` プロパティ）・`CompositeInterpreter`（`interpreters` プロパティ）・`BasicJapaneseCharacterInterpreter` を指定する | 対象 | 一致 | 3クラスとも本モジュールが提供（`git ls-tree -r 3c4bd2a --name-only -- src/main/java` を `grep interpreter`）。プロパティは `DateTimeInterpreter.java:70`（`setSystemTimeProvider`）・`CompositeInterpreter.java:61`（`setInterpreters(List<TestDataInterpreter>)`）・`BasicJapaneseCharacterInterpreter.java:18`。`:59` の `ref="systemTimeProvider"` が指すコンポーネント名は `nablarch-core` の `SystemTimeUtil.java:26,109` が引く固定名であり、名前を変えると解決できない（実行して確認 [N]） | 一致 | なし |
| :70 | コード例中のXMLコメント `<!-- テストデータを解析するコンポーネント -->` | 対象外 | — | コメント文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :71-75 | テストデータ解析コンポーネントは `dbInfo`・`interpreters` プロパティを持つ | 一部対象外 | 一致 | プロパティ名の定義は `TestDataParser.java:100`（`setDbInfo`）・`:107`（`setInterpreters`）、実装は `BasicTestDataParser.java:221,230`。ただし判定対象はここでは `YamlTestDataParser` に付くプロパティなので、子がセッターをオーバーライドして `super` を呼ぶことまで確かめた：`nablarch-testing-yaml` `05ada91`:`YamlTestDataParser.java:78-82`（`setDbInfo` が `super.setDbInfo` を呼ぶ）・`:86-90`（`setInterpreters` が `super.setInterpreters` を呼ぶ）。子は独自フィールド `:47`・`:49` を持ち `rebuildBuilders()`（同 `:188-192`）で自分のビルダにも反映する。`:73` の `ref="dbInfo"` が指すコンポーネントの定義はプロジェクト側またはデフォルト設定モジュール側であり対象外 | 一致 | なし |
| :77 | Excel 形式で必要な `NullInterpreter`・`QuotationTrimmer`・`LineSeparatorInterpreter` は YAML 形式では指定しない | 一部対象外 | 一致（3クラスの所属と Excel 側での使用） | 3クラスとも本モジュールが提供（`NullInterpreter.java:8`・`QuotationTrimmer.java:9`・`LineSeparatorInterpreter.java:28`）。Excel 形式の設定例で実際に使われている（`src/test/resources/unit-test.xml:30,33,40`）。「YAML のパーサが構文として解釈するから不要」の部分は `nablarch-testing-yaml` 担当 | 一致 | なし |
| :79 | `.. important::` のディレクティブ宣言行 | 対象外 | — | ディレクティブの宣言行であり、本文の成否は :81 の行で判定する | — | なし |
| :81 | `NullInterpreter` を指定すると、文字列として書いた `"null"` も Java の `null` になり区別できなくなる | 一部対象外 | 一致（`NullInterpreter` の挙動） | `NullInterpreter.java:15-16`（`equalsIgnoreCase("null")` なら Java の `null` を返す）。実行して確認（[I-3]）：`NullInterpreter` 単体に長さ4の `null` を渡すと `<<JAVA null>>`、引用符付きの `"null"`（長さ6）を渡すと長さ6の文字列のまま返る。すなわち Java の `null` になるのは解釈クラスに長さ4の `null` が渡ったときであり、YAML で `"null"` と書いた値がパーサからその形で渡るかどうかは `nablarch-testing-yaml` 担当。前回この行の根拠に置いていた `[G] Quotation+Null("\"null\"") -> null` は偽の根拠だったため取り下げた（下記「取り下げた不一致候補 :176/:252」） | 一致 | なし |
| :83 | `testDataReader` は指定しない（`YamlTestDataParser` は YAML ファイルを直接読むため使用しない） | 対象外 | — | `testDataReader` というプロパティ名の出所は本モジュール（`TestDataParser.java:92`）だが、`YamlTestDataParser` が使わないことの確認は `nablarch-testing-yaml` 担当 | — | なし |
| :85 | テストデータの記法は別ページを参照 | 対象外 | — | 解説書内の相互参照 | — | なし |
| :87-88 | 見出し「テストデータの読み込み先を変更する」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :89 | テストデータはデフォルトでは `src/test/java` 配下から読み込まれる。変更する場合に `nablarch.test.resource-root` を設定する。値はカレントディレクトリからの相対パス | 対象 | 判定保留 | 前回の「不一致（解説書側の誤りの疑い）」は取り下げた。下記「取り下げた不一致候補 :89」および上記「保留中の論点」参照 | **判定保留（ピンの扱いについて user 判断待ち）** | 報告のみ。§2 のピンに `nablarch-testing-default-configuration` を加えるかどうかの user 判断待ち |
| :91 | `.. code-block:: properties` の宣言行 | 対象外 | — | ディレクティブの宣言行であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :93 | 設定例 `nablarch.test.resource-root=path/to/test-data-dir` | 対象 | 一致 | キー名の出所は `TestSupport.java:33`（`RESOURCE_ROOT_KEY = "nablarch.test.resource-root"`）で、`:356-361` がこの名前で読み出す。実行して確認（[P]）：この名前で環境設定ファイルとシステムプロパティの値が読み出される。値がカレントディレクトリからの相対パスとして解決されることは [F-1][F-2] のとおり（`dupA/nablarch/test/dupprobe` がカレントディレクトリ配下として解決された） | 一致 | なし |
| :95-99 | 読み込み先はセミコロン（`;`）区切りで複数指定できる | 対象 | 一致 | `TestSupport.java:42`（`PATH_SEPARATOR = ";"`）・`:329`（`split(PATH_SEPARATOR)`）・`:339-346`。既存テスト `TestSupportTest.java:566-575`（`testGetPathOf`）と `:594-604`（`testGetTestDataPaths`）は緑だが、緑だけを根拠にはしない。`PATH_SEPARATOR` を `","` に変異させた実装で同テストクラスを実行すると `Tests run: 24, Failures: 2` に落ち、落ちるのがこの2件であることを実測した（[M-B]）。すなわちこの2件は `;` 区切りを検知している | 一致 | なし |
| :101 | `.. important::` のディレクティブ宣言行 | 対象外 | — | ディレクティブの宣言行であり、本文の成否は :103 の行で判定する | — | なし |
| :103 | 同名のテストデータが複数のディレクトリに存在する場合、最初に見つかったものが読み込まれる | 対象 | 一致 | 実装は `TestSupport.java:308-315`（候補パスを先頭から走査し、最初にヒットしたものを返す）。**重複ケースを自分で作って実測した**（[F]）：同名の `DupProbe.xls` を `dupA/nablarch/test/dupprobe/` と `dupB/nablarch/test/dupprobe/` の両方に実在させ、`nablarch.test.resource-root=dupA;dupB` で `TestSupport#getListMap` を呼ぶと `dupA` 側の値が返る。`dupB;dupA` に入れ替えると `dupB` 側の値が返る（順序が効いている）。`getPathResourceExisting` を last-match-wins に変異させると、この実測は `dupB` を返して落ちる（[F-mut]）。**既存テスト `TestSupportTest.java:606-613` の緑は根拠にしない** — 同じ変異を入れても24件全緑であることを実測した（[M-A]）。候補のうち1件しかヒットしないため first/last を区別していない | 一致 | なし |
| :105 | `.. tip::` のディレクティブ宣言行 | 対象外 | — | ディレクティブの宣言行であり、本文の成否は :107 の行で判定する | — | なし |
| :107 | 一時的な変更は `-Dnablarch.test.resource-root=...` をシステムプロパティで指定してもよい | 対象 | 一致（条件つき） | 読むだけで済ませず動かして確かめた（[P]）。`target/test-classes` をクラスパスに載せてリポジトリ初期化が成功する構成では、`-Dnablarch.test.resource-root=OVERRIDDEN/BY/SYSPROP` が環境設定ファイル `src/test/resources/unit-test.config:8`（`nablarch.test.resource-root=src/test/java`）を上書きし、`TestSupport.getResourceRootSetting()` が `OVERRIDDEN/BY/SYSPROP` を返す。`target/test-classes` を外すと `unit-test.xml` が参照するテスト用クラスを解決できずリポジトリ初期化が失敗し、その例外は `TestEventDispatcher.java:39-41,68-74` が握りつぶすため `SystemRepository` は空のままになり、`-D` を付けても実装既定値 `test/java/`（`TestSupport.java:30`）が返る。この条件差を記録しておく。上書き機構そのものは `nablarch-core` の担当 | 一致 | なし |
| :109-110 | 見出し「システム日時を固定する」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :111 | テスティングフレームワークはシステム日時として固定値を返す機能を提供する | 対象 | 一致 | `FixedSystemTimeProvider.java:20`（`implements SystemTimeProvider`）・`:75-89` | 一致 | なし |
| :113-120 | `SystemTimeProvider` の実装を `nablarch.test.FixedSystemTimeProvider` に差し替え、`fixedDate` プロパティに固定したい日時を指定する | 対象 | 一致 | `FixedSystemTimeProvider.java:20,29,42`（`setFixedDate(String)` = `fixedDate` プロパティ）。パッケージも `nablarch.test`（同 `:1`）。`:117` の `name="systemTimeProvider"` はコンポーネント名で、`nablarch-core` の `SystemTimeUtil.java:26,109` がこの固定名で引くため名前を変えると解決できない（実行して確認 [N]） | 一致 | なし |
| :122 | `fixedDate` は `yyyyMMddHHmmss`（14桁）または `yyyyMMddHHmmssSSS`（17桁）に合致する文字列。この設定を行うとアプリケーションが `SystemTimeProvider` を通じて取得する日時が固定される | 対象 | 一致 | 実装が検証しているのは**桁数（14／17）だけ**である。`FixedSystemTimeProvider.java:42-53` は長さが 17 でも 14 でもなければ `IllegalArgumentException` を投げるが、日付としての妥当性は見ておらず、`:62-64` の `new SimpleDateFormat(format).parse(...)` は lenient のままである。実行して確認（[T]）：`isLenient()` は `true`、`"20101332123456"` は例外にならず `2011-02-01 12:34:56.000`、`"20100914993456"` は `2010-09-18 03:34:56.000`、`"99999999999999"` は `10007-06-11 04:40:39.000` になる。13桁・15桁は `IllegalArgumentException`。第2文が成り立つ前提はコンポーネント名が `systemTimeProvider` であることで、`nablarch-core` の `SystemTimeUtil.java:26,109` がその名前で引く（実行して確認 [N]：名前を `fixedSystemTimeProvider` にすると `IllegalArgumentException: specified systemTimeProvider is not registered in SystemRepository.`）。なお同ファイルの Javadoc `:35-36` は「12桁」「15桁」と書いており実装とも解説書とも食い違う（`src/main` のため変更しない。事実として記録するだけ） | 一致 | なし |
| :124-125 | 見出し「シーケンス採番をテーブル採番に置き換える」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :126 | テスティングフレームワークは、シーケンスオブジェクトを使用した採番処理をコンポーネント設定ファイルの変更だけでテーブル採番に置き換える機能を提供する | 対象 | **不一致** | 下記「不一致の詳細 :126」参照 | **不一致（解説書側の誤りの疑い）** | 報告のみ（実装・テストとも変更しない） |
| :128-154 | `OracleSequenceIdGenerator`／`FastTableIdGenerator` の設定例（`idTable`・`tableName`・`idColumnName`・`noColumnName`・`dbTransactionManager`） | 対象外 | — | `FastTableIdGenerator` は `nablarch-common-idgenerator-jdbc` が提供（ローカル `~/.m2` の全 jar を走査して `FastTableIdGenerator.class` を含むのはこの artifactId の jar のみ）。`OracleSequenceIdGenerator` はプロジェクト側クラス（`com.example.common.idgenerator`）。`:153` の `ref="dbTransactionManager"` が指すコンポーネントもプロジェクト側の定義。いずれも `nablarch-testing` の実装で成否が決まらない | — | なし |
| :156-160 | `IdGenerator` の参照リンク・テストデータ記述例へのリンク | 対象外 | — | 解説書内の相互参照 | — | なし |
| :162 | 参照ラベル `_testing_framework_common-send_sync_test_data:` | 対象外 | — | 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :164-165 | 見出し「同期応答メッセージ送信・HTTPメッセージ送信のテストデータの読み込みを設定する」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :166 | 応答電文の読み込みにはベースディレクトリとテストデータ解析コンポーネントの設定が必要で、どちらもテスティングフレームワークのデフォルト設定には含まれない。設定していない場合はテストの実行時に例外が発生する | 対象 | 判定保留（例外が出ることは一致） | 設定していない場合に例外が出ることは実行して確認済み（[1][2]）：`sendSyncTestData` 未設定→`IllegalArgumentException: Unknown basePathName: sendSyncTestData`（`SendSyncSupport.java:346` → `nablarch-core` `FilePathSetting.java:143-151`）、`messagingTestDataParser` 未設定→`IllegalStateException: can't get TestDataParser. check configuration.`（`SendSyncSupport.java:473-476`）。「デフォルト設定に含まれない」の根拠は、前回書いた「本モジュールの `src/main/resources` が2件」ではなく、デフォルト設定モジュール側にも `sendSyncTestData`／`messagingTestDataParser` が無いこと（上記「保留中の論点」の4）。デフォルト設定モジュールをピンに含めるかが未決のため保留 | **判定保留（ピンの扱いについて user 判断待ち）** | 報告のみ。§2 のピンに `nablarch-testing-default-configuration` を加えるかどうかの user 判断待ち |
| :168 | ベースディレクトリはファイルパス管理の `sendSyncTestData` キー、フォーマット定義ファイルは `format` キー、解析コンポーネントは `messagingTestDataParser` という名前 | 対象 | 一致 | `SendSyncSupport.java:49`（`SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData"`）・`:346,348` で使用、`:473`（`SystemRepository.get("messagingTestDataParser")`）。`format` は `MockMessagingClient.java:166,196`・`RequestTestingMessagingClient.java:539`。リソース名は `<リクエストID>/message`（`SendSyncSupport.java:46-47,347`） | 一致 | なし |
| :170 | テストデータの記法を解釈するクラスは Excel 形式と YAML 形式で共通である | 対象 | 一致 | 実装側の経路で確かめた（前回の「解説書の両ブロックがともに `messagingTestInterpreters` を参照している」は解説書の主張を解説書で裏づける循環根拠だったので取り下げた）。`interpreters` プロパティは `TestDataParser.java:107` に定義される。Excel 側の `BasicTestDataParser` は `getMessageWithoutCache`（`:99-101`）で `addBinaryFileInterpreter(path)`（`:205-212`。`setInterpreters`（`:230`）で受けたリストの先頭に `BinaryFileInterpreter` を足したもの）を `SendSyncMessageParser` に渡す。YAML 側の `YamlTestDataParser` は `setInterpreters` をオーバーライドして `super` にも渡し（`05ada91`:`YamlTestDataParser.java:86-90`）、`rebuildBuilders()`（同 `:188-192`）で同じリストから `InterpreterResolver` を組み、`getMessageWithoutCache` のオーバーライド（同 `:162-166`）でそれを使う。Excel 側で実際に適用されることは実行して確認（[R]）：セルに引用符付きの `"QUOTED"` を置くと、`messagingTestInterpreters` を与えた場合は `QUOTED`、空リストの場合は `"QUOTED"` が返る | 一致 | なし |
| :172 | `.. code-block:: xml` の宣言行 | 対象外 | — | ディレクティブの宣言行であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :174-185 | `messagingTestInterpreters` に `NullInterpreter`・`QuotationTrimmer`・`CompositeInterpreter`（配下に `BasicJapaneseCharacterInterpreter`）を指定する | 対象 | 一致 | 4クラスとも本モジュールが提供（`NullInterpreter.java:8`・`QuotationTrimmer.java:9`・`CompositeInterpreter.java:15,61`・`BasicJapaneseCharacterInterpreter.java:18`） | 一致 | なし |
| （:176 と :252 の組み合わせ） | YAML 形式の `messagingTestDataParser` にも `NullInterpreter` を含む `messagingTestInterpreters` を指定する | 対象 | 一致 | 前回の不一致判定は取り下げた。`InterpretationContext.java:81-93` の `invokeNext` は FIFO なので、解説書の順序では `NullInterpreter` が先に引用符付きの `"null"`（長さ6）を見て不一致となり、その後 `QuotationTrimmer` が引用符を外す。結果は Java の `null` ではなく長さ4の文字列 `null` である。下記「取り下げた不一致候補 :176/:252」参照。この行の行番号（:176・:252）は `:174-185`・`:249-255` の行に含まれるため、行の網羅の検算では二重に数えない | 一致 | なし |
| :187 | ベースディレクトリの指定と解析コンポーネントの設定はテストデータの形式によって異なる | 対象 | 一致 | 実行して確認（[D]）：`fileExtensions` に `sendSyncTestData` を設定するかどうかで `FilePathSetting#getFileIfExists` の解決先がファイル／ディレクトリに変わる | 一致 | なし |
| :189 | `.. tip::` のディレクティブ宣言行 | 対象外 | — | ディレクティブの宣言行であり、本文の成否は :191 の行で判定する | — | なし |
| :191 | ベースディレクトリは `classpath:` ではなく `file:` で指定することを推奨する。ファイルシステムのパスならアプリケーションサーバ起動中にテストデータを編集してテストを続けられる | 対象 | 一致 | 前回の根拠（「`classpath:` は設定時に URL 解決される」）は主張と無関係だったので取り下げた。`nablarch-core` の `FilePathSetting.java:229,231` の `FileUtil.getResourceURL(path)` はスキームに関係なく無条件に呼ばれ、`file:` でも設定時に解決されるからである。実際の鍵は2つ：(1) `SendSyncSupport.java:359,362-375` が呼び出しのたびに最終更新日時スナップショットを採り、変化していれば再読み込みする。(2) `BasicTestDataParser#getMessageWithoutCache`（`:99-101`）が `agent.parse(..., false)` を渡すため、`TestDataParsingTemplate.java:131,136-141` が `PoiXlsReader#setUseCache(false)` を呼び、`PoiXlsReader.java:45,68-72` の `useCache` 経路（同 `:159` の static `bookCache`）がバイパスされる。実行して確認（[C]）：`file:` 指定のベースディレクトリで1回読んだあと、同じ JVM のままテストデータを書き換えて読み直すと `BEFORE_EDIT` から `AFTER_EDIT` に変わる。`PoiXlsReader` 単体で比べると `useCache=true` は書き換え後も古いブック（`V1`）を返し、`useCache=false` は `V2` を返す | 一致 | なし |
| :193-194 | 見出し「Excel形式の場合」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :195-212 | Excel 形式：`filePathSetting` に `basePathSettings`（`sendSyncTestData`・`format`）と `fileExtensions`（`sendSyncTestData=xlsx`・`format=fmt`）を設定する | 一部対象外 | 一致 | プロパティは `nablarch-core` の `FilePathSetting.java:192-196`（`setBasePathSettings`）・`:309-311`（`setFileExtensions`）。キー名の出所は上記 :168。`:198` の `name="filePathSetting"` は `FilePathSetting.getInstance()` が引く固定キー（同 `:30`・`:43-49`）で、名前を変えると `SendSyncSupport.java:345` が別インスタンス（`DEFAULT_SETTING`）を掴んで解決できない。`:199` の `autowireType="None"` は `nablarch-core` の DI が解釈する属性であり `nablarch-testing` の実装で成否が決まらない（対象外）。実行して確認（[D-xlsx]）：`RB11AC0200.xlsx` が `getFileIfExists("sendSyncTestData","RB11AC0200")` で解決される | 一致 | なし |
| :214-223 | Excel 形式：`messagingTestDataParser` は `BasicTestDataParser`、`testDataReader` に `PoiXlsReader`、`interpreters` に前掲の `messagingTestInterpreters` | 対象 | 一致 | `BasicTestDataParser.java:32,216,230`、`PoiXlsReader.java:30`（`implements TestDataReader`）。`SendSyncSupport.java:473` は取得結果を `BasicTestDataParser` 型の変数へ代入するため、`BasicTestDataParser`（またはその派生）であることが必要 | 一致 | なし |
| :225 | `fileExtensions` の `sendSyncTestData` には実際に配置するファイルの拡張子（`xlsx` または `xls`）を指定する。**指定した拡張子と一致しないファイルは読み込まれない**。リクエストIDごとに1つのファイルを置く | 対象 | **不一致** | 下記「不一致の詳細 :225」参照 | **不一致（解説書が正＝実装側の誤りの疑い）** | 報告のみ（実装・テストとも変更しない） |
| :227 | ベースディレクトリ配下の構成図（画像） | 対象外 | — | 解説書の画像アセット。図が表す構成（リクエストIDごとに1ファイル）の成否は :225 の行で判定済み | — | なし |
| :229-230 | 見出し「YAML形式の場合」 | 対象外 | — | 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない | — | なし |
| :231-247 | YAML 形式：`filePathSetting` の `fileExtensions` には `format` だけを設定する | 一部対象外 | 一致 | 実行して確認（[D-noext]）：`sendSyncTestData` に拡張子を設定しない場合、`getFileIfExists("sendSyncTestData","RB11AC0100")` はディレクトリ `RB11AC0100` を返す（`nablarch-core` `FilePathSetting.java:176-184` で拡張子を結合しないため）。`:234` の `name="filePathSetting"`・`:235` の `autowireType="None"` は :198・:199 と同じ扱い | 一致 | なし |
| :249-255 | YAML 形式：`messagingTestDataParser` は `YamlTestDataParser`、`interpreters` は前掲と共通、`testDataReader` は指定しない | 一部対象外 | 一致（本モジュール側の受け口） | `SendSyncSupport.java:473` の代入先が `BasicTestDataParser` 型であるため、`YamlTestDataParser` が `BasicTestDataParser` を継承していることが必要。継承していることを実測（`nablarch-testing-yaml` `05ada91`:`YamlTestDataParser.java:43`）。`testDataReader` を使わないことの確認は `nablarch-testing-yaml` 担当 | 一致 | なし |
| :257-259 | `fileExtensions` に `sendSyncTestData` を設定しない。設定するとテストデータが見つからず、テストの実行時に例外が発生する | 対象 | 一致 | 実行して確認（[3]）：ディレクトリ `RB11AC0100` を置いたうえで `fileExtensions.sendSyncTestData=xlsx` を設定すると `IllegalStateException: test data file was not found. request id=[RB11AC0100], ...`（`SendSyncSupport.java:350-353`） | 一致 | なし |

対象外と判定した記述と理由（対象外と判定してよい基準は指示書 `87a21d6` §4-2 手順2「この記述が成り立つかどうかは、`nablarch-testing` の実装で決まるか」である）:

- :1・:35・:162 の参照ラベル — 解説書内のラベル定義であり、`nablarch-testing` の実装で成否が決まらない
- :3-4・:10-11・:15-16・:18-19・:37-38・:87-88・:109-110・:124-125・:164-165・:193-194・:229-230 の見出し — 見出し文字列であり、`nablarch-testing` の実装で成否が決まらない
- :6-8 の `.. contents::` — Sphinx のディレクティブであり、解説書のページ構成が決める
- :31・:79・:101・:105・:189 のディレクティブ宣言行（`.. tip::`・`.. important::`）— 宣言行であり、本文の成否はそれぞれ :33・:81・:103・:107・:191 の行で判定した
- :43・:54・:91・:172 の `.. code-block::` 宣言行 — 同上（中身はそれぞれの行で判定した）
- :56・:70 のコード例中のXMLコメント — コメント文字列であり、`nablarch-testing` の実装で成否が決まらない
- :33 の tip「専用のモジュールが `nablarch-testing` に依存する場合は個別に追加しなくてよい」— 他モジュールの `pom.xml` が決めることであり、`nablarch-testing` の実装で成否が決まらない
- :20-29 のうち `<scope>test</scope>` と書く指示 — 利用側プロジェクトの `pom.xml` が決める（座標と「テストでのみ使用する」は対象として判定した）
- :41 の「YAML 形式のテストデータを解析するクラスはこのモジュールが提供する」— `YamlTestDataParser` は `nablarch-testing-yaml` の指示書が担当する
- :45-50 の `nablarch-testing-yaml` の dependency 定義 — 同上
- :71-72 の `class="nablarch.test.core.reader.YamlTestDataParser"` — 同上（`dbInfo`・`interpreters` のプロパティ名は対象として判定した）
- :73 の `ref="dbInfo"` が指すコンポーネント定義 — プロジェクト側またはデフォルト設定モジュール側が定義する
- :77 の「null・空文字・ダブルクォート・改行文字は YAML のパーサが構文として解釈する」— YAML パーサの挙動であり `nablarch-testing-yaml` の指示書が担当する
- :81 のうち「YAML で `"null"` と書いた値が解釈クラスにどう渡るか」— 同上
- :83 の「`YamlTestDataParser` は YAML ファイルを直接読み込むためこの設定を使用しない」— 同上
- :85・:158・:160 のページ間リンク — 解説書内の相互参照であり、`nablarch-testing` の実装で成否が決まらない
- :107 の「システムプロパティを使って環境依存値を上書きする」機構そのもの — `nablarch-core` の `SystemRepository` が担当する（`nablarch-testing` 側の読み出しと条件差は対象として判定した）
- :128-154 の採番設定例に出てくるクラス（`OracleSequenceIdGenerator`・`FastTableIdGenerator`）とそのプロパティ、および `:153` の `ref="dbTransactionManager"` — 前者はプロジェクト側クラス、後者は `nablarch-common-idgenerator-jdbc` が提供するクラスであり、`nablarch-testing` の実装で成否が決まらない
- :199・:235 の `autowireType="None"` — `nablarch-core` の DI が解釈する属性であり、`nablarch-testing` の実装で成否が決まらない
- :227 の構成図（画像） — 解説書の画像アセット。図が表す構成の成否は :225 の行で判定した
- :255 の「`testDataReader` は指定しない」— `YamlTestDataParser` の挙動であり `nablarch-testing-yaml` の指示書が担当する
- 空行 :2・:5・:9・:12・:14・:17・:30・:32・:34・:36・:40・:42・:44・:51・:53・:55・:69・:76・:78・:80・:82・:84・:86・:90・:92・:94・:100・:102・:104・:106・:108・:112・:121・:123・:127・:155・:161・:163・:167・:169・:171・:173・:186・:188・:190・:192・:213・:224・:226・:228・:248・:256 — 記述がない（他の空行は上表の行レンジの内側にある）

##### 行の網羅の検算

表の「行」欄と、対象外リストの「空行」の行番号を機械的に集め、
対象ページの 1..259 を隙間なく・重複なく覆っているかを検算する。
検算スクリプトは `<scratchpad>/fix1/verify_lines.py`（表の1つ目のセルが `（` で始まる横断行は分割の対象外として無視する）。

```
$ python3 <scratchpad>/fix1/verify_lines.py .rn/step4-01-nablarch-testing.md 'setup/common.rst' 259
section        : setup/common.rst
総行数         : 259
拾ったレンジ数 : 115（表 63 / 対象外リスト(空行) 52）
覆った行数     : 259
欠け           : なし
重複           : なし
範囲外         : なし
判定           : OK — 1..259 を隙間なく1回ずつ覆っている
$ echo $?
0
```


##### 取り下げた不一致候補 :89

1. **なぜいったん不一致と判定したか**
   `TestSupport.java:30` の実装既定値は `private static final String DEFAULT_RESOURCE_ROOT = "test/java/";` であり、
   `:356-361` は `SystemRepository.get("nablarch.test.resource-root")` が `null` のときこれを返す。
   解説書 `:89` は「変更する場合は設定する」と書いているため、設定しなければ `src/test/java` から読まれると読める。
   実測（[A][B]）でも、リポジトリが空のときの読み込み先は `test/java/` だった。
2. **なぜ取り下げたか**
   「デフォルト」を決めているのは `nablarch-testing` の実装既定値ではなく、解説書自身が
   `40b9c52`:`setup/request_unit_test/rest.rst:30-33` で名指ししている
   `com.nablarch.configuration:nablarch-testing-default-configuration` である。
   同モジュールの `nablarch/test/test-data.config` が `nablarch.test.resource-root=src/test/java` を持つ。
   `TestSupport.java:30` の `test/java/` は、そのデフォルト設定を読み込まない場合のフォールバックにすぎない。
3. **取り下げの根拠**（自分で再実行して確かめた）

   ```
   $ git -C ~/work/nablarch/nablarch-document show 40b9c52:ja/development_tools/testing_framework/setup/request_unit_test/rest.rst | sed -n '30,34p'
     <!-- テスティングフレームワークのデフォルト設定 -->
     <dependency>
       <groupId>com.nablarch.configuration</groupId>
       <artifactId>nablarch-testing-default-configuration</artifactId>
       <scope>test</scope>

   $ unzip -p ~/.m2/repository/com/nablarch/configuration/nablarch-testing-default-configuration/6-NEXT-SNAPSHOT/nablarch-testing-default-configuration-6-NEXT-20260327.002359-3.jar nablarch/test/test-data.config
   nablarch.test.resource-root=src/test/java
   ```

   実装側の実測は `3c4bd2a`:`TestSupport.java:30,33,356-361`。jar の版は下記「ピン外の引用と、その版」。
4. **現在の判定**: **判定保留（ピンの扱いについて user 判断待ち）**。上記「保留中の論点」参照。

##### 不一致の詳細 :126

1. **解説書の逐語**（`40b9c52`:`setup/common.rst:126`）
   「テスティングフレームワークは、シーケンスオブジェクトを使用した採番処理を、コンポーネント設定ファイルの変更だけでテーブル採番に置き換える機能を提供する。」
2. **実装での実測**（`3c4bd2a`）
   `git grep -rin "idgenerator" 3c4bd2a -- src/main` が0件。`git grep -rn "採番" 3c4bd2a -- src/main` も0件。
   解説書が置き換え先として挙げる `nablarch.common.idgenerator.FastTableIdGenerator`（`setup/common.rst:149`）は
   `nablarch-common-idgenerator-jdbc` が提供する（ローカル `~/.m2` の全 jar を走査し
   `FastTableIdGenerator.class` を含むのはこの artifactId の jar のみ）。
   さらに `pom.xml:98-102` はこの依存を `provided` スコープで宣言しており、Maven の依存スコープ規則により利用者へ推移しない。

   ```
   $ git show 3c4bd2a:pom.xml | sed -n '98,102p'
       <dependency>
         <groupId>com.nablarch.framework</groupId>
         <artifactId>nablarch-common-idgenerator-jdbc</artifactId>
         <scope>provided</scope>
       </dependency>
   ```

   デフォルト設定モジュール（`nablarch-testing-default-configuration`）にも採番関連は皆無である。

   ```
   $ (jar を展開して) grep -rni 'idgenerator' . | wc -l
   0
   $ grep -rn '採番' . | wc -l
   0
   ```
3. **どちらの側が誤っていると考えるか**: **解説書側の誤りの疑い**。
   置き換えの手順（コンポーネント設定の上書き）は `nablarch-core` のリポジトリ機能で成立し、
   置き換え先のクラスは `nablarch-common-idgenerator-jdbc` が提供する。
   `nablarch-testing` はこの機能に対して一切のコードを持たず、依存も `provided` で利用者へ推移せず、
   デフォルト設定モジュールにも設定を持っていない。
   したがって、手順そのものは成立するが「テスティングフレームワークが提供する」という**帰属だけ**が実装と合っていない。

##### 不一致の詳細 :225

1. **解説書の逐語**（`40b9c52`:`setup/common.rst:225`）
   「``fileExtensions``\ の\ ``sendSyncTestData``\ には、実際に配置するテストデータのファイルの拡張子（\ ``xlsx``\ または\ ``xls``\ ）を指定する。指定した拡張子と一致しないファイルは読み込まれない。ベースディレクトリの配下は次の図のとおりで、リクエストIDごとに1つのファイルを置く。」
2. **実装での実測**（`3c4bd2a`）
   読み込みの実体は `src/main/java/nablarch/test/core/reader/PoiXlsReader.java:62-65` である。

   ```java
   File file = new File(path + '/' + fileName + ".xls");
   if (!file.exists()) {
       file = new File(path + '/' + fileName + ".xlsx");
   }
   ```

   ここは `fileExtensions` を一切参照せず、`.xls` を先に試して無ければ `.xlsx` を使う。
   経路を自分で追った結果は次のとおり。
   `SendSyncSupport.java:346`（`basePath` を取得）→ `:347`（`resourceName = <リクエストID>/message`）→
   `:348`（`getFileIfExists` の戻り値。`:349-353` の `null` 判定と `:359` の最終更新日時スナップショットにしか使わない）→
   `:473`（`messagingTestDataParser` を取得）→ `:478`（`getMessageWithoutCache(path, resourceName, ...)`）→
   `BasicTestDataParser.java:99-101` → `TestDataParsingTemplate.java:131,141`（`reader.open(directory, resource)`）→
   `PoiXlsReader.java:48,62-65`。
   すなわち `fileExtensions` が効くのは `nablarch-core` の `FilePathSetting#getFileIfExists`（`:78-81` → `:176-184`）の
   **存在チェックまで**であり、実際に開くファイルは `PoiXlsReader` が拡張子を決め直している。

   実行手順と出力（[X]。検証コードは `fix1/Fix1Xls.java`）:
   同じベースディレクトリに `RB11AC0200.xls`（セルの値 `FROM_DOT_XLS_FILE`）と
   `RB11AC0200.xlsx`（セルの値 `FROM_DOT_XLSX_FILE`）を POI で作り、
   `fileExtensions.sendSyncTestData=xlsx` を設定して `SendSyncSupport#getResponseMessageByRequestId` を呼んだ。

   ```
   ########## CASE 1: .xls と .xlsx の両方を配置 ##########
   [X-0] <base>/RB11AC0200.xls exists=true (値=FROM_DOT_XLS_FILE)
   [X-0] <base>/RB11AC0200.xlsx exists=true (値=FROM_DOT_XLSX_FILE)
   [X-1] FilePathSetting#getFileIfExists("sendSyncTestData","RB11AC0200") = <base>/RB11AC0200.xlsx
   [X-2] SendSyncSupport#getResponseMessageByRequestId -> {DataFileFragment:firstFieldKey=1, XML1=FROM_DOT_XLS_FILE}

   ########## CASE 2: .xlsx のみ配置 ##########
   [X-0] <base>/RB11AC0200.xls exists=false (値=FROM_DOT_XLS_FILE)
   [X-0] <base>/RB11AC0200.xlsx exists=true (値=FROM_DOT_XLSX_FILE)
   [X-1] FilePathSetting#getFileIfExists("sendSyncTestData","RB11AC0200") = <base>/RB11AC0200.xlsx
   [X-2] SendSyncSupport#getResponseMessageByRequestId -> {DataFileFragment:firstFieldKey=1, XML1=FROM_DOT_XLSX_FILE}
   ```

   CASE 1 では、存在チェックが `fileExtensions=xlsx` に従って `.xlsx` を解決しているのに、
   実際に読まれた中身は `.xls` のものである。
3. **どちらの側が誤っていると考えるか**: **解説書が正（＝実装側の誤りの疑い）**。
   解説書は「指定した拡張子と一致しないファイルは読み込まれない」と、`fileExtensions` が読み込み対象を決めると書いている。
   実装は `FilePathSetting` が解決した `File` を `SendSyncSupport.java:348` で受け取っていながら、
   読み込みの段でそれを使わず `PoiXlsReader.java:62-65` で拡張子を決め直しており、設定値の意味を途中で捨てている。
   設定値が読み込み対象を決めるという解説書の契約のほうが素直であり、実装がそれを守っていないと考える。
   なお、この食い違いが表に出るのは `.xls` と `.xlsx` を同じリクエストIDで両方置いたときだけである。
   同じ文が「リクエストIDごとに1つのファイルを置く」とも述べているため、解説書のとおりに1つだけ置く利用者はこの差を踏まない
   （片方しか無い場合、拡張子が一致しなければ `getFileIfExists` が `null` を返して `SendSyncSupport.java:350-353` が例外になる。実測 [D-xls]）。
   **処置は報告のみ。実装は直さない**（`src/main` を変更しない。user 判断 2026-08-26）。

##### 取り下げた不一致候補 :176/:252

1. **なぜいったん不一致と判定したか**
   解説書 `:81` が「``NullInterpreter``\ を指定してはならない。指定すると、文字列として記述した ``"null"``\ も\ Java\ の\ null\ になり、両者を区別できなくなる。」と書いているのに、
   `:176`（`messagingTestInterpreters` の先頭要素 `NullInterpreter`）と
   `:250-252`（YAML 形式の `messagingTestDataParser` の `interpreters` にその `messagingTestInterpreters` を指定）が
   組み合わさると、YAML 形式の解析コンポーネントに `NullInterpreter` が入る。
   前回はこれを「解説書内の記述同士の矛盾」と判定し、根拠に `[G] Quotation+Null("\"null\"") -> null` という出力を置いた。
2. **なぜ取り下げたか**
   その `[G]` の出力は**偽の根拠**だった。`System.out.println("... -> " + v)` は、`v` が Java の `null` でも
   長さ4の文字列 `null` でも同じ `null` と表示する。判別できる出力に直して測り直したところ、
   解説書の順序では **Java の `null` にならない**ことが分かった。
3. **取り下げの根拠**（`file:line` ＋ SHA ＋ 実行結果）
   `3c4bd2a`:`src/main/java/nablarch/test/core/util/interpreter/InterpretationContext.java:81-93` の `invokeNext` は
   `interpreters.remove()` であり、`interpreters` は `LinkedList`（同 `:5,31,51`）なので **FIFO**（先頭から取り出す）である。
   解説書の順序（`:176` `NullInterpreter` → `:177` `QuotationTrimmer` → `:178-184` `CompositeInterpreter`）では、
   `NullInterpreter`（`3c4bd2a`:`NullInterpreter.java:15-16`）が先に**引用符付きの** `"null"`（長さ6）を見るため
   `equalsIgnoreCase("null")` は偽になり、`context.invokeNext()` で次へ進む。
   次の `QuotationTrimmer`（`3c4bd2a`:`QuotationTrimmer.java:12-16,24-30`）が引用符を外すが、
   そのときにはもう `NullInterpreter` はキューに残っていない。結果は長さ4の文字列 `null` である。

   実行結果（[I]。検証コードは `fix1/Fix1Interpreter.java`）:

   ```
   [I-0] Queue impl = java.util.LinkedList / remove() は先頭要素を取り出す
   --- [I-1] 解説書の順序 (Null -> Quotation -> Composite) ---
   [I-1] input="null" (引用符あり, len=6) -> "null" (len=4, isJavaNull=false)
   [I-1] input=null   (引用符なし, len=4) -> <<JAVA null>>
   [I-1] input=NULL   (引用符なし, len=4) -> <<JAVA null>>
   [I-1] input=abc                        -> "abc" (len=3, isJavaNull=false)
   --- [I-2] 順序を入れ替えた場合 (Quotation -> Null -> Composite) ---
   [I-2] input="null" (引用符あり) -> <<JAVA null>>
   --- [I-3] NullInterpreter 単体 ---
   [I-3] input="null" (引用符あり) -> ""null"" (len=6, isJavaNull=false)
   [I-3] input=null   (引用符なし) -> <<JAVA null>>
   --- [I-4] 前回の記録が偽の根拠だったことの再現 ---
   [I-4] println("result -> " + r) の見た目 : result -> null
   [I-4] 判別可能な出力              : result -> "null" (len=4, isJavaNull=false)
   ```

   あわせて、`:81` の `.. important::` は「テストデータの形式をYAMLに変更する」節
   （見出し `:37-38`、範囲は次の見出し `:87-88` の手前まで）の中にあり、主語は直前 `:77` の `yamlInterpreters` である。
   別節（見出し `:164-165`）の `messagingTestInterpreters` には及ばない。
   `:170` は「テストデータの記法を解釈するクラスは、Excel 形式と YAML 形式で共通である」と意図的に述べている。
4. **現在の判定**: **一致**。実装は解説書 `:176`／`:252` のとおりに動く。

##### 動かして確かめた内容

`src/main`・`src/test`・解説書を変更しないため、検証コードはすべてスクラッチパッド配下に置いて実行した。
今回の是正で新しく組んだものは
`/tmp/claude-1000/-home-tie303177-work-nablarch-nablarch-testing/92111fc8-7ec9-430c-96c2-1a8e7db53a6f/scratchpad/fix1/` 配下にある
（`Fix1Interpreter.java`・`Fix1Xls.java`・`Fix1Time.java`・`Fix1Cache.java`・`Fix1Interp2.java`・`Fix1Dump.java`・
`dupsrc/nablarch/test/Fix1FirstWins.java`・`dupsrc/nablarch/test/Fix1Env.java`・
`mutsrc/`・`mutsrc2/`（変異させた `TestSupport.java`）・`verify_lines.py`）。
クラスパスは `mvn -o dependency:build-classpath` の出力＋`target/classes`（`mvn -o compile` でビルド）＋
ログ設定用の `fix1/logcp`。

`[A][B][D]`（据え置き。旧 `probe/Probe.java`）:

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
```

`[1][2][3]`（据え置き。旧 `probe/Probe2.java`。`SendSyncSupport#getResponseMessageByRequestId` を実際に呼んだ結果）:

```
[1] sendSyncTestData未設定 -> java.lang.IllegalArgumentException: Unknown basePathName: sendSyncTestData
[2] messagingTestDataParser未設定 -> java.lang.IllegalStateException: can't get TestDataParser. check configuration.
[3] YAML(dir)にxlsx指定 -> java.lang.IllegalStateException: test data file was not found. request id=[RB11AC0100], base path=[<scratchpad>/msgbase], resource name=[RB11AC0100/message], absolute base path=[<scratchpad>/msgbase].
```

`[I]`（`fix1/Fix1Interpreter.java`。解釈クラスの適用順と、Java の `null` と長さ4の文字列 `null` の判別）
— 出力は上記「取り下げた不一致候補 :176/:252」に掲載。

`[X]`（`fix1/Fix1Xls.java`。`.xls` と `.xlsx` のどちらが読まれるか）
— 出力は上記「不一致の詳細 :225」に掲載。

`[F]`（`fix1/dupsrc/nablarch/test/Fix1FirstWins.java`。同名テストデータが複数ディレクトリに実在する場合の先勝ち）:

```
$ cd <scratchpad>/fix1/dupwork
$ java -cp "<out>:<cp>" nablarch.test.Fix1FirstWins . "dupA;dupB"
[F-0] ./dupA/nablarch/test/dupprobe/DupProbe.xls exists=true 値=FROM_DIR_dupA
[F-0] ./dupB/nablarch/test/dupprobe/DupProbe.xls exists=true 値=FROM_DIR_dupB
[F-0] nablarch.test.resource-root = dupA;dupB
[F-1] getTestDataPaths()   = [dupA/nablarch/test/dupprobe, dupB/nablarch/test/dupprobe]
[F-2] getPathOf("DupProbe/dup") = dupA/nablarch/test/dupprobe
[F-2b] getPathResourceExisting(候補, "DupProbe/dup") = dupA/nablarch/test/dupprobe
[F-3] getListMap("dup","probe") = [{which=FROM_DIR_dupA}]
[F-5] 先勝ち(先頭 dupA が読まれた)か = true
[F-5] ASSERTION OK      (exit=0)

$ java -cp "<out>:<cp>" nablarch.test.Fix1FirstWins . "dupB;dupA"     # 先頭を入れ替える
[F-2] getPathOf("DupProbe/dup") = dupB/nablarch/test/dupprobe
[F-3] getListMap("dup","probe") = [{which=FROM_DIR_dupB}]
[F-5] ASSERTION FAILED: 先勝ちではない   (exit=1)
```

`[F-mut]`（同じ実測を、`getPathResourceExisting` を last-match-wins に変異させた実装で実行した負のテスト）:

```
$ diff <(git show 3c4bd2a:src/main/java/nablarch/test/TestSupport.java) fix1/mutsrc/nablarch/test/TestSupport.java
308a309,310
>         // ===== MUTANT: last-match-wins =====
>         String found = null;
311c313
<                 return basePath;
---
>                 found = basePath;
314c316
<         return null;
---
>         return found;

$ java -cp "<mutclasses>:<out>:<cp>" nablarch.test.Fix1FirstWins . "dupA;dupB"
[F-2] getPathOf("DupProbe/dup") = dupB/nablarch/test/dupprobe
[F-3] getListMap("dup","probe") = [{which=FROM_DIR_dupB}]
[F-5] ASSERTION FAILED: 先勝ちではない   (exit=1)
```

`[M-A][M-B]`（既存テストの検知力を変異で測った結果。`org.junit.runner.JUnitCore` を直接起動しており、`src/test` は変更していない）:

```
$ CP="target/test-classes:target/classes:src/test/resources:$(cat cp.txt)"

# 素の実装
$ java -cp "$CP" org.junit.runner.JUnitCore nablarch.test.TestSupportTest
OK (24 tests)

# [M-A] 変異A: getPathResourceExisting を last-match-wins にする
$ java -cp "<mutclasses>:$CP" org.junit.runner.JUnitCore nablarch.test.TestSupportTest
OK (24 tests)          <- 落ちない。既存テストは :103 を検知していない

# [M-B] 変異B: PATH_SEPARATOR を ";" から "," にする
$ java -cp "<mutclasses2>:$CP" org.junit.runner.JUnitCore nablarch.test.TestSupportTest
1) testGetTestDataPaths(nablarch.test.TestSupportTest)
2) testGetPathOf(nablarch.test.TestSupportTest)
Tests run: 24,  Failures: 2      <- 落ちる。既存テストは :95-99 を検知している
```

`[P]`（`fix1/dupsrc/nablarch/test/Fix1Env.java`。`-Dnablarch.test.resource-root` が効く条件）:

```
###### target/test-classes あり（リポジトリ初期化が成功する）/ -D なし ######
[P] TestSupport.getResourceRootSetting() = src/test/java
[P] (TestSupport ロード後) SystemRepository.get = src/test/java
###### target/test-classes あり / -Dnablarch.test.resource-root=OVERRIDDEN/BY/SYSPROP ######
[P] TestSupport.getResourceRootSetting() = OVERRIDDEN/BY/SYSPROP
[P] (TestSupport ロード後) SystemRepository.get = OVERRIDDEN/BY/SYSPROP
###### target/test-classes なし（unit-test.xml の参照クラスが解決できず初期化が失敗し握りつぶされる）/ -D なし ######
[P] unit-test.xml on classpath = true
[P] TestSupport.getResourceRootSetting() = test/java/
[P] (TestSupport ロード後) SystemRepository.get = null
###### target/test-classes なし / -Dnablarch.test.resource-root=OVERRIDDEN/BY/SYSPROP ######
[P] TestSupport.getResourceRootSetting() = test/java/
[P] (TestSupport ロード後) SystemRepository.get = null      <- -D が効かない
```

`[T][N]`（`fix1/Fix1Time.java`。`fixedDate` の検証範囲と、コンポーネント名 `systemTimeProvider`）:

```
[T] SimpleDateFormat#isLenient の既定値 = true
[T] setFixedDate("20100914123456") len=14 -> getDate()=2010-09-14 12:34:56.000
[T] setFixedDate("20101332123456") len=14 -> getDate()=2011-02-01 12:34:56.000
[T] setFixedDate("20100914993456") len=14 -> getDate()=2010-09-18 03:34:56.000
[T] setFixedDate("99999999999999") len=14 -> getDate()=10007-06-11 04:40:39.000
[T] setFixedDate("20100914123456789") len=17 -> getDate()=2010-09-14 12:34:56.789
[T] setFixedDate("2010091412345") len=13 -> java.lang.IllegalArgumentException: datetime string 2010091412345
[T] setFixedDate("201009141234561") len=15 -> java.lang.IllegalArgumentException: datetime string 201009141234561
[N] コンポーネント名="systemTimeProvider" -> SystemTimeUtil.getDate() = 2010-09-14 12:34:56.000
[N] コンポーネント名="fixedSystemTimeProvider" -> java.lang.IllegalArgumentException: specified systemTimeProvider is not registered in SystemRepository.
```

`[C]`（`fix1/Fix1Cache.java`。同じ JVM のままテストデータを書き換えて読み直せるか）:

```
[C-1] 1回目（編集前）   -> {DataFileFragment:firstFieldKey=1, XML1=BEFORE_EDIT}
[C-2] テストデータを書き換えた（BEFORE_EDIT -> AFTER_EDIT）
[C-3] 2回目（編集後）   -> {DataFileFragment:firstFieldKey=1, XML1=AFTER_EDIT}

[C-4] PoiXlsReader 単体で useCache の効果を比較する
[C-4] useCache=true  1回目 -> [1, V1]
[C-4] useCache=true  2回目（ファイルは V2 に書き換え済み）-> [1, V1]
[C-4] useCache=false 3回目（同じファイル）              -> [1, V2]
```

`[R]`（`fix1/Fix1Interp2.java`。`interpreters` が Excel 側の同期応答メッセージ読み込み経路で実際に適用されるか）:

```
[R] セルの値 = "QUOTED" （前後にダブルクォートあり）
[R] interpreters = messagingTestInterpreters（Null/Quotation/Composite） -> {DataFileFragment:firstFieldKey=1, XML1=QUOTED}
[R] interpreters = 空リスト（解釈クラスを与えない）              -> {DataFileFragment:firstFieldKey=1, XML1="QUOTED"}
```

**取り下げた出力**: 旧 `[G]`（`Quotation+Null("\"null\"") -> null`）は `println` が Java の `null` と
文字列 `null` を区別できないことによる偽の根拠だったため取り下げた（[I-4] で再現）。
旧 `[H]`（`fixedDate` の桁数チェック）は `[T]` に置き換えた。
旧 `[T]`（`mvn -o test -Dtest=TestSupportTest` が24件緑）は「緑であること」を根拠にしていたため取り下げ、
変異で落ちることの実測（`[M-A]`・`[M-B]`）に差し替えた。

##### ピン外の引用と、その版

§2 のピン（`nablarch-document` `40b9c52` ／ `nablarch-testing` `3c4bd2a`）の外から引いた事実と、その版を示す。

| 引用元 | 版の特定 | 本記録で引いた箇所 |
|---|---|---|
| `nablarch-core` | `mvn -o dependency:build-classpath` が解決した実体は `~/.m2/repository/com/nablarch/framework/nablarch-core/6-NEXT-SNAPSHOT/nablarch-core-6-NEXT-SNAPSHOT.jar`。md5 `739824ac93ef8d391599a284cdd716c2` で、同ディレクトリの `nablarch-core-6-NEXT-20260717.011251-20.jar` と同一（同居する `nablarch-core-6-NEXT-20260327.002503-19.jar` は md5 `03035e977de61a5178c65fced43141eb` で別物）。行番号は同じ版の `-sources.jar` から取り出したソースのもの | `FilePathSetting.java:27,30,43-49,78-81,143-151,176-184,192-196,229,231,309-311`、`SystemTimeUtil.java:26,109` |
| `nablarch-testing-yaml` | コミット `05ada91` | `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:43,47,49,78-82,86-90,162-166,188-192` |
| `nablarch-testing-default-configuration` | `~/.m2/repository/com/nablarch/configuration/nablarch-testing-default-configuration/6-NEXT-SNAPSHOT/nablarch-testing-default-configuration-6-NEXT-20260327.002359-3.jar`。md5 `f1432ceb68d7dd0018c922f3b6e7df82` で、同ディレクトリの `…-6-NEXT-SNAPSHOT.jar` と同一 | `nablarch/test/test-data.config`、`nablarch/test/test-data.xml`、`nablarch/test/test-data-interpreter.xml` |
| `nablarch-common-idgenerator-jdbc` | `~/.m2/repository/com/nablarch/framework/nablarch-common-idgenerator-jdbc/6-NEXT-SNAPSHOT/nablarch-common-idgenerator-jdbc-6-NEXT-20260327.004352-2.jar`。md5 `99bd6c847e2a4960d889cda72127de3f` で、同ディレクトリの `…-6-NEXT-SNAPSHOT.jar` と同一 | `FastTableIdGenerator.class` の所在 |
| Maven の依存スコープ規則 | 仕様であり版を持たない。本記録では「`provided` スコープの依存は利用者へ推移しない」ことのみを使う | :126 の判定根拠 |

集計: 表の行 64（うち1件は複数箇所にまたがる横断行「:176 と :252 の組み合わせ」で、行番号は `:174-185`・`:249-255` に含まれる）。
対象 22件・一部対象外 8件（合わせて判定対象30件）／対象外 34行。
判定の内訳は 一致 25件／不一致（解説書が正＝実装側の誤りの疑い）1件（:225）／不一致（解説書側の誤りの疑い）1件（:126）／
判定保留（ピンの扱いについて user 判断待ち）3件（:39・:89・:166）。
前回からの変更: :89 の不一致は取り下げて判定保留に、:176/:252 の不一致は取り下げて一致に、:225 を新規に不一致として起票した。

