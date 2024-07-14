package com.aston.flow.store;

import java.util.List;
import java.util.Optional;

import com.aston.micronaut.sql.aop.Query;
import com.aston.micronaut.sql.aop.SqlApi;
import com.aston.micronaut.sql.convert.JsonConverterFactory;
import com.aston.micronaut.sql.entity.Format;
import com.aston.model.FlowTask;

@SqlApi
public interface IFlowTaskStore {

    Optional<FlowTaskEntity> loadById(String id);

    void insert(FlowTaskEntity flowCase);

    @Query("""
           update flow_task set
           finished=current_timestamp,
           response=:response
           where id=:id
           """)
    void finishFlowOk(String id, @Format(JsonConverterFactory.JSON)Object response);

    @Query("""
           update flow_task set
           finished=current_timestamp,
           error=:error
           where id=:id
           """)
    void finishFlowError(String id, @Format(JsonConverterFactory.JSON)Object error);

    @Query("""
           update flow_task set
           started=current_timestamp
           where id=:id
           """)
    void startRunning(String id);

    @Query("select * from flow_task where flowCaseId=:flowCaseId order by created")
    List<FlowTask> selectFlowTaskByCaseId(String flowCaseId);

    @Query("select * from flow_task where flowCaseId=:flowCaseId order by created")
    List<FlowTaskEntity> selectTaskByCaseId(String flowCaseId);

    @Query("delete from flow_task where id=:id")
    void deleteTask(String id);

    @Query("""
        select count(*)
        from flow_task
        where flowCaseId=:flowCaseId
        and step=:step
        and started is null
    """)
    int selectCountStepWaiting(String flowCaseId, String step);

    @Query("""
        select count(*)
        from flow_task
        where flowCaseId=:flowCaseId
        and step=:step
        and finished is null
    """)
    int selectCountStepOpen(String flowCaseId, String step);

    @Query("delete from flow_task where flowCaseId=:flowCaseId")
    void deleteTasksByCaseId(String flowCaseId);
}
