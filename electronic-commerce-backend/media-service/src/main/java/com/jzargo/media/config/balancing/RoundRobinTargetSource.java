package com.jzargo.media.config.balancing;

import com.jzargo.media.exceptions.BackendOutOfSpaceException;
import com.jzargo.media.exceptions.CannotAddFileIntoStorageException;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.TargetSource;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinTargetSource implements TargetSource, MethodInterceptor {
    private final List<?> targets;
    private final AtomicInteger counter;
    private final Class<?> targetClass;

    public RoundRobinTargetSource(List<?> targets, Class<?> targetClass) {
        this.targets = targets;
        this.counter = new AtomicInteger(0);
        this.targetClass = targetClass;
    }

    @Override
    public @Nullable Class<?> getTargetClass() {
        return targetClass;
    }

    @Override
    public boolean isStatic() {
        return false;
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
            throw new RuntimeException(e);
        }
    }
}
