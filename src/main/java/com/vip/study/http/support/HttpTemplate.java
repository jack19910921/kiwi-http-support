package com.vip.study.http.support;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vip.study.http.support.cons.HttpConstant;
import com.vip.study.http.support.enums.ParameterOrder;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.http.support.enums.error.HttpErrorEnum;
import com.vip.study.http.support.exception.HttpException;
import com.vip.study.util.ReflectUtil;
import com.vip.study.util.SignUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.Assert;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.Comparator;
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
        this.parameterOrder = builder.parameterOrder;
    }

    public <T> T doPost(String url, Map<String, String> params, HttpCallback<T> action)
            throws HttpException {
        return execute(url, params, RequestMethod.POST, action);
    }

    public <T> T doPost(String url, Object params, HttpCallback<T> action)
            throws HttpException {
        Assert.notNull(params, "params must not be null");

        return execute(url, params, RequestMethod.POST, action);
    }

    public <T> T doGet(String url, HttpCallback<T> action)
            throws HttpException {
        return execute(url, null, RequestMethod.GET, action);
    }


    public <T> T execute(String url, Object params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        Assert.notNull(params, "params must not be null");

        Map<String, String> map = ReflectUtil.convertJavaBean2Map(params);

        return execute(url, map, method, action);
    }

    public <T> T execute(String url, Map<String, String> params, RequestMethod method, HttpCallback<T> action)
            throws HttpException {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;

        try {
            //1.sort params and build sign
            Map<String, String> sortedMapWithSign = sign(params, parameterOrder, charset);

            //2.create http or https client
            httpclient = newHttpClient(protocol);

            //3.send request
            response = doExecute(httpclient, url, sortedMapWithSign, method);

            //4.consume response
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

                switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_METHOD_NOT_ALLOWED:
                        throw new HttpException(HttpErrorEnum.SC_METHOD_NOT_ALLOWED.getErrorCode(),
                                HttpErrorEnum.SC_METHOD_NOT_ALLOWED.getErrorMessage());
                    default:
                        throw new HttpException(HttpErrorEnum.RESPONSE_STATUS_CODE_INVALID.getErrorCode(),
                                HttpErrorEnum.RESPONSE_STATUS_CODE_INVALID.getErrorMessage());
                }

            }

            //5.invoke callback
            return action.doParseResult(result);

        } catch (Exception e) {
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
                throw new HttpException(HttpErrorEnum.CLOSE_CHANNEL_ERROR.getErrorCode(),
                        HttpErrorEnum.CLOSE_CHANNEL_ERROR.getErrorMessage());
            }
        }
    }

    private Map<String, String> sign(Map<String, String> params, final ParameterOrder parameterOrder, String charset) {
        Map<String, String> map = Maps.newLinkedHashMap();

        if (params == null) {
            return params;
        }

        if (parameterOrder == ParameterOrder.IMMUTABLE) {
            map.putAll(params);
        } else {
            Map<String, String> treeMap = Maps.newTreeMap(new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    if (parameterOrder == ParameterOrder.ASC) {
                        return o1.compareTo(o2);
                    } else {
                        return o2.compareTo(o1);
                    }
                }
            });
            map.putAll(treeMap);
        }

        doSign(map, charset);

        return map;
    }

    private void doSign(Map<String, String> map, String charset) {
        try {
            if (this.signSupport != null) {
                signSupport.doSign(map, charset);
            } else {
                String sign = SignUtil.sign(map, charset);
                map.put(HttpConstant.SIGN_KEY, sign);
            }

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("do sign has some problem.", e);
            }
            logger.error("ErrorMessage:" + e.getMessage());
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
            UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(requestParams, charset);
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
        ParameterOrder parameterOrder = ParameterOrder.IMMUTABLE;

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder charSet(String charSet) {
            this.charset = charSet;
            return this;
        }

        public Builder requestMethod(RequestMethod requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public Builder parameterOrder(ParameterOrder parameterOrder) {
            this.parameterOrder = parameterOrder;
            return this;
        }

        public HttpTemplate build() {
            return new HttpTemplate(this);
        }
    }
}
