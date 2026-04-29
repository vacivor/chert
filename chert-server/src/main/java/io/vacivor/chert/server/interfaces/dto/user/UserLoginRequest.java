package io.vacivor.chert.server.interfaces.dto.user;

public record UserLoginRequest(
    String username,
    String password
) {
}
