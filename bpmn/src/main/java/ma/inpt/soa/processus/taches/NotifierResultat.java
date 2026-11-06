package ma.inpt.soa.processus.taches;

import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 8 : envoi de la décision à l'étudiant. */
public class NotifierResultat extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        boolean acceptee = "ACCEPTEE".equals(texte(e, "decision"));
        String motif = texte(e, "motifDecision");
        String message = "Votre candidature à l'offre « " + texte(e, "titreOffre") + " » de "
                + texte(e, "nomEntreprise") + (acceptee ? " est acceptée." : " est refusée.")
                + (motif == null ? "" : " " + motif);
        uneFois(e, "resultatNotifie", () -> Services.notifier(
                acceptee ? "CANDIDATURE_ACCEPTEE" : "CANDIDATURE_REFUSEE", "ETUDIANT", entier(e, "idEtudiant"),
                texte(e, "emailEtudiant"), acceptee ? "Candidature acceptée" : "Candidature refusée", message,
                entier(e, "candidatureId"), entier(e, "idOffre")));
    }
}
