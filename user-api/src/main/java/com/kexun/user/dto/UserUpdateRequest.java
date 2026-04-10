package com.kexun.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UserUpdateRequest {
    @NotBlank
    private String name;

    public UserUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
