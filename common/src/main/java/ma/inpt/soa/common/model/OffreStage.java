package ma.inpt.soa.common.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@XmlAccessorType(XmlAccessType.FIELD)
public class OffreStage {
    private Integer idOffre;
    private Integer idEntreprise;
    private String nomEntreprise;
    private String titre;
    private String description;
    private String competencesRequises;
    private Integer dureeMois;
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate dateLimite;
    private Integer placesDisponibles;
    /** ACTIVE ou CLOTUREE. */
    private String statut;
    /** Filières acceptées, séparées par des virgules. Vide : offre ouverte à toutes les filières. */
    private String filieresCibles;
    /** Année d'études minimale (2 accepte INE 2 et INE 3). Vide : aucun minimum. */
    private Integer niveauMinimum;

    /** Lit une ligne de Offre_Stage ; la requête doit fournir nom_entreprise. */
    public static OffreStage depuisLigne(ResultSet rs) throws SQLException {
        OffreStage o = new OffreStage();
        o.idOffre = rs.getInt("id_offre");
        o.idEntreprise = rs.getInt("id_entreprise");
        o.nomEntreprise = rs.getString("nom_entreprise");
        o.titre = rs.getString("titre");
        o.description = rs.getString("description");
        o.competencesRequises = rs.getString("competences_requises");
        o.dureeMois = rs.getInt("duree_mois");
        o.dateLimite = rs.getDate("date_limite").toLocalDate();
        o.placesDisponibles = rs.getInt("places_disponibles");
        o.statut = rs.getString("statut");
        o.filieresCibles = rs.getString("filieres_cibles");
        int niveau = rs.getInt("niveau_minimum");
        o.niveauMinimum = rs.wasNull() ? null : niveau;
        return o;
    }

    public Integer getIdOffre() { return idOffre; }
    public void setIdOffre(Integer idOffre) { this.idOffre = idOffre; }
    public Integer getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(Integer idEntreprise) { this.idEntreprise = idEntreprise; }
    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCompetencesRequises() { return competencesRequises; }
    public void setCompetencesRequises(String competencesRequises) { this.competencesRequises = competencesRequises; }
    public Integer getDureeMois() { return dureeMois; }
    public void setDureeMois(Integer dureeMois) { this.dureeMois = dureeMois; }
    public LocalDate getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }
    public Integer getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(Integer placesDisponibles) { this.placesDisponibles = placesDisponibles; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getFilieresCibles() { return filieresCibles; }
    public void setFilieresCibles(String filieresCibles) { this.filieresCibles = filieresCibles; }
    public Integer getNiveauMinimum() { return niveauMinimum; }
    public void setNiveauMinimum(Integer niveauMinimum) { this.niveauMinimum = niveauMinimum; }
}
