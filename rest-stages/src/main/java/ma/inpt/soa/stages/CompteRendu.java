package ma.inpt.soa.stages;

import java.time.LocalDateTime;

public class CompteRendu {
    private Integer idCompteRendu;
    private Integer idStage;
    private String periode;
    private String contenu;
    private String auteur;
    private LocalDateTime dateDepot;

    public Integer getIdCompteRendu() { return idCompteRendu; }
    public void setIdCompteRendu(Integer idCompteRendu) { this.idCompteRendu = idCompteRendu; }
    public Integer getIdStage() { return idStage; }
    public void setIdStage(Integer idStage) { this.idStage = idStage; }
    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }
    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }
}
