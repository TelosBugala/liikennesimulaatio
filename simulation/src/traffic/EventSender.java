package traffic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EventSender {

    private static final URI EVENTS_URI = URI.create("http://localhost:5256/events");

    private final HttpClient client = HttpClient.newHttpClient();

    public void send(PassEvent event) {
        String json = String.format(
            "{ \"tick\": %d, \"pointId\": %d, \"carId\": %d, \"direction\": \"%s\" }",
            event.tick(), event.pointId(), event.carId(), event.direction().name());

        HttpRequest request = HttpRequest.newBuilder(EVENTS_URI)
            .timeout(Duration.ofSeconds(2))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .whenComplete((response, error) -> {
                if (error != null) {
                    System.err.println("Event send failed: " + error);
                } else if (response.statusCode() != 201) {
                    System.err.println(
                        "Service replied " + response.statusCode() + " for " + event);
                }
            });
    }
}