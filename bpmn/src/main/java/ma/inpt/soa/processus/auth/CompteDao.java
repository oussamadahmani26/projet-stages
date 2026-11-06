package ma.inpt.soa.processus.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;

class CompteDao {
    /** Compte lu en base, avec son empreinte de mot de passe. */
    static final class Compte {
        final Session session = new Session();
        String empreinte;
    }

    void creer(String identifiant, String empreinte, String role, String matricule, Integer idEntreprise)
            throws ServiceException {
        String sql = "INSERT INTO Compte (identifiant, mot_de_passe_hash, role, matricule, id_entreprise) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identifiant);
            ps.setString(2, empreinte);
            ps.setString(3, role);
            ps.setString(4, matricule);
            if (idEntreprise == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, idEntreprise);
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, "Un compte existe déjà avec cet identifiant.", "Référence de compte invalide.");
        }
    }

    Compte trouver(String identifiant) throws ServiceException {
        String sql = "SELECT c.identifiant, c.mot_de_passe_hash, c.role, c.matricule, c.id_entreprise,"
                + " e.id_etudiant, e.nom AS nom_etudiant, en.nom AS nom_entreprise FROM Compte c"
                + " LEFT JOIN Etudiant e ON e.matricule = c.matricule"
                + " LEFT JOIN Entreprise en ON en.id_entreprise = c.id_entreprise"
                + " WHERE c.identifiant = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identifiant);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Compte compte = new Compte();
                compte.empreinte = rs.getString("mot_de_passe_hash");
                Session s = compte.session;
                s.setIdentifiant(rs.getString("identifiant"));
                s.setRole(rs.getString("role"));
                if (s.estEtudiant()) {
                    s.setMatricule(rs.getString("matricule"));
                    s.setIdEtudiant(rs.getInt("id_etudiant"));
                    s.setNom(rs.getString("nom_etudiant"));
                } else if (s.estEntreprise()) {
                    s.setIdEntreprise(rs.getInt("id_entreprise"));
                    s.setNom(rs.getString("nom_entreprise"));
                } else {
                    s.setNom("Responsable des stages");
                }
                return compte;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }
}
