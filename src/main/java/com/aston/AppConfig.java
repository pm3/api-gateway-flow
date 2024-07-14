package com.aston;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppConfig {
    private String workerApiKey;
    private String appGatewayUrl;
    private String flowAppUrl;

    public String getWorkerApiKey() {
        return workerApiKey;
    }

    public void setWorkerApiKey(String workerApiKey) {
        this.workerApiKey = workerApiKey;
    }

    public String getAppGatewayUrl() {
        return appGatewayUrl;
    }

    public void setAppGatewayUrl(String appGatewayUrl) {
        this.appGatewayUrl = appGatewayUrl;
    }

    public String getFlowAppUrl() {
        return flowAppUrl;
    }

    public void setFlowAppUrl(String flowAppUrl) {
        this.flowAppUrl = flowAppUrl;
    }
}
