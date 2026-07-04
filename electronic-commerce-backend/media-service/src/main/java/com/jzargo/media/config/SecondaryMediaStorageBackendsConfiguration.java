package com.jzargo.media.config;

import com.jzargo.media.storages.persistent.MediaPersistentStorageBackendNative;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Set;

import static com.jzargo.media.config.ApplicationPropertyStorage.STORAGES_PROPERTIES;

public class SecondaryMediaStorageBackendsConfiguration
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<ApplicationPropertyStorage.SecondStorage> storages =
                Binder.get(environment)
                        .bind(
                                STORAGES_PROPERTIES,
                                Bindable.setOf(ApplicationPropertyStorage.SecondStorage.class)
                        ).orElse(
                                Collections.emptySet()
                        );

        for (ApplicationPropertyStorage.SecondStorage storage: storages) {
            switch (storage.getStorageType()) {

                case NATIVE_DISK -> {

                    var beanDefinition = new GenericBeanDefinition();

                    beanDefinition.setBeanClass(MediaPersistentStorageBackendNative.class);

                    registry.registerBeanDefinition("mediaPersistentStorageBackendNative", beanDefinition);
                }

                default -> throw new BeanCreationException("Unknown storage type was supplied: " + storage.getStorageType());
            }
        }
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }
}
