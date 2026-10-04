package io.battery.script.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Host IP lookups for {@link NetworkFunctions}, using timeouts so that scripts don't block
 * indefinitely on network problems. The public IP is looked up once and cached.
 */
final class NetworkLookups {
    private static final String CHECK_IP_HOST = "checkip.amazonaws.com";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static volatile String publicIP;

    private NetworkLookups() {
    }

    static String localIP() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(CHECK_IP_HOST, 80), (int) TIMEOUT.toMillis());
            return socket.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String publicIP() {
        String ip = publicIP;
        if (ip == null) {
            synchronized (NetworkLookups.class) {
                ip = publicIP;
                if (ip == null) {
                    ip = lookupPublicIP();
                    publicIP = ip;
                }
            }
        }
        return ip;
    }

    private static String lookupPublicIP() {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + CHECK_IP_HOST + "/"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Public IP lookup failed with HTTP status " + response.statusCode());
            }
            return response.body().trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted looking up public IP", e);
        }
    }
}
