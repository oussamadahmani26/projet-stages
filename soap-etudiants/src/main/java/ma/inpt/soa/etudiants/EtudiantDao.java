package ma.inpt.soa.etudiants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Etudiant;

class EtudiantDao {
    private static final String DOUBLON = "Ce matricule ou cet e-mail est déjà utilisé.";
    private static final String REFERENCE = "Suppression impossible : cet étudiant a des candidatures ou des stages.";

    void inserer(Etudiant e) throws ServiceException {
        String sql = "INSERT INTO Etudiant (matricule, nom, filiere, niveau_etudes, email, telephone, statut)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getMatricule());
            ps.setString(2, e.getNom());
            ps.setString(3, e.getFiliere());
            ps.setString(4, e.getNiveauEtudes());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getTelephone());
            ps.setString(7, e.getStatut());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    Etudiant trouver(String matricule) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM Etudiant WHERE matricule = ?")) {
            ps.setString(1, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Etudiant> lister() throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM Etudiant ORDER BY nom");
                ResultSet rs = ps.executeQuery()) {
            List<Etudiant> liste = new ArrayList<>();
            while (rs.next()) {
                liste.add(lire(rs));
            }
            return liste;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    boolean modifier(Etudiant e) throws ServiceException {
        String sql = "UPDATE Etudiant SET nom = ?, filiere = ?, niveau_etudes = ?, email = ?, telephone = ?"
                + " WHERE matricule = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getFiliere());
            ps.setString(3, e.getNiveauEtudes());
            ps.setString(4, e.getEmail());
            ps.setString(5, e.getTelephone());
            ps.setString(6, e.getMatricule());
            return lignesTrouvees(ps, e.getMatricule(), c);
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    boolean changerStatut(String matricule, String statut) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("UPDATE Etudiant SET statut = ? WHERE matricule = ?")) {
            ps.setString(1, statut);
            ps.setString(2, matricule);
            return lignesTrouvees(ps, matricule, c);
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    boolean supprimer(String matricule) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM Etudiant WHERE matricule = ?")) {
            ps.setString(1, matricule);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, DOUBLON, REFERENCE);
        }
    }

    /** Un UPDATE sans changement renvoie 0 : on distingue ce cas d'une ligne réellement absente. */
    private boolean lignesTrouvees(PreparedStatement ps, String matricule, Connection c) throws SQLException {
        if (ps.executeUpdate() > 0) {
            return true;
        }
        try (PreparedStatement existe = c.prepareStatement("SELECT 1 FROM Etudiant WHERE matricule = ?")) {
            existe.setString(1, matricule);
            try (ResultSet rs = existe.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Etudiant lire(ResultSet rs) throws SQLException {
        Etudiant e = new Etudiant();
        e.setIdEtudiant(rs.getInt("id_etudiant"));
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setFiliere(rs.getString("filiere"));
        e.setNiveauEtudes(rs.getString("niveau_etudes"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setStatut(rs.getString("statut"));
        return e;
    }
}
