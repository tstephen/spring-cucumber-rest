package com.knowprocess.cucumber;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ArrayType;

public class ResponseResults {
    private final ObjectMapper objectMapper;
    private final ClientHttpResponse theResponse;
    private final String body;

    protected ResponseResults(final ClientHttpResponse response, final ObjectMapper objectMapper)
            throws IOException {
        this.objectMapper = objectMapper;
        this.theResponse = response;
        final InputStream bodyInputStream = response.getBody();
        if (null == bodyInputStream) {
            this.body = "{}";
        } else {
            final StringWriter stringWriter = new StringWriter();
            IOUtils.copy(bodyInputStream, stringWriter);
            this.body = stringWriter.toString();
        }
    }

    protected ClientHttpResponse getTheResponse() {
        return theResponse;
    }

    public String getBody() {
        return body;
    }

    public ResponseResults statusCodeIs(HttpStatus expected)
            throws IOException {
        assertEquals(expected, theResponse.getStatusCode());
        return this;
    }

    public ResponseResults contentTypeIs(MediaType contentType) {
        assertEquals(contentType, theResponse.getHeaders().getContentType());
        return this;
    }

    public Object parseArray(Class<?> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayType type = objectMapper.getTypeFactory().constructArrayType(clazz);
        return objectMapper.readValue(getBody(), type);
    }
    
    public Object parseObject(Class<?> clazz) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(getBody(), clazz);
    }

}