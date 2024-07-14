package com.aston.flow.store;

import java.time.Instant;

import com.aston.micronaut.sql.convert.JsonConverterFactory;
import com.aston.micronaut.sql.entity.Format;
import com.aston.micronaut.sql.entity.Table;

@Table(name = "flow_task")
public class FlowTaskEntity {
    private String id;
    private String flowCaseId;
    private String step;
    private String worker;
    private String assetId;

    @Format(JsonConverterFactory.JSON)
    private Object response;

    @Format(JsonConverterFactory.JSON)
    private Object error;

    private Instant created;
    private Instant started;
    private Instant finished;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFlowCaseId() {
        return flowCaseId;
    }

    public void setFlowCaseId(String flowCaseId) {
        this.flowCaseId = flowCaseId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getStarted() {
        return started;
    }

    public void setStarted(Instant started) {
        this.started = started;
    }

    public Instant getFinished() {
        return finished;
    }

    public void setFinished(Instant finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "Task "+step+"/"+worker+" case="+flowCaseId+" id="+id;
    }
}
