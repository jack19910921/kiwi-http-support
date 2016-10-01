package com.vip.study.http.support.spi;

/**
 * Created by jack on 16/10/1.
 */
public abstract class ConfigProvider {
    public abstract String getString(String key, String defaultValue);
}
