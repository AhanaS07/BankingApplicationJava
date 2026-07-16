package com.tnf.common_dto.dto.customer;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    @NotBlank(message = "Line1 Cannot be empty")
    private String line1;
    private String line2;

    @NotBlank(message = "City Cannot be empty")
    private String city;

    @NotBlank(message = "State Cannot be empty")
    private String state;

    @NotBlank(message = "Zip Cannot be empty")
    private String zip;

    @NotBlank(message = "Country Cannot be empty")
    private String country;
}
