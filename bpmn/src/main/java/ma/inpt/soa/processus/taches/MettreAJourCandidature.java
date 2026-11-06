package ma.inpt.soa.processus.taches;

import java.util.HashMap;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Enregistre la décision finale dans le service REST des candidatures. */
public class MettreAJourCandidature extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        Map<String, String> corps = new HashMap<>();
        corps.put("statut", texte(e, "decision"));
        corps.put("motif", texte(e, "motifDecision"));
        Services.rest().envoyer("PATCH", Services.candidatures() + "/" + entier(e, "candidatureId") + "/statut", corps);
    }
}
