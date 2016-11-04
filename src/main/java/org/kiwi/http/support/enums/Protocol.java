package org.kiwi.http.support.enums;

import org.springframework.util.Assert;

/**
 * Created by jack on 16/8/1.
 */
public enum Protocol {
    HTTP("http"), HTTPS("https");

    private String text;

    Protocol(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static Protocol determineProtocolByText(String text) {
        Assert.notNull(text, "text is required");
        for (Protocol p : Protocol.values()) {
            if (p.getText().equalsIgnoreCase(text))
                return p;
        }
        return HTTP;
    }
}
