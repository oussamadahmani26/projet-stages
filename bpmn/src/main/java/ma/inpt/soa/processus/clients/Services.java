package ma.inpt.soa.processus.clients;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.config.TlsSupport;
import ma.inpt.soa.common.exception.ServiceException;

/** Points d'accès aux six services orchestrés. */
public final class Services {
    private static SoapClient etudiants;
    private static SoapClient entreprises;
    private static SoapClient offres;
    private static RestClient rest;

    private Services() {
    }

    public static synchronized SoapClient etudiants() {
        if (etudiants == null) {
            etudiants = new SoapClient(url("SOAP_ETUDIANTS_URL", 8081, "/ws/etudiants"), "http://soa.inpt.ma/etudiants");
        }
        return etudiants;
    }

    public static synchronized SoapClient entreprises() {
        if (entreprises == null) {
            entreprises = new SoapClient(url("SOAP_ENTREPRISES_URL", 8082, "/ws/entreprises"), "http://soa.inpt.ma/entreprises");
        }
        return entreprises;
    }

    public static synchronized SoapClient offres() {
        if (offres == null) {
            offres = new SoapClient(url("SOAP_OFFRES_URL", 8083, "/ws/offres"), "http://soa.inpt.ma/offres");
        }
        return offres;
    }

    public static synchronized RestClient rest() {
        if (rest == null) {
            rest = new RestClient();
        }
        return rest;
    }

    public static String candidatures() {
        return url("REST_CANDIDATURES_URL", 8084, "/api/candidatures");
    }

    public static String notifications() {
        return url("REST_NOTIFICATIONS_URL", 8085, "/api/notifications");
    }

    public static String stages() {
        return url("REST_STAGES_URL", 8086, "/api/stages");
    }

    private static String url(String variable, int port, String chemin) {
        return AppConfig.get(variable, TlsSupport.schema() + "://localhost:" + port + chemin);
    }

    /** Émet une notification via le service REST. */
    public static void notifier(String type, String destinataireType, Integer destinataireId, String email,
            String sujet, String message, Integer idCandidature, Integer idOffre) throws ServiceException {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("type", type);
        corps.put("destinataireType", destinataireType);
        corps.put("destinataireId", destinataireId);
        corps.put("destinataireEmail", email);
        corps.put("sujet", sujet);
        corps.put("message", message);
        corps.put("idCandidature", idCandidature);
        corps.put("idOffre", idOffre);
        rest().envoyer("POST", notifications(), corps);
    }

    /** Une candidature existe déjà pour ce couple, hors candidature annulée. */
    public static boolean candidatureEnDouble(int idEtudiant, int idOffre) throws ServiceException {
        JsonNode liste = rest().get(candidatures() + "?idEtudiant=" + idEtudiant + "&idOffre=" + idOffre);
        for (JsonNode c : liste) {
            if (!"ANNULEE".equals(c.path("statut").asText())) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> premier(List<Map<String, String>> resultat) {
        return resultat.isEmpty() ? Map.of() : resultat.get(0);
    }
}
