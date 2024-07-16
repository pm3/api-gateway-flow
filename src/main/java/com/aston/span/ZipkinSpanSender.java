package com.aston.span;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.model.def.FlowDef;
import com.aston.model.def.FlowWorkerDef;
import com.aston.utils.SuperTimer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Requires(property = "zipkin.url")
public class ZipkinSpanSender implements ISpanSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(ZipkinSpanSender.class);

    private final String zipkinUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private List<ZipkinSpan> cache = Collections.synchronizedList(new ArrayList<>());

    public ZipkinSpanSender(@Value("${zipkin.url}") String zipkinUrl,
                            HttpClient httpClient,
                            ObjectMapper objectMapper,
                            SuperTimer superTimer) {
        this.zipkinUrl = zipkinUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        Executor executor = Executors.newSingleThreadExecutor();
        superTimer.schedulePeriod(10_000L, ()->executor.execute(this::send));
    }

    private void send() {
        if(!cache.isEmpty()){
            List<ZipkinSpan> cache0 = this.cache;
            this.cache = Collections.synchronizedList(new ArrayList<>());
            sendCache(cache0);
        }
    }

    private void sendCache(List<ZipkinSpan> cache0) {
        try{
            String data = objectMapper.writeValueAsString(cache0);
            HttpRequest r = HttpRequest
                    .newBuilder()
                    .uri(new URI(zipkinUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .build();
            HttpResponse<String> resp = httpClient.send(r, HttpResponse.BodyHandlers.ofString());
            if(resp.statusCode()!=202) throw new Exception("status code "+resp.statusCode()+" "+resp.body());
            LOGGER.info("send {}", cache0.getFirst().getName());
        }catch (Exception e){
            LOGGER.error("send to zipkin {}", e.getMessage());
        }
    }

    private String createId(){
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0,16);
    }

    @Override
    public void createFlow(FlowCaseEntity flowCase) {
        ZipkinSpan span = new ZipkinSpan();
        span.setTraceId(flowCase.getId());
        span.setName("flow-start");
        span.setId(createId());
        span.setParentId(flowCase.getId().substring(0,16));
        span.setTimestamp(flowCase.getCreated().toEpochMilli()*1000);
        span.setDuration(1000);
        span.setLocalEndpoint(new ZipkinEndpoint("/flow/"+flowCase.getTenant()+"/"+flowCase.getCaseType()));
        cache.add(span);
        sendCache(List.of(span));
    }

    @Override
    public void finishFlow(FlowCaseEntity flowCase, FlowDef flowDef, String error) {
        ZipkinSpan span = new ZipkinSpan();
        span.setTraceId(flowCase.getId());
        span.setName("flow");
        span.setId(flowCase.getId().substring(0,16));
        span.setTimestamp(flowCase.getCreated().toEpochMilli()*1000);
        span.setDuration(Duration.between(flowCase.getCreated(), flowCase.getFinished()).toMillis()*1000);
        span.setLocalEndpoint(new ZipkinEndpoint("/flow/"+flowCase.getTenant()+"/"+flowCase.getCaseType()));
        span.setTags(new HashMap<>());
        if(error!=null) {
            span.getTags().put("error", error);
            span.setName(span.getName()+" error");
        }
        if(flowDef.getLabels()!=null && !flowDef.getLabels().isEmpty()){
            flowDef.getLabels().forEach((k,v)->span.getTags().put("flow."+k, v));
        }
        if(flowCase.getExternalId()!=null) {
            span.getTags().put("case.externalId", flowCase.getExternalId());
        }
        span.getTags().put("case.id", flowCase.getId());
        span.getTags().put("case.type", flowCase.getCaseType());
        span.getTags().put("tenant", flowCase.getTenant());
        cache.add(span);
        sendCache(List.of(span));
    }

    @Override
    public void finishWaitingTask(FlowCaseEntity flowCase, FlowTaskEntity task, String error) {
        ZipkinSpan span = new ZipkinSpan();
        span.setTraceId(flowCase.getId());
        span.setName("task-waiting");
        span.setId(createId());
        span.setParentId(task.getId().substring(0,16));
        span.setTimestamp(task.getCreated().toEpochMilli()*1000);
        span.setDuration(Duration.between(task.getCreated(), task.getStarted()).toMillis()*1000);
        if(span.getDuration()<3000) span.setDuration(3000);
        span.setLocalEndpoint(new ZipkinEndpoint("/flow/"+flowCase.getTenant()+"/"+flowCase.getCaseType()+"/"+task.getStep()+(task.getAssetId()!=null ? "/"+task.getAssetId() : "")+"/"+task.getWorker()));
        if(error!=null){
            span.setTags(new HashMap<>());
            span.getTags().put("error", error);
            span.setName(span.getName()+" error");
        }
        cache.add(span);
        sendCache(List.of(span));
    }

    @Override
    public void finishRunningTask(FlowCaseEntity flowCase, FlowTaskEntity task, FlowWorkerDef workerDef, int statusCode, String error) {
        ZipkinSpan span = new ZipkinSpan();
        span.setTraceId(flowCase.getId());
        span.setName("task-running");
        span.setId(createId());
        span.setParentId(task.getId().substring(0,16));
        span.setTimestamp(task.getStarted().toEpochMilli()*1000);
        span.setDuration(Duration.between(task.getStarted(), task.getFinished()).toMillis()*1000);
        span.setLocalEndpoint(new ZipkinEndpoint("/flow/"+flowCase.getTenant()+"/"+flowCase.getCaseType()+"/"+task.getStep()+(task.getAssetId()!=null ? "/"+task.getAssetId() : "")+"/"+task.getWorker()));
        span.setTags(new HashMap<>());
        if(error!=null){
            span.getTags().put("error", error);
            span.setName(span.getName()+" error");
        }
        span.getTags().put("http.method", workerDef.getMethod());
        span.getTags().put("http.path", workerDef.getPath()!=null? workerDef.getPath() : workerDef.getPathExpr());
        span.getTags().put("http.status_code", task.getError()!=null ? "400":"200");
        cache.add(span);
        sendCache(List.of(span));
    }

    @Override
    public void finishTask(FlowCaseEntity flowCase, FlowTaskEntity task, FlowWorkerDef workerDef) {
        ZipkinSpan span = new ZipkinSpan();
        span.setTraceId(flowCase.getId());
        span.setName("task");
        span.setId(task.getId().substring(0,16));
        span.setTimestamp(task.getCreated().toEpochMilli()*1000);
        span.setDuration(Duration.between(task.getCreated(), task.getFinished()).toMillis()*1000);
        span.setLocalEndpoint(new ZipkinEndpoint("/flow/"+flowCase.getTenant()+"/"+flowCase.getCaseType()+"/"+task.getStep()+(task.getAssetId()!=null ? "/"+task.getAssetId() : "")+"/"+task.getWorker()));
        span.setTags(new HashMap<>());
        if(workerDef.getLabels()!=null && !workerDef.getLabels().isEmpty()){
            span.getTags().putAll(workerDef.getLabels());
        }
        span.getTags().put("step", task.getStep());
        if(task.getAssetId()!=null) span.getTags().put("assetId", task.getAssetId());
        span.getTags().put("worker", workerDef.getCode());
        if(workerDef.getLabels()!=null){
            workerDef.getLabels().forEach((k,v)->span.getTags().put("worker."+k, v));
        }
        cache.add(span);
        sendCache(List.of(span));
    }
}
