package ma.inpt.soa.tests;

import static ma.inpt.soa.tests.Plateforme.api;
import static ma.inpt.soa.tests.Plateforme.rest;
import static ma.inpt.soa.tests.Plateforme.soap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.tests.Plateforme.Reponse;
import ma.inpt.soa.tests.Plateforme.Service;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Contrats des six services, appelés directement comme le ferait SoapUI ou Postman. */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ServicesTest {
    private static final String NS_ETU = "http://soa.inpt.ma/etudiants";
    private static final String NS_ENT = "http://soa.inpt.ma/entreprises";
    private static final String NS_OFF = "http://soa.inpt.ma/offres";
    private static final String WS_ETU = "/ws/etudiants";
    private static final String WS_ENT = "/ws/entreprises";
    private static final String WS_OFF = "/ws/offres";

    @BeforeAll
    static void demarrer() throws Exception {
        Assumptions.assumeTrue(Plateforme.baseAccessible(), "MySQL/MariaDB inaccessible : tests ignorés.");
        Plateforme.initialiserBase();
        Plateforme.demarrer();
    }

    @AfterAll
    static void arreter() {
        Plateforme.arreterTout();
    }

    private static String offreXml(String date, int places) {
        return "<offre><idEntreprise>1</idEntreprise><titre>Offre test</titre><description>Desc</description>"
                + "<competencesRequises>Java</competencesRequises><dureeMois>3</dureeMois><dateLimite>" + date
                + "</dateLimite><placesDisponibles>" + places + "</placesDisponibles>"
                + "<filieresCibles>Innovation et AMOA</filieresCibles><niveauMinimum>2</niveauMinimum></offre>";
    }

    private static String id(String xml, String balise) {
        return xml.replaceAll("(?s).*<" + balise + ">(.*?)</" + balise + ">.*", "$1");
    }

    @Test
    @DisplayName("S01 Les WSDL sont publics et décrivent les opérations demandées")
    void wsdl() throws Exception {
        Map<Service, String> chemins = Map.of(Service.ETUDIANTS, WS_ETU, Service.ENTREPRISES, WS_ENT, Service.OFFRES, WS_OFF);
        Map<Service, List<String>> operations = Map.of(
                Service.ETUDIANTS, List.of("ajouterEtudiant", "consulterEtudiant", "modifierEtudiant", "supprimerEtudiant", "verifierEtudiant"),
                Service.ENTREPRISES, List.of("ajouterEntreprise", "consulterEntreprise", "modifierEntreprise", "supprimerEntreprise", "consulterOffresEntreprise"),
                Service.OFFRES, List.of("creerOffre", "modifierOffre", "supprimerOffre", "verifierDisponibilite", "verifierCompatibilite"));
        for (Service s : chemins.keySet()) {
            Reponse r = Plateforme.get("http://localhost:" + Plateforme.port(s) + chemins.get(s) + "?wsdl");
            assertEquals(200, r.statut);
            for (String op : operations.get(s)) {
                assertTrue(r.texte.contains("operation name=\"" + op + "\""), op);
            }
        }
    }

    @Test
    @DisplayName("S02 Les opérations SOAP et REST exigent une authentification")
    void authentification() throws Exception {
        assertEquals(401, soap(Service.ETUDIANTS, WS_ETU, NS_ETU, "<n:listerEtudiants/>", false).statut);
        assertEquals(401, rest(Service.CANDIDATURES, "GET", "/candidatures", null, false).statut);
        assertEquals(401, rest(Service.NOTIFICATIONS, "GET", "/notifications", null, false).statut);
        assertEquals(401, rest(Service.STAGES, "GET", "/stages", null, false).statut);
    }

    @Test
    @DisplayName("S03 SOAP Étudiants : cycle complet, validation et erreurs")
    void etudiants() throws Exception {
        String etu = "<etudiant><matricule>T-S03</matricule><nom>Test S03</nom><filiere>Data Science</filiere>"
                + "<niveauEtudes>INE 1</niveauEtudes><email>s03@test.ma</email><telephone>+212600000099</telephone></etudiant>";
        Reponse ajout = soap(Service.ETUDIANTS, WS_ETU, NS_ETU, "<n:ajouterEtudiant>" + etu + "</n:ajouterEtudiant>", true);
        assertEquals(200, ajout.statut, ajout.texte);
        assertTrue(ajout.texte.contains("<statut>ACTIF</statut>"));

        Reponse doublon = soap(Service.ETUDIANTS, WS_ETU, NS_ETU, "<n:ajouterEtudiant>" + etu + "</n:ajouterEtudiant>", true);
        assertEquals(500, doublon.statut);
        assertTrue(doublon.texte.contains("<code>CONFLIT</code>"), doublon.texte);

        Reponse invalide = soap(Service.ETUDIANTS, WS_ETU, NS_ETU, "<n:ajouterEtudiant>"
                + etu.replace("s03@test.ma", "pas-un-email").replace("T-S03", "T-S03B") + "</n:ajouterEtudiant>", true);
        assertTrue(invalide.texte.contains("<code>VALIDATION</code>"), invalide.texte);

        Reponse modif = soap(Service.ETUDIANTS, WS_ETU, NS_ETU, "<n:modifierEtudiant>"
                + etu.replace("Test S03", "Nom Modifié") + "</n:modifierEtudiant>", true);
        assertTrue(modif.texte.contains("Nom Modifié"), modif.texte);

        Reponse verif = soap(Service.ETUDIANTS, WS_ETU, NS_ETU,
                "<n:verifierEtudiant><matricule>T-S03</matricule></n:verifierEtudiant>", true);
        assertTrue(verif.texte.contains("<eligible>true</eligible>"), verif.texte);

        assertEquals(200, soap(Service.ETUDIANTS, WS_ETU, NS_ETU,
                "<n:supprimerEtudiant><matricule>T-S03</matricule></n:supprimerEtudiant>", true).statut);
        Reponse absent = soap(Service.ETUDIANTS, WS_ETU, NS_ETU,
                "<n:consulterEtudiant><matricule>T-S03</matricule></n:consulterEtudiant>", true);
        assertTrue(absent.texte.contains("<code>INTROUVABLE</code>"), absent.texte);
        assertFalse(absent.texte.contains("at ma.inpt"), "Aucune trace technique ne doit fuiter");
    }

    @Test
    @DisplayName("S04 SOAP Entreprises : cycle complet et offres d'une entreprise")
    void entreprises() throws Exception {
        String ent = "<entreprise>%s<nom>%s</nom><secteurActivite>IT</secteurActivite><email>rh@s04.ma</email>"
                + "<responsableContact>Mme X</responsableContact></entreprise>";
        Reponse ajout = soap(Service.ENTREPRISES, WS_ENT, NS_ENT,
                "<n:ajouterEntreprise>" + String.format(ent, "", "S04 SA") + "</n:ajouterEntreprise>", true);
        assertEquals(200, ajout.statut, ajout.texte);
        String id = id(ajout.texte, "idEntreprise");

        Reponse modif = soap(Service.ENTREPRISES, WS_ENT, NS_ENT, "<n:modifierEntreprise>"
                + String.format(ent, "<idEntreprise>" + id + "</idEntreprise>", "S04 Maroc") + "</n:modifierEntreprise>", true);
        assertTrue(modif.texte.contains("S04 Maroc"), modif.texte);

        Reponse offres = soap(Service.ENTREPRISES, WS_ENT, NS_ENT,
                "<n:consulterOffresEntreprise><idEntreprise>1</idEntreprise></n:consulterOffresEntreprise>", true);
        assertTrue(offres.texte.contains("Assistant Maîtrise"), offres.texte);

        Reponse liee = soap(Service.ENTREPRISES, WS_ENT, NS_ENT,
                "<n:supprimerEntreprise><idEntreprise>1</idEntreprise></n:supprimerEntreprise>", true);
        assertTrue(liee.texte.contains("<code>CONFLIT</code>"), liee.texte);

        assertEquals(200, soap(Service.ENTREPRISES, WS_ENT, NS_ENT,
                "<n:supprimerEntreprise><idEntreprise>" + id + "</idEntreprise></n:supprimerEntreprise>", true).statut);
        Reponse verif = soap(Service.ENTREPRISES, WS_ENT, NS_ENT,
                "<n:verifierEntreprise><idEntreprise>" + id + "</idEntreprise></n:verifierEntreprise>", true);
        assertTrue(verif.texte.contains("<referencee>false</referencee>"), verif.texte);
    }

    @Test
    @DisplayName("S05 SOAP Offres : création, disponibilité, compatibilité, réservation")
    void offres() throws Exception {
        String demain = LocalDate.now().plusDays(1).toString();
        Reponse passee = soap(Service.OFFRES, WS_OFF, NS_OFF,
                "<n:creerOffre>" + offreXml("2020-01-01", 1) + "</n:creerOffre>", true);
        assertTrue(passee.texte.contains("<code>VALIDATION</code>"), passee.texte);

        Reponse creee = soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:creerOffre>" + offreXml(demain, 1) + "</n:creerOffre>", true);
        assertEquals(200, creee.statut, creee.texte);
        String id = id(creee.texte, "idOffre");
        String paramId = "<idOffre>" + id + "</idOffre>";

        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:verifierDisponibilite>" + paramId + "</n:verifierDisponibilite>", true)
                .texte.contains("<disponible>true</disponible>"));
        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:verifierCompatibilite>" + paramId
                + "<filiere>Innovation et AMOA</filiere><niveauEtudes>INE 2</niveauEtudes></n:verifierCompatibilite>", true)
                .texte.contains("<compatible>true</compatible>"));
        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:verifierCompatibilite>" + paramId
                + "<filiere>Data Science</filiere><niveauEtudes>INE 2</niveauEtudes></n:verifierCompatibilite>", true)
                .texte.contains("<compatible>false</compatible>"));
        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:verifierCompatibilite>" + paramId
                + "<filiere>Innovation et AMOA</filiere><niveauEtudes>INE 1</niveauEtudes></n:verifierCompatibilite>", true)
                .texte.contains("<compatible>false</compatible>"));

        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:reserverPlace>" + paramId + "</n:reserverPlace>", true)
                .texte.contains("<reservee>true</reservee>"));
        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:reserverPlace>" + paramId + "</n:reserverPlace>", true)
                .texte.contains("<reservee>false</reservee>"));
        assertTrue(soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:verifierDisponibilite>" + paramId + "</n:verifierDisponibilite>", true)
                .texte.contains("<disponible>false</disponible>"));
        assertEquals(200, soap(Service.OFFRES, WS_OFF, NS_OFF, "<n:supprimerOffre>" + paramId + "</n:supprimerOffre>", true).statut);
    }

    @Test
    @DisplayName("S06 REST Candidatures : codes HTTP et règles de gestion")
    void candidatures() throws Exception {
        Service s = Service.CANDIDATURES;
        Reponse creee = rest(s, "POST", "/candidatures", Map.of("idEtudiant", 1, "idOffre", 5), true);
        assertEquals(201, creee.statut, creee.texte);
        int id = creee.json.path("idCandidature").asInt();
        assertEquals("DEPOSEE", creee.json.path("statut").asText());

        assertEquals(409, rest(s, "POST", "/candidatures", Map.of("idEtudiant", 1, "idOffre", 5), true).statut);
        assertEquals(404, rest(s, "POST", "/candidatures", Map.of("idEtudiant", 9999, "idOffre", 5), true).statut);
        assertEquals(400, rest(s, "POST", "/candidatures", Map.of("idOffre", 5), true).statut);
        assertEquals(400, rest(s, "POST", "/candidatures", "{invalide", true).statut);
        assertEquals(404, rest(s, "GET", "/candidatures/99999", null, true).statut);
        assertEquals(1, rest(s, "GET", "/candidatures?idEtudiant=1&idOffre=5", null, true).json.size());

        assertEquals(409, rest(s, "PATCH", "/candidatures/" + id + "/statut", Map.of("statut", "ACCEPTEE"), true).statut,
                "DEPOSEE ne peut pas passer directement à ACCEPTEE");
        assertEquals(400, rest(s, "PATCH", "/candidatures/" + id + "/statut", Map.of("statut", "INCONNU"), true).statut);

        assertEquals(204, rest(s, "DELETE", "/candidatures/" + id, null, true).statut);
        assertEquals(204, rest(s, "DELETE", "/candidatures/" + id, null, true).statut, "L'annulation est idempotente");

        Reponse redepot = rest(s, "POST", "/candidatures", Map.of("idEtudiant", 1, "idOffre", 5), true);
        assertEquals(201, redepot.statut, "Une candidature annulée peut être redéposée");
        assertEquals(id, redepot.json.path("idCandidature").asInt());

        assertEquals(200, rest(s, "PATCH", "/candidatures/" + id + "/statut", Map.of("statut", "EN_COURS_EVALUATION"), true).statut);
        assertEquals(200, rest(s, "PATCH", "/candidatures/" + id + "/statut", Map.of("statut", "REFUSEE", "motif", "Profil"), true).statut);
        assertEquals(409, rest(s, "DELETE", "/candidatures/" + id, null, true).statut, "Décision finale : annulation interdite");
    }

    @Test
    @DisplayName("S07 REST Notifications : envoi, validation, consultation, rappels")
    void notifications() throws Exception {
        Service s = Service.NOTIFICATIONS;
        Map<String, Object> corps = Map.of("type", "DEMANDE_COMPLEMENT", "destinataireType", "ETUDIANT",
                "destinataireId", 2, "destinataireEmail", "chafik.jawad@ine.inpt.ac.ma", "sujet", "Sujet", "message", "Message");
        Reponse creee = rest(s, "POST", "/notifications", corps, true);
        assertEquals(201, creee.statut, creee.texte);
        assertEquals("SIMULEE", creee.json.path("statutEnvoi").asText());
        assertEquals(400, rest(s, "POST", "/notifications", Map.of("type", "INCONNU"), true).statut);
        assertEquals(404, rest(s, "GET", "/notifications/99999", null, true).statut);
        assertTrue(rest(s, "GET", "/notifications?destinataireType=ETUDIANT&destinataireId=2", null, true).json.size() >= 1);

        Plateforme.sql("UPDATE Offre_Stage SET date_limite = DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY) WHERE id_offre = 4");
        Reponse rappels = rest(s, "POST", "/notifications/rappels?joursAvant=5", null, true);
        assertEquals(200, rappels.statut, rappels.texte);
        assertTrue(rappels.json.path("rappelsEnvoyes").asInt() >= 1, rappels.texte);
        assertEquals(0, rest(s, "POST", "/notifications/rappels?joursAvant=5", null, true).json.path("rappelsEnvoyes").asInt(),
                "Un seul rappel par jour et par destinataire");
    }

    @Test
    @DisplayName("S08 REST Stages : création, statut, comptes rendus, évaluations, historique")
    void stages() throws Exception {
        Service s = Service.STAGES;
        Reponse cand = rest(Service.CANDIDATURES, "POST", "/candidatures", Map.of("idEtudiant", 3, "idOffre", 3), true);
        int idCand = cand.json.path("idCandidature").asInt();
        Map<String, Object> corps = Map.of("idCandidature", idCand, "dateDebut", "2027-03-01", "dateFin", "2027-09-01");
        assertEquals(409, rest(s, "POST", "/stages", corps, true).statut, "La candidature n'est pas acceptée");

        rest(Service.CANDIDATURES, "PATCH", "/candidatures/" + idCand + "/statut", Map.of("statut", "EN_COURS_EVALUATION"), true);
        rest(Service.CANDIDATURES, "PATCH", "/candidatures/" + idCand + "/statut", Map.of("statut", "ACCEPTEE"), true);

        assertEquals(400, rest(s, "POST", "/stages",
                Map.of("idCandidature", idCand, "dateDebut", "2027-09-01", "dateFin", "2027-03-01"), true).statut);
        Reponse stage = rest(s, "POST", "/stages", corps, true);
        assertEquals(201, stage.statut, stage.texte);
        assertEquals(409, rest(s, "POST", "/stages", corps, true).statut, "Un seul stage par candidature");
        int id = stage.json.path("idStage").asInt();
        assertEquals("2027-03-01", stage.json.path("dateDebut").asText());

        Map<String, Object> cr = Map.of("periode", "Mars 2027", "contenu", "Avancement", "auteur", "Tuteur");
        assertEquals(409, rest(s, "POST", "/stages/" + id + "/comptes-rendus", cr, true).statut, "Stage pas encore en cours");
        assertEquals(409, rest(s, "PATCH", "/stages/" + id, Map.of("statut", "TERMINE"), true).statut);
        assertEquals(200, rest(s, "PATCH", "/stages/" + id, Map.of("statut", "EN_COURS"), true).statut);
        assertEquals(201, rest(s, "POST", "/stages/" + id + "/comptes-rendus", cr, true).statut);
        assertEquals(1, rest(s, "GET", "/stages/" + id + "/comptes-rendus", null, true).json.size());

        assertEquals(400, rest(s, "POST", "/stages/" + id + "/evaluations", Map.of("note", 25, "evaluateur", "Tuteur"), true).statut);
        assertEquals(201, rest(s, "POST", "/stages/" + id + "/evaluations", Map.of("note", 16.5, "evaluateur", "Tuteur"), true).statut);
        assertEquals(16.5, rest(s, "GET", "/stages/" + id + "/evaluations", null, true).json.get(0).path("note").asDouble());
        assertEquals(1, rest(s, "GET", "/stages?idEtudiant=3", null, true).json.size());
        assertEquals(404, rest(s, "GET", "/stages/99999", null, true).statut);
    }

    @Test
    @DisplayName("S09 API façade : les données SOAP sont exposées en JSON pour le navigateur")
    void facade() throws Exception {
        Reponse etudiants = api("GET", "/etudiants", null);
        assertEquals(200, etudiants.statut, etudiants.texte);
        assertTrue(etudiants.json.size() >= 5);
        assertEquals(6, api("GET", "/sante", null).json.size());
        assertEquals(404, api("GET", "/etudiants/INCONNU", null).statut);
    }
}
