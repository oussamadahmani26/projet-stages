package ma.inpt.soa.candidatures;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.rest.RestServer;

public final class CandidaturesApplication {
    private CandidaturesApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return RestServer.demarrer(port, CandidatureResource.class);
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8084);
        demarrer(port);
        System.out.println("Service REST Candidatures : http://localhost:" + port + "/api/candidatures");
    }
}
