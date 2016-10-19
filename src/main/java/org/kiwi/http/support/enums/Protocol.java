package org.kiwi.http.support.enums;

import org.springframework.util.Assert;

/**
 * Created by jack on 16/8/1.
 */
public enum Protocol {
    HTTP("http"), HTTPS("https");

    private String label;

    Protocol(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Protocol determineProtocolByLabel(String label) {
        Assert.notNull(label, "label is required");

        for (Protocol p : Protocol.values()) {
            if (p.getLabel().equalsIgnoreCase(label))
                return p;
        }

        return HTTP;
    }
}
