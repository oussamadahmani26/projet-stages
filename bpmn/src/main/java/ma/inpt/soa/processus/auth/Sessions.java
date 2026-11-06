package ma.inpt.soa.processus.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sessions en mémoire, identifiées par un jeton aléatoire. Un redémarrage demande de se reconnecter. */
public final class Sessions {
    public static final Duration DUREE = Duration.ofHours(8);
    private static final SecureRandom ALEA = new SecureRandom();
    private static final Map<String, Session> ACTIVES = new ConcurrentHashMap<>();

    private Sessions() {
    }

    static String ouvrir(Session session) {
        ACTIVES.values().removeIf(s -> s.expiration().isBefore(Instant.now()));
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        session.expireA(Instant.now().plus(DUREE));
        ACTIVES.put(jeton, session);
        return jeton;
    }

    static Session trouver(String jeton) {
        if (jeton == null) {
            return null;
        }
        Session s = ACTIVES.get(jeton);
        if (s == null) {
            return null;
        }
        if (s.expiration().isBefore(Instant.now())) {
            ACTIVES.remove(jeton);
            return null;
        }
        return s;
    }

    static void fermer(String jeton) {
        if (jeton != null) {
            ACTIVES.remove(jeton);
        }
    }
}
