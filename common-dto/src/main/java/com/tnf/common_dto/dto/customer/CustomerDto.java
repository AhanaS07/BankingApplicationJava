package com.tnf.common_dto.dto.customer;


import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private String id;

    @NotBlank(message = "FirstName Cannot be empty")
    private String firstName;

    @NotBlank(message = "LastName Cannot be empty")
    private String lastName;

    @Email
    @NotBlank(message = "Email Cannot be empty")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$")
    @NotBlank(message = "Phone Number Cannot be empty")
    private String phone;

    private AddressDto address;
}