package ma.inpt.soa.processus.taches;

import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 6 : transmission du dossier à l'entreprise. */
public class NotifierEntreprise extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        uneFois(e, "entrepriseInformee", () -> Services.notifier("RECEPTION_ENTREPRISE", "ENTREPRISE",
                entier(e, "idEntreprise"), texte(e, "emailEntreprise"),
                "Nouvelle candidature reçue",
                texte(e, "nomEtudiant") + " a candidaté à votre offre « " + texte(e, "titreOffre")
                        + " ». Le dossier attend votre décision.",
                entier(e, "candidatureId"), entier(e, "idOffre")));
    }
}
