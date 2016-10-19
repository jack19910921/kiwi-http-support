package org.kiwi.http.support.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by jack08.liu on 2016/10/10.
 */
public class HttpNamespaceHandler extends NamespaceHandlerSupport{

    @Override
    public void init() {
        registerBeanDefinitionParser("http-support", new HttpParser());
    }
}
