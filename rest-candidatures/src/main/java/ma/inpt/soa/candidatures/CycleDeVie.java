package ma.inpt.soa.candidatures;

import java.util.List;
import java.util.Map;
import ma.inpt.soa.common.exception.ServiceException;

/** Règles de gestion : transitions autorisées du statut d'une candidature. */
final class CycleDeVie {
    static final String[] STATUTS = {"DEPOSEE", "EN_COURS_EVALUATION", "ACCEPTEE", "REFUSEE", "ANNULEE"};

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "DEPOSEE", List.of("EN_COURS_EVALUATION", "REFUSEE", "ANNULEE"),
            "EN_COURS_EVALUATION", List.of("ACCEPTEE", "REFUSEE", "ANNULEE"),
            "ACCEPTEE", List.of(),
            "REFUSEE", List.of(),
            "ANNULEE", List.of());

    private CycleDeVie() {
    }

    static void verifierTransition(String actuel, String nouveau) throws ServiceException {
        if (!TRANSITIONS.get(actuel).contains(nouveau)) {
            throw ServiceException.conflit("Transition interdite : " + actuel + " vers " + nouveau + ".");
        }
    }
}
