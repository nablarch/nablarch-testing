# 期待値0件テーブル検証の偽陰性 — 対応方針

作成日: 2026-08-13 / 全面改訂: 2026-08-19

**位置づけ**: NTF 本体（`nablarch-testing`）への修正提案。`docs/pr75/steering.md` の #23 の根拠となる文書であり、この文書が正である。関連して `nablarch-testing-converter` への申し送りを含む（8章）。

**出典の書き方**: 事実は実物のソースで確認したものに限り、「リポジトリ相対パス:行番号」で示す。リポジトリ名の記載が無いものは `nablarch-testing`（本リポジトリ）を指す。実測値には実行日を添える。確認していないことは「未確認」と明記する。案件担当者の知見に基づく前提は、その旨を明記する。

---

## 1. 提案（結論）

**本体（`nablarch-testing`）を直す箇所は1つだけである。** `TableData#loadData()` が、カラム名0件のとき DB を読まずに0行を返す点（問題1）。これが YAML の `rows: []`（0件テーブル）を偽陰性にしている。

**Excel 形式の挙動（問題2）は直さない。** Excel 記法はデータ行が0件でもカラム名の行を書く。これは記載例・実データの双方が示す仕様であり（1.1）、記法どおりに書けば問題2 は発生しない。

| | #23（問題1） | 問題2 |
|---|---|---|
| 層 | `TableData`（**形式共通**） | 表形式リーダ（Excel 等の行指向データ） |
| 何が起きるか | 検証すべきものを検証しない（偽陰性） | 記法違反のデータを黙って別の意味に読む |
| 判定 | **不具合。本体を修正する** | **不具合ではない。本体は修正しない**（1.1） |
| 直す場所 | `TableData#loadData()` | — |
| 後方互換影響 | 実測で該当0件（3章） | なし（変更しない） |

**converter（`nablarch-testing-converter`）側に1件の対応が要る。** 中間モデルのカラム名が0件のとき、Excel 書き出しで**マーカーカラム（`[ ]`）だけのカラム名行**を出す。現在は例外で変換を中止している。これは Excel 記法に反しない対応であり、#23 と組み合わせて0件テーブルの検証が成立する（5.2）。

**後方互換影響は極小**である。その根拠が本書の中心であり、3章にまとめた。

**現行動作へ戻すための設定は設けない**（理由は 6.2）。

### 1.1 不具合か、仕様変更か

| | 判定 | 根拠 |
|---|---|---|
| **問題1** | **不具合** | 公開中の解説書は `EXPECTED_TABLE` を「テスト実行後の期待するデータベースのデータ」と定義しており（`nablarch-document` `origin/main` `06_TestFWGuide/01_Abstract.rst:275-276`）、**検証を行わない条件はどこにも書かれていない**。「カラム名が0件なら DB を読まない」は実装都合であって仕様ではない。テストが嘘の合格を返すのは、テスティングフレームワークの目的に反する |
| **問題2** | **不具合ではない** | Excel 記法では**データ行が0件でもカラム名の行を書く**。記載例がそうなっており、本リポジトリの実データもそうなっている（下記）。記法どおりに書けば識別子行が連続することはなく、問題2 は発生しえない |

**Excel 記法がそうであることの根拠**:

- **記載例** — `nablarch-document`（`ntf-yaml-support`）`testdata_examples.rst` の「0件のテーブルデータを記述する」は、`SETUP_TABLE=ORDER_HEADER` / `ORDER_ID ITEM_COUNT STATUS` / （データ行なし）という形を示し、`EXPECTED_TABLE` も同様である。本文に「いずれもカラム名の行までを記述し、データ行を記述していない。」とある（`b75f1d7`・2026-08-14 追加）
- **実データ** — 本リポジトリの `.xls` 走査（2026-08-19）で、カラム名行を書いてデータ行が0件のブロックが**5件**実在する（3.2）。いずれも `columnNames.length >= 1` であり、問題1 にも当たらず正しく動いている
- **公開中の解説書** — 「1行目：`SETUP_TABLE=<テーブル名>` / 2行目：そのテーブルのカラム名 / 3行目～：登録するレコード」と構成を定義している（`origin/main` `06_TestFWGuide/02_DbAccessTest.rst:110-117, 228-235`）

**では、なぜ YAML では本体の変更が要るのか。** YAML は表ではなく NTF のデータモデルを表す形式であり、カラム名は行の並びから決まる（`nablarch-testing-yaml` `YamlSection.java:145-150`。先頭行のキーがカラム名）。行が0件なら**カラム名を書く場所が構造上存在しない**。カラム名の行は表形式（Excel）の記法要素であって、データモデルの構成要素ではない。したがって、

- **Excel**: 表なのでカラム名の行が構造上必要。**現状で正しい**
- **YAML**: モデルなので `rows: []` だけで0件を表現できる。**本体が対応すべきはここだけ**

**「カラム名の行は省略できない」という明文は未リリースだが、記法の追加ではなく現行仕様の明文化である。** `ntf-yaml-support` ブランチに 2026-08-14 に追加された記述（`testdata_notation.rst:802`）は `origin/main` には存在しないが、上記の記載例・実データと一致しており、現行の Excel 記法をそのまま書き取ったものである。#23 はこの記述に触れない。

---

## 2. 前提 — なぜ後方互換にこだわるか

**案件担当者の前提（2026-08-19 の議論による。以下2点は走査による裏付けを取っていない）**:

- 利用PJは規模の大きい基幹システムであり、NTF のテストデータ量が多い。とくにテーブル周りはテストデータが膨大である
- したがって、本体変更による後方互換影響は出したくない。既存テストが落ちる形の変更は受け入れにくい

**対象PJの運用**: テストデータを YAML で生成し（AI がテストデータを生成するため）、converter を通して **Excel で NTF を使う**。したがって YAML 経路と Excel 経路の両方が関わる。

この前提のもとで、影響を出さないために「設定で現行動作を残す」案まで検討した。最終的に採らない判断に至っている（6.2）。

---

## 3. 後方互換影響が極小である理由

### 3.1 Excel 記法どおりに書かれたデータは、#23 の分岐に入らない

#23 が変えるのは `colNames.length == 0` のときの挙動だけである（`src/main/java/nablarch/test/core/db/TableData.java:343`）。Excel 経路でここに到達する経路は、**2つしかない**。

| 到達経路 | 根拠 | 記法との関係 |
|---|---|---|
| 識別子行がシート末尾にあり、次の行が無い | `readLine()` が返さず `HeaderLine` の `keys` が空になる（`src/main/java/nablarch/test/core/reader/HeaderLine.java:32-38`） | カラム名の行が無い＝**記法違反** |
| ヘッダ行がマーカーカラム（`[ ]`）のみ | `getEffectiveColumnNames()` がマーカーカラムを除外するため長さ0になる（`HeaderLine.java:39-40, 47-50, 85-91`） | カラム名の行は存在するので記法どおり。ただし DB 操作対象カラムが1つも無い |

**記載例どおりに書いた0件テーブル（カラム名行あり・データ行0件）は、`columnNames.length >= 1` なので分岐に入らない。** 挙動は1ミリも変わらない。

**「識別子行を2行続けて書く」も #23 には到達しない。** 次の識別子行がカラム名行として消費され、`columnNames` は長さ1（例: `["SETUP_TABLE=NEXT_T"]`）になるからである（`src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116`）。この書き方は記法違反であり、現状は後続ブロックが消える（4.3）。**#23 でも挙動は変わらない。**

### 3.2 実測 — 本リポジトリに該当は0件（2026-08-19）

`src/test` 配下の `.xls`/`.xlsx` 全件を Apache POI で走査した。空行を除去したうえで、テーブル系識別子行（`SETUP_TABLE=`／`EXPECTED_TABLE=`／`EXPECTED_COMPLETE_TABLE=`／`LIST_MAP=`）の直後の行を判定した。

| 走査対象 | 件数 |
|---|---|
| ファイル | 59 |
| シート | 242 |
| テーブル系識別子行 | 324 |

| 検出した形 | 件数 | #23 の影響 |
|---|---|---|
| 識別子行の直後が識別子行（記法違反） | **0** | — |
| 識別子行がシート末尾（カラム名行なし・記法違反） | **0** | — |
| ヘッダ行がマーカーカラムのみ | 14 | すべて `LIST_MAP`。`ListMapParser` 経由で `TableData` を生成しないため `loadData()` に到達しない。**なし** |
| **カラム名行あり・データ行0件（記載例どおりの0件テーブル）** | **5** | `columnNames.length >= 1` で分岐に入らない。**なし** |
| ヘッダ行の先頭カラムが DataType 名で始まる（`MESSAGE_ID` 型の罠） | **0** | — |

カラム名行あり・データ行0件の5件（0件テーブルが実在することの証拠）:

| ファイル | シート | ブロック |
|---|---|---|
| `MessagingReceiveTestSupportTest.xls` | `testExtends` | `SETUP_TABLE=RECEIVE_TEST` |
| `MessagingReceiveTestSupportTest.xls` | `testUnExtends` | `SETUP_TABLE=RECEIVE_TEST` |
| `AbstractHttpRequestTestTemplateTest.xls` | `testGetEmptyTestCase` | `LIST_MAP=testCases` |
| `HttpRequestTestSupportTest.xls` | `testAssertObjectPropertyEquals2` | `LIST_MAP=nullValue` |
| `MessagingRequestTestSupportTest.xls` | `testMessagingSample` | `LIST_MAP[case2]=EXPECTED_LOG` |

**→ 本リポジトリに、#23 で挙動が変わる既存テストデータは存在しない。**

走査プログラムは scratchpad に置いたのみでリポジトリには残していない。再実行が必要な場合は上記の判定条件で書き直すこと。

### 3.3 影響面はさらに狭い

`loadData()` のプロダクションコードからの呼び出し元は `src/main/java/nablarch/test/Assertion.java:81` の1箇所のみで、`assertTableEquals(String, TableData)` からしか呼ばれない（`grep -rn "loadData()" src/main` で確認）。期待値検証の入口は `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362` の1本で、`TestShot`・`AbstractHttpRequestTestTemplate` もここを通る。

| 経路 | 影響 |
|---|---|
| 期待値検証（`assertTableEquals`） | **対象** |
| 準備データ投入・マスタデータ投入 | `loadData()` を通らない。影響なし |
| 変換ツール（converter）の読み込み経路 | `loadData()` を通らない。影響なし |
| `LIST_MAP` | `TableData` を作らない。影響なし |
| カラム名を1つでも書いているテーブル | 分岐条件は `colNames.length == 0` のみ（`TableData.java:343`）。影響なし |

### 3.4 影響が出るとしたら、どう出るか

- **DB が実際に空だった** → PASS のまま。影響なし
- **DB に行が残っていた** → 新たに FAIL する。これは検証されていなかったものが検証されるようになった結果であり、意図したアサートの失敗である
- **テーブル名を誤記していた** → 新たに例外になる。現状の0件カラム経路は `dbInfo` に触れずに return するため誤記でも PASS するが、修正後は `getPrimaryKeys()` → `dbInfo.getPrimaryKeys(tableName)`（`TableData.java:480-482`）に到達する

いずれも記法違反のデータでのみ起こる。実測では該当0件（3.2）。

### 3.5 未走査の範囲

**対象PJのテストデータは未走査である。** 本リポジトリの結果をもって対象PJに該当が無いとは言えない。記法どおりのデータは分岐に入らないという 3.1 の論理は形式上の制約なので対象PJにも当てはまるが、記法違反データの有無は機械で確認していない。

---

## 4. 何が起きているか

### 4.1 カラム名は表形式の記法要素であって、データモデルの構成要素ではない

**0件のとき、テストデータに書かれたカラム名はどこからも参照されない。**

**期待値（`EXPECTED_TABLE`）が0行のとき** — `Assertion.java:256` で `expected.getColumnNames()` を取得するが、これを使うカラム比較ループは期待値の行ループの内側にある（`Assertion.java:297-303`）。行数0なら1度も回らない。実データ側の余剰行の検出を担う `dbDataFound` の走査（`Assertion.java:306-314`）はカラム名を使わない。PK 突合に使う `getPrimaryKeys()` は `dbInfo` 由来である（`TableData.java:480-482`）。

**準備データ（`SETUP_TABLE`）が0行のとき** — `deleteData` はテーブル名のみを使う（`TableData.java:127-130`）。`insertData` の INSERT 文は `getNonComputedColumns()`＝`dbInfo` 由来で組み立てられ（`TableData.java:139-141`）、`contents` が0件なのでバインドループが回らない（`TableData.java:143`）。

つまり、**0件テーブルの意味を成り立たせるのにカラム名は要らない。** それでも Excel がカラム名の行を書くのは、表という記法がブロックの区切りをその行に頼っているからである。

| 形式 | カラム名の行 | 理由 |
|---|---|---|
| データモデル（仕様） | 概念として存在しない | 0件では参照されない。カラム名は DB スキーマが持つ情報 |
| Excel（表） | **必要**（記法どおり） | 識別子行の次の行がカラム名の行と決まっており、そこがブロックの区切りになる |
| YAML（モデル） | **書く場所が無い** | カラム名は `rows` の先頭要素のキーから決まる（`nablarch-testing-yaml` `YamlSection.java:145-150`）。`rows: []` にはキーが無い |

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

### 4.3 問題2 — 記法違反のデータを黙って別の意味に読む（本体では直さない）

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

`readLine()` が返した行が本当にカラム名行なのかを検証していない。空行は `readTestData()` で既に除去済みなので（`src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182`）、ここで返るのは「次の非空行」である。同じ書き方が `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` にもある。

**識別子行が連続していると、次ブロックが消える。**

```
SETUP_TABLE=EMPTY_T     ← カラム名の行を省略した（記法違反）
SETUP_TABLE=NEXT_T      ← 食われる（カラム名行にされる）
PK   NAME
1    foo
```

→ `EMPTY_T` のカラム名が `SETUP_TABLE=NEXT_T`、データ2行。**`NEXT_T` は消滅する。** `doParse` のループはその次から再開し、`nowReading` は真のままなので、次ブロックのデータ行が手前のテーブルのデータ行として取り込まれる（`TestDataParsingTemplate.java:287-309`、`TableDataParser.java:97-100`）。**エラーにも警告にもならない。**

**これは不具合ではない。** Excel 記法はカラム名の行を必須としており（1.1）、上の入力は記法違反である。記法どおりに書けば発生しない。

**本体を直さない判断の意味** — 記法違反のデータは今後も黙って壊れる。エラー化も行わない（理由は 6.1）。converter は既にこの形を書き出さないよう番人を置いており（`nablarch-testing-converter` `XlsFormatWriter.java:233-240`）、5.2 の対応後もカラム名の行を必ず出す。

---

## 5. 修正案

### 5.1 #23（本体 `nablarch-testing`）— 0件テーブルでも DB の実データを読む

| 項目 | 内容 |
|---|---|
| 修正箇所 | `TableData#loadData()`（`TableData.java:337-346`）の early return を削除する |
| 修正方法 | `colNames.length == 0` のとき `dbInfo.getColumns(tableName)` を SELECT 対象カラムとする。**修正は `loadData()` 内に閉じる** |
| 変更しないもの | `getColumnNames()` は変更しない |
| 再現テスト | DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を `Assertion.assertTableEquals` に渡し、現状では PASS してしまうことを示す |

**`getColumnNames()` を変更してはいけない。** 同等の変更を YAML 側で行って差し戻した経緯がある（`nablarch-testing-yaml` の revert コミット `190cc9a`。`getColumnNames()` を DB 参照に変えた結果、`StubDbInfo` を差す DB レスの変換ツール経路が `UnsupportedOperationException` で壊れた）。`loadData()` はもともと DB を読むメソッドなので、この問題を踏まない。

**修正後、検証は実際に効く。** 期待値の行ループが0回なのでカラム比較（`Assertion.java:296-303`）は回らないが、`dbDataFound` の走査が余剰行を検出して `an unexpected record is included in the table of [T]` で落ちる（`Assertion.java:306-314`）。PK 値も `dbInfo` 由来のカラムで SELECT した `contents` に入っているので `getPkValues`（`TableData.java:683-691`）は動く。

### 5.2 converter（`nablarch-testing-converter`）— Excel 書き出しでマーカーカラム行を出す

**現状** — 中間モデルのカラム名が0件のブロックを Excel へ書き出そうとすると `IllegalArgumentException` で変換を中止する（`XlsFormatWriter.java:233-240`）。YAML の `rows: []` はカラム名0件の中間モデルになるため、**0件テーブルを含む YAML は Excel へ変換できない**。実際に climan サンプルの `SETUP_TABLE=CLIENT` で変換が止まり、その状態がテストとして固定されている（`SampleConversionTest.java:65-82`）。

**対応** — 例外の代わりに、識別子行の次へ**マーカーカラム（`[ ]`）だけのカラム名行**を1行出力する。

```
SETUP_TABLE=EMPTY_T
[空]                    ← カラム名の行として実在する
SETUP_TABLE=NEXT_T      ← 無傷
PK   NAME
1    foo
```

読み戻すと、マーカーカラムは本体が「比較対象外の列」として除外するため（`HeaderLine.java:39-40, 85-91`）、`EMPTY_T` はカラム名0件・0行の `TableData` になる。#23 が入っていれば、そこから DB の全カラムで SELECT され**検証が効く**。カラム名の行が物理的に存在するので**次のブロックも無傷**であり、問題2 は起きない。

**実測（2026-08-19）**: 本体の `TableDataParser` に行リストを直接流して測定した（`DataType.SETUP_TABLE_DATA`／インメモリ `TestDataReader`／スタブ `DbInfo`）。

| 入力 | 結果 |
|---|---|
| 識別子行のみ（記法違反） | ブロック **2→1件**。`table=EMPTY_T columnNames=[SETUP_TABLE=NEXT_T] rows=2` |
| 識別子行＋マーカーカラムのみのヘッダ行 | ブロック **2件**。`table=EMPTY_T columnNames=[] rows=0`／`table=NEXT_T columnNames=[PK, NAME] rows=1` |
| 通常の2ブロック（対照） | ブロック 2件。正常 |

マーカーカラムの判定は `startsWith("[") && endsWith("]")` のみである（`HeaderLine.java:85-91`）。本リポジトリの既存データにも同じ形が14件あり実際に動いている（3.2）。

**この案は Excel 記法に反しない。** 公開中の解説書が定める「2行目はそのテーブルのカラム名」（`02_DbAccessTest.rst:110-117`）に対し、マーカーカラム行はカラム名行として実在する。未リリースの `testdata_notation.rst:802`（「カラム名の行は省略できない」）にも反しない。

**なぜ「YAML 出力時にマーカーを出す」では解けないか** — converter は Excel／YAML の双方を中間モデル経由で扱い、**どちらの入口でもマーカーカラムは中間モデルに入る前に除去される**。

| 経路 | 実装 | マーカーの扱い |
|---|---|---|
| 中間モデルの契約 | `model/TableDataBlock.java:43` | カラム名リストは「マーカーカラムを含む」 |
| YAML→中間モデル | `yaml/YamlFormatReader.java:154-170`（159・169） | `nablarch-testing-yaml` の `YamlTableDataBuilder.java:100-115`（105 で `isMarker` 除外・115 で `TableData` 生成）が返したカラム名をそのまま渡す。**除去される** |
| Excel→中間モデル | `xls/XlsFormatReader.java:149, 161` | 本体 `HeaderLine.java:41` が除外済みのカラム名をそのまま渡す。**除去される** |
| 中間モデル→YAML | `yaml/YamlFormatWriter.java:274-277` | 行0件なら `rows: []` のみ。カラム名は書かない |
| 中間モデル→Excel | `xls/XlsFormatWriter.java:233-240` | カラム名0件なら例外（本節で置き換える箇所） |

YAML にマーカーを書いても中間モデルのカラム名は0件のままなので、Excel 書き出し時の例外は消えない。**マーカーを出せるのは中間モデル→Excel の書き出しだけである。** 書き手側の下地は既にあり、マーカー位置を `BlockLayout` に記録して `Fill.MARKER` で塗る処理が実装済みである（`XlsFormatWriter.java:246-249`）。

**往復の可逆性は壊れない。** `RoundTripTest`（`src/test/java/nablarch/test/tool/converter/RoundTripTest.java:34-56`）が検証しているのは**中間モデル → 同一形式 → 中間モデル**の往復であり、XLS 経路・YAML 経路それぞれで独立に見ている。クロス形式（Excel→YAML→Excel）の一致は対象外である。

- XLS 経路: 中間モデル(カラム名0件) → Excel(`[空]`) → 中間モデル(カラム名0件)。**一致する**。現状は例外で往復できないため、可逆になる方向の変更である
- YAML 経路: 中間モデル(カラム名0件) → `rows: []` → 中間モデル(カラム名0件)。**現状も一致し、変わらない**

**マーカーカラムのセルに何を書くかは converter 側で決める**（`[空]` を想定。未決。9章）。

### 5.3 それぞれが解くもの

| 目的 | 必要な対応 |
|---|---|
| YAML を直接読んで検証する（`rows: []` を効かせる） | **#23 のみ**（`YamlTableDataBuilder` は表形式リーダを通らない） |
| YAML の0件テーブルを Excel へ変換できるようにする | **converter のマーカーカラム行出力**（5.2） |
| 変換した Excel で0件テーブルの検証を効かせる | **#23 ＋ converter**（マーカーカラムは本体が除外するのでカラム名0件になり、#23 が DB から補う） |
| 手書き Excel で0件テーブルを書く | **対応不要**。記載例どおりカラム名の行を書けばよい（1.1） |

**順序の制約は無い。** #23 と converter の対応は独立に入れられる。ただし converter の対応だけ先に入れた場合、生成された Excel の0件テーブルは #23 が入るまで偽陰性のまま（検証が素通り）になるため、**#23 を先に入れるのが望ましい**。

---

## 6. 検討した代替案と採否

### 6.1 採らなかった案

| 案 | 採否理由 |
|---|---|
| **問題2 を本体で直す（識別子行を消費せず、カラム名0件ブロックとして確定する）** | Excel 記法はカラム名の行を必須としており、問題2 は記法違反時にだけ起こる（1.1）。直すと「カラム名行を省略した0件ブロック」という**新しい書き方が Excel に生まれる**。記法を増やす変更であり、YAML 対応に必要でもない（5.3）。converter は記法どおりの Excel しか書かないため、対象PJの運用でも不要 |
| **問題2 をエラーにする** | 記法違反の検出という意味はあるが、リリース済みの読み込み挙動を変える。実測で該当0件（3.2）であり、払うべきコストに見合う便益が観測できていない。将来の課題として 8章に申し送る |
| **YAML 形式のときだけ問題1を直す** | 形式によって検証の挙動が変わる。`TableData` は読み込み元の形式を保持していない（コンストラクタは `dbInfo`/`tableName`/`columnNames`/`defaultValues` のみ。`TableData.java:71-75, 85`）ため、現構造とも噛み合わない |
| **YAML に `columns:` フィールドを追加する** | カラム名は DB スキーマが持つ情報であり、テストデータに二重に持たせるものではない。YAML はデータモデルをそのまま表す形式であり、現在のスキーマがあるべき姿である |
| **「明示的に0件と宣言されたテーブル」にだけカラムを補う（フラグ案）** | 後方互換は完全に不変にできる（`TableData` に「明示的0件宣言」フラグを持たせ、YAML の `rows: []` でだけ立てる）。しかし既存 Excel の「シート末尾に `EXPECTED_TABLE=X` だけ書く」を素通りさせ続ける判断になり、**嘘の合格を返す経路を仕様として恒久化する**。実測で払うべき互換コストが観測できていない（3.2）ため、採らない |

### 6.2 設定フラグを設けない理由

一度は「設定で現行動作を残す（デフォルト無効のオプトイン）」案を検討した。デフォルトを現行動作にすれば既存PJの挙動は1行も変わらず、「嘘の合格を維持する設定」ではなく「0件テーブルの厳格検証を有効にする設定」というオプトインの機能追加として成立する、という整理だった。

**この案は採らない。** 判断の根拠は次の2点である（案件担当者の判断・2026-08-19）。

1. **#23 の分岐に入るのは記法違反のデータだけである**（3.1）。記法どおりに書かれたデータは1件も挙動が変わらない
2. **したがって、#23 で NG になるテストは、利用PJ側が見直す対象である。** 設定で温存することは、その見直しを先送りするだけになる

加えて、設定を残すと「0件テーブルの検証が効くかどうかが設定に依存する」という状態が恒久化し、テスティングフレームワークとして筋が悪い。実測でも本リポジトリの該当は0件である（3.2）。

---

## 7. 本体リリースを待たない暫定策（対象PJ向け）

#23 が本体に入って `install` されるまでの間、対象PJは PJ 側の実装で同じ効果を得られる。**本体クラスの上書きは不要で、本体対応が入ったら設定から外すだけで済む。**

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

**converter 側（5.2）には暫定策が無い。** 現状は変換が中止されるため、0件テーブルを含む YAML は Excel へ変換できない。5.2 の対応が入るまでは、該当テーブルのカラム名行を手で足すか、YAML 側で1行だけダミーを持たせる等の回避が要る（未検討）。

---

## 8. スコープ外の申し送り

| 項目 | 対応先 | 前提 |
|---|---|---|
| `@Ignore` 4件の解除と `YamlTableDataBuilder.java:110-114` の FIXME 削除 | `nablarch-testing-yaml`（別リポジトリ・別セッション） | #23 が本体に入り `install` された後でないと検証できない |
| **マーカーカラム行の出力対応**（`XlsFormatWriter.java:233-240` の番人の置き換え。5.2） | `nablarch-testing-converter` | #23 とは独立に着手可。ただし #23 より先に入れると、変換後の Excel は #23 が入るまで偽陰性のまま |
| 上に伴うテストの書き換え | `nablarch-testing-converter` | 番人の存在を前提にした3件。`XlsFormatWriterTest.java:451`（テーブル）／`:471`（`LIST_MAP`）／`SampleConversionTest.java:65-82`（climan サンプルの変換が中止されることの確認）。`SampleConversionTest` の Javadoc は「本体が対応したら変換が成功して2冊出力されるテストへ戻す」と予告している |
| 記法違反（識別子行の連続）のエラー化 | `nablarch-testing`（将来の課題） | 今回は見送り（6.1）。記法違反データが黙って壊れる状態は残る |
| 解説書の記述更新 | `nablarch-document` | 下記 |

`nablarch-testing-yaml` で `@Ignore` されている4件（revert コミット `190cc9a` のメッセージに記載）:

- `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:399`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`（同 `yaml/YamlTableDataBuilderTest.java:147`）
- `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`（同 `:421`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`（同 `:871`）

### 解説書（`nablarch-document`）への反映

- **`testdata_notation.rst:802`（「データ行を書かない場合でも、カラム名の行は省略できない」）は変更不要。** Excel 記法として正しく、#23 でも変わらない
- **YAML の `rows: []` が0件検証として有効**である旨を明記する。`ja/development_tools/` 配下の rst に `rows: []` の記述は無い（未確認: この件数は再確認していない）
- 公式の記載例はいずれもカラム名行を書いており、**例の変更は不要**
- converter が出力するマーカーカラム行（5.2）を記法の説明に載せるかは、converter の対応内容が固まってから解説書チームが判断する

---

## 9. 未確認事項

- **対象PJのテストデータは未走査**（3.5）。記法違反データの出現頻度は確認していない
- **カラム名0件かつ行が1件以上ある期待値**（Excel でマーカーカラムのみのヘッダにデータ行が続く形）の #23 修正後の挙動。現状は実データ0行扱いで PK 不一致 fail、修正後は `expected.getValue()` 側に値が無く別の失敗の仕方になる可能性がある
- **先頭カラム名がデータタイプ名で始まるテーブル**（`MESSAGE_ID` 等）が既存テストデータに実在するか。本リポジトリの xls 走査では0件（3.2）だが、対象PJは未確認。`getDataType()` の判定が前方一致であるため（`TestDataParsingTemplate.java:328`）、将来ここを触る場合の注意点として残す
- **実 `.xlsx` を通した経路**。5.2 の実測はパーサ層に行リストを直接流したものであり、`XlsReader` の空セル処理を挟んだ往復では実行していない
- **マーカーカラムのセルに何を書くかの取り決め**（`[空]` など）。未決。converter 側で決める
- **0件テーブルを含む YAML を Excel へ変換したい場合の暫定回避策**（7章）。未検討

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
| マーカーカラムを除外して有効カラム名を作る | `src/main/java/nablarch/test/core/reader/HeaderLine.java:39-41, 47-50` |
| マーカーカラムの判定は `[` 始まり `]` 終わり | `src/main/java/nablarch/test/core/reader/HeaderLine.java:85-91` |
| 空行は読み込み時に除去される | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182` |
| `doParse` のループ構造 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:284-310` |
| `getDataType()` は前方一致 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:321-333`（判定は328行目） |
| 期待値検証の入口 | `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362`／`src/main/java/nablarch/test/TestSupport.java:278-281` |
| `testDataParser` は `SystemRepository` 解決 | `src/main/java/nablarch/test/TestSupport.java:404` |
| `BasicTestDataParser#setDbInfo` | `src/main/java/nablarch/test/core/reader/BasicTestDataParser.java:221` |
| YAML はカラム名を `rows` の先頭要素のキーから解決する | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:145-150`（呼び出しは `yaml/YamlTableDataBuilder.java:88`） |
| YAML 読み込みはマーカーカラムを除外する | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:100-115`（105・115）／`yaml/YamlSection.java:155-157` |
| 本事象の FIXME コメント | `nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:110-114` |
| 差し戻しの経緯と `@Ignore` した4件 | `nablarch-testing-yaml` コミット `190cc9a` |
| 中間モデルのカラム名は「マーカーカラムを含む」契約 | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/model/TableDataBlock.java:43` |
| YAML→中間モデルでカラム名は本体アダプタ由来（マーカー除去済み） | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/yaml/YamlFormatReader.java:154-170`（159・169） |
| Excel→中間モデルでカラム名は本体アダプタ由来（マーカー除去済み） | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/xls/XlsFormatReader.java:149, 161` |
| 中間モデル→YAML は行0件なら `rows: []` のみでカラム名を書かない | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/yaml/YamlFormatWriter.java:274-277` |
| カラム名0件ブロックを書き出せない番人 | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/xls/XlsFormatWriter.java:233-240` |
| マーカーカラムの版面記録（書き手側の下地） | `nablarch-testing-converter` `src/main/java/nablarch/test/tool/converter/xls/XlsFormatWriter.java:246-249` |
| 往復テストは中間モデル→同一形式→中間モデルのみ（クロス形式は対象外） | `nablarch-testing-converter` `src/test/java/nablarch/test/tool/converter/RoundTripTest.java:34-56` |
| 番人を前提にした既存テスト | `nablarch-testing-converter` `src/test/java/nablarch/test/tool/converter/xls/XlsFormatWriterTest.java:451, 471`／`src/test/java/nablarch/test/tool/converter/SampleConversionTest.java:65-82` |
| 公開中の記法定義（1行目=識別子行／2行目=カラム名／3行目～=データ行） | `nablarch-document` `origin/main` `ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/02_DbAccessTest.rst:110-117, 228-235` |
| 0件テーブルの記載例（カラム名の行までを記述し、データ行を記述しない） | `nablarch-document`（`ntf-yaml-support`）`ja/development_tools/testing_framework/implementation/testdata_examples.rst`「0件のテーブルデータを記述する」（`b75f1d7`・2026-08-14 追加） |
| 「カラム名の行は省略できない」（`origin/main` に同記述なし。現行 Excel 記法の明文化） | `nablarch-document`（`ntf-yaml-support`）`testdata_notation.rst:802`（`b75f1d7`・2026-08-14 追加） |
| `EXPECTED_TABLE` の定義（テスト実行後の期待するデータベースのデータ） | `nablarch-document` `origin/main` `06_TestFWGuide/01_Abstract.rst:275-276` |
