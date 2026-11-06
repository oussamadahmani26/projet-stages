package ma.inpt.soa.processus.taches;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 9 : création du dossier de stage. Sans date fournie par l'entreprise, le stage débute 30 jours plus tard. */
public class CreerDossierStage extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        if (e.getVariable("stageId") != null) {
            return;
        }
        String date = texte(e, "dateDebutStage");
        LocalDate debut = date == null ? LocalDate.now().plusDays(30) : LocalDate.parse(date);
        LocalDate fin = debut.plusMonths(entier(e, "dureeMois"));
        JsonNode stage = Services.rest().envoyer("POST", Services.stages(), Map.of(
                "idCandidature", entier(e, "candidatureId"),
                "dateDebut", debut.toString(),
                "dateFin", fin.toString()));
        e.setVariable("stageId", stage.path("idStage").asInt());
    }
}
