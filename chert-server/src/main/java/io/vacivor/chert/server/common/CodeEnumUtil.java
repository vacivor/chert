package io.vacivor.chert.server.common;

public class CodeEnumUtil {
    public static <T, E extends Enum<E> & CodeEnum<T>> E fromCode(Class<E> enumClass, T code) {
      if (code == null) {
        return null;
      }
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException(
            "Unknown code " + code + " for " + enumClass.getSimpleName());
    }
}