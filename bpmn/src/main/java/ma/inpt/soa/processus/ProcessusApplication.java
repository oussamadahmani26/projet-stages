package ma.inpt.soa.processus;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.processus.api.FacadeResource;
import ma.inpt.soa.processus.api.FichiersStatiques;
import ma.inpt.soa.processus.api.ProcessusResource;
import ma.inpt.soa.processus.auth.AuthFilter;
import ma.inpt.soa.processus.auth.AuthResource;
import ma.inpt.soa.rest.RestServer;

/** Démarre le moteur BPMN, l'API façade et l'interface graphique. */
public final class ProcessusApplication {
    private ProcessusApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        Moteur.moteur();
        HttpServer serveur = RestServer.creer(port,
                RestServer.configuration(ProcessusResource.class, FacadeResource.class, AuthResource.class, AuthFilter.class));
        serveur.createContext("/", new FichiersStatiques());
        serveur.start();
        return serveur;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        int port = AppConfig.getInt("PORT", 8090);
        demarrer(port);
        System.out.println("Plateforme : http://localhost:" + port + "/");
    }
}
