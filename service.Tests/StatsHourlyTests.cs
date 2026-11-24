using System.Net;
using System.Net.Http.Json;
using Xunit;

namespace TrafficService.Tests;

public record HourlyCountResponse(int PointId, string PointName, string HourStart, int PassCount);

public class StatsHourlyTests : ApiTestBase
{
    private async Task<List<HourlyCountResponse>> GetHourlyAsync() =>
        await Client.GetFromJsonAsync<List<HourlyCountResponse>>("/stats/hourly")
        ?? throw new InvalidOperationException("Response body was empty");

    // Lähettää kelvollisen tapahtuman ja tarkistaa, että tallennus onnistui.
    private async Task PostPass(int pointId = 1, string name = "Lehtotie", string simTime = "2026-01-05T06:00:00")
    {
        var response = await PostAsync(EventJson(pointId: pointId, pointName: name, simTime: simTime));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Fact]
    public async Task Get_EmptyDatabase_ReturnsEmptyList()
    {
        Assert.Empty(await GetHourlyAsync());
    }

    [Fact]
    public async Task Get_EventsInSameHour_AreCountedTogether()
    {
        await PostPass(simTime: "2026-01-05T06:00:00");
        await PostPass(simTime: "2026-01-05T06:30:15");
        await PostPass(simTime: "2026-01-05T06:59:59");

        var row = Assert.Single(await GetHourlyAsync());

        Assert.Equal(1, row.PointId);
        Assert.Equal("Lehtotie", row.PointName);
        Assert.Equal("2026-01-05T06:00:00", row.HourStart);
        Assert.Equal(3, row.PassCount);
    }

    [Fact]
    public async Task Get_HourBoundary_SplitsIntoSeparateHours()
    {
        await PostPass(simTime: "2026-01-05T06:59:59");
        await PostPass(simTime: "2026-01-05T07:00:00");

        var rows = await GetHourlyAsync();

        Assert.Equal(new[] { "2026-01-05T06:00:00", "2026-01-05T07:00:00" }, rows.Select(r => r.HourStart));
        Assert.All(rows, r => Assert.Equal(1, r.PassCount));
    }

    [Fact]
    public async Task Get_SameHourOnDifferentDays_AreSeparateRows()
    {
        await PostPass(simTime: "2026-01-05T08:15:00");
        await PostPass(simTime: "2026-01-06T08:30:00");

        var rows = await GetHourlyAsync();

        Assert.Equal(new[] { "2026-01-05T08:00:00", "2026-01-06T08:00:00" }, rows.Select(r => r.HourStart));
    }

    [Fact]
    public async Task Get_HoursWithoutEvents_AreNotListed()
    {
        await PostPass(simTime: "2026-01-05T06:10:00");
        await PostPass(simTime: "2026-01-05T09:10:00");

        var rows = await GetHourlyAsync();

        Assert.Equal(new[] { "2026-01-05T06:00:00", "2026-01-05T09:00:00" }, rows.Select(r => r.HourStart));
    }

    [Fact]
    public async Task Get_Points_AreCountedSeparatelyAndSortedByPointThenHour()
    {
        await PostPass(pointId: 2, name: "Koivutie", simTime: "2026-01-05T07:00:00");
        await PostPass(pointId: 1, name: "Lehtotie", simTime: "2026-01-05T08:00:00");
        await PostPass(pointId: 1, name: "Lehtotie", simTime: "2026-01-05T06:00:00");
        await PostPass(pointId: 2, name: "Koivutie", simTime: "2026-01-05T07:30:00");

        var rows = await GetHourlyAsync();

        var expected = new[]
        {
            (1, "2026-01-05T06:00:00", 1),
            (1, "2026-01-05T08:00:00", 1),
            (2, "2026-01-05T07:00:00", 2),
        };
        Assert.Equal(expected, rows.Select(r => (r.PointId, r.HourStart, r.PassCount)));
    }

    [Fact]
    public async Task Get_RenamedPoint_ShowsNewestNameInEveryRow()
    {
        await PostPass(name: "Vanha", simTime: "2026-01-05T06:10:00");
        await PostPass(name: "Uusi", simTime: "2026-01-05T08:10:00");

        var rows = await GetHourlyAsync();

        Assert.Equal(2, rows.Count);
        Assert.All(rows, r => Assert.Equal("Uusi", r.PointName));
    }
}