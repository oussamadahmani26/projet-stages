package ma.inpt.soa.entreprises;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Entreprise;
import ma.inpt.soa.common.model.OffreStage;

class EntrepriseDao {
    private static final String DOUBLON = "Une entreprise porte déjà ce nom.";
    private static final String REFERENCE = "Suppression impossible : cette entreprise a des offres de stage.";

    int inserer(Entreprise e) throws ServiceException {
        String sql = "INSERT INTO Entreprise (nom, secteur_activite, email, telephone, responsable_contact)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, e);
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    Entreprise trouver(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM Entreprise WHERE id_entreprise = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Entreprise> lister() throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM Entreprise ORDER BY nom");
                ResultSet rs = ps.executeQuery()) {
            List<Entreprise> liste = new ArrayList<>();
            while (rs.next()) {
                liste.add(lire(rs));
            }
            return liste;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    void modifier(Entreprise e) throws ServiceException {
        String sql = "UPDATE Entreprise SET nom = ?, secteur_activite = ?, email = ?, telephone = ?,"
                + " responsable_contact = ? WHERE id_entreprise = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, e);
            ps.setInt(6, e.getIdEntreprise());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    boolean supprimer(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM Entreprise WHERE id_entreprise = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    List<OffreStage> offres(int idEntreprise) throws ServiceException {
        String sql = "SELECT o.*, e.nom AS nom_entreprise FROM Offre_Stage o"
                + " JOIN Entreprise e ON e.id_entreprise = o.id_entreprise"
                + " WHERE o.id_entreprise = ? ORDER BY o.date_limite";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEntreprise);
            try (ResultSet rs = ps.executeQuery()) {
                List<OffreStage> liste = new ArrayList<>();
                while (rs.next()) {
                    liste.add(OffreStage.depuisLigne(rs));
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    private static void remplir(PreparedStatement ps, Entreprise e) throws SQLException {
        ps.setString(1, e.getNom());
        ps.setString(2, e.getSecteurActivite());
        ps.setString(3, e.getEmail());
        ps.setString(4, e.getTelephone());
        ps.setString(5, e.getResponsableContact());
    }

    private static Entreprise lire(ResultSet rs) throws SQLException {
        Entreprise e = new Entreprise();
        e.setIdEntreprise(rs.getInt("id_entreprise"));
        e.setNom(rs.getString("nom"));
        e.setSecteurActivite(rs.getString("secteur_activite"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setResponsableContact(rs.getString("responsable_contact"));
        return e;
    }
}
