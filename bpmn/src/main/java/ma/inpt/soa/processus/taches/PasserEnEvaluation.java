package ma.inpt.soa.processus.taches;

import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** La candidature passe de DEPOSEE à EN_COURS_EVALUATION. */
public class PasserEnEvaluation extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        Services.rest().envoyer("PATCH", Services.candidatures() + "/" + entier(e, "candidatureId") + "/statut",
                Map.of("statut", "EN_COURS_EVALUATION"));
    }
}
