package com.szu;

import com.google.gson.JsonObject;
import com.szu.annotation.Refrain;
import com.szu.annotation.Sonnet;
import com.szu.annotation.*;

import java.util.Map;

@Sonnet("/api")
public class ExampleAPI {
    @Refrain
    ExampleFunction exampleFunction;

    @GETRequest("/hello")
    public JsonObject response(@QueryParam Map<String, String> Params) {
        String name = Params.get("name");
        JsonObject result = new JsonObject();
        result.addProperty("message", exampleFunction.hello() + " " + name);
        return result;
    }
}

