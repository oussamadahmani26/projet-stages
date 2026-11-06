package ma.inpt.soa.stages;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Stage;
import ma.inpt.soa.common.util.Validation;

@Path("stages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StageResource {
    private static final String[] STATUTS = {"PLANIFIE", "EN_COURS", "TERMINE", "INTERROMPU"};
    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "PLANIFIE", List.of("EN_COURS", "INTERROMPU"),
            "EN_COURS", List.of("TERMINE", "INTERROMPU"),
            "TERMINE", List.of(),
            "INTERROMPU", List.of());
    private final StageDao dao = new StageDao();

    /** Enregistre le stage d'une candidature acceptée. idEtudiant et idOffre sont déduits de la candidature. */
    @POST
    public Response creer(Stage s) throws ServiceException {
        if (s == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        Validation.entier(s.getIdCandidature(), "idCandidature", 1, Integer.MAX_VALUE);
        verifierDates(s);
        int id = dao.creer(s);
        return Response.created(URI.create("stages/" + id)).entity(consulter(id)).build();
    }

    /** Sans filtre : tous les stages. Avec idEtudiant : l'historique de l'étudiant. */
    @GET
    public List<Stage> lister(@QueryParam("idEtudiant") Integer idEtudiant,
            @QueryParam("idCandidature") Integer idCandidature, @QueryParam("statut") String statut)
            throws ServiceException {
        return dao.lister(idEtudiant, idCandidature, statut);
    }

    @GET
    @Path("{id}")
    public Stage consulter(@PathParam("id") int id) throws ServiceException {
        Stage s = dao.trouver(id);
        if (s == null) {
            throw ServiceException.introuvable("Aucun stage avec l'identifiant " + id + ".");
        }
        return s;
    }

    /** Modifie le statut et/ou les dates. Corps : {"statut": "EN_COURS", "dateDebut": "...", "dateFin": "..."}. */
    @PATCH
    @Path("{id}")
    public Stage modifier(@PathParam("id") int id, Stage modifications) throws ServiceException {
        if (modifications == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        Stage actuel = consulter(id);
        if (modifications.getStatut() != null && !modifications.getStatut().equals(actuel.getStatut())) {
            Validation.parmi(modifications.getStatut(), "statut", STATUTS);
            if (!TRANSITIONS.get(actuel.getStatut()).contains(modifications.getStatut())) {
                throw ServiceException.conflit(
                        "Transition interdite : " + actuel.getStatut() + " vers " + modifications.getStatut() + ".");
            }
            actuel.setStatut(modifications.getStatut());
        }
        if (modifications.getDateDebut() != null) {
            actuel.setDateDebut(modifications.getDateDebut());
        }
        if (modifications.getDateFin() != null) {
            actuel.setDateFin(modifications.getDateFin());
        }
        verifierDates(actuel);
        dao.modifier(actuel);
        return consulter(id);
    }

    @POST
    @Path("{id}/comptes-rendus")
    public Response ajouterCompteRendu(@PathParam("id") int id, CompteRendu cr) throws ServiceException {
        if (cr == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        Stage stage = consulter(id);
        if (!"EN_COURS".equals(stage.getStatut())) {
            throw ServiceException.conflit("Un compte rendu ne peut être déposé que sur un stage en cours.");
        }
        cr.setIdStage(id);
        cr.setPeriode(Validation.texte(cr.getPeriode(), "periode", 60));
        cr.setContenu(Validation.texte(cr.getContenu(), "contenu", 10000));
        cr.setAuteur(Validation.texte(cr.getAuteur(), "auteur", 120));
        dao.ajouterCompteRendu(cr);
        return Response.status(Response.Status.CREATED).entity(cr).build();
    }

    @GET
    @Path("{id}/comptes-rendus")
    public List<CompteRendu> listerComptesRendus(@PathParam("id") int id) throws ServiceException {
        consulter(id);
        return dao.comptesRendus(id);
    }

    @POST
    @Path("{id}/evaluations")
    public Response ajouterEvaluation(@PathParam("id") int id, Evaluation e) throws ServiceException {
        if (e == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        Stage stage = consulter(id);
        if (!"EN_COURS".equals(stage.getStatut()) && !"TERMINE".equals(stage.getStatut())) {
            throw ServiceException.conflit("Une évaluation exige un stage en cours ou terminé.");
        }
        if (e.getNote() == null || e.getNote().compareTo(BigDecimal.ZERO) < 0
                || e.getNote().compareTo(BigDecimal.valueOf(20)) > 0) {
            throw ServiceException.validation("Le champ 'note' doit être compris entre 0 et 20.");
        }
        e.setIdStage(id);
        e.setEvaluateur(Validation.texte(e.getEvaluateur(), "evaluateur", 120));
        e.setCommentaire(Validation.texteOptionnel(e.getCommentaire(), "commentaire", 5000));
        dao.ajouterEvaluation(e);
        return Response.status(Response.Status.CREATED).entity(e).build();
    }

    @GET
    @Path("{id}/evaluations")
    public List<Evaluation> listerEvaluations(@PathParam("id") int id) throws ServiceException {
        consulter(id);
        return dao.evaluations(id);
    }

    private static void verifierDates(Stage s) throws ServiceException {
        Validation.date(s.getDateDebut(), "dateDebut");
        Validation.date(s.getDateFin(), "dateFin");
        if (!s.getDateFin().isAfter(s.getDateDebut())) {
            throw ServiceException.validation("La date de fin doit suivre la date de début.");
        }
    }
}
