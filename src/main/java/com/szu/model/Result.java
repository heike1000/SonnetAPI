package com.szu.model;

public class Result {
    public int status;
    public String body;

    public Result(int status, String body) {
        this.status = status;
        this.body = body;
    }
}