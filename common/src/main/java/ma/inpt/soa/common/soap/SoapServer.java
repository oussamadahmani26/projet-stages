package ma.inpt.soa.common.soap;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.common.config.HttpServers;

/** Publie un service JAX-WS. Le WSDL reste public, les opérations exigent une authentification Basic. */
public final class SoapServer {
    private SoapServer() {
    }

    public static HttpServer publier(int port, String chemin, Object service) throws IOException {
        HttpServer serveur = HttpServers.creer(port);
        HttpContext contexte = serveur.createContext(chemin);
        contexte.setAuthenticator(new Authentificateur());
        Endpoint.create(service).publish(contexte);
        serveur.start();
        return serveur;
    }

    private static final class Authentificateur extends BasicAuthenticator {
        Authentificateur() {
            super("services-soa");
        }

        @Override
        public Result authenticate(HttpExchange echange) {
            String requete = echange.getRequestURI().getQuery();
            boolean description = "GET".equals(echange.getRequestMethod()) && requete != null
                    && (requete.startsWith("wsdl") || requete.startsWith("xsd"));
            return description ? new Success(new HttpPrincipal("anonyme", getRealm())) : super.authenticate(echange);
        }

        @Override
        public boolean checkCredentials(String utilisateur, String motDePasse) {
            return egal(utilisateur, AppConfig.utilisateurServices())
                    & egal(motDePasse, AppConfig.motDePasseServices());
        }

        private static boolean egal(String a, String b) {
            return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
        }
    }
}
