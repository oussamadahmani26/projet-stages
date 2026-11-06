package ma.inpt.soa.processus.auth;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.rest.ErreurApi;

/** Exige une session pour toute l'API façade, hormis la connexion, l'inscription et la disponibilité des services. */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
    public static final String COOKIE = "SESSION_STAGES";
    static final String PROPRIETE = "session";

    @Override
    public void filter(ContainerRequestContext requete) {
        String chemin = requete.getUriInfo().getPath();
        if ("auth/connexion".equals(chemin) || "auth/inscription".equals(chemin) || "sante".equals(chemin)) {
            return;
        }
        Cookie cookie = requete.getCookies().get(COOKIE);
        Session session = cookie == null ? null : Sessions.trouver(cookie.getValue());
        if (session == null) {
            requete.abortWith(Response.status(401).type(MediaType.APPLICATION_JSON)
                    .entity(new ErreurApi(ServiceException.NON_AUTHENTIFIE, "Veuillez vous connecter.")).build());
            return;
        }
        requete.setProperty(PROPRIETE, session);
    }
}
