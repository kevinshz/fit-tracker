package com.fit_tracker.security.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserDto {

    @NotBlank
    public String userName;

    @NotBlank
    public String password;
}
