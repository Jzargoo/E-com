package com.jzargo.media.config.balancing;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.TargetSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinTargetSource implements TargetSource {
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
}
