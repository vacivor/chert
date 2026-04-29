package io.vacivor.chert.server.interfaces.dto.user;

import java.util.Set;

public record UserUpdateRequest(
    String password,
    Set<String> roles
) {
}
