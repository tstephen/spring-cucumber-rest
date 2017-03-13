package com.knowprocess.cucumber;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cucumber.runtime.CucumberException;

public class IntegrationTestSupport {

    protected String usr;
    protected String pwd;
    protected String baseUrl;
    protected String tenantId;
    
    protected ObjectMapper mapper = new ObjectMapper();
    
    protected static ResponseResults latestResponse = null;

    @Autowired
    protected RestTemplate restTemplate = new RestTemplate();
    
    protected ResponseEntity<?> latestEntity;
    protected Object latestObject;
    private Properties properties;
    protected long latestTiming;
    
    public IntegrationTestSupport() {
        properties = new Properties();
        InputStream is = null ;
        try {
            is = getClass().getResourceAsStream("/cucumber.properties");
            properties.load(is);
            usr = properties.getProperty("kp.app.username");
            pwd = properties.getProperty("kp.app.password");
            baseUrl = properties.getProperty("kp.app.baseUrl", "http://localhost:8080");
            tenantId = properties.getProperty("kp.app.tenantId");
        } catch (Exception e) {
            throw new CucumberException("Need cucumber.properties on classpath to define server connection(s)");
        } finally { 
            try {
                is.close();
            } catch (Exception e) {
                ;
            }
        }
    }

    protected ResponseResults executeGet(String url) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(usr, pwd));
        
        long start = System.currentTimeMillis();
        latestResponse = restTemplate.execute(url, HttpMethod.GET, requestCallback, new ResponseExtractor<ResponseResults>() {
            @Override
            public ResponseResults extractData(ClientHttpResponse response) throws IOException {
                if (errorHandler.hadError) {
                    return (errorHandler.getResults());
                } else {
                    return (new ResponseResults(response, mapper));
                }
            }
        });
        latestTiming = System.currentTimeMillis()-start;
        return latestResponse;
    }
    
    protected void getForEntity(String url, Class<?> responseType) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
//        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(usr, pwd));
        latestEntity = restTemplate.getForEntity(url, responseType);
    }
    
    protected void getForObject(String url, Class<?> responseType) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
//        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(usr, pwd));
        latestObject = restTemplate.getForObject(url, responseType);
    }

    private String getRemoteUrl(String url) {
        if (!url.startsWith("http")) {
            url = baseUrl + url;
        }
        return url;
    }
    
    protected void executePost(String url) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(usr, pwd));
        latestResponse = restTemplate.execute(url, HttpMethod.POST, requestCallback, new ResponseExtractor<ResponseResults>() {
            @Override
            public ResponseResults extractData(ClientHttpResponse response) throws IOException {
                if (errorHandler.hadError) {
                    return (errorHandler.getResults());
                } else {
                    return (new ResponseResults(response, mapper));
                }
            }
        });

    }

    protected ResponseResults executePut(String url, Object updatedBean) throws JsonProcessingException {
        url = getRemoteUrl(url);
        
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        requestCallback.setBody(mapper.writeValueAsString(updatedBean));
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(usr, pwd));
        latestResponse = restTemplate.execute(url, HttpMethod.PUT, requestCallback, new ResponseExtractor<ResponseResults>() {
            @Override
            public ResponseResults extractData(ClientHttpResponse response) throws IOException {
                if (errorHandler.hadError) {
                    return (errorHandler.getResults());
                } else {
                    return (new ResponseResults(response, mapper));
                }
            }
        });
        return latestResponse;
    }

    private class ResponseResultErrorHandler implements ResponseErrorHandler {
        private ResponseResults results = null;
        private Boolean hadError = false;

        private ResponseResults getResults() {
            return results;
        }

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            hadError = response.getRawStatusCode() >= 400;
            return hadError;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            results = new ResponseResults(response, mapper);
        }
    }
}
