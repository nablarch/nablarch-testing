package nablarch.test.core.entity;

import nablarch.core.message.Message;

import java.util.List;

public class ValidationTestContext {

    private final List<Message> messages;

    public ValidationTestContext(List<Message> messages) {
        this.messages = messages;
    }

    public boolean isValid() {
        return messages.isEmpty();
    }

    public List<Message> getMessages() {
        return messages;
    }
}
