package com.vip.study.http.support;

import com.google.common.collect.Maps;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.http.support.exception.HttpException;
import org.junit.Test;

import java.util.Map;

/**
 * Created by jack on 16/8/9.
 */
public class HttpTest {

    @Test
    public void testDoPost() {
        try {

            HttpTemplate httpTemplate = new HttpTemplate.Builder()
                    .charSet("UTF-8")
                    .protocol(Protocol.HTTP)
                    .build();


            Map<String, String> response = httpTemplate.execute("url", Maps.<String, String>newHashMap(), RequestMethod.POST,

                    new HttpCallback<Map<String, String>>() {

                        @Override
                        public Map<String, String> doParseResult(String result) throws HttpException {
                            return null;
                        }
                    });

        } catch (HttpException e) {
            e.printStackTrace();
        }
    }
}
