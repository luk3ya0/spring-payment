package com.luke.payment.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class R {

    // Responsive Code
    private Integer code;
    // Responsive Message
    private String message;
    // A Map to store other thing
    private Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();
        r.setCode(0);
        r.setMessage("succeed");

        return r;
    }

    public static R error() {
        R r = new R();
        r.setCode(-1);
        r.setMessage("failed");

        return r;
    }

    public R data(String key, Object value) {
        this.data.put(key, value);

        return this;
    }
}
