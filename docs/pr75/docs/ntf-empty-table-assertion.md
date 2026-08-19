# 期待値0件テーブル検証の偽陰性 — 対応方針

作成日: 2026-08-18

**位置づけ**: ユーザーと合意した対応方針。`docs/pr75/steering.md` にタスクとして起票する前の判断根拠を残す。
後方互換への影響があるため、ステアリングとは別文書とする。

**出典の書き方**: 本書の事実はすべて実物のソースで確認したものに限る。出典は「リポジトリ相対パス:行番号」で示す。
リポジトリ名の記載が無いものは `nablarch-testing`（本リポジトリ）を指す。確認していないことは「未確認」と明記する。

---

## 1. 結論（あるべき姿）

**0件のテーブルブロックにカラム名は不要。形式を問わず統一する。**

カラム名は「検証対象カラムの絞り込み」であり、行が0件なら絞り込む対象が存在しない。
**カラム名を書いたか書かなかったかで検証結果が変わってはならない。**

現状はこの原則が2箇所で破れており、それぞれ層の異なる別問題である。

| | 問題1 | 問題2 |
|---|---|---|
| 層 | `TableData`（**形式共通**） | 表形式リーダ（**Excel 等の行指向データ**） |
| 何が起きるか | 検証すべきものを検証しない（偽陰性） | テストデータの一部を捨て、別の意味に解釈する |
| 直す場所 | `TableData#loadData()` | `TestDataParsingTemplate` + 各パーサの `onTargetTypeFound` |
| 後方互換影響 | あり（嘘の合格をしていたテストが落ちる） | 実質なし |

---

## 2. 「0件にカラム名は不要」の根拠

**0件のとき、テストデータに書かれたカラム名はどこからも参照されない。**

### 期待値（EXPECTED_TABLE）が0行のとき

`src/main/java/nablarch/test/Assertion.java:256` で `expected.getColumnNames()` を取得するが、
これを使うカラム比較ループは期待値の行ループの内側にある（`Assertion.java:297-303`）。行数0なら1度も回らない。

実データ側の余剰行の検出を担う `dbDataFound` の走査（`Assertion.java:306-314`）はカラム名を使わない。
PK 突合に使う `getPrimaryKeys()` は `dbInfo` 由来である（`src/main/java/nablarch/test/core/db/TableData.java:480-482`）。

### 準備データ（SETUP_TABLE）が0行のとき

`deleteData` はテーブル名のみを使う（`TableData.java:127-130`）。
`insertData` の INSERT 文は `getNonComputedColumns()`＝`dbInfo` 由来で組み立てられ（`TableData.java:139-141`）、
`contents` が0件なのでバインドループが回らない（`TableData.java:143`）。

### 形式ごとの帰結

| 形式 | カラム名の要否 | 理由 |
|---|---|---|
| 仕様（データモデル） | **不要** | 0件では参照されない。カラム名は DB スキーマが持つ情報 |
| YAML | **不要**（書く場所が無い） | `rows: []` が0件表明として成立する。構造でブロックが区切られる |
| Excel | **不要** | 識別子行の並びから0件ブロックであることを判別できる（4章） |

---

## 3. 問題1 — カラム名0件だと SQL を発行しない（形式共通）

### 現状

`TableData.java:337-346`

```java
public void loadData() {

    String[] colNames = getColumnNames();

    // 取得対象カラムが1つも存在しない場合は
    // data_に空のListをセットして終了する
    if (colNames.length == 0) {
        contents = new ArrayList<SqlRow>(0);
        return;
    }
```

期待値0行・カラム名0件のとき、DB を読まずに「実データも0行」として扱うため、
**DB に行が残っていても検証が必ず PASS する。**

### なぜ誤りか

ここは「カラム名が0件」という*テストデータの書かれ方*を、「DB を読まなくてよい」という*検証の実施可否*に読み替えている。
カラム名が無いのは「**何を比較するか**の情報が無い」だけで、「**比較しなくてよい**」ことは意味しない。

「カラム名が無いと SELECT が組めない」も成立しない。カラム名は DB スキーマが持っており、`TableData` は `dbInfo` を保持している。
実際、`getColumnNames()` は `columnNames == null` のとき `dbInfo.getColumns(tableName)` にフォールバックする
（`TableData.java:501-506`）。**情報源は既にあり、長さ0の配列のときだけそこに繋がっていない。**

### あるべき姿

`loadData()` は、テーブル名が決まっている以上つねに DB の実データを読む。
カラム名の宣言は「検証対象カラムの絞り込み」であって「検証するかどうか」のスイッチではない。

### 修正方針

`loadData()` の early return を削除し、`colNames.length == 0` のときは `dbInfo.getColumns(tableName)` を
SELECT 対象カラムとする。**修正は `loadData()` 内に閉じる。**

`getColumnNames()` 自体は変更しない。同等の変更を YAML 側で行って差し戻した経緯があるためである
（`nablarch-testing-yaml` の revert コミット `190cc9a`。`getColumnNames()` を DB 参照に変えた結果、
`StubDbInfo` を差す DB レスの変換ツール経路が `UnsupportedOperationException` で壊れた）。
`loadData()` はもともと DB を読むメソッドなので、この問題を踏まない。

### 待機中のテスト

`nablarch-testing-yaml` に、本修正を待って `@Ignore` されているテストが4件ある（revert コミット `190cc9a` のメッセージに記載）。
本修正の完了後に `@Ignore` を外す必要がある。

- `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`
- `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`
- `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`
- `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`

同リポジトリの `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:110-114` に、
本事象を説明する FIXME コメントが残っている。本修正後に削除する。

---

## 4. 問題2 — カラム名行として次の識別子行を食う（表形式リーダ）

### 現状

`src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116`

```java
void onTargetTypeFound(List<String> line) {
    // テーブル名
    String tableName = getTypeValue(line);
    // カラム名の行を読み込み
    header = new HeaderLine(readLine());
    ...
}
```

`readLine()` が返した行が本当にカラム名行なのかを検証していない。
空行は `readTestData()` で既に除去済みなので（`TestDataParsingTemplate.java:180-182`）、ここで返るのは「次の非空行」である。

同じ書き方が `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` にもある。
`TableDataParser` 固有ではなく、表形式リーダ層に共通する構造である。

### 何が起きるか

**(a) 次の行が別ブロックの識別子行のとき** — その行がカラム名として消費される。
`doParse` のループはその次から再開し、`nowReading` は真のままなので、
**次ブロックのデータ行が手前のテーブルのデータ行として取り込まれる**
（`TestDataParsingTemplate.java:287-309`、`TableDataParser.java:97-100`）。
次ブロックは消え、手前のテーブルは汚染される。

**(b) 次の行が無い（シート末尾）のとき** — `HeaderLine` が `Collections.emptyList()` になり
（`src/main/java/nablarch/test/core/reader/HeaderLine.java:32-38`）、カラム名0件の `TableData` ができる。
これは問題1に落ちる。

### なぜ誤りか

**(a) は曖昧ではない。パーサは判別できる。** カラム名に `EXPECTED_TABLE=` のような文字列は現れない。
`doParse` は既に全行に対して `getDataType()` を実行しており（`TestDataParsingTemplate.java:290`）、判定手段は最初から手元にある。

問題は判定できないことではなく、`onTargetTypeFound` が判定せずに無条件で `readLine()` していることである。

### あるべき姿

識別子行の次の行が:

| 次の行 | 扱い | 現状からの変更 |
|---|---|---|
| 通常行（`DataType.DEFAULT`） | カラム名行として消費する | なし |
| **識別子行** | **消費しない。カラム名0件ブロックとして確定し、その識別子行は次の反復で処理する** | **本修正** |
| 無い（シート末尾） | カラム名0件ブロックとして確定 | なし |

**エラーにする箇所は無い。** 「0件にカラム名は不要」という仕様と一貫する。

なお、識別子行の次が**通常行**のとき、それが「カラム名行」なのか「カラム名を省略したデータ行」なのかは区別できない。
ここは現状どおりカラム名行と解釈するしかないが、**0件ブロックにはデータ行が無いのでこの曖昧さに突き当たらない。**

### 修正方針

`TestDataParsingTemplate` にヘッダ行読み込み用のヘルパを追加し、
次行が識別子行の書式に一致する場合は消費せずに巻き戻す（`index` を戻す。`TestDataParsingTemplate.java:352-357`）。
`TableDataParser#onTargetTypeFound`（`TableDataParser.java:111`）と
`ListMapParser#onTargetTypeFound`（`ListMapParser.java:80`）の `readLine()` をこのヘルパに置き換える。

`doParse` のループ側は変更不要である。巻き戻された識別子行は次の反復で読まれ、
同じデータタイプなら `onTargetTypeFound`、別のデータタイプなら `else` 節で `break` と、いずれも正しく処理される
（`TestDataParsingTemplate.java:292-308`）。

### 実装上の注意 — 判定は厳密にしないと既存を壊す

**既存の `getDataType()` をそのままヘッダ行の判定に流用してはいけない。**
判定が前方一致だからである（`TestDataParsingTemplate.java:328`）。

```java
if (dataTypeCell.startsWith(type.getName())) {
```

`DataType` には `MESSAGE` がある（`src/main/java/nablarch/test/core/reader/DataType.java:44`）。
したがって **`MESSAGE_ID` というカラム名は `DataType.MESSAGE` と判定される。**
先頭カラムが `MESSAGE_ID` のテーブルのヘッダ行を「識別子行だから消費しない」と扱うと、
そのテーブルはカラム名0件になり、ヘッダ行は `else` 節で `break` されて**ブロックが壊れる**。
現在動いているテストを壊す変更になる。

判定は識別子の書式そのもの、すなわち **`TYPE=` または `TYPE[groupId]=`** に限定する。
`getTypeValue` が `indexOf('=')` を前提にしていること（`TestDataParsingTemplate.java:342-343`）からも、
識別子行は `=` を含む形で確定している。カラム名に `=` は現れない。

---

## 5. 後方互換への影響

### 問題1の修正

**影響範囲は期待値検証だけ。** `loadData()` のプロダクションコードからの呼び出し元は `Assertion.java:81` の1箇所のみで、
`assertTableEquals(String, TableData)` からしか呼ばれない
（`grep -rn "loadData()" src/main` で確認）。
**準備データ投入・マスタデータ投入・変換ツールには一切影響しない。**

分岐条件は `colNames.length == 0` のみ（`TableData.java:343`）なので、
**カラム名を1つでも書いていれば挙動は完全に不変。**

該当するのは次の書き方である。

| 形式 | 該当する書き方 | 互換対象か |
|---|---|---|
| Excel | 識別子行がシート末尾（カラム名行なし） | 対象 |
| Excel | カラム名行がマーカーカラム（`[ ]`）のみ | 対象 |
| YAML | `rows: []` | 対象外（未リリース） |

**影響の中身**:

- **DB が実際に空だった → PASS のまま。影響なし**
- **DB に行が残っていた → 新たに FAIL。** これは検証されていなかったものが検証されるようになったものであり、
  落ちるのは意図したアサートの結果である

**新たに発生しうる失敗パターンが1つある。** 現状の0件カラム経路は `dbInfo` に一切触れずに return するため、
**DB に存在しないテーブル名を書いていても PASS する。**
修正後は `getPrimaryKeys()` → `dbInfo.getPrimaryKeys(tableName)`（`TableData.java:480-482`）に到達するため例外になる。
テーブル名の誤記が検出されるようになる、という変化である。

### 問題2の修正

**互換影響は実質ゼロ。**

対象となる (a) のケースでは、現状すでに次ブロックが消失し、そのデータ行が手前のテーブルに取り込まれている
（`TestDataParsingTemplate.java:287-309`）。この状態で意図どおり動いていたテストは存在し得ない。

(b) のシート末尾ケースは**現状どおり0件ブロックとして読まれ続ける**（挙動不変）。
「シート末尾に `SETUP_TABLE=X` だけ書いてテーブルをクリアする」という書き方も従来どおり動作する。

ただし4章の「判定は厳密に」を守らない場合、`MESSAGE_ID` 等のカラム名を持つテーブルを壊す。
これは実装上の注意点であり、方針そのものの互換影響ではない。

### まとめ

| 修正 | 互換影響 |
|---|---|
| 問題1（`loadData`） | 期待値検証のみ。これまで嘘の合格をしていたテストが落ちる。正しく空だったケースは無影響。加えてテーブル名の誤記が例外になる |
| 問題2（識別子行を食わない） | 実質ゼロ。現状データが失われている状態のため |

---

## 6. 採らなかった案とその理由

| 案 | 却下理由 |
|---|---|
| **設定で現行動作（SQL を投げず0行扱い）に戻せるようにする** | 「テストが嘘の合格を返す」挙動を設定で維持できるようにするのは、テスティングフレームワークとして筋が悪い。守る対象も、Excel は記載例どおりなら踏まず、YAML は未リリースで互換制約が無いため、費用対効果が立たない |
| **YAML 形式のときだけ直す** | 形式によって検証の挙動が変わる。`TableData` は読み込み元の形式を保持していない（コンストラクタは `dbInfo`/`tableName`/`columnNames`/`defaultValues` のみ。`TableData.java:71-75`）ため、現構造とも噛み合わない。Excel 側の事象も残る |
| **問題2 で識別子行を食うケースをエラーにする** | 曖昧ではなく判別できるので、エラーにする理由が無い。「0件にカラム名は不要」という仕様と矛盾する |
| **問題2 でカラム名行が無い（シート末尾）ケースをエラーにする** | 現在動作している「識別子行のみでテーブルをクリアする」書き方を壊す。かつ問題1を直せば偽陰性は消えるので不要 |
| **YAML に `columns:` フィールドを追加する** | カラム名は DB スキーマが持つ情報であり、テストデータに二重に持たせるものではない。YAML はデータモデルをそのまま表す形式であり、現在のスキーマがあるべき姿である |

### 設定フラグを設けない理由（ユーザー提起・2026-08-19）

一度は「設定フラグ（デフォルト無効）で現行動作を残す」案を検討したが、次の2点により**設定は設けない**。

1. **問題2があるため、カラム名行を書かないブロックは利用PJで成立していない。** 識別子行の直後に識別子行を置いた
   データは、後続ブロックを食われて別の意味に解釈される（4章）。動いているように見えているものは偶然であり、
   利用PJ側で見直すべき書き方である
2. **したがって、問題1の修正で NG になるテストは、利用PJ側が見直す対象である。** 嘘の合格を設定で維持できるように
   することは、その見直しを先送りするだけで、テスティングフレームワークとして筋が悪い（6章の1行目と同じ理由）

**本リポジトリの実測でも、問題1の修正で挙動が変わる既存データは0件である（8.1）。**
対象PJのテストデータは未走査（10章）。

---

## 7. 確認方法（TDD の進め方）

ステアリングの追加フェーズ専用ルール（事象ごとに再現テスト→修正の順でコミットを分ける）に従う。

**問題1の再現テスト**: DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を
`Assertion.assertTableEquals` に渡し、現状では PASS してしまうことを示す。

**問題2の再現テスト**: 識別子行の直後に別の識別子行が続くテストデータを読ませ、
現状では後続ブロックが結果に現れず、手前のテーブルに後続ブロックのデータ行が混入することを示す。

**互換確認**: 問題2については、先頭カラム名が `MESSAGE_ID` のようにデータタイプ名で始まるテーブルを含む
テストデータが正しく読めることを、修正と同じコミットで確認する。

---

## 8. 実測結果（2026-08-19）

本章の数値はすべて 2026-08-19 に実行して得たものである。

### 8.1 既存 Excel テストデータの走査（本リポジトリ）

`src/test` 配下の `.xls`/`.xlsx` 全件を Apache POI で走査した。空行を除去したうえで、テーブル系識別子行
（`SETUP_TABLE=`／`EXPECTED_TABLE=`／`EXPECTED_COMPLETE_TABLE=`／`LIST_MAP=`）の直後の行を判定した。

| 走査対象 | 件数 |
|---|---|
| ファイル | 59 |
| シート | 242 |
| テーブル系識別子行 | 324 |

| 検出した形 | 件数 |
|---|---|
| 識別子行の直後が識別子行（問題2(a)） | **0** |
| 識別子行がシート末尾（カラム名行なし） | **0** |
| ヘッダ行がマーカーカラムのみ | 14（すべて `LIST_MAP=requestParams`） |
| ヘッダ行の先頭カラムが DataType 名で始まる（`MESSAGE_ID` 型の罠） | **0** |

マーカーカラムのみの14件はすべて `LIST_MAP` であり、`ListMapParser` 経由で `TableData` を生成しないため
`loadData()` に到達しない。

**→ 本リポジトリに、問題1の修正で挙動が変わる既存テストデータは存在しない。**

走査プログラムは scratchpad に置いたのみでリポジトリには残していない。再実行が必要な場合は上記の判定条件で書き直すこと。

### 8.2 問題1の影響範囲（コードで確認）

`loadData()` の呼び出し元は `src/main/java/nablarch/test/Assertion.java:81` の1箇所のみで、
`assertTableEquals(String, TableData)` からしか到達しない（`grep -rn "loadData()" src/main`）。
期待値検証の入口は `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362` の1本であり、
`TestShot`・`AbstractHttpRequestTestTemplate` もここを通る。

### 8.3 マーカーカラム案の実測

本体の `TableDataParser` に行リストを直接流して測定した
（`DataType.SETUP_TABLE_DATA`／インメモリ `TestDataReader`／スタブ `DbInfo`）。

| 入力 | 結果 |
|---|---|
| 識別子行のみ（現状の問題2） | ブロック **2→1件**。`table=EMPTY_T columnNames=[SETUP_TABLE=NEXT_T] rows=2` |
| 識別子行＋マーカーカラムのみのヘッダ行 | ブロック **2件**。`table=EMPTY_T columnNames=[] rows=0`／`table=NEXT_T columnNames=[PK, NAME] rows=1` |
| 通常の2ブロック（対照） | ブロック 2件。正常 |

**→ マーカーカラム行を1行はさむと、0件テーブルが正しく生成され、次のブロックも無傷になる。**
マーカーカラムの判定は `startsWith("[") && endsWith("]")` のみである
（`src/main/java/nablarch/test/core/reader/HeaderLine.java:88-90`）。

`nablarch-testing-converter` の `issues.md` XLS-27 に「マーカーカラム案の実測は未実施」とあるが、
**本体側の成立は本測定で確認した。**

**未確認**: 本測定はパーサ層に行リストを直接流したものであり、`XlsReader` を通した実 `.xlsx` 経路では
実行していない。converter 側（実 `.xlsx` 経路・Excel→YAML の逆変換）も未確認である。

### 8.4 マーカーカラム案は現行記法に反しない

`testdata_notation.rst:802` は「データ行を書かない場合でも、カラム名の行は省略できない」と定めている。
マーカーカラム行はカラム名行として実在するため、この記法を破らない。
**マーカーカラム案を採る場合、記法の変更は不要である。**

---

## 9. 解説書への反映（`nablarch-document` チーム向け）

以下は本リポジトリのスコープ外。解説書チームへ別途伝達する。

- ~~0件のテーブルブロックはカラム名行を省略できる旨を明記する。現状の解説書には「必須」「省略した場合はこうなる」の記述が無い~~
  **← この記述は誤りだった。訂正する（2026-08-19）。** `testdata_notation.rst:802`
  （`nablarch-document` の `ntf-yaml-support` ブランチ・`b75f1d7`・2026-08-14 追記）に
  「データ行を書かない場合でも、カラム名の行は省略できない。識別子行の次の行がカラム名の行として
  読み込まれるため、カラム名の行を書かないと、その次に現れた行がカラム名の行になる。」と明記されている。
  **問題2は未文書の不具合ではなく、明文化された現行仕様である。**
- **YAML の `rows: []` が0件検証として有効**である旨を明記する。`ja/development_tools/` 配下の rst に `rows: []` の記述は無い（未確認: この件数は本セッションでは再確認していない）
- 公式の記載例はいずれもカラム名行を書いているため、**例の変更は不要**

---

## 10. 未確認事項

- **カラム名0件かつ行が1件以上ある期待値**（Excel でマーカーカラムのみのヘッダにデータ行が続く形）の
  問題1修正後の挙動。現状は実データ0行扱いで PK 不一致 fail、修正後は `expected.getValue()` 側に値が無く
  別の失敗の仕方になる可能性がある
- **既存プロジェクトでの該当パターンの出現頻度。** 本リポジトリのテストデータ以外は調査していない
- **先頭カラム名がデータタイプ名で始まるテーブル**（`MESSAGE_ID` 等）が既存テストデータに実在するか。
  `src/test/resources` の SQL・XML には該当が見つからなかったが、xls 内までは走査していない
- **Excel の実測**（実 xlsx をリーダに読ませた結果）。本書の記述はすべてソースを読んで確認したものであり、
  実行して再現したものではない

---

## 11. 参照した出典の一覧

| 内容 | 出典 |
|---|---|
| 期待値を clone して `loadData()` で DB を読む | `src/main/java/nablarch/test/Assertion.java:79-83` |
| カラム比較ループは期待値の行ループの内側 | `src/main/java/nablarch/test/Assertion.java:263, 297-303` |
| 実データ側の余剰行の検出 | `src/main/java/nablarch/test/Assertion.java:306-314` |
| カラム名0件なら SQL を投げず0行を返す | `src/main/java/nablarch/test/core/db/TableData.java:337-346` |
| `getColumnNames()` の `null` フォールバック | `src/main/java/nablarch/test/core/db/TableData.java:501-506` |
| `getPrimaryKeys()` は `dbInfo` 由来 | `src/main/java/nablarch/test/core/db/TableData.java:480-482` |
| 初期化は削除→挿入 | `src/main/java/nablarch/test/core/db/TableData.java:117-120` |
| 削除は `DELETE FROM` のみ | `src/main/java/nablarch/test/core/db/TableData.java:127-130` |
| 挿入は `dbInfo` 由来のカラムで組み、0行ならループしない | `src/main/java/nablarch/test/core/db/TableData.java:139-143` |
| 識別子行の次の行を無条件にカラム名行として読む | `src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116` |
| 同じ書き方が `ListMapParser` にもある | `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` |
| 読む行が無ければカラム名0件になる | `src/main/java/nablarch/test/core/reader/HeaderLine.java:32-38` |
| 空行は読み込み時に除去される | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182` |
| `doParse` のループ構造 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:284-310` |
| `getDataType()` は前方一致 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:321-333`（判定は328行目） |
| 識別子行は `=` を含む | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:341-344` |
| `readLine()` は `index` を進めて返す | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:352-357` |
| `DataType.MESSAGE` の存在 | `src/main/java/nablarch/test/core/reader/DataType.java:44` |
| YAML はカラム名を `rows` の先頭要素から解決する | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:88` |
| 本事象の FIXME コメント | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:110-114` |
| 差し戻しの経緯と `@Ignore` した4件 | `nablarch-testing-yaml` コミット `190cc9a` |
