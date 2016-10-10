package com.vip.study.http.support;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.http.support.exception.HttpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Created by jack on 16/8/9.
 */
@Slf4j
public class HttpTest {

    @Test
    public void testReadXml() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document root = db.parse(HttpTest.class.getClassLoader().getResourceAsStream("test.xml"));

            /*
            NodeList nodeList = root.getChildNodes();
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("property")) {
                        NamedNodeMap namedNodeMap = node.getAttributes();
                        System.out.println(namedNodeMap);
                    }
                }
            }*/
            list1(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void list1(Node node) {
        HttpTemplate httpTemplate = new HttpTemplate();

        if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("property")) {
            NamedNodeMap namedNodeMap = node.getAttributes();

            Node nameNode = namedNodeMap.getNamedItem("name");
            Node valueNode = namedNodeMap.getNamedItem("value");
            setValue(httpTemplate, nameNode.getNodeValue(), valueNode.getNodeValue());
        }
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            list1(child);
        }

    }

    public static void setValue(Object o, String propertyName, Object propertyValue) {
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
            throw new IllegalArgumentException("param is invalid.");
        }
    }


    public static void list(Node node) {
        if (node.getNodeType() == node.TEXT_NODE) {
            System.out.println(node.getTextContent());
        }
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            list(child);
        }
    }

    @Test
    public void testDoGet() {
        try {
            HttpTemplate httpTemplate = new HttpTemplate.Builder()
                    .charset("UTF-8")
                    .protocol(Protocol.HTTP)
                    .build();

            String url = "http://127.0.0.1:8080/job/list";

            Map<String, String> response = httpTemplate.doGet(url, new HttpCallback<Map<String, String>>() {

                @Override
                public Map<String, String> doParseResult(String result) throws HttpException {
                    return (Map<String, String>) JSON.parse(result);
                }
            });

            log.info("--------->>>Response:{}", response);

        } catch (HttpException e) {
            log.error(e.getErrorCode() + ":" + e.getErrorMessage());
        }
    }

    @Test
    public void testDoPost() {
        try {

            HttpTemplate httpTemplate = new HttpTemplate.Builder()
                    .charset("UTF-8")
                    .protocol(Protocol.HTTP)
                    .build();
            String url = "http://127.0.0.1:8080/job/list";

            Map<String, String> response = httpTemplate.execute(url, Maps.<String, String>newHashMap(), RequestMethod.POST,

                    new HttpCallback<Map<String, String>>() {

                        @Override
                        public Map<String, String> doParseResult(String result) throws HttpException {
                            return (Map<String, String>) JSON.parse(result);
                        }
                    });

            log.info("POST:--------->>>Response:{}", response);

        } catch (HttpException e) {
            log.error(e.getErrorCode() + ":" + e.getErrorMessage());
        }
    }
}
