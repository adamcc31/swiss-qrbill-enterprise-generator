package com.exata.swissqrbill.model;

import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private List<ValidationError> errors;

    public ApiResponse() {}

    public ApiResponse(boolean success, T data, List<ValidationError> errors) {
        this.success = success;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, List.of());
    }

    public static <T> ApiResponse<T> error(List<ValidationError> errors) {
        return new ApiResponse<>(false, null, errors);
    }

    public static <T> ApiResponse<T> error(String field, String message) {
        return new ApiResponse<>(false, null, List.of(new ValidationError(field, message)));
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }
}
