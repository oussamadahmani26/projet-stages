package ma.inpt.soa.processus.taches;

import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * Une acceptation n'est effective que si l'étudiant reste disponible et qu'une place peut être réservée.
 * Sinon, deux candidats retenus sur la dernière place par exemple, elle est transformée en refus motivé.
 */
public class ConfirmerDecision extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        if (!"ACCEPTEE".equals(texte(e, "decision")) || Boolean.TRUE.equals(e.getVariable("placeReservee"))) {
            return;
        }
        Map<String, String> etudiant = Services.etudiants().appelerUn("verifierEtudiant",
                Map.of("matricule", texte(e, "matricule")));
        if (!Boolean.parseBoolean(etudiant.get("eligible"))) {
            refuser(e, "L'étudiant n'est plus disponible pour ce stage.");
            return;
        }
        boolean reservee = Boolean.parseBoolean(Services.offres()
                .appelerUn("reserverPlace", Map.of("idOffre", entier(e, "idOffre"))).get("valeur"));
        if (reservee) {
            e.setVariable("placeReservee", true);
        } else {
            refuser(e, "Toutes les places de cette offre ont été attribuées.");
        }
    }

    private static void refuser(DelegateExecution e, String motif) {
        e.setVariable("decision", "REFUSEE");
        e.setVariable("motifDecision", motif);
    }
}
