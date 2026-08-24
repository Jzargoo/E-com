package com.jzargo.core.helper;

import java.util.Map;

public class DebeziumParser {

    @SuppressWarnings("unchecked")
    public static String getOperationByRoot(Map<String, Object> root) {
        var payload = (Map<String, Object>) root.get("payload");

        return (String) payload.get("op");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAfterByRoot(Map<String, Object> root) {
        var payload = (Map<String, Object>) root.get("payload");

        return (Map<String, Object>) payload.get("after");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getBeforeByRoot(Map<String, Object> root) {
        var payload = (Map<String, Object>) root.get("payload");

        return (Map<String, Object>) payload.get("before");
    }


}
