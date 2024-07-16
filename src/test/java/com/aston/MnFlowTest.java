package com.aston;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.aston.blob.BlobStore;
import com.aston.controller.FlowCaseController;
import com.aston.flow.FlowCaseManager;
import com.aston.flow.FlowDefStore;
import com.aston.flow.WaitingFlowCaseManager;
import com.aston.flow.store.IFlowCaseStore;
import com.aston.flow.store.IFlowTaskStore;
import com.aston.model.FlowCase;
import com.aston.model.FlowCaseCreate;
import com.aston.model.IdValue;
import com.aston.span.ISpanSender;
import com.aston.user.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = {"local"})
@SuppressWarnings("unchecked")
public class MnFlowTest {
    @Inject
    EmbeddedApplication<?> application;

    @Inject
    ObjectMapper objectMapper;

    public FlowCaseManager flowCaseManager(){
        TestTaskSender taskSender = new TestTaskSender();
        FlowCaseManager flowCaseManager = new FlowCaseManager(
                application.getApplicationContext().getBean(BlobStore.class),
                application.getApplicationContext().getBean(IFlowCaseStore.class),
                application.getApplicationContext().getBean(IFlowTaskStore.class),
                taskSender,
                application.getApplicationContext().getBean(FlowDefStore.class),
                application.getApplicationContext().getBean(WaitingFlowCaseManager.class),
                application.getApplicationContext().getBean(ISpanSender.class)
        );
        taskSender.setFlowCaseManager(flowCaseManager);
        return flowCaseManager;
    }

    public FlowCaseController flowCaseController(){
        return new FlowCaseController(
                flowCaseManager(),
                application.getApplicationContext().getBean(WaitingFlowCaseManager.class),
                application.getApplicationContext().getBean(BlobStore.class),
                objectMapper
        );
    }

    public FlowCase testFlow(FlowCaseCreate create) throws Exception {
        UserContext userContext = new UserContext("test", "aston");
        FlowCaseController flowCaseController = flowCaseController();
        IdValue value = flowCaseController.createCase(create, userContext);
        System.out.println(value.id());
        FlowCase flowCase = null;
        for(int i=0;i<40; i++) {
            CompletableFuture<FlowCase> future = flowCaseController.fetchCase(value.id(), 5, false, userContext);
            flowCase = future.get();
            if(flowCase.getFinished()!=null) break;
            try{
                Thread.sleep(250);
            }catch (Exception ignore){
            }
        }
        return flowCase;
    }

    @Test
    public void testAsset() {
        UserContext userContext = new UserContext("test", "aston");
        FlowCaseController flowCaseController = flowCaseController();
        CompletedFileUpload file = new TestCompletedFileUpload("file", "test.txt",
                                                               MediaType.TEXT_PLAIN_TYPE, "test".getBytes(StandardCharsets.UTF_8));
        IdValue value = flowCaseController.createAsset(file,null,null, null, null, null, userContext);
        System.out.println(value.id());

    }

    //assets
    //88759e854cb04743be0578d4353e6d7b

    @Test
    public void test1() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("a", "1");
        params.put("b", "1");
        FlowCaseCreate create = new FlowCaseCreate("flow1", null, List.of(), params, null);
        FlowCase flowCase = testFlow(create);
        System.out.println(objectMapper.writeValueAsString(flowCase));
        Assertions.assertNotNull(flowCase.getFinished(), "not finished case");
        Assertions.assertNotNull(flowCase.getResponse(), "case response");
        Assertions.assertInstanceOf(Map.class, flowCase.getResponse());
        Map<String, Object> resp = (Map<String, Object>) flowCase.getResponse();
        Assertions.assertEquals(16, resp.get("sum"), "flow Case sum");
    }

    @Test
    public void test2() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("a", "1");
        params.put("b", "1");
        FlowCaseCreate create = new FlowCaseCreate("flow2", null, List.of("88759e854cb04743be0578d4353e6d7b", "d7f3ef99771445988a2f7163594e5cdd"), params, null);
        FlowCase flowCase = testFlow(create);
        System.out.println(objectMapper.writeValueAsString(flowCase));
        Assertions.assertNotNull(flowCase.getFinished(), "not finished case");
        Assertions.assertNotNull(flowCase.getResponse(), "case response");
        Assertions.assertInstanceOf(Map.class, flowCase.getResponse());
    }

}