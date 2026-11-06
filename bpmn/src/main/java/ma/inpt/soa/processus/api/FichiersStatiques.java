package ma.inpt.soa.processus.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Sert l'interface graphique, embarquée dans le jar sous /static. */
public class FichiersStatiques implements HttpHandler {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml");

    @Override
    public void handle(HttpExchange echange) throws IOException {
        try {
            String chemin = echange.getRequestURI().getPath();
            if (chemin.equals("/")) {
                chemin = "/index.html";
            }
            InputStream fichier = chemin.contains("..") ? null
                    : FichiersStatiques.class.getResourceAsStream("/static" + chemin);
            if (fichier == null) {
                echange.sendResponseHeaders(404, -1);
                return;
            }
            try (fichier) {
                byte[] contenu = fichier.readAllBytes();
                String extension = chemin.substring(chemin.lastIndexOf('.') + 1);
                echange.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(extension, "application/octet-stream"));
                echange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                echange.sendResponseHeaders(200, contenu.length);
                echange.getResponseBody().write(contenu);
            }
        } finally {
            echange.close();
        }
    }
}
