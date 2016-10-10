package com.vip.study.http.support.config;

import com.vip.study.util.log.SLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Created by jack08.liu on 2016/10/10.
 */
public class HttpParser extends AbstractBeanDefinitionParser {

    protected final Logger logger = SLoggerFactory.getLogger(HttpParser.class);

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

        RootBeanDefinition httpTemplate = new RootBeanDefinition();//TODO
        parserContext.getRegistry().registerBeanDefinition("httpTemplate", httpTemplate);
        return null;
    }
}
