package com.jzargo.productservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
public @interface IT {
    @AliasFor(annotation = SpringBootTest.class, attribute = "properties")
    String[] properties() default {};

}
