package com.szu.model;

public class Result {
    public final int status;
    public final String body;

    public Result(int status, String body) {
        this.status = status;
        this.body = body;
    }
}