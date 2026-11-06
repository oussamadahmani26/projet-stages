package ma.inpt.soa.stages;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;
import ma.inpt.soa.common.model.Stage;

class StageDao {
    private static final String SELECT = "SELECT s.*, e.matricule, e.nom AS nom_etudiant, o.titre,"
            + " en.nom AS nom_entreprise FROM Stage s"
            + " JOIN Etudiant e ON e.id_etudiant = s.id_etudiant"
            + " JOIN Offre_Stage o ON o.id_offre = s.id_offre"
            + " JOIN Entreprise en ON en.id_entreprise = o.id_entreprise";

    /** Règle de gestion : un stage n'existe que pour une candidature acceptée, et une seule fois. */
    int creer(Stage s) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement candidature = c.prepareStatement(
                        "SELECT id_etudiant, id_offre, statut FROM Candidature WHERE id_candidature = ?")) {
            candidature.setInt(1, s.getIdCandidature());
            int idEtudiant;
            int idOffre;
            try (ResultSet rs = candidature.executeQuery()) {
                if (!rs.next()) {
                    throw ServiceException.introuvable("Aucune candidature avec l'identifiant " + s.getIdCandidature() + ".");
                }
                if (!"ACCEPTEE".equals(rs.getString("statut"))) {
                    throw ServiceException.conflit("Un stage ne peut être créé que pour une candidature acceptée.");
                }
                idEtudiant = rs.getInt("id_etudiant");
                idOffre = rs.getInt("id_offre");
            }
            String sql = "INSERT INTO Stage (id_candidature, id_etudiant, id_offre, date_debut, date_fin, statut)"
                    + " VALUES (?, ?, ?, ?, ?, 'PLANIFIE')";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, s.getIdCandidature());
                ps.setInt(2, idEtudiant);
                ps.setInt(3, idOffre);
                ps.setDate(4, Date.valueOf(s.getDateDebut()));
                ps.setDate(5, Date.valueOf(s.getDateFin()));
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    return cles.getInt(1);
                }
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex, "Un stage existe déjà pour cette candidature.", "Référence invalide.");
        }
    }

    Stage trouver(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(SELECT + " WHERE s.id_stage = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Stage> lister(Integer idEtudiant, Integer idCandidature, String statut) throws ServiceException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (idEtudiant != null) {
            sql.append(" AND s.id_etudiant = ?");
            params.add(idEtudiant);
        }
        if (idCandidature != null) {
            sql.append(" AND s.id_candidature = ?");
            params.add(idCandidature);
        }
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND s.statut = ?");
            params.add(statut);
        }
        sql.append(" ORDER BY s.date_debut DESC");
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Stage> liste = new ArrayList<>();
                while (rs.next()) {
                    liste.add(lire(rs));
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    void modifier(Stage s) throws ServiceException {
        String sql = "UPDATE Stage SET statut = ?, date_debut = ?, date_fin = ? WHERE id_stage = ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getStatut());
            ps.setDate(2, Date.valueOf(s.getDateDebut()));
            ps.setDate(3, Date.valueOf(s.getDateFin()));
            ps.setInt(4, s.getIdStage());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    void ajouterCompteRendu(CompteRendu cr) throws ServiceException {
        String sql = "INSERT INTO Compte_Rendu (id_stage, periode, contenu, auteur) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cr.getIdStage());
            ps.setString(2, cr.getPeriode());
            ps.setString(3, cr.getContenu());
            ps.setString(4, cr.getAuteur());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                cr.setIdCompteRendu(cles.getInt(1));
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<CompteRendu> comptesRendus(int idStage) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM Compte_Rendu WHERE id_stage = ? ORDER BY date_depot, id_compte_rendu")) {
            ps.setInt(1, idStage);
            try (ResultSet rs = ps.executeQuery()) {
                List<CompteRendu> liste = new ArrayList<>();
                while (rs.next()) {
                    CompteRendu cr = new CompteRendu();
                    cr.setIdCompteRendu(rs.getInt("id_compte_rendu"));
                    cr.setIdStage(rs.getInt("id_stage"));
                    cr.setPeriode(rs.getString("periode"));
                    cr.setContenu(rs.getString("contenu"));
                    cr.setAuteur(rs.getString("auteur"));
                    cr.setDateDepot(rs.getTimestamp("date_depot").toLocalDateTime());
                    liste.add(cr);
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    void ajouterEvaluation(Evaluation e) throws ServiceException {
        String sql = "INSERT INTO Evaluation (id_stage, note, commentaire, evaluateur) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getIdStage());
            ps.setBigDecimal(2, e.getNote());
            ps.setString(3, e.getCommentaire());
            ps.setString(4, e.getEvaluateur());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                e.setIdEvaluation(cles.getInt(1));
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Evaluation> evaluations(int idStage) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM Evaluation WHERE id_stage = ? ORDER BY date_depot, id_evaluation")) {
            ps.setInt(1, idStage);
            try (ResultSet rs = ps.executeQuery()) {
                List<Evaluation> liste = new ArrayList<>();
                while (rs.next()) {
                    Evaluation e = new Evaluation();
                    e.setIdEvaluation(rs.getInt("id_evaluation"));
                    e.setIdStage(rs.getInt("id_stage"));
                    e.setNote(rs.getBigDecimal("note"));
                    e.setCommentaire(rs.getString("commentaire"));
                    e.setEvaluateur(rs.getString("evaluateur"));
                    e.setDateDepot(rs.getTimestamp("date_depot").toLocalDateTime());
                    liste.add(e);
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    private static Stage lire(ResultSet rs) throws SQLException {
        Stage s = new Stage();
        s.setIdStage(rs.getInt("id_stage"));
        s.setIdCandidature(rs.getInt("id_candidature"));
        s.setIdEtudiant(rs.getInt("id_etudiant"));
        s.setIdOffre(rs.getInt("id_offre"));
        s.setDateDebut(rs.getDate("date_debut").toLocalDate());
        s.setDateFin(rs.getDate("date_fin").toLocalDate());
        s.setStatut(rs.getString("statut"));
        s.setMatricule(rs.getString("matricule"));
        s.setNomEtudiant(rs.getString("nom_etudiant"));
        s.setTitreOffre(rs.getString("titre"));
        s.setNomEntreprise(rs.getString("nom_entreprise"));
        return s;
    }
}
