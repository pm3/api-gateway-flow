package com.aston.flow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    public BaseTaskSender(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.workerApiKey = appConfig.getWorkerApiKey()!=null && appConfig.getWorkerApiKey().length()>1 ? appConfig.getWorkerApiKey().getBytes(StandardCharsets.UTF_8) : null;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendTask(FlowTaskEntity task, String method, String path, Map<String, String> headers, Object params) throws Exception {
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
        String callbackPath = "/api-gateway-flow/response/"+ task.getId();
        b.header("x-gw-callback", new URI(appConfig.getFlowAppUrl()).resolve(callbackPath).toString());
        String apiKey = workerApiKey!=null ? Hash.hmacSha1(callbackPath.getBytes(StandardCharsets.UTF_8), workerApiKey) : "1234567890";
        b.header("x-gw-callback-x-api-key", apiKey);
        HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
        LOGGER.info("send task {} response {} {}", task.getId(), resp.statusCode(), resp.body());
    }
}
