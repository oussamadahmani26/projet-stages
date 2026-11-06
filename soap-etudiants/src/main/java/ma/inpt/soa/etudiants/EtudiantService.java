package ma.inpt.soa.etudiants;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import java.util.List;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Etudiant;
import ma.inpt.soa.common.util.Validation;

@WebService(name = "EtudiantPortType", serviceName = "EtudiantService", portName = "EtudiantPort",
        targetNamespace = "http://soa.inpt.ma/etudiants")
public class EtudiantService {
    private static final String[] STATUTS = {"ACTIF", "SUSPENDU", "EN_STAGE"};
    private final EtudiantDao dao = new EtudiantDao();

    @WebMethod
    @WebResult(name = "etudiant")
    public Etudiant ajouterEtudiant(@WebParam(name = "etudiant") Etudiant etudiant) throws ServiceException {
        valider(etudiant);
        etudiant.setMatricule(Validation.texte(etudiant.getMatricule(), "matricule", 30));
        etudiant.setStatut(etudiant.getStatut() == null ? "ACTIF"
                : Validation.parmi(etudiant.getStatut(), "statut", STATUTS));
        dao.inserer(etudiant);
        return consulterEtudiant(etudiant.getMatricule());
    }

    @WebMethod
    @WebResult(name = "etudiant")
    public Etudiant consulterEtudiant(@WebParam(name = "matricule") String matricule) throws ServiceException {
        Etudiant etudiant = dao.trouver(Validation.texte(matricule, "matricule", 30));
        if (etudiant == null) {
            throw ServiceException.introuvable("Aucun étudiant avec le matricule " + matricule + ".");
        }
        return etudiant;
    }

    @WebMethod
    @WebResult(name = "etudiant")
    public List<Etudiant> listerEtudiants() throws ServiceException {
        return dao.lister();
    }

    /** Le matricule identifie l'étudiant et ne peut pas être modifié ; le statut passe par changerStatutEtudiant. */
    @WebMethod
    @WebResult(name = "etudiant")
    public Etudiant modifierEtudiant(@WebParam(name = "etudiant") Etudiant etudiant) throws ServiceException {
        valider(etudiant);
        String matricule = Validation.texte(etudiant.getMatricule(), "matricule", 30);
        if (!dao.modifier(etudiant)) {
            throw ServiceException.introuvable("Aucun étudiant avec le matricule " + matricule + ".");
        }
        return consulterEtudiant(matricule);
    }

    @WebMethod
    public void supprimerEtudiant(@WebParam(name = "matricule") String matricule) throws ServiceException {
        if (!dao.supprimer(Validation.texte(matricule, "matricule", 30))) {
            throw ServiceException.introuvable("Aucun étudiant avec le matricule " + matricule + ".");
        }
    }

    @WebMethod
    @WebResult(name = "verification")
    public VerificationEtudiant verifierEtudiant(@WebParam(name = "matricule") String matricule)
            throws ServiceException {
        Etudiant etudiant = dao.trouver(Validation.texte(matricule, "matricule", 30));
        if (etudiant == null) {
            return new VerificationEtudiant(false, false, null, "Étudiant inconnu.");
        }
        boolean eligible = "ACTIF".equals(etudiant.getStatut());
        return new VerificationEtudiant(true, eligible, etudiant.getStatut(),
                eligible ? null : "Statut de l'étudiant : " + etudiant.getStatut() + ".");
    }

    @WebMethod
    @WebResult(name = "etudiant")
    public Etudiant changerStatutEtudiant(@WebParam(name = "matricule") String matricule,
            @WebParam(name = "statut") String statut) throws ServiceException {
        Validation.parmi(statut, "statut", STATUTS);
        if (!dao.changerStatut(Validation.texte(matricule, "matricule", 30), statut)) {
            throw ServiceException.introuvable("Aucun étudiant avec le matricule " + matricule + ".");
        }
        return consulterEtudiant(matricule);
    }

    private static void valider(Etudiant e) throws ServiceException {
        if (e == null) {
            throw ServiceException.validation("L'étudiant est obligatoire.");
        }
        e.setNom(Validation.texte(e.getNom(), "nom", 120));
        e.setFiliere(Validation.texte(e.getFiliere(), "filiere", 120));
        e.setNiveauEtudes(Validation.texte(e.getNiveauEtudes(), "niveauEtudes", 30));
        e.setEmail(Validation.email(e.getEmail(), "email"));
        e.setTelephone(Validation.telephone(e.getTelephone()));
    }
}
