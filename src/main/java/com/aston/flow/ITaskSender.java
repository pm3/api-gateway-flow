package com.aston.flow;

import java.util.Map;

import com.aston.flow.store.FlowTaskEntity;

public interface ITaskSender {
    void sendTask(FlowTaskEntity task,
                  String method,
                  String path,
                  Map<String, String> headers,
                  Object params
                 ) throws Exception;
}
