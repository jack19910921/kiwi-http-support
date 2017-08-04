package org.kiwi.http.support.spi;

/**
 * Created by jack on 16/10/1.
 */
public abstract class ConfigProvider {

    public abstract String get(String key, String defaultValue);
}
