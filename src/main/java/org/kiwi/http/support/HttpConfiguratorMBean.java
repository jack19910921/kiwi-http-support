package org.kiwi.http.support;

/**
 * Created by jack on 16/11/5.
 */
public interface HttpConfiguratorMBean {
    void setRetryCnt(int retryCnt);

    void setRetryInterval(int retryInterval);

    void setRetryStaffIsOn(boolean retryStaffIsOn);

    int getRetryCnt();

    int getRetryInterval();

    boolean isRetryStaffIsOn();
}
