package ma.inpt.soa.processus.taches;

import java.util.Map;
import java.util.logging.Logger;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * Le responsable renonce à reprendre le traitement : la place réservée est rendue, la candidature annulée
 * et l'étudiant prévenu. Chaque action est indépendante : un service encore en panne n'empêche pas les autres.
 */
public class CompenserAbandon implements JavaDelegate {
    private static final Logger LOG = Logger.getLogger(CompenserAbandon.class.getName());

    @Override
    public void execute(DelegateExecution e) {
        Integer idOffre = (Integer) e.getVariable("idOffre");
        Integer candidatureId = (Integer) e.getVariable("candidatureId");
        if (Boolean.TRUE.equals(e.getVariable("placeReservee")) && e.getVariable("stageId") == null) {
            tenter(() -> Services.offres().appeler("libererPlace", Map.of("idOffre", idOffre)));
        }
        if (candidatureId != null) {
            tenter(() -> Services.rest().envoyer("DELETE", Services.candidatures() + "/" + candidatureId, null));
        }
        String email = (String) e.getVariable("emailEtudiant");
        if (email != null) {
            tenter(() -> Services.notifier("CANDIDATURE_ANNULEE", "ETUDIANT", (Integer) e.getVariable("idEtudiant"),
                    email, "Traitement de candidature interrompu",
                    "Suite à un incident technique, votre candidature n'a pas pu être traitée. Vous pouvez la déposer à nouveau.",
                    candidatureId, idOffre));
        }
    }

    private static void tenter(TacheService.Action action) {
        try {
            action.executer();
        } catch (ServiceException ex) {
            LOG.warning("Compensation incomplète : " + ex.getMessage());
        }
    }
}
