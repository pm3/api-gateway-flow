package com.aston.flow.store;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.aston.micronaut.sql.convert.JsonConverterFactory;
import com.aston.micronaut.sql.entity.Format;
import com.aston.micronaut.sql.entity.Table;
import com.aston.model.Callback;
import com.aston.model.FlowAsset;

@Table(name = "flow_case")
public class FlowCaseEntity {
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
    private Map<String, Object> response;

    private Instant created;
    private Instant finished;
    private String state;

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

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
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

    @Override
    public String toString() {
        return "FlowCaseEntity{" + "id='" + id + '\'' + ", tenant='" + tenant + '\'' + ", caseType='" + caseType +
                '\'' + ", externalId='" + externalId + '\'' + '}';
    }
}
