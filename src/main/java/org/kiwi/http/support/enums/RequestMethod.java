package org.kiwi.http.support.enums;

import org.springframework.util.Assert;

/**
 * Created by jack on 16/7/31.
 */
public enum RequestMethod {
    POST("post"), GET("get"), PUT("put"), DELETE("delete");

    private String text;

    RequestMethod(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static RequestMethod determineRequestMethodByText(String text) {
        Assert.notNull(text, "text is required");
        for (RequestMethod p : RequestMethod.values()) {
            if (p.getText().equalsIgnoreCase(text))
                return p;
        }
        return POST;
    }
}
