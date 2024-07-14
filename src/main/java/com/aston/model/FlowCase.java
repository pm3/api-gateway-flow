package com.aston.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.aston.micronaut.sql.convert.JsonConverterFactory;
import com.aston.micronaut.sql.entity.Format;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class FlowCase {
    private String id;
    private String tenant;
    private String caseType;
    private String externalId;
    @Format(JsonConverterFactory.JSON)
    private Callback callback;

    @Format(JsonConverterFactory.JSON)
    private Map<String, Object> params;
    @Format(JsonConverterFactory.JSON)
    private List<FlowAsset> assets;
    @Format(JsonConverterFactory.JSON)
    private Object response;

    private Instant created;
    private Instant finished;
    private String state;

    @Format(JsonConverterFactory.JSON)
    private List<FlowTask> tasks;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public List<FlowAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<FlowAsset> assets) {
        this.assets = assets;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getFinished() {
        return finished;
    }

    public void setFinished(Instant finished) {
        this.finished = finished;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<FlowTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<FlowTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        return "FlowCase{" + "id='" + id + '\'' + ", tenant='" + tenant + '\'' + ", caseType='" + caseType + '\'' +
                ", externalId='" + externalId + '\'' + '}';
    }
}