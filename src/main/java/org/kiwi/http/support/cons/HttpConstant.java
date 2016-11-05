package org.kiwi.http.support.cons;

/**
 * Created by jack on 16/10/1.
 */
public class HttpConstant {
    public static final String CONFIG_KEY_PREFIX = "http.config.";

    /**
     * config param
     */
    public static final String CONFIG_KEY_PROTOCOL = CONFIG_KEY_PREFIX + "protocol";
    public static final String CONFIG_KEY_REQUEST_METHOD = CONFIG_KEY_PREFIX + "requestMethod";
    public static final String CONFIG_KEY_CONTENT_TYPE = CONFIG_KEY_PREFIX + "contentType";
    public static final String CONFIG_KEY_CHARSET = CONFIG_KEY_PREFIX + "charset";

    /**
     * config param default value
     */
    public static final String DEFAULT_PROTOCOL = "http";
    public static final String DEFAULT_REQUEST_METHOD = "post";
    public static final String DEFAULT_CONTENT_TYPE = "application/json";
    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String DEFAULT_CONFIG_CLASS = "com.vip.xfd.account.components.ConfigManager";
    public static final String DEFAULT_CONFIG_METHOD_NAME = "getString";

    /**
     * ssl config
     */
    public static final int DEFAULT_CONNECT_TIME = -1;
    public static final String TLS = "TLS";

    /**
     * retry config
     */
    public static final int DEFAULT_RETRY_CNT = 1;
    public static final int DEFAULT_RETRY_INTERVAL = 1000;
    public static final boolean DEFAULT_RETRY_STAFF_IS_ON = false;
}
