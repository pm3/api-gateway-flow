package com.aston.model.def;

import java.util.List;
import java.util.Map;

public final class FlowDef {
    private String tenant;
    private String code;
    private List<FlowStepDef> steps;
    private String paramsRef;
    private String jsonSchemaPath;
    private Map<String, Object> response;
    private Map<String, String> labels;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<FlowStepDef> getSteps() {
        return steps;
    }

    public void setSteps(List<FlowStepDef> steps) {
        this.steps = steps;
    }

    public String getParamsRef() {
        return paramsRef;
    }

    public void setParamsRef(String paramsRef) {
        this.paramsRef = paramsRef;
    }

    public String getJsonSchemaPath() {
        return jsonSchemaPath;
    }

    public void setJsonSchemaPath(String jsonSchemaPath) {
        this.jsonSchemaPath = jsonSchemaPath;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "FlowDef{" + "tenant='" + tenant + '\'' + ", code='" + code + '\'' + '}';
    }
}

