package com.aston.flow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.aston.AppConfig;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.utils.Hash;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BaseTaskSender implements ITaskSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(BaseTaskSender.class);

    private final AppConfig appConfig;
    private final byte[] workerApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor blockedExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private FlowCaseManager flowCaseManager;

    public BaseTaskSender(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.workerApiKey = appConfig.getWorkerApiKey()!=null && appConfig.getWorkerApiKey().length()>1 ? appConfig.getWorkerApiKey().getBytes(StandardCharsets.UTF_8) : null;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setFlowCaseManager(FlowCaseManager flowCaseManager) {
        this.flowCaseManager = flowCaseManager;
    }

    @Override
    public void sendTask(FlowTaskEntity task, String method, String path, Map<String, String> headers, Object params, boolean blocked) throws Exception {
        URI uri = new URI(path);
        if(uri.getHost()==null){
            uri = new URI(appConfig.getAppGatewayUrl()).resolve(path);
        }
        HttpRequest.Builder b = HttpRequest.newBuilder();
        b.uri(uri);
        HttpRequest.BodyPublisher publisher = params !=null
                                              ? HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                params))
                                              : HttpRequest.BodyPublishers.noBody();
        b.method(method, publisher);
        if(headers !=null){
            headers.forEach(b::header);
        }
        b.header("x-flow-id", task.getFlowCaseId());
        b.header("x-flow-task-id", task.getId());
        b.header("X-B3-TraceId", task.getFlowCaseId());
        b.header("X-B3-SpanId", task.getId().substring(0,15)+"2");
        if(blocked){
            blockedExecutor.execute(()->sendBlocked(task, b));
        } else {
            sendWithCallback(task, b);
        }
    }

    private void sendWithCallback(FlowTaskEntity task, HttpRequest.Builder b) throws Exception {
        String callbackPath = "/api-gateway-flow/response/"+ task.getId();
        b.header("x-gw-callback", new URI(appConfig.getFlowAppUrl()).resolve(callbackPath).toString());
        String apiKey = workerApiKey!=null ? Hash.hmacSha1(callbackPath.getBytes(StandardCharsets.UTF_8), workerApiKey) : "1234567890";
        b.header("x-gw-callback-x-api-key", apiKey);

        HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
        LOGGER.info("send task {} response {} {}", task.getId(), resp.statusCode(), resp.body());
    }

    private void sendBlocked(FlowTaskEntity task, HttpRequest.Builder b) {
        try{
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            Object root = resp.body();
            if(resp.statusCode()>=200 && resp.statusCode()<300){
                try {
                    root = objectMapper.readValue(resp.body(), Object.class);
                }catch (Exception e){
                    LOGGER.info("send task {} parse response error {}", task.getId(), e.getMessage());
                }
            }
            flowCaseManager.finishTask(task.getId(), resp.statusCode(), root);
        }catch (Exception e) {
            LOGGER.info("send task {} error {}", task.getId(), e.getMessage());
            flowCaseManager.finishTask(task.getId(), 500, Map.of("error", e.getMessage()));
        }
    }
}
