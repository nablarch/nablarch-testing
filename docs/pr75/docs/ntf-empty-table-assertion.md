# 期待値0件テーブル検証の偽陰性 — 対応方針

作成日: 2026-08-13 / 全面改訂: 2026-08-19

**位置づけ**: NTF 本体（`nablarch-testing`）への修正提案。`docs/pr75/steering.md` の #23・#24 の根拠となる文書であり、この文書が正である。

**出典の書き方**: 事実は実物のソースで確認したものに限り、「リポジトリ相対パス:行番号」で示す。リポジトリ名の記載が無いものは `nablarch-testing`（本リポジトリ）を指す。実測値には実行日を添える。確認していないことは「未確認」と明記する。案件担当者の知見に基づく前提は、その旨を明記する。

---

## 1. 提案（結論）

**0件のテーブルブロックにカラム名は不要。形式を問わず統一する。** カラム名は「検証対象カラムの絞り込み」であり、行が0件なら絞り込む対象が存在しない。**カラム名を書いたか書かなかったかで検証結果が変わってはならない。**

この原則が2箇所で破れている。層の異なる別問題なので、2タスクに分けて本体を修正する。

| | #23（問題1） | #24（問題2） |
|---|---|---|
| 層 | `TableData`（**形式共通**） | 表形式リーダ（**Excel 等の行指向データ**） |
| 何が起きるか | 検証すべきものを検証しない（偽陰性） | テストデータの一部を捨て、別の意味に解釈する |
| 直す場所 | `TableData#loadData()` | `TestDataParsingTemplate` ＋ 各パーサの `onTargetTypeFound` |
| 後方互換影響 | 嘘の合格をしていたテストが落ちる（該当データは実測0件） | 実質ゼロ（該当データは現状すでに壊れて読まれている） |

**順序は #23 → #24 で固定。逆順は禁止**（理由は 5.3）。

**現行動作へ戻すための設定は設けない**（理由は 6.2）。

**後方互換影響は極小**である。その根拠が本書の中心であり、3章にまとめた。

### 1.1 不具合か、仕様変更か

**両方とも不具合である。** 解説書チームへ回答した事象1〜4と同じ枠組みで判定した。

| | 判定 | 根拠 |
|---|---|---|
| **問題1** | **不具合** | 公開中の解説書は `EXPECTED_TABLE` を「テスト実行後の期待するデータベースのデータ」と定義しており（`06_TestFWGuide/01_Abstract.rst:275-276`）、**検証を行わない条件はどこにも書かれていない**。「カラム名が0件なら DB を読まない」は実装都合であって仕様ではない。テストが嘘の合格を返すのは、テスティングフレームワークの目的に反する |
| **問題2** | **不具合** | 公開中の解説書は「1行目：識別子行 / 2行目：カラム名 / 3行目～：データ行」と構成を定義するのみで（`02_DbAccessTest.rst:110-117, 228-235`）、**カラム名行を省略したときに次のブロックを食う挙動は書かれていない**。テストデータが黙って消え、別テーブルのデータとして解釈されるのは設計された挙動ではない |

**ただし #24 には「記法の拡張」が付随する。** 問題2を直すと、これまで書けなかった「カラム名行を省略した0件ブロック」という**新しい書き方が可能になる**。これは不具合修正の結果として生じる記法の追加であり、既存の書き方（カラム名行を書く）は一切変わらない。

**「カラム名の行は省略できない」という明文は未リリースである。** `ntf-yaml-support` ブランチに 2026-08-14 に追加された記述（`testdata_notation.rst:802`）は、解説書の作り直しの過程で現行挙動＝問題2をそのまま書き取ったものであり、公開済みの解説書には存在しない。**リリース前に直せるため、公開済み仕様の変更にはならない**（8章）。


---

## 2. 前提 — なぜ後方互換にこだわるか

**案件担当者の前提（2026-08-19 の議論による。以下2点は走査による裏付けを取っていない）**:

- 利用PJは規模の大きい基幹システムであり、NTF のテストデータ量が多い。とくにテーブル周りはテストデータが膨大である
- したがって、本体変更による後方互換影響は出したくない。既存テストが落ちる形の変更は受け入れにくい

**対象PJの運用**: テストデータを YAML で生成し（AI がテストデータを生成するため）、YAML→Excel 変換を通して **Excel で NTF を使う**。したがって YAML 経路と Excel 経路の両方が関わる。

この前提のもとで、影響を出さないために「設定で現行動作を残す」案まで検討した。最終的に採らない判断に至っている（6.2）。

---

## 3. 後方互換影響が極小である理由

### 3.1 問題2があるため、問題1に該当するデータは書けていない

Excel で問題1（`columnNames.length == 0`）に到達する経路は、**2つしかない**。

| 到達経路 | 根拠 |
|---|---|
| 識別子行がシート末尾にあり、次の行が無い | `readLine()` が返さず `HeaderLine` の `keys` が空になる（`src/main/java/nablarch/test/core/reader/HeaderLine.java:32-38`） |
| ヘッダ行がマーカーカラム（`[ ]`）のみ | `getEffectiveColumnNames()` がマーカーカラムを除外するため長さ0になる（`HeaderLine.java:39-40, 47-50, 85-91`） |

**「識別子行を2行続けて書く」——0件テーブルを書こうとしたときの最も自然な書き方は、問題1に到達しない。** 次の識別子行がカラム名行として消費され、`columnNames` は長さ1（例: `["EXPECTED_TABLE=NEXT_T"]`）になるからである（`src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116`）。代わりに問題2が起き、後続ブロックが消える。

つまり、**問題2があるせいで、問題1に該当する書き方が事実上封じられている。** 書けば別のバグに当たるため、問題1に当たるデータは利用PJに蓄積されていない。動いているように見えているものは、この2つのバグが打ち消し合っている状態にすぎない。

**なお「記法違反だから存在しない」とは言い切れない。** 公開中の解説書（`nablarch-document` `origin/main`）は「1行目：`SETUP_TABLE=<テーブル名>` / 2行目：そのテーブルのカラム名 / 3行目～：登録するレコード」と構成を定義しているだけで（`ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/02_DbAccessTest.rst:110-117, 228-235`）、**カラム名行を省略した場合の扱いは書かれていない**。「データ行を書かない場合でも、カラム名の行は省略できない」と明記した文は `ntf-yaml-support` ブランチに 2026-08-14 に追加されたもので（`testdata_notation.rst:802`、`b75f1d7`）、**未リリースである**。したがって該当データが無いことの根拠は、記法ではなく上記の機械的な理由と 3.2 の実測による。

### 3.2 実測 — 本リポジトリに該当は0件（2026-08-19）

`src/test` 配下の `.xls`/`.xlsx` 全件を Apache POI で走査した。空行を除去したうえで、テーブル系識別子行（`SETUP_TABLE=`／`EXPECTED_TABLE=`／`EXPECTED_COMPLETE_TABLE=`／`LIST_MAP=`）の直後の行を判定した。

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
| ヘッダ行の先頭カラムが DataType 名で始まる（`MESSAGE_ID` 型の罠。5.2 参照） | **0** |

マーカーカラムのみの14件はすべて `LIST_MAP` であり、`ListMapParser` 経由で `TableData` を生成しないため `loadData()` に到達しない。

**→ 本リポジトリに、問題1・問題2の修正で挙動が変わる既存テストデータは存在しない。**

走査プログラムは scratchpad に置いたのみでリポジトリには残していない。再実行が必要な場合は上記の判定条件で書き直すこと。

### 3.3 影響面はさらに狭い

`loadData()` のプロダクションコードからの呼び出し元は `src/main/java/nablarch/test/Assertion.java:81` の1箇所のみで、`assertTableEquals(String, TableData)` からしか呼ばれない（`grep -rn "loadData()" src/main` で確認）。期待値検証の入口は `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362` の1本で、`TestShot`・`AbstractHttpRequestTestTemplate` もここを通る。

| 経路 | 影響 |
|---|---|
| 期待値検証（`assertTableEquals`） | **対象** |
| 準備データ投入・マスタデータ投入 | `loadData()` を通らない。影響なし |
| 変換ツール（converter）の読み込み経路 | `loadData()` を通らない。影響なし |
| `LIST_MAP` | `TableData` を作らない。影響なし |
| カラム名を1つでも書いているテーブル | 分岐条件は `colNames.length == 0` のみ（`src/main/java/nablarch/test/core/db/TableData.java:343`）。影響なし |

### 3.4 影響が出るとしたら、どう出るか

- **DB が実際に空だった** → PASS のまま。影響なし
- **DB に行が残っていた** → 新たに FAIL する。これは検証されていなかったものが検証されるようになった結果であり、意図したアサートの失敗である
- **テーブル名を誤記していた** → 新たに例外になる。現状の0件カラム経路は `dbInfo` に触れずに return するため誤記でも PASS するが、修正後は `getPrimaryKeys()` → `dbInfo.getPrimaryKeys(tableName)`（`TableData.java:480-482`）に到達する

### 3.5 未走査の範囲

**対象PJのテストデータは未走査である。** 本リポジトリの結果をもって対象PJに該当が無いとは言えない。3.1 の論理（問題2があるため書けない）は形式上の制約なので対象PJにも当てはまるが、機械での確認は行っていない。

---

## 4. 何が起きているか

### 4.1 なぜ「0件にカラム名は不要」と言えるか

**0件のとき、テストデータに書かれたカラム名はどこからも参照されない。**

**期待値（`EXPECTED_TABLE`）が0行のとき** — `Assertion.java:256` で `expected.getColumnNames()` を取得するが、これを使うカラム比較ループは期待値の行ループの内側にある（`Assertion.java:297-303`）。行数0なら1度も回らない。実データ側の余剰行の検出を担う `dbDataFound` の走査（`Assertion.java:306-314`）はカラム名を使わない。PK 突合に使う `getPrimaryKeys()` は `dbInfo` 由来である（`TableData.java:480-482`）。

**準備データ（`SETUP_TABLE`）が0行のとき** — `deleteData` はテーブル名のみを使う（`TableData.java:127-130`）。`insertData` の INSERT 文は `getNonComputedColumns()`＝`dbInfo` 由来で組み立てられ（`TableData.java:139-141`）、`contents` が0件なのでバインドループが回らない（`TableData.java:143`）。

| 形式 | カラム名の要否 | 理由 |
|---|---|---|
| 仕様（データモデル） | **不要** | 0件では参照されない。カラム名は DB スキーマが持つ情報 |
| YAML | **不要**（書く場所が無い） | `rows: []` が0件表明として成立する。構造でブロックが区切られる |
| Excel | **不要** | 識別子行の並びから0件ブロックであることを判別できる（4.3） |

### 4.2 問題1 — カラム名0件だと SQL を発行しない（形式共通）

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

期待値0行・カラム名0件のとき、DB を読まずに「実データも0行」として扱うため、**DB に行が残っていても検証が必ず PASS する。**

**なぜ誤りか** — ここは「カラム名が0件」という*テストデータの書かれ方*を、「DB を読まなくてよい」という*検証の実施可否*に読み替えている。カラム名が無いのは「**何を比較するか**の情報が無い」だけで、「**比較しなくてよい**」ことは意味しない。

「カラム名が無いと SELECT が組めない」も成立しない。カラム名は DB スキーマが持っており、`TableData` は `dbInfo` を保持している。実際、`getColumnNames()` は `columnNames == null` のとき `dbInfo.getColumns(tableName)` にフォールバックする（`TableData.java:501-506`）。**情報源は既にあり、長さ0の配列のときだけそこに繋がっていない。**

### 4.3 問題2 — カラム名行として次の識別子行を食う（表形式リーダ）

`TableDataParser.java:107-116`

```java
void onTargetTypeFound(List<String> line) {
    // テーブル名
    String tableName = getTypeValue(line);
    // カラム名の行を読み込み
    header = new HeaderLine(readLine());
    ...
}
```

`readLine()` が返した行が本当にカラム名行なのかを検証していない。空行は `readTestData()` で既に除去済みなので（`src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182`）、ここで返るのは「次の非空行」である。同じ書き方が `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` にもある。`TableDataParser` 固有ではなく、表形式リーダ層に共通する構造である。

**(a) 次の行が別ブロックの識別子行のとき** — その行がカラム名として消費される。`doParse` のループはその次から再開し、`nowReading` は真のままなので、**次ブロックのデータ行が手前のテーブルのデータ行として取り込まれる**（`TestDataParsingTemplate.java:287-309`、`TableDataParser.java:97-100`）。次ブロックは消え、手前のテーブルは汚染される。**エラーにも警告にもならない。**

```
SETUP_TABLE=EMPTY_T     ← 0件にしたいテーブル
SETUP_TABLE=NEXT_T      ← 食われる（カラム名行にされる）
PK   NAME
1    foo
```

→ `EMPTY_T` のカラム名が `SETUP_TABLE=NEXT_T`、データ2行。**`NEXT_T` は消滅する。**

**(b) 次の行が無い（シート末尾）のとき** — `HeaderLine` が空になり（`HeaderLine.java:32-38`）、カラム名0件の `TableData` ができる。これは問題1に落ちる。

**なぜ誤りか** — **(a) は曖昧ではない。パーサは判別できる。** カラム名に `EXPECTED_TABLE=` のような文字列は現れない。`doParse` は既に全行に対して `getDataType()` を実行しており（`TestDataParsingTemplate.java:290`）、判定手段は最初から手元にある。問題は判定できないことではなく、`onTargetTypeFound` が判定せずに無条件で `readLine()` していることである。

なお、識別子行の次が**通常行**のとき、それが「カラム名行」なのか「カラム名を省略したデータ行」なのかは区別できない。ここは現状どおりカラム名行と解釈するしかないが、**0件ブロックにはデータ行が無いのでこの曖昧さに突き当たらない。**

---

## 5. 修正案

### 5.1 #23 — 問題1: 0件テーブルでも DB の実データを読む

| 項目 | 内容 |
|---|---|
| 修正箇所 | `TableData#loadData()`（`TableData.java:337-346`）の early return を削除する |
| 修正方法 | `colNames.length == 0` のとき `dbInfo.getColumns(tableName)` を SELECT 対象カラムとする。**修正は `loadData()` 内に閉じる** |
| 変更しないもの | `getColumnNames()` は変更しない |
| 再現テスト | DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を `Assertion.assertTableEquals` に渡し、現状では PASS してしまうことを示す |

**`getColumnNames()` を変更してはいけない。** 同等の変更を YAML 側で行って差し戻した経緯がある（`nablarch-testing-yaml` の revert コミット `190cc9a`。`getColumnNames()` を DB 参照に変えた結果、`StubDbInfo` を差す DB レスの変換ツール経路が `UnsupportedOperationException` で壊れた）。`loadData()` はもともと DB を読むメソッドなので、この問題を踏まない。

**修正後、検証は実際に効く。** 期待値の行ループが0回なのでカラム比較（`Assertion.java:296-303`）は回らないが、`dbDataFound` の走査が余剰行を検出して `an unexpected record is included in the table of [T]` で落ちる（`Assertion.java:306-314`）。PK 値も `dbInfo` 由来のカラムで SELECT した `contents` に入っているので `getPkValues`（`TableData.java:683-691`）は動く。

### 5.2 #24 — 問題2: 識別子行をカラム名行として消費しない

識別子行の次の行が:

| 次の行 | 扱い | 現状からの変更 |
|---|---|---|
| 通常行（`DataType.DEFAULT`） | カラム名行として消費する | なし |
| **識別子行** | **消費しない。カラム名0件ブロックとして確定し、その識別子行は次の反復で処理する** | **本修正** |
| 無い（シート末尾） | カラム名0件ブロックとして確定 | なし |

**エラーにする箇所は無い。** 「0件にカラム名は不要」という仕様と一貫する。

| 項目 | 内容 |
|---|---|
| 修正箇所 | `TestDataParsingTemplate` にヘッダ行読み込み用のヘルパを追加し、次行が識別子行の書式なら消費せずに `index` を巻き戻す（`TestDataParsingTemplate.java:352-357`）。`TableDataParser.java:111` と `ListMapParser.java:80` の `readLine()` をこのヘルパに置き換える |
| ループ側 | 変更不要。巻き戻された識別子行は次の反復で読まれ、同じデータタイプなら `onTargetTypeFound`、別のデータタイプなら `else` 節で `break` と、いずれも正しく処理される（`TestDataParsingTemplate.java:292-308`） |
| 再現テスト | 識別子行の直後に別の識別子行が続くテストデータを読ませ、現状では後続ブロックが結果に現れず、手前のテーブルにそのデータ行が混入することを示す |

**実装上の注意 — 判定は厳密にしないと既存を壊す。** 既存の `getDataType()` をそのままヘッダ行の判定に流用してはいけない。判定が前方一致だからである（`TestDataParsingTemplate.java:328`）。

```java
if (dataTypeCell.startsWith(type.getName())) {
```

`DataType` には `MESSAGE` がある（`src/main/java/nablarch/test/core/reader/DataType.java:44`）。したがって **`MESSAGE_ID` というカラム名は `DataType.MESSAGE` と判定される。** 先頭カラムが `MESSAGE_ID` のテーブルのヘッダ行を「識別子行だから消費しない」と扱うと、そのテーブルはカラム名0件になり、ヘッダ行は `else` 節で `break` されて**ブロックが壊れる**。

判定は識別子の書式そのもの、すなわち **`TYPE=` または `TYPE[groupId]=`** に限定する。`getTypeValue` が `indexOf('=')` を前提にしていること（`TestDataParsingTemplate.java:342-343`）からも、識別子行は `=` を含む形で確定している。カラム名に `=` は現れない。

### 5.3 順序は #23 → #24 で固定。逆順は禁止

**#24 を先に入れると、新たな偽陰性を作る。** これまで書けなかった「識別子行だけの0件ブロック」が書けるようになり、それが問題1に落ちて全部 PASS するからである。

| 単独で入れると | 結果 |
|---|---|
| **#23 のみ** | YAML `rows: []` の偽陰性が解消する。`nablarch-testing-yaml` の `@Ignore` 4件が外せる。Excel は現行記法のまま（0件テーブルは書けない）→ **安全** |
| **#24 のみ** | Excel で0件テーブルが書けるようになるが、全部 PASS する → **入れてはいけない** |

### 5.4 それぞれが解くもの

| 目的 | 必要な対応 |
|---|---|
| YAML を直接読んで検証する（`rows: []` を効かせる） | **#23 のみ**（`YamlTableDataBuilder` は表形式リーダを通らない） |
| Excel で0件テーブルを素直に書けるようにする | #23 ＋ #24 |
| converter の変換制約（0件ブロックを Excel へ書き出せない）を解く | #23 ＋ #24 |

converter の制約とは、`nablarch-testing-converter` の `XlsFormatWriter.java:233-240` にある番人である。カラム名を1件も持たないブロックを書き出そうとすると `IllegalArgumentException` を投げる。#24 が入れば、この番人を外して「識別子行だけ」を書き出せるようになる。

---

## 6. 検討した代替案と採否

### 6.1 採らなかった案

| 案 | 採否理由 |
|---|---|
| **YAML 形式のときだけ直す** | 形式によって検証の挙動が変わる。`TableData` は読み込み元の形式を保持していない（コンストラクタは `dbInfo`/`tableName`/`columnNames`/`defaultValues` のみ。`TableData.java:71-75, 85`）ため、現構造とも噛み合わない。Excel 側の事象も残る |
| **YAML に `columns:` フィールドを追加する** | カラム名は DB スキーマが持つ情報であり、テストデータに二重に持たせるものではない。YAML はデータモデルをそのまま表す形式であり、現在のスキーマがあるべき姿である |
| **問題2 で識別子行を食うケースをエラーにする** | 曖昧ではなく判別できるので、エラーにする理由が無い。「0件にカラム名は不要」という仕様と矛盾する |
| **問題2 でカラム名行が無い（シート末尾）ケースをエラーにする** | 現在動作している「識別子行のみでテーブルをクリアする」書き方を壊す。かつ問題1を直せば偽陰性は消えるので不要 |
| **「明示的に0件と宣言されたテーブル」にだけカラムを補う（フラグ案）** | 後方互換は完全に不変にできる（`TableData` に「明示的0件宣言」フラグを持たせ、YAML の `rows: []` と #24 修正後の Excel でだけ立てる）。しかし既存 Excel の「シート末尾に `EXPECTED_TABLE=X` だけ書く」を素通りさせ続ける判断になり、**嘘の合格を返す経路を仕様として恒久化する**。実測で払うべき互換コストが観測できていない（3.2）ため、採らない |
| **#24 を見送り、converter のマーカーカラム出力で代替する** | マーカーカラム案（7章）が救うのは converter が生成した Excel だけであり、手書き Excel で識別子行が連続した場合に後続ブロックが消える事象（4.3(a)）は残る。またマーカーカラム行というノイズが記法に残り続ける。**恒久対応としては #24 が必要** |

### 6.2 設定フラグを設けない理由

一度は「設定で現行動作を残す（デフォルト無効のオプトイン）」案を検討した。デフォルトを現行動作にすれば既存PJの挙動は1行も変わらず、「嘘の合格を維持する設定」ではなく「0件テーブルの厳格検証を有効にする設定」というオプトインの機能追加として成立する、という整理だった。

**この案は採らない。** 判断の根拠は次の2点である（案件担当者の判断・2026-08-19）。

1. **問題2があるため、カラム名行を書かないブロックは利用PJで成立していない**（3.1）。動いているように見えているものは2つのバグが打ち消し合っているだけであり、利用PJ側で見直すべき書き方である
2. **したがって、問題1の修正で NG になるテストは、利用PJ側が見直す対象である。** 設定で温存することは、その見直しを先送りするだけになる

加えて、設定を残すと「0件テーブルの検証が効くかどうかが設定に依存する」という状態が恒久化し、テスティングフレームワークとして筋が悪い。実測でも本リポジトリの該当は0件である（3.2）。

---

## 7. 本体リリースを待たない暫定策（対象PJ向け）

対象PJは、本体の #23・#24 を待たずに `rows: []` 相当を使える。**本体クラスの上書きは不要で、本体対応が入ったら順に外せる形になっている。**

### 7.1 問題2の回避 — converter がマーカーカラム行を出す

0件ブロックを Excel へ書き出すとき、識別子行の次に**マーカーカラム（`[ ]`）のみのヘッダ行**を1行出す。

```
SETUP_TABLE=EMPTY_T
[空]                    ← これが身代わりに食われる
SETUP_TABLE=NEXT_T      ← 無傷
PK   NAME
1    foo
```

食われるべき行がそこにあるので次のブロックは無事であり、マーカーカラムは本体が「比較対象外の列」として除外するため（`HeaderLine.java:39-40, 85-91`）、`EMPTY_T` はカラム名0件・0行のテーブルとして読まれる。

**実測（2026-08-19）**: 本体の `TableDataParser` に行リストを直接流して測定した（`DataType.SETUP_TABLE_DATA`／インメモリ `TestDataReader`／スタブ `DbInfo`）。

| 入力 | 結果 |
|---|---|
| 識別子行のみ（現状の問題2） | ブロック **2→1件**。`table=EMPTY_T columnNames=[SETUP_TABLE=NEXT_T] rows=2` |
| 識別子行＋マーカーカラムのみのヘッダ行 | ブロック **2件**。`table=EMPTY_T columnNames=[] rows=0`／`table=NEXT_T columnNames=[PK, NAME] rows=1` |
| 通常の2ブロック（対照） | ブロック 2件。正常 |

マーカーカラムの判定は `startsWith("[") && endsWith("]")` のみである（`HeaderLine.java:85-91`）。本リポジトリの既存データにも同じ形が14件あり実際に動いている（3.2）。

**この案は記法に反しない。** 公開中の解説書が定める「2行目はそのテーブルのカラム名」（`02_DbAccessTest.rst:110-117`）に対し、マーカーカラム行はカラム名行として実在するため、記法の変更は不要である。未リリースの `testdata_notation.rst:802`（「カラム名の行は省略できない」）にも反しない。

converter 側では `XlsFormatWriter.java:233-240` の番人を、例外ではなくマーカーカラム行の出力に置き換えることになる。

### 7.2 問題1の回避 — PJ 側で `testDataParser` を包む

`TestDataParser` は `SystemRepository.get("testDataParser")` で解決される（`src/main/java/nablarch/test/TestSupport.java:404`）。PJ 側で `BasicTestDataParser` を包む実装を登録し、`getExpectedTableData` の戻り値を後処理すればよい。

```java
public class ColumnFillingTestDataParser implements TestDataParser {
    private TestDataParser delegate;   // BasicTestDataParser を注入
    private DbInfo dbInfo;             // PJ の設定に既にあるものを流用
    private DefaultValues defaultValues;

    @Override
    public List<TableData> getExpectedTableData(String path, String resource, String groupId) {
        List<TableData> src = delegate.getExpectedTableData(path, resource, groupId);
        List<TableData> result = new ArrayList<TableData>(src.size());
        for (TableData td : src) {
            if (td.getColumnNames().length == 0) {
                // カラム名を DB のカラムで補う。行は 0 件のまま
                result.add(new TableData(dbInfo, td.getTableName(),
                                         dbInfo.getColumns(td.getTableName()), defaultValues));
            } else {
                result.add(td);
            }
        }
        return result;
    }
    // 他のメソッドは delegate へそのまま委譲
}
```

成立する根拠:

- `TableData` のコンストラクタは public（`TableData.java:71, 85`）
- `DbInfo` は PJ のコンポーネント設定に既にある（`BasicTestDataParser#setDbInfo`、`src/main/java/nablarch/test/core/reader/BasicTestDataParser.java:221`）
- 期待値検証の入口は `DbAccessTestSupport#assertTableEquals` → `TestSupport#getExpectedTableData` の1本（`DbAccessTestSupport.java:362`、`TestSupport.java:278-281`）。`TestShot` も `AbstractHttpRequestTestTemplate` もここを通る
- 準備データ・マスタデータは `loadData()` を通らないので包む必要がない（3.3）

**問題2はPJ側では実質対応できない。** `onTargetTypeFound` は package-private（`TestDataParsingTemplate.java:63`）、巻き戻しに使う `index` は private（`TestDataParsingTemplate.java:40`）であるため、7.1 の converter 側対応で回避する。

### 7.3 本体対応が入った後

| | 暫定策 | #23・#24 投入後 |
|---|---|---|
| 問題2 | converter がマーカーカラム行を出す | converter は「識別子行だけ」を書き出すよう切り替える。マーカーカラムのノイズが消える |
| 問題1 | PJ が `testDataParser` の wrapper を登録する | wrapper を設定から外すだけ |

---

## 8. スコープ外の申し送り

| 項目 | 対応先 | 前提 |
|---|---|---|
| `@Ignore` 4件の解除と `YamlTableDataBuilder.java:110-114` の FIXME 削除 | `nablarch-testing-yaml`（別リポジトリ・別セッション） | #23 が本体に入り `install` された後でないと検証できない |
| マーカーカラム行の出力対応（`XlsFormatWriter.java:233-240` の番人の置き換え） | `nablarch-testing-converter` | 暫定策として #24 より先に必要。#24 投入後は「識別子行だけ」へ切り替え |
| 解説書の記述更新 | `nablarch-document` | 9章 |

`nablarch-testing-yaml` で `@Ignore` されている4件（revert コミット `190cc9a` のメッセージに記載）:

- `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:399`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`（同 `yaml/YamlTableDataBuilderTest.java:147`）
- `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`（同 `:421`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`（同 `:871`）

### 解説書（`nablarch-document`）への反映

- **`testdata_notation.rst:802` は #24 の投入に合わせて書き直す。** この文（「データ行を書かない場合でも、カラム名の行は省略できない。識別子行の次の行がカラム名の行として読み込まれるため…」）は 2026-08-14 に `ntf-yaml-support` ブランチへ追加されたもので**未リリース**であり、現行挙動＝問題2をそのまま記述したものである。#24 が入れば「**データ行が0件の場合に限り**カラム名の行を省略できる。省略した場合は0件のテーブルとして扱われる」に置き換わる。**リリース前に直せるため、公開済み記述の訂正にはならない**
- 公開中の解説書には、カラム名行を省略した場合の扱いに関する記述が無い（`02_DbAccessTest.rst:110-117, 228-235` は「2行目はカラム名」と定義するのみ）。#23・#24 の投入は、公開済みの記述と矛盾しない
- **YAML の `rows: []` が0件検証として有効**である旨を明記する。`ja/development_tools/` 配下の rst に `rows: []` の記述は無い（未確認: この件数は再確認していない）
- 公式の記載例はいずれもカラム名行を書いているため、**例の変更は不要**

---

## 9. 未確認事項

- **対象PJのテストデータは未走査**（3.5）。本リポジトリ以外での該当パターンの出現頻度は確認していない
- **カラム名0件かつ行が1件以上ある期待値**（Excel でマーカーカラムのみのヘッダにデータ行が続く形）の #23 修正後の挙動。現状は実データ0行扱いで PK 不一致 fail、修正後は `expected.getValue()` 側に値が無く別の失敗の仕方になる可能性がある
- **先頭カラム名がデータタイプ名で始まるテーブル**（`MESSAGE_ID` 等）が既存テストデータに実在するか。本リポジトリの xls 走査では0件（3.2）だが、対象PJは未確認
- **実 `.xlsx` を通した経路**。7.1 の実測はパーサ層に行リストを直接流したものであり、`XlsReader` の空セル処理を挟んだ往復では実行していない
- **Excel→YAML の逆変換**でマーカーカラム行が `rows: []` に戻るか。converter 側の `XlsFormatReader` と XLS-08 の正規化が絡む。未確認
- **マーカーカラムのセルに何を書くかの取り決め**（`[空]` など）。未決

---

## 10. 参照した出典の一覧

| 内容 | 出典 |
|---|---|
| 期待値を clone して `loadData()` で DB を読む | `src/main/java/nablarch/test/Assertion.java:79-83` |
| カラム比較ループは期待値の行ループの内側 | `src/main/java/nablarch/test/Assertion.java:263, 297-303` |
| 実データ側の余剰行の検出 | `src/main/java/nablarch/test/Assertion.java:306-314` |
| カラム名0件なら SQL を投げず0行を返す | `src/main/java/nablarch/test/core/db/TableData.java:337-346`（分岐は343行目） |
| `getColumnNames()` の `null` フォールバック | `src/main/java/nablarch/test/core/db/TableData.java:501-506` |
| `getPrimaryKeys()` は `dbInfo` 由来 | `src/main/java/nablarch/test/core/db/TableData.java:480-482` |
| `getPkValues` | `src/main/java/nablarch/test/core/db/TableData.java:683-691` |
| `TableData` のコンストラクタは public | `src/main/java/nablarch/test/core/db/TableData.java:71, 85` |
| 初期化は削除→挿入 | `src/main/java/nablarch/test/core/db/TableData.java:117-120` |
| 削除は `DELETE FROM` のみ | `src/main/java/nablarch/test/core/db/TableData.java:127-130` |
| 挿入は `dbInfo` 由来のカラムで組み、0行ならループしない | `src/main/java/nablarch/test/core/db/TableData.java:139-143` |
| 識別子行の次の行を無条件にカラム名行として読む | `src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116` |
| 同じ書き方が `ListMapParser` にもある | `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` |
| 読む行が無ければカラム名0件になる | `src/main/java/nablarch/test/core/reader/HeaderLine.java:32-38` |
| マーカーカラムを除外して有効カラム名を作る | `src/main/java/nablarch/test/core/reader/HeaderLine.java:39-40, 47-50` |
| マーカーカラムの判定は `[` 始まり `]` 終わり | `src/main/java/nablarch/test/core/reader/HeaderLine.java:85-91` |
| 空行は読み込み時に除去される | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182` |
| `onTargetTypeFound` は package-private／`index` は private | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:63, 40` |
| `doParse` のループ構造 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:284-310` |
| `getDataType()` は前方一致 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:321-333`（判定は328行目） |
| 識別子行は `=` を含む | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:341-344` |
| `readLine()` は `index` を進めて返す | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:352-357` |
| `DataType.MESSAGE` の存在 | `src/main/java/nablarch/test/core/reader/DataType.java:44` |
| 期待値検証の入口 | `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362`／`src/main/java/nablarch/test/TestSupport.java:278-281` |
| `testDataParser` は `SystemRepository` 解決 | `src/main/java/nablarch/test/TestSupport.java:404` |
| `BasicTestDataParser#setDbInfo` | `src/main/java/nablarch/test/core/reader/BasicTestDataParser.java:221` |
| YAML はカラム名を `rows` の先頭要素から解決する | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:88` |
| 本事象の FIXME コメント | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:110-114` |
| 差し戻しの経緯と `@Ignore` した4件 | `nablarch-testing-yaml` コミット `190cc9a` |
| カラム名0件ブロックを書き出せない番人 | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/xls/XlsFormatWriter.java:233-240` |
| 公開中の記法定義（1行目=識別子行／2行目=カラム名／3行目～=データ行） | `nablarch-document` `origin/main` `ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/02_DbAccessTest.rst:110-117, 228-235` |
| 「カラム名の行は省略できない」（**未リリース**。`origin/main` に同記述なし） | `nablarch-document`（`ntf-yaml-support`）`testdata_notation.rst:802`（`b75f1d7`・2026-08-14 追加） |
| `EXPECTED_TABLE` の定義（テスト実行後の期待するデータベースのデータ） | `nablarch-document` `origin/main` `06_TestFWGuide/01_Abstract.rst:275-276` |
