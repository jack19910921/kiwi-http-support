package com.vip.study.http.support.config;

import com.vip.study.http.support.HttpTemplate;
import com.vip.study.util.log.SLoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
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
public class HttpParser extends AbstractBeanDefinitionParser {

    public final Logger logger = SLoggerFactory.getLogger(HttpParser.class);

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

    @Override
    protected AbstractBeanDefinition parseInternal(Element root, ParserContext parserContext) {
        String id = root.getAttribute(this.ID_ATTRIBUTE);
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

}
