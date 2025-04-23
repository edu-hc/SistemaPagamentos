package com.abs.pagamentos.dtos;

import com.abs.pagamentos.model.user.UserType;

import java.math.BigDecimal;

public record UserDTO(String firstName, String lastName, String document, BigDecimal balance, String email, String password, UserType userType) {
}
