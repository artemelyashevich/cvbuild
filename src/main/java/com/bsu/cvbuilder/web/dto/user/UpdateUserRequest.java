package com.bsu.cvbuilder.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    @NotBlank(message = "Firstname must be not null")
    private String firstName;

    @NotBlank(message = "Lastname must be not null")
    private String lastName;

    private String email;
}
