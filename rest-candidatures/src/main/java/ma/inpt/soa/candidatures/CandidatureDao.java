package ma.inpt.soa.candidatures;

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
import ma.inpt.soa.common.model.Candidature;

class CandidatureDao {
    private static final String SELECT = "SELECT c.*, e.matricule, e.nom AS nom_etudiant, o.titre, o.id_entreprise,"
            + " en.nom AS nom_entreprise FROM Candidature c"
            + " JOIN Etudiant e ON e.id_etudiant = c.id_etudiant"
            + " JOIN Offre_Stage o ON o.id_offre = c.id_offre"
            + " JOIN Entreprise en ON en.id_entreprise = o.id_entreprise";
    private static final int DOUBLON = 1062;
    private static final int REFERENCE_ABSENTE = 1452;

    int deposer(Candidature c) throws ServiceException {
        String sql = "INSERT INTO Candidature (id_etudiant, id_offre, lettre_motivation, statut)"
                + " VALUES (?, ?, ?, 'DEPOSEE')";
        try (Connection cx = DatabaseConnection.getConnection();
                PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getIdEtudiant());
            ps.setInt(2, c.getIdOffre());
            ps.setString(3, c.getLettreMotivation());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == DOUBLON) {
                return redeposer(c);
            }
            if (ex.getErrorCode() == REFERENCE_ABSENTE) {
                throw ServiceException.introuvable("Étudiant ou offre introuvable.");
            }
            throw SqlErrors.traduire(ex);
        }
    }

    /** Réactive une candidature annulée ; toute autre candidature existante constitue un doublon. */
    private int redeposer(Candidature c) throws ServiceException {
        String sql = "UPDATE Candidature SET statut = 'DEPOSEE', motif = NULL, lettre_motivation = ?,"
                + " date_depot = CURRENT_TIMESTAMP WHERE id_etudiant = ? AND id_offre = ? AND statut = 'ANNULEE'";
        try (Connection cx = DatabaseConnection.getConnection(); PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, c.getLettreMotivation());
            ps.setInt(2, c.getIdEtudiant());
            ps.setInt(3, c.getIdOffre());
            if (ps.executeUpdate() == 0) {
                throw ServiceException.conflit("Cet étudiant a déjà une candidature pour cette offre.");
            }
            try (PreparedStatement id = cx.prepareStatement(
                    "SELECT id_candidature FROM Candidature WHERE id_etudiant = ? AND id_offre = ?")) {
                id.setInt(1, c.getIdEtudiant());
                id.setInt(2, c.getIdOffre());
                try (ResultSet rs = id.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    Candidature trouver(int id) throws ServiceException {
        try (Connection cx = DatabaseConnection.getConnection();
                PreparedStatement ps = cx.prepareStatement(SELECT + " WHERE c.id_candidature = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Candidature> lister(Integer idEtudiant, Integer idOffre, Integer idEntreprise, String statut)
            throws ServiceException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (idEtudiant != null) {
            sql.append(" AND c.id_etudiant = ?");
            params.add(idEtudiant);
        }
        if (idOffre != null) {
            sql.append(" AND c.id_offre = ?");
            params.add(idOffre);
        }
        if (idEntreprise != null) {
            sql.append(" AND o.id_entreprise = ?");
            params.add(idEntreprise);
        }
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND c.statut = ?");
            params.add(statut);
        }
        sql.append(" ORDER BY c.date_depot DESC, c.id_candidature DESC");
        try (Connection cx = DatabaseConnection.getConnection();
                PreparedStatement ps = cx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Candidature> liste = new ArrayList<>();
                while (rs.next()) {
                    liste.add(lire(rs));
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    /** Mise à jour conditionnée au statut lu : deux décisions concurrentes ne peuvent pas s'écraser. */
    boolean changerStatut(int id, String attendu, String nouveau, String motif) throws ServiceException {
        String sql = "UPDATE Candidature SET statut = ?, motif = ? WHERE id_candidature = ? AND statut = ?";
        try (Connection cx = DatabaseConnection.getConnection(); PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, nouveau);
            ps.setString(2, motif);
            ps.setInt(3, id);
            ps.setString(4, attendu);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    private static Candidature lire(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setIdCandidature(rs.getInt("id_candidature"));
        c.setIdEtudiant(rs.getInt("id_etudiant"));
        c.setIdOffre(rs.getInt("id_offre"));
        c.setLettreMotivation(rs.getString("lettre_motivation"));
        c.setDateDepot(rs.getTimestamp("date_depot").toLocalDateTime());
        c.setDateMaj(rs.getTimestamp("date_maj").toLocalDateTime());
        c.setStatut(rs.getString("statut"));
        c.setMotif(rs.getString("motif"));
        c.setMatricule(rs.getString("matricule"));
        c.setNomEtudiant(rs.getString("nom_etudiant"));
        c.setTitreOffre(rs.getString("titre"));
        c.setIdEntreprise(rs.getInt("id_entreprise"));
        c.setNomEntreprise(rs.getString("nom_entreprise"));
        return c;
    }
}
