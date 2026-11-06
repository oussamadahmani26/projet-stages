package ma.inpt.soa.processus.taches;

import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 5, branche négative. Un matricule inconnu n'a pas d'adresse : le motif est alors seulement renvoyé à l'appelant. */
public class NotifierRejet extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        String email = texte(e, "emailEtudiant");
        if (email != null) {
            Services.notifier("CANDIDATURE_REJETEE", "ETUDIANT", entier(e, "idEtudiant"), email,
                    "Candidature rejetée",
                    "Votre candidature n'a pas pu être enregistrée. " + texte(e, "motifsRejet"),
                    null, Boolean.TRUE.equals(e.getVariable("offreConnue")) ? entier(e, "idOffre") : null);
        }
    }
}
