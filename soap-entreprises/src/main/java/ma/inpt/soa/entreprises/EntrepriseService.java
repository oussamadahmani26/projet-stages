package ma.inpt.soa.entreprises;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import java.util.List;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Entreprise;
import ma.inpt.soa.common.model.OffreStage;
import ma.inpt.soa.common.util.Validation;

@WebService(name = "EntreprisePortType", serviceName = "EntrepriseService", portName = "EntreprisePort",
        targetNamespace = "http://soa.inpt.ma/entreprises")
public class EntrepriseService {
    private final EntrepriseDao dao = new EntrepriseDao();

    @WebMethod
    @WebResult(name = "entreprise")
    public Entreprise ajouterEntreprise(@WebParam(name = "entreprise") Entreprise entreprise)
            throws ServiceException {
        valider(entreprise);
        return consulterEntreprise(dao.inserer(entreprise));
    }

    @WebMethod
    @WebResult(name = "entreprise")
    public Entreprise consulterEntreprise(@WebParam(name = "idEntreprise") Integer idEntreprise)
            throws ServiceException {
        Entreprise entreprise = dao.trouver(identifiant(idEntreprise));
        if (entreprise == null) {
            throw ServiceException.introuvable("Aucune entreprise avec l'identifiant " + idEntreprise + ".");
        }
        return entreprise;
    }

    @WebMethod
    @WebResult(name = "entreprise")
    public List<Entreprise> listerEntreprises() throws ServiceException {
        return dao.lister();
    }

    @WebMethod
    @WebResult(name = "entreprise")
    public Entreprise modifierEntreprise(@WebParam(name = "entreprise") Entreprise entreprise)
            throws ServiceException {
        valider(entreprise);
        identifiant(entreprise.getIdEntreprise());
        consulterEntreprise(entreprise.getIdEntreprise());
        dao.modifier(entreprise);
        return consulterEntreprise(entreprise.getIdEntreprise());
    }

    @WebMethod
    public void supprimerEntreprise(@WebParam(name = "idEntreprise") Integer idEntreprise)
            throws ServiceException {
        if (!dao.supprimer(identifiant(idEntreprise))) {
            throw ServiceException.introuvable("Aucune entreprise avec l'identifiant " + idEntreprise + ".");
        }
    }

    /** Une entreprise est référencée dès lors qu'elle existe dans le référentiel. */
    @WebMethod
    @WebResult(name = "referencee")
    public boolean verifierEntreprise(@WebParam(name = "idEntreprise") Integer idEntreprise)
            throws ServiceException {
        return dao.trouver(identifiant(idEntreprise)) != null;
    }

    @WebMethod
    @WebResult(name = "offre")
    public List<OffreStage> consulterOffresEntreprise(@WebParam(name = "idEntreprise") Integer idEntreprise)
            throws ServiceException {
        consulterEntreprise(idEntreprise);
        return dao.offres(idEntreprise);
    }

    private static int identifiant(Integer id) throws ServiceException {
        return Validation.entier(id, "idEntreprise", 1, Integer.MAX_VALUE);
    }

    private static void valider(Entreprise e) throws ServiceException {
        if (e == null) {
            throw ServiceException.validation("L'entreprise est obligatoire.");
        }
        e.setNom(Validation.texte(e.getNom(), "nom", 160));
        e.setSecteurActivite(Validation.texte(e.getSecteurActivite(), "secteurActivite", 160));
        e.setEmail(Validation.email(e.getEmail(), "email"));
        e.setTelephone(Validation.telephone(e.getTelephone()));
        e.setResponsableContact(Validation.texte(e.getResponsableContact(), "responsableContact", 120));
    }
}
