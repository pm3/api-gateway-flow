package com.aston.flow;

import java.util.Map;

import com.aston.flow.store.FlowTaskEntity;

public interface ITaskSender {
    void setFlowCaseManager(FlowCaseManager flowCaseManager);

    void sendTask(FlowTaskEntity task,
                  String method,
                  String path,
                  Map<String, String> headers,
                  Object params,
                  boolean blocked
                 ) throws Exception;
}
