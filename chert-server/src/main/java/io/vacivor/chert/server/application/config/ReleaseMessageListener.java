package io.vacivor.chert.server.application.config;

import io.vacivor.chert.server.domain.config.ReleaseMessage;

public interface ReleaseMessageListener {

  void handleMessage(ReleaseMessage message);
}
