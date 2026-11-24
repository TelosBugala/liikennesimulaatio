using Microsoft.Data.Sqlite;

// Tilastokyselyt. Luvut lasketaan SQL:llä tallennetuista tapahtumista, ja ne käyttävät
// simulaatioaikaa (sim_time), eivät vastaanottoaikaa (received_at).
// Luokka saa valmiin yhteyden parametrina, koska yhteyden avaus (OpenConnection) on Program.cs:ssä.
static class Statistics
{
    // Pisteen uusin nimi simulaatioajan mukaan (samalla ajalla myöhemmin tallennettu voittaa).
    // Kysely viittaa ulomman kyselyn tauluun nimellä "e", joten sitä käytetään vain kyselyissä,
    // joissa events-taulun alias on e. Yhteinen teksti takaa, että kummassakin tilastossa
    // pisteen nimi valitaan samalla säännöllä.
    private const string LatestPointName = """
        (SELECT e2.point_name
         FROM events e2
         WHERE e2.point_id = e.point_id
         ORDER BY e2.sim_time DESC, e2.id DESC
         LIMIT 1)
        """;

    internal static readonly string PointSummariesSql = $"""
        SELECT
            e.point_id,
            {LatestPointName}               AS point_name,
            COUNT(*)                        AS pass_count,
            ROUND(AVG(e.speed_kmh), 1)      AS average_speed,
            SUM(e.direction = 'NORTH')      AS north,
            SUM(e.direction = 'EAST')       AS east,
            SUM(e.direction = 'SOUTH')      AS south,
            SUM(e.direction = 'WEST')       AS west
        FROM events e
        GROUP BY e.point_id
        ORDER BY e.point_id;
        """;

    internal static readonly string HourlyCountsSql = $"""
        SELECT
            e.point_id,
            {LatestPointName}                                AS point_name,
            strftime('%Y-%m-%dT%H:00:00', e.sim_time)        AS hour_start,
            COUNT(*)                                         AS pass_count
        FROM events e
        GROUP BY e.point_id, hour_start
        ORDER BY e.point_id, hour_start;
        """;

    public static List<PointSummary> GetPointSummaries(SqliteConnection connection)
    {
        var result = new List<PointSummary>();

        using var command = connection.CreateCommand();
        command.CommandText = PointSummariesSql;

        using var reader = command.ExecuteReader();
        while (reader.Read())
        {
            result.Add(new PointSummary(
                reader.GetInt32(0),
                reader.GetString(1),
                reader.GetInt32(2),
                reader.GetDouble(3),
                new DirectionCounts(
                    reader.GetInt32(4),
                    reader.GetInt32(5),
                    reader.GetInt32(6),
                    reader.GetInt32(7))));
        }

        return result;
    }

    public static List<HourlyCount> GetHourlyCounts(SqliteConnection connection)
    {
        var result = new List<HourlyCount>();

        using var command = connection.CreateCommand();
        command.CommandText = HourlyCountsSql;

        using var reader = command.ExecuteReader();
        while (reader.Read())
        {
            result.Add(new HourlyCount(
                reader.GetInt32(0),
                reader.GetString(1),
                reader.GetString(2),
                reader.GetInt32(3)));
        }

        return result;
    }
}

record DirectionCounts(int North, int East, int South, int West);

record PointSummary(int PointId, string PointName, int PassCount, double AverageSpeedKmh,
                    DirectionCounts Directions);

record HourlyCount(int PointId, string PointName, string HourStart, int PassCount);