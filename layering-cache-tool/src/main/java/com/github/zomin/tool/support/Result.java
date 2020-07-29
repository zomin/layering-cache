package com.github.zomin.tool.support;

import java.io.Serializable;
import java.util.UUID;

/**
 * 与前端交互对象
 *
 * @param <T>
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 5925101851082556646L;
    private T data;
    private String requestId;
    private String code;
    private String message;

    public Result() {
        this.requestId = UUID.randomUUID().toString();
    }


    public static Result success() {
        Result result = new Result<>();
        result.setCode("200");
        result.setMessage(Status.SUCCESS.name());
        return result;
    }

    public static <T> Result success(T data) {
        Result<T> result = new Result<>();
        result.setCode("200");
        result.setMessage(Status.SUCCESS.name());
        result.setData(data);
        return result;
    }

    public static <T> Result error(String msg) {
        Result<T> result = new Result<>();
        result.setCode("500");
        result.setMessage(msg);
        return result;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public static enum Status {

        SUCCESS("OK"),
        ERROR("ERROR");

        private String code;

        private Status(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

}
