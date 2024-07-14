package com.aston;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.aston.flow.FlowCaseManager;
import com.aston.flow.ITaskSender;
import com.aston.flow.store.FlowTaskEntity;

public class TestTaskSender implements ITaskSender {

    private FlowCaseManager flowCaseManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public void setFlowCaseManager(FlowCaseManager flowCaseManager) {
        this.flowCaseManager = flowCaseManager;
    }

    @Override
    public void sendTask(FlowTaskEntity task, String method, String path, Map<String, String> headers, Object params) {
        executor.execute(()->{
            try{
                Thread.sleep(100);
                boolean ok = true;
                Map<String, Object> resp = new HashMap<>();
                if(path.equals("/flow/echo") && params instanceof Map paramsMap){
                    resp.putAll(paramsMap);
                }
                if(path.equals("/flow/sum") && params instanceof Map paramsMap){
                    int a = Integer.parseInt(paramsMap.get("a").toString());
                    int b = Integer.parseInt(paramsMap.get("b").toString());
                    int c = a+b;
                    resp.put("c", c);
                    if(c>100) throw new Exception("big sun "+c);
                }
                flowCaseManager.finishTask(task.getId(), ok, resp);
            }catch (Exception e){
                e.printStackTrace();
                flowCaseManager.finishTask(task.getId(), false, Map.of("error", e.getMessage()));
            }
        });
    }
}
