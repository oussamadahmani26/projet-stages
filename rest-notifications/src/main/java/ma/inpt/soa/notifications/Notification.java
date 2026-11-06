package ma.inpt.soa.notifications;

import java.time.LocalDateTime;

public class Notification {
    private Integer idNotification;
    private String type;
    private String destinataireType;
    private Integer destinataireId;
    private String destinataireEmail;
    private String sujet;
    private String message;
    private Integer idCandidature;
    private Integer idOffre;
    private String statutEnvoi;
    private LocalDateTime dateEnvoi;

    public Integer getIdNotification() { return idNotification; }
    public void setIdNotification(Integer idNotification) { this.idNotification = idNotification; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDestinataireType() { return destinataireType; }
    public void setDestinataireType(String destinataireType) { this.destinataireType = destinataireType; }
    public Integer getDestinataireId() { return destinataireId; }
    public void setDestinataireId(Integer destinataireId) { this.destinataireId = destinataireId; }
    public String getDestinataireEmail() { return destinataireEmail; }
    public void setDestinataireEmail(String destinataireEmail) { this.destinataireEmail = destinataireEmail; }
    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getIdCandidature() { return idCandidature; }
    public void setIdCandidature(Integer idCandidature) { this.idCandidature = idCandidature; }
    public Integer getIdOffre() { return idOffre; }
    public void setIdOffre(Integer idOffre) { this.idOffre = idOffre; }
    public String getStatutEnvoi() { return statutEnvoi; }
    public void setStatutEnvoi(String statutEnvoi) { this.statutEnvoi = statutEnvoi; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}
