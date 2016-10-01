package com.vip.study.http.support.enums;

import org.springframework.util.Assert;

/**
 * Created by jack on 16/8/9.
 */
public enum ParameterOrder {
    ASC("asc"), DESC("desc"), IMMUTABLE("immutable");

    private String label;

    ParameterOrder(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ParameterOrder determineParameterOrderByLabel(String label) {
        Assert.notNull(label, "label is required");

        for (ParameterOrder p : ParameterOrder.values()) {
            if (p.getLabel().equalsIgnoreCase(label))
                return p;
        }

        return IMMUTABLE;
    }
}
