using Xunit;

namespace TrafficService.Tests;

public class EventsGetTests : ApiTestBase
{
    [Fact]
    public async Task Get_EmptyDatabase_ReturnsEmptyList()
    {
        var events = await GetEventsAsync();

        Assert.Empty(events);
    }

    [Fact]
    public async Task Get_ReturnsNewestFirst()
    {
        await PostAsync(EventJson(tick: 1));
        await PostAsync(EventJson(tick: 2));
        await PostAsync(EventJson(tick: 3));

        var events = await GetEventsAsync();

        Assert.Equal(new[] { 3, 2, 1 }, events.Select(e => e.Tick));
    }

    [Fact]
    public async Task Get_ReturnsAtMost100Events()
    {
        for (var i = 0; i < 105; i++)
        {
            await PostAsync(EventJson(tick: i, carId: i + 1));
        }

        var events = await GetEventsAsync();

        Assert.Equal(100, events.Count);
        Assert.Equal(104, events.First().Tick);  // uusin
        Assert.Equal(5, events.Last().Tick);     // 5 vanhinta jäi pois
    }
}