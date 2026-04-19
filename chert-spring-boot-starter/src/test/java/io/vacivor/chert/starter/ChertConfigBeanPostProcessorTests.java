package io.vacivor.chert.starter;

import io.vacivor.chert.client.ChertClient;
import io.vacivor.chert.client.ChertConfig;
import io.vacivor.chert.client.ChertConfigListener;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChertConfigBeanPostProcessorTests {

  @Test
  void shouldInvokeAnnotatedMethodOnUpdate() {
    ChertClient client = mock(ChertClient.class);
    ChertConfigBeanPostProcessor processor = new ChertConfigBeanPostProcessor(client);
    
    TestBean bean = new TestBean();
    processor.postProcessAfterInitialization(bean, "testBean");
    
    ArgumentCaptor<ChertConfigListener> listenerCaptor = ArgumentCaptor.forClass(ChertConfigListener.class);
    verify(client).addListener(listenerCaptor.capture());
    
    ChertConfigListener listener = listenerCaptor.getValue();
    listener.onChange("new content");
    
    assertThat(bean.content).isEqualTo("new content");
  }

  @Test
  void shouldRegisterListenerWithResource() {
    ChertClient client = mock(ChertClient.class);
    ChertConfigBeanPostProcessor processor = new ChertConfigBeanPostProcessor(client);

    ResourceTestBean bean = new ResourceTestBean();
    processor.postProcessAfterInitialization(bean, "resourceTestBean");

    verify(client).addListener(eq("other.yml"), any(ChertConfigListener.class));
  }

  static class TestBean {
    String content;
    
    @ChertConfig
    void onUpdate(String content) {
      this.content = content;
    }
  }

  static class ResourceTestBean {
    @ChertConfig(resource = "other.yml")
    void onUpdate(String content) {}
  }

}
