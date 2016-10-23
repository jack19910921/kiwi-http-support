package org.kiwi.http.support.config;

import org.apache.commons.lang3.StringUtils;
import org.kiwi.http.support.HttpTemplate;
import org.kiwi.util.log.KLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
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

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();

        //id
        String id = element.getAttribute(this.ID_ATTRIBUTE);
        if (StringUtils.isBlank(id)) {
            id = this.DEFAULT_ID;
        }
        BeanDefinitionHolder idHolder = new BeanDefinitionHolder(rootBeanDefinition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(idHolder,
                parserContext.getRegistry());

        //class
        String className = element.getAttribute(this.CLASS_ATTRIBUTE);
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
        BeanDefinitionHolder classNameHolder = new BeanDefinitionHolder(rootBeanDefinition, className);
        BeanDefinitionReaderUtils.registerBeanDefinition(classNameHolder,
                parserContext.getRegistry());
        rootBeanDefinition.setBeanClassName(className);

        //configClass
        String configClass = element.getAttribute(this.CONFIG_CLASS_ATTRIBUTE);
        if (StringUtils.isNotBlank(configClass)) {
            BeanDefinitionHolder configClassHolder = new BeanDefinitionHolder(rootBeanDefinition,
                    configClass);
            BeanDefinitionReaderUtils.registerBeanDefinition(configClassHolder,
                    parserContext.getRegistry());
            rootBeanDefinition.getPropertyValues().addPropertyValue(this.CONFIG_CLASS_ATTRIBUTE, configClass);
        }

        //configMethodName
        String configMethodName = element.getAttribute(this.CONFIG_METHOD_NAME_ATTRIBUTE);
        if (StringUtils.isNotBlank(configMethodName)) {
            BeanDefinitionHolder configMethodNameHolder = new BeanDefinitionHolder(rootBeanDefinition,
                    configMethodName);
            BeanDefinitionReaderUtils.registerBeanDefinition(configMethodNameHolder,
                    parserContext.getRegistry());
            rootBeanDefinition.getPropertyValues().addPropertyValue(this.CONFIG_METHOD_NAME_ATTRIBUTE, configMethodName);
        }

        return rootBeanDefinition;
    }
}
