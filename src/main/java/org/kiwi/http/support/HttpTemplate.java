package org.kiwi.http.support;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kiwi.http.support.enums.HttpError;
import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.exception.HttpException;
import org.kiwi.util.ReflectUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static org.kiwi.http.support.cons.HttpConstant.*;

/**
 * Created by jack on 16/7/29.
 */
public class HttpTemplate extends HttpConfigurer implements HttpOperations {

    public HttpTemplate() {
    }

    private HttpTemplate(Builder builder) {
        this.protocol = builder.protocol;
        this.contentType = builder.contentType;
        this.charset = builder.charset;
        this.requestMethod = builder.requestMethod;
        this.connectionRequestTimeout = builder.connectionRequestTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.connectTimeout = builder.connectTimeout;
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

    @Override
    public <T> T executeXml(String url, String xml, HttpCallback<T> action) throws HttpException {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        try {
            //create http or https client
            httpclient = newHttpClient(this.protocol);
            //send request
            response = doPostInternal(httpclient, url, xml);
            //consume response
            String result = doConsume(response);
            //invoke callback
            return action.doParseResult(result);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("http invoke has some problem.", e);
            }
            if (e instanceof HttpException) {
                throw (HttpException) e;
            }
            if (e instanceof UnknownHostException) {
                throw new HttpException(HttpError.UNKNOWN_HOST.getErrorCode(),
                        HttpError.UNKNOWN_HOST.getErrorMessage());
            }
            throw new HttpException(HttpError.SYSTEM_INTERNAL_ERROR.getErrorCode(),
                    HttpError.SYSTEM_INTERNAL_ERROR.getErrorMessage());
        } finally {
            close(httpclient, response);
        }
    }

    public <T> T execute(String url, Object params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        return execute(url, params != null ? ReflectUtil.convertJavaBean2Map(params) : new HashMap<>(), method, action);
    }

    public <T> T execute(String url, Map<String, String> params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        requested();
        try {
            //resolve immutable map problem and retry problem
            Map<String, String> paramMap = params != null ? new HashMap<>(params) : new HashMap<String, String>();
            //create http or https client
            httpclient = newHttpClient(this.protocol);
            //send request
            response = doExecute(httpclient, url, paramMap, method);
            //consume response
            String result = doConsume(response);
            //invoke callback
            return action.doParseResult(result);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("http invoke has some problem.", e);
            }
            if (ConnectException.class.isAssignableFrom(e.getClass())) {
                // need retry
                if (this.retryStaffIsOn && HttpConnectionHolder.getRetryCnt() < this.retryCnt) {
                    int retryCnt = HttpConnectionHolder.getAndIncrementRetryCnt();
                    if (logger.isDebugEnabled()) {
                        logger.debug("reconnect to server and retryCnt is {}", retryCnt);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(this.retryInterval);
                    } catch (InterruptedException e1) {
                        //do nothing
                    }
                    return execute(url, params, method, action);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("retry staff is not open or arrive limit retry cnt.");
                    }
                    throw new HttpException(HttpError.CONNECTION_REFUSED.getErrorCode(),
                            HttpError.CONNECTION_REFUSED.getErrorMessage());
                }
            }
            if (e instanceof HttpException) {
                throw (HttpException) e;
            }
            if (e instanceof UnknownHostException) {
                throw new HttpException(HttpError.UNKNOWN_HOST.getErrorCode(),
                        HttpError.UNKNOWN_HOST.getErrorMessage());
            }
            throw new HttpException(HttpError.SYSTEM_INTERNAL_ERROR.getErrorCode(),
                    HttpError.SYSTEM_INTERNAL_ERROR.getErrorMessage());
        } finally {
            doRelease();
            close(httpclient, response);
        }
    }

    private void requested() {
        HttpConnectionHolder.requested();
    }

    private String doConsume(CloseableHttpResponse response) throws HttpException, IOException {
        if (response == null || response.getEntity() == null) {
            throw new HttpException(HttpError.RESPONSE_IS_EMPTY.getErrorCode(),
                    HttpError.RESPONSE_IS_EMPTY.getErrorMessage());
        }
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, charset);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            if (logger.isDebugEnabled()) {
                logger.debug("ErrorMessage:" + response.toString());
            }
            throw new HttpException(HttpError.RESPONSE_FAILURE.getErrorCode(),
                    HttpError.RESPONSE_FAILURE.getErrorMessage() + "【StatusCode=" + response.getStatusLine().getStatusCode()
                            + ",\tReasonPhrase:" + response.getStatusLine().getReasonPhrase() + "】");
        }
        return result;
    }

    private void doRelease() {
        HttpConnectionHolder.released();
        if (HttpConnectionHolder.getReferenceCount() == 0) {
            HttpConnectionHolder.reset();
        }
    }

    private void close(CloseableHttpClient httpclient, CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
            if (httpclient != null) {
                httpclient.close();
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("release http connection has some problem.");
            }
        }
    }

    private CloseableHttpResponse doExecute(CloseableHttpClient httpclient, String url, Map<String, String> params, RequestMethod method)
            throws Exception {
        if (method == RequestMethod.POST) {
            return doPostInternal(httpclient, url, params);
        } else if (method == RequestMethod.GET) {
            return doGetInternal(httpclient, url);
        }
        throw new HttpException(HttpError.UNSUPPORTED_REQUEST_METHOD.getErrorCode(),
                HttpError.UNSUPPORTED_REQUEST_METHOD.getErrorMessage());
    }

    private CloseableHttpResponse doGetInternal(CloseableHttpClient httpclient, String url)
            throws Exception {
        HttpGet httpGet = new HttpGet(url);
        return httpclient.execute(httpGet);
    }

    private CloseableHttpResponse doPostInternal(CloseableHttpClient httpclient, String url, Map<String, String> params)
            throws Exception {
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> requestParams = new ArrayList<>();
        for (Entry<String, String> entry : params.entrySet()) {
            requestParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        if (requestParams.size() > 0) {
            UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(requestParams, this.charset);
            httpPost.setEntity(uefEntity);
        }
        return httpclient.execute(httpPost);
    }

    private CloseableHttpResponse doPostInternal(CloseableHttpClient httpclient, String url, String xml)
            throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(EntityBuilder.create().setContentType(ContentType.create(MIME_TYPE_XML, Consts.UTF_8)).setText(xml).build());
        return httpclient.execute(httpPost);
    }

    private CloseableHttpClient newHttpClient(Protocol protocol) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(this.connectionRequestTimeout)
                .setSocketTimeout(this.socketTimeout)
                .setConnectTimeout(this.connectTimeout)
                .build();

        if (protocol == Protocol.HTTP) {
            return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        } else {
            X509TrustManager x509mgr = HttpsSupport.newX509TrustManager();

            SSLContext sslContext = HttpsSupport.newSSLContext(x509mgr);
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            return HttpClients.custom()
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        }
    }

    public static final class Builder {

        Protocol protocol = Protocol.HTTP;
        String contentType = DEFAULT_CONTENT_TYPE;
        String charset = DEFAULT_CHARSET;
        RequestMethod requestMethod = RequestMethod.POST;
        int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
        int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

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

        public Builder connectionRequestTimeout(int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public HttpTemplate build() {
            return new HttpTemplate(this);
        }
    }
}
