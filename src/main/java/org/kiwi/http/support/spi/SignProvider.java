package org.kiwi.http.support.spi;

import java.util.Map;

/**
 * Created by jack on 16/10/1.
 */
public abstract class SignProvider {
    public abstract void doSign(Map<String, String> param, String charset);
}
