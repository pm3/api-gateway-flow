package com.aston.model.def;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TenantDef {

    private String code;
    private String name;
    private List<BaseAuthDef> adminUsers = new ArrayList<>();
    private List<JwtIssuerDef> jwtIssuers = new ArrayList<>();
    private Map<String, String> labels;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BaseAuthDef> getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(List<BaseAuthDef> adminUsers) {
        this.adminUsers = adminUsers;
    }

    public List<JwtIssuerDef> getJwtIssuers() {
        return jwtIssuers;
    }

    public void setJwtIssuers(List<JwtIssuerDef> jwtIssuers) {
        this.jwtIssuers = jwtIssuers;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "TenantDef{" + "code='" + code + '\'' + '}';
    }
}
