using System.Globalization;
using Microsoft.Data.Sqlite;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

// Tietokannan sijainti tulee asetuksesta ConnectionStrings:Traffic.
// Jos asetusta ei ole (tavallinen ajo), käytetään tiedostoa traffic.db.
// Luku tehdään vasta Build-kutsun jälkeen, jotta myös testien antamat asetukset ehtivät mukaan.
var connectionString = app.Configuration.GetConnectionString("Traffic") ?? "Data Source=traffic.db";

InitializeDatabase(connectionString);

app.MapGet("/", () => "Hello World!");

app.MapPost("/events", (EventDto e) =>
{
    var errors = Validate(e);
    if (errors.Count > 0)
    {
        return Results.BadRequest(new { errors });
    }

    using var connection = OpenConnection(connectionString);

    using var command = connection.CreateCommand();
    command.CommandText = """
        INSERT INTO events (tick, sim_time, point_id, point_name, car_id, direction, speed_kmh)
        VALUES ($tick, $simTime, $pointId, $pointName, $carId, $direction, $speedKmh);
        """;
    // Validate on jo varmistanut, ettei mikään kenttä ole null
    command.Parameters.AddWithValue("$tick", e.Tick);
    command.Parameters.AddWithValue("$simTime", e.SimTime);
    command.Parameters.AddWithValue("$pointId", e.PointId);
    command.Parameters.AddWithValue("$pointName", e.PointName);
    command.Parameters.AddWithValue("$carId", e.CarId);
    command.Parameters.AddWithValue("$direction", e.Direction);
    command.Parameters.AddWithValue("$speedKmh", e.SpeedKmh);
    command.ExecuteNonQuery();

    return Results.Created("/events", e);
});

app.MapGet("/events", () =>
{
    var events = new List<StoredEvent>();

    using var connection = OpenConnection(connectionString);

    using var command = connection.CreateCommand();
    command.CommandText = """
        SELECT id, tick, sim_time, point_id, point_name, car_id, direction, speed_kmh, received_at
        FROM events
        ORDER BY id DESC
        LIMIT 100;
        """;

    using var reader = command.ExecuteReader();
    while (reader.Read())
    {
        events.Add(new StoredEvent(
            reader.GetInt32(0),
            reader.GetInt32(1),
            reader.GetString(2),
            reader.GetInt32(3),
            reader.GetString(4),
            reader.GetInt32(5),
            reader.GetString(6),
            reader.GetInt32(7),
            reader.GetString(8)));
    }

    return Results.Ok(events);
});

app.Run();

// Palauttaa listan virheistä. Tyhjä lista tarkoittaa, että tapahtuma on kelvollinen.
static List<string> Validate(EventDto e)
{
    var errors = new List<string>();

    if (e.Tick is null) errors.Add("tick is required");
    else if (e.Tick < 0) errors.Add("tick must be 0 or greater");

    if (e.SimTime is null)
    {
        errors.Add("simTime is required");
    }
    else if (!DateTime.TryParseExact(e.SimTime, "yyyy-MM-dd'T'HH:mm:ss",
                 CultureInfo.InvariantCulture, DateTimeStyles.None, out _))
    {
        errors.Add("simTime must be in format yyyy-MM-ddTHH:mm:ss");
    }

    if (e.PointId is null) errors.Add("pointId is required");
    else if (e.PointId < 1) errors.Add("pointId must be 1 or greater");

    if (e.PointName is null) errors.Add("pointName is required");
    else if (!IsValidPointName(e.PointName)) errors.Add(
        "pointName must be 1-30 characters: letters, digits, spaces and hyphens, " +
        "with no space at the start or end");

    if (e.CarId is null) errors.Add("carId is required");
    else if (e.CarId < 1) errors.Add("carId must be 1 or greater");

    if (e.Direction is null) errors.Add("direction is required");
    else if (e.Direction is not ("NORTH" or "EAST" or "SOUTH" or "WEST"))
        errors.Add("direction must be NORTH, EAST, SOUTH or WEST");

     if (e.SpeedKmh is null) errors.Add("speedKmh is required");

    return errors;
}

static bool IsValidPointName(string name)
{
    if (name.Length is < 1 or > 30) return false;
    if (name[0] == ' ' || name[^1] == ' ') return false;

    foreach (var c in name)
    {
        if (!char.IsLetterOrDigit(c) && c != ' ' && c != '-') return false;
    }
    return true;
}

static SqliteConnection OpenConnection(string connectionString)
{
    var connection = new SqliteConnection(connectionString);
    connection.Open();

    // synchronous on yhteyskohtainen asetus, joten se asetetaan jokaiselle yhteydelle
    using var command = connection.CreateCommand();
    command.CommandText = "PRAGMA synchronous = NORMAL;";
    command.ExecuteNonQuery();

    return connection;
}

static void InitializeDatabase(string connectionString)
{
    using var connection = new SqliteConnection(connectionString);
    connection.Open();

    // WAL-tila säilyy tietokantatiedostossa, joten riittää asettaa kerran
    using (var pragma = connection.CreateCommand())
    {
        pragma.CommandText = "PRAGMA journal_mode = WAL;";
        pragma.ExecuteNonQuery();
    }

    using var command = connection.CreateCommand();
    command.CommandText = """
        CREATE TABLE IF NOT EXISTS events (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            tick        INTEGER NOT NULL,
            sim_time    TEXT    NOT NULL,
            point_id    INTEGER NOT NULL,
            point_name  TEXT    NOT NULL,
            car_id      INTEGER NOT NULL,
            direction   TEXT    NOT NULL,
            speed_kmh   INTEGER NOT NULL,
            received_at TEXT    NOT NULL DEFAULT (datetime('now'))
        );

        CREATE INDEX IF NOT EXISTS idx_events_point_time ON events (point_id, sim_time);
        """;
    command.ExecuteNonQuery();
}

record EventDto(int? Tick, string? SimTime, int? PointId, string? PointName,
                int? CarId, string? Direction, int? SpeedKmh);

record StoredEvent(int Id, int Tick, string SimTime, int PointId, string PointName,
                   int CarId, string Direction, int SpeedKmh, string ReceivedAt);

// Tekee Program-luokasta näkyvän testiprojektille (WebApplicationFactory<Program>).
// Ilman tätä rivi luokka olisi piilossa, koska ylimmän tason lauseet luovat sen automaattisesti ei-julkisena.
public partial class Program { }