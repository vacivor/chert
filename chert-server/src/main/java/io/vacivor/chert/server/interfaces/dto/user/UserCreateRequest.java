package io.vacivor.chert.server.interfaces.dto.user;

import java.util.Set;

public record UserCreateRequest(
    String username,
    String email,
    String password,
    Set<String> roles
) {
}
