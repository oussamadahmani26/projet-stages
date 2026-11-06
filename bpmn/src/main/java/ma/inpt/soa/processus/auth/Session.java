package ma.inpt.soa.processus.auth;

import java.time.Instant;

/** Utilisateur connecté. idEtudiant et matricule, ou idEntreprise, sont renseignés selon le rôle. */
public class Session {
    public static final String ETUDIANT = "ETUDIANT";
    public static final String ENTREPRISE = "ENTREPRISE";
    public static final String RESPONSABLE = "RESPONSABLE";

    private String identifiant;
    private String role;
    private String nom;
    private String matricule;
    private Integer idEtudiant;
    private Integer idEntreprise;
    private transient Instant expiration;

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public Integer getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(Integer idEtudiant) { this.idEtudiant = idEtudiant; }
    public Integer getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(Integer idEntreprise) { this.idEntreprise = idEntreprise; }

    Instant expiration() { return expiration; }
    void expireA(Instant instant) { expiration = instant; }

    public boolean estEtudiant() { return ETUDIANT.equals(role); }
    public boolean estEntreprise() { return ENTREPRISE.equals(role); }
    public boolean estResponsable() { return RESPONSABLE.equals(role); }
}
