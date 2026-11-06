package ma.inpt.soa.common.db;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ma.inpt.soa.common.exception.ServiceException;

/** Traduit les erreurs SQL en erreurs métier, sans exposer le détail technique au client. */
public final class SqlErrors {
    private static final Logger LOG = Logger.getLogger(SqlErrors.class.getName());
    private static final int DOUBLON = 1062;
    private static final int LIGNE_REFERENCEE = 1451;
    private static final int REFERENCE_ABSENTE = 1452;

    private SqlErrors() {
    }

    public static ServiceException traduire(SQLException e, String messageDoublon, String messageReference) {
        switch (e.getErrorCode()) {
            case DOUBLON:
                return ServiceException.conflit(messageDoublon);
            case LIGNE_REFERENCEE:
            case REFERENCE_ABSENTE:
                return ServiceException.conflit(messageReference);
            default:
                return traduire(e);
        }
    }

    public static ServiceException traduire(SQLException e) {
        LOG.log(Level.SEVERE, "Erreur base de données (SQLState " + e.getSQLState() + ")", e);
        return new ServiceException(ServiceException.INDISPONIBLE, "Base de données indisponible.");
    }
}
