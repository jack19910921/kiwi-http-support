package org.kiwi.http.support;

import java.util.Map;

/**
 * Created by jack on 16/11/5.
 */
public interface HttpConfiguratorMBean {
    void setRetryCnt(int retryCnt);

    void setRetryInterval(int retryInterval);

    void setRetryStaffIsOn(boolean retryStaffIsOn);

    Map<String, String> getConfig();
}
