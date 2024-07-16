package com.aston;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.model.Callback;
import com.aston.model.FlowAsset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class CreateYaml {
    public static void main(String[] args) throws Exception {
        List<FlowTaskEntity> list = new ArrayList<>();

        for(int i=0; i<10; i++){
            FlowTaskEntity task = new FlowTaskEntity();
            task.setId(UUID.randomUUID().toString().replace("-", ""));
            task.setFlowCaseId("flow-id");
            task.setStep("init");
            task.setWorker("worker-"+i);
            task.setAssetId("asset1");
            task.setResponse("");
            task.setError("");
            task.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            task.setStarted(task.getCreated());
            task.setFinished(task.getCreated());
            list.add(task);
        }

        FlowCaseEntity flowCaseEntity = new FlowCaseEntity();
        flowCaseEntity.setId("flow-id");
        flowCaseEntity.setTenant("aston");
        flowCaseEntity.setCaseType("type1");
        flowCaseEntity.setExternalId("12345");
        flowCaseEntity.setCallback(new Callback("/callback", Map.of("header", "value")));
        flowCaseEntity.setParams(Map.of("param1", "value1", "param2", 1));
        flowCaseEntity.setAssets(new ArrayList<>());
        flowCaseEntity.getAssets().add(new FlowAsset());
        flowCaseEntity.getAssets().get(0).setId("asset1");
        flowCaseEntity.setCreated(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        flowCaseEntity.setState("init");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MapType type = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);

        FlowScriptTest.Tasks tasks = new FlowScriptTest.Tasks();
        tasks.setTasks(list);
        String json1 = objectMapper.writeValueAsString(tasks);
        Map<String, Object> map1 = objectMapper.readValue(json1, type);
        String s1 = yaml.dump(map1);
        System.out.println(s1);

        String json2 = objectMapper.writeValueAsString(flowCaseEntity);
        Map<String, Object> map2 = objectMapper.readValue(json2, type);
        String s2 = yaml.dump(map2);
        System.out.println(s2);

    }

}
