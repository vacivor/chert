package io.vacivor.chert.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 Chert 配置更新的监听器。
 * 被标记的方法通常接收一个 String 类型的参数（配置内容）。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChertConfig {

  /**
   * 指定监听的资源名称。如果不指定，则使用默认资源。
   */
  String resource() default "";

}
