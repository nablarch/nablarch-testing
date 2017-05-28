package nablarch.test.core.http;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link AbstractHttpRequestTestTemplate}でテスト対象のシート名に関する確認をするテストクラス。
 */
public class AbstractHttpRequestTestTemplateSheetNameTest extends AbstractHttpRequestTestTemplate<TestCaseInfo> {
    
    private String sheetName;
    private Advice<TestCaseInfo> advice;
    private boolean shouldSetUpDb;
    

    @Override
    protected String getBaseUri() {
        return "dummy";
    }

    @Test
    public void シート名を明示的に指定パターン１() throws Exception {
        execute("明示的に指定したシート名");
        assertThat(sheetName, is("明示的に指定したシート名"));
    }

    @Test
    public void シート名を明示的に指定パターン２() throws Exception {
        execute("明示的に指定したシート名２", true);
        assertThat(sheetName, is("明示的に指定したシート名２"));
        assertThat(shouldSetUpDb, is(true));
    }
    
    @Test
    public void シート名を明示的に指定パターン３() throws Exception {
        final Advice<TestCaseInfo> basicAdvice = new BasicAdvice();
        execute("明示的に指定したシート名３", basicAdvice);
        assertThat(sheetName, is("明示的に指定したシート名３"));
        assertThat(advice, sameInstance(basicAdvice));
    }
    
    @Test
    public void シート名を明示的に指定パターン４() throws Exception {
        final Advice<TestCaseInfo> basicAdvice = new BasicAdvice();
        execute("明示的に指定したシート名４", basicAdvice, true);
        assertThat(sheetName, is("明示的に指定したシート名４"));
        assertThat(shouldSetUpDb, is(true));
        assertThat(advice, sameInstance(basicAdvice));
    }
    
    @Test
    public void シート名は指定しないパターン１() throws Exception {
        execute();
        assertThat(sheetName, is("シート名は指定しないパターン１"));
    }
    
    @Test
    public void シート名は指定しないパターン２() throws Exception {
        execute(true);
        assertThat(sheetName, is("シート名は指定しないパターン２"));
        assertThat(shouldSetUpDb, is(true));
    }
    
    @Test
    public void シート名は指定しないパターン３() throws Exception {
        final Advice<TestCaseInfo> basicAdvice = new BasicAdvice();
        execute(basicAdvice);
        assertThat(sheetName, is("シート名は指定しないパターン３"));
        assertThat(advice, sameInstance(basicAdvice));
    }
    @Test
    public void シート名は指定しないパターン４() throws Exception {
        final Advice<TestCaseInfo> basicAdvice = new BasicAdvice();
        execute(basicAdvice, false);
        assertThat(sheetName, is("シート名は指定しないパターン４"));
        assertThat(advice, sameInstance(basicAdvice));
        assertThat(shouldSetUpDb, is(false));
    }

    @Override
    public void execute(final String sheetName, final Advice<TestCaseInfo> advice, final boolean shouldSetUpDb) {
        this.sheetName = sheetName;
        this.advice = advice;
        this.shouldSetUpDb = shouldSetUpDb;
    }
}