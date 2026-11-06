package ma.inpt.soa.common.config;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/** Crée les serveurs HTTP(S) des services, liés par défaut à l'interface locale. */
public final class HttpServers {
    private static final Logger LOG = Logger.getLogger(HttpServers.class.getName());

    private HttpServers() {
    }

    public static HttpServer creer(int port) throws IOException {
        InetSocketAddress adresse = new InetSocketAddress(AppConfig.get("BIND_HOST", "127.0.0.1"), port);
        HttpServer serveur;
        if (TlsSupport.actif()) {
            HttpsServer https = HttpsServer.create(adresse, 0);
            https.setHttpsConfigurator(new HttpsConfigurator(TlsSupport.contexte()));
            serveur = https;
        } else {
            serveur = HttpServer.create(adresse, 0);
        }
        serveur.setExecutor(Executors.newCachedThreadPool());
        if (AppConfig.motDePasseParDefaut()) {
            LOG.warning("SOA_PASSWORD non défini : mot de passe de développement utilisé.");
        }
        return serveur;
    }
}
