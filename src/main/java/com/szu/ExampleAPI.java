package com.szu;

import com.google.gson.JsonObject;
import com.szu.annotation.Refrain;
import com.szu.annotation.Sonnet;
import com.szu.annotation.*;

@Sonnet("/api")
public class ExampleAPI {
    @Refrain
    ExampleClass exampleClass;

    @POSTRequest("/getAge")
    public JsonObject getAge(@JsonBody JsonObject params) {
        String name = params.get("name").getAsString();;
        JsonObject result = new JsonObject();
        result.addProperty("age", exampleClass.get(name));
        return result;
    }
}

