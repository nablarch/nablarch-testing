# nablarch-testing 

## 変更時に併せて対応すること

YAML 形式のテストデータを読む [nablarch-testing-yaml](https://github.com/nablarch/nablarch-testing-yaml) と、Excel 形式と YAML 形式のテストデータを相互に変換する [nablarch-testing-converter](https://github.com/nablarch/nablarch-testing-converter) は、本リポジトリのテストデータの読み込みに依存している。次の変更をした場合は、あわせて対応すること。

- テストデータの読み込みやインタープリタに機能（新しい記法など）を足した場合は、nablarch-testing-yaml と nablarch-testing-converter を対応させる。本リポジトリのテストデータを写して、Excel 形式と YAML 形式の読み込み結果を比べているテストがあれば、その写しも更新する。
- テストデータのパーサの外に、テストデータの形式（Excel 形式・YAML 形式）で処理を分ける箇所を足した場合は、nablarch-testing-yaml にテストを足す。
