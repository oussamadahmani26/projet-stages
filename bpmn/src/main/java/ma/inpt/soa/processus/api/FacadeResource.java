package ma.inpt.soa.processus.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.Moteur;
import ma.inpt.soa.processus.auth.Acces;
import ma.inpt.soa.processus.auth.Session;
import ma.inpt.soa.processus.clients.RestClient;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.Execution;

/**
 * API façade de l'interface graphique. Le navigateur ne parle ni SOAP ni ne détient les identifiants des services :
 * cette couche appelle les six services pour lui, expose tout en JSON et limite chaque compte à ses propres données.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacadeResource {
    private final RestClient rest = Services.rest();
    private final ProcessEngine moteur = Moteur.moteur();

    @Context
    private ContainerRequestContext requete;

    // ---------- Étudiants (SOAP)

    @GET
    @Path("etudiants")
    public List<Map<String, String>> etudiants() throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        return Services.etudiants().appeler("listerEtudiants", Map.of());
    }

    @GET
    @Path("etudiants/{matricule}")
    public Map<String, String> etudiant(@PathParam("matricule") String matricule) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ETUDIANT, Session.RESPONSABLE);
        if (s.estEtudiant() && !matricule.equals(s.getMatricule())) {
            throw Acces.interdit();
        }
        return Services.etudiants().appelerUn("consulterEtudiant", Map.of("matricule", matricule));
    }

    // ---------- Entreprises (SOAP)

    @GET
    @Path("entreprises")
    public List<Map<String, String>> entreprises() throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        return Services.entreprises().appeler("listerEntreprises", Map.of());
    }

    @GET
    @Path("entreprises/{id}/offres")
    public List<Map<String, String>> offresEntreprise(@PathParam("id") int id) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        exigerEntreprise(s, id);
        return Services.entreprises().appeler("consulterOffresEntreprise", Map.of("idEntreprise", id));
    }

    // ---------- Offres (SOAP)

    @GET
    @Path("offres")
    public List<Map<String, String>> offres() throws ServiceException {
        return Services.offres().appeler("listerOffres", Map.of());
    }

    @POST
    @Path("offres")
    public Response creerOffre(Map<String, Object> offre) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        Map<String, Object> complete = new LinkedHashMap<>(offre);
        if (s.estEntreprise()) {
            complete.put("idEntreprise", s.getIdEntreprise());
        }
        Map<String, String> creee = Services.offres().appelerUn("creerOffre", Map.of("offre", complete));
        return Response.status(Response.Status.CREATED).entity(creee).build();
    }

    @PUT
    @Path("offres/{id}")
    public Map<String, String> modifierOffre(@PathParam("id") int id, Map<String, Object> offre)
            throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        exigerOffre(s, id);
        Map<String, Object> complete = new LinkedHashMap<>(offre);
        complete.put("idOffre", id);
        if (s.estEntreprise()) {
            complete.put("idEntreprise", s.getIdEntreprise());
        }
        return Services.offres().appelerUn("modifierOffre", Map.of("offre", complete));
    }

    @DELETE
    @Path("offres/{id}")
    public Response supprimerOffre(@PathParam("id") int id) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        exigerOffre(s, id);
        Services.offres().appeler("supprimerOffre", Map.of("idOffre", id));
        return Response.noContent().build();
    }

    // ---------- Candidatures (REST)

    /** Un étudiant ne voit que ses candidatures, une entreprise que celles reçues : le filtre vient de la session. */
    @GET
    @Path("candidatures")
    public JsonNode candidatures(@QueryParam("idEtudiant") Integer idEtudiant,
            @QueryParam("idEntreprise") Integer idEntreprise) throws ServiceException {
        Session s = Acces.session(requete);
        Integer etudiant = s.estEtudiant() ? s.getIdEtudiant() : s.estEntreprise() ? null : idEtudiant;
        Integer entreprise = s.estEntreprise() ? s.getIdEntreprise() : s.estEtudiant() ? null : idEntreprise;
        return rest.get(Services.candidatures() + filtres(Map.of(
                "idEtudiant", etudiant == null ? "" : etudiant.toString(),
                "idEntreprise", entreprise == null ? "" : entreprise.toString())));
    }

    /**
     * Annule la candidature et signale l'annulation au processus qui attend la décision de l'entreprise.
     * Refusée pendant un incident : le responsable doit d'abord reprendre ou abandonner le traitement.
     */
    @DELETE
    @Path("candidatures/{id}")
    public Response annuler(@PathParam("id") int id) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ETUDIANT, Session.RESPONSABLE);
        if (s.estEtudiant()) {
            JsonNode candidature = rest.get(Services.candidatures() + "/" + id);
            if (candidature.path("idEtudiant").asInt() != s.getIdEtudiant()) {
                throw Acces.interdit();
            }
        }
        Execution enAttente = moteur.getRuntimeService().createExecutionQuery()
                .messageEventSubscriptionName(Moteur.MESSAGE_ANNULATION)
                .processVariableValueEquals("candidatureId", id).singleResult();
        if (enAttente == null && moteur.getRuntimeService().createProcessInstanceQuery()
                .variableValueEquals("candidatureId", id).count() > 0) {
            throw ServiceException.conflit("Un incident est en cours sur cette candidature : contactez le responsable.");
        }
        rest.envoyer("DELETE", Services.candidatures() + "/" + id, null);
        if (enAttente != null) {
            moteur.getRuntimeService().messageEventReceived(Moteur.MESSAGE_ANNULATION, enAttente.getId());
        }
        return Response.noContent().build();
    }

    // ---------- Notifications (REST)

    @GET
    @Path("notifications")
    public JsonNode notifications(@QueryParam("destinataireType") String type,
            @QueryParam("destinataireId") Integer id) throws ServiceException {
        Session s = Acces.session(requete);
        String typeFiltre = s.estEtudiant() ? "ETUDIANT" : s.estEntreprise() ? "ENTREPRISE" : type;
        Integer idFiltre = s.estEtudiant() ? s.getIdEtudiant() : s.estEntreprise() ? s.getIdEntreprise() : id;
        return rest.get(Services.notifications() + filtres(Map.of(
                "destinataireType", typeFiltre == null ? "" : typeFiltre,
                "destinataireId", idFiltre == null ? "" : idFiltre.toString())));
    }

    @POST
    @Path("notifications/rappels")
    public JsonNode rappels(@QueryParam("joursAvant") Integer joursAvant) throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        return rest.envoyer("POST", Services.notifications() + "/rappels?joursAvant="
                + (joursAvant == null ? 7 : joursAvant), null);
    }

    // ---------- Stages (REST)

    @GET
    @Path("stages")
    public JsonNode stages(@QueryParam("idEtudiant") Integer idEtudiant) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ETUDIANT, Session.RESPONSABLE);
        Integer filtre = s.estEtudiant() ? s.getIdEtudiant() : idEtudiant;
        return rest.get(Services.stages() + filtres(Map.of("idEtudiant", filtre == null ? "" : filtre.toString())));
    }

    /** À la fin du stage, l'étudiant redevient disponible pour une nouvelle candidature. */
    @PATCH
    @Path("stages/{id}")
    public JsonNode modifierStage(@PathParam("id") int id, Map<String, Object> corps) throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        JsonNode stage = rest.envoyer("PATCH", Services.stages() + "/" + id, corps);
        String statut = stage.path("statut").asText();
        if ("TERMINE".equals(statut) || "INTERROMPU".equals(statut)) {
            Services.etudiants().appeler("changerStatutEtudiant",
                    Map.of("matricule", stage.path("matricule").asText(), "statut", "ACTIF"));
        }
        return stage;
    }

    @GET
    @Path("stages/{id}/comptes-rendus")
    public JsonNode comptesRendus(@PathParam("id") int id) throws ServiceException {
        exigerStage(id);
        return rest.get(Services.stages() + "/" + id + "/comptes-rendus");
    }

    @POST
    @Path("stages/{id}/comptes-rendus")
    public Response ajouterCompteRendu(@PathParam("id") int id, Map<String, Object> corps) throws ServiceException {
        exigerStage(id);
        JsonNode cr = rest.envoyer("POST", Services.stages() + "/" + id + "/comptes-rendus", corps);
        return Response.status(Response.Status.CREATED).entity(cr).build();
    }

    @GET
    @Path("stages/{id}/evaluations")
    public JsonNode evaluations(@PathParam("id") int id) throws ServiceException {
        exigerStage(id);
        return rest.get(Services.stages() + "/" + id + "/evaluations");
    }

    @POST
    @Path("stages/{id}/evaluations")
    public Response ajouterEvaluation(@PathParam("id") int id, Map<String, Object> corps) throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        JsonNode e = rest.envoyer("POST", Services.stages() + "/" + id + "/evaluations", corps);
        return Response.status(Response.Status.CREATED).entity(e).build();
    }

    // ---------- Supervision

    /** Disponibilité de chaque service. Publique : elle sert aussi au script de démarrage et ne révèle aucune donnée. */
    @GET
    @Path("sante")
    public Map<String, Boolean> sante() {
        Map<String, Boolean> etats = new LinkedHashMap<>();
        etats.put("SOAP Étudiants", disponible(() -> Services.etudiants().appeler("listerEtudiants", Map.of())));
        etats.put("SOAP Entreprises", disponible(() -> Services.entreprises().appeler("listerEntreprises", Map.of())));
        etats.put("SOAP Offres", disponible(() -> Services.offres().appeler("listerOffres", Map.of())));
        etats.put("REST Candidatures", disponible(() -> rest.get(Services.candidatures() + "/0")));
        etats.put("REST Notifications", disponible(() -> rest.get(Services.notifications() + "/0")));
        etats.put("REST Stages", disponible(() -> rest.get(Services.stages() + "/0")));
        return etats;
    }

    private interface Sonde {
        void executer() throws ServiceException;
    }

    /** Une erreur métier (404) prouve que le service répond ; seule l'indisponibilité compte comme panne. */
    private static boolean disponible(Sonde sonde) {
        try {
            sonde.executer();
            return true;
        } catch (ServiceException e) {
            return !ServiceException.INDISPONIBLE.equals(e.getCode());
        }
    }

    // ---------- Droits

    private static void exigerEntreprise(Session s, int idEntreprise) throws ServiceException {
        if (s.estEntreprise() && s.getIdEntreprise() != idEntreprise) {
            throw Acces.interdit();
        }
    }

    private static void exigerOffre(Session s, int idOffre) throws ServiceException {
        if (s.estEntreprise()) {
            Map<String, String> offre = Services.offres().appelerUn("consulterOffre", Map.of("idOffre", idOffre));
            exigerEntreprise(s, Integer.parseInt(offre.get("idEntreprise")));
        }
    }

    /** Le stage est accessible à son étudiant et au responsable. */
    private void exigerStage(int idStage) throws ServiceException {
        Session s = Acces.exigerRole(requete, Session.ETUDIANT, Session.RESPONSABLE);
        if (s.estEtudiant()) {
            JsonNode stage = rest.get(Services.stages() + "/" + idStage);
            if (stage.path("idEtudiant").asInt() != s.getIdEtudiant()) {
                throw Acces.interdit();
            }
        }
    }

    private static String filtres(Map<String, String> valeurs) {
        StringBuilder texte = new StringBuilder();
        for (Map.Entry<String, String> e : valeurs.entrySet()) {
            if (!e.getValue().isEmpty()) {
                texte.append(texte.length() == 0 ? '?' : '&').append(e.getKey()).append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return texte.toString();
    }
}
