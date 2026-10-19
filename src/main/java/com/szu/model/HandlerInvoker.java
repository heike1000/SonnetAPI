package com.szu.model;

import com.google.gson.JsonElement;

@FunctionalInterface
public interface HandlerInvoker {
    JsonElement invoke(Object arg) throws Throwable;
}
