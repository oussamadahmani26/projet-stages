package ma.inpt.soa.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import ma.inpt.soa.candidatures.CandidaturesApplication;
import ma.inpt.soa.entreprises.EntreprisesApplication;
import ma.inpt.soa.etudiants.EtudiantsApplication;
import ma.inpt.soa.notifications.NotificationsApplication;
import ma.inpt.soa.offres.OffresApplication;
import ma.inpt.soa.processus.ProcessusApplication;
import ma.inpt.soa.stages.StagesApplication;

/** Démarre les vrais services sur une base dédiée, et fournit les appels HTTP des tests. */
final class Plateforme {
    enum Service { ETUDIANTS, ENTREPRISES, OFFRES, CANDIDATURES, NOTIFICATIONS, STAGES }

    static final String BASE = "gestion_stages_test";
    static final String SERVEUR_DB = System.getProperty("TEST_DB_SERVER", "jdbc:mysql://localhost:3306/");
    static final String DB_USER = System.getProperty("DB_USER", "root");
    static final String DB_PASSWORD = System.getProperty("DB_PASSWORD", "");
    static final String AUTH = "Basic " + Base64.getEncoder().encodeToString(
            "soa_client:soa_dev_password".getBytes(StandardCharsets.UTF_8));
    private static final int FACADE = 18080;
    static final String MDP_ETUDIANT = "etudiant2026";
    static final String MDP_ENTREPRISE = "entreprise2026";
    static final String MDP_RESPONSABLE = "responsable2026";
    private static final HttpClient CLIENT = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<Service, HttpServer> SERVEURS = new EnumMap<>(Service.class);
    private static HttpServer facade;

    private Plateforme() {
    }

    static boolean baseAccessible() {
        try (Connection c = DriverManager.getConnection(SERVEUR_DB, DB_USER, DB_PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Recrée uniquement la base de test, à partir des scripts livrés. */
    static void initialiserBase() throws IOException, SQLException {
        try (Connection c = DriverManager.getConnection(SERVEUR_DB, DB_USER, DB_PASSWORD);
                Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + BASE);
            s.execute("CREATE DATABASE " + BASE + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            s.execute("USE " + BASE);
            for (String script : new String[] {"schema.sql", "data.sql", "comptes.sql"}) {
                String texte = Files.readString(Path.of("..", "sql", script), StandardCharsets.UTF_8);
                for (String instruction : texte.split(";\\s*\\R")) {
                    String nettoyee = instruction.replaceAll("(?m)^\\s*--.*$", "").trim();
                    if (!nettoyee.isEmpty() && !nettoyee.toUpperCase().startsWith("USE ")
                            && !nettoyee.toUpperCase().startsWith("CREATE DATABASE")) {
                        s.execute(nettoyee);
                    }
                }
            }
        }
    }

    static void sql(String instruction) throws SQLException {
        try (Connection c = DriverManager.getConnection(SERVEUR_DB + BASE, DB_USER, DB_PASSWORD);
                Statement s = c.createStatement()) {
            s.execute(instruction);
        }
    }

    static void demarrer() throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("DB_URL", SERVEUR_DB + BASE + "?useUnicode=true&characterEncoding=UTF-8");
        System.setProperty("FLOWABLE_DB_URL", "jdbc:h2:mem:flowable-test;DB_CLOSE_DELAY=-1");
        System.setProperty("SOAP_ETUDIANTS_URL", "http://localhost:" + port(Service.ETUDIANTS) + "/ws/etudiants");
        System.setProperty("SOAP_ENTREPRISES_URL", "http://localhost:" + port(Service.ENTREPRISES) + "/ws/entreprises");
        System.setProperty("SOAP_OFFRES_URL", "http://localhost:" + port(Service.OFFRES) + "/ws/offres");
        System.setProperty("REST_CANDIDATURES_URL", "http://localhost:" + port(Service.CANDIDATURES) + "/api/candidatures");
        System.setProperty("REST_NOTIFICATIONS_URL", "http://localhost:" + port(Service.NOTIFICATIONS) + "/api/notifications");
        System.setProperty("REST_STAGES_URL", "http://localhost:" + port(Service.STAGES) + "/api/stages");
        for (Service s : Service.values()) {
            relancer(s);
        }
        facade = ProcessusApplication.demarrer(FACADE);
        Reponse connexion = api("POST", "/auth/connexion", java.util.Map.of("identifiant", "responsable", "motDePasse", MDP_RESPONSABLE));
        if (connexion.statut != 200) {
            throw new IllegalStateException("Connexion du responsable impossible : " + connexion.texte);
        }
    }

    static void arreterTout() {
        SERVEURS.values().forEach(s -> s.stop(0));
        SERVEURS.clear();
        if (facade != null) {
            facade.stop(0);
        }
    }

    static int port(Service s) {
        return 18081 + s.ordinal();
    }

    /** Simule une panne réelle : le port ne répond plus. */
    static void couper(Service s) {
        SERVEURS.remove(s).stop(0);
    }

    static void relancer(Service s) throws IOException {
        int p = port(s);
        HttpServer serveur;
        switch (s) {
            case ETUDIANTS: serveur = EtudiantsApplication.demarrer(p); break;
            case ENTREPRISES: serveur = EntreprisesApplication.demarrer(p); break;
            case OFFRES: serveur = OffresApplication.demarrer(p); break;
            case CANDIDATURES: serveur = CandidaturesApplication.demarrer(p); break;
            case NOTIFICATIONS: serveur = NotificationsApplication.demarrer(p); break;
            default: serveur = StagesApplication.demarrer(p); break;
        }
        SERVEURS.put(s, serveur);
    }

    /** Réponse HTTP dont le corps JSON est déjà analysé. */
    static final class Reponse {
        final int statut;
        final String texte;
        final JsonNode json;

        Reponse(int statut, String texte) throws IOException {
            this.statut = statut;
            this.texte = texte;
            this.json = texte == null || texte.isBlank() || !texte.trim().matches("(?s)^[\\[{].*")
                    ? JSON.nullNode() : JSON.readTree(texte);
        }
    }

    /** Appel de l'API façade (celle de l'interface graphique). */
    static Reponse api(String methode, String chemin, Object corps) throws Exception {
        return appel(methode, "http://localhost:" + FACADE + "/api" + chemin, corps, null);
    }

    /** Appel direct d'un service REST. */
    static Reponse rest(Service s, String methode, String chemin, Object corps, boolean authentifie) throws Exception {
        return appel(methode, "http://localhost:" + port(s) + "/api" + chemin, corps, authentifie ? AUTH : null);
    }

    private static Reponse appel(String methode, String url, Object corps, String auth) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json");
        if (auth != null) {
            b.header("Authorization", auth);
        }
        b.method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(
                        corps instanceof String ? (String) corps : JSON.writeValueAsString(corps)));
        HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reponse(r.statusCode(), r.body());
    }

    /** Appel direct d'un service SOAP avec une enveloppe écrite à la main. */
    static Reponse soap(Service s, String chemin, String ns, String corps, boolean authentifie) throws Exception {
        String enveloppe = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:n=\"" + ns
                + "\"><s:Body>" + corps + "</s:Body></s:Envelope>";
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port(s) + chemin))
                .header("Content-Type", "text/xml; charset=utf-8");
        if (authentifie) {
            b.header("Authorization", AUTH);
        }
        HttpResponse<String> r = CLIENT.send(b.POST(HttpRequest.BodyPublishers.ofString(enveloppe)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reponse(r.statusCode(), r.body());
    }

    static Reponse get(String url) throws Exception {
        HttpResponse<String> r = CLIENT.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reponse(r.statusCode(), r.body());
    }
}
