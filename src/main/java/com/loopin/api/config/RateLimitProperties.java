package com.loopin.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private List<Policy> policies = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public static class Policy {

        private String name;
        private boolean enabled = true;
        private int requests;
        private Duration window = Duration.ofMinutes(1);
        private List<String> methods = new ArrayList<>();
        private List<String> paths = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
}
