package com.drathveloper.pocgrpcclient.dto;

import java.time.LocalDate;

public record UserDto(
        String username,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        AddressDto address) {
}
