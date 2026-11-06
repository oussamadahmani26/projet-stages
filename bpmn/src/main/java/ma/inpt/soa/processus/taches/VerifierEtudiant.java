package ma.inpt.soa.processus.taches;

import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 2 : existence et statut de l'étudiant (SOAP). */
public class VerifierEtudiant extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        e.setVariable("motifsRejet", "");
        Map<String, String> parametres = Map.of("matricule", texte(e, "matricule"));
        Map<String, String> verification = Services.etudiants().appelerUn("verifierEtudiant", parametres);
        if (!Boolean.parseBoolean(verification.get("eligible"))) {
            ajouterMotif(e, verification.get("motif"));
        }
        if (Boolean.parseBoolean(verification.get("existe"))) {
            Map<String, String> etudiant = Services.etudiants().appelerUn("consulterEtudiant", parametres);
            e.setVariable("idEtudiant", Integer.valueOf(etudiant.get("idEtudiant")));
            e.setVariable("nomEtudiant", etudiant.get("nom"));
            e.setVariable("emailEtudiant", etudiant.get("email"));
            e.setVariable("filiere", etudiant.get("filiere"));
            e.setVariable("niveauEtudes", etudiant.get("niveauEtudes"));
        }
    }
}
