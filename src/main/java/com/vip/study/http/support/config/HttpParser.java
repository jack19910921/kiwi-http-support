package com.vip.study.http.support.config;

import com.vip.study.http.support.HttpTemplate;
import com.vip.study.util.log.SLoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

/**
 * Created by jack08.liu on 2016/10/10.
 */
public class HttpParser implements BeanDefinitionParser {
    public final Logger logger = SLoggerFactory.getLogger(HttpParser.class);

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
     * Constant for the property element
     */
    public static final String PROPERTY_ELEMENT = "property";

    /**
     * Constant for the name attribute
     */
    public static final String PROPERTY_NAME = "name";

    /**
     * Constant for the value attribute
     */
    public static final String PROPERTY_VALUE = "value";


    protected AbstractBeanDefinition parseInternal(Element root, ParserContext parserContext) {
        String id = root.getAttribute(this.ID_ATTRIBUTE);
        if (StringUtils.isBlank(id)) {
            id = "httpTemplate";
        }

        String className = root.getAttribute("class");
        if (StringUtils.isBlank(id)) {
            id = "httpTemplate";
        }

        HttpTemplate httpTemplate = new HttpTemplate();

        forEach(root, httpTemplate);

        httpTemplate.afterPropertiesSet();

        RootBeanDefinition httpBeanDefinition = new RootBeanDefinition(httpTemplate.getClass());
        parserContext.getRegistry().registerBeanDefinition(id, httpBeanDefinition);

        return null;
    }

    private void forEach(Node node, Object o) {
        if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(this.PROPERTY_ELEMENT)) {
            NamedNodeMap namedNodeMap = node.getAttributes();

            Node nameNode = namedNodeMap.getNamedItem(this.PROPERTY_NAME);
            Node valueNode = namedNodeMap.getNamedItem(this.PROPERTY_VALUE);
            setValue(o, nameNode.getNodeValue(), valueNode.getNodeValue());
        }

        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            forEach(child, o);
        }

    }

    private void setValue(Object o, String propertyName, Object propertyValue) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(o.getClass());
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();

            if (ArrayUtils.isNotEmpty(pds)) {
                for (PropertyDescriptor pd : pds) {
                    if (pd.getName().equals(propertyName)) {
                        pd.getWriteMethod().invoke(o, propertyValue);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("setValue has some problem.", e);
            }
            throw new IllegalArgumentException("param is invalid.");
        }
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext context) {
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();

        //id
        String id = element.getAttribute(this.ID_ATTRIBUTE);
        if (StringUtils.isBlank(id)) {
            id = this.DEFAULT_ID;
        }
        BeanDefinitionHolder idHolder = new BeanDefinitionHolder(rootBeanDefinition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(idHolder,
                context.getRegistry());

        //class
        String className = element.getAttribute(this.CLASS_ATTRIBUTE);
        if (StringUtils.isBlank(className)) {
            className = HttpTemplate.class.getName();
        }
        BeanDefinitionHolder classNameHolder = new BeanDefinitionHolder(rootBeanDefinition, className);
        BeanDefinitionReaderUtils.registerBeanDefinition(classNameHolder,
                context.getRegistry());
        rootBeanDefinition.setBeanClassName(className);

        //configClass
        String configClass = element.getAttribute(this.CONFIG_CLASS_ATTRIBUTE);
        if (StringUtils.isNotBlank(configClass)) {
            BeanDefinitionHolder configClassHolder = new BeanDefinitionHolder(rootBeanDefinition,
                    configClass);
            BeanDefinitionReaderUtils.registerBeanDefinition(configClassHolder,
                    context.getRegistry());
            rootBeanDefinition.getPropertyValues().addPropertyValue(this.CONFIG_CLASS_ATTRIBUTE, configClass);
        }

        //configMethodName
        String configMethodName = element.getAttribute(this.CONFIG_METHOD_NAME_ATTRIBUTE);
        if (StringUtils.isNotBlank(configMethodName)) {
            BeanDefinitionHolder configMethodNameHolder = new BeanDefinitionHolder(rootBeanDefinition,
                    configMethodName);
            BeanDefinitionReaderUtils.registerBeanDefinition(configMethodNameHolder,
                    context.getRegistry());
            rootBeanDefinition.getPropertyValues().addPropertyValue(this.CONFIG_METHOD_NAME_ATTRIBUTE, configMethodName);
        }

        return rootBeanDefinition;
    }
}
