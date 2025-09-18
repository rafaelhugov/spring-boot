package mppb.microservices.common.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseWebMvcTest {

    @Autowired
    protected ObjectMapper objectMapper;

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar JSON", e);
        }
    }
}
