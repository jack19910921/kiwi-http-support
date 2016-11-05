package org.kiwi.http.support;

/**
 * Created by jack on 16/10/28.
 */
public class HttpConnectionHolder {

    private static final ThreadLocal<Integer> RETRY_CNT = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    private static final ThreadLocal<Integer> REFERENCE_COUNT = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static int getRetryCnt() {
        return RETRY_CNT.get();
    }

    public static int getAndIncrementRetryCnt() {
        RETRY_CNT.set(RETRY_CNT.get() + 1);
        return getRetryCnt();
    }

    public static void requested() {
        REFERENCE_COUNT.set(REFERENCE_COUNT.get() + 1);
    }

    public static void released() {
        REFERENCE_COUNT.set(REFERENCE_COUNT.get() - 1);
    }

    public static int getReferenceCount() {
        return REFERENCE_COUNT.get();
    }

    public static void reset() {
        RETRY_CNT.set(0);
        REFERENCE_COUNT.set(0);
    }

}
