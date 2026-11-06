package ma.inpt.soa.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import ma.inpt.soa.processus.Moteur;
import ma.inpt.soa.tests.Plateforme.Reponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Connexion et droits : chaque compte n'accède qu'à son espace et à ses données. */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ConnexionTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ETUDIANT_1 = "INE-2026-001";
    private static final String ETUDIANT_2 = "INE-2026-002";
    private static final String CRM = "contact@crmvalue.ma";
    private static final String BONITA = "rh@bonitasoft.com";

    /** Navigateur simulé : chaque client garde ses propres cookies. */
    private static final class Client {
        private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String setCookie;

        Reponse appel(String methode, String chemin, Object corps) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:18080/api" + chemin))
                    .header("Content-Type", "application/json")
                    .method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corps)));
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            setCookie = r.headers().firstValue("Set-Cookie").orElse(setCookie);
            return new Reponse(r.statusCode(), r.body());
        }

        Reponse get(String chemin) throws Exception {
            return appel("GET", chemin, null);
        }
    }

    private static Client connecte(String identifiant, String motDePasse) throws Exception {
        Client c = new Client();
        Reponse r = c.appel("POST", "/auth/connexion", Map.of("identifiant", identifiant, "motDePasse", motDePasse));
        assertEquals(200, r.statut, r.texte);
        return c;
    }

    private static Client etudiant(String matricule) throws Exception {
        return connecte(matricule, Plateforme.MDP_ETUDIANT);
    }

    private static Client entreprise(String identifiant) throws Exception {
        return connecte(identifiant, Plateforme.MDP_ENTREPRISE);
    }

    private static Map<String, Object> offre(String titre) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("titre", titre);
        o.put("description", "Description");
        o.put("competencesRequises", "Java");
        o.put("dureeMois", 3);
        o.put("dateLimite", "2030-01-31");
        o.put("placesDisponibles", 2);
        return o;
    }

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

    @Test
    @DisplayName("C01 Sans connexion, l'API refuse tout sauf la disponibilité des services")
    void anonyme() throws Exception {
        Client c = new Client();
        for (String chemin : new String[] {"/auth/moi", "/etudiants", "/offres", "/candidatures", "/stages",
            "/notifications", "/processus", "/processus/taches", "/processus/incidents"}) {
            assertEquals(401, c.get(chemin).statut, chemin);
        }
        assertEquals(401, c.appel("POST", "/processus/candidatures", Map.of("matricule", ETUDIANT_1, "idOffre", 5)).statut);
        assertEquals(200, c.get("/sante").statut);
    }

    @Test
    @DisplayName("C02 Connexion : mauvais identifiants refusés, message identique, cookie protégé")
    void connexion() throws Exception {
        Client c = new Client();
        Reponse mauvais = c.appel("POST", "/auth/connexion", Map.of("identifiant", ETUDIANT_2, "motDePasse", "faux"));
        assertEquals(401, mauvais.statut);
        Reponse inconnu = c.appel("POST", "/auth/connexion", Map.of("identifiant", "personne", "motDePasse", "faux"));
        assertEquals(401, inconnu.statut);
        assertEquals(mauvais.json.path("message").asText(), inconnu.json.path("message").asText());
        assertEquals(400, c.appel("POST", "/auth/connexion", Map.of("identifiant", ETUDIANT_2)).statut);

        Client ok = etudiant(ETUDIANT_2);
        assertTrue(ok.setCookie.contains("HttpOnly"), ok.setCookie);
        assertTrue(ok.setCookie.contains("SameSite=Strict"), ok.setCookie);
        JsonNode moi = ok.get("/auth/moi").json;
        assertEquals("ETUDIANT", moi.path("role").asText());
        assertEquals(ETUDIANT_2, moi.path("matricule").asText());
        assertFalse(moi.toString().contains("pbkdf2"), "Aucune empreinte ne doit sortir");
    }

    @Test
    @DisplayName("C03 Chaque rôle reçoit son identité")
    void roles() throws Exception {
        JsonNode e = entreprise(CRM).get("/auth/moi").json;
        assertEquals("ENTREPRISE", e.path("role").asText());
        assertEquals(1, e.path("idEntreprise").asInt());
        assertEquals("CRM VALUE", e.path("nom").asText());
        assertEquals("RESPONSABLE", connecte("responsable", Plateforme.MDP_RESPONSABLE).get("/auth/moi").json.path("role").asText());
    }

    @Test
    @DisplayName("C04 Déconnexion : la session n'est plus valable")
    void deconnexion() throws Exception {
        Client c = etudiant(ETUDIANT_2);
        assertEquals(200, c.get("/auth/moi").statut);
        assertEquals(204, c.appel("POST", "/auth/deconnexion", null).statut);
        assertEquals(401, c.get("/auth/moi").statut);
    }

    @Test
    @DisplayName("C05 Un étudiant dépose toujours en son nom et ne voit que ses données")
    void etudiantIsole() throws Exception {
        Client un = etudiant(ETUDIANT_1);
        Reponse depot = un.appel("POST", "/processus/candidatures", Map.of("matricule", ETUDIANT_2, "idOffre", 4));
        assertEquals(201, depot.statut, depot.texte);
        assertEquals(ETUDIANT_1, depot.json.path("matricule").asText(), "Le matricule envoyé est ignoré");

        JsonNode siennes = un.get("/candidatures?idEtudiant=2").json;
        assertEquals(1, siennes.size());
        assertEquals(1, siennes.get(0).path("idEtudiant").asInt(), "Le filtre envoyé est ignoré");
        assertEquals(0, etudiant(ETUDIANT_2).get("/candidatures?idEtudiant=1").json.size());

        assertEquals(403, un.get("/etudiants/" + ETUDIANT_2).statut);
        assertEquals(403, un.get("/etudiants").statut);
        int id = siennes.get(0).path("idCandidature").asInt();
        assertEquals(403, etudiant(ETUDIANT_2).appel("DELETE", "/candidatures/" + id, null).statut, "Pas la sienne");
        assertEquals(204, un.appel("DELETE", "/candidatures/" + id, null).statut);
    }

    @Test
    @DisplayName("C06 Un étudiant n'a accès ni aux décisions ni à la supervision")
    void etudiantSansDroits() throws Exception {
        Client c = etudiant(ETUDIANT_2);
        for (String chemin : new String[] {"/processus", "/processus/taches", "/processus/incidents", "/entreprises", "/entreprises/1/offres"}) {
            assertEquals(403, c.get(chemin).statut, chemin);
        }
        assertEquals(403, c.appel("POST", "/processus/taches/1/decision", Map.of("decision", "ACCEPTEE")).statut);
        assertEquals(403, c.appel("POST", "/offres", offre("Interdite")).statut);
        assertEquals(403, c.appel("POST", "/notifications/rappels", null).statut);
        assertEquals(403, c.appel("PATCH", "/stages/1", Map.of("statut", "EN_COURS")).statut);
    }

    @Test
    @DisplayName("C07 Une entreprise ne traite que ses dossiers et ne gère que ses offres")
    void entrepriseIsolee() throws Exception {
        Client candidat = etudiant("INE-2026-003");
        assertEquals("EN_ATTENTE_DECISION", candidat.appel("POST", "/processus/candidatures",
                Map.of("idOffre", 3)).json.path("etat").asText());

        Client bonita = entreprise(BONITA);
        Client crm = entreprise(CRM);
        JsonNode dossiers = bonita.get("/processus/taches?idEntreprise=1").json;
        assertEquals(1, dossiers.size(), "Le filtre envoyé est ignoré");
        assertEquals(0, crm.get("/processus/taches?idEntreprise=2").json.size());
        String tache = dossiers.get(0).path("idTache").asText();
        assertEquals(403, crm.appel("POST", "/processus/taches/" + tache + "/decision", Map.of("decision", "ACCEPTEE")).statut);
        assertEquals(0, crm.get("/candidatures?idEntreprise=2").json.size(), "Les candidatures reçues par une autre restent cachées");
        assertEquals(403, crm.get("/entreprises/2/offres").statut);
        assertEquals(200, crm.get("/entreprises/1/offres").statut);
        assertEquals(403, crm.appel("DELETE", "/offres/3", null).statut, "Offre d'une autre entreprise");
        assertEquals(403, crm.appel("PUT", "/offres/3", offre("Détournée")).statut);
        assertEquals(403, crm.get("/processus/incidents").statut);
        assertEquals(403, crm.get("/stages").statut);

        Map<String, Object> usurpee = offre("Offre CRM");
        usurpee.put("idEntreprise", 2);
        Reponse creee = crm.appel("POST", "/offres", usurpee);
        assertEquals(201, creee.statut, creee.texte);
        assertEquals("1", creee.json.path("idEntreprise").asText(), "L'entreprise vient de la session");

        assertEquals("ACCEPTEE", bonita.appel("POST", "/processus/taches/" + tache + "/decision",
                Map.of("decision", "ACCEPTEE")).json.path("etat").asText());
        JsonNode stages = candidat.get("/stages").json;
        assertEquals(1, stages.size());
        int stage = stages.get(0).path("idStage").asInt();
        assertEquals(200, candidat.get("/stages/" + stage + "/evaluations").statut);
        assertEquals(403, etudiant(ETUDIANT_2).get("/stages/" + stage + "/evaluations").statut);
    }

    @Test
    @DisplayName("C08 Après cinq échecs, l'identifiant est bloqué, même avec le bon mot de passe")
    void blocage() throws Exception {
        Client c = new Client();
        Map<String, String> faux = Map.of("identifiant", "INE-2026-004", "motDePasse", "faux");
        for (int i = 0; i < 5; i++) {
            assertEquals(401, c.appel("POST", "/auth/connexion", faux).statut);
        }
        Reponse bloque = c.appel("POST", "/auth/connexion",
                Map.of("identifiant", "INE-2026-004", "motDePasse", Plateforme.MDP_ETUDIANT));
        assertEquals(429, bloque.statut, bloque.texte);
        assertNotEquals(200, bloque.statut);
    }

    @Test
    @DisplayName("C09 L'interface web est servie sans connexion, ses données non")
    void interfacePublique() throws Exception {
        Reponse page = Plateforme.get("http://localhost:18080/");
        assertEquals(200, page.statut);
        assertTrue(page.texte.contains("Plateforme de stages"));
    }

    private static Map<String, String> etudiantNeuf(String matricule, String email) {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("role", "ETUDIANT");
        c.put("matricule", matricule);
        c.put("nom", "Nouvel Etudiant");
        c.put("filiere", "Innovation et AMOA");
        c.put("niveauEtudes", "INE 2");
        c.put("email", email);
        c.put("motDePasse", "nouveau-mdp-1");
        return c;
    }

    private static Map<String, String> entrepriseNeuve(String nom, String email) {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("role", "ENTREPRISE");
        c.put("nom", nom);
        c.put("secteurActivite", "IT");
        c.put("responsableContact", "Mme Test");
        c.put("email", email);
        c.put("motDePasse", "nouveau-mdp-2");
        return c;
    }

    @Test
    @DisplayName("C10 Inscription d'un étudiant : connecté, statut ACTIF imposé, peut candidater puis se reconnecter")
    void inscriptionEtudiant() throws Exception {
        Client c = new Client();
        Map<String, String> corps = etudiantNeuf("NEW-001", "new001@test.ma");
        corps.put("statut", "SUSPENDU");
        Reponse r = c.appel("POST", "/auth/inscription", corps);
        assertEquals(201, r.statut, r.texte);
        assertEquals("ETUDIANT", r.json.path("role").asText());
        assertFalse(r.texte.contains("pbkdf2"));
        assertEquals("ACTIF", c.get("/etudiants/NEW-001").json.path("statut").asText(), "Le statut envoyé est ignoré");
        assertEquals("EN_ATTENTE_DECISION", c.appel("POST", "/processus/candidatures", Map.of("idOffre", 2)).json.path("etat").asText());
        assertEquals(200, connecte("NEW-001", "nouveau-mdp-1").get("/auth/moi").statut);
        assertEquals(401, new Client().appel("POST", "/auth/connexion", Map.of("identifiant", "NEW-001", "motDePasse", "faux-mdp-1")).statut);
        try (java.sql.Connection cx = java.sql.DriverManager.getConnection(Plateforme.SERVEUR_DB + Plateforme.BASE, Plateforme.DB_USER, Plateforme.DB_PASSWORD);
                java.sql.ResultSet rs = cx.createStatement().executeQuery("SELECT mot_de_passe_hash FROM Compte WHERE identifiant = 'NEW-001'")) {
            assertTrue(rs.next());
            assertTrue(rs.getString(1).startsWith("pbkdf2$"), "Le mot de passe n'est jamais stocké en clair");
        }
    }

    @Test
    @DisplayName("C11 Inscription d'une entreprise : peut publier une offre, reçoit ses dossiers")
    void inscriptionEntreprise() throws Exception {
        Client c = new Client();
        Reponse r = c.appel("POST", "/auth/inscription", entrepriseNeuve("Nouvelle SA", "rh@nouvelle.ma"));
        assertEquals(201, r.statut, r.texte);
        assertEquals("ENTREPRISE", r.json.path("role").asText());
        Reponse offre = c.appel("POST", "/offres", offre("Offre de Nouvelle SA"));
        assertEquals(201, offre.statut, offre.texte);
        assertEquals(r.json.path("idEntreprise").asText(), offre.json.path("idEntreprise").asText());
    }

    @Test
    @DisplayName("C12 Un compte existant ne peut pas être réutilisé ni usurpé")
    void inscriptionDoublons() throws Exception {
        Client c = new Client();
        assertEquals(409, c.appel("POST", "/auth/inscription", etudiantNeuf(ETUDIANT_1, "autre@test.ma")).statut, "Matricule pris");
        assertEquals(409, c.appel("POST", "/auth/inscription", etudiantNeuf("NEW-002", "dahmani.oussama@ine.inpt.ac.ma")).statut, "E-mail pris");
        assertEquals(409, c.appel("POST", "/auth/inscription", etudiantNeuf("responsable", "resp@test.ma")).statut, "Identifiant réservé");
        assertEquals(409, c.appel("POST", "/auth/inscription", entrepriseNeuve("Autre SA", CRM)).statut, "E-mail d'entreprise pris");
        assertEquals(409, c.appel("POST", "/auth/inscription", entrepriseNeuve("CRM VALUE", "autre@crm.ma")).statut, "Nom d'entreprise pris");
        assertEquals(401, c.get("/auth/moi").statut, "Aucune session ouverte");
        assertEquals(200, etudiant(ETUDIANT_1).get("/auth/moi").statut, "Le compte d'origine reste intact");
    }

    @Test
    @DisplayName("C13 Inscription invalide : rôle réservé, mot de passe court, champs manquants, sans fiche orpheline")
    void inscriptionInvalide() throws Exception {
        Client c = new Client();
        Map<String, String> responsable = etudiantNeuf("NEW-003", "new003@test.ma");
        responsable.put("role", "RESPONSABLE");
        assertEquals(400, c.appel("POST", "/auth/inscription", responsable).statut, "Rôle non proposé");
        Map<String, String> court = etudiantNeuf("NEW-004", "new004@test.ma");
        court.put("motDePasse", "court");
        assertEquals(400, c.appel("POST", "/auth/inscription", court).statut);
        assertEquals(400, c.appel("POST", "/auth/inscription", etudiantNeuf("NEW-005", "pas-un-email")).statut);
        Map<String, String> sansNom = etudiantNeuf("NEW-006", "new006@test.ma");
        sansNom.remove("nom");
        assertEquals(400, c.appel("POST", "/auth/inscription", sansNom).statut);
        assertEquals(400, c.appel("POST", "/auth/inscription", Map.of()).statut);
        Client corrige = new Client();
        assertEquals(201, corrige.appel("POST", "/auth/inscription", etudiantNeuf("NEW-006", "new006@test.ma")).statut,
                "Une tentative refusée ne laisse rien derrière elle");
    }
}
