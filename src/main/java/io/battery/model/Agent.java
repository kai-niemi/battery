package io.battery.model;

import org.springframework.hateoas.Link;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

/**
 * A remote Battery instance that this instance can control through its REST API, for example
 * to launch scenarios or list workers on several instances from the shell.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Agent {
    @NotNull
    private String url;

    private String name;

    @JsonIgnore
    public Link indexLink() {
        String path = (getUrl().endsWith("/") ? "api" : "/api");
        return Link.of(getUrl() + path);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
