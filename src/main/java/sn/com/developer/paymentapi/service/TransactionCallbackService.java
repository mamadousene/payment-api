package sn.com.developer.paymentapi.service;

import sn.com.developer.paymentapi.domain.entity.TransactionCallback;

public interface TransactionCallbackService {

    void triggerCallbackAsync(TransactionCallback callback);

    void callbackFallback(TransactionCallback callback, Throwable t);
}
