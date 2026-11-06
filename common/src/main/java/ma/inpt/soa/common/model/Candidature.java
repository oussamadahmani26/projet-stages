package ma.inpt.soa.common.model;

import java.time.LocalDateTime;

public class Candidature {
    private Integer idCandidature;
    private Integer idEtudiant;
    private Integer idOffre;
    private String lettreMotivation;
    private LocalDateTime dateDepot;
    private LocalDateTime dateMaj;
    /** DEPOSEE, EN_COURS_EVALUATION, ACCEPTEE, REFUSEE ou ANNULEE. */
    private String statut;
    private String motif;
    // Champs de lecture, issus des jointures.
    private String matricule;
    private String nomEtudiant;
    private String titreOffre;
    private Integer idEntreprise;
    private String nomEntreprise;

    public Integer getIdCandidature() { return idCandidature; }
    public void setIdCandidature(Integer idCandidature) { this.idCandidature = idCandidature; }
    public Integer getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(Integer idEtudiant) { this.idEtudiant = idEtudiant; }
    public Integer getIdOffre() { return idOffre; }
    public void setIdOffre(Integer idOffre) { this.idOffre = idOffre; }
    public String getLettreMotivation() { return lettreMotivation; }
    public void setLettreMotivation(String lettreMotivation) { this.lettreMotivation = lettreMotivation; }
    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }
    public LocalDateTime getDateMaj() { return dateMaj; }
    public void setDateMaj(LocalDateTime dateMaj) { this.dateMaj = dateMaj; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNomEtudiant() { return nomEtudiant; }
    public void setNomEtudiant(String nomEtudiant) { this.nomEtudiant = nomEtudiant; }
    public String getTitreOffre() { return titreOffre; }
    public void setTitreOffre(String titreOffre) { this.titreOffre = titreOffre; }
    public Integer getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(Integer idEntreprise) { this.idEntreprise = idEntreprise; }
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
}
