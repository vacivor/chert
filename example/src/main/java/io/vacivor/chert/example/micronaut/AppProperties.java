package io.vacivor.chert.example.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.runtime.context.scope.Refreshable;

@ConfigurationProperties("app")
@Refreshable
public class AppProperties {
    private String title = "Default Title";
    private int version = 1;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
