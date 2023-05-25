package nablarch.test.core.http;

import static nablarch.test.Assertion.fail;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.upload.PartInfo;
import nablarch.fw.web.upload.util.UploadHelper;
import nablarch.test.RepositoryInitializer;
import nablarch.test.Trap;
import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.tool.Hereis;
import nablarch.test.tool.sanitizingcheck.util.FileUtil;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 * {@link AbstractHttpRequestTestTemplate}のテストクラス。
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class AbstractHttpRequestTestTemplateTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/http/http-test-configuration.xml");

    /** アップロード先ディレクトリ */
    private static File workDir = new File("work");

    /** アップロード先ディレクトリの準備 */
    @BeforeClass
    public static void prepareUploadDir() {
        workDir.delete();
        workDir.mkdir();
    }

    /** 
     * アップロード先ディレクトリの削除 
     * HttpRequestTestSupportをデフォルトに復元する
     */
    @AfterClass
    public static void deleteUploadDir() {
        workDir.delete();
        HttpRequestTestSupport.resetHttpServer();
    }

    @Before
    public void setUp() {
        HttpRequestTestSupport.resetHttpServer();
    }

    /** テスト対象 */
    private AbstractHttpRequestTestTemplate<TestCaseInfo> target;

    /**
     * テスト対象機能にてリクエストパラメータが変更される場合、
     * テスト側で変更内容を検証できること。
     * <p/>
     * EXCELで設定したCookieが正しく設定されていることを確認する。
     */
    @Test
    public void testAssertRequestParameterModified() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {

            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // リクエストパラメータを変更する
                req.setParam("foo", "modified");

                // Cookieをリクエストスコープに移送
                ctx.setRequestScopedVar("requestScopeLang", req.getCookie()
                        .get("lang"));
                ctx.setRequestScopedVar("requestScopeHoge", req.getCookie()
                        .get("cookieHoge"));

                // ボディを設定する。
                String body = Hereis.string();
                /*
                <html>
                <head><title>test</title></head>
                <body><p>Hello, World!</p></body>
                </html>*/
                HttpResponse res = new HttpResponse().write(body); // 200 OK

                // ステータスコードを移送
                HttpRequestTestSupport.getTestSupportHandler()
                        .setStatusCode(res.getStatusCode());

                return res;
            }
        });


        // 実行
        target.execute("testAssertRequest", new BasicAdvice() {
            /** 実行前のコールバック */
            @Override
            public void beforeExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                // リクエストパラメータに新たな値は設定されていないこと
                HttpRequest request = testCaseInfo.getHttpRequest();
                assertThat(request.getParam("foo")[0], is("original"));
                assertThat(request.getCookie()
                        .get("lang"), is("en"));
                assertThat(request.getCookie()
                        .get("cookieHoge"), is("hoge-value"));
            }

            /** 実行後のコールバック */
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                // リクエストパラメータに新たな値が設定されていること
                HttpRequest request = testCaseInfo.getHttpRequest();
                assertThat(request.getParam("foo")[0], is("modified"));

                // リクエストスコープに移送したCookieのアサート
                assertThat(context.<String>getRequestScopedVar("requestScopeLang"), is("en"));
                assertThat(context.<String>getRequestScopedVar("requestScopeHoge"), is("hoge-value"));
            }
        });
    }


    /**
     * テストフレームワーク側のアサートが失敗した場合、
     * そのテストショット実行後のコールバックが起動されないこと。
     */
    @Test
    public void testAssertionCalledBeforeCallback() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {

            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext unused) {
                HttpResponse res = new HttpResponse(400);
                // ステータスコードを移送
                HttpRequestTestSupport.getTestSupportHandler()
                        .setStatusCode(res.getStatusCode());
                return res;
            }
        });

        new Trap("ステータスコードが期待値200でないのでassertAllが失敗する。") {
            @Override
            protected void shouldFail() throws Exception {
                target.execute("testAssertRequest", new BasicAdvice() {
                    /** 実行後のコールバック */
                    @Override
                    public void afterExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                        throw new IllegalStateException(
                                "ステータスコード比較失敗でテストが終了するため、ここには到達しない。");
                    }
                });
            }
        }.capture(AssertionError.class)
                .whichMessageEndsWith("expected:<[2]00> but was:<[4]00>");

    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    /**
     * ダウンロードの場合にHTMLチェックツールが実行されないこと。
     * HTMLチェックツールをONにしてリクエスト単体テストを実施する。
     */
    @Test
    public void testAssertDownloadResponseWithHtmlCheckOn() {
        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-with-htmlcheck.xml"
        );


        HttpRequestHandler handler = new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse().setContentType("text/plain; charset=UTF-8");
            }
        };

        target = new MockHttpRequestTestTemplate(getClass(), handler) {
            @Override
            protected HttpServer createHttpServer() {
                return new HttpServerForTesting() {
                    @Override
                    public File getHttpDumpFile() {
                        try {
                            return Hereis.file(tempDir.newFile("テスト一時ファイル.txt").getAbsolutePath());
                            /*
                            abcdefghij*/
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };

        // 実行
        target.execute("testAssertDownloadResponse", false);
    }

    /**
     * レスポンスが空の場合にHTMLチェックツールが実行されず、正常終了すること。
     * HTMLチェックツールをONにしてリクエスト単体テストを実施する。
     */
    @Test
    public void testAssertDownloadResponseEmptyWithHtmlCheckOn() {
        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-with-htmlcheck.xml"
        );


        HttpRequestHandler handler = new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse(200);
            }
        };

        target = new MockHttpRequestTestTemplate(getClass(), handler) {
            @Override
            protected HttpServer createHttpServer() {
                return new HttpServerForTesting() {
                    @Override
                    public File getHttpDumpFile() {
                        try {
                            return Hereis.file(tempDir.newFile("テスト一時ファイル.txt").getAbsolutePath());
                            /*
                            abcdefghij*/
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };

        // 実行
        target.execute("testAssertDownloadEmptyResponse", false);
    }

    /** TestCasesが空であった場合、例外が発生すること。 */
    @Test
    public void testGetEmptyTestCase() {
        RepositoryInitializer.initializeDefaultRepository();

        target = createDefaultMock();
        try {
            // ExcelのLIST_MAP=testCasesは空
            target.execute("testGetEmptyTestCase");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("testShots (LIST_MAP=testShots) must have one or more test " +
                    "shots"));
        }
    }

    /**
     * リクエストスコープの値が2段階にネストしている場合に、
     * ネストしたオブジェクトでアサートされること。
     */
    @Test
    public void testNestedRequestScopeVar() {
        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                Map<String, String> foo = new HashMap<String, String>();
                foo.put("bar", "buz");
                ctx.setRequestScopedVar("foo", foo);
                return new HttpResponse();
            }
        });
        // 実行
        target.execute("testNestedRequestScopeVar");
    }

    /**
     * リクエストスコープの値が取得できない場合にNullPointerExceptionが発生しないこと。<br/>
     * 
     */
    @Test
    public void testRequestScopeVar() {

        target = createDefaultMock();

        // 実行
        try {
            target.execute("testRequestScopeVar");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is(
                    "testRequestScopeVar_Shot1_リクエストパラメータが取得できない[foo] expected:<[bar]> but was:<[null]>"));
        }
    }

    /**
     * リクエストスコープの値が2段階にネストしており、
     * ネストしたオブジェクトが要素数０の配列の場合、以下の内容がメッセージに含まれていること。
     * <ul>
     * <li>その要素を取得した際のリクエストスコープキー名</li>
     * <li>実際の値が空の配列であること</li>
     * </ul>
     */
    @Test
    public void testEmptyArrayRequestScopeVar() {

        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                Map<String, String[]> foo = new HashMap<String, String[]>();
                foo.put("bar", new String[0]);
                ctx.setRequestScopedVar("foo", foo);
                return new HttpResponse();
            }
        });

        // 実行
        try {
            target.execute("testNestedRequestScopeVar");
            fail();
        } catch (AssertionError e) {
            // キー名が表示されること
            assertThat(e.getMessage(), containsString("[foo.bar]"));
            // 実際の値が空の配列であることが示されていること
            assertThat(e.getMessage(), containsString("actual value is empty array"));
        }
    }

    /**
     * リクエストスコープの値が2段階にネストしており、
     * ネストしたオブジェクトが複数の要素をもつ配列の場合、
     * 先頭のオブジェクトでアサートされること。
     */
    @Test
    public void testArrayInRequestScopeVar() {

        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                Map<String, String[]> foo = new HashMap<String, String[]>();
                foo.put("bar", new String[] {"buz", "hoge"});
                ctx.setRequestScopedVar("foo", foo);
                return new HttpResponse();
            }
        });
        // 実行
        target.execute("testArrayInRequestScopeVar");
    }

    /**
     * リクエストスコープの値が2段階にネストしており、
     * ネストしたオブジェクトが複数の要素をもつ配列の場合、
     * 先頭のオブジェクトでアサートされること。
     * アサートが失敗した場合に、
     */
    @Test
    public void testArrayInRequestScopeVarFail() {
        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                Map<String, String[]> foo = new HashMap<String, String[]>();
                foo.put("bar", new String[] {"buz", "hoge"});
                ctx.setRequestScopedVar("foo", foo);
                HttpResponse res = new HttpResponse(); // 200 OK
                HttpRequestTestSupport.getTestSupportHandler()
                        .setStatusCode(res.getStatusCode());
                return res;
            }
        });
        // 実行
        try {
            target.execute("testArrayInRequestScopeVarFail");
            fail();
        } catch (AssertionError e) {
            // キー名が表示されること
            assertThat(e.getMessage(), containsString("[foo.bar]"));
            // メッセージに以下の文言が含まれていないこと
            assertFalse(e.getMessage()
                    .contains("actual value is empty array"));
        }
    }

    /** ${attach:filepath}でファイルアップロードを擬似できること。 */
    @Test
    public void testUpload() {

        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // アップロードされたファイルの内容を確認
                checkUploaded(req);
                HttpResponse res = new HttpResponse(); // 200 OK
                HttpRequestTestSupport.getTestSupportHandler()
                        .setStatusCode(res.getStatusCode());
                return res;
            }
        });
        target.execute("testUpload");
    }

    /** ${attach:filepath}でファイルアップロードを擬似できること。 */
    @Test
    public void testUpload2() {

        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                List<PartInfo> file1 = req.getPart("file1");
                UploadHelper helper = new UploadHelper(file1.get(0));
                helper.moveFileTo("move", "file1.png");
                return new HttpResponse();
            }
        });

        target.execute("testUpload2");
        List<Map<String, String>> listMap = target.getListMap("testUpload2", "EXPTECTED_FILE1");

        // 移動したファイルが、期待値に設定された${binaryFile}とバイト数が一致すること
        // 期待値のファイルサイズは、16進数表記の文字列になっているので長さが倍になるのでとりあえずこの形でアサート
        assertThat(new File("tmp/file1.png").length() * 2, is((long) listMap.get(0)
                .get("FILE_1")
                .length()));
        // 移動元のファイルは削除されずに残っていること
        assertThat(new File("src/test/java/nablarch/test/core/http/upload2.png").exists(), is(true));
    }

    /** ${attach:filepath}の閉じ括弧が存在しない場合、例外が発生すること。 */
    @Test
    public void testUploadFail() {

        target = createDefaultMock();
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.execute("testUploadFail");
            }
        }.capture(IllegalStateException.class)
                .whichMessageContains("missing closed parenthesis");

    }


    private void checkUploaded(HttpRequest req) {
        // リクエストパラメータにアップロードされたファイル名が設定されていること
        {
            String[] file1 = req.getParam("file1");
            assertThat(file1.length, is(2));
            assertThat(file1[0], is("foo.txt"));
            assertThat(file1[1], is("bar.txt"));
        }
        {
            String[] file2 = req.getParam("file2");
            assertThat(file2.length, is(1));
            assertThat(file2[0], is("buz.txt"));
        }

        // ファイル以外のパラメータは、普通に取得できること。
        String[] param = req.getParam("param");
        assertThat(param, notNullValue());
        assertThat(param.length, is(1));
        assertThat(param[0], is("aaa"));

        // ------------- 1つめのパラメータ ------------------
        List<PartInfo> file1 = req.getPart("file1");
        // １つのキーで、２つのファイルがアップロードされていること
        assertThat(file1.size(), is(2));
        {
            PartInfo foo = file1.get(0);
            // ファイル名が正しいこと
            assertThat(foo.getFileName(), is("foo.txt"));
            // ファイルの中身が想定通りであること
            List<String> actual = readAll(foo.getInputStream(), Charset.forName("Windows-31J"));
            assertThat(actual, is(Arrays.asList("1000100010hello     ", "1000200020good bye. ")));
        }
        {
            PartInfo bar = file1.get(1);
            // ファイル名が正しいこと
            assertThat(bar.getFileName(), is("bar.txt"));
            // ファイルの中身が想定通りであること
            List<String> actual = readAll(bar.getInputStream(), Charset.forName("Windows-31J"));
            assertThat(actual, is(Arrays.asList(
                    "'1',''",
                    "'2','ﾍﾝｺｳｺﾞ ｶﾅｼﾒｲ','変更後　漢字氏名','Henkougo romaji','1',''",
                    "'8','','1',''")));
        }
        // ------------- 2つめのパラメータ ------------------
        List<PartInfo> file2 = req.getPart("file2");
        assertThat(file2.size(), is(1));
        {
            PartInfo buz = file2.get(0);
            // ファイル名が正しいこと
            assertThat(buz.getFileName(), is("buz.txt"));
            // ファイルの中身が想定通りであること
            List<String> actual = readAll(buz.getInputStream(), Charset.forName("Windows-31J"));
            assertThat(actual, is(Arrays.asList("1000100010konichiha ", "1000200020sayonara  ")));
        }
    }

    private static List<String> readAll(InputStream in, Charset charset) {
        BufferedReader reader = null;
        List<String> result = new ArrayList<String>();
        String line;
        try {
            reader = new BufferedReader(new InputStreamReader(in, charset));
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
    }

    /**
     * Cookieの設定に関連したテスト。
     */
    @Test
    public void testCookieNormal() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // ボディを設定する。
                String body = Hereis.string();
                // Cookieをリクエストスコープに移送
                /*
                <html>
                  <head><title>test</title></head>
                  <body><p>Hello, World!</p></body>
                </html>
                */
                return new HttpResponse().write(body);
            }
        });

        // 実行
        target.execute("testCookieNormal", new BasicAdvice() {
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo,
                    ExecutionContext context) {
                String no = testCaseInfo.getTestCaseNo();
                HttpRequest request = testCaseInfo.getHttpRequest();

                if (no.equals("3")) {
                    // 3ケース目は、Cookieを設定していないので空のはず
                    assertThat(StringUtil.isNullOrEmpty(request.getHeader(
                            "Cookie")), is(true));
                } else {
                    // それ以外は、Excelに設定したCookieが設定されているはず
                    Map<String, String> map = target.getListMap(
                            testCaseInfo.getSheetName(), "expectedCookie_" + no)
                            .get(0);
                    HttpCookie httpCookie = request.getCookie();

                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        assertThat(httpCookie.get(entry.getKey()), is(entry.getValue()));
                    }
                }
            }
        });
    }

    /**
     * Cookieの設定に関連したテスト。
     * <p/>
     * Cookie列に定義された参照先が存在しない場合
     */
    @Test
    public void testCookieFailed() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // ボディを設定する。
                String body = Hereis.string();
                // Cookieをリクエストスコープに移送
                /*
                <html>
                <head><title>test</title></head>
                <body><p>Hello, World!</p></body>
                </html>*/
                return new HttpResponse().write(body); // 200 OK
            }
        });

        // 実行
        try {
            target.execute("testCookieFailed");
            fail("does not run...");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(
                    "Cookie LIST_MAP was not found. name = [cookie0]"));
        }
    }

    @Test
    public void testHttpMethod() {
        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                context.setRequestScopedVar("http_method", request.getMethod());
                return new HttpResponse();
            }
        });

        target.execute("testHttpMethod");
    }

    /**
     * urlからクエリパラメータを取得する。
     * テスト用であるため、urlの厳密な解析は実施していない。
     * @param url
     * @return
     */
    private Map<String, String> getQueryParams(String url) {
        String[] split = url.split("\\?");
        if (split.length == 1) {
            return null;
        }

        String[] params = split[1].split("&");
        Map<String, String> result = new HashMap<String, String>();
        for (String param : params) {
            String[] keyValue = param.split("=");
            if(keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            } else {
                result.put(keyValue[0], "");
            }
        }

        return result;
    }


    /**
     * クエリパラメータの設定が正しくなされていることを検証する。
     */
    @Test
    public void testQueryParamsNormal() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // ボディを設定する。
                String body = Hereis.string();
                // Cookieをリクエストスコープに移送
                /*
                <html>
                  <head><title>test</title></head>
                  <body><p>Hello, World!</p></body>
                </html>
                */
                return new HttpResponse().write(body);
            }
        });

        // 実行
        target.execute("testQueryParamsNormal", new BasicAdvice() {
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo,
                    ExecutionContext context) {
                String no = testCaseInfo.getTestCaseNo();
                HttpRequest request = testCaseInfo.getHttpRequest();

                Map<String, String> queryParamsMap = getQueryParams(request.getRequestUri());
                if (no.equals("3")) {
                    // 3ケース目は、クエリパラメータを設定していないので空のはず
                    assertNull(queryParamsMap);
                } else {
                    // それ以外は、Excelに設定したCookieが設定されているはず
                    assert queryParamsMap != null;
                    Map<String, String> map = target.getListMap(
                            testCaseInfo.getSheetName(), "expectedQueryParams" + no)
                            .get(0);

                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        if (entry.getValue() == null) {
                            assertNull(queryParamsMap.get(entry.getKey()));
                        } else {
                            assertEquals(entry.getValue(), queryParamsMap.get(entry.getKey()));
                        }
                    }
                }
            }
        });
    }

    /**
     * Cookie列に定義された参照先が存在しない場合、例外が発生することを検証する。
     */
    @Test
    public void testQueryParamsFailed() {

        // テスト用にサブクラス化
        target = createMock(new HttpRequestHandler() {
            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // ボディを設定する。
                String body = Hereis.string();
                // Cookieをリクエストスコープに移送
                /*
                <html>
                <head><title>test</title></head>
                <body><p>Hello, World!</p></body>
                </html>*/
                return new HttpResponse().write(body); // 200 OK
            }
        });

        // 実行
        try {
            target.execute("testQueryParamsFailed");
            fail("does not run...");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(
                    "Query parameter LIST_MAP was not found. name = [queryParams0]"));
        }
    }

     /**
     * {@link TestCaseInfo#setIsAssertApplicationMessageId(boolean)}が設定されている場合、
     * メッセージIDのアサートが行われていないことを検証する。
     */
    @Test
    public void testIgnoreMessageIdAssertion() {

        target = createMock(new HttpRequestHandler() {
            /** ステータス400のレスポンスを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse().setStatusCode(400);
            }
        });

        // 実行
        target.execute("testIgnoreMessageIdAssertion", new BasicAdvice() {
            /** isAssertApplicationMessageIdをfalseに設定する **/
            @Override
            public void beforeExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                testCaseInfo.setIsAssertApplicationMessageId(false);
            }
        });
    }

    /**
     * {@link TestCaseInfo#setIsAssertRequestScopeVar(boolean)}が設定されている場合、
     * リクエストスコープのアサートが行われていないことを検証する。
     */
    @Test
    public void testIgnoreRequestScopeAssertion() {


        target = createDefaultMock();

        // 実行
        target.execute("testIgnoreRequestScopeAssertion", new BasicAdvice() {
            /** isAssertRequestScopeVarをfalseに設定する **/
            @Override
            public void beforeExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                testCaseInfo.setIsAssertRequestScopeVar(false);
            }
        });
    }

    @Test
    public void testAssertSqlResultSet() throws Exception {

        VariousDbTestHelper.createTable(SearchResultAssertTest.class);

        target = createMock(new HttpRequestHandler() {

            /** ステータス200のレスポンスを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {

                /** 全件検索を行う */
                SqlResultSet rs = new SimpleDbTransactionExecutor<SqlResultSet>(getManager()) {
                    @Override
                    public SqlResultSet execute(AppDbConnection connection) {
                        return connection.prepareStatement(
                                "SELECT * FROM SEARCH_RESULT_ASSERT_TEST ORDER BY PK_COL1"
                        )
                                .retrieve();
                    }
                }.doTransaction();
                ctx.setRequestScopedVar("searchResult", rs);
                return new HttpResponse().setStatusCode(200);
            }
        });

        // 実行
        target.execute("testAssertSqlResultSet");
    }


    /**
     * Excelのセルに"\r"が含まれている場合、CRに変換された結果と比較されること。
     *
     * @see nablarch.test.core.util.interpreter.LineSeparatorInterpreter
     * @see nablarch.test.core.util.interpreter.LineSeparatorInterpreterTest
     */
    @Test
    public void testAssertTablesCRLF() {

        VariousDbTestHelper.createTable(CrlfTest.class);

        target = createMock(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                VariousDbTestHelper.setUpTable(new CrlfTest("12345", "A\r\nBC"));
                return new HttpResponse();
            }
        });

        // 実行
        target.execute("testAssertTablesCRLF");
    }

    /** @see MockHttpRequestTestTemplate */
    private AbstractHttpRequestTestTemplate<TestCaseInfo> createDefaultMock() {
        return new MockHttpRequestTestTemplate(getClass());
    }

    /** @see MockHttpRequestTestTemplate */
    private AbstractHttpRequestTestTemplate<TestCaseInfo> createMock(HttpRequestHandler handler) {
        return new MockHttpRequestTestTemplate(getClass(), handler);
    }

    /**
     * テスト用の{@link SimpleDbTransactionManager}を返却する。
     *
     * @return テスト用SimpleDbTransactionManager
     */
    private static SimpleDbTransactionManager getManager() {
        return SystemRepository.get(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST);
    }

    @Entity
    @Table(name = "SEARCH_RESULT_ASSERT_TEST")
    public static class SearchResultAssertTest {

        public SearchResultAssertTest() {
        }

        ;

        public SearchResultAssertTest(String pkCol1) {
            this.pkCol1 = pkCol1;
        }

        @Id
        @Column(name = "PK_COL1", length = 5, nullable = false)
        public String pkCol1;
    }

    @Entity
    @Table(name = "CRLF_TEST")
    public static class CrlfTest {

        public CrlfTest() {
        }

        ;

        public CrlfTest(String pkCol1, String col2) {
            this.pkCol1 = pkCol1;
            this.col2 = col2;
        }

        @Id
        @Column(name = "PK_COL1", length = 5, nullable = false)
        public String pkCol1;

        @Column(name = "COL2", length = 8)
        public String col2;
    }
}
