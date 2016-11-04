package org.kiwi.http.support;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.kiwi.http.support.enums.Protocol;
import org.kiwi.http.support.enums.RequestMethod;
import org.kiwi.http.support.exception.HttpException;

import java.util.Map;

/**
 * Created by jack on 16/8/9.
 */
@Slf4j
public class HttpTest {

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
                public Map<String, String> doParseResult(String result) {
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
                        public Map<String, String> doParseResult(String result) {
                            return (Map<String, String>) JSON.parse(result);
                        }
                    });

            log.info("POST:--------->>>Response:{}", response);

        } catch (HttpException e) {
            log.error(e.getErrorCode() + ":" + e.getErrorMessage());
        }
    }
}
