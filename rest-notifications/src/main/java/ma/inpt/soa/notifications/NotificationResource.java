package ma.inpt.soa.notifications;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.util.Validation;

@Path("notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {
    private static final String[] TYPES = {"CONFIRMATION_DEPOT", "RECEPTION_ENTREPRISE", "CANDIDATURE_ACCEPTEE",
        "CANDIDATURE_REFUSEE", "CANDIDATURE_REJETEE", "DEMANDE_COMPLEMENT", "CANDIDATURE_ANNULEE",
        "RAPPEL_DATE_LIMITE"};
    private final NotificationDao dao = new NotificationDao();

    /** Enregistre et émet une notification (l'émission est simulée, voir NotificationDao). */
    @POST
    public Response envoyer(Notification n) throws ServiceException {
        if (n == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        n.setType(Validation.parmi(n.getType(), "type", TYPES));
        n.setDestinataireType(Validation.parmi(n.getDestinataireType(), "destinataireType", "ETUDIANT", "ENTREPRISE"));
        n.setDestinataireEmail(Validation.email(n.getDestinataireEmail(), "destinataireEmail"));
        n.setSujet(Validation.texte(n.getSujet(), "sujet", 200));
        n.setMessage(Validation.texte(n.getMessage(), "message", 5000));
        int id = dao.inserer(n);
        return Response.created(URI.create("notifications/" + id)).entity(consulter(id)).build();
    }

    @GET
    public List<Notification> lister(@QueryParam("destinataireType") String destinataireType,
            @QueryParam("destinataireId") Integer destinataireId,
            @QueryParam("idCandidature") Integer idCandidature) throws ServiceException {
        return dao.lister(destinataireType, destinataireId, idCandidature);
    }

    @GET
    @Path("{id}")
    public Notification consulter(@PathParam("id") int id) throws ServiceException {
        Notification n = dao.trouver(id);
        if (n == null) {
            throw ServiceException.introuvable("Aucune notification avec l'identifiant " + id + ".");
        }
        return n;
    }

    /**
     * Rappelle les dates limites des offres actives qui expirent dans les prochains jours :
     * à l'entreprise et aux étudiants actifs n'ayant pas encore candidaté. Un rappel par jour au maximum.
     */
    @POST
    @Path("rappels")
    public Map<String, Integer> envoyerRappels(@QueryParam("joursAvant") @DefaultValue("7") int joursAvant)
            throws ServiceException {
        Validation.entier(joursAvant, "joursAvant", 0, 365);
        return Map.of("rappelsEnvoyes", dao.envoyerRappels(joursAvant));
    }
}
