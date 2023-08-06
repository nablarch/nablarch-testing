package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;


public class ValidationTestContextTest {

    private List<Message> messages;

    @Before
    public void setup(){
        messages = new ArrayList<Message>();
        messages.add(new Message(MessageLevel.ERROR, new MockStringResource()));
    }


    @Test
    public void コンテキストからメッセージを取得できること() {
        // setup
        ValidationTestContext context = new ValidationTestContext(messages);

        // execute, verify
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void メッセージが存在するとき検証失敗となること() {
        // setup
        ValidationTestContext context = new ValidationTestContext(messages);

        // execute, verify
        assertFalse(context.isValid());
    }

    @Test
    public void メッセージが存在しないとき検証成功となること() {
        // setup
        ValidationTestContext context = new ValidationTestContext(new ArrayList<Message>());

        // execute, verify
        assertTrue(context.isValid());
    }

    static class MockStringResource implements StringResource {

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getValue(Locale locale) {
            return null;
        }
    }


}