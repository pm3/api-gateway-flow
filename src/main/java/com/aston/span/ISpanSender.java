package com.aston.span;

import com.aston.flow.store.FlowCaseEntity;
import com.aston.flow.store.FlowTaskEntity;
import com.aston.model.def.FlowDef;
import com.aston.model.def.FlowWorkerDef;

public interface ISpanSender {

    void createFlow(FlowCaseEntity flowCase);

    void finishFlow(FlowCaseEntity flowCase, FlowDef flowDef, String error);

    void finishWaitingTask(FlowCaseEntity flowCase, FlowTaskEntity task, String error);

    void finishRunningTask(FlowCaseEntity flowCase, FlowTaskEntity task, FlowWorkerDef workerDef, String error);

    void finishTask(FlowCaseEntity flowCase, FlowTaskEntity task, FlowWorkerDef workerDef);
}
