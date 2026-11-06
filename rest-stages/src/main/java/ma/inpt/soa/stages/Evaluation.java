package ma.inpt.soa.stages;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Evaluation {
    private Integer idEvaluation;
    private Integer idStage;
    private BigDecimal note;
    private String commentaire;
    private String evaluateur;
    private LocalDateTime dateDepot;

    public Integer getIdEvaluation() { return idEvaluation; }
    public void setIdEvaluation(Integer idEvaluation) { this.idEvaluation = idEvaluation; }
    public Integer getIdStage() { return idStage; }
    public void setIdStage(Integer idStage) { this.idStage = idStage; }
    public BigDecimal getNote() { return note; }
    public void setNote(BigDecimal note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public String getEvaluateur() { return evaluateur; }
    public void setEvaluateur(String evaluateur) { this.evaluateur = evaluateur; }
    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }
}
