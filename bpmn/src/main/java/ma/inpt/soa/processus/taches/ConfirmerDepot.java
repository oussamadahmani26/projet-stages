package ma.inpt.soa.processus.taches;

import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Confirmation du dépôt adressée à l'étudiant. */
public class ConfirmerDepot extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        uneFois(e, "depotConfirme", () -> Services.notifier("CONFIRMATION_DEPOT", "ETUDIANT",
                entier(e, "idEtudiant"), texte(e, "emailEtudiant"),
                "Candidature enregistrée",
                "Votre candidature à l'offre « " + texte(e, "titreOffre") + " » a bien été enregistrée.",
                entier(e, "candidatureId"), entier(e, "idOffre")));
    }
}
