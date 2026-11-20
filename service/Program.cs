using Microsoft.Data.Sqlite;

const string connectionString = "Data Source=traffic.db";

InitializeDatabase(connectionString);

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapGet("/", () => "Hello World!");

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