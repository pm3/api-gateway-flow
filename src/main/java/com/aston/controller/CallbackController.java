package com.aston.controller;

import java.util.Map;

import com.aston.flow.FlowCaseManager;
import com.aston.user.UserContext;
import com.aston.user.UserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Controller("/api-gateway-flow")
@SecurityRequirement(name = "ApiKeyAuth")
@ApiResponse(responseCode = "200", description = "ok")
@ApiResponse(responseCode = "401", description = "authorization required")
public class CallbackController {

    private final FlowCaseManager flowCaseManager;
    private final ObjectMapper objectMapper;

    public CallbackController(FlowCaseManager flowCaseManager, ObjectMapper objectMapper) {
        this.flowCaseManager = flowCaseManager;
        this.objectMapper = objectMapper;
    }

    @Operation(tags = {"internal"})
    @Post(uri = "/response/{taskId}", consumes = MediaType.APPLICATION_JSON)
    public void response(@PathVariable String taskId,
                         @Nullable @Header("xgw-status") Integer callbackStatus,
                         @Body byte[] data,
                         @Parameter(hidden = true) UserContext userContext){

        boolean ok = callbackStatus==null || (callbackStatus>=200 && callbackStatus<299);
        try {
            Object root = objectMapper.readValue(data, Object.class);
            flowCaseManager.finishTask(taskId, ok, root);
        }catch (Exception e){
            flowCaseManager.finishTask(taskId, false, Map.of("error","body not parse to json "+e.getMessage()));
            throw new UserException("body not parse to json");
        }
    }

}
