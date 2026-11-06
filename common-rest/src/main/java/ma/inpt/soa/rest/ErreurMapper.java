package ma.inpt.soa.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import ma.inpt.soa.common.exception.ServiceException;

/** Traduit les exceptions en codes HTTP. Aucune trace technique n'est renvoyée au client. */
@Provider
public class ErreurMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(ErreurMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof ServiceException) {
            ServiceException se = (ServiceException) e;
            return reponse(statut(se.getCode()), se.getCode(), se.getMessage());
        }
        if (e instanceof JsonProcessingException) {
            return reponse(400, ServiceException.VALIDATION, "Corps JSON invalide.");
        }
        if (e instanceof WebApplicationException) {
            Response r = ((WebApplicationException) e).getResponse();
            if (r.hasEntity()) {
                return r;
            }
            return reponse(r.getStatus(), "HTTP_" + r.getStatus(), r.getStatusInfo().getReasonPhrase());
        }
        LOG.log(Level.SEVERE, "Erreur inattendue", e);
        return reponse(500, "ERREUR_INTERNE", "Erreur interne du service.");
    }

    private static int statut(String code) {
        switch (code) {
            case ServiceException.VALIDATION: return 400;
            case ServiceException.INTROUVABLE: return 404;
            case ServiceException.CONFLIT: return 409;
            case ServiceException.NON_AUTHENTIFIE: return 401;
            case ServiceException.INTERDIT: return 403;
            case ServiceException.TROP_DE_TENTATIVES: return 429;
            default: return 503;
        }
    }

    private static Response reponse(int statut, String code, String message) {
        return Response.status(statut).type(MediaType.APPLICATION_JSON).entity(new ErreurApi(code, message)).build();
    }
}
