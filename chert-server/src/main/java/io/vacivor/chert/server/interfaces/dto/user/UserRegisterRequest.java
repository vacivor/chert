package io.vacivor.chert.server.interfaces.dto.user;

public record UserRegisterRequest(
    String username,
    String email,
    String password
) {
}
