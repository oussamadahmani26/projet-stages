package ma.inpt.soa.offres;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import ma.inpt.soa.common.model.LocalDateAdapter;

/** Résultat de verifierDisponibilite : l'offre existe, est active, dans les délais et a des places. */
@XmlAccessorType(XmlAccessType.FIELD)
public class DisponibiliteOffre {
    private boolean existe;
    private boolean disponible;
    private String motif;
    private Integer idEntreprise;
    private String titre;
    private Integer dureeMois;
    private String statut;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate dateLimite;
    private Integer placesDisponibles;

    public boolean isExiste() { return existe; }
    public void setExiste(boolean existe) { this.existe = existe; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public Integer getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(Integer idEntreprise) { this.idEntreprise = idEntreprise; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public Integer getDureeMois() { return dureeMois; }
    public void setDureeMois(Integer dureeMois) { this.dureeMois = dureeMois; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }
    public Integer getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(Integer placesDisponibles) { this.placesDisponibles = placesDisponibles; }
}
