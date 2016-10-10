package com.vip.study.http.support;

import com.vip.study.http.support.cons.HttpConstant;
import com.vip.study.http.support.enums.ParameterOrder;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.http.support.spi.ConfigProvider;
import com.vip.study.http.support.spi.SignProvider;
import com.vip.study.util.log.SLoggerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Created by jack on 16/7/31.
 */
public abstract class HttpConfigurator implements InitializingBean, ApplicationContextAware {

    protected final Logger logger = SLoggerFactory.getLogger(this.getClass());

    protected Protocol protocol;
    protected String contentType;
    protected String charset;
    protected RequestMethod requestMethod;
    protected ParameterOrder parameterOrder;

    protected String configClass = HttpConstant.DEFAULT_CONFIG_CLASS;
    protected String configMethodName = HttpConstant.DEFAULT_CONFIG_METHOD_NAME;
    protected SignProvider signProvider;

    protected ApplicationContext applicationContext;

    public void setConfigClass(String configClass) {
        this.configClass = configClass;
    }

    public void setConfigMethodName(String configMethodName) {
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
            ConfigProvider configProvider = null;

            /**
             * look up ConfigProvider spi implementation from classpath.
             * search all (META-INF/services/com.vip.study.http.support.spi.ConfigProvider) file in classpath.
             */
            ServiceLoader<ConfigProvider> serviceLoader = ServiceLoader.load(ConfigProvider.class);
            Iterator<ConfigProvider> iterator = serviceLoader.iterator();
            while (iterator.hasNext()) {
                configProvider = iterator.next();
                break;
            }
            if (configProvider != null) {
                initConfigBySpiImpl(configProvider);
            }

            /**
             * using thread context classLoader.
             * look up ConfigProvider spi implementation from ioc container.
             */
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> clazz = loader.loadClass(ConfigProvider.class.getName());

            Map<String, ?> configProviderBeanMap = applicationContext.getBeansOfType(clazz);
            if (!CollectionUtils.isEmpty(configProviderBeanMap)) {
                for (Object configProviderBean : configProviderBeanMap.values()) {
                    configProvider = (ConfigProvider) configProviderBean;
                    break;
                }
            }

            if (configProvider != null) {
                initConfigBySpiImpl(configProvider);
            } else {
                /**
                 * using specified class with method in order to init config.
                 * (need setter injection,if not config,using "com.vip.xfd.account.components.ConfigManager#getString(String,String)")
                 */
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

            /**
             * look up SignProvider spi implementation from classpath.
             */
            ServiceLoader<SignProvider> serviceLoader1 = ServiceLoader.load(SignProvider.class);
            Iterator<SignProvider> iterator1 = serviceLoader1.iterator();
            while (iterator1.hasNext()) {
                this.signProvider = iterator1.next();
                break;
            }

            if (this.signProvider == null) {
                /**
                 * look up SignProvider spi implementation from ioc container.
                 */
                Class<?> signClazz = loader.loadClass(SignProvider.class.getName());

                Map<String, ?> signProviderBeanMap = applicationContext.getBeansOfType(signClazz);
                if (!CollectionUtils.isEmpty(signProviderBeanMap)) {
                    for (Object signProviderBean : signProviderBeanMap.values()) {
                        this.signProvider = (SignProvider) signProviderBean;
                        break;
                    }
                }
            }

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("load config has some problem,using default config.", e);
            }
            initDefaultConfig();
        }

    }

    private void initConfigBySpiImpl(ConfigProvider configProvider) {
        this.protocol = Protocol.determineProtocolByLabel(configProvider.getString(HttpConstant.CONFIG_KEY_PROTOCOL, HttpConstant.DEFAULT_PROTOCOL));
        this.requestMethod = RequestMethod.determineRequestMethodByLabel(configProvider.getString(HttpConstant.CONFIG_KEY_REQUEST_METHOD, HttpConstant.DEFAULT_REQUEST_METHOD));
        this.parameterOrder = ParameterOrder.determineParameterOrderByLabel(configProvider.getString(HttpConstant.CONFIG_KEY_PARAMETER_ORDER, HttpConstant.DEFAULT_PARAMETER_ORDER));
        this.contentType = configProvider.getString(HttpConstant.CONFIG_KEY_CONTENT_TYPE, HttpConstant.DEFAULT_CONTENT_TYPE);
        this.charset = configProvider.getString(HttpConstant.CONFIG_KEY_CHARSET, HttpConstant.DEFAULT_CHARSET);
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