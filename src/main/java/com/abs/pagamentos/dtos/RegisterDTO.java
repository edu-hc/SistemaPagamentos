package com.abs.pagamentos.dtos;

import com.abs.pagamentos.model.user.UserType;

public record RegisterDTO(String email, String password, UserType role) {
}
