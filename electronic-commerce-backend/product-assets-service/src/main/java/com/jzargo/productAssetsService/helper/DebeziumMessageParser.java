package com.jzargo.productAssetsService.helper;

import com.jzargo.core.helper.DebeziumParser;

import java.util.Map;

public class DebeziumMessageParser extends DebeziumParser {

    private DebeziumMessageParser (){}

    public static Long productIdByProductAsset(Map<String, Object> source){

        Number id = (Number) source.get("product_id");

        return id.longValue();
    }

}
