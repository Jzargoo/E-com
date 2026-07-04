package com.jzargo.media.config;

import com.jzargo.media.storages.virtual.KafkaVirtualStorageProcessor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Set;

import static com.jzargo.media.config.ApplicationPropertyStorage.STORAGES_PROPERTIES;

@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaVirtualProcessorsConfiguration
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry)
            throws BeansException {

        Set<ApplicationPropertyStorage.SecondStorage> storages =
                Binder.get(environment)
                        .bind(
                                STORAGES_PROPERTIES,
                                Bindable.setOf(ApplicationPropertyStorage.SecondStorage.class)
                        ).orElse(
                                Collections.emptySet()
                        );

        for (ApplicationPropertyStorage.SecondStorage storage: storages) {

            AnnotatedBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(KafkaVirtualStorageProcessor.class);

            beanDefinition.setScope("singleton");

            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, storage.getConsumerGroup());

            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, storage.getStorageType());

            registry.registerBeanDefinition(
                    "kafkaVirtualStorageProcessor%s"
                            .formatted(storage.getStorageType()),
                    beanDefinition
            );

        }

    }

}
