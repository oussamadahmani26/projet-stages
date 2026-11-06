package ma.inpt.soa.common.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Etudiant {
    private Integer idEtudiant;
    private String matricule;
    private String nom;
    private String filiere;
    private String niveauEtudes;
    private String email;
    private String telephone;
    /** ACTIF, SUSPENDU ou EN_STAGE. */
    private String statut;

    public Integer getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(Integer idEtudiant) { this.idEtudiant = idEtudiant; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    public String getNiveauEtudes() { return niveauEtudes; }
    public void setNiveauEtudes(String niveauEtudes) { this.niveauEtudes = niveauEtudes; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
