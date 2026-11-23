using System.Net.Http.Json;
using System.Text;
using System.Text.Json.Nodes;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.Sqlite;

namespace TrafficService.Tests;

// Palvelun vastauksessa olevan tapahtuman muoto. Tämä on testien oma kuvaus,
// koska palvelun StoredEvent on sen sisäinen, eikä testiprojekti näe sitä.
public record StoredEventResponse(
    int Id, int Tick, string SimTime, int PointId, string PointName,
    int CarId, string Direction, int SpeedKmh, string ReceivedAt);

// Palvelun virhevastauksen muoto: { "errors": [ ... ] }
public record ErrorResponse(List<string> Errors);

// Käynnistää palvelun muistissa (ilman oikeaa porttia) ja antaa sille asetuksena
// väliaikaisen tietokantatiedoston.
internal class TestFactory : WebApplicationFactory<Program>
{
    private readonly string _databasePath;

    public TestFactory(string databasePath)
    {
        _databasePath = databasePath;
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseSetting("ConnectionStrings:Traffic", $"Data Source={_databasePath}");
    }
}

// Kaikkien palvelutestien yhteinen pohja.
// xUnit luo testiluokasta uuden olion jokaiselle testille: konstruktori ajetaan ennen
// testiä ja Dispose sen jälkeen. Siksi jokainen testi saa oman tyhjän tietokannan.
public abstract class ApiTestBase : IDisposable
{
    private readonly string _databasePath =
        Path.Combine(Path.GetTempPath(), $"traffic-test-{Guid.NewGuid():N}.db");

    private readonly TestFactory _factory;

    protected HttpClient Client { get; }

    protected ApiTestBase()
    {
        _factory = new TestFactory(_databasePath);
        Client = _factory.CreateClient();
    }

    public void Dispose()
    {
        Client.Dispose();
        _factory.Dispose();

        // SQLite-kirjasto pitää yhteyksiä avoinna uudelleenkäyttöä varten. Ne suljetaan,
        // jotta Windows sallii tiedostojen poiston.
        SqliteConnection.ClearAllPools();

        foreach (var file in new[] { _databasePath, _databasePath + "-wal", _databasePath + "-shm" })
        {
            File.Delete(file);
        }
    }

    // Rakentaa tapahtuman JSON:n. Oletusarvot ovat kelvollinen tapahtuma,
    // ja testi korvaa vain ne kentät, joilla on väliä.
    // Huom: arvoihin ei saa laittaa lainausmerkkejä (ei tarvita näissä testeissä).
    protected static string EventJson(
        int tick = 37,
        string simTime = "2026-01-05T06:03:05",
        int pointId = 1,
        string pointName = "Lehtotie",
        int carId = 2,
        string direction = "EAST",
        int speedKmh = 50) =>
        $$"""
        { "tick": {{tick}}, "simTime": "{{simTime}}", "pointId": {{pointId}}, "pointName": "{{pointName}}", "carId": {{carId}}, "direction": "{{direction}}", "speedKmh": {{speedKmh}} }
        """;

    // Lähettää annetun JSON:n tekstinä POST /events -osoitteeseen.
    protected Task<HttpResponseMessage> PostAsync(string json) =>
        Client.PostAsync("/events", new StringContent(json, Encoding.UTF8, "application/json"));

    // Hakee tallennetut tapahtumat (GET /events).
    protected async Task<List<StoredEventResponse>> GetEventsAsync() =>
        await Client.GetFromJsonAsync<List<StoredEventResponse>>("/events")
        ?? throw new InvalidOperationException("Response body was empty");

        // Rakentaa kelvollisen tapahtuman, josta on poistettu yksi kenttä.
    protected static string EventJsonWithout(string field)
    {
        var node = JsonNode.Parse(EventJson())!.AsObject();
        node.Remove(field);
        return node.ToJsonString();
    }

    // Rakentaa kelvollisen tapahtuman, jossa yhden kentän arvo on korvattu.
    // Arvo voi olla mitä tahansa JSONia: luku, merkkijono, null tai väärän tyyppinen arvo.
    protected static string EventJsonWith(string field, JsonNode? value)
    {
        var node = JsonNode.Parse(EventJson())!.AsObject();
        node[field] = value;
        return node.ToJsonString();
    }

    // Lukee virheilmoitukset 400-vastauksesta.
    protected static async Task<List<string>> ReadErrorsAsync(HttpResponseMessage response)
    {
        var body = await response.Content.ReadFromJsonAsync<ErrorResponse>();
        return body?.Errors ?? throw new InvalidOperationException("Response had no errors list");
    }
}