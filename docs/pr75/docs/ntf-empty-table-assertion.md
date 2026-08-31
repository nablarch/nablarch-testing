# 期待値0件テーブル検証の偽陰性 — NTF 本体への変更提案

更新日: 2026-08-19

**宛先**: Nablarch 本体（`nablarch-testing`）チーム

**お願いしたいこと**: 4章の変更（`TableData#loadData()` の1メソッド）を入れてよいかの判断。

**出典の書き方**: 事実は実物のソースで確認したものに限り、「リポジトリ相対パス:行番号」で示す。リポジトリ名の記載が無いものは `nablarch-testing` を指す。実測値には実行日を添える。確認していないことは「未確認」と明記する。

---

## 1. 要約

**何が起きるか** — 期待値テーブルのカラム名が0件のとき、`TableData#loadData()` が DB を読まずに「実データも0行」として扱う。**DB に行が残っていても検証が必ず PASS する**（偽陰性）。

**なぜ今これが問題になるか** — Excel 形式はデータ行が0件でもカラム名の行を書くため、記法どおりに書く限りこの経路には到達しなかった。YAML 形式ではカラム名が `rows` の先頭要素のキーから決まるため、0件テーブル（`rows: []`）には**カラム名を書く場所が構造上存在しない**。YAML 対応によって、この経路が正規の入力になった。

**したがってこれは、リリース済みの不具合の修正ではなく、新しい記法を受け入れるための変更である**（3章）。

**何を直すか** — `TableData#loadData()` の early return を、`dbInfo` からカラムを補う実装に置き換える。修正は1メソッドに閉じる（4章）。

**後方互換影響** — 本リポジトリのテストデータ全件（59ファイル・242シート・テーブル系識別子行324箇所）を走査し、挙動が変わるデータは**0件**だった（5章）。

**Excel 形式の読み込み（`TableDataParser` 等）は変更しない。** 「識別子行の次の行を無条件にカラム名行として読む」挙動が別にあるが、Excel 記法どおりに書けば発生しないため、変更不要と判断した（6.1）。

---

## 2. 事象

### 2.1 カラム名が0件だと SQL を発行しない

`src/main/java/nablarch/test/core/db/TableData.java:337-346`

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

`loadData()` は期待値検証のとき、期待値 `TableData` を clone して実データを読み込むために呼ばれる（`src/main/java/nablarch/test/Assertion.java:79-83`）。ここで DB を読まずに0行を返すため、**期待値0行・カラム名0件のとき、DB に行が残っていても比較相手が0行どうしになり、検証が必ず PASS する。**

### 2.2 YAML の0件テーブルは必ずこの経路に落ちる

```yaml
expected_tables:
  - table: ORDER_HEADER
    rows: []
```

YAML 形式はカラム名を `rows` の先頭要素のキーから決める（`nablarch-testing-yaml` `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:145-150`）。`rows: []` にはキーが無いのでカラム名は0件になり、そのまま `TableData` が生成される（同 `yaml/YamlTableDataBuilder.java:100-115`）。同箇所には本事象の FIXME が残っている（同 `:110-114`）。

このため `nablarch-testing-yaml` では、0件テーブルの検証に関するテスト4件が `@Ignore` で止まっている（付録B）。

### 2.3 0件のとき、テストデータに書かれたカラム名はどこからも参照されない

**期待値（`EXPECTED_TABLE`）が0行のとき** — `Assertion.java:256` で `expected.getColumnNames()` を取得するが、これを使うカラム比較ループは期待値の行ループの内側にある（`Assertion.java:263, 297-303`）。行数0なら1度も回らない。実データ側の余剰行を検出する `dbDataFound` の走査（`Assertion.java:306-314`）はカラム名を使わない。PK 突合に使う `getPrimaryKeys()` は `dbInfo` 由来である（`TableData.java:480-482`）。

**準備データ（`SETUP_TABLE`）が0行のとき** — `deleteData` はテーブル名のみを使う（`TableData.java:127-130`）。`insertData` の INSERT 文は `getNonComputedColumns()`＝`dbInfo` 由来で組み立てられ（`TableData.java:139-141`）、`contents` が0件なのでバインドループが回らない（`TableData.java:143`）。

**つまり、0件テーブルの意味を成り立たせるのにカラム名は要らない。** 現在の実装は「カラム名が0件」という*テストデータの書かれ方*を、「DB を読まなくてよい」という*検証の実施可否*に読み替えている。カラム名が無いのは「**何を比較するか**の情報が無い」だけで、「**比較しなくてよい**」ことは意味しない。

「カラム名が無いと SELECT が組めない」も成立しない。カラム名は DB スキーマが持っており、`TableData` は `dbInfo` を保持している。実際、`getColumnNames()` は `columnNames == null` のとき `dbInfo.getColumns(tableName)` にフォールバックする（`TableData.java:501-506`）。**情報源は既にあり、長さ0の配列のときだけそこに繋がっていない。**

---

## 3. なぜ本体の変更が必要か

### 3.1 カラム名の行は表形式の記法要素であって、データモデルの構成要素ではない

| 形式 | カラム名の行 | 理由 |
|---|---|---|
| データモデル（仕様） | 概念として存在しない | 0件では参照されない（2.3）。カラム名は DB スキーマが持つ情報 |
| Excel（表） | **必要**。記法どおり | 識別子行の次の行がカラム名の行と決まっており、そこがブロックの区切りになる |
| YAML（モデル） | **書く場所が無い** | カラム名は `rows` の先頭要素のキーから決まる。`rows: []` にはキーが無い |

**Excel がデータ行0件でもカラム名の行を書くことの根拠**:

- **記載例** — `nablarch-document`（`ntf-yaml-support`）`ja/development_tools/testing_framework/implementation/testdata_examples.rst` の「0件のテーブルデータを記述する」は、`SETUP_TABLE=ORDER_HEADER` / `ORDER_ID ITEM_COUNT STATUS` / （データ行なし）という形を示し、`EXPECTED_TABLE` も同様である。本文に「いずれもカラム名の行までを記述し、データ行を記述していない。」とある（`b75f1d7`・2026-08-14 追加）
- **実データ** — 本リポジトリの `.xls` 走査（2026-08-19）で、カラム名行を書いてデータ行が0件のブロックが**5件**実在する（5.2）。いずれも `columnNames.length >= 1` なので本事象に当たらず、正しく動いている
- **公開中の解説書** — 「1行目：`SETUP_TABLE=<テーブル名>` / 2行目：そのテーブルのカラム名 / 3行目～：登録するレコード」と構成を定義している（`nablarch-document` `origin/main` `ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/02_DbAccessTest.rst:110-117, 228-235`）

### 3.2 これは不具合ではない

**Excel 形式だけの世界では、記法どおりの書き方でこの経路に到達しない。** 到達するのは「識別子行の次の行が無い（シート末尾）」＝カラム名の行を書いていない記法違反のときだけで、実測でも0件である（5.1・5.2）。**不具合として顕在化したことがない防御コードだった。**

YAML を足したことで `rows: []` が正規の入力になり、初めてこの経路が日常的に通る。したがって**「リリース済みの不具合を直す」のではなく「新しい記法を受け入れるために本体を変える」**という位置づけになる。

補足として、Excel でも記法に反せず到達する形が1つある。**ヘッダ行がマーカーカラム（`[ ]`）のみ**のときで、カラム名の行は実在するので記法違反ではない（`src/main/java/nablarch/test/core/reader/HeaderLine.java:39-41, 85-91`）。この形の `EXPECTED_TABLE` で検証が素通りするのが誤りなのか、「全列を比較対象外にした」意図どおりなのかは、**記法に定義が無く判断できない（未確認・付録C）**。実測ではテーブル系0件であり、検出した14件はすべて `LIST_MAP` で `loadData()` に到達しない（5.2）。

### 3.3 直さないと何が困るか

- **YAML で書いた0件テーブルの期待値検証が、すべて素通りする。** 「このテーブルは空になっているはず」という検証が常に成功するため、テストが機能しない
- `nablarch-testing-yaml` の該当テスト4件を有効化できない（付録B）
- 0件テーブルを含む YAML を Excel へ変換できない状態も解けない（付録B）

---

## 4. 直す範囲

### 4.1 変更する — `TableData#loadData()`

| 項目 | 内容 |
|---|---|
| 修正箇所 | `TableData#loadData()`（`TableData.java:337-346`）の early return |
| 修正方法 | `colNames.length == 0` のとき `dbInfo.getColumns(tableName)` を SELECT 対象カラムとする。**修正は `loadData()` 内に閉じる** |
| 確認テスト | DB に行を挿入した状態で、カラム名0件の期待値 `TableData` を `Assertion.assertTableEquals` に渡し、修正前は PASS・修正後は FAIL することを示す |

**修正後、検証は実際に効く。** 期待値の行ループが0回なのでカラム比較（`Assertion.java:297-303`）は回らないが、`dbDataFound` の走査が余剰行を検出して `an unexpected record is included in the table of [T]` で落ちる（`Assertion.java:306-314`）。PK 値も `dbInfo` 由来のカラムで SELECT した `contents` に入っているので `getPkValues`（`TableData.java:683-691`）は動く。

### 4.2 変更しない

| 対象 | 理由 |
|---|---|
| **`getColumnNames()`** | 同等の変更を YAML 側で行って差し戻した経緯がある（`nablarch-testing-yaml` の revert コミット `190cc9a`）。`getColumnNames()` を DB 参照に変えた結果、`StubDbInfo` を差す **DB を持たない変換ツールの読み込み経路が `UnsupportedOperationException` で壊れた**。`loadData()` はもともと DB を読むメソッドなので、この問題を踏まない |
| **表形式リーダ**（`TableDataParser`・`ListMapParser`・`TestDataParsingTemplate`） | 6.1 |
| 準備データ投入・マスタデータ投入 | `loadData()` を通らない |
| 変換ツール（`nablarch-testing-converter`）の読み込み経路 | `loadData()` を通らない |
| `LIST_MAP` | `TableData` を作らない |

**現行動作へ戻すための設定は設けない**（理由は 6.3）。

---

## 5. 後方互換影響

### 5.1 変更後に挙動が変わりうるデータは、2つの形しかない

変えるのは `colNames.length == 0` のときの挙動だけである（分岐は `TableData.java:343`）。Excel 経路でここに到達する経路は2つしかない。

| 到達経路 | 根拠 | 記法との関係 |
|---|---|---|
| 識別子行がシート末尾にあり、次の行が無い | `readLine()` が返さず `HeaderLine` の `keys` が空になる（`HeaderLine.java:32-38`） | カラム名の行が無い＝**記法違反** |
| ヘッダ行がマーカーカラム（`[ ]`）のみ | `getEffectiveColumnNames()` がマーカーカラムを除外するため長さ0になる（`HeaderLine.java:39-41, 47-50, 85-91`） | カラム名の行は存在するので記法どおり。ただし DB 操作対象カラムが1つも無い |

**記載例どおりに書いた0件テーブル（カラム名行あり・データ行0件）は、`columnNames.length >= 1` なので分岐に入らない。** 挙動は1ミリも変わらない。

**「識別子行を2行続けて書く」も分岐に到達しない。** 次の識別子行がカラム名行として消費され、`columnNames` は長さ1（例: `["SETUP_TABLE=NEXT_T"]`）になるからである（`src/main/java/nablarch/test/core/reader/TableDataParser.java:107-116`）。この書き方は記法違反であり、現状は後続ブロックが消える（6.1）。**本変更でもその挙動は変わらない。**

### 5.2 実測 — 本リポジトリに該当は0件（2026-08-19）

`src/test` 配下の `.xls`/`.xlsx` 全件を Apache POI で走査した。空行を除去したうえで、テーブル系識別子行（`SETUP_TABLE=`／`EXPECTED_TABLE=`／`EXPECTED_COMPLETE_TABLE=`／`LIST_MAP=`）の直後の行を判定した。

| 走査対象 | 件数 |
|---|---|
| ファイル | 59 |
| シート | 242 |
| テーブル系識別子行 | 324 |

| 検出した形 | 件数 | 本変更の影響 |
|---|---|---|
| 識別子行がシート末尾（カラム名行なし・記法違反） | **0** | — |
| 識別子行の直後が識別子行（記法違反） | **0** | — |
| ヘッダ行がマーカーカラムのみ | 14 | すべて `LIST_MAP`。`ListMapParser` 経由で `TableData` を生成しないため `loadData()` に到達しない。**なし** |
| **カラム名行あり・データ行0件（記載例どおりの0件テーブル）** | **5** | `columnNames.length >= 1` で分岐に入らない。**なし** |

カラム名行あり・データ行0件の5件（記法どおりの0件テーブルが実在することの証拠）:

| ファイル | シート | ブロック |
|---|---|---|
| `MessagingReceiveTestSupportTest.xls` | `testExtends` | `SETUP_TABLE=RECEIVE_TEST` |
| `MessagingReceiveTestSupportTest.xls` | `testUnExtends` | `SETUP_TABLE=RECEIVE_TEST` |
| `AbstractHttpRequestTestTemplateTest.xls` | `testGetEmptyTestCase` | `LIST_MAP=testCases` |
| `HttpRequestTestSupportTest.xls` | `testAssertObjectPropertyEquals2` | `LIST_MAP=nullValue` |
| `MessagingRequestTestSupportTest.xls` | `testMessagingSample` | `LIST_MAP[case2]=EXPECTED_LOG` |

**→ 本リポジトリに、本変更で挙動が変わる既存テストデータは存在しない。**

走査プログラムは作業用ディレクトリに置いたのみでリポジトリには残していない。再実行が必要な場合は上記の判定条件で書き直すこと。

### 5.3 影響を受ける経路は1本しかない

`loadData()` のプロダクションコードからの呼び出し元は `Assertion.java:81` の1箇所のみで、`assertTableEquals(String, TableData)` からしか呼ばれない（`grep -rn "loadData()" src/main` で確認）。期待値検証の入口は `src/main/java/nablarch/test/core/db/DbAccessTestSupport.java:362` の1本で、`TestShot`・`AbstractHttpRequestTestTemplate` もここを通る。

### 5.4 影響が出るとしたら、どう出るか

- **DB が実際に空だった** → PASS のまま。影響なし
- **DB に行が残っていた** → 新たに FAIL する。検証されていなかったものが検証されるようになった結果であり、意図したアサートの失敗である
- **テーブル名を誤記していた** → 新たに例外になる。現状の0件カラム経路は `dbInfo` に触れずに return するため誤記でも PASS するが、修正後は `getPrimaryKeys()` → `dbInfo.getPrimaryKeys(tableName)`（`TableData.java:480-482`）に到達する

いずれも 5.1 の2つの形のデータでのみ起こる。実測では0件。

### 5.5 未走査の範囲

**利用PJのテストデータは未走査である。** 本リポジトリの結果をもって利用PJに該当が無いとは言えない。記法どおりのデータは分岐に入らないという 5.1 の論理は形式上の制約なので利用PJにも当てはまるが、記法違反データの有無は機械で確認していない。

---

## 6. 検討した代替案と不採用理由

### 6.1 Excel 形式の読み込みも直す案（不採用）

**現在の挙動** — 表形式リーダは識別子行の次の行が本当にカラム名行なのかを検証していない（`TableDataParser.java:107-116`）。

```java
void onTargetTypeFound(List<String> line) {
    // テーブル名
    String tableName = getTypeValue(line);
    // カラム名の行を読み込み
    header = new HeaderLine(readLine());
```

空行は読み込み時に除去済みなので（`src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:180-182`）、ここで返るのは「次の非空行」である。同じ書き方が `src/main/java/nablarch/test/core/reader/ListMapParser.java:78-82` にもある。このため識別子行が連続すると、次のブロックが消える。

```
SETUP_TABLE=EMPTY_T     ← カラム名の行を省略した（記法違反）
SETUP_TABLE=NEXT_T      ← 食われる（カラム名行にされる）
PK   NAME
1    foo
```

→ `EMPTY_T` のカラム名が `SETUP_TABLE=NEXT_T`、データ2行。**`NEXT_T` は消滅する。** `doParse` のループはその次から再開し、`nowReading` は真のままなので、次ブロックのデータ行が手前のテーブルのデータ行として取り込まれる（`TestDataParsingTemplate.java:284-310`、`TableDataParser.java:97-100`）。エラーにも警告にもならない。

**直さない理由** — Excel 記法はデータ行が0件でもカラム名の行を書くのが仕様であり（3.1）、**上の入力は記法違反である。記法どおりに書けば発生しない。** 直すと「カラム名行を省略した0件ブロック」という**新しい書き方が Excel に生まれる**。記法を増やす変更であり、YAML 対応にも必要ない。

**エラー化も行わない。** 記法違反の検出という意味はあるが、リリース済みの読み込み挙動を変えることになる。実測で該当0件（5.2）であり、払うコストに見合う便益が観測できていない。将来の課題として付録Bに申し送る。

**なお、0件テーブルを含む YAML を Excel へ変換する経路には別途対応が要る。** 変換ツール側でマーカーカラムだけのカラム名行を出す形で解ける（付録B）。本体の変更は不要である。

### 6.2 その他の案

| 案 | 不採用理由 |
|---|---|
| **YAML 形式のときだけ直す** | 形式によって検証の挙動が変わる。`TableData` は読み込み元の形式を保持していない（コンストラクタは `dbInfo`/`tableName`/`columnNames`/`defaultValues` のみ。`TableData.java:71-75, 85`）ため、現構造とも噛み合わない |
| **YAML に `columns:` フィールドを追加する** | カラム名は DB スキーマが持つ情報であり、テストデータに二重に持たせるものではない。YAML はデータモデルをそのまま表す形式であり、現在のスキーマがあるべき姿である |
| **「明示的に0件と宣言されたテーブル」にだけカラムを補う（フラグ案）** | 後方互換は完全に不変にできる（`TableData` に「明示的0件宣言」フラグを持たせ、YAML の `rows: []` でだけ立てる）。しかし記法違反の Excel（シート末尾に識別子行だけ）を素通りさせ続ける判断になり、**嘘の合格を返す経路を仕様として恒久化する**。実測で払うべき互換コストが観測できていない（5.2）ため、採らない |

### 6.3 現行動作へ戻すための設定を設けない理由

一度は「設定で現行動作を残す（デフォルト無効のオプトイン）」案を検討した。デフォルトを現行動作にすれば既存PJの挙動は1行も変わらず、「嘘の合格を維持する設定」ではなく「0件テーブルの厳格検証を有効にする設定」というオプトインの機能追加として成立する、という整理だった。

**採らない。** 根拠は2点である。

1. **分岐に入るのは 5.1 の2つの形だけであり、記法どおりに書かれたデータは1件も挙動が変わらない**
2. **したがって、本変更で NG になるテストは、利用PJ側が見直す対象である。** 設定で温存することは、その見直しを先送りするだけになる

加えて、設定を残すと「0件テーブルの検証が効くかどうかが設定に依存する」という状態が恒久化し、テスティングフレームワークとして筋が悪い。

---

## 付録A. 本体リリースを待たない暫定策（利用PJ向け）

本変更がリリースされるまでの間、PJ 側の実装で同じ効果を得られる。**本体クラスの上書きは不要で、本体対応が入ったら設定から外すだけで済む。**

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
- 期待値検証の入口は1本（5.3）
- 準備データ・マスタデータは `loadData()` を通らないので包む必要がない（4.2）

---

## 付録B. 他リポジトリ・解説書への申し送り

### B.1 `nablarch-testing-converter` — Excel 書き出しでマーカーカラム行を出す

**現状** — 中間モデルのカラム名が0件のブロックを Excel へ書き出そうとすると `IllegalArgumentException` で変換を中止する（`src/main/java/nablarch/test/tool/converter/xls/XlsFormatWriter.java:233-240`）。YAML の `rows: []` はカラム名0件の中間モデルになるため、**0件テーブルを含む YAML は Excel へ変換できない**。実際に climan サンプルの `SETUP_TABLE=CLIENT` で変換が止まり、その状態がテストとして固定されている（`src/test/java/nablarch/test/tool/converter/SampleConversionTest.java:65-82`）。

**対応** — 例外の代わりに、識別子行の次へ**マーカーカラム（`[ ]`）だけのカラム名行**を1行出力する。

```
SETUP_TABLE=EMPTY_T
[空]                    ← カラム名の行として実在する
SETUP_TABLE=NEXT_T      ← 無傷
PK   NAME
1    foo
```

読み戻すと、マーカーカラムは本体が「比較対象外の列」として除外するため（`HeaderLine.java:39-41, 85-91`）、`EMPTY_T` はカラム名0件・0行の `TableData` になる。本体の変更（4章）が入っていれば、そこから DB の全カラムで SELECT され**検証が効く**。カラム名の行が物理的に存在するので**次のブロックも無傷**であり、6.1 の事象は起きない。

**実測（2026-08-19）**: 本体の `TableDataParser` に行リストを直接流して測定した（`DataType.SETUP_TABLE_DATA`／インメモリ `TestDataReader`／スタブ `DbInfo`）。

| 入力 | 結果 |
|---|---|
| 識別子行のみ（記法違反） | ブロック **2→1件**。`table=EMPTY_T columnNames=[SETUP_TABLE=NEXT_T] rows=2` |
| 識別子行＋マーカーカラムのみのヘッダ行 | ブロック **2件**。`table=EMPTY_T columnNames=[] rows=0`／`table=NEXT_T columnNames=[PK, NAME] rows=1` |
| 通常の2ブロック（対照） | ブロック 2件。正常 |

**この案は Excel 記法に反しない。** 「2行目はそのテーブルのカラム名」（`02_DbAccessTest.rst:110-117`）に対し、マーカーカラム行はカラム名行として実在する。

**マーカーを出せるのは中間モデル→Excel の書き出しだけである。** converter は Excel／YAML の双方を中間モデル経由で扱い、どちらの入口でもマーカーカラムは中間モデルに入る前に除去される。

| 経路 | 実装 | マーカーの扱い |
|---|---|---|
| 中間モデルの契約 | `model/TableDataBlock.java:43` | カラム名リストは「マーカーカラムを含む」 |
| YAML→中間モデル | `yaml/YamlFormatReader.java:154-170`（159・169） | `nablarch-testing-yaml` の `YamlTableDataBuilder.java:100-115` が返したカラム名（マーカー除外済み）をそのまま渡す。**除去される** |
| Excel→中間モデル | `xls/XlsFormatReader.java:149, 161` | 本体 `HeaderLine.java:41` が除外済みのカラム名をそのまま渡す。**除去される** |
| 中間モデル→YAML | `yaml/YamlFormatWriter.java:274-277` | 行0件なら `rows: []` のみ。カラム名は書かない |
| 中間モデル→Excel | `xls/XlsFormatWriter.java:233-240` | カラム名0件なら例外（ここを置き換える） |

書き手側の下地は既にあり、マーカー位置を `BlockLayout` に記録して `Fill.MARKER` で塗る処理が実装済みである（`xls/XlsFormatWriter.java:246-249`）。

**往復の可逆性は壊れない。** `RoundTripTest`（`src/test/java/nablarch/test/tool/converter/RoundTripTest.java:34-56`）が検証しているのは**中間モデル → 同一形式 → 中間モデル**の往復であり、XLS 経路・YAML 経路それぞれで独立に見ている。クロス形式（Excel→YAML→Excel）の一致は対象外である。

- XLS 経路: 中間モデル(カラム名0件) → Excel(`[空]`) → 中間モデル(カラム名0件)。**一致する**。現状は例外で往復できないため、可逆になる方向の変更である
- YAML 経路: 中間モデル(カラム名0件) → `rows: []` → 中間モデル(カラム名0件)。**現状も一致し、変わらない**

**書き換えが必要な既存テスト**（番人の存在を前提にしている）:

- `src/test/java/nablarch/test/tool/converter/xls/XlsFormatWriterTest.java:451`（テーブル）／`:471`（`LIST_MAP`）
- `src/test/java/nablarch/test/tool/converter/SampleConversionTest.java:65-82`。Javadoc に「本体が対応したら変換が成功して2冊出力されるテストへ戻す」と予告がある

**マーカーカラムのセルに何を書くかは converter 側で決める**（`[空]` を想定。未決）。

### B.2 `nablarch-testing-yaml` — `@Ignore` 4件の解除

本体の変更（4章）が入り `install` された後に実施する。あわせて `YamlTableDataBuilder.java:110-114` の FIXME を削除する。

- `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:399`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`（同 `yaml/YamlTableDataBuilderTest.java:147`）
- `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`（同 `:421`）
- `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`（同 `:871`）

### B.3 `nablarch-document` — 解説書

- **`testdata_notation.rst:802`（「データ行を書かない場合でも、カラム名の行は省略できない」）は変更不要。** Excel 記法として正しく、本変更でも変わらない
- **YAML の `rows: []` が0件検証として有効**である旨を明記する。`ja/development_tools/` 配下の rst に `rows: []` の記述は無い（未確認: この件数は再確認していない）
- 公式の記載例はいずれもカラム名行を書いており、**例の変更は不要**
- converter が出力するマーカーカラム行（B.1）を記法の説明に載せるかは、converter の対応内容が固まってから判断する

### B.4 将来の課題 — 記法違反（識別子行の連続）のエラー化

今回は見送った（6.1）。記法違反データが黙って壊れる状態は残る。

---

## 付録C. 未確認事項

- **ヘッダ行がマーカーカラムのみの `EXPECTED_TABLE`** で検証が素通りするのが誤りなのか、意図どおりなのか。記法に定義が無く判断できない（3.2）。実測ではテーブル系0件
- **利用PJのテストデータは未走査**（5.5）。記法違反データの出現頻度は確認していない
- **カラム名0件かつ行が1件以上ある期待値**（Excel でマーカーカラムのみのヘッダにデータ行が続く形）の修正後の挙動。現状は実データ0行扱いで PK 不一致 fail、修正後は `expected.getValue()` 側に値が無く別の失敗の仕方になる可能性がある
- **先頭カラム名がデータタイプ名で始まるテーブル**（`MESSAGE_ID` 等）が既存テストデータに実在するか。本リポジトリの xls 走査では0件だが、利用PJは未確認。`getDataType()` の判定が前方一致であるため（`TestDataParsingTemplate.java:321-333`、判定は328行目）、将来 6.1 に手を入れる場合の注意点として残す
- **実 `.xlsx` を通した経路**。B.1 の実測はパーサ層に行リストを直接流したものであり、`XlsReader` の空セル処理を挟んだ往復では実行していない
- **マーカーカラムのセルに何を書くかの取り決め**（`[空]` など）。未決
- **0件テーブルを含む YAML を Excel へ変換したい場合の暫定回避策**（B.1 の対応が入るまで）。未検討
