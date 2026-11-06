package ma.inpt.soa.processus.taches;

import java.util.logging.Logger;
import ma.inpt.soa.common.exception.ServiceException;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * Base des tâches de service. Toute erreur d'un service appelé devient une erreur BPMN,
 * interceptée dans le diagramme par un événement intermédiaire de type erreur.
 */
abstract class TacheService implements JavaDelegate {
    private static final Logger LOG = Logger.getLogger(TacheService.class.getName());
    static final String ERREUR_SERVICE = "ERREUR_SERVICE";

    abstract void realiser(DelegateExecution e) throws ServiceException;

    @Override
    public final void execute(DelegateExecution e) {
        try {
            realiser(e);
        } catch (ServiceException ex) {
            LOG.warning("Tâche " + getClass().getSimpleName() + " en échec : " + ex.getMessage());
            e.setVariable("erreurMessage", ex.getMessage());
            throw new BpmnError(ERREUR_SERVICE, ex.getMessage());
        }
    }

    /** Exécute l'action une seule fois, y compris si l'étape est relancée après un incident. */
    static void uneFois(DelegateExecution e, String drapeau, Action action) throws ServiceException {
        if (!Boolean.TRUE.equals(e.getVariable(drapeau))) {
            action.executer();
            e.setVariable(drapeau, true);
        }
    }

    static String texte(DelegateExecution e, String nom) {
        Object v = e.getVariable(nom);
        return v == null ? null : v.toString();
    }

    static Integer entier(DelegateExecution e, String nom) {
        Object v = e.getVariable(nom);
        return v == null ? null : (Integer) v;
    }

    static void ajouterMotif(DelegateExecution e, String motif) {
        String actuel = texte(e, "motifsRejet");
        e.setVariable("motifsRejet", actuel == null || actuel.isEmpty() ? motif : actuel + " " + motif);
    }

    interface Action {
        void executer() throws ServiceException;
    }
}
