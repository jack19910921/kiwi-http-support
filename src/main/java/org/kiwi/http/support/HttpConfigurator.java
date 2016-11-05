package org.kiwi.http.support;

import org.kiwi.http.support.cons.HttpConstant;
import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.spi.ConfigProvider;
import org.kiwi.http.support.spi.SignProvider;
import org.kiwi.util.ReflectUtil;
import org.kiwi.util.log.KLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
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
public abstract class HttpConfigurator implements InitializingBean, ApplicationContextAware, HttpConfiguratorMBean {

    protected final Logger logger = KLoggerFactory.getLogger(this.getClass());

    protected Protocol protocol;
    protected String contentType;
    protected String charset;
    protected RequestMethod requestMethod;

    protected String configClass = DEFAULT_CONFIG_CLASS;
    protected String configMethodName = DEFAULT_CONFIG_METHOD_NAME;
    protected SignProvider signProvider;

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
    public Map<String, String> getConfig() {
        return ReflectUtil.convertJavaBean2Map(this);
    }

    @PostConstruct
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
    }

    private void initConfig() {
        Assert.hasText(this.configClass, "configClass is required");
        Assert.hasText(this.configMethodName, "configMethodName is required");

        try {
            ConfigProvider configProvider = null;

            /**
             * look up ConfigProvider spi implementation from classpath.
             * search all (META-INF/services/ConfigProvider) file in classpath.
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
                try {
                    /**
                     * using specified class with method in order to init config.
                     * (need setter injection,if not config,using "com.vip.xfd.account.components.ConfigManager#getString(String,String)")
                     */
                    clazz = loader.loadClass(this.configClass);
                    Object configManager = applicationContext.getBean(clazz);

                    Method method = clazz.getDeclaredMethod(this.configMethodName, String.class, String.class);
                    method.setAccessible(true);

                    String protocolStr = (String) method.invoke(configManager, CONFIG_KEY_PROTOCOL, HttpConstant.DEFAULT_PROTOCOL);
                    this.protocol = Protocol.determineProtocolByText(protocolStr);

                    String requestMethodStr = (String) method.invoke(configManager, CONFIG_KEY_REQUEST_METHOD, HttpConstant.DEFAULT_REQUEST_METHOD);
                    this.requestMethod = RequestMethod.determineRequestMethodByText(requestMethodStr);

                    String contentType = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_CONTENT_TYPE, HttpConstant.DEFAULT_CONTENT_TYPE);
                    this.contentType = contentType;

                    String charset = (String) method.invoke(configManager, HttpConstant.CONFIG_KEY_CHARSET, HttpConstant.DEFAULT_CHARSET);
                    this.charset = charset;
                } catch (ClassNotFoundException e) {
                    logger.debug("load config has some problem,using default config.[{}]", e.getMessage());
                    initDefaultConfig();
                }
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
            logger.debug("load config has some problem,using default config.", e);
            initDefaultConfig();
        }

    }

    private void initConfigBySpiImpl(ConfigProvider configProvider) {
        this.protocol = Protocol.determineProtocolByText(configProvider.getString(CONFIG_KEY_PROTOCOL, DEFAULT_PROTOCOL));
        this.requestMethod = RequestMethod.determineRequestMethodByText(configProvider.getString(CONFIG_KEY_REQUEST_METHOD, DEFAULT_REQUEST_METHOD));
        this.contentType = configProvider.getString(CONFIG_KEY_CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        this.charset = configProvider.getString(CONFIG_KEY_CHARSET, DEFAULT_CHARSET);
    }

    private void initDefaultConfig() {
        this.protocol = Protocol.HTTP;
        this.contentType = DEFAULT_CONTENT_TYPE;
        this.charset = DEFAULT_CHARSET;
        this.requestMethod = RequestMethod.POST;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}