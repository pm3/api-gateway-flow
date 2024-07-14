package com.aston.flow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.aston.blob.BlobStore;
import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.flow.store.IFlowCaseStore;
import com.aston.flow.store.IFlowTaskStore;
import com.aston.model.FlowAsset;
import com.aston.model.FlowCase;
import com.aston.model.FlowCaseCreate;
import com.aston.model.def.FlowDef;
import com.aston.model.def.FlowStepDef;
import com.aston.model.def.FlowStepType;
import com.aston.model.def.FlowWorkerDef;
import com.aston.user.UserException;
import com.aston.utils.FlowScript;
import jakarta.inject.Singleton;
import ognl.OgnlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowCaseManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(FlowCaseManager.class);

    private final BlobStore blobStore;
    private final IFlowCaseStore caseStore;
    private final IFlowTaskStore taskStore;
    private final ITaskSender taskSender;
    private final FlowDefStore flowDefStore;
    private final WaitingFlowCaseManager waitingFlowCaseManager;
    private final ConcurrentHashMap<String, String> nextTasks = new ConcurrentHashMap<>();

    public FlowCaseManager(BlobStore blobStore,
                           IFlowCaseStore caseStore,
                           IFlowTaskStore taskStore,
                           ITaskSender taskSender,
                           FlowDefStore flowDefStore,
                           WaitingFlowCaseManager waitingFlowCaseManager) {
        this.blobStore = blobStore;
        this.caseStore = caseStore;
        this.taskStore = taskStore;
        this.taskSender = taskSender;
        this.flowDefStore = flowDefStore;
        this.waitingFlowCaseManager = waitingFlowCaseManager;
    }

    public void createFlow(String tenant, String id, FlowCaseCreate caseCreate) {

        FlowDef flowDef = flowDefStore.flowDef(tenant, caseCreate.caseType());
        if(flowDef==null){
            throw new UserException("invalid case type, tenant="+tenant+" case="+caseCreate.caseType());
        }
        List<FlowAsset> flowAssets = new ArrayList<>();
        if(caseCreate.assets()!=null){
            for(String assetId : caseCreate.assets()){
                try{
                    FlowAsset flowAsset = blobStore.loadAssetInfo(tenant, assetId);
                    if(flowAsset==null){
                        throw new UserException("invalid assetId "+assetId);
                    }
                    flowAsset.setUrl(null);
                    flowAssets.add(flowAsset);
                }catch (UserException e){
                    throw e;
                }catch (Exception e){
                    throw new UserException("load asset "+assetId);
                }
            }
        }

        FlowCaseEntity entity = new FlowCaseEntity();
        entity.setId(id);
        entity.setTenant(tenant);
        entity.setCaseType(caseCreate.caseType());
        entity.setExternalId(caseCreate.externalId());
        entity.setCallback(caseCreate.callback());
        entity.setParams(caseCreate.params());
        entity.setAssets(flowAssets);
        entity.setCreated(Instant.now());
        entity.setState("created");
        caseStore.insert(entity);
        nextTask1(id, id);
    }

    public FlowCase loadFlow(String tenant, String id) {
        FlowCase flowCase = caseStore.loadFlowCaseById(id);
        if(flowCase!=null && !Objects.equals(tenant,flowCase.getTenant())){
            LOGGER.info("load flow from other tenant {} flow {}", tenant, flowCase.getId());
            return null;
        }
        return flowCase;
    }

    public void finishTask(String taskId, boolean ok, Object response) {
        FlowTaskEntity taskEntity = taskStore.loadById(taskId)
                                             .orElseThrow(()->new UserException("undefined taskId "+taskId));
        if(taskEntity.getFinished()!=null){
            throw new UserException("task is finished "+taskId);
        }
        if(ok){
            taskStore.finishFlowOk(taskId, response);
        } else {
            taskStore.finishFlowError(taskId, response);
        }
        nextTask1(taskEntity.getFlowCaseId(), taskId);
    }

    private void nextTask1(String caseFlowId, String taskId) {
        if(nextTasks.put(caseFlowId, taskId)==null){
            nextTask2(caseFlowId);
            String nextTaskId = nextTasks.remove(caseFlowId);
            if(nextTaskId!=null && !Objects.equals(nextTaskId, taskId)){
                nextTask1(caseFlowId, nextTaskId);
            }
        }
    }

    private void nextTask2(String flowCaseId) {
        FlowCaseEntity flowCaseEntity = caseStore.loadById(flowCaseId)
                                                 .orElseThrow(()->new UserException("invalid flowCaseId "+flowCaseId));
        FlowDef flowDef = flowDefStore.flowDef(flowCaseEntity.getTenant(), flowCaseEntity.getCaseType());
        if(flowDef==null) {
            throw new UserException("undefined flowDef "+flowCaseEntity.getTenant()+"/"+flowCaseEntity.getCaseType());
        }
        FlowStepDef aktStep = aktStep(flowDef, flowCaseEntity);
        if(aktStep!=null) {
            int stepWaitingTasksCount = taskStore.selectCountStepWaiting(flowCaseEntity.getId(), aktStep.getCode());
            if(stepWaitingTasksCount>0) {
                List<FlowTaskEntity> tasks = taskStore.selectTaskByCaseId(flowCaseId);
                List<FlowTaskEntity> stepWaitingTasks = tasks.stream()
                                        .filter(t->Objects.equals(t.getStep(), aktStep.getCode()) && t.getStarted()==null)
                                        .toList();
                for(FlowTaskEntity task : stepWaitingTasks) {
                    try{
                        boolean run = checkWaitingTask(flowDef, flowCaseEntity, tasks, task, false);
                        if(run) {
                            taskStore.startRunning(task.getId());
                        } else {
                            taskStore.deleteTask(task.getId());
                            tasks.remove(task);
                        }
                    }catch (WaitingException e){
                        LOGGER.debug(e.getMessage());
                    }
                }
                return;
            }
        }
        if(aktStep!=null) {
            int stepOpenTasksCount = taskStore.selectCountStepOpen(flowCaseEntity.getId(), aktStep.getCode());
            if(stepOpenTasksCount>0){
                //waiting to response
                return;
            }
        }
        //any waiting and any open tasks
        FlowStepDef nextStep = nextStep(flowDef, flowCaseEntity);
        if(nextStep!=null){
            saveNextStep(flowDef, flowCaseEntity, nextStep);
        } else {
            saveResponseAndFinish(flowDef, flowCaseEntity);
        }
    }

    private FlowStepDef aktStep(FlowDef flowDef, FlowCaseEntity flowCaseEntity) {
        if(flowCaseEntity.getState()==null || !flowCaseEntity.getState().startsWith("step-")) {
            return null;
        }
        return flowDef.getSteps().stream()
                      .filter(s->Objects.equals("step-"+s.getCode(), flowCaseEntity.getState()))
                .findFirst().orElse(null);
    }

    private FlowStepDef nextStep(FlowDef flowDef, FlowCaseEntity flowCaseEntity) {
        if(flowCaseEntity.getState()!=null) {
            if (flowCaseEntity.getState().equals("created")) {
                return flowDef.getSteps().getFirst();
            }
            if (flowCaseEntity.getState().startsWith("step-")) {
                for(Iterator<FlowStepDef> it = flowDef.getSteps().iterator(); it.hasNext();){
                    FlowStepDef step = it.next();
                    if(Objects.equals("step-"+step.getCode(), flowCaseEntity.getState()) && it.hasNext()){
                        return it.next();
                    }
                }
            }
        }
        return null;
    }

    private void saveNextStep(FlowDef flowDef, FlowCaseEntity flowCaseEntity, FlowStepDef step) {
        List<FlowTaskEntity> tasks = taskStore.selectTaskByCaseId(flowCaseEntity.getId());
        List<FlowTaskEntity> newTasks = new ArrayList<>();
        if(step.getType()==FlowStepType.CASE){
            //create new tasks
            for(FlowWorkerDef workerDef : step.getWorkers()){
                FlowTaskEntity taskEntity = createNewTask(flowCaseEntity, step, workerDef, null);
                newTasks.add(taskEntity);
                tasks.add(taskEntity);
            }
        } else if(step.getType()==FlowStepType.ASSET) {
            for(FlowAsset asset : flowCaseEntity.getAssets()){
                //create new tasks
                for(FlowWorkerDef workerDef : step.getWorkers()){
                    FlowTaskEntity taskEntity = createNewTask(flowCaseEntity, step, workerDef, asset.getId());
                    newTasks.add(taskEntity);
                    tasks.add(taskEntity);
                }
            }
        }
        caseStore.updateFlowState(flowCaseEntity.getId(), "step-"+step.getCode());
        //check waiting tasks
        for(FlowTaskEntity task : newTasks){
            try{
                boolean run = checkWaitingTask(flowDef, flowCaseEntity, tasks, task, true);
                if(run){
                    //running task
                    if(task.getStarted()==null) {
                        task.setStarted(Instant.now());
                    }
                    taskStore.insert(task);
                } else {
                    //task where condition is false
                    tasks.remove(task);
                }
            }catch (WaitingException e){
                //waiting task to other task
                taskStore.insert(task);
            }
        }
    }

    private FlowTaskEntity createNewTask(FlowCaseEntity flowCaseEntity,
                                                FlowStepDef step,
                                                FlowWorkerDef workerDef,
                                                String assetId) {
        FlowTaskEntity taskEntity = new FlowTaskEntity();
        taskEntity.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        taskEntity.setFlowCaseId(flowCaseEntity.getId());
        taskEntity.setStep(step.getCode());
        taskEntity.setWorker(workerDef.getCode());
        taskEntity.setAssetId(assetId);
        taskEntity.setCreated(Instant.now());
        return taskEntity;
    }

    private boolean checkWaitingTask(FlowDef flowDef, FlowCaseEntity flowCaseEntity, List<FlowTaskEntity> tasks, FlowTaskEntity task, boolean newTask) throws
                                                                                                                                                       WaitingException {
        FlowScript script = FlowScriptBuilder.createTaskCtx(flowCaseEntity, tasks, task, blobStore);
        FlowWorkerDef workerDef = flowDefStore.cacheWorker(flowDef, task.getStep(), task.getWorker());
        if(workerDef.getWhere()!=null){
            try {
                boolean resp = script.execWhere(workerDef.getWhere());
                if(!resp) return false;
            }catch (OgnlException e){
                LOGGER.warn("ignore task {} where {}, exec exception {}", task, workerDef.getWhere(), e.getMessage());
                return false;
            }
        }
        String error = null;
        try{
            sendTaskHttp(script, workerDef, task);
            task.setStarted(Instant.now());
        }catch (WaitingException e) {
                throw e;
        }catch (TaskResponseException e) {
            error = e.getMessage();
        }catch (OgnlException e) {
            error = "parse params error "+e.getMessage();
        }catch (Exception e) {
            error = "send to worker error "+e.getMessage();
        }
        if(error!=null){
            if(newTask){
                task.setStarted(task.getCreated());
                task.setFinished(task.getCreated());
                task.setError(Map.of("error", error));
                nextTask1(task.getFlowCaseId(), task.getId());
            } else {
                taskStore.finishFlowError(task.getId(), Map.of("error", error));
                nextTask1(task.getFlowCaseId(), task.getId());
            }
        }
        return true;
    }

    private void saveResponseAndFinish(FlowDef flowDef, FlowCaseEntity flowCaseEntity) {
        List<FlowTaskEntity> tasks = taskStore.selectTaskByCaseId(flowCaseEntity.getId());
        Map<String, Object> response = null;
        if(flowDef.getResponse()!=null) {
            try{
                FlowScript script = FlowScriptBuilder.createTaskCtx(flowCaseEntity, tasks, null, null);
                response = script.execMap(flowDef.getResponse());
            }catch (Exception e){
                LOGGER.debug("error calculate flow response {} error {}", flowDef.getResponse(), e.getMessage());
                response = Map.of("error", e.getMessage());
            }
        }
        caseStore.finishFlow(flowCaseEntity.getId(), response);
        //save to blob and clean db
        FlowCase flowCase = caseStore.loadFlowCaseById(flowCaseEntity.getId());
        flowCase.setTasks(taskStore.selectFlowTaskByCaseId(flowCaseEntity.getId()));
        try{
            blobStore.saveFinalCase(flowCaseEntity.getTenant(), flowCaseEntity.getId(), flowCase);
            taskStore.deleteTasksByCaseId(flowCaseEntity.getId());
        }catch (Exception e){
            LOGGER.warn("saveFinalCase {}", e.getMessage(), e);
        }
        waitingFlowCaseManager.finished(flowCase);
    }

    @SuppressWarnings("rawtypes")
    private void sendTaskHttp(FlowScript script, FlowWorkerDef workerDef, FlowTaskEntity task) throws Exception {

        String path = workerDef.getPath();

        if(path==null && workerDef.getPathExpr()!=null) {
            Object o = script.execExpr(workerDef.getPathExpr());
            path = o!=null ? o.toString() : null;
        }
        if(path==null) throw new UserException("task has empty task");

        Map<String,String> headers = null;
        if(workerDef.getHeaders()!=null){
            headers = script.execMapS(workerDef.getHeaders());
        }

        Object params = null;
        if(workerDef.getParams()!=null){
            params = script.execMap(workerDef.getParams());
            if (params instanceof Map paramsMap && paramsMap.size()==1 && paramsMap.containsKey("$.")){
                params = paramsMap.get("$.");
            }
        }
        taskSender.sendTask(task, workerDef.getMethod(), path, headers, params);
    }
}
