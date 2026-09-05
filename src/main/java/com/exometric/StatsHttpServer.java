package com.exometric;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import net.fabricmc.loader.api.FabricLoader;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class StatsHttpServer {

    private static HttpServer server;
    private static int currentPort = -1;

    private static HttpsServer httpsServer;
    private static int currentHttpsPort = -1;

    public static void start() {
        ConfigManager.Config config = ConfigManager.getConfig();
        if (!config.api_enabled) {
            System.out.println("[ExoMetric] API is disabled in config.");
            return;
        }

        if (config.api_port <= 0) {
            System.out.println("[ExoMetric] API port is not set (0). Please configure api_port in config/ExoMetric.json");
        } else {
            try {
                currentPort = config.api_port;
                server = HttpServer.create(new InetSocketAddress(currentPort), 0);
                server.createContext("/mc-stats", new MyHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("[ExoMetric] API Server started on port " + currentPort);
            } catch (IOException e) {
                System.err.println("[ExoMetric] Failed to start API Server: " + e.getMessage());
            }
        }

        startHttps(config);
    }

    private static void startHttps(ConfigManager.Config config) {
        if (!config.api_https_enabled) {
            return;
        }

        if (config.api_https_port <= 0) {
            System.out.println("[ExoMetric] api_https_port is not set (0). Please configure api_https_port in config/ExoMetric.json");
            return;
        }

        try {
            File keystoreFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "exometric-ssl.p12");
            SSLContext sslContext = CertUtil.getOrCreateSelfSignedContext(
                    keystoreFile, config.api_https_keystore_password, "localhost");

            currentHttpsPort = config.api_https_port;
            httpsServer = HttpsServer.create(new InetSocketAddress(currentHttpsPort), 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            httpsServer.createContext("/mc-stats", new MyHandler());
            httpsServer.setExecutor(null);
            httpsServer.start();
            System.out.println("[ExoMetric] HTTPS API Server started on port " + currentHttpsPort
                    + " (self-signed certificate; browsers will show a security warning)");
        } catch (Exception e) {
            System.err.println("[ExoMetric] Failed to start HTTPS API Server: " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("[ExoMetric] API Server stopped.");
        }
        if (httpsServer != null) {
            httpsServer.stop(0);
            httpsServer = null;
            System.out.println("[ExoMetric] HTTPS API Server stopped.");
        }
    }

    public static void reload() {
        ConfigManager.Config config = ConfigManager.getConfig();

        boolean httpChanged = server == null || config.api_port != currentPort || !config.api_enabled;
        boolean httpsChanged = (httpsServer == null) != !config.api_https_enabled
                || config.api_https_port != currentHttpsPort;

        if (httpChanged || httpsChanged) {
            stop();
            if (config.api_enabled) {
                start();
            }
        } else {
            System.out.println("[ExoMetric] Config reloaded (token updated).");
        }
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                if (!"GET".equals(t.getRequestMethod())) {
                    t.sendResponseHeaders(405, -1);
                    return;
                }

                String query = t.getRequestURI().getQuery();
                String expectedToken = ConfigManager.getConfig().api_token;
                boolean authorized = false;

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2 && "token".equals(pair[0])) {
                            if (expectedToken.equals(pair[1])) {
                                authorized = true;
                                break;
                            }
                        }
                    }
                }

                if (!authorized) {
                    byte[] response = "{\"error\": \"Unauthorized\"}".getBytes("UTF-8");
                    t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    t.sendResponseHeaders(401, response.length);
                    try (OutputStream os = t.getResponseBody()) {
                        os.write(response);
                    }
                    return;
                }

                String path = t.getRequestURI().getPath();
                MetricsData metrics = MetricsCollector.getLatestMetrics();
                
                String json;
                if (path.startsWith("/mc-stats/players")) {
                    json = metrics.toJsonPlayers();
                } else if (path.startsWith("/mc-stats/system")) {
                    json = metrics.toJsonSystem();
                } else {
                    json = metrics.toJson();
                }

                byte[] responseBytes = json.getBytes("UTF-8");
                t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                t.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = t.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                t.sendResponseHeaders(500, -1);
            }
        }
    }
}
