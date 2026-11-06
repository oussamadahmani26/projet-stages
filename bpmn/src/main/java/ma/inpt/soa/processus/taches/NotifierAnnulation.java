package ma.inpt.soa.processus.taches;

import java.util.logging.Logger;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/** La candidature est déjà annulée à ce stade : un échec de notification est journalisé et ne bloque rien. */
public class NotifierAnnulation implements JavaDelegate {
    private static final Logger LOG = Logger.getLogger(NotifierAnnulation.class.getName());

    @Override
    public void execute(DelegateExecution e) {
        try {
            Services.notifier("CANDIDATURE_ANNULEE", "ENTREPRISE", (Integer) e.getVariable("idEntreprise"),
                    (String) e.getVariable("emailEntreprise"),
                    "Candidature annulée",
                    e.getVariable("nomEtudiant") + " a annulé sa candidature à l'offre « "
                            + e.getVariable("titreOffre") + " ».",
                    (Integer) e.getVariable("candidatureId"), (Integer) e.getVariable("idOffre"));
        } catch (ServiceException ex) {
            LOG.warning("Notification d'annulation non émise : " + ex.getMessage());
        }
    }
}
