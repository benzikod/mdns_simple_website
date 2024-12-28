package Function.Network;

import Function.Variable;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MDNS {
    private static final String SERVICE_NAME = "vmmp";
    private static final String SERVICE_TYPE = "_http._tcp.local.";
    private static final int PORT = 80;
    private static HttpServer server;
    private static JmDNS jmdns;
    private static boolean isServerStarted = false;
    private static boolean isInitialized = false;
    private static String leaderIpv4;


    public static String getLeaderIpv4() {
        if (leaderIpv4 != null) {
            return leaderIpv4;
        }
        if (!isServerStarted) {
            start();
        }
        try {
            InetAddress localHost = null;
            try {
                localHost = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                localHost = InetAddress.getLoopbackAddress();
            }

            JmDNS jmdns = JmDNS.create(localHost);
            ServiceInfo[] services = jmdns.list(SERVICE_TYPE);
            if (services != null) {
                for (ServiceInfo service : services) {
                    if (service.getName().equals(SERVICE_NAME) && service.getType().equals(SERVICE_TYPE)) {
                        String[] addresses = service.getHostAddresses();
                        if (addresses != null && addresses.length > 0) {
                            leaderIpv4 = addresses[0];
                            return leaderIpv4;
                        }
                    }
                }
            }
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized void start() {
        if (!isInitialized) {
            try {
                initializeMDNS();
                InetAddress bindAddress = InetAddress.getByName("0.0.0.0");
                server = HttpServer.create(new InetSocketAddress(bindAddress, PORT), 0);
                server.setExecutor(Executors.newCachedThreadPool());
                server.createContext("/", new WebHandler());
                isInitialized = true;
            } catch (IOException e) {
                return;
            }
        }

        if (!isServerStarted && server != null) {
            try {
                server.start();
                isServerStarted = true;
            } catch (Exception ignored) {}
        }
    }

    public static synchronized void stop() {
        if (isServerStarted && server != null) {
            server.stop(0);
            isServerStarted = false;
            isInitialized = false;
            server = null;
        }

        if (jmdns != null) {
            try {
                jmdns.unregisterAllServices();
                jmdns.close();
            } catch (IOException ignored) {}
            finally {
                jmdns = null;
            }
        }
    }

    private static synchronized void initializeMDNS() throws IOException {
        InetAddress address = InetAddress.getByName(Info.getIPv4Address());
        jmdns = JmDNS.create(address, SERVICE_NAME);
        Map<String, String> props = new HashMap<>();
        props.put("path", "/");
        String fullServiceName = SERVICE_NAME + "." + SERVICE_TYPE;
        ServiceInfo serviceInfo = ServiceInfo.create(fullServiceName, SERVICE_NAME, PORT, 0, 0, props);
        jmdns.registerService(serviceInfo);
    }

    private static class WebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = loadHtmlResponse();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private String loadHtmlResponse() {
            String response = "";
            try (InputStream inputStream = getClass().getResourceAsStream("/HTML/index.html")) {
                if (inputStream != null) {
                    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                        response = scanner.useDelimiter("\\A").next();
                    }
                }
            } catch (IOException ignored) {}
            return response;
        }
    }























}