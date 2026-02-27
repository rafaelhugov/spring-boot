package mppb.restapi.common.core.error.feign;

import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status >= 400 && status < 600) {
            String body = readBody(response);
            return new ClientApiException(status, methodKey+" # ApiHttpStatus: "+status + " # body: " + body, new String[]{});
        }

        // fallback to default decoder
        return defaultDecoder.decode(methodKey, response);
    }

    private String readBody(Response response) {
        String bodyString = "";

        if (response.body() != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))) {
                bodyString = reader.lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                bodyString = "[error reading response body: " + e.getMessage() + "]";
            }
        }
        return bodyString;
    }
}