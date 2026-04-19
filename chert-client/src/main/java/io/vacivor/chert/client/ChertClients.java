package io.vacivor.chert.client;

public final class ChertClients {

  private ChertClients() {
  }

  public static ChertClient create(ChertClientConfig config) {
    return new HttpChertClient(config);
  }

}
