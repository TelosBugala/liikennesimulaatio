package traffic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class EventSender {

    private final URI eventsUri;
    private final HttpClient client = HttpClient.newHttpClient();

    public EventSender(String serviceUrl) {
        this.eventsUri = URI.create(serviceUrl + "/events");
    }

    public void send(PassEvent event) {
        String json = String.format(
            "{ \"tick\": %d, \"simTime\": \"%s\", \"pointId\": %d, "
                + "\"carId\": %d, \"direction\": \"%s\" }",
            event.tick(),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(event.simTime()),
            event.pointId(), event.carId(), event.direction().name());
        Debug.log("Sending " + json);

        HttpRequest request = HttpRequest.newBuilder(eventsUri)
            .timeout(Duration.ofSeconds(2))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .whenComplete((response, error) -> {
                if (error != null) {
                    System.err.println("Event send to " + eventsUri + " failed: " + error);
                } else if (response.statusCode() != 201) {
                    System.err.println("Service at " + eventsUri + " replied "
                        + response.statusCode() + " for " + event);
                }
            });
    }
}