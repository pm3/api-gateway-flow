package com.aston.model.def;

import java.util.List;

public class FlowStepDef {
    private String code;
    private FlowStepType type = FlowStepType.CASE;
    private List<FlowWorkerDef> workers;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public FlowStepType getType() {
        return type;
    }

    public void setType(FlowStepType type) {
        this.type = type;
    }

    public List<FlowWorkerDef> getWorkers() {
        return workers;
    }

    public void setWorkers(List<FlowWorkerDef> workers) {
        this.workers = workers;
    }
}
