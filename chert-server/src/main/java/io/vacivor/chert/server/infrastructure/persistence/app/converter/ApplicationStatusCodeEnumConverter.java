package io.vacivor.chert.server.infrastructure.persistence.app.converter;

import io.vacivor.chert.server.common.AbstractCodeEnumConverter;
import io.vacivor.chert.server.domain.app.ApplicationStatusEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApplicationStatusCodeEnumConverter extends
    AbstractCodeEnumConverter<ApplicationStatusEnum, Integer> {

  public ApplicationStatusCodeEnumConverter() {
    super(ApplicationStatusEnum.class);
  }
}
