package ma.inpt.soa.candidatures;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import ma.inpt.soa.common.model.Candidature;
import ma.inpt.soa.common.util.Validation;

@Path("candidatures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidatureResource {
    private final CandidatureDao dao = new CandidatureDao();

    /** Dépose une candidature. Une candidature annulée au préalable peut être redéposée. */
    @POST
    public Response deposer(Candidature c) throws ServiceException {
        if (c == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        Validation.entier(c.getIdEtudiant(), "idEtudiant", 1, Integer.MAX_VALUE);
        Validation.entier(c.getIdOffre(), "idOffre", 1, Integer.MAX_VALUE);
        c.setLettreMotivation(Validation.texteOptionnel(c.getLettreMotivation(), "lettreMotivation", 5000));
        int id = dao.deposer(c);
        return Response.created(URI.create("candidatures/" + id)).entity(consulter(id)).build();
    }

    @GET
    public List<Candidature> lister(@QueryParam("idEtudiant") Integer idEtudiant,
            @QueryParam("idOffre") Integer idOffre, @QueryParam("idEntreprise") Integer idEntreprise,
            @QueryParam("statut") String statut) throws ServiceException {
        return dao.lister(idEtudiant, idOffre, idEntreprise, statut);
    }

    @GET
    @Path("{id}")
    public Candidature consulter(@PathParam("id") int id) throws ServiceException {
        Candidature c = dao.trouver(id);
        if (c == null) {
            throw ServiceException.introuvable("Aucune candidature avec l'identifiant " + id + ".");
        }
        return c;
    }

    /** Corps : {"statut": "ACCEPTEE", "motif": "..."}. Les transitions autorisées sont dans CycleDeVie. */
    @PATCH
    @Path("{id}/statut")
    public Candidature changerStatut(@PathParam("id") int id, Map<String, String> corps) throws ServiceException {
        if (corps == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        String nouveau = Validation.parmi(corps.get("statut"), "statut", CycleDeVie.STATUTS);
        String motif = Validation.texteOptionnel(corps.get("motif"), "motif", 500);
        Candidature actuelle = consulter(id);
        if (actuelle.getStatut().equals(nouveau)) {
            return actuelle;
        }
        CycleDeVie.verifierTransition(actuelle.getStatut(), nouveau);
        if (!dao.changerStatut(id, actuelle.getStatut(), nouveau, motif)) {
            throw ServiceException.conflit("La candidature vient d'être modifiée : réessayez.");
        }
        return consulter(id);
    }

    /** Annule la candidature, tant qu'aucune décision finale n'a été rendue. Idempotent. */
    @DELETE
    @Path("{id}")
    public Response annuler(@PathParam("id") int id) throws ServiceException {
        Candidature actuelle = consulter(id);
        if ("ANNULEE".equals(actuelle.getStatut())) {
            return Response.noContent().build();
        }
        if ("ACCEPTEE".equals(actuelle.getStatut()) || "REFUSEE".equals(actuelle.getStatut())) {
            throw ServiceException.conflit("Annulation impossible : la décision finale a déjà été rendue.");
        }
        if (!dao.changerStatut(id, actuelle.getStatut(), "ANNULEE", "Annulée par l'étudiant.")) {
            throw ServiceException.conflit("La candidature vient d'être modifiée : réessayez.");
        }
        return Response.noContent().build();
    }
}
