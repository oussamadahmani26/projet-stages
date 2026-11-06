package ma.inpt.soa.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.ext.ContextResolver;
import java.io.IOException;
import ma.inpt.soa.common.config.HttpServers;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/** Démarre une application JAX-RS (Jersey) sur le serveur HTTP du JDK. */
public final class RestServer {
    private RestServer() {
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Configuration commune : JSON et traduction des erreurs. */
    public static ResourceConfig configuration(Class<?>... ressources) {
        ResourceConfig config = new ResourceConfig(ressources);
        config.register(JacksonFeature.withoutExceptionMappers());
        config.register(ObjectMapperProvider.class);
        config.register(ErreurMapper.class);
        return config;
    }

    /** Démarre un service protégé par authentification Basic. */
    public static HttpServer demarrer(int port, Class<?>... ressources) throws IOException {
        ResourceConfig config = configuration(ressources);
        config.register(BasicAuthFilter.class);
        HttpServer serveur = creer(port, config);
        serveur.start();
        return serveur;
    }

    /** Crée, sans le démarrer, un serveur exposant la configuration sous /api. */
    public static HttpServer creer(int port, ResourceConfig config) throws IOException {
        HttpServer serveur = HttpServers.creer(port);
        serveur.createContext("/api", ContainerFactory.createContainer(HttpHandler.class, config));
        return serveur;
    }
}
