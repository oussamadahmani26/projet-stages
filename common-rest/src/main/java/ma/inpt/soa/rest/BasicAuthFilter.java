package ma.inpt.soa.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import ma.inpt.soa.common.config.AppConfig;

/** Authentification HTTP Basic des services REST. */
@Provider
public class BasicAuthFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requete) {
        if (!autorise(requete.getHeaderString(HttpHeaders.AUTHORIZATION))) {
            requete.abortWith(Response.status(401)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"services-soa\"")
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErreurApi("NON_AUTHENTIFIE", "Authentification requise."))
                    .build());
        }
    }

    private static boolean autorise(String entete) {
        if (entete == null || !entete.startsWith("Basic ")) {
            return false;
        }
        String attendu = AppConfig.utilisateurServices() + ":" + AppConfig.motDePasseServices();
        try {
            return MessageDigest.isEqual(Base64.getDecoder().decode(entete.substring(6)),
                    attendu.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
