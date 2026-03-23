package com.qiushui1012.mod.multiyggdrasil.util;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@Setter
public class Pair<K, V> {
    private K k;
    private V v;

    public V getOrCompute(Supplier<V> computer) {
        return this.v == null ? this.v = computer.get() : this.v;
    }
}