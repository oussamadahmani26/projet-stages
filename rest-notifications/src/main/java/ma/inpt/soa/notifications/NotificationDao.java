package ma.inpt.soa.notifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import ma.inpt.soa.common.db.DatabaseConnection;
import ma.inpt.soa.common.db.SqlErrors;
import ma.inpt.soa.common.exception.ServiceException;

/**
 * L'émission est simulée : chaque notification est journalisée et conservée avec le statut SIMULEE.
 * Aucun e-mail réel n'est envoyé. Un connecteur SMTP se brancherait dans inserer().
 */
class NotificationDao {
    private static final Logger LOG = Logger.getLogger(NotificationDao.class.getName());
    private static final String ETAT_RAPPEL =
            " WHERE o.statut = 'ACTIVE' AND o.places_disponibles > 0"
            + " AND o.date_limite BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL ? DAY)";
    private static final String SANS_RAPPEL_DU_JOUR =
            " AND NOT EXISTS (SELECT 1 FROM Notification n WHERE n.type = 'RAPPEL_DATE_LIMITE' AND n.id_offre = o.id_offre"
            + " AND n.destinataire_type = '%s' AND n.destinataire_id = %s AND DATE(n.date_envoi) = CURRENT_DATE)";

    int inserer(Notification n) throws ServiceException {
        String sql = "INSERT INTO Notification (type, destinataire_type, destinataire_id, destinataire_email, sujet,"
                + " message, id_candidature, id_offre, statut_envoi) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SIMULEE')";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getType());
            ps.setString(2, n.getDestinataireType());
            entierOuNull(ps, 3, n.getDestinataireId());
            ps.setString(4, n.getDestinataireEmail());
            ps.setString(5, n.getSujet());
            ps.setString(6, n.getMessage());
            entierOuNull(ps, 7, n.getIdCandidature());
            entierOuNull(ps, 8, n.getIdOffre());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                int id = cles.getInt(1);
                LOG.info(() -> "Notification " + id + " simulée : " + n.getType() + " vers " + n.getDestinataireType());
                return id;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    Notification trouver(int id) throws ServiceException {
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM Notification WHERE id_notification = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? lire(rs) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    List<Notification> lister(String destinataireType, Integer destinataireId, Integer idCandidature)
            throws ServiceException {
        StringBuilder sql = new StringBuilder("SELECT * FROM Notification WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (destinataireType != null && !destinataireType.isBlank()) {
            sql.append(" AND destinataire_type = ?");
            params.add(destinataireType);
        }
        if (destinataireId != null) {
            sql.append(" AND destinataire_id = ?");
            params.add(destinataireId);
        }
        if (idCandidature != null) {
            sql.append(" AND id_candidature = ?");
            params.add(idCandidature);
        }
        sql.append(" ORDER BY date_envoi DESC, id_notification DESC");
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> liste = new ArrayList<>();
                while (rs.next()) {
                    liste.add(lire(rs));
                }
                return liste;
            }
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    int envoyerRappels(int joursAvant) throws ServiceException {
        String sujet = "CONCAT('Rappel : date limite du ', o.date_limite, ' - ', o.titre)";
        String entreprises = "INSERT INTO Notification (type, destinataire_type, destinataire_id, destinataire_email,"
                + " sujet, message, id_offre) SELECT 'RAPPEL_DATE_LIMITE', 'ENTREPRISE', e.id_entreprise, e.email, " + sujet
                + ", CONCAT('Votre offre « ', o.titre, ' » clôture ses candidatures le ', o.date_limite, '.'), o.id_offre"
                + " FROM Offre_Stage o JOIN Entreprise e ON e.id_entreprise = o.id_entreprise" + ETAT_RAPPEL
                + String.format(SANS_RAPPEL_DU_JOUR, "ENTREPRISE", "e.id_entreprise");
        String etudiants = "INSERT INTO Notification (type, destinataire_type, destinataire_id, destinataire_email,"
                + " sujet, message, id_offre) SELECT 'RAPPEL_DATE_LIMITE', 'ETUDIANT', et.id_etudiant, et.email, " + sujet
                + ", CONCAT('Vous pouvez candidater à « ', o.titre, ' » jusqu''au ', o.date_limite, '.'), o.id_offre"
                + " FROM Offre_Stage o JOIN Etudiant et ON et.statut = 'ACTIF'" + ETAT_RAPPEL
                + " AND NOT EXISTS (SELECT 1 FROM Candidature c WHERE c.id_offre = o.id_offre"
                + " AND c.id_etudiant = et.id_etudiant AND c.statut <> 'ANNULEE')"
                + String.format(SANS_RAPPEL_DU_JOUR, "ETUDIANT", "et.id_etudiant");
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement pe = c.prepareStatement(entreprises);
                PreparedStatement pt = c.prepareStatement(etudiants)) {
            pe.setInt(1, joursAvant);
            pt.setInt(1, joursAvant);
            return pe.executeUpdate() + pt.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.traduire(ex);
        }
    }

    private static void entierOuNull(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }

    private static Integer entierLu(ResultSet rs, String colonne) throws SQLException {
        int v = rs.getInt(colonne);
        return rs.wasNull() ? null : v;
    }

    private static Notification lire(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setIdNotification(rs.getInt("id_notification"));
        n.setType(rs.getString("type"));
        n.setDestinataireType(rs.getString("destinataire_type"));
        n.setDestinataireId(entierLu(rs, "destinataire_id"));
        n.setDestinataireEmail(rs.getString("destinataire_email"));
        n.setSujet(rs.getString("sujet"));
        n.setMessage(rs.getString("message"));
        n.setIdCandidature(entierLu(rs, "id_candidature"));
        n.setIdOffre(entierLu(rs, "id_offre"));
        n.setStatutEnvoi(rs.getString("statut_envoi"));
        n.setDateEnvoi(rs.getTimestamp("date_envoi").toLocalDateTime());
        return n;
    }
}
