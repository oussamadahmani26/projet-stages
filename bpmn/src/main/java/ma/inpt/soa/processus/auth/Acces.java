package ma.inpt.soa.processus.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Arrays;
import ma.inpt.soa.common.exception.ServiceException;

/** Contrôles de droits des ressources de l'API façade. */
public final class Acces {
    private Acces() {
    }

    public static Session session(ContainerRequestContext requete) {
        return (Session) requete.getProperty(AuthFilter.PROPRIETE);
    }

    public static Session exigerRole(ContainerRequestContext requete, String... roles) throws ServiceException {
        Session s = session(requete);
        if (!Arrays.asList(roles).contains(s.getRole())) {
            throw interdit();
        }
        return s;
    }

    public static ServiceException interdit() {
        return new ServiceException(ServiceException.INTERDIT, "Accès non autorisé pour ce compte.");
    }
}
