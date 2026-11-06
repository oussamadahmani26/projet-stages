package ma.inpt.soa.processus.taches;

import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** L'entreprise demande un complément d'information ; le dossier revient ensuite à sa décision. */
public class NotifierComplement extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        String motif = texte(e, "motifDecision");
        Services.notifier("DEMANDE_COMPLEMENT", "ETUDIANT", entier(e, "idEtudiant"), texte(e, "emailEtudiant"),
                "Complément d'information demandé",
                texte(e, "nomEntreprise") + " demande un complément pour l'offre « " + texte(e, "titreOffre") + " »"
                        + (motif == null ? "." : " : " + motif),
                entier(e, "candidatureId"), entier(e, "idOffre"));
    }
}
