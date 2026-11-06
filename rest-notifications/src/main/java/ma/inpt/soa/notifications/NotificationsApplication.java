package ma.inpt.soa.notifications;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import ma.inpt.soa.common.config.AppConfig;
import ma.inpt.soa.rest.RestServer;

public final class NotificationsApplication {
    private NotificationsApplication() {
    }

    public static HttpServer demarrer(int port) throws IOException {
        return RestServer.demarrer(port, NotificationResource.class);
    }

    public static void main(String[] args) throws IOException {
        int port = AppConfig.getInt("PORT", 8085);
        demarrer(port);
        System.out.println("Service REST Notifications : http://localhost:" + port + "/api/notifications");
    }
}
