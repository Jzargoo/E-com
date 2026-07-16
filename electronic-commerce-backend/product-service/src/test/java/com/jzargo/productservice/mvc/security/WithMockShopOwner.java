package com.jzargo.productservice.mvc.security;

public @interface WithMockShopOwner {
    String username() default "joe doe";
    int shopId() default 1;
}
