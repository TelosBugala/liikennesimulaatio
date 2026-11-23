using System.Net;
using System.Text.Json.Nodes;
using Xunit;

namespace TrafficService.Tests;

public class EventsValidationTests : ApiTestBase
{
    // ---------- Puuttuvat ja null-arvoiset kentät ----------

    [Theory]
    [InlineData("tick")]
    [InlineData("simTime")]
    [InlineData("pointId")]
    [InlineData("pointName")]
    [InlineData("carId")]
    [InlineData("direction")]
    [InlineData("speedKmh")]
    public async Task Post_MissingField_ReturnsBadRequestNamingTheField(string field)
    {
        var response = await PostAsync(EventJsonWithout(field));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal($"{field} is required", Assert.Single(await ReadErrorsAsync(response)));
    }

    [Theory]
    [InlineData("tick")]
    [InlineData("simTime")]
    [InlineData("pointId")]
    [InlineData("pointName")]
    [InlineData("carId")]
    [InlineData("direction")]
    [InlineData("speedKmh")]
    public async Task Post_NullField_IsTreatedAsMissing(string field)
    {
        var response = await PostAsync(EventJsonWith(field, null));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal($"{field} is required", Assert.Single(await ReadErrorsAsync(response)));
    }

    // ---------- Numerokentät ----------

    [Theory]
    [InlineData("tick", -1, "tick must be 0 or greater")]
    [InlineData("pointId", 0, "pointId must be 1 or greater")]
    [InlineData("pointId", -5, "pointId must be 1 or greater")]
    [InlineData("carId", 0, "carId must be 1 or greater")]
    [InlineData("carId", -5, "carId must be 1 or greater")]
    public async Task Post_NumberOutOfRange_ReturnsBadRequest(string field, int value, string expectedError)
    {
        var response = await PostAsync(EventJsonWith(field, value));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal(expectedError, Assert.Single(await ReadErrorsAsync(response)));
    }

    [Fact]
    public async Task Post_TickZero_IsAccepted()
    {
        // Simulaatio alkaa tickistä 0, joten nolla on sallittu raja-arvo.
        var response = await PostAsync(EventJson(tick: 0));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Theory]
    [InlineData("42.5")]
    [InlineData("\"fast\"")]
    [InlineData("true")]
    public async Task Post_NonIntegerSpeed_ReturnsBadRequest(string rawJsonValue)
    {
        // Nopeus on kokonaisluku. Väärän tyyppisen arvon hylkää ASP.NET Core jo ennen omaa tarkistustamme,
        // joten vastauksen muotoa ei tarkisteta, vain koodi.
        var response = await PostAsync(EventJsonWith("speedKmh", JsonNode.Parse(rawJsonValue)));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    // ---------- Suunta ----------

    [Theory]
    [InlineData("UP")]
    [InlineData("north")]
    [InlineData("North")]
    [InlineData("")]
    [InlineData("NORTH ")]
    public async Task Post_InvalidDirection_ReturnsBadRequest(string direction)
    {
        var response = await PostAsync(EventJson(direction: direction));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal("direction must be NORTH, EAST, SOUTH or WEST",
            Assert.Single(await ReadErrorsAsync(response)));
    }

    [Theory]
    [InlineData("NORTH")]
    [InlineData("EAST")]
    [InlineData("SOUTH")]
    [InlineData("WEST")]
    public async Task Post_ValidDirection_IsAccepted(string direction)
    {
        var response = await PostAsync(EventJson(direction: direction));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    // ---------- Simulaatioaika ----------

    [Theory]
    [InlineData("yesterday")]
    [InlineData("2026-02-30T06:00:00")]   // päivää 30. helmikuuta ei ole
    [InlineData("2026-02-29T12:00:00")]   // 2026 ei ole karkausvuosi
    [InlineData("2026-01-05 06:03:05")]   // välilyönti T-kirjaimen tilalla
    [InlineData("2026-01-05T06:03")]      // sekunnit puuttuvat
    [InlineData("2026-01-05T25:00:00")]   // tunti 25
    [InlineData("2026-01-05T06:03:05Z")]  // aikavyöhyke ei kuulu muotoon
    [InlineData("")]
    public async Task Post_InvalidSimTime_ReturnsBadRequest(string simTime)
    {
        var response = await PostAsync(EventJson(simTime: simTime));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal("simTime must be in format yyyy-MM-ddTHH:mm:ss",
            Assert.Single(await ReadErrorsAsync(response)));
    }

    [Theory]
    [InlineData("2026-01-05T06:03:05")]
    [InlineData("2028-02-29T12:00:00")]   // 2028 on karkausvuosi
    [InlineData("2026-12-31T23:59:59")]
    public async Task Post_ValidSimTime_IsAcceptedAndStoredUnchanged(string simTime)
    {
        var response = await PostAsync(EventJson(simTime: simTime));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Equal(simTime, Assert.Single(await GetEventsAsync()).SimTime);
    }

    // ---------- Pisteen nimi ----------

    [Theory]
    [InlineData("")]
    [InlineData(" Lehtotie")]   // välilyönti alussa
    [InlineData("Lehtotie ")]   // välilyönti lopussa
    [InlineData("  ")]          // pelkkiä välilyöntejä
    [InlineData("Lehtotie!")]   // kielletty merkki
    [InlineData("Piste_1")]     // alaviiva ei ole sallittu
    [InlineData("A.B")]         // piste ei ole sallittu
    public async Task Post_InvalidPointName_ReturnsBadRequest(string name)
    {
        var response = await PostAsync(EventJson(pointName: name));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var error = Assert.Single(await ReadErrorsAsync(response));
        Assert.StartsWith("pointName", error);
    }

    [Theory]
    [InlineData("Lehtotie")]
    [InlineData("Päätie-2")]
    [InlineData("A")]            // lyhin sallittu nimi
    [InlineData("Tie 12 B")]     // välilyönnit keskellä sallittu
    [InlineData("A-B")]
    [InlineData("Öö")]
    public async Task Post_ValidPointName_IsAccepted(string name)
    {
        var response = await PostAsync(EventJson(pointName: name));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Fact]
    public async Task Post_PointNameOf30Characters_IsAccepted()
    {
        var response = await PostAsync(EventJson(pointName: new string('A', 30)));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Fact]
    public async Task Post_PointNameOf31Characters_ReturnsBadRequest()
    {
        var response = await PostAsync(EventJson(pointName: new string('A', 31)));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    // ---------- Koko pyyntö ----------

    [Fact]
    public async Task Post_EmptyObject_ReportsAllSevenMissingFields()
    {
        var response = await PostAsync("{}");

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal(7, (await ReadErrorsAsync(response)).Count);
    }

    [Theory]
    [InlineData("")]
    [InlineData("{ not json")]
    public async Task Post_UnreadableBody_ReturnsBadRequest(string body)
    {
        var response = await PostAsync(body);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Post_SeveralErrors_AreAllReportedAtOnce()
    {
        // Sama pyyntö kuin requests.http:n "Several errors at once".
        // Nopeus 99 on sallittu, joten virheitä on kuusi.
        var json = """
            { "tick": -5, "simTime": "yesterday", "pointId": 0, "pointName": "", "carId": 0, "direction": "UP", "speedKmh": 99 }
            """;

        var response = await PostAsync(json);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var errors = await ReadErrorsAsync(response);
        Assert.Equal(6, errors.Count);
        foreach (var field in new[] { "tick", "simTime", "pointId", "pointName", "carId", "direction" })
        {
            Assert.Contains(errors, e => e.StartsWith(field));
        }
        Assert.DoesNotContain(errors, e => e.StartsWith("speedKmh"));
    }

    [Fact]
    public async Task Post_InvalidEvent_IsNotStored()
    {
        await PostAsync(EventJson(direction: "UP"));

        Assert.Empty(await GetEventsAsync());
    }
}