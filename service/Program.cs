using Microsoft.Data.Sqlite;

const string connectionString = "Data Source=traffic.db";

InitializeDatabase(connectionString);

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapGet("/", () => "Hello World!");

app.MapPost("/events", (EventDto e) =>
{
    if (e.Direction is not ("NORTH" or "EAST" or "SOUTH" or "WEST"))
    {
        return Results.BadRequest("direction must be NORTH, EAST, SOUTH or WEST");
    }

    using var connection = new SqliteConnection(connectionString);
    connection.Open();

    using var command = connection.CreateCommand();
    command.CommandText = """
        INSERT INTO events (tick, point_id, car_id, direction)
        VALUES ($tick, $pointId, $carId, $direction);
        """;
    command.Parameters.AddWithValue("$tick", e.Tick);
    command.Parameters.AddWithValue("$pointId", e.PointId);
    command.Parameters.AddWithValue("$carId", e.CarId);
    command.Parameters.AddWithValue("$direction", e.Direction);
    command.ExecuteNonQuery();

    return Results.Created("/events", e);
});

app.MapGet("/events", () =>
{
    var events = new List<StoredEvent>();

    using var connection = new SqliteConnection(connectionString);
    connection.Open();

    using var command = connection.CreateCommand();
    command.CommandText = """
        SELECT id, tick, point_id, car_id, direction, received_at
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
            reader.GetInt32(2),
            reader.GetInt32(3),
            reader.GetString(4),
            reader.GetString(5)));
    }

    return Results.Ok(events);
});

app.Run();

static void InitializeDatabase(string connectionString)
{
    using var connection = new SqliteConnection(connectionString);
    connection.Open();

    using var command = connection.CreateCommand();
    command.CommandText = """
        CREATE TABLE IF NOT EXISTS events (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            tick        INTEGER NOT NULL,
            point_id    INTEGER NOT NULL,
            car_id      INTEGER NOT NULL,
            direction   TEXT    NOT NULL,
            received_at TEXT    NOT NULL DEFAULT (datetime('now'))
        );
        """;
    command.ExecuteNonQuery();
}

record EventDto(int Tick, int PointId, int CarId, string Direction);
record StoredEvent(int Id, int Tick, int PointId, int CarId, string Direction, string ReceivedAt);