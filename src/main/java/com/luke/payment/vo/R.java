package com.luke.payment.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class R {

    // Responsive Code
    private Integer code;
    // Responsive Message
    private String message;
    // A Map to store other thing
    private Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();
        return r.setCode(0).setMessage("succeed");
    }

    public static R error() {
        R r = new R();
        return r.setCode(-1).setMessage("failed");
    }

    public R data(String key, Object value) {
        this.data.put(key, value);

        return this;
    }
}
