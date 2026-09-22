package com.jzargo.media.grpc;

@FunctionalInterface
public interface Checker {
    void check() throws CheckIsNotSatisfiedException;
}
