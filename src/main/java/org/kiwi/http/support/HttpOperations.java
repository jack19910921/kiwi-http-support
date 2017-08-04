package org.kiwi.http.support;

import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.exception.HttpException;

import java.util.Map;

/**
 * Created by jack on 16/7/31.
 */
public interface HttpOperations {

    <T> T execute(String url, Map<String, String> params, RequestMethod method, HttpCallback<T> action) throws HttpException;

    <T> T execute(String url, Object params, RequestMethod method, HttpCallback<T> action) throws HttpException;

    <T> T executeXml(String url, String xml, HttpCallback<T> action) throws HttpException;

    <T> T doPost(String url, Map<String, String> params, HttpCallback<T> action) throws HttpException;

    <T> T doPost(String url, Object params, HttpCallback<T> action) throws HttpException;

    <T> T doGet(String url, HttpCallback<T> action) throws HttpException;

}
