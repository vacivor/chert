package io.vacivor.chert.server.infrastructure.persistence.app.converter;

import io.vacivor.chert.server.common.AbstractCodeEnumConverter;
import io.vacivor.chert.server.domain.app.ApplicationSecretStatusEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApplicationSecretStatusCodeEnumConverter extends
    AbstractCodeEnumConverter<ApplicationSecretStatusEnum, Integer> {

  public ApplicationSecretStatusCodeEnumConverter() {
    super(ApplicationSecretStatusEnum.class);
  }
}
