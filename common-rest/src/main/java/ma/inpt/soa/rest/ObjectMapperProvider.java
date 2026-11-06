package ma.inpt.soa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private static final ObjectMapper MAPPER = RestServer.objectMapper();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return MAPPER;
    }
}
