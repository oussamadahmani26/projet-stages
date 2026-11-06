package ma.inpt.soa.common.model;

import java.time.LocalDate;

public class Stage {
    private Integer idStage;
    private Integer idCandidature;
    private Integer idEtudiant;
    private Integer idOffre;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    /** PLANIFIE, EN_COURS, TERMINE ou INTERROMPU. */
    private String statut;
    // Champs de lecture, issus des jointures.
    private String matricule;
    private String nomEtudiant;
    private String titreOffre;
    private String nomEntreprise;

    public Integer getIdStage() { return idStage; }
    public void setIdStage(Integer idStage) { this.idStage = idStage; }
    public Integer getIdCandidature() { return idCandidature; }
    public void setIdCandidature(Integer idCandidature) { this.idCandidature = idCandidature; }
    public Integer getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(Integer idEtudiant) { this.idEtudiant = idEtudiant; }
    public Integer getIdOffre() { return idOffre; }
    public void setIdOffre(Integer idOffre) { this.idOffre = idOffre; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNomEtudiant() { return nomEtudiant; }
    public void setNomEtudiant(String nomEtudiant) { this.nomEtudiant = nomEtudiant; }
    public String getTitreOffre() { return titreOffre; }
    public void setTitreOffre(String titreOffre) { this.titreOffre = titreOffre; }
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
}
