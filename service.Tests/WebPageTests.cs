using System.Net;
using Xunit;

namespace TrafficService.Tests;

public class WebPageTests : ApiTestBase
{
    [Fact]
    public async Task Get_Root_ReturnsHtmlPage()
    {
        var response = await Client.GetAsync("/");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("text/html", response.Content.Headers.ContentType?.MediaType);
        Assert.Contains("<title>", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task Get_Root_PageIsSelfContained()
    {
        // Päätös: sivu ei käytä ulkoisia kirjastoja eikä verkkopalveluja.
        var html = await Client.GetStringAsync("/");

        Assert.DoesNotContain("<script src", html);
        Assert.DoesNotContain("src=\"http", html);
        Assert.DoesNotContain("href=\"http", html);
        Assert.DoesNotContain("rel=\"stylesheet\"", html);
    }
}