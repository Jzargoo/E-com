package com.jzargo.inventory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalLogger {

    public static void logStartingExecution(String methodName) {
        log.info("{} is executing", methodName);
    }

    public static void logException(Exception e,String action) {
        log.error("Occurred an exception with message {} while {}", e.getMessage(), action, e);
    }

    public static void logRepeatedMessage(String messageId){
        log.warn("Caught repeated message with message id {}", messageId);
    }

    public static void logProcessedStreamMessage(String key, Object value) {

        log.info("sending a message with key {} with a value {}", key, value);
    }
}
