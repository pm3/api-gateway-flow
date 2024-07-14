package com.aston.flow.store;

import java.util.Map;
import java.util.Optional;

import com.aston.micronaut.sql.aop.Query;
import com.aston.micronaut.sql.aop.SqlApi;
import com.aston.micronaut.sql.convert.JsonConverterFactory;
import com.aston.micronaut.sql.entity.Format;
import com.aston.model.FlowCase;

@SqlApi
public interface IFlowCaseStore {

    Optional<FlowCaseEntity> loadById(String id);

    void insert(FlowCaseEntity flowCase);

    @Query("""
           update flow_case
           set finished=current_timestamp,
           response=:response,
           state='FINISHED'
           where id=:id
           """)
    void finishFlow(String id, @Format(JsonConverterFactory.JSON) Map<String, Object> response);

    @Query("""
           update flow_case
           set state=:state
           where id=:id
           """)
    void updateFlowState(String id, String state);

    @Query("""
           select *
           from flow_case
           where id=:id
           """)
    FlowCase loadFlowCaseById(String id);
}
