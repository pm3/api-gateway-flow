package com.aston;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aston.flow.FlowScriptBuilder;
import com.aston.flow.WaitingException;
import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.model.def.FlowDef;
import com.aston.utils.FlowScript;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlowScriptTest {

    private FlowCaseEntity flowCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("flow-case.yaml"), FlowCaseEntity.class);
    }

    private FlowDef flowDef() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("flow-def.yaml"), FlowDef.class);
    }

    private List<FlowTaskEntity> tasks() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Tasks tasks = mapper.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream("tasks.yaml"), Tasks.class);
        return tasks.tasks;
    }

    public static class Tasks{
        List<FlowTaskEntity> tasks;

        public List<FlowTaskEntity> getTasks() {
            return tasks;
        }

        public void setTasks(List<FlowTaskEntity> tasks) {
            this.tasks = tasks;
        }
    }

    @Test
    public void testLoadDef() throws Exception {
        FlowDef def = flowDef();
        printJson(def);
    }

    @Test
    public void testScript1() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("a", "a");
        params.put("$b", "1");
        Map<String, Object> params2 = parseParams(0, params);
        printJson(params2);
        Assertions.assertEquals(1, params2.get("b"), "$b");
    }


    @Test
    public void testScriptTask0() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("$caseId", "case.id");
        params.put("$finish_worker_1", "step.init.worker_1");
        Map<String, Object> params2 = parseParams(0, params);
        printJson(params2);
        Assertions.assertEquals("flow-id", params2.get("caseId"), "$caseId");
        Assertions.assertNotNull(params2.get("finish_worker_1"), "$finish_worker_1");
    }

    @Test
    public void testScriptTask21() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("$worker_2", "worker_2");
        Map<String, Object> params2 = parseParams(2, params);
        printJson(params2);
        Assertions.assertNotNull(params2.get("worker_2"), "$worker_2");
    }

    @Test
    public void testScriptTask22() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("$worker_3", "worker_3");
        try {
            Map<String, Object> params2 = parseParams(2, params);
            Assertions.assertNull(params2, "expected exception");
        }catch (WaitingException e) {
            Assertions.assertNotNull(e);
        }
    }

    @Test
    public void testScriptTask222() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("$worker_3", "worker_3_error.error");
        Map<String, Object> params2 = parseParams(2, params);
        Assertions.assertNotNull(params2.get("worker_3"), "$worker_3");
    }

    @Test
    public void testScriptTask23() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("$worker_4", "worker_4");
        try {
            Map<String, Object> params2 = parseParams(2, params);
            Assertions.assertNull(params2, "expected exception");
        }catch (WaitingException e){
            Assertions.assertNotNull(e);
        }
    }

    private Map<String, Object> parseParams(int taskPos, Map<String, Object> params) throws Exception {
        List<FlowTaskEntity> tasks = tasks();
        FlowTaskEntity task = tasks.get(taskPos);
        FlowScript script = FlowScriptBuilder.createTaskCtx(flowCase(), tasks, task, null);
        return script.execMap(params);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    private static void printJson(Object val) throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
        System.out.println(json);
    }

}
