package ma.inpt.soa.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Active HTTPS lorsque SOA_KEYSTORE désigne un keystore PKCS12.
 * Le même fichier sert de magasin de clés aux serveurs et de magasin de confiance aux clients.
 */
public final class TlsSupport {
    private TlsSupport() {
    }

    public static boolean actif() {
        return AppConfig.get("SOA_KEYSTORE", null) != null;
    }

    public static String schema() {
        return actif() ? "https" : "http";
    }

    public static SSLContext contexte() {
        try (InputStream in = Files.newInputStream(Path.of(AppConfig.get("SOA_KEYSTORE", null)))) {
            char[] motDePasse = AppConfig.get("SOA_KEYSTORE_PASSWORD", "").toCharArray();
            KeyStore magasin = KeyStore.getInstance("PKCS12");
            magasin.load(in, motDePasse);
            KeyManagerFactory cles = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            cles.init(magasin, motDePasse);
            TrustManagerFactory confiance =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            confiance.init(magasin);
            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(cles.getKeyManagers(), confiance.getTrustManagers(), null);
            return contexte;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Keystore TLS illisible : " + e.getMessage());
        }
    }
}
