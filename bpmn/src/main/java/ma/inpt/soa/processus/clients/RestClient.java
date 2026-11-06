package ma.inpt.soa.processus.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.rest.RestServer;

/** Client JSON des services REST. Traduit les codes HTTP en ServiceException. */
public class RestClient {
    private final ObjectMapper json = RestServer.objectMapper();

    public JsonNode get(String url) throws ServiceException {
        return traiter(HttpSupport.requete(url).GET().build());
    }

    public JsonNode envoyer(String methode, String url, Object corps) throws ServiceException {
        try {
            HttpRequest.BodyPublisher contenu = corps == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corps));
            return traiter(HttpSupport.requete(url).header("Content-Type", "application/json")
                    .method(methode, contenu).build());
        } catch (JsonProcessingException e) {
            throw ServiceException.validation("Corps de requête invalide.");
        }
    }

    private JsonNode traiter(HttpRequest requete) throws ServiceException {
        HttpResponse<String> reponse = HttpSupport.envoyer(requete);
        int statut = reponse.statusCode();
        JsonNode corps = lire(reponse.body());
        if (statut >= 200 && statut < 300) {
            return corps;
        }
        String message = corps.path("message").asText("Erreur HTTP " + statut);
        switch (statut) {
            case 400: return lever(ServiceException.VALIDATION, message);
            case 404: return lever(ServiceException.INTROUVABLE, message);
            case 409: return lever(ServiceException.CONFLIT, message);
            default: throw HttpSupport.indisponible(requete);
        }
    }

    private static JsonNode lever(String code, String message) throws ServiceException {
        throw new ServiceException(code, message);
    }

    private JsonNode lire(String texte) {
        try {
            return texte == null || texte.isBlank() ? json.nullNode() : json.readTree(texte);
        } catch (JsonProcessingException e) {
            return json.nullNode();
        }
    }
}
