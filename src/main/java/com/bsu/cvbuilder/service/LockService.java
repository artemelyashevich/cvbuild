package com.bsu.cvbuilder.service;


import java.util.function.Supplier;

public interface LockService {

    <T> T withLock(String lockName, Supplier<T> supplier);

    void clear();
}
