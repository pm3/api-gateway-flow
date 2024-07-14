package com.aston.model.def;

public class JwtIssuerDef {
    private String issuer;
    private String url;
    private String aud;
    private String tenant;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public String toString() {
        return "JwtIssuerDef{" + "issuer='" + issuer + '\'' + ", tenant='" + tenant + '\'' + '}';
    }
}
