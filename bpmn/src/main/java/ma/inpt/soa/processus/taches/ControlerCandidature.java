package ma.inpt.soa.processus.taches;

import java.time.LocalDate;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 4 : date limite, places disponibles et absence de doublon. Positionne la variable "valide". */
public class ControlerCandidature extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        if (Boolean.TRUE.equals(e.getVariable("offreConnue"))) {
            if (LocalDate.parse(texte(e, "dateLimite")).isBefore(LocalDate.now())) {
                ajouterMotif(e, "La date limite de candidature est dépassée.");
            }
            if (entier(e, "placesDisponibles") <= 0) {
                ajouterMotif(e, "Plus aucune place disponible.");
            }
            Integer idEtudiant = entier(e, "idEtudiant");
            if (idEtudiant != null && Services.candidatureEnDouble(idEtudiant, entier(e, "idOffre"))) {
                ajouterMotif(e, "Une candidature existe déjà pour cette offre.");
            }
        }
        e.setVariable("valide", texte(e, "motifsRejet").isEmpty());
    }
}
