package ma.inpt.soa.offres;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.OffreStage;
import ma.inpt.soa.common.util.Validation;

@WebService(name = "OffrePortType", serviceName = "OffreService", portName = "OffrePort",
        targetNamespace = "http://soa.inpt.ma/offres")
public class OffreService {
    private static final Pattern ANNEE = Pattern.compile("(\\d+)");
    private final OffreDao dao = new OffreDao();

    @WebMethod
    @WebResult(name = "offre")
    public OffreStage creerOffre(@WebParam(name = "offre") OffreStage offre) throws ServiceException {
        valider(offre);
        if (offre.getDateLimite().isBefore(LocalDate.now())) {
            throw ServiceException.validation("La date limite de candidature est déjà passée.");
        }
        offre.setStatut("ACTIVE");
        return consulterOffre(dao.inserer(offre));
    }

    @WebMethod
    @WebResult(name = "offre")
    public OffreStage consulterOffre(@WebParam(name = "idOffre") Integer idOffre) throws ServiceException {
        OffreStage offre = dao.trouver(identifiant(idOffre));
        if (offre == null) {
            throw ServiceException.introuvable("Aucune offre avec l'identifiant " + idOffre + ".");
        }
        return offre;
    }

    @WebMethod
    @WebResult(name = "offre")
    public List<OffreStage> listerOffres() throws ServiceException {
        return dao.lister();
    }

    @WebMethod
    @WebResult(name = "offre")
    public OffreStage modifierOffre(@WebParam(name = "offre") OffreStage offre) throws ServiceException {
        valider(offre);
        identifiant(offre.getIdOffre());
        offre.setStatut(Validation.parmi(offre.getStatut(), "statut", "ACTIVE", "CLOTUREE"));
        consulterOffre(offre.getIdOffre());
        dao.modifier(offre);
        return consulterOffre(offre.getIdOffre());
    }

    @WebMethod
    public void supprimerOffre(@WebParam(name = "idOffre") Integer idOffre) throws ServiceException {
        if (!dao.supprimer(identifiant(idOffre))) {
            throw ServiceException.introuvable("Aucune offre avec l'identifiant " + idOffre + ".");
        }
    }

    @WebMethod
    @WebResult(name = "disponibilite")
    public DisponibiliteOffre verifierDisponibilite(@WebParam(name = "idOffre") Integer idOffre)
            throws ServiceException {
        DisponibiliteOffre resultat = new DisponibiliteOffre();
        OffreStage offre = dao.trouver(identifiant(idOffre));
        if (offre == null) {
            resultat.setMotif("Offre inconnue.");
            return resultat;
        }
        resultat.setExiste(true);
        resultat.setIdEntreprise(offre.getIdEntreprise());
        resultat.setTitre(offre.getTitre());
        resultat.setDureeMois(offre.getDureeMois());
        resultat.setStatut(offre.getStatut());
        resultat.setDateLimite(offre.getDateLimite());
        resultat.setPlacesDisponibles(offre.getPlacesDisponibles());

        List<String> motifs = new ArrayList<>();
        if (!"ACTIVE".equals(offre.getStatut())) {
            motifs.add("L'offre n'est plus active.");
        }
        if (offre.getDateLimite().isBefore(LocalDate.now())) {
            motifs.add("La date limite de candidature est dépassée.");
        }
        if (offre.getPlacesDisponibles() <= 0) {
            motifs.add("Plus aucune place disponible.");
        }
        resultat.setDisponible(motifs.isEmpty());
        resultat.setMotif(motifs.isEmpty() ? null : String.join(" ", motifs));
        return resultat;
    }

    /**
     * Compare le profil de l'étudiant aux filières ciblées et au niveau minimum de l'offre.
     * Le niveau est l'année présente dans le libellé : « INE 2 » vaut 2.
     */
    @WebMethod
    @WebResult(name = "compatibilite")
    public CompatibiliteOffre verifierCompatibilite(@WebParam(name = "idOffre") Integer idOffre,
            @WebParam(name = "filiere") String filiere,
            @WebParam(name = "niveauEtudes") String niveauEtudes) throws ServiceException {
        OffreStage offre = consulterOffre(idOffre);
        String filiereEtudiant = Validation.texte(filiere, "filiere", 120);
        String niveau = Validation.texte(niveauEtudes, "niveauEtudes", 30);
        if (offre.getFilieresCibles() != null && !offre.getFilieresCibles().isBlank()) {
            boolean trouvee = false;
            for (String cible : offre.getFilieresCibles().split(",")) {
                trouvee |= cible.trim().equalsIgnoreCase(filiereEtudiant);
            }
            if (!trouvee) {
                return new CompatibiliteOffre(false,
                        "La filière " + filiereEtudiant + " ne correspond pas à l'offre (" + offre.getFilieresCibles() + ").");
            }
        }
        if (offre.getNiveauMinimum() != null) {
            Matcher annee = ANNEE.matcher(niveau);
            if (!annee.find() || Integer.parseInt(annee.group(1)) < offre.getNiveauMinimum()) {
                return new CompatibiliteOffre(false,
                        "Le niveau " + niveau + " est inférieur au minimum requis (année " + offre.getNiveauMinimum() + ").");
            }
        }
        return new CompatibiliteOffre(true, null);
    }

    /** Décrémente le nombre de places de façon atomique. Renvoie false s'il n'en reste plus. */
    @WebMethod
    @WebResult(name = "reservee")
    public boolean reserverPlace(@WebParam(name = "idOffre") Integer idOffre) throws ServiceException {
        consulterOffre(idOffre);
        return dao.reserverPlace(idOffre);
    }

    /** Opération de compensation de reserverPlace. */
    @WebMethod
    public void libererPlace(@WebParam(name = "idOffre") Integer idOffre) throws ServiceException {
        consulterOffre(idOffre);
        dao.libererPlace(idOffre);
    }

    private static int identifiant(Integer id) throws ServiceException {
        return Validation.entier(id, "idOffre", 1, Integer.MAX_VALUE);
    }

    private static void valider(OffreStage o) throws ServiceException {
        if (o == null) {
            throw ServiceException.validation("L'offre est obligatoire.");
        }
        Validation.entier(o.getIdEntreprise(), "idEntreprise", 1, Integer.MAX_VALUE);
        o.setTitre(Validation.texte(o.getTitre(), "titre", 200));
        o.setDescription(Validation.texte(o.getDescription(), "description", 5000));
        o.setCompetencesRequises(Validation.texte(o.getCompetencesRequises(), "competencesRequises", 500));
        Validation.entier(o.getDureeMois(), "dureeMois", 1, 12);
        Validation.date(o.getDateLimite(), "dateLimite");
        Validation.entier(o.getPlacesDisponibles(), "placesDisponibles", 0, 1000);
        o.setFilieresCibles(Validation.texteOptionnel(o.getFilieresCibles(), "filieresCibles", 500));
        if (o.getNiveauMinimum() != null) {
            Validation.entier(o.getNiveauMinimum(), "niveauMinimum", 1, 10);
        }
    }
}
