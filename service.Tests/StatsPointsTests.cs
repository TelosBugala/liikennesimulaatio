using System.Net;
using System.Net.Http.Json;
using Xunit;

namespace TrafficService.Tests;

// Tilastovastauksen muoto testien puolella (palvelun omat tyypit eivät ole testeille näkyvissä).
public record DirectionCountsResponse(int North, int East, int South, int West);

public record PointSummaryResponse(
    int PointId, string PointName, int PassCount, double AverageSpeedKmh,
    DirectionCountsResponse Directions);

public class StatsPointsTests : ApiTestBase
{
    private async Task<List<PointSummaryResponse>> GetSummariesAsync() =>
        await Client.GetFromJsonAsync<List<PointSummaryResponse>>("/stats/points")
        ?? throw new InvalidOperationException("Response body was empty");

    // Lähettää kelvollisen tapahtuman. Parametreina ovat vain tilastoihin vaikuttavat kentät.
    // Tarkistaa, että tallennus onnistui, jotta virhe ei jää piiloon tilastotestin sisään.
    private async Task PostPass(
        int pointId = 1,
        string name = "Lehtotie",
        string simTime = "2026-01-05T06:00:00",
        string direction = "EAST",
        int speed = 50)
    {
        var response = await PostAsync(EventJson(
            pointId: pointId, pointName: name, simTime: simTime,
            direction: direction, speedKmh: speed));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Fact]
    public async Task Get_EmptyDatabase_ReturnsEmptyList()
    {
        Assert.Empty(await GetSummariesAsync());
    }

    [Fact]
    public async Task Get_SinglePoint_ReturnsCountAverageAndDirections()
    {
        await PostPass(direction: "EAST", speed: 50, simTime: "2026-01-05T06:00:00");
        await PostPass(direction: "EAST", speed: 25, simTime: "2026-01-05T06:00:05");
        await PostPass(direction: "WEST", speed: 25, simTime: "2026-01-05T06:00:10");

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal(1, point.PointId);
        Assert.Equal("Lehtotie", point.PointName);
        Assert.Equal(3, point.PassCount);
        Assert.Equal(33.3, point.AverageSpeedKmh);   // (50 + 25 + 25) / 3 = 33,33...
        Assert.Equal(new DirectionCountsResponse(North: 0, East: 2, South: 0, West: 1), point.Directions);
    }

    [Fact]
    public async Task Get_AverageSpeed_IsRoundedToOneDecimal()
    {
        await PostPass(speed: 50);
        await PostPass(speed: 25);
        await PostPass(speed: 17);

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal(30.7, point.AverageSpeedKmh);   // 92 / 3 = 30,666...
    }

    [Fact]
    public async Task Get_EachDirectionIsCountedSeparately()
    {
        var plan = new[] { ("NORTH", 1), ("EAST", 2), ("SOUTH", 3), ("WEST", 4) };
        foreach (var (direction, count) in plan)
        {
            for (var i = 0; i < count; i++)
            {
                await PostPass(direction: direction);
            }
        }

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal(10, point.PassCount);
        Assert.Equal(new DirectionCountsResponse(North: 1, East: 2, South: 3, West: 4), point.Directions);
    }

    [Fact]
    public async Task Get_MultiplePoints_ReturnsOneRowPerPointSortedByPointId()
    {
        await PostPass(pointId: 3, name: "Kuusitie");
        await PostPass(pointId: 1, name: "Lehtotie");
        await PostPass(pointId: 3, name: "Kuusitie");
        await PostPass(pointId: 2, name: "Koivutie");
        await PostPass(pointId: 3, name: "Kuusitie");
        await PostPass(pointId: 1, name: "Lehtotie");

        var points = await GetSummariesAsync();

        Assert.Equal(new[] { 1, 2, 3 }, points.Select(p => p.PointId));
        Assert.Equal(new[] { "Lehtotie", "Koivutie", "Kuusitie" }, points.Select(p => p.PointName));
        Assert.Equal(new[] { 2, 1, 3 }, points.Select(p => p.PassCount));
    }

    [Fact]
    public async Task Get_RenamedPoint_ShowsNewestNameAndStaysOneRow()
    {
        await PostPass(name: "Vanha", simTime: "2026-01-05T06:00:00");
        await PostPass(name: "Uusi", simTime: "2026-01-05T06:10:00");

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal("Uusi", point.PointName);
        Assert.Equal(2, point.PassCount);   // nimenvaihto ei jaa pistettä kahdeksi
    }

    [Fact]
    public async Task Get_EventsArriveOutOfOrder_NameFollowsSimulationTime()
    {
        // Uudempi tapahtuma saapuu ensin: tapahtumat lähetetään asynkronisesti,
        // joten järjestys voi mennä sekaisin. Nimen pitää silti seurata simulaatioaikaa.
        await PostPass(name: "Uusi", simTime: "2026-01-05T06:10:00");
        await PostPass(name: "Vanha", simTime: "2026-01-05T06:00:00");

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal("Uusi", point.PointName);
    }

    [Fact]
    public async Task Get_SameSimulationTime_UsesLastStoredName()
    {
        await PostPass(name: "Ensin", simTime: "2026-01-05T06:00:00");
        await PostPass(name: "Sitten", simTime: "2026-01-05T06:00:00");

        var point = Assert.Single(await GetSummariesAsync());

        Assert.Equal("Sitten", point.PointName);
    }
}