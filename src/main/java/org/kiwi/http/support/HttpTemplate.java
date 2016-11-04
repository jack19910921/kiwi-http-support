package org.kiwi.http.support;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kiwi.http.support.cons.HttpConstant;
import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.enums.error.HttpErrorEnum;
import org.kiwi.http.support.exception.HttpException;
import org.kiwi.util.ReflectUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by jack on 16/7/29.
 */
public class HttpTemplate extends HttpConfigurator implements HttpOperations {

    public HttpTemplate() {
    }

    private HttpTemplate(Builder builder) {
        this.protocol = builder.protocol;
        this.contentType = builder.contentType;
        this.charset = builder.charset;
        this.requestMethod = builder.requestMethod;
    }

    public <T> T doPost(String url, Map<String, String> params, HttpCallback<T> action)
            throws HttpException {
        return execute(url, params, RequestMethod.POST, action);
    }

    public <T> T doPost(String url, Object params, HttpCallback<T> action)
            throws HttpException {
        return execute(url, params, RequestMethod.POST, action);
    }

    public <T> T doGet(String url, HttpCallback<T> action)
            throws HttpException {
        return execute(url, null, RequestMethod.GET, action);
    }

    public <T> T execute(String url, Object params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        return execute(url, params != null ? ReflectUtil.convertJavaBean2Map(params) : Maps.<String, String>newHashMap(), method, action);
    }

    public <T> T execute(String url, Map<String, String> params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        try {
            //1.resolve immutable map problem
            params = params != null ? Maps.newHashMap(params) : Maps.<String, String>newHashMap();

            //2.build sign and other common param into params map
            doSign(params, this.charset);

            //3.create http or https client
            httpclient = newHttpClient(this.protocol);

            //4.send request
            response = doExecute(httpclient, url, params, method);

            //5.consume response
            if (response == null || response.getEntity() == null) {
                throw new HttpException(HttpErrorEnum.RESPONSE_IS_EMPTY.getErrorCode(),
                        HttpErrorEnum.RESPONSE_IS_EMPTY.getErrorMessage());
            }

            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, charset);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ErrorMessage:" + response.toString());
                }

                throw new HttpException(HttpErrorEnum.RESPONSE_FAILURE.getErrorCode(),
                        HttpErrorEnum.RESPONSE_FAILURE.getErrorMessage() + "【StatusCode=" + response.getStatusLine().getStatusCode() + ",\tReasonPhrase:" + response.getStatusLine().getReasonPhrase() + "】");

            }

            //6.invoke callback
            return action.doParseResult(result);

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("http invoke has some problem.", e);
            }

            if (e instanceof HttpHostConnectException) {
                //// TODO: 2016/11/4 need retry
                throw new HttpException(HttpErrorEnum.CONNECTION_REFUSED.getErrorCode(),
                        HttpErrorEnum.CONNECTION_REFUSED.getErrorMessage());
            }

            if (e instanceof HttpException) {
                throw (HttpException) e;
            }

            throw new HttpException(HttpErrorEnum.SYSTEM_INTERNAL_ERROR.getErrorCode(),
                    HttpErrorEnum.SYSTEM_INTERNAL_ERROR.getErrorMessage());
        } finally {
            try {
                if (response != null) response.close();
                if (httpclient != null) httpclient.close();
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("release http connection has some problem.", e);
                }
            }
        }
    }

    private void doSign(Map<String, String> params, String charset) {
        if (this.signProvider != null) {
            this.signProvider.doSign(params, charset);
        }
    }

    private CloseableHttpResponse doExecute(CloseableHttpClient httpclient, String url, Map<String, String> params, RequestMethod method)
            throws Exception {
        if (method == RequestMethod.POST) {
            return doPostInternal(httpclient, url, params);
        } else if (method == RequestMethod.GET) {
            return doGetInternal(httpclient, url);
        }

        throw new HttpException(HttpErrorEnum.UNSUPPORTED_REQUEST_METHOD.getErrorCode(),
                HttpErrorEnum.UNSUPPORTED_REQUEST_METHOD.getErrorMessage());
    }

    private CloseableHttpResponse doGetInternal(CloseableHttpClient httpclient, String url)
            throws Exception {
        HttpGet httpGet = new HttpGet(url);

        return httpclient.execute(httpGet);
    }

    private CloseableHttpResponse doPostInternal(CloseableHttpClient httpclient, String url, Map<String, String> params)
            throws Exception {
        HttpPost httpPost = new HttpPost(url);

        List<NameValuePair> requestParams = Lists.newArrayList();
        for (Entry<String, String> entry : params.entrySet()) {
            requestParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        if (requestParams.size() > 0) {
            UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(requestParams, this.charset);
            httpPost.setEntity(uefEntity);
        }

        return httpclient.execute(httpPost);
    }

    private CloseableHttpClient newHttpClient(Protocol protocol) throws Exception {
        if (protocol == Protocol.HTTP) {
            return HttpClients.createDefault();
        } else {
            return newHttpsClient();
        }
    }

    private CloseableHttpClient newHttpsClient() throws Exception {
        X509TrustManager x509mgr = HttpsSupport.newX509TrustManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(HttpConstant.DEFAULT_CONNECT_TIME)
                .setConnectTimeout(HttpConstant.DEFAULT_CONNECT_TIME)
                .build();

        SSLContext sslContext = HttpsSupport.newSSLContext(x509mgr);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        return HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static final class Builder {

        Protocol protocol = Protocol.HTTP;
        String contentType = HttpConstant.DEFAULT_CONTENT_TYPE;
        String charset = HttpConstant.DEFAULT_CHARSET;
        RequestMethod requestMethod = RequestMethod.POST;

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder charset(String charset) {
            this.charset = charset;
            return this;
        }

        public Builder requestMethod(RequestMethod requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public HttpTemplate build() {
            return new HttpTemplate(this);
        }
    }
}
