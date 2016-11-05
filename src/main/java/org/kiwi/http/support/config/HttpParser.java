package org.kiwi.http.support.config;

import org.apache.commons.lang3.StringUtils;
import org.kiwi.http.support.HttpTemplate;
import org.kiwi.util.log.KLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Created by jack08.liu on 2016/10/10.
 */
public class HttpParser implements BeanDefinitionParser {
    private final Logger logger = KLoggerFactory.getLogger(HttpParser.class);

    public static final String DEFAULT_ID = "httpTemplate";

    /**
     * Constant for the id attribute
     */
    public static final String ID_ATTRIBUTE = "id";

    /**
     * Constant for the class attribute
     */
    public static final String CLASS_ATTRIBUTE = "class";

    /**
     * Constant for the configClass attribute
     */
    public static final String CONFIG_CLASS_ATTRIBUTE = "configClass";

    /**
     * Constant for the configMethodName attribute
     */
    public static final String CONFIG_METHOD_NAME_ATTRIBUTE = "configMethodName";

    /**
     * Constant for the retryCnt attribute
     */
    public static final String RETRY_CNT_ATTRIBUTE = "retryCnt";

    /**
     * Constant for the retryInterval attribute
     */
    public static final String RETRY_INTERVAL_ATTRIBUTE = "retryInterval";

    /**
     * Constant for the retryStaffIsOn attribute
     */
    public static final String RETRY_STAFF_IS_ON_ATTRIBUTE = "retryStaffIsOn";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();

        //id
        String id = element.getAttribute(ID_ATTRIBUTE);
        if (StringUtils.isBlank(id)) {
            id = DEFAULT_ID;
        }

        //className
        String className = element.getAttribute(CLASS_ATTRIBUTE);
        if (StringUtils.isBlank(className)) {
            className = HttpTemplate.class.getName();
        } else {
            try {
                Class clazz = Class.forName(className);
                if (!HttpTemplate.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("className attribute:{" + className + "} is invalid.");
                }
            } catch (ClassNotFoundException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Class:{" + className + "} not found.", e);
                }
                className = HttpTemplate.class.getName();
            }
        }
        rootBeanDefinition.setBeanClassName(className);

        //configClass
        String configClass = element.getAttribute(CONFIG_CLASS_ATTRIBUTE);
        if (StringUtils.isNotBlank(configClass)) {
            rootBeanDefinition.getPropertyValues().add(CONFIG_CLASS_ATTRIBUTE, configClass);
        }

        //configMethodName
        String configMethodName = element.getAttribute(CONFIG_METHOD_NAME_ATTRIBUTE);
        if (StringUtils.isNotBlank(configMethodName)) {
            rootBeanDefinition.getPropertyValues().add(CONFIG_METHOD_NAME_ATTRIBUTE, configMethodName);
        }

        //retryCnt
        String retryCnt = element.getAttribute(RETRY_CNT_ATTRIBUTE);
        if (StringUtils.isNotBlank(retryCnt)) {
            rootBeanDefinition.getPropertyValues().add(RETRY_CNT_ATTRIBUTE, retryCnt);
        }

        //retryInterval
        String retryInterval = element.getAttribute(RETRY_INTERVAL_ATTRIBUTE);
        if (StringUtils.isNotBlank(retryInterval)) {
            rootBeanDefinition.getPropertyValues().add(RETRY_INTERVAL_ATTRIBUTE, retryInterval);
        }

        //retryStaffIsOn
        String retryStaffIsOn = element.getAttribute(RETRY_STAFF_IS_ON_ATTRIBUTE);
        if (StringUtils.isNotBlank(retryStaffIsOn)) {
            rootBeanDefinition.getPropertyValues().add(RETRY_STAFF_IS_ON_ATTRIBUTE, retryStaffIsOn);
        }

        parserContext.getRegistry().registerBeanDefinition(id, rootBeanDefinition);

        return rootBeanDefinition;
    }
}
