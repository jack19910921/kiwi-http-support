package org.kiwi.http.support;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.exception.HttpException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jack on 16/8/9.
 */
public class HttpTest {

    public static final String URL = "http://localhost/";

    @Test
    public void testDoGet() {
        try {
            HttpTemplate httpTemplate = new HttpTemplate.Builder()
                    .charset("UTF-8")
                    .protocol(Protocol.HTTP)
                    .build();
            Map<String, String> response = httpTemplate.doGet(URL, new HttpCallback<Map<String, String>>() {
                @Override
                public Map<String, String> doParseResult(String result) {
                    return (Map<String, String>) JSON.parse(result);
                }
            });
            System.out.println("--------->>>Response:" + response);
        } catch (HttpException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDoPost() {
        try {
            HttpTemplate httpTemplate = new HttpTemplate.Builder()
                    .charset("UTF-8")
                    .protocol(Protocol.HTTP)
                    .build();
            Map<String, String> response = httpTemplate.execute(URL, new HashMap<String, String>(), RequestMethod.POST,
                    new HttpCallback<Map<String, String>>() {
                        @Override
                        public Map<String, String> doParseResult(String result) {
                            return (Map<String, String>) JSON.parse(result);
                        }
                    });
            System.out.println("--------->>>Response:" + response);
        } catch (HttpException e) {
            e.printStackTrace();
        }
    }
}
