package ma.inpt.soa.stages;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.rest.RestServer;

public final class StagesApplication {
    private StagesApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return RestServer.demarrer(port, StageResource.class);
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8086);
        demarrer(port);
        System.out.println("Service REST Stages : http://localhost:" + port + "/api/stages");
    }
}
