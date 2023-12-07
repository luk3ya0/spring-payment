package com.luke.payment.util;

import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

import static com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders.*;

public class WechatPay2Validator4Request {
    protected static final Logger log = LoggerFactory.getLogger(com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator.class);
    protected static final long RESPONSE_EXPIRED_MINUTES = 5L;
    protected final Verifier verifier;
    protected final String requestId;
    protected final String body;

    public WechatPay2Validator4Request(Verifier verifier, String requestId, String body) {
        this.verifier = verifier;
        this.requestId = requestId;
        this.body = body;
    }

    protected static IllegalArgumentException parameterError(String message, Object... args) {
        message = String.format(message, args);
        return new IllegalArgumentException("parameter error: " + message);
    }

    protected static IllegalArgumentException verifyFail(String message, Object... args) {
        message = String.format(message, args);
        return new IllegalArgumentException("signature verify fail: " + message);
    }

    public final boolean validate(HttpServletRequest request) {
        try {
            this.validateParameters(request);
            String message = this.buildMessage(request);
            String serial = request.getHeader(WECHAT_PAY_SERIAL);
            String signature = request.getHeader(WECHAT_PAY_SIGNATURE);
            if (!this.verifier.verify(serial, message.getBytes(StandardCharsets.UTF_8), signature)) {
                throw verifyFail("serial=[%s] message=[%s] sign=[%s], request-id=[%s]",
                        serial, message, signature, requestId);
            } else {
                return true;
            }
        } catch (IllegalArgumentException var5) {
            log.warn(var5.getMessage());
            return false;
        }
    }

    protected final void validateParameters(HttpServletRequest request) {
        if (requestId == null) {
            throw parameterError("empty Request-ID");
        } else {
            String[] headers = new String[]{WECHAT_PAY_SERIAL, WECHAT_PAY_SIGNATURE, WECHAT_PAY_NONCE, WECHAT_PAY_TIMESTAMP};

            for (String headerName : headers) {
                if (request.getHeader(headerName)== null) {
                    throw parameterError("empty [%s], request-id=[%s]", headerName, requestId);
                }
            }

            String timestampStr = request.getHeader(WECHAT_PAY_TIMESTAMP);

            try {
                Instant responseTime = Instant.ofEpochSecond(Long.parseLong(timestampStr));
                if (Duration.between(responseTime, Instant.now()).abs().toMinutes() >= RESPONSE_EXPIRED_MINUTES) {
                    throw parameterError("timestamp=[%s] expires, request-id=[%s]", timestampStr, requestId);
                }
            } catch (NumberFormatException | DateTimeException var10) {
                throw parameterError("invalid timestamp=[%s], request-id=[%s]", timestampStr, requestId);
            }
        }
    }

    protected final String buildMessage(HttpServletRequest request) {
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        return timestamp + "\n" + nonce + "\n" + this.body + "\n";
    }

}
