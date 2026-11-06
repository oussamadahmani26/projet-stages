package ma.inpt.soa.processus.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.util.Validation;
import ma.inpt.soa.processus.Moteur;
import ma.inpt.soa.processus.auth.Acces;
import ma.inpt.soa.processus.auth.Session;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;

/** Pilotage du processus BPMN : dépôt, décisions de l'entreprise, incidents et suivi. */
@Path("processus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessusResource {
    private static final String GROUPE_RESPONSABLES = "responsables";
    private static final String TACHE_DECISION = "decisionEntreprise";
    private static final String TACHE_INCIDENT = "traiterIncident";

    private final ProcessEngine moteur = Moteur.moteur();

    @Context
    private ContainerRequestContext requete;

    /** Étape 1 : déclenche le processus. La réponse indique le résultat immédiat (rejet, attente ou incident). */
    @POST
    @Path("candidatures")
    public Response deposer(Map<String, Object> corps) throws ServiceException {
        if (corps == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        // Un étudiant dépose toujours en son propre nom.
        Session session = Acces.exigerRole(requete, Session.ETUDIANT, Session.RESPONSABLE);
        String matricule = session.estEtudiant() ? session.getMatricule()
                : Validation.texte(asTexte(corps.get("matricule")), "matricule", 30);
        Integer idOffre = asEntier(corps.get("idOffre"), "idOffre");
        Map<String, Object> variables = new HashMap<>();
        variables.put("matricule", matricule);
        variables.put("idOffre", idOffre);
        variables.put("lettreMotivation", Validation.texteOptionnel(asTexte(corps.get("lettreMotivation")),
                "lettreMotivation", 5000));
        String id = moteur.getRuntimeService().startProcessInstanceByKey(Moteur.CLE_PROCESSUS, variables).getId();
        return Response.status(Response.Status.CREATED).entity(etat(id)).build();
    }

    /** Dossiers en attente de décision, éventuellement pour une entreprise. */
    @GET
    @Path("taches")
    public List<Map<String, Object>> taches(@QueryParam("idEntreprise") Integer idEntreprise) throws ServiceException {
        Session session = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        Integer filtre = session.estEntreprise() ? session.getIdEntreprise() : idEntreprise;
        var recherche = moteur.getTaskService().createTaskQuery().taskDefinitionKey(TACHE_DECISION)
                .includeProcessVariables().orderByTaskCreateTime().asc();
        if (filtre != null) {
            recherche = recherche.taskCandidateGroup("entreprise-" + filtre);
        }
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Task t : recherche.list()) {
            Map<String, Object> v = t.getProcessVariables();
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("idTache", t.getId());
            ligne.put("idProcessus", t.getProcessInstanceId());
            ligne.put("creeeLe", t.getCreateTime());
            for (String cle : List.of("candidatureId", "matricule", "nomEtudiant", "filiere", "niveauEtudes",
                    "titreOffre", "lettreMotivation", "idEntreprise")) {
                ligne.put(cle, v.get(cle));
            }
            liste.add(ligne);
        }
        return liste;
    }

    /** Étape 7. Corps : {"decision": "ACCEPTEE|REFUSEE|COMPLEMENT", "motif": "...", "dateDebut": "AAAA-MM-JJ"}. */
    @POST
    @Path("taches/{idTache}/decision")
    public Map<String, Object> decider(@PathParam("idTache") String idTache, Map<String, Object> corps)
            throws ServiceException {
        Session session = Acces.exigerRole(requete, Session.ENTREPRISE, Session.RESPONSABLE);
        Task tache = tache(idTache, TACHE_DECISION);
        if (session.estEntreprise() && !session.getIdEntreprise().equals(
                moteur.getRuntimeService().getVariable(tache.getProcessInstanceId(), "idEntreprise"))) {
            throw Acces.interdit();
        }
        if (corps == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        String decision = Validation.parmi(asTexte(corps.get("decision")), "decision",
                "ACCEPTEE", "REFUSEE", "COMPLEMENT");
        Map<String, Object> variables = new HashMap<>();
        variables.put("decision", decision);
        variables.put("motifDecision", Validation.texteOptionnel(asTexte(corps.get("motif")), "motif", 500));
        String dateDebut = asTexte(corps.get("dateDebut"));
        if (dateDebut != null && !dateDebut.isBlank()) {
            try {
                LocalDate.parse(dateDebut);
            } catch (DateTimeParseException e) {
                throw ServiceException.validation("Le champ 'dateDebut' doit respecter le format AAAA-MM-JJ.");
            }
            variables.put("dateDebutStage", dateDebut);
        }
        String idProcessus = tache.getProcessInstanceId();
        moteur.getTaskService().complete(tache.getId(), variables);
        return etat(idProcessus);
    }

    /** Incidents ouverts : un service était indisponible ou a répondu par une erreur. */
    @GET
    @Path("incidents")
    public List<Map<String, Object>> incidents() throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Task t : moteur.getTaskService().createTaskQuery().taskCandidateGroup(GROUPE_RESPONSABLES)
                .includeProcessVariables().orderByTaskCreateTime().asc().list()) {
            Map<String, Object> v = t.getProcessVariables();
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("idTache", t.getId());
            ligne.put("idProcessus", t.getProcessInstanceId());
            ligne.put("creeeLe", t.getCreateTime());
            ligne.put("etape", v.get("etapeEnErreur"));
            ligne.put("message", v.get("erreurMessage"));
            ligne.put("matricule", v.get("matricule"));
            ligne.put("titreOffre", v.get("titreOffre"));
            ligne.put("candidatureId", v.get("candidatureId"));
            liste.add(ligne);
        }
        return liste;
    }

    /** Corps : {"action": "RELANCER|ABANDONNER"}. */
    @POST
    @Path("incidents/{idTache}/resolution")
    public Map<String, Object> resoudre(@PathParam("idTache") String idTache, Map<String, Object> corps)
            throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        Task tache = tache(idTache, TACHE_INCIDENT);
        String action = Validation.parmi(corps == null ? null : asTexte(corps.get("action")), "action",
                "RELANCER", "ABANDONNER");
        String idProcessus = tache.getProcessInstanceId();
        moteur.getTaskService().complete(tache.getId(), Map.of("action", action));
        return etat(idProcessus);
    }

    /** Liste des processus, du plus récent au plus ancien. */
    @GET
    public List<Map<String, Object>> lister(@QueryParam("matricule") String matricule) throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        var recherche = moteur.getHistoryService().createHistoricProcessInstanceQuery()
                .processDefinitionKey(Moteur.CLE_PROCESSUS).orderByProcessInstanceStartTime().desc();
        if (matricule != null && !matricule.isBlank()) {
            recherche = recherche.variableValueEquals("matricule", matricule);
        }
        List<Map<String, Object>> liste = new ArrayList<>();
        for (HistoricProcessInstance p : recherche.listPage(0, 200)) {
            liste.add(etat(p.getId()));
        }
        return liste;
    }

    /** État détaillé : chaque activité exécutée, dans l'ordre. */
    @GET
    @Path("{id}")
    public Map<String, Object> detail(@PathParam("id") String id) throws ServiceException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        exiger(id);
        Map<String, Object> resultat = etat(id);
        List<Map<String, Object>> activites = new ArrayList<>();
        for (HistoricActivityInstance a : activites(id)) {
            if (a.getActivityName() == null || !parcourue(a)) {
                continue;
            }
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("id", a.getActivityId());
            ligne.put("nom", a.getActivityName());
            ligne.put("type", a.getActivityType());
            ligne.put("debut", a.getStartTime());
            ligne.put("fin", a.getEndTime());
            activites.add(ligne);
        }
        resultat.put("activites", activites);
        return resultat;
    }

    /** Diagramme PNG du processus ; les activités déjà parcourues sont surlignées. */
    @GET
    @Path("{id}/diagramme")
    @Produces("image/png")
    public Response diagramme(@PathParam("id") String id, @QueryParam("surligner") @DefaultValue("true") boolean surligner)
            throws ServiceException, IOException {
        Acces.exigerRole(requete, Session.RESPONSABLE);
        HistoricProcessInstance instance = exiger(id);
        BpmnModel modele = moteur.getRepositoryService().getBpmnModel(instance.getProcessDefinitionId());
        List<String> actives = new ArrayList<>();
        for (HistoricActivityInstance a : activites(id)) {
            if (surligner && parcourue(a)) {
                actives.add(a.getActivityId());
            }
        }
        try (InputStream image = moteur.getProcessEngineConfiguration().getProcessDiagramGenerator()
                .generateDiagram(modele, "png", actives, List.of(), "Arial", "Arial", "Arial", null, 1.0, true)) {
            return Response.ok(image.readAllBytes()).build();
        }
    }

    /** Le moteur journalise les événements de bord dès l'entrée dans l'étape, même si rien ne les déclenche. */
    private static boolean parcourue(HistoricActivityInstance a) {
        return !"sequenceFlow".equals(a.getActivityType()) && !"boundaryEvent".equals(a.getActivityType());
    }

    private List<HistoricActivityInstance> activites(String id) {
        return moteur.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(id)
                .orderByHistoricActivityInstanceStartTime().asc().list();
    }

    private HistoricProcessInstance exiger(String id) throws ServiceException {
        HistoricProcessInstance instance = moteur.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(id).singleResult();
        if (instance == null) {
            throw ServiceException.introuvable("Processus inconnu : " + id + ".");
        }
        return instance;
    }

    private Task tache(String id, String attendue) throws ServiceException {
        Task tache = moteur.getTaskService().createTaskQuery().taskId(id).singleResult();
        if (tache == null || !attendue.equals(tache.getTaskDefinitionKey())) {
            throw ServiceException.introuvable("Cette tâche n'existe pas ou a déjà été traitée.");
        }
        return tache;
    }

    /** Résume un processus à partir de l'historique du moteur. */
    private Map<String, Object> etat(String id) {
        HistoryService historique = moteur.getHistoryService();
        HistoricProcessInstance instance = historique.createHistoricProcessInstanceQuery()
                .processInstanceId(id).singleResult();
        Map<String, Object> v = new HashMap<>();
        for (HistoricVariableInstance h : historique.createHistoricVariableInstanceQuery()
                .processInstanceId(id).list()) {
            v.put(h.getVariableName(), h.getValue());
        }
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("idProcessus", id);
        resultat.put("etat", nomEtat(id, instance));
        resultat.put("debut", instance.getStartTime());
        resultat.put("fin", instance.getEndTime());
        for (String cle : List.of("matricule", "nomEtudiant", "idOffre", "titreOffre", "nomEntreprise",
                "candidatureId", "stageId", "motifsRejet", "motifDecision", "erreurMessage")) {
            resultat.put(cle, v.get(cle));
        }
        return resultat;
    }

    private String nomEtat(String id, HistoricProcessInstance instance) {
        if (instance.getEndTime() == null) {
            List<Task> taches = moteur.getTaskService().createTaskQuery().processInstanceId(id).list();
            boolean incident = taches.stream().anyMatch(t -> TACHE_INCIDENT.equals(t.getTaskDefinitionKey()));
            return incident ? "INCIDENT" : "EN_ATTENTE_DECISION";
        }
        for (HistoricActivityInstance a : activites(id)) {
            if (a.getActivityId().startsWith("fin_")) {
                return a.getActivityId().substring(4).toUpperCase();
            }
        }
        return "TERMINE";
    }

    private static String asTexte(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer asEntier(Object o, String champ) throws ServiceException {
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            throw ServiceException.validation("Le champ '" + champ + "' doit être un nombre entier.");
        }
    }
}
