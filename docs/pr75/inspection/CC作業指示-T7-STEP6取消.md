# CC作業指示: STEP6 の取り消し（既存コード変更を全て戻す）

方針を変更する。STEP6 で行った「既存テストクラスへの @RunWith 付与」と「製品コード（src/main）への変更」を全て元に戻す。理由は後述。新方式の実装指示は別途出すので、本指示では戻す作業のみ行う。

## 背景（なぜ戻すか）
STEP6 では、Excel と YAML を同一プロセスで実行するために、製品コード（src/main）にテスト専用のキャッシュクリアメソッド等を追加した。これは「テストのために製品コードを変更する」ことであり、認められない。製品コードを一切変更しない方式に作り直すため、STEP6 の変更を取り消す。

## 戻す対象（STEP6 コミット: fe43dd6, 5118fc4, fc0ecd9）

### 1. 製品コード（src/main）の変更を全て元に戻す
以下のファイルへの STEP6 変更を取り消し、STEP5 完了時点（コミット a6c1a98）の状態に戻す。追加された clearCacheForTest 系メソッド、BinaryFileInterpreter の fallbackPath 追加、その他 STEP6 で入れた製品コード変更を全て除去する。
- src/main/java/nablarch/test/core/messaging/RequestTestingSendSyncSupport.java
- src/main/java/nablarch/test/core/messaging/SendSyncSupport.java
- src/main/java/nablarch/test/core/reader/BasicTestDataParser.java
- src/main/java/nablarch/test/core/reader/DataFileParser.java
- src/main/java/nablarch/test/core/reader/ListMapParser.java
- src/main/java/nablarch/test/core/reader/PoiXlsReader.java
- src/main/java/nablarch/test/core/reader/TableDataParser.java
- src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java
- src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java
- src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java
- src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java
- src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java
- src/main/java/nablarch/test/core/reader/yaml/YamlTestDataParser.java
- src/main/java/nablarch/test/core/util/interpreter/BinaryFileInterpreter.java
- src/main/java/nablarch/test/core/util/interpreter/QuotationTrimmer.java
- src/main/java/nablarch/test/tool/converter/xls/XlsFormatReader.java
- src/main/java/nablarch/test/tool/converter/yaml/YamlFormatWriter.java

注意: これらのうち STEP2（NPE 修正）・STEP3（enum 化）・STEP4（パッケージ移動）で正当に入った変更は残す。戻すのは STEP6（コミット fe43dd6 以降）で入った差分のみ。判断に迷う場合は、対象ファイルを STEP5 完了時点（a6c1a98）の内容と比較し、STEP6 で増えた差分のみを除去する。

### 2. 既存テストクラス（src/test）の @RunWith 付与を全て戻す
STEP6 で 18 クラスに付与した `@RunWith(NtfTestdataTestRunner.class)` を削除し、各クラスを STEP6 前の状態（@RunWith なし、または元の @RunWith(DatabaseTestRunner.class)）に戻す。テストロジック本体・テストデータへの変更も STEP6 分は戻す。
対象: STEP6 で変更された src/test 配下の全テストクラス（BatchRequestTestSupportTest, DBtoDBBatchSampleTest, FileToFileBatchSampleTest, SimpleBatchSampleTest, DbAccessTestSupportTest, EntityTestSupportTest, TestBeanTest, TestEntityTest, FileSupportTest, FileSupportWithDbLessTestDataParserTest, AbstractHttpRequestTestTemplateTest, MessagingReceiveTestSupportTest, MessagingRequestTestSupportTest, RequestTestingMessagingClientTest, RequestTestingMessagingContextTest, RequestTestingSendSyncBatchTest, RequestTestingSendSyncSupportTest, TestSupportTest, および関連テストデータ・派生テスト）。

### 3. NtfTestdataTestRunner（STEP5 で作成した Runner）の扱い
本指示では削除しない。残したまま（どのテストからも @RunWith で参照されない状態になる）。新方式の設計後に、改めて扱いを指示する。

## 完了条件
- src/main（製品コード）が STEP5 完了時点（a6c1a98）と同一になっている（STEP2/3/4 の正当な変更は保持、STEP6 分のみ除去）。
- 既存テストクラスから STEP6 で付与した @RunWith(NtfTestdataTestRunner.class) が全て除去され、STEP6 前の状態に戻っている。
- mvn test 全クラスで、STEP5 完了時点のベースラインと同じ結果（新規失敗なし）。
- 生成された target 配下の YAML や、STEP6 で追加したテストデータが残っていない。

## このステップのルール
- 着手前・完了時に mvn test 全クラスを実行し、STEP5 完了時点のベースラインと一致することを確認する。
- 製品コード（src/main）に、テスト専用のメソッド・引数・クラスが残っていないことを確認する。
- 迷う場合は a6c1a98 との差分で判断し、勝手な追加変更をしない。
