package com.aston.flow;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.aston.model.def.BaseAuthDef;
import com.aston.model.def.FlowDef;
import com.aston.model.def.FlowStepDef;
import com.aston.model.def.FlowStepType;
import com.aston.model.def.FlowWorkerDef;
import com.aston.model.def.JwtIssuerDef;
import com.aston.model.def.TenantDef;
import com.aston.user.UserContext;
import com.aston.user.UserException;
import com.aston.utils.BaseValid;
import com.aston.utils.Hash;
import com.aston.utils.JwtVerify;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowDefStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(FlowDefStore.class);

    private final JwtVerify jwtVerify;
    private final Map<String, FlowDef> flowsMap = new ConcurrentHashMap<>();
    private final Map<String, BaseAuthDef> baseAuthMap = new ConcurrentHashMap<>();
    private final Map<String, FlowWorkerDef> workerMap = new ConcurrentHashMap<>();
    private final ObjectMapper yamlObjectMapper;

    public FlowDefStore(HttpClient httpClient, ObjectMapper objectMapper) {
        this.jwtVerify = new JwtVerify(httpClient, objectMapper);
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.registerModule(new JavaTimeModule());
    }

    public FlowDef flowDef(String tenant, String type){
        return flowsMap.get(tenant+"-"+type);
    }

    public UserContext baseAuth(String user, String password){
        String key = user+":"+ Hash.sha2(password.getBytes(StandardCharsets.UTF_8));
        BaseAuthDef baseAuthDef = baseAuthMap.get(key);
        return baseAuthDef!=null ? new UserContext(baseAuthDef.getTenant(), baseAuthDef.getLogin()) : null;
    }

    public UserContext verifyJwt(DecodedJWT decodedJWT) throws Exception{
        return jwtVerify.verify(decodedJWT);
    }

    public void loadRoot(File rootDir, boolean clear) {
        LOGGER.info("start reload configs, clear {}", clear);
        if(!rootDir.isDirectory()) {
            throw new UserException("invalid root dir "+rootDir.getAbsolutePath());
        }
        if(clear) {
            flowsMap.clear();
            workerMap.clear();
            baseAuthMap.clear();
            jwtVerify.clear();
        }
        for(File f : Objects.requireNonNull(rootDir.listFiles())){
            if(f.isDirectory()){
                File tenantFile = new File(f, f.getName()+".yaml");
                if(tenantFile.exists()){
                    try{
                        loadTenant(f, tenantFile);
                    }catch (Exception e){
                        LOGGER.warn("error load tenant config {} - {}",f.getName(), e.getMessage());
                    }
                } else {
                    LOGGER.warn("ignore dir {}, no tenant file",f.getAbsolutePath());
                }
            }
        }
    }

    public void loadTenant(File dir, File tenantFile) throws IOException {
        LOGGER.info("start load tenant {}", dir.getName());
        TenantDef tenantDef = yamlObjectMapper.readValue(tenantFile, TenantDef.class);
        BaseValid.require(tenantDef, "tenant");
        BaseValid.code(tenantDef.getCode(), "tenant.code");
        if(tenantDef.getAdminUsers()==null) tenantDef.setAdminUsers(new ArrayList<>());
        if(tenantDef.getJwtIssuers()==null) tenantDef.setJwtIssuers(new ArrayList<>());
        if(tenantDef.getAdminUsers().isEmpty() && tenantDef.getJwtIssuers().isEmpty()){
            throw new UserException("tenant without users "+tenantDef.getCode());
        }

        for(BaseAuthDef baseAuth : tenantDef.getAdminUsers()){
            BaseValid.require(baseAuth, "baseAuth");
            BaseValid.require(baseAuth.getLogin(), "baseAuth.login");
            BaseValid.require(baseAuth.getPassword(), "baseAuth.password");
            baseAuth.setTenant(tenantDef.getCode());
            baseAuthMap.put(baseAuth.getLogin()+":"+baseAuth.getPassword(), baseAuth);
        }

        for(JwtIssuerDef issuer : tenantDef.getJwtIssuers()){
            BaseValid.require(issuer, "issuer");
            BaseValid.require(issuer.getIssuer(), "issuer.issuer");
            BaseValid.require(issuer.getAud(), "issuer.audience");
            BaseValid.require(issuer.getUrl(), "issuer.url");
            jwtVerify.addIssuer(issuer.getIssuer(), issuer.getAud(), issuer.getUrl(), tenantDef.getCode());
        }

        LOGGER.info("start load tenant {} flows", dir.getName());
        for(File f : Objects.requireNonNull(dir.listFiles())){
            if(f.isFile() && f.getName().endsWith(".yaml") && !f.equals(tenantFile)){
                try{
                    LOGGER.info("start load tenant {} flow {}", dir.getName(), f.getName());
                    loadFlow(tenantDef, f);
                }catch (Exception e){
                    LOGGER.warn("ignore flow file {} - {}", f.getAbsolutePath(), e.getMessage());
                }
            }
        }

    }

    private void loadFlow(TenantDef tenantDef, File flowFile) throws IOException {
        FlowDef flowDef = yamlObjectMapper.readValue(flowFile, FlowDef.class);
        flowDef.setTenant(tenantDef.getCode());
        BaseValid.require(flowDef, "flow");
        BaseValid.code(flowDef.getCode(), "flow.code");

        if(flowDef.getSteps()==null || flowDef.getSteps().isEmpty()){
            throw new UserException("empty flow.steps");
        }
        for(FlowStepDef step : flowDef.getSteps()){
            BaseValid.require(step, "step");
            BaseValid.code(step.getCode(), "step.code");
            if(step.getType()==null) step.setType(FlowStepType.CASE);
            if(step.getWorkers()==null || step.getWorkers().isEmpty()){
                throw new UserException("empty step.workers");
            }
            for(FlowWorkerDef worker : step.getWorkers()){
                BaseValid.require(worker, "worker");
                BaseValid.code(worker.getCode(), "worker.code");
                BaseValid.str(worker.getPath(), -1, 512,"worker.path");
                BaseValid.str(worker.getPathExpr(), -1, 512,"worker.$path");
                if(worker.getPath()==null && worker.getPathExpr()==null){
                    throw new UserException("require worker.path or worker.$path");
                }
            }
        }
        flowsMap.put(flowDef.getTenant()+"-"+flowDef.getCode(), flowDef);
    }

    public FlowWorkerDef cacheWorker(FlowDef flowDef, String step, String worker) {
        String key = flowDef.getTenant()+"|"+flowDef.getCode()+"|"+step+"|"+worker;
        FlowWorkerDef workerDef = workerMap.get(key);
        if(workerDef==null){
            for(FlowStepDef step2 : flowDef.getSteps()){
                for(FlowWorkerDef worker2 : step2.getWorkers()){
                    String key2 = flowDef.getTenant()+"|"+flowDef.getCode()+"|"+step2.getCode()+"|"+worker2.getCode();
                    workerMap.put(key2, worker2);
                }
            }
            workerDef = workerMap.get(key);
        }
        return workerDef;
    }
}
