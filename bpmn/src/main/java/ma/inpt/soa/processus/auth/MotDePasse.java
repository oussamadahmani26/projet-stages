package ma.inpt.soa.processus.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Vérifie un mot de passe haché en PBKDF2 : pbkdf2$iterations$sel$empreinte (Base64). */
final class MotDePasse {
    /** Empreinte de référence : un identifiant inconnu prend le même temps qu'un mauvais mot de passe. */
    static final String LEURRE = "pbkdf2$120000$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private static final int ITERATIONS = 120000;
    private static final SecureRandom ALEA = new SecureRandom();

    private MotDePasse() {
    }

    static String hacher(String motDePasse) {
        byte[] sel = new byte[16];
        ALEA.nextBytes(sel);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(sel) + "$"
                + Base64.getEncoder().encodeToString(deriver(motDePasse, sel, ITERATIONS, 32));
    }

    private static byte[] deriver(String motDePasse, byte[] sel, int iterations, int octets) {
        try {
            PBEKeySpec spec = new PBEKeySpec(motDePasse.toCharArray(), sel, iterations, octets * 8);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algorithme PBKDF2 indisponible.");
        }
    }

    static boolean verifier(String motDePasse, String stocke) {
        String[] parties = stocke.split("\\$");
        if (parties.length != 4 || !"pbkdf2".equals(parties[0])) {
            return false;
        }
        byte[] sel = Base64.getDecoder().decode(parties[2]);
        byte[] attendu = Base64.getDecoder().decode(parties[3]);
        return MessageDigest.isEqual(deriver(motDePasse, sel, Integer.parseInt(parties[1]), attendu.length), attendu);
    }
}
