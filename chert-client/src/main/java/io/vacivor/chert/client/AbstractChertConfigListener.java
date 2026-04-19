package io.vacivor.chert.client;

import java.util.concurrent.Executor;

/**
 * Chert 配置监听器的抽象实现，提供了 getExecutor() 的默认实现（返回 null）。
 * 用户可以继承此类并重写 getExecutor() 以指定回调执行线程。
 */
public abstract class AbstractChertConfigListener implements ChertConfigListener {

  @Override
  public Executor getExecutor() {
    return null;
  }

}
