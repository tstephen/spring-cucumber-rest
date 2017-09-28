package com.knowprocess.cucumber;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.knowprocess.cucumber.model.JwtResponse;

import cucumber.runtime.CucumberException;

public class IntegrationTestSupport {
    protected static final Logger LOGGER = LoggerFactory
            .getLogger(IntegrationTestSupport.class);

    protected static Properties properties;

    protected static String usr;
    protected static String pwd;
    protected static String authMethod;
    protected static String jwtToken;
    protected static String baseUrl;
    protected static String tenantId;

    protected ObjectMapper mapper = new ObjectMapper();

    protected static ResponseResults latestResponse = null;

    @Autowired
    protected RestTemplate restTemplate = new RestTemplate();

    protected ResponseEntity<?> latestEntity;
    protected Object latestObject;

    protected long latestTiming;

    static {
        properties = new Properties();
        InputStream is = null ;
        try {
            is = IntegrationTestSupport.class.getResourceAsStream("/cucumber.properties");
            properties.load(is);
            usr = properties.getProperty("kp.app.username");
            pwd = properties.getProperty("kp.app.password");
            authMethod = properties.getProperty("kp.app.authMethod", "BASIC");
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

    protected ResponseResults login() throws IOException {
        latestResponse = executePost("/auth/login",
                String.format("{\"username\":\"%1$s\",\"password\":\"%2$s\"}", usr, pwd), true);
        latestObject = mapper.readValue(latestResponse.getBody(), JwtResponse.class);
        jwtToken = ((JwtResponse) latestObject).getToken();
        return latestResponse;
    }

    protected ResponseResults executeDelete(String url) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        applyAuthMethod(headers, false);

        long start = System.currentTimeMillis();
        latestResponse = restTemplate.execute(url, HttpMethod.DELETE, requestCallback, new ResponseExtractor<ResponseResults>() {
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

    protected ResponseResults executeGet(String url) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        applyAuthMethod(headers, false);

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

    private void applyAuthMethod(final Map<String, String> headers, final boolean isLogin) {
        switch (authMethod.toUpperCase()) {
        case "BASIC":
            restTemplate.getInterceptors().add(
                    new BasicAuthorizationInterceptor(usr, pwd));
            break;
        case "JWT":
            headers.put("X-Requested-With", "XMLHttpRequest");
            if (isLogin) {
                LOGGER.info("Performing login");
            } else if (jwtToken == null) {
                throw new IllegalStateException("Attempt to use auth method JWT without token. Did you call login?");
            } else {
                headers.put("X-Authorization", "Bearer " + jwtToken);
            }
            break;
        default:
            LOGGER.warn("unknown authentication method %1$s", authMethod);
        }
    }

    protected void getForEntity(String url, Class<?> responseType) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        applyAuthMethod(headers, false);

        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();
        restTemplate.setErrorHandler(errorHandler);

        latestEntity = restTemplate.getForEntity(url, responseType);
    }

    protected void getForObject(String url, Class<?> responseType) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        applyAuthMethod(headers, false);

        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();
        restTemplate.setErrorHandler(errorHandler);

        latestObject = restTemplate.getForObject(url, responseType);
    }

    private String getRemoteUrl(String url) {
        if (!url.startsWith("http")) {
            url = baseUrl + url;
        }
        return url;
    }

    protected ResponseResults executePost(String url, Object bean) throws IOException {
        return executePost(url, mapper.writeValueAsString(bean), false);
    }

    protected ResponseResults executePost(String url, Object bean, boolean isLogin) throws IOException {
        return executePost(url, mapper.writeValueAsString(bean), isLogin);
    }

    protected ResponseResults executePost(String url, String json, boolean isLogin) throws IOException {
        url = getRemoteUrl(url);
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        requestCallback.setBody(json);
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        applyAuthMethod(headers, isLogin);
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
        return latestResponse;
    }

    protected ResponseResults executePut(String url, Object updatedBean) throws JsonProcessingException {
        url = getRemoteUrl(url);

        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        final HeaderSettingRequestCallback requestCallback = new HeaderSettingRequestCallback(headers);
        requestCallback.setBody(mapper.writeValueAsString(updatedBean));
        final ResponseResultErrorHandler errorHandler = new ResponseResultErrorHandler();

        restTemplate.setErrorHandler(errorHandler);
        applyAuthMethod(headers, false);
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
