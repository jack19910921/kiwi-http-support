package org.kiwi.http.support;

import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.spi.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import static org.kiwi.http.support.cons.HttpConstant.*;

/**
 * Created by jack on 16/7/31.
 */
public abstract class HttpConfigurer implements InitializingBean, ApplicationContextAware, HttpConfigurerMBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Protocol protocol;
    protected String contentType;
    protected String charset;
    protected RequestMethod requestMethod;
    protected int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    protected int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    protected int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    protected String configClass;
    protected String configMethodName;

    protected volatile int retryCnt = DEFAULT_RETRY_CNT;
    protected volatile int retryInterval = DEFAULT_RETRY_INTERVAL;
    protected volatile boolean retryStaffIsOn = DEFAULT_RETRY_STAFF_IS_ON;

    protected ApplicationContext applicationContext;

    public void setConfigClass(String configClass) {
        this.configClass = configClass;
    }

    public void setConfigMethodName(String configMethodName) {
        this.configMethodName = configMethodName;
    }

    @Override
    public void setRetryCnt(int retryCnt) {
        this.retryCnt = retryCnt;
    }

    @Override
    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    @Override
    public void setRetryStaffIsOn(boolean retryStaffIsOn) {
        this.retryStaffIsOn = retryStaffIsOn;
    }

    @Override
    public int getRetryCnt() {
        return this.retryCnt;
    }

    @Override
    public int getRetryInterval() {
        return this.retryInterval;
    }

    @Override
    public boolean isRetryStaffIsOn() {
        return this.retryStaffIsOn;
    }

    public void register() {
        synchronized (this) {
            try {
                String mbeanName = this.getClass().getSimpleName();

                MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName objectName = new ObjectName("org.kiwi.http:type=" + mbeanName);

                if (!mbeanServer.isRegistered(objectName)) {
                    mbeanServer.registerMBean(this, objectName);
                    if (logger.isDebugEnabled()) {
                        logger.debug("org.kiwi.http:type=" + mbeanName + " registered successfully");
                    }
                }
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
    }

    public void afterPropertiesSet() {
        initConfig();

        Assert.notNull(this.protocol, "protocol is required");
        Assert.notNull(this.contentType, "contentType is required");
        Assert.notNull(this.charset, "charset is required");
        Assert.notNull(this.requestMethod, "requestMethod is required");

        register();
    }

    private void initConfig() {
        try {
            ConfigProvider configProvider = null;

            /**
             * look up ConfigProvider spi implementation from classpath.
             * search all (META-INF/services/org.kiwi.http.support.spi.ConfigProvider) file in classpath.
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
                 * need setter injection.
                 */
                clazz = loader.loadClass(this.configClass);
                Object configManager = applicationContext.getBean(clazz);

                Method method = clazz.getDeclaredMethod(this.configMethodName, String.class, String.class);
                method.setAccessible(true);

                String protocolStr = (String) method.invoke(configManager, CONFIG_KEY_PROTOCOL, DEFAULT_PROTOCOL);
                this.protocol = Protocol.determineProtocolByText(protocolStr);

                String requestMethodStr = (String) method.invoke(configManager, CONFIG_KEY_REQUEST_METHOD, DEFAULT_REQUEST_METHOD);
                this.requestMethod = RequestMethod.determineRequestMethodByText(requestMethodStr);

                String contentType = (String) method.invoke(configManager, CONFIG_KEY_CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
                this.contentType = contentType;

                String charset = (String) method.invoke(configManager, CONFIG_KEY_CHARSET, DEFAULT_CHARSET);
                this.charset = charset;

                int connectionRequestTimeout = Integer.parseInt(method.invoke(configManager, CONFIG_KEY_CONNECTION_REQUEST_TIMEOUT, DEFAULT_TIMEOUT_STR).toString());
                this.connectionRequestTimeout = connectionRequestTimeout;

                int socketTimeout = Integer.parseInt(method.invoke(configManager, CONFIG_KEY_SOCKET_TIMEOUT, DEFAULT_TIMEOUT_STR).toString());
                this.socketTimeout = socketTimeout;

                int connectTimeout = Integer.parseInt(method.invoke(configManager, CONFIG_KEY_CONNECT_TIMEOUT, DEFAULT_TIMEOUT_STR).toString());
                this.connectTimeout = connectTimeout;
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("load config has some problem,using default config.", e);
            }
            initDefaultConfig();
        }
    }

    private void initConfigBySpiImpl(ConfigProvider configProvider) {
        this.protocol = Protocol.determineProtocolByText(configProvider.get(CONFIG_KEY_PROTOCOL, DEFAULT_PROTOCOL));
        this.requestMethod = RequestMethod.determineRequestMethodByText(configProvider.get(CONFIG_KEY_REQUEST_METHOD, DEFAULT_REQUEST_METHOD));
        this.contentType = configProvider.get(CONFIG_KEY_CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        this.charset = configProvider.get(CONFIG_KEY_CHARSET, DEFAULT_CHARSET);
        this.connectionRequestTimeout = Integer.parseInt(configProvider.get(CONFIG_KEY_CONNECTION_REQUEST_TIMEOUT, DEFAULT_TIMEOUT_STR));
        this.socketTimeout = Integer.parseInt(configProvider.get(CONFIG_KEY_SOCKET_TIMEOUT, DEFAULT_TIMEOUT_STR));
        this.connectTimeout = Integer.parseInt(configProvider.get(CONFIG_KEY_CONNECT_TIMEOUT, DEFAULT_TIMEOUT_STR));
    }

    private void initDefaultConfig() {
        this.protocol = Protocol.HTTP;
        this.contentType = DEFAULT_CONTENT_TYPE;
        this.charset = DEFAULT_CHARSET;
        this.requestMethod = RequestMethod.POST;
        this.connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
        this.socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}