package io.vacivor.chert.server.application.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vacivor.chert.server.config.ChertServerProperties;
import io.vacivor.chert.server.domain.config.ReleaseMessage;
import io.vacivor.chert.server.infrastructure.persistence.config.ReleaseMessageRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseMessageScannerTests {

  @Mock
  private ReleaseMessageRepository releaseMessageRepository;
  @Mock
  private ChertServerProperties properties;

  @InjectMocks
  private ReleaseMessageScanner scanner;

  @BeforeEach
  void setUp() {
    // Initialized state
  }

  @Test
  void shouldInitializeOnStartup() {
    ReleaseMessage latest = new ReleaseMessage();
    latest.setId(100L);
    when(releaseMessageRepository.findFirstByOrderByIdDesc()).thenReturn(latest);

    scanner.initialize();

    verify(releaseMessageRepository).findFirstByOrderByIdDesc();
  }

  @Test
  void shouldScanAndProcessMessages() {
    // 1. Initialize
    when(releaseMessageRepository.findFirstByOrderByIdDesc()).thenReturn(null);
    scanner.initialize();

    // 2. Add listener
    ReleaseMessageListener listener = org.mockito.Mockito.mock(ReleaseMessageListener.class);
    scanner.addMessageListener(listener);

    // 3. Scan
    ReleaseMessage msg1 = new ReleaseMessage();
    msg1.setId(1L);
    msg1.setAppId("app1");
    msg1.setEnvCode("env1");
    msg1.setName("config1");
    
    when(releaseMessageRepository.findFirst100ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of(msg1));

    scanner.scanMessages();

    verify(releaseMessageRepository).findFirst100ByIdGreaterThanOrderByIdAsc(0L);
    verify(listener).handleMessage(msg1);
  }

  @Test
  void shouldHandleGapsAndScanMissingMessages() {
    // 1. Initialize at ID 0
    when(releaseMessageRepository.findFirstByOrderByIdDesc()).thenReturn(null);
    scanner.initialize();

    ReleaseMessageListener listener = org.mockito.Mockito.mock(ReleaseMessageListener.class);
    scanner.addMessageListener(listener);

    // 2. Scan with a gap: last=0, current message ID=2. Missing ID=1.
    ReleaseMessage msg2 = new ReleaseMessage();
    msg2.setId(2L);
    msg2.setAppId("app1");
    msg2.setEnvCode("env1");
    msg2.setName("config2");

    when(releaseMessageRepository.findFirst100ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of(msg2));

    scanner.scanMessages();

    verify(listener).handleMessage(msg2);

    // 3. Now verify that missing ID 1 is being searched
    ReleaseMessage msg1 = new ReleaseMessage();
    msg1.setId(1L);
    msg1.setAppId("app1");
    msg1.setEnvCode("env1");
    msg1.setName("config1");

    when(releaseMessageRepository.findAllById(List.of(1L))).thenReturn(List.of(msg1));
    // No more new messages
    when(releaseMessageRepository.findFirst100ByIdGreaterThanOrderByIdAsc(2L)).thenReturn(List.of());

    scanner.scanMessages();

    verify(releaseMessageRepository).findAllById(List.of(1L));
    verify(listener).handleMessage(msg1);
  }
}
