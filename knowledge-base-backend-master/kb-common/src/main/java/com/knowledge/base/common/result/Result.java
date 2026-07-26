package com.knowledge.base.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Unified response result wrapper class
 *
 * <p>Designed following the Alibaba Java Development Guidelines, all interfaces return responses in a unified format</p>
 *
 * @param <T> data type
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Response code
     */
    private Integer code;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data
     */
    private T data;

    /**
     * Timestamp
     */
    private Long timestamp;

    /**
     * Private constructor
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Private constructor
     *
     * @param code    response code
     * @param message response message
     * @param data    response data
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Success response (no data)
     *
     * @param <T> data type
     * @return unified response result
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * Success response (with data)
     *
     * @param data data
     * @param <T>  data type
     * @return unified response result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * Success response (custom message)
     *
     * @param message message
     * @param <T>     data type
     * @return unified response result
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    /**
     * Success response (custom message and data)
     *
     * @param message message
     * @param data    data
     * @param <T>     data type
     * @return unified response result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * Failure response (default error)
     *
     * @param <T> data type
     * @return unified response result
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * Failure response (custom message)
     *
     * @param message error message
     * @param <T>     data type
     * @return unified response result
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * Failure response (custom error code and message)
     *
     * @param code    error code
     * @param message error message
     * @param <T>     data type
     * @return unified response result
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * Failure response (using a result code enum)
     *
     * @param resultCode result code enum
     * @param <T>        data type
     * @return unified response result
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * Return success or failure based on a condition
     *
     * @param flag condition flag
     * @param <T>  data type
     * @return unified response result
     */
    public static <T> Result<T> status(boolean flag) {
        return flag ? success() : error();
    }
}
