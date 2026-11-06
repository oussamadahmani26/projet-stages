package ma.inpt.soa.processus.taches;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 5, branche positive : enregistrement via le service REST. */
public class EnregistrerCandidature extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        if (e.getVariable("candidatureId") != null) {
            return;
        }
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("idEtudiant", entier(e, "idEtudiant"));
        corps.put("idOffre", entier(e, "idOffre"));
        corps.put("lettreMotivation", texte(e, "lettreMotivation"));
        JsonNode cree = Services.rest().envoyer("POST", Services.candidatures(), corps);
        e.setVariable("candidatureId", cree.path("idCandidature").asInt());
    }
}
