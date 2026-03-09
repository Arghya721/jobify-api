package com.jobify.api.config;

import io.grpc.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GrpcAuthInterceptor implements ServerInterceptor {

    @Value("${grpc.api-key}")
    private String apiKey;

    private static final Metadata.Key<String> API_KEY_HEADER = Metadata.Key.of("x-api-key",
            Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(
            ServerCall<Req, Resp> call, Metadata headers, ServerCallHandler<Req, Resp> next) {

        String key = headers.get(API_KEY_HEADER);

        if (!apiKey.equals(key)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid API key"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }
}
