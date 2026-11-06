package ma.inpt.soa.offres;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.OffreStage;

class OffreDao {
    private static final String SELECT = "SELECT o.*, e.nom AS nom_entreprise FROM Offre_Stage o"
            + " JOIN Entreprise e ON e.id_entreprise = o.id_entreprise";
    private static final String REFERENCE =
            "Opération impossible : entreprise inconnue, ou offre liée à des candidatures.";

    int inserer(OffreStage o) throws ServiceException {
        String sql = "INSERT INTO Offre_Stage (id_entreprise, titre, description, competences_requises, duree_mois,"
                + " date_limite, places_disponibles, statut, filieres_cibles, niveau_minimum)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, o);
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, REFERENCE, REFERENCE);
        }
    }

    OffreStage trouver(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(SELECT + " WHERE o.id_offre = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OffreStage.depuisLigne(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<OffreStage> lister() throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY o.date_limite");
                ResultSet rs = ps.executeQuery()) {
            List<OffreStage> liste = new ArrayList<>();
            while (rs.next()) {
                liste.add(OffreStage.depuisLigne(rs));
            }
            return liste;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    void modifier(OffreStage o) throws ServiceException {
        String sql = "UPDATE Offre_Stage SET id_entreprise = ?, titre = ?, description = ?, competences_requises = ?,"
                + " duree_mois = ?, date_limite = ?, places_disponibles = ?, statut = ?, filieres_cibles = ?,"
                + " niveau_minimum = ? WHERE id_offre = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, o);
            ps.setInt(11, o.getIdOffre());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, REFERENCE, REFERENCE);
        }
    }

    boolean supprimer(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM Offre_Stage WHERE id_offre = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, REFERENCE, REFERENCE);
        }
    }

    boolean reserverPlace(int id) throws ServiceException {
        return mettreAJourPlaces(
                "UPDATE Offre_Stage SET places_disponibles = places_disponibles - 1"
                        + " WHERE id_offre = ? AND places_disponibles > 0", id);
    }

    void libererPlace(int id) throws ServiceException {
        mettreAJourPlaces("UPDATE Offre_Stage SET places_disponibles = places_disponibles + 1 WHERE id_offre = ?", id);
    }

    private boolean mettreAJourPlaces(String sql, int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    private static void remplir(PreparedStatement ps, OffreStage o) throws SQLException {
        ps.setInt(1, o.getIdEntreprise());
        ps.setString(2, o.getTitre());
        ps.setString(3, o.getDescription());
        ps.setString(4, o.getCompetencesRequises());
        ps.setInt(5, o.getDureeMois());
        ps.setDate(6, Date.valueOf(o.getDateLimite()));
        ps.setInt(7, o.getPlacesDisponibles());
        ps.setString(8, o.getStatut());
        ps.setString(9, o.getFilieresCibles());
        if (o.getNiveauMinimum() == null) {
            ps.setNull(10, Types.INTEGER);
        } else {
            ps.setInt(10, o.getNiveauMinimum());
        }
    }
}
