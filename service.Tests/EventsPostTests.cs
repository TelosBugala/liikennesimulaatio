using System.Net;
using Xunit;

namespace TrafficService.Tests;

public class EventsPostTests : ApiTestBase
{
    [Fact]
    public async Task Post_ValidEvent_ReturnsCreated()
    {
        var response = await PostAsync(EventJson());

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    [Fact]
    public async Task Post_ValidEvent_IsStoredWithAllFields()
    {
        await PostAsync(EventJson(
            tick: 37, simTime: "2026-01-05T06:03:05", pointId: 1, pointName: "Lehtotie",
            carId: 2, direction: "EAST", speedKmh: 50));

        var events = await GetEventsAsync();

        var stored = Assert.Single(events);
        Assert.Equal(37, stored.Tick);
        Assert.Equal("2026-01-05T06:03:05", stored.SimTime);
        Assert.Equal(1, stored.PointId);
        Assert.Equal("Lehtotie", stored.PointName);
        Assert.Equal(2, stored.CarId);
        Assert.Equal("EAST", stored.Direction);
        Assert.Equal(50, stored.SpeedKmh);
        Assert.False(string.IsNullOrEmpty(stored.ReceivedAt));
    }

    [Fact]
    public async Task Post_FinnishCharactersInName_AreStoredCorrectly()
    {
        await PostAsync(EventJson(pointName: "Päätie-2 Öljy"));

        var events = await GetEventsAsync();

        Assert.Equal("Päätie-2 Öljy", Assert.Single(events).PointName);
    }

    [Theory]
    [InlineData(83)]
    [InlineData(0)]
    [InlineData(-10)]
    public async Task Post_AnyIntegerSpeed_IsAccepted(int speed)
    {
        var response = await PostAsync(EventJson(speedKmh: speed));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Equal(speed, Assert.Single(await GetEventsAsync()).SpeedKmh);
    }
}