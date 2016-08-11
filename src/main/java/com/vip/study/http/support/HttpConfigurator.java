package com.vip.study.http.support;

import com.vip.study.http.support.enums.Order;
import com.vip.study.http.support.enums.Protocol;
import com.vip.study.http.support.enums.RequestMethod;
import com.vip.study.util.log.SLoggerFactory;
import org.slf4j.Logger;


/**
 * Created by jack on 16/7/31.
 */
public abstract class HttpConfigurator {

    protected final Logger logger = SLoggerFactory.getLogger(this.getClass());

    protected Protocol protocol;
    protected String contentType;
    protected String charset;
    protected RequestMethod requestMethod;
    protected Order order;
    protected int connectionTime = -1;

}
