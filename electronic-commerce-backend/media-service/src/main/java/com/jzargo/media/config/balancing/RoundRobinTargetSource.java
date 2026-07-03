package com.jzargo.media.config.balancing;

import com.jzargo.media.exceptions.BackendOutOfSpaceException;
import com.jzargo.media.exceptions.CannotAddFileIntoStorageException;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.TargetSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinTargetSource implements TargetSource, MethodInterceptor {
    private final List<?> targets;
    private final AtomicInteger counter;
    private final Class<?> targetClass;
    private final MediaPersistentStorageBackendRegistry mediaPersistentStorageBackendRegistry;

    public RoundRobinTargetSource(List<?> targets, Class<?> targetClass, MediaPersistentStorageBackendRegistry mediaPersistentStorageBackendRegistry) {
        this.targets = targets;
        this.mediaPersistentStorageBackendRegistry = mediaPersistentStorageBackendRegistry;
        this.counter = new AtomicInteger(0);
        this.targetClass = targetClass;
    }

    @Override
    public @Nullable Class<?> getTargetClass() {
        return targetClass;
    }

    @Override
    public @Nullable Object getTarget() throws Exception {
        var index = Math.abs(
                counter.getAndIncrement() % targets.size()
        );

        return targets.get(index);
    }

    @Override
    public void releaseTarget(@NonNull Object target) throws Exception {
        // Spring manages with it
    }


    @Override
    public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (BackendOutOfSpaceException | CannotAddFileIntoStorageException e) {
            mediaPersistentStorageBackendRegistry
                    .removeBackend(
                            (MediaPersistentStorageBackend)
                                    Objects.requireNonNull(invocation.getThis())
                    );
            // TODO: implement a watcher that will send a request to check whether storage become an active in some period of time or 0 if it should not work
            log.error("Backend out of space or cannot add file into the storage. The Service was removed from a registry", e);
            // TODO: start watcher right here. It should be stored in a registry to be self-healing
            throw new RuntimeException("Error occurred in media persistent storage backend. " + e.getLocalizedMessage());
        }
    }
}
