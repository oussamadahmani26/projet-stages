package ma.inpt.soa.processus.taches;

import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.processus.clients.Services;
import org.flowable.engine.delegate.DelegateExecution;

/** Étape 3 : offre active, entreprise référencée, profil compatible (SOAP). */
public class VerifierOffre extends TacheService {
    @Override
    void realiser(DelegateExecution e) throws ServiceException {
        Map<String, Integer> idOffre = Map.of("idOffre", entier(e, "idOffre"));
        Map<String, String> offre = Services.offres().appelerUn("verifierDisponibilite", idOffre);
        boolean existe = Boolean.parseBoolean(offre.get("existe"));
        e.setVariable("offreConnue", existe);
        if (!existe) {
            ajouterMotif(e, offre.get("motif"));
            return;
        }
        e.setVariable("titreOffre", offre.get("titre"));
        e.setVariable("dureeMois", Integer.valueOf(offre.get("dureeMois")));
        e.setVariable("dateLimite", offre.get("dateLimite"));
        e.setVariable("placesDisponibles", Integer.valueOf(offre.get("placesDisponibles")));
        if (!"ACTIVE".equals(offre.get("statut"))) {
            ajouterMotif(e, "L'offre n'est plus active.");
        }

        Integer idEntreprise = Integer.valueOf(offre.get("idEntreprise"));
        e.setVariable("idEntreprise", idEntreprise);
        Map<String, Integer> parametres = Map.of("idEntreprise", idEntreprise);
        boolean referencee = Boolean.parseBoolean(
                Services.entreprises().appelerUn("verifierEntreprise", parametres).get("valeur"));
        if (referencee) {
            Map<String, String> entreprise = Services.entreprises().appelerUn("consulterEntreprise", parametres);
            e.setVariable("nomEntreprise", entreprise.get("nom"));
            e.setVariable("emailEntreprise", entreprise.get("email"));
        } else {
            ajouterMotif(e, "L'entreprise n'est pas référencée.");
        }

        if (e.getVariable("idEtudiant") != null) {
            Map<String, String> compatibilite = Services.offres().appelerUn("verifierCompatibilite", Map.of(
                    "idOffre", entier(e, "idOffre"),
                    "filiere", texte(e, "filiere"),
                    "niveauEtudes", texte(e, "niveauEtudes")));
            if (!Boolean.parseBoolean(compatibilite.get("compatible"))) {
                ajouterMotif(e, compatibilite.get("motif"));
            }
        }
    }
}
