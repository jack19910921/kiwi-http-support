package org.kiwi.http.support;

import org.kiwi.http.support.exception.HttpException;

/**
 * Created by jack on 16/7/31.
 */
public interface HttpCallback<T> {
    T doParseResult(String result) throws HttpException;
}
