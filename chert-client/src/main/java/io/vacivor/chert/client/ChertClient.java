package io.vacivor.chert.client;


import java.util.Map;

public interface ChertClient extends AutoCloseable {

  ChertConfigResponse fetchConfig();

  /**
   * 获取指定资源的配置。
   *
   * @param configName 配置资源名称
   * @return 配置响应
   */
  ChertConfigResponse fetchConfig(String configName);

  /**
   * 获取键值对形式的配置。仅在配置资源类型为 ENTRIES 时可用。
   */
  Map<String, String> fetchEntries();

  /**
   * 获取指定资源的键值对形式配置。
   *
   * @param configName 配置资源名称
   * @return 配置项 Map
   */
  Map<String, String> fetchEntries(String configName);

  /**
   * 注册配置更新监听器，监听默认配置。
   */
  void addListener(ChertConfigListener listener);

  /**
   * 注册指定配置资源的更新监听器。
   * 参考 Nacos/Diamond，支持按资源 ID 进行监听。
   *
   * @param configName 配置资源名称
   * @param listener 监听器
   */
  void addListener(String configName, ChertConfigListener listener);

  /**
   * 更新键值对配置并发布。仅在配置资源类型为 ENTRIES 时可用。
   */
  void updateEntries(Map<String, String> entries);

  /**
   * 更新指定配置资源的键值对并发布。
   *
   * @param configName 配置资源名称
   * @param entries  待更新的条目
   */
  void updateEntries(String configName, Map<String, String> entries);

  @Override
  default void close() {
  }

}
