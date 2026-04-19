package io.vacivor.chert.client;

import java.util.concurrent.Executor;
/**
 * 客户端配置监听器，用于配置发生变化时的通知。
 */
public interface ChertConfigListener {

  /**
   * 当配置发生变化时触发回调。
   *
   * @param content 最新的配置内容
   */
  void onChange(String content);

  /**
   * 获取此监听器的执行器。如果返回 null，则在主通知线程中执行。
   * 参考 Nacos/Diamond 的设计，允许用户自定义回调执行线程。
   *
   * @return 执行器，默认为 null
   */
  default Executor getExecutor() {
    return null;
  }

}
