package ma.inpt.soa.processus.taches;

import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 9 : l'étudiant passe au statut EN_STAGE. */
public class MettreAJourEtudiant extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        Services.etudiants().appeler("changerStatutEtudiant",
                Map.of("matricule", texte(e, "matricule"), "statut", "EN_STAGE"));
    }
}
