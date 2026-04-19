package io.vacivor.chert.server.domain.app;

import io.vacivor.chert.server.common.CodeEnum;

public enum ApplicationSecretStatusEnum implements CodeEnum<Integer> {
  ACTIVE(1),
  DISABLED(2);

  private final int code;

  ApplicationSecretStatusEnum(int code) {
    this.code = code;
  }

  @Override
  public Integer getCode() {
    return code;
  }
}
