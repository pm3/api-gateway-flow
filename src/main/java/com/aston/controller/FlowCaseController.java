package com.aston.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.aston.blob.BlobStore;
import com.aston.flow.FlowCaseManager;
import com.aston.flow.WaitingFlowCaseManager;
import com.aston.model.Callback;
import com.aston.model.FlowAsset;
import com.aston.model.FlowCase;
import com.aston.model.FlowCaseCreate;
import com.aston.model.IdValue;
import com.aston.user.UserContext;
import com.aston.user.UserException;
import com.aston.utils.BaseValid;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api-gateway-flow")
@SecurityRequirement(name = "BasicAuth")
@SecurityRequirement(name = "BearerAuth")
@ApiResponse(responseCode = "200", description = "ok")
@ApiResponse(responseCode = "401", description = "authorization required")
@ApiResponse(responseCode = "403", description = "forbidden")
public class FlowCaseController {

    private final static Logger LOGGER = LoggerFactory.getLogger(FlowCaseController.class);

    private final FlowCaseManager flowCaseManager;
    private final WaitingFlowCaseManager waitingFlowCaseManager;
    private final BlobStore blobStore;
    private final ObjectMapper objectMapper;

    public FlowCaseController(FlowCaseManager flowCaseManager,
                              WaitingFlowCaseManager waitingFlowCaseManager,
                              BlobStore blobStore,
                              ObjectMapper objectMapper) {
        this.flowCaseManager = flowCaseManager;
        this.waitingFlowCaseManager = waitingFlowCaseManager;
        this.blobStore = blobStore;
        this.objectMapper = objectMapper;
    }

    @Operation(tags = {"case"})
    @Post("/case")
    public IdValue createCase(@Body FlowCaseCreate caseCreate,
                              @Parameter(hidden = true) UserContext userContext) {
        BaseValid.str(caseCreate.caseType(), 1, 128, "type");
        BaseValid.str(caseCreate.externalId(), -1, 128, "externalId");
        String caseId = UUID.randomUUID().toString().replaceAll("-", "");
        flowCaseManager.createFlow(userContext.tenant(), caseId, caseCreate);
        return new IdValue(caseId);
    }

    @Operation(tags = {"case"})
    @Get("/case/{id}")
    public CompletableFuture<FlowCase> fetchCase(@PathVariable String id,
                                             @QueryValue @Nullable Integer waitTimeSeconds,
                                             @QueryValue @Nullable Boolean full,
                                             @Parameter(hidden = true) UserContext userContext){

        CompletableFuture<FlowCase> future = new CompletableFuture<>();
        FlowCase flowCase = flowCaseManager.loadFlow(userContext.tenant(), id);
        if (waitTimeSeconds!=null && waitTimeSeconds>0 && flowCase!=null && flowCase.getFinished()==null) {
            if(waitTimeSeconds>55) waitTimeSeconds = 55;
            waitingFlowCaseManager.waitFinish(flowCase, future, waitTimeSeconds);
        } else {
            future.complete(flowCase);
        }
        if(full!=null && full){
            future = future.thenApply(this::loadFullFlow);
        }
        future = future.thenApply(this::createAssetsUrl);
        return future;
    }

    private FlowCase loadFullFlow(FlowCase flowCase) {
        if(flowCase!=null && flowCase.getFinished()!=null && flowCase.getTasks()==null) {
            try{
                FlowCase finalCase = blobStore.loadFinalCase(flowCase.getTenant(), flowCase.getId());
                if(finalCase!=null) return finalCase;
            }catch (Exception e){
                LOGGER.debug("error load final case {}",e.getMessage(), e);
                return flowCase;
            }
        }
        return flowCase;
    }

    private FlowCase createAssetsUrl(FlowCase flowCase) {
        if(flowCase!=null && flowCase.getAssets()!=null){
            for(FlowAsset asset : flowCase.getAssets()){
                try{
                    asset.setUrl(blobStore.createAssetUrl(flowCase.getTenant(), asset.getId()));
                }catch (Exception e){
                    LOGGER.debug("error createAssetsUrl {} {}", asset.getId(), e.getMessage(), e);
                }
            }
        }
        return flowCase;
    }


    @Operation(tags = {"case"})
    @Post(uri = "/asset", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public IdValue createAsset(
            @NonNull @Part CompletedFileUpload file,
            @Nullable @Part String caseType,
            @Nullable @Part String externalId,
            @Nullable @Part CompletedFileUpload params,
            @Nullable String callbackUrl,
            @Nullable @Part CompletedPart calbackHeaders,
            @Parameter(hidden = true) UserContext userContext) {

        String fileName = file.getFilename();
        if (fileName != null && !fileName.matches("^[A-Za-z0-9-_,\\s]{1,60}[.]{1}[A-Za-z0-9]{3,4}$"))
            fileName = null;

        Map<String, Object> parsedParams = null;
        if(params!=null){
            try{
                MapType type = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
                parsedParams = objectMapper.readValue(params.getInputStream(), type);
            }catch (Exception e){
                throw new UserException("params not cast to json object");
            }
        }

        Callback callback = null;
        if(callbackUrl!=null){
            Map<String, String> headers = new HashMap<>();
            if(calbackHeaders!=null){
                try{
                    MapType type = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
                    headers = objectMapper.readValue(calbackHeaders.getBytes(), type);
                }catch (Exception e){
                    throw new UserException("callback headers not cast to map");
                }
            }
            callback = new Callback(callbackUrl, headers);
        }

        String assetId = UUID.randomUUID().toString().replaceAll("-", "");
        try{
            blobStore.saveAsset(userContext.tenant(), assetId, file.getBytes(), fileName);
        }catch (Exception e){
            LOGGER.debug("error saveAsset {} {}", assetId, e.getMessage(), e);
            throw new UserException("save asset error "+e.getMessage());
        }

        if (caseType != null) {
            BaseValid.str(caseType, 1, 128, "type");
            BaseValid.str(externalId, -1, 128, "externalId");

            FlowCaseCreate create = new FlowCaseCreate(
                    caseType,
                    externalId,
                    List.of(assetId),
                    parsedParams,
                    callback);
            flowCaseManager.createFlow(userContext.tenant(), assetId, create);
        }
        return new IdValue(assetId);
    }
}