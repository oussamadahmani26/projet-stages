package ma.inpt.soa.processus.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ma.inpt.soa.common.exception.ServiceException;

/** Après 5 échecs sur un identifiant, les essais sont bloqués pendant une minute. */
final class LimiteTentatives {
    private static final int MAX = 5;
    private static final Duration BLOCAGE = Duration.ofMinutes(1);

    private static final class Etat {
        int echecs;
        Instant blocageJusque = Instant.MIN;
    }

    private final Map<String, Etat> etats = new ConcurrentHashMap<>();

    void verifier(String identifiant) throws ServiceException {
        Etat e = etats.get(identifiant);
        if (e != null && e.blocageJusque.isAfter(Instant.now())) {
            throw new ServiceException(ServiceException.TROP_DE_TENTATIVES,
                    "Trop de tentatives. Réessayez dans une minute.");
        }
    }

    synchronized void echec(String identifiant) {
        Etat e = etats.computeIfAbsent(identifiant, k -> new Etat());
        e.echecs++;
        if (e.echecs >= MAX) {
            e.blocageJusque = Instant.now().plus(BLOCAGE);
            e.echecs = 0;
        }
    }

    void succes(String identifiant) {
        etats.remove(identifiant);
    }
}
