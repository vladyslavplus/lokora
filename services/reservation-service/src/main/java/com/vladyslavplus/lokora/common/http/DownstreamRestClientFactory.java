package com.vladyslavplus.lokora.common.http;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

public final class DownstreamRestClientFactory {

    private DownstreamRestClientFactory() {
    }

    public static RestClient create(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
