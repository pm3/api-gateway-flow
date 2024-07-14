package com.aston;

import javax.sql.DataSource;
import java.io.File;
import java.net.http.HttpClient;

import com.aston.blob.AzureBlobAuthBuilder;
import com.aston.blob.BlobStore;
import com.aston.flow.FlowDefStore;
import com.aston.utils.SuperTimer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Factory
public class AppFactory {

    @Singleton
    public SuperTimer superTimer(){
        return new SuperTimer();
    }

    @Singleton
    public HttpClient httpClient(){
        return HttpClient.newBuilder()
                         .version(HttpClient.Version.HTTP_1_1)
                         .build();
    }

    @Singleton
    public BlobStore blobStore(HttpClient httpClient,
                               ObjectMapper objectMapper,
                               @Value("${blob.url}") String blobUrl,
                               @Value("${blob.auth}") String blobAuth){
        AzureBlobAuthBuilder azureBlobAuthBuilder = new AzureBlobAuthBuilder(blobUrl, blobAuth);
        return new BlobStore(httpClient, objectMapper, azureBlobAuthBuilder);
    }

    @Singleton
    public FlowDefStore flowDefStore(HttpClient httpClient,
                                     ObjectMapper objectMapper,
                                     @Value("${app.rootDir}") File root,
                                     DataSource dataSource){
        FlowDefStore flowDefStore = new FlowDefStore(httpClient, objectMapper);
        flowDefStore.loadRoot(root, false);
        return flowDefStore;
    }
}
