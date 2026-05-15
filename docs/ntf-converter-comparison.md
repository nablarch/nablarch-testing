# nablarch-test-data-converter × スキーマ設計 比較

## 1. nablarch-test-data-converter の概要

### ツールの目的
Nablarch Testing Framework のテストデータ Excel ファイルを YAML に変換する CLI ツール。
`java -jar nablarch-test-data-converter-0.0.1.jar <Excelファイルパス> [出力ディレクトリ]` 形式で実行する。

### アーキテクチャ
| クラス | 役割 |
|---|---|
| `Main.java` | CLI エントリポイント。引数解析・Excel 読み込み指示 |
| `ExcelReader.java` | Apache POI でシートを読み込む |
| `DataSectionParser.java` | シートの行を走査し `DataSection` のリストに変換 |
| `ExcelToYamlConverter.java` | Excel → YAML の変換フロー制御 |
| `YamlWriter.java` | SnakeYAML を使って YAML ファイルを出力 |
| `TestDataSheet` | シート1枚の表現（シート名 + DataSection リスト） |
| `DataSection` | データブロック1つの表現（dataType, groupId, targetName, columns, rows） |
| `DataType` | データタイプ列挙型（SETUP_TABLE, LIST_MAP, SETUP_FIXED 等） |

### 出力構造
```
<出力ディレクトリ>/<Excelファイル名（拡張子なし）>/<シート名>.yaml
```

1 シート = 1 YAML ファイル（本スキーマの設計方針「選択肢 A: 1 シート 1 ファイル分割」と一致）。

### 出力 YAML のトップ構造
```yaml
sheetName: <シート名>
sections:
  - dataType: SETUP_TABLE
    groupId: "1"
    tableName: TEST_TABLE
    columns: [ID, NAME, VALUE]
    rows:
      - {ID: "001", NAME: "太郎", VALUE: "100"}
```

テーブル系でない（LIST_MAP・ファイル系・メッセージ系）場合は `tableName` の代わりに `dataId` を使用する。

---

## 2. 差分一覧

| # | 項目 | 本スキーマ（ntf-testdata-yaml-schema.json） | nablarch-test-data-converter | 採用方針 |
|---|---|---|---|---|
| 1 | **トップレベルキー構造** | `setup_tables`, `expected_tables`, `setup_files` 等の**種別別配列**がトップレベルに並ぶ | `sheetName` + `sections` の2キー。sections 内に全種別データが混在 | **本スキーマに合わせるべき** |
| 2 | **テーブル名フィールド名** | `table:` | `tableName:` | **本スキーマに合わせるべき** |
| 3 | **LIST_MAP / ファイル系 ID フィールド名** | `id:` | `dataId:` | **本スキーマに合わせるべき** |
| 4 | **データタイプ区別方法** | トップレベルキー（`setup_tables` 等）で暗黙に決まる。各要素に `dataType` キーは存在しない | `dataType: SETUP_TABLE` のように各 section に明示 | **本スキーマに合わせるべき** |
| 5 | **SETUP_FIXED / SETUP_VARIABLE の統合** | `type: fixed` / `type: variable` フィールドで区別した上で `setup_files:` に統合 | `dataType: SETUP_FIXED` と `dataType: SETUP_VARIABLE` を別 section として出力（統合しない） | **本スキーマに合わせるべき** |
| 6 | **EXPECTED_FIXED / EXPECTED_VARIABLE の統合** | 同上、`expected_files:` に統合 | `dataType: EXPECTED_FIXED` と `dataType: EXPECTED_VARIABLE` を別 section として出力 | **本スキーマに合わせるべき** |
| 7 | **ファイル系のフィールド・レコード定義** | `records:` 配列の各要素に `record_type`, `fields`（name/type/length）, `rows`（配列の配列）を持つ詳細構造 | `columns:` リスト + `rows:`（オブジェクト配列）のみ。フィールド型・長さ・レコード種別の概念なし | **本スキーマに合わせるべき** |
| 8 | **ファイル系 rows の形式** | **配列の配列** `[["val1","val2"], ...]` | **オブジェクト配列** `[{COL: "val"}, ...]`（テーブル系と同形式） | **本スキーマに合わせるべき** |
| 9 | **ディレクティブ（file-type, text-encoding 等）** | `directives:` オブジェクト（型安全・構造化） | `columns:` / `rows:` に混在させて出力（README 例: `file-type: text-encoding`, `Fixed: windows-31j` 等、ディレクティブ行もデータ行と同列に扱う） | **本スキーマに合わせるべき** |
| 10 | **マーカーカラムの扱い** | YAML キーとして `"[COLNAME]"` 形式で**保持**する | マーカーカラムを**除外**して出力しない | **要議論** |
| 11 | **シート名フィールド** | スキーマに `sheetName` は存在しない。1 ファイル = 1 シートに対応するためシート名はファイル名で表現 | `sheetName:` がトップレベルに必ず存在 | **本スキーマに合わせるべき** |
| 12 | **columns リスト** | `columns:` フィールドは存在しない。テーブル系は各 row オブジェクトのキーでカラムを表現。ファイル系は `fields:` で定義 | `columns:` を別途持ち、rows オブジェクトのキーとの二重定義になる | **本スキーマに合わせるべき** |
| 13 | **RAW_DATA（非 Nablarch フォーマット）** | スキーマに定義なし。ドキュメントシートを変換対象外とするかの設計判断が未定 | `dataType: RAW_DATA` + `rawData:` 二次元配列として出力（テストケース一覧シート等への対応） | **どちらでもよい / 要議論** |
| 14 | **group_id の型・省略方法** | 省略する場合は `group_id:` キー自体を書かない（`null` や `""` は禁止・minLength: 1） | 省略時は `groupId` キー自体を出力しない（同じ挙動。ただしキー名が `groupId`、スキーマは `group_id`） | **本スキーマに合わせるべき**（snake_case 統一） |
| 15 | **数値セルの文字列化** | すべての値を文字列（クォート付き）で記述することを要求 | Excelの数値セルを `DataFormatter` で文字列化して出力（方向性は同じ。ただし `DataFormatter` ではなく POI 3.8 の `cell.getNumericCellValue()` を使用 ）| **どちらでもよい / 要議論** |
| 16 | **空シートの表現** | `setup_tables: []` のように空配列。または対応するセクションキー自体を省略 | `sheetName: setUpDb \n sections: []` として出力 | **本スキーマに合わせるべき** |

---

## 3. 採用方針の詳細

### #1 トップレベルキー構造（最重要差分）

本スキーマは種別別のトップレベルキー（`setup_tables:`, `expected_tables:` 等）を採用しており、YAML ファイルを見た際にどのセクションにどの種別のデータがあるかが一目でわかる。また JSON Schema でのバリデーション、型安全な YAML パーサ実装が容易になる。

nablarch-test-data-converter は `sections:` 配列の各要素に `dataType: SETUP_TABLE` のように種別を明示する Wrapper 方式を採用している。この方式では以下の問題がある。

- パーサが `sections` 配列を走査しながら `dataType` を見て処理を切り替える必要があり、実装が複雑になる
- JSON Schema で種別ごとに異なる必須フィールドを定義するのが困難（`anyOf` / `discriminator` が必要）
- 本スキーマとの互換性がなく、YAMLアダプタ実装時に読み込み先のスキーマを別途定義することになる

**実装ツール側を本スキーマ形式に合わせるべき。**

### #2/#3 フィールド名（`tableName` → `table`、`dataId` → `id`）

本スキーマは一貫して短い名前（`table`、`id`）を使用している。`tableName` / `dataId` は冗長であり、スキーマ定義・ドキュメント・パーサコードのいずれにおいても本スキーマの `table`/`id` に統一すべき。

### #4 dataType フィールドの明示 vs トップレベルキー

`#1` と連動する問題。本スキーマではトップレベルキーで種別が決まるため `dataType` フィールドは不要。nablarch-test-data-converter 出力の `dataType` フィールドはむしろ YAMLアダプタにとって不要な情報となる。**本スキーマに合わせてトップレベルキーで種別を表現すべき。**

### #5/#6 SETUP_FIXED / SETUP_VARIABLE の統合

`BasicTestDataParser#getSetupFile()` が固定長・可変長の両者をまとめて返す実装に対応するため、本スキーマでは `setup_files:` 配列に統合して `type: fixed/variable` で区別する設計を採用している。この設計には以下の根拠がある。

- NTF の `getSetupFile()` は `SETUP_FIXED` / `SETUP_VARIABLE` を区別せずまとめて返す
- テストコードから見ると両者は「セットアップ用ファイルデータ」として同一視される
- YAML 上で分けておく必要がない

nablarch-test-data-converter が別 section として出力するのは、Excel のデータタイプ名をそのまま保持しているためと推定される。**本スキーマの統合方式に合わせるべき。**

### #7 ファイル系のレコード定義詳細

これは最も実装コストが高い差分である。本スキーマでは固定長・可変長ファイルのレコード構造（`record_type`、`fields`（`name`/`type`/`length`）、`rows`（配列の配列））を完全に表現する。

nablarch-test-data-converter は `columns:` のみを保持しておりデータ型・フィールド長の情報を持たない。これは以下の問題を引き起こす。

- YAMLアダプタが固定長ファイルを出力する際にフィールド長がわからないためパディングを決定できない
- `DataFileFragment#setTypes()` / `setLengths()` に渡す情報が欠如している
- 可変長ファイルと固定長ファイルを同一構造で扱えてしまい、誤使用を防げない

**本スキーマの詳細構造に合わせるべき。ただし nablarch-test-data-converter の簡略版は Excel の生データを保持するのに使えるため、変換中間フォーマットとしての位置付けは参考にしてよい。**

### #8 ファイル系 rows の形式（オブジェクト配列 vs 配列の配列）

本スキーマはファイル系の `rows` を「配列の配列」（`[["val1","val2"],...]`）として定義している。理由はフィールド名が `fields:` に定義済みのため各データ行でキー名を繰り返すと冗長かつ長くなるためである（設計ドキュメント §2 参照）。

nablarch-test-data-converter はテーブル系・ファイル系を区別せず全て「オブジェクト配列」にしている。一貫性の観点では実装ツールの方が単純だが、本スキーマの「配列の配列」方式の方が固定長ファイル（多数フィールド・多数行）において YAML の可読性とトークン効率が高い。**本スキーマに合わせるべき。**

### #9 ディレクティブの扱い

nablarch-test-data-converter は Excel の `file-type | Fixed` のようなディレクティブ行をデータ行と区別せず `columns:` / `rows:` に混入させた形で出力している（README 例より）。これは以下の問題がある。

- ディレクティブとデータ行の区別がなくなり、パーサが `rows` を走査してディレクティブを判別する必要がある
- 型安全ではない（`text-encoding` の値として整数が来ても検出できない）
- 本スキーマの `directives:` オブジェクトとは完全に異なる構造

**本スキーマの `directives:` 構造化方式に合わせるべき。**

### #10 マーカーカラムの扱い（要議論）

本スキーマはマーカーカラム（`[COLNAME]` 形式）を YAML キーとして `"[COLNAME]"` 形式で保持する。`HeaderLine` の規則に従い DB 操作から除外されるが、テストデータとして明示的に存在することで以下の利点がある。

- マーカーカラムに記載された補足情報（テスト用フラグ等）が YAML に残る
- Excel との往復変換（YAML→Excel）が可能になる

nablarch-test-data-converter はマーカーカラムを完全に除外する。変換後の YAML からは元の Excel を完全には再現できない。

**ユースケース次第**: 変換後 YAML を人間が読む・AI が生成するだけなら除外でも問題ない。YAMLアダプタで NTF に渡す際にマーカーカラムが必要か不要かを確認してから方針を決定すること。NTF の `HeaderLine` が `[COLNAME]` をマーカーとして処理するのはカラム名行を読む時点であるため、YAML 側でマーカーカラムを省略してしまうと NTF 側の挙動に影響しない可能性が高いが、要確認。

### #11 `sheetName` フィールド

本スキーマでは「1 シート = 1 ファイル」のため、シート名はファイル名で表現される（`FooTest.setUpDb.yaml` 等）。YAML ファイル内に `sheetName:` は不要であり冗長。nablarch-test-data-converter が `sheetName:` を持つのは内部的にシートを単位として処理しているためだが、YAMLアダプタのパーサには不要な情報。**本スキーマに合わせて削除すべき。**

### #12 `columns:` リスト

本スキーマはテーブル系では `rows` オブジェクトのキーでカラムを表現し、ファイル系では `fields:` で定義するため、独立した `columns:` リストを持たない。nablarch-test-data-converter の `columns:` は `rows` のキーと重複する冗長なフィールドである。**本スキーマに合わせて削除すべき。**

### #13 RAW_DATA（どちらでもよい / 要議論）

本スキーマにはドキュメントシート（テストケース一覧等）の取り扱いが未定義である。nablarch-test-data-converter の `RAW_DATA` 方式はドキュメントシートの内容を情報損失なく保存できる実用的な機能であり、YAMLアダプタ実装において同様の要件があるなら参考にすべきである。ただし NTF 本体のパーサは RAW_DATA を処理しないため、このデータはツール専用の拡張フォーマットである。**テストデータとして NTF が利用しない情報は本スキーマの管轄外として位置づけ、必要に応じて別途設計すること。**

### #14 group_id のキー名（`groupId` vs `group_id`）

本スキーマは `group_id`（snake_case）、nablarch-test-data-converter は `groupId`（camelCase）。Java 側のフィールド名に合わせて camelCase にする合理的な理由はあるが、YAML は一般的に snake_case が慣例であり、本スキーマの一貫性（`record_type`, `group_id`, `list_maps` 等すべて snake_case）を維持する方が望ましい。**本スキーマの snake_case に合わせるべき。**

### #15 数値セルの文字列化

方向性は同じ（すべて文字列として出力）だが、nablarch-test-data-converter は Apache POI 3.8 の `cell.getNumericCellValue()` を直接使用しており、`DataFormatter#formatCellValue(cell)` を使っていない。本スキーマの設計ドキュメント（「変換ツール方針」節）では `DataFormatter#formatCellValue(cell)` を推奨している。`001` が整数 `1` として格納されている場合に `"1"` と出力されるか `"001"` と出力されるかの違いが生じる可能性があるため、**`DataFormatter` の使用が推奨される。**

### #16 空シートの表現

本スキーマは各トップレベルキーが省略可能であるため、空シートは単に空 YAML ファイル（`{}`）または対応するセクションキーを省略することで表現できる。nablarch-test-data-converter の `sections: []` 方式は `sheetName:` への依存と合わせて廃止すればよい。

---

## 4. 総合評価

### 整合性の評価

nablarch-test-data-converter の YAML 出力形式は、本スキーマと**構造的に大きく異なる**。差分の数・規模ともに「微調整」ではなく「設計の根本的な差異」に相当する。

| 差異の種類 | 差分数 |
|---|---|
| 本スキーマに合わせるべき（重大） | 12件（#1, #2, #3, #4, #5, #6, #7, #8, #9, #11, #12, #16） |
| 要議論 | 2件（#10, #13） |
| どちらでもよい / 細部 | 2件（#14, #15） |

### 根本的な設計差異の要約

nablarch-test-data-converter は**「Excel の構造をできるだけそのまま YAML に写す」**という方針で設計されており、以下の特徴を持つ。

- `sections:` 配列に全データを `dataType` 付きで混在させる（Excel シートの行の順序を保持）
- `columns:` リストを明示的に保持する（Excel ヘッダ行の構造を保持）
- ファイル系のフィールド型・長さを保持しない（Excel の型・長さ行を省略）
- ディレクティブ行をデータ行と同列に扱う

本スキーマは**「NTF のデータ構造を YAML で直接表現する」**という方針で設計されており、NTF のパーサ（`BasicTestDataParser` 等）が直接消費できる形式を目指している。

### 今後の対応方針

nablarch-test-data-converter の出力 YAML をそのまま NTF の YAMLアダプタで読み込むことは**できない**。両者の形式は互換性がないため、以下のいずれかの対応が必要である。

1. **nablarch-test-data-converter を修正して本スキーマ形式を出力するように変更する**（推奨）
   - 修正コストは大きいが、NTF との統合が直接的になる
   - ファイル系（固定長・可変長）のフィールド型・長さ情報を Excel から読み取って出力する機能が必要
   - マーカーカラムの扱い（#10）とドキュメントシート対応（#13）は別途設計決定が必要

2. **本スキーマを nablarch-test-data-converter 出力形式に合わせて修正する**（非推奨）
   - NTF パーサとの整合性が失われる
   - ファイル系のフィールド型・長さが表現できないという根本的な欠陥が残る

3. **nablarch-test-data-converter の出力を中間フォーマットとして扱い、本スキーマ形式への変換ステップを追加する**（回避策）
   - 2段階変換（Excel → nablarch-test-data-converter 形式 → 本スキーマ形式）になり複雑
   - ただしファイル系のフィールド型・長さは nablarch-test-data-converter が出力しないため変換不可能

**結論: 対応方針1（nablarch-test-data-converter を本スキーマ形式に合わせて修正する）を採用し、段階的に修正することを推奨する。特に #7（ファイル系のフィールド型・長さの出力）は Excel から情報を正しく読み取る実装が必要であり、設計上最も重要な修正点である。**
