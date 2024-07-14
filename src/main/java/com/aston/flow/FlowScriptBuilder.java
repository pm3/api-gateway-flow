package com.aston.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aston.blob.BlobStore;
import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.model.FlowAsset;
import com.aston.utils.FlowScript;

public class FlowScriptBuilder {

    public static FlowScript createTaskCtx(FlowCaseEntity flowCase, List<FlowTaskEntity> tasks, FlowTaskEntity task, BlobStore blobStore) {
        FlowScript.LazyMap map = new FlowScript.LazyMap();
        map.put("case", flowCase);
        Map<String, FlowScript.LazyMap> steps = new HashMap<>();
        for(FlowTaskEntity task2 : tasks){
            String stepName = task2.getAssetId()!=null ? task2.getStep()+"_"+task2.getAssetId() : task2.getStep();
            FlowScript.LazyMap stepMap = steps.computeIfAbsent(stepName, (k)-> new FlowScript.LazyMap());
            taskResponseToMap(task2, stepMap);
            if(task!=null && Objects.equals(task.getStep(), task2.getStep()) && Objects.equals(task.getAssetId(), task2.getAssetId())){
                taskResponseToMap(task2, map);
            }
        }
        map.put("step", steps);
        if(task!=null && task.getAssetId()!=null){
            map.put("asset", taskAsset(flowCase, task.getAssetId(),blobStore));
        }
        return FlowScript.create(map);
    }

    private static FlowAsset taskAsset(FlowCaseEntity flowCase, String assetId, BlobStore blobStore) {
        for(FlowAsset a : flowCase.getAssets()){
            if(a.getId().equals(assetId)){
                try{
                    return new FlowAsset(assetId, a.getExtName(), blobStore.createAssetUrl(flowCase.getTenant(), assetId));
                }catch (Exception ignore){}
            }
        }
        return new FlowAsset(assetId, null, null);
    }

    private static void taskResponseToMap(FlowTaskEntity task, FlowScript.LazyMap stepMap) {
        if(task.getFinished()==null) {
            stepMap.put(task.getWorker(), new WaitingException(task.toString()));
        } else if(task.getResponse()!=null) {
            stepMap.put(task.getWorker(), task.getResponse());
        } else if(task.getError()!=null) {
            stepMap.put(task.getWorker(), new TaskResponseException(task.toString()));
            stepMap.put(task.getWorker()+"_error", task.getError());
        } else {
            stepMap.put(task.getWorker(), task.getResponse());
        }
    }
}
