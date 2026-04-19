package io.vacivor.chert.server.common;

import jakarta.persistence.AttributeConverter;

public abstract class AbstractCodeEnumConverter<E extends Enum<E> & CodeEnum<T>, T>
    implements AttributeConverter<E, T> {

  private final Class<E> enumClass;

  protected AbstractCodeEnumConverter(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  @Override
  public T convertToDatabaseColumn(E attribute) {
    return attribute == null ? null : attribute.getCode();
  }

  @Override
  public E convertToEntityAttribute(T dbData) {
    return dbData == null ? null : CodeEnumUtil.fromCode(enumClass, dbData);
  }
}