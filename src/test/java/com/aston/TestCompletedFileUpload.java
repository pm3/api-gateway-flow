package com.aston;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;

public class TestCompletedFileUpload implements CompletedFileUpload {

    private final String name;
    private final String fileName;
    private final MediaType contentType;
    private final byte[] data;

    public TestCompletedFileUpload(String name, String fileName, MediaType contentType, byte[] data) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }

    @Override
    public Optional<MediaType> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFilename() {
        return fileName;
    }

    @Override
    public long getSize() {
        return data.length;
    }

    @Override
    public long getDefinedSize() {
        return data.length;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(data);
    }
}
