package com.szu;

import com.google.gson.JsonObject;
import com.szu.annotation.JsonBody;
import com.szu.annotation.PostRequest;
import com.szu.annotation.Refrain;
import com.szu.annotation.Sonnet;
import com.szu.annotation.*;

@Sonnet("/api")
public class ExampleAPI {
    @Refrain
    ExampleFunction exampleFunction;

    @PostRequest("/hello")
    public JsonObject response(@JsonBody JsonObject json) {
        String name = json.get("name").getAsString();
        JsonObject result = new JsonObject();
        result.addProperty("status", "success");
        result.addProperty("message", exampleFunction.hello() + " " + name);
        return result;
    }
}

