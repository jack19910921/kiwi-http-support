package com.vip.study.http.support.enums;

import org.springframework.util.Assert;

/**
 * Created by jack on 16/7/31.
 */
public enum RequestMethod {
    POST("post"), GET("get"), PUT("put"), DELETE("delete");

    private String label;

    RequestMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RequestMethod determineRequestMethodByLabel(String label) {
        Assert.notNull(label, "label is required");

        for (RequestMethod p : RequestMethod.values()) {
            if (p.getLabel().equalsIgnoreCase(label))
                return p;
        }

        return POST;
    }
}
