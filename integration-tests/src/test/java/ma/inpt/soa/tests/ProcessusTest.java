package ma.inpt.soa.tests;

import static ma.inpt.soa.tests.Plateforme.api;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import ma.inpt.soa.processus.Moteur;
import ma.inpt.soa.tests.Plateforme.Reponse;
import ma.inpt.soa.tests.Plateforme.Service;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Scénarios du processus BPMN, de bout en bout, à travers l'API façade. */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ProcessusTest {
    private static final AtomicInteger NUMERO = new AtomicInteger();

    @BeforeAll
    static void demarrer() throws Exception {
        Assumptions.assumeTrue(Plateforme.baseAccessible(), "MySQL/MariaDB inaccessible : tests ignorés.");
        Plateforme.initialiserBase();
        Plateforme.demarrer();
    }

    @AfterAll
    static void arreter() {
        Plateforme.arreterTout();
        Moteur.arreter();
    }

    // ---------- Jeux de données propres à chaque scénario

    private static String nouvelEtudiant() throws Exception {
        int n = NUMERO.incrementAndGet();
        String matricule = "P-" + n;
        Reponse r = Plateforme.soap(Service.ETUDIANTS, "/ws/etudiants", "http://soa.inpt.ma/etudiants",
                "<n:ajouterEtudiant><etudiant><matricule>" + matricule + "</matricule><nom>Etudiant " + n
                        + "</nom><filiere>Innovation et AMOA</filiere><niveauEtudes>INE 2</niveauEtudes><email>p" + n
                        + "@test.ma</email></etudiant></n:ajouterEtudiant>", true);
        assertEquals(200, r.statut, r.texte);
        return matricule;
    }

    private static int nouvelleOffre(int places) throws Exception {
        Map<String, Object> offre = new LinkedHashMap<>();
        offre.put("idEntreprise", 2);
        offre.put("titre", "Offre " + NUMERO.incrementAndGet());
        offre.put("description", "Description");
        offre.put("competencesRequises", "BPMN");
        offre.put("dureeMois", 3);
        offre.put("dateLimite", LocalDate.now().plusDays(10).toString());
        offre.put("placesDisponibles", places);
        offre.put("filieresCibles", "Innovation et AMOA");
        Reponse r = api("POST", "/offres", offre);
        assertEquals(201, r.statut, r.texte);
        return r.json.path("idOffre").asInt();
    }

    private static JsonNode deposer(String matricule, int idOffre) throws Exception {
        Reponse r = api("POST", "/processus/candidatures",
                Map.of("matricule", matricule, "idOffre", idOffre, "lettreMotivation", "Motivé"));
        assertEquals(201, r.statut, r.texte);
        return r.json;
    }

    private static String tacheDe(JsonNode processus) throws Exception {
        for (JsonNode t : api("GET", "/processus/taches", null).json) {
            if (t.path("idProcessus").asText().equals(processus.path("idProcessus").asText())) {
                return t.path("idTache").asText();
            }
        }
        throw new AssertionError("Aucune tâche de décision pour " + processus);
    }

    private static String incidentDe(JsonNode processus) throws Exception {
        for (JsonNode t : api("GET", "/processus/incidents", null).json) {
            if (t.path("idProcessus").asText().equals(processus.path("idProcessus").asText())) {
                return t.path("idTache").asText();
            }
        }
        throw new AssertionError("Aucun incident pour " + processus);
    }

    private static JsonNode decider(JsonNode processus, String decision, String motif) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("decision", decision);
        corps.put("motif", motif);
        Reponse r = api("POST", "/processus/taches/" + tacheDe(processus) + "/decision", corps);
        assertEquals(200, r.statut, r.texte);
        return r.json;
    }

    private static JsonNode resoudre(JsonNode processus, String action) throws Exception {
        Reponse r = api("POST", "/processus/incidents/" + incidentDe(processus) + "/resolution", Map.of("action", action));
        assertEquals(200, r.statut, r.texte);
        return r.json;
    }

    private static JsonNode etudiant(String matricule) throws Exception {
        return api("GET", "/etudiants/" + matricule, null).json;
    }

    private static int idEtudiant(String matricule) throws Exception {
        return etudiant(matricule).path("idEtudiant").asInt();
    }

    private static JsonNode candidature(String matricule) throws Exception {
        JsonNode liste = api("GET", "/candidatures?idEtudiant=" + idEtudiant(matricule), null).json;
        assertEquals(1, liste.size(), "Une candidature attendue : " + liste);
        return liste.get(0);
    }

    private static List<String> notificationsEtudiant(String matricule) throws Exception {
        List<String> types = new ArrayList<>();
        for (JsonNode n : api("GET", "/notifications?destinataireType=ETUDIANT&destinataireId=" + idEtudiant(matricule), null).json) {
            types.add(n.path("type").asText());
        }
        return types;
    }

    private static int notificationsEntreprise(int idCandidature, String type) throws Exception {
        int total = 0;
        for (JsonNode n : api("GET", "/notifications?destinataireType=ENTREPRISE&destinataireId=2", null).json) {
            if (n.path("idCandidature").asInt() == idCandidature && n.path("type").asText().equals(type)) {
                total++;
            }
        }
        return total;
    }

    private static int places(int idOffre) throws Exception {
        for (JsonNode o : api("GET", "/offres", null).json) {
            if (o.path("idOffre").asInt() == idOffre) {
                return o.path("placesDisponibles").asInt();
            }
        }
        throw new AssertionError("Offre absente : " + idOffre);
    }

    // ---------- Scénarios fonctionnels

    @Test
    @DisplayName("P01 Candidature acceptée : stage créé, étudiant en stage, place consommée, notifications émises")
    void acceptation() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(2);
        JsonNode p = deposer(m, offre);
        assertEquals("EN_ATTENTE_DECISION", p.path("etat").asText(), p.toString());
        JsonNode c = candidature(m);
        assertEquals("EN_COURS_EVALUATION", c.path("statut").asText());
        assertEquals(1, notificationsEntreprise(c.path("idCandidature").asInt(), "RECEPTION_ENTREPRISE"));

        JsonNode fin = decider(p, "ACCEPTEE", null);
        assertEquals("ACCEPTEE", fin.path("etat").asText(), fin.toString());
        assertEquals("ACCEPTEE", candidature(m).path("statut").asText());
        assertEquals("EN_STAGE", etudiant(m).path("statut").asText());
        assertEquals(1, places(offre));
        JsonNode stages = api("GET", "/stages?idEtudiant=" + idEtudiant(m), null).json;
        assertEquals(1, stages.size());
        assertEquals("PLANIFIE", stages.get(0).path("statut").asText());
        assertEquals(List.of("CANDIDATURE_ACCEPTEE", "CONFIRMATION_DEPOT"), notificationsEtudiant(m));
    }

    @Test
    @DisplayName("P02 Candidature refusée : motif transmis, aucun stage, statut et places inchangés")
    void refus() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        JsonNode fin = decider(deposer(m, offre), "REFUSEE", "Profil insuffisant");
        assertEquals("REFUSEE", fin.path("etat").asText());
        JsonNode c = candidature(m);
        assertEquals("REFUSEE", c.path("statut").asText());
        assertEquals("Profil insuffisant", c.path("motif").asText());
        assertEquals(1, places(offre));
        assertEquals("ACTIF", etudiant(m).path("statut").asText());
        assertEquals(0, api("GET", "/stages?idEtudiant=" + idEtudiant(m), null).json.size());
        assertTrue(notificationsEtudiant(m).contains("CANDIDATURE_REFUSEE"));
    }

    @Test
    @DisplayName("P03 Rejet : étudiant suspendu, sans candidature enregistrée, avec notification")
    void rejetSuspendu() throws Exception {
        JsonNode p = deposer("INE-2026-005", 5);
        assertEquals("REJETEE", p.path("etat").asText(), p.toString());
        assertTrue(p.path("motifsRejet").asText().contains("SUSPENDU"), p.toString());
        assertEquals(0, api("GET", "/candidatures?idEtudiant=5", null).json.size());
        assertTrue(notificationsEtudiant("INE-2026-005").contains("CANDIDATURE_REJETEE"));
    }

    @Test
    @DisplayName("P04 Rejet : étudiant inconnu")
    void rejetInconnu() throws Exception {
        JsonNode p = deposer("INEXISTANT", 5);
        assertEquals("REJETEE", p.path("etat").asText(), p.toString());
        assertTrue(p.path("motifsRejet").asText().contains("inconnu"), p.toString());
    }

    @Test
    @DisplayName("P05 Rejet : offre inconnue, offre clôturée")
    void rejetOffre() throws Exception {
        String m = nouvelEtudiant();
        JsonNode inconnue = deposer(m, 99999);
        assertEquals("REJETEE", inconnue.path("etat").asText());
        assertTrue(inconnue.path("motifsRejet").asText().contains("Offre inconnue"), inconnue.toString());

        JsonNode cloturee = deposer("INE-2026-004", 6);
        assertEquals("REJETEE", cloturee.path("etat").asText());
        assertTrue(cloturee.path("motifsRejet").asText().contains("plus active"), cloturee.toString());
    }

    @Test
    @DisplayName("P06 Rejet : profil académique incompatible")
    void rejetIncompatible() throws Exception {
        JsonNode p = deposer("INE-2026-004", 1);
        assertEquals("REJETEE", p.path("etat").asText());
        assertTrue(p.path("motifsRejet").asText().contains("filière"), p.toString());
    }

    @Test
    @DisplayName("P07 Rejet : date limite dépassée, puis plus de place disponible")
    void rejetDateEtPlaces() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        Plateforme.sql("UPDATE Offre_Stage SET date_limite = '2020-01-01' WHERE id_offre = " + offre);
        JsonNode date = deposer(m, offre);
        assertEquals("REJETEE", date.path("etat").asText());
        assertTrue(date.path("motifsRejet").asText().contains("date limite"), date.toString());

        int complete = nouvelleOffre(0);
        JsonNode places = deposer(m, complete);
        assertEquals("REJETEE", places.path("etat").asText());
        assertTrue(places.path("motifsRejet").asText().contains("place"), places.toString());
    }

    @Test
    @DisplayName("P08 Rejet : candidature en double")
    void rejetDoublon() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        deposer(m, offre);
        JsonNode doublon = deposer(m, offre);
        assertEquals("REJETEE", doublon.path("etat").asText());
        assertTrue(doublon.path("motifsRejet").asText().contains("existe déjà"), doublon.toString());
        assertEquals(1, api("GET", "/candidatures?idEtudiant=" + idEtudiant(m), null).json.size());
    }

    @Test
    @DisplayName("P09 Annulation pendant l'évaluation : le processus se termine et l'étudiant peut redéposer")
    void annulation() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        JsonNode p = deposer(m, offre);
        int id = candidature(m).path("idCandidature").asInt();
        assertEquals(204, api("DELETE", "/candidatures/" + id, null).statut);
        assertEquals("ANNULEE", candidature(m).path("statut").asText());
        assertEquals("ANNULEE", api("GET", "/processus/" + p.path("idProcessus").asText(), null).json.path("etat").asText());
        assertEquals(1, notificationsEntreprise(id, "CANDIDATURE_ANNULEE"));

        JsonNode nouveau = deposer(m, offre);
        assertEquals("EN_ATTENTE_DECISION", nouveau.path("etat").asText(), nouveau.toString());
        assertEquals(id, nouveau.path("candidatureId").asInt());
        decider(nouveau, "ACCEPTEE", null);
        assertEquals(409, api("DELETE", "/candidatures/" + id, null).statut, "Décision finale : annulation refusée");
    }

    @Test
    @DisplayName("P10 Demande de complément : l'étudiant est notifié et le dossier revient à l'entreprise")
    void complement() throws Exception {
        String m = nouvelEtudiant();
        JsonNode p = deposer(m, nouvelleOffre(1));
        JsonNode apres = decider(p, "COMPLEMENT", "Merci d'envoyer le CV");
        assertEquals("EN_ATTENTE_DECISION", apres.path("etat").asText(), apres.toString());
        assertTrue(notificationsEtudiant(m).contains("DEMANDE_COMPLEMENT"));
        assertEquals("ACCEPTEE", decider(p, "ACCEPTEE", null).path("etat").asText());
    }

    @Test
    @DisplayName("P11 Deux candidats retenus pour la dernière place : le second reçoit un refus motivé")
    void dernierePlace() throws Exception {
        String a = nouvelEtudiant();
        String b = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        JsonNode pa = deposer(a, offre);
        JsonNode pb = deposer(b, offre);
        assertEquals("ACCEPTEE", decider(pa, "ACCEPTEE", null).path("etat").asText());
        JsonNode second = decider(pb, "ACCEPTEE", null);
        assertEquals("REFUSEE", second.path("etat").asText(), second.toString());
        assertTrue(candidature(b).path("motif").asText().contains("places"), candidature(b).toString());
        assertEquals(0, places(offre));
    }

    @Test
    @DisplayName("P12 Fin de stage : l'étudiant redevient actif et peut candidater à nouveau")
    void finDeStage() throws Exception {
        String m = nouvelEtudiant();
        decider(deposer(m, nouvelleOffre(1)), "ACCEPTEE", null);
        int offre2 = nouvelleOffre(1);
        assertEquals("REJETEE", deposer(m, offre2).path("etat").asText(), "Un étudiant en stage ne peut pas candidater");

        int stage = api("GET", "/stages?idEtudiant=" + idEtudiant(m), null).json.get(0).path("idStage").asInt();
        assertEquals(200, api("PATCH", "/stages/" + stage, Map.of("statut", "EN_COURS")).statut);
        assertEquals("EN_STAGE", etudiant(m).path("statut").asText());
        assertEquals(200, api("PATCH", "/stages/" + stage, Map.of("statut", "TERMINE")).statut);
        assertEquals("ACTIF", etudiant(m).path("statut").asText());
        int offre3 = nouvelleOffre(1);
        assertEquals("EN_ATTENTE_DECISION", deposer(m, offre3).path("etat").asText());
    }

    @Test
    @DisplayName("P13 Historique et diagramme du processus disponibles")
    void historique() throws Exception {
        String m = nouvelEtudiant();
        JsonNode p = deposer(m, nouvelleOffre(1));
        String id = p.path("idProcessus").asText();
        JsonNode detail = api("GET", "/processus/" + id, null).json;
        List<String> activites = new ArrayList<>();
        detail.path("activites").forEach(a -> activites.add(a.path("id").asText()));
        assertTrue(activites.containsAll(List.of("verifierEtudiant", "verifierOffre", "controlerCandidature",
                "enregistrer", "decisionEntreprise")), activites.toString());
        Reponse image = Plateforme.get("http://localhost:18080/api/processus/" + id + "/diagramme");
        assertEquals(200, image.statut);
        assertTrue(image.texte.startsWith("�PNG"), "Une image PNG est attendue");
        assertEquals(1, api("GET", "/processus?matricule=" + m, null).json.size());
    }

    // ---------- Gestion des erreurs et des indisponibilités

    @Test
    @DisplayName("P20 SOAP indisponible aux contrôles : incident, puis reprise après rétablissement")
    void panneControles() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        Plateforme.couper(Service.ETUDIANTS);
        JsonNode p;
        try {
            p = deposer(m, offre);
            assertEquals("INCIDENT", p.path("etat").asText(), p.toString());
            assertTrue(p.path("erreurMessage").asText().contains("indisponible"), p.toString());
            JsonNode encorePanne = resoudre(p, "RELANCER");
            assertEquals("INCIDENT", encorePanne.path("etat").asText(), "Le service est toujours coupé");
        } finally {
            Plateforme.relancer(Service.ETUDIANTS);
        }
        JsonNode repris = resoudre(p, "RELANCER");
        assertEquals("EN_ATTENTE_DECISION", repris.path("etat").asText(), repris.toString());
        assertEquals("EN_COURS_EVALUATION", candidature(m).path("statut").asText());
    }

    @Test
    @DisplayName("P21 Notifications indisponibles à la transmission : reprise sans doublon de candidature")
    void panneTransmission() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        Plateforme.couper(Service.NOTIFICATIONS);
        JsonNode p;
        try {
            p = deposer(m, offre);
            assertEquals("INCIDENT", p.path("etat").asText(), p.toString());
            assertEquals("DEPOSEE", candidature(m).path("statut").asText(), "Enregistrée avant la panne");
        } finally {
            Plateforme.relancer(Service.NOTIFICATIONS);
        }
        assertEquals("EN_ATTENTE_DECISION", resoudre(p, "RELANCER").path("etat").asText());
        JsonNode c = candidature(m);
        assertEquals("EN_COURS_EVALUATION", c.path("statut").asText());
        assertEquals(1, notificationsEntreprise(c.path("idCandidature").asInt(), "RECEPTION_ENTREPRISE"));
        assertEquals(List.of("CONFIRMATION_DEPOT"), notificationsEtudiant(m));
    }

    @Test
    @DisplayName("P22 Stages indisponible à la clôture : reprise sans double réservation ni double notification")
    void panneCloture() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(2);
        JsonNode p = deposer(m, offre);
        Plateforme.couper(Service.STAGES);
        JsonNode panne;
        try {
            panne = decider(p, "ACCEPTEE", null);
            assertEquals("INCIDENT", panne.path("etat").asText(), panne.toString());
            assertEquals("ACCEPTEE", candidature(m).path("statut").asText());
            assertEquals(1, places(offre));
        } finally {
            Plateforme.relancer(Service.STAGES);
        }
        JsonNode fin = resoudre(p, "RELANCER");
        assertEquals("ACCEPTEE", fin.path("etat").asText(), fin.toString());
        assertEquals(1, places(offre), "La place ne doit pas être réservée deux fois");
        assertEquals("EN_STAGE", etudiant(m).path("statut").asText());
        assertEquals(1, api("GET", "/stages?idEtudiant=" + idEtudiant(m), null).json.size());
        assertEquals(List.of("CANDIDATURE_ACCEPTEE", "CONFIRMATION_DEPOT"), notificationsEtudiant(m));
    }

    @Test
    @DisplayName("P23 Abandon par le responsable : candidature annulée et étudiant prévenu")
    void abandon() throws Exception {
        String m = nouvelEtudiant();
        int offre = nouvelleOffre(1);
        Plateforme.couper(Service.NOTIFICATIONS);
        JsonNode p;
        try {
            p = deposer(m, offre);
            assertEquals("INCIDENT", p.path("etat").asText());
        } finally {
            Plateforme.relancer(Service.NOTIFICATIONS);
        }
        assertEquals(409, api("DELETE", "/candidatures/" + candidature(m).path("idCandidature").asInt(), null).statut,
                "L'annulation est bloquée tant que l'incident n'est pas traité");
        JsonNode fin = resoudre(p, "ABANDONNER");
        assertEquals("ABANDONNEE", fin.path("etat").asText(), fin.toString());
        assertEquals("ANNULEE", candidature(m).path("statut").asText());
        assertTrue(notificationsEtudiant(m).contains("CANDIDATURE_ANNULEE"));
        assertEquals(1, places(offre));
        assertEquals("EN_ATTENTE_DECISION", deposer(m, offre).path("etat").asText(), "Un nouveau dépôt reste possible");
    }

    @Test
    @DisplayName("P24 Panne sur la notification de rejet et de complément : reprise à la bonne étape")
    void panneNotificationsIsolees() throws Exception {
        String m = nouvelEtudiant();
        JsonNode attente = deposer(m, nouvelleOffre(1));
        Plateforme.couper(Service.NOTIFICATIONS);
        JsonNode rejet;
        JsonNode complement;
        try {
            rejet = deposer("INE-2026-005", 5);
            assertEquals("INCIDENT", rejet.path("etat").asText(), rejet.toString());
            complement = decider(attente, "COMPLEMENT", "CV");
            assertEquals("INCIDENT", complement.path("etat").asText(), complement.toString());
        } finally {
            Plateforme.relancer(Service.NOTIFICATIONS);
        }
        assertEquals("REJETEE", resoudre(rejet, "RELANCER").path("etat").asText());
        assertEquals("EN_ATTENTE_DECISION", resoudre(attente, "RELANCER").path("etat").asText());
        assertTrue(notificationsEtudiant(m).contains("DEMANDE_COMPLEMENT"));
        assertNotNull(tacheDe(attente));
        assertFalse(api("GET", "/sante", null).json.isEmpty());
    }

    @Test
    @DisplayName("P25 Validation de l'API : tâche inconnue, décision invalide, requête incomplète")
    void validationApi() throws Exception {
        assertEquals(404, api("POST", "/processus/taches/inconnue/decision", Map.of("decision", "ACCEPTEE")).statut);
        assertEquals(404, api("POST", "/processus/incidents/inconnu/resolution", Map.of("action", "RELANCER")).statut);
        assertEquals(404, api("GET", "/processus/inconnu", null).statut);
        assertEquals(400, api("POST", "/processus/candidatures", Map.of("idOffre", 1)).statut);
        assertEquals(400, api("POST", "/processus/candidatures", Map.of("matricule", "X", "idOffre", "abc")).statut);
        String m = nouvelEtudiant();
        JsonNode p = deposer(m, nouvelleOffre(1));
        assertEquals(400, api("POST", "/processus/taches/" + tacheDe(p) + "/decision", Map.of("decision", "PEUT_ETRE")).statut);
        assertEquals(400, api("POST", "/processus/taches/" + tacheDe(p) + "/decision",
                Map.of("decision", "ACCEPTEE", "dateDebut", "demain")).statut);
    }
}
