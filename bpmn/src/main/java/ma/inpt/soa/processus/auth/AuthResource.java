package ma.inpt.soa.processus.auth;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import ma.inpt.soa.common.config.TlsSupport;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.util.Validation;
import ma.inpt.soa.processus.clients.Services;

/** Connexion à l'interface. La session est portée par un cookie inaccessible au JavaScript. */
@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class.getName());
    private static final int LONGUEUR_MINIMALE = 8;
    private static final CompteDao COMPTES = new CompteDao();
    private static final LimiteTentatives LIMITE = new LimiteTentatives();

    @Context
    private ContainerRequestContext requete;

    /** Corps : {"identifiant": "...", "motDePasse": "..."}. */
    @POST
    @Path("connexion")
    public Response connexion(Map<String, String> corps) throws ServiceException {
        String identifiant = Validation.texte(corps == null ? null : corps.get("identifiant"), "identifiant", 160);
        String motDePasse = Validation.texte(corps.get("motDePasse"), "motDePasse", 200);
        LIMITE.verifier(identifiant);
        CompteDao.Compte compte = COMPTES.trouver(identifiant);
        boolean valide = MotDePasse.verifier(motDePasse, compte == null ? MotDePasse.LEURRE : compte.empreinte);
        if (compte == null || !valide) {
            LIMITE.echec(identifiant);
            throw new ServiceException(ServiceException.NON_AUTHENTIFIE, "Identifiant ou mot de passe incorrect.");
        }
        LIMITE.succes(identifiant);
        return Response.ok(compte.session)
                .header("Set-Cookie", cookie(Sessions.ouvrir(compte.session), Sessions.DUREE.getSeconds()))
                .build();
    }

    /**
     * Crée un étudiant ou une entreprise, puis son compte, et ouvre la session.
     * Seuls les champs listés sont lus : le statut et le rôle responsable ne peuvent pas être choisis.
     */
    @POST
    @Path("inscription")
    public Response inscription(Map<String, String> corps) throws ServiceException {
        if (corps == null) {
            throw ServiceException.validation("Le corps de la requête est obligatoire.");
        }
        String role = Validation.parmi(corps.get("role"), "role", Session.ETUDIANT, Session.ENTREPRISE);
        String motDePasse = corps.get("motDePasse");
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MINIMALE || motDePasse.length() > 200) {
            throw ServiceException.validation(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MINIMALE + " caractères.");
        }
        String identifiant = Session.ETUDIANT.equals(role)
                ? Validation.texte(corps.get("matricule"), "matricule", 30)
                : Validation.email(corps.get("email"), "email");
        if (COMPTES.trouver(identifiant) != null) {
            throw ServiceException.conflit("Un compte existe déjà avec cet identifiant.");
        }
        String empreinte = MotDePasse.hacher(motDePasse);
        if (Session.ETUDIANT.equals(role)) {
            inscrireEtudiant(identifiant, corps, empreinte);
        } else {
            inscrireEntreprise(identifiant, corps, empreinte);
        }
        CompteDao.Compte compte = COMPTES.trouver(identifiant);
        return Response.status(Response.Status.CREATED).entity(compte.session)
                .header("Set-Cookie", cookie(Sessions.ouvrir(compte.session), Sessions.DUREE.getSeconds()))
                .build();
    }

    private static void inscrireEtudiant(String matricule, Map<String, String> corps, String empreinte)
            throws ServiceException {
        Map<String, String> etudiant = new HashMap<>();
        etudiant.put("matricule", matricule);
        etudiant.put("nom", corps.get("nom"));
        etudiant.put("filiere", corps.get("filiere"));
        etudiant.put("niveauEtudes", corps.get("niveauEtudes"));
        etudiant.put("email", corps.get("email"));
        etudiant.put("telephone", corps.get("telephone"));
        Services.etudiants().appeler("ajouterEtudiant", Map.of("etudiant", etudiant));
        try {
            COMPTES.creer(matricule, empreinte, Session.ETUDIANT, matricule, null);
        } catch (ServiceException e) {
            annulerCreation(() -> Services.etudiants().appeler("supprimerEtudiant", Map.of("matricule", matricule)));
            throw e;
        }
    }

    private static void inscrireEntreprise(String email, Map<String, String> corps, String empreinte)
            throws ServiceException {
        Map<String, String> entreprise = new HashMap<>();
        entreprise.put("nom", corps.get("nom"));
        entreprise.put("secteurActivite", corps.get("secteurActivite"));
        entreprise.put("email", email);
        entreprise.put("telephone", corps.get("telephone"));
        entreprise.put("responsableContact", corps.get("responsableContact"));
        int id = Integer.parseInt(Services.entreprises()
                .appelerUn("ajouterEntreprise", Map.of("entreprise", entreprise)).get("idEntreprise"));
        try {
            COMPTES.creer(email, empreinte, Session.ENTREPRISE, null, id);
        } catch (ServiceException e) {
            annulerCreation(() -> Services.entreprises().appeler("supprimerEntreprise", Map.of("idEntreprise", id)));
            throw e;
        }
    }

    private interface Action {
        void executer() throws ServiceException;
    }

    /** Sans compte, la fiche créée serait orpheline et bloquerait une nouvelle inscription. */
    private static void annulerCreation(Action suppression) {
        try {
            suppression.executer();
        } catch (ServiceException e) {
            LOG.warning("Fiche orpheline après une inscription échouée : " + e.getMessage());
        }
    }

    @POST
    @Path("deconnexion")
    public Response deconnexion(@CookieParam(AuthFilter.COOKIE) String jeton) {
        Sessions.fermer(jeton);
        return Response.noContent().header("Set-Cookie", cookie("", 0)).build();
    }

    @GET
    @Path("moi")
    public Session moi() {
        return Acces.session(requete);
    }

    private static String cookie(String valeur, long duree) {
        return AuthFilter.COOKIE + "=" + valeur + "; Path=/; Max-Age=" + duree + "; HttpOnly; SameSite=Strict"
                + (TlsSupport.actif() ? "; Secure" : "");
    }
}
