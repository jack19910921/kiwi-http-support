package com.vip.study.http.support;

import com.vip.study.http.support.cons.HttpConstant;
import com.vip.study.http.support.enums.ParameterOrder;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.http.support.spi.ConfigManagerSupport;
import com.vip.study.http.support.spi.SignSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * Created by jack on 16/7/31.
 */
public abstract class HttpConfigurator implements InitializingBean, ApplicationContextAware {

    protected final Log logger = LogFactory.getLog(this.getClass());

    protected Protocol protocol;
    protected String contentType;
    protected String charset;
    protected RequestMethod requestMethod;
    protected ParameterOrder parameterOrder;

    protected String configClass = HttpConstant.DEFAULT_CONFIG_CLASS;
    protected String configMethodName = HttpConstant.DEFAULT_CONFIG_METHOD_NAME;
    protected SignSupport signSupport;

    protected ApplicationContext applicationContext;

    protected void setConfigClass(String configClass) {
        this.configClass = configClass;
    }

    protected void setConfigMethodName(String configMethodName) {
        this.configMethodName = configMethodName;
    }

    public void afterPropertiesSet() {
        initConfig();

        Assert.notNull(this.protocol, "protocol is required");
        Assert.notNull(this.contentType, "contentType is required");
        Assert.notNull(this.charset, "charset is required");
        Assert.notNull(this.requestMethod, "requestMethod is required");
        Assert.notNull(this.parameterOrder, "parameterOrder is required");
    }

    private void initConfig() {
        Assert.hasText(this.configClass, "configClass is required");
        Assert.hasText(this.configMethodName, "configMethodName is required");

        try {
            //load config from data dict table or zk or classpath
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> clazz = loader.loadClass(ConfigManagerSupport.class.getName());

            ConfigManagerSupport configManagerSupport = (ConfigManagerSupport) applicationContext.getBean(clazz);
            if (configManagerSupport != null) {
                //has ConfigManagerSupport spi implement
                this.protocol = Protocol.determineProtocolByLabel(configManagerSupport.getString(HttpConstant.CONFIG_KEY_PROTOCOL, HttpConstant.DEFAULT_PROTOCOL));
                this.requestMethod = RequestMethod.determineRequestMethodByLabel(configManagerSupport.getString(HttpConstant.CONFIG_KEY_REQUEST_METHOD, HttpConstant.DEFAULT_REQUEST_METHOD));
                this.parameterOrder = ParameterOrder.determineParameterOrderByLabel(configManagerSupport.getString(HttpConstant.CONFIG_KEY_PARAMETER_ORDER, HttpConstant.DEFAULT_PARAMETER_ORDER));
                this.contentType = configManagerSupport.getString(HttpConstant.CONFIG_KEY_CONTENT_TYPE, HttpConstant.DEFAULT_CONTENT_TYPE);
                this.charset = configManagerSupport.getString(HttpConstant.CONFIG_KEY_CHARSET, HttpConstant.DEFAULT_CHARSET);

            } else {
                clazz = loader.loadClass(this.configClass);
                Object configManager = applicationContext.getBean(clazz);

                Method method = clazz.getDeclaredMethod(this.configMethodName, String.class, String.class);
                method.setAccessible(true);

                String protocolStr = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_PROTOCOL, HttpConstant.DEFAULT_PROTOCOL);
                this.protocol = Protocol.determineProtocolByLabel(protocolStr);

                String requestMethodStr = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_REQUEST_METHOD, HttpConstant.DEFAULT_REQUEST_METHOD);
                this.requestMethod = RequestMethod.determineRequestMethodByLabel(requestMethodStr);

                String parameterOrderStr = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_PARAMETER_ORDER, HttpConstant.DEFAULT_PARAMETER_ORDER);
                this.parameterOrder = ParameterOrder.determineParameterOrderByLabel(parameterOrderStr);

                String contentType = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_CONTENT_TYPE, HttpConstant.DEFAULT_CONTENT_TYPE);
                this.contentType = contentType;

                String charset = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_CHARSET, HttpConstant.DEFAULT_CHARSET);
                this.charset = charset;
            }

            //find SignSupport spi implement
            Class<?> signClazz = loader.loadClass(SignSupport.class.getName());
            this.signSupport = (SignSupport) applicationContext.getBean(signClazz);

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("load config has some problem,using default config.", e);
            }
            initDefaultConfig();
        }

    }

    private void initDefaultConfig() {
        this.protocol = Protocol.HTTP;
        this.contentType = HttpConstant.DEFAULT_CONTENT_TYPE;
        this.charset = HttpConstant.DEFAULT_CHARSET;
        this.requestMethod = RequestMethod.POST;
        this.parameterOrder = ParameterOrder.IMMUTABLE;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}