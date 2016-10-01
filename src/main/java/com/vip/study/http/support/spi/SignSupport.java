package com.vip.study.http.support.spi;

import java.util.Map;

/**
 * Created by jack on 16/10/1.
 */
public interface SignSupport {
    void doSign(Map<String, String> param, String charset);
}
