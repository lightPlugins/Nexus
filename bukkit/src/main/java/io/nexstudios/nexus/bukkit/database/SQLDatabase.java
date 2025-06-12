package io.nexstudios.nexus.bukkit.database;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.database.model.DatabaseTypes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SQLDatabase {

    protected final Nexus plugin;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    SQLDatabase(Nexus plugin) {
        this.plugin = plugin;
        startMonitoring();
    }

    public abstract DatabaseTypes getDatabaseType();
    public abstract void connect();
    public abstract void close();
    public abstract Connection getConnection();

    /**
     * Executes a SQL statement asynchronously and returns the amount of affected lines.
     * If the result is above 0, the statement was successful.
     * The following SQL functions can be used:
     * <li>COUNT: Returns the number of rows that match a specified condition.</li>
     * <li>SUM: Returns the total sum of a numeric column.</li>
     * <li>AVG: Returns the average value of a numeric column.</li>
     * <li>MAX: Returns the maximum value in a set.</li>
     * <li>MIN: Returns the minimum value in a set.</li>
     * <li>UPDATE: Returns the number of rows affected.</li>
     * <li>DELETE: Returns the number of rows affected.</li>
     * <li>INSERT: Returns the number of rows affected (usually 1 for a single insert).</li>
     * @param sql The SQL statement
     * @param replacements The replacements for the statement
     * @return The amount of affected lines
     */
    public CompletableFuture<Integer> executeSqlFuture(String sql, Object... replacements) {

        CompletableFuture<Integer> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements)) {
                int affectedLines = statement.executeUpdate();
                future.complete(affectedLines);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public boolean checkConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Executes a SQL query asynchronously and returns a list of objects.
     * <ul>Examples of SQL queries:</ul>
     * <li>querySqlFuture("SELECT name FROM users WHERE age > ?", "name", 18)</li>
     * <li>querySqlFuture("SELECT * FROM users WHERE name = ?", "*", "John")</li>
     * <li>querySqlFuture("SELECT * FROM users WHERE name = ? AND age = ?", "*", "John", 18)</li>
     * @param sql The SQL query
     * @param column The needed column to search for
     * @param replacements The replacements for the query
     * @return A CompletableFuture containing the list of objects
     * @throws RuntimeException if the query could not be executed
     */
    public CompletableFuture<List<Object>> querySqlFuture(String sql, String column, Object... replacements) {
        CompletableFuture<List<Object>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements); ResultSet resultSet = statement.executeQuery()) {
                List<Object> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(resultSet.getObject(column)); // Assuming you want the specified column
                }
                future.complete(results);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Executes a SQL query asynchronously and returns a HashMap of objects.
     * <ul>sql query examples:</ul>
     * <li>querySqlFuture("SELECT id, name FROM users WHERE age > ?", "id", "name", 18)</li>
     * @param sql The SQL query
     * @param keyColumn The column to use as keys
     * @param valueColumn The column to use as values
     * @param replacements The replacements for the query
     * @return A CompletableFuture containing the HashMap of objects
     * @throws RuntimeException if the query could not be executed
     */
    public CompletableFuture<HashMap<Object, Object>> querySqlFuture(String sql, String keyColumn, String valueColumn, Object... replacements) {
        CompletableFuture<HashMap<Object, Object>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements); ResultSet resultSet = statement.executeQuery()) {
                HashMap<Object, Object> results = new HashMap<>();
                while (resultSet.next()) {
                    results.put(resultSet.getObject(keyColumn), resultSet.getObject(valueColumn));
                }
                future.complete(results);
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(new RuntimeException("[LightCore] Could not execute SQL query", e));
            }
        });
        return future;
    }

    /**
     * Prepares a statement with the given SQL query and replacements.
     * @param connection The connection to use
     * @param sql The SQL query
     * @param replacements The replacements for the query
     * @return The prepared statement
     */
    private PreparedStatement prepareStatement(Connection connection, String sql, Object... replacements) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(sql);
            this.replaceQueryParameters(statement,replacements);

            return statement;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("[LightCore] Could not prepare/read SQL statement:" + sql, e);
        }
    }

    /**
     * Replaces the query parameters in the prepared statement.
     * @param statement The statement to replace the parameters in
     * @param replacements The replacements for the statement
     */
    private void replaceQueryParameters(PreparedStatement statement, Object[] replacements) {
        if (replacements != null) {
            for (int i = 0; i < replacements.length; i++) {
                int position = i + 1;
                Object value = replacements[i];
                try {
                    statement.setObject(position, value);
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Unable to set query parameter at position " + position + " to " +
                            value + " for query: " + statement, e);
                }
            }
        }
    }

    /**
     * Starts monitoring the database connection.
     * Logs the status of the connection at regular intervals.
     */
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {

            Date date = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
            String currentTime = simpleDateFormat.format(date);

            try (Connection connection = getConnection()) {
                if (connection != null && connection.isClosed()) {
                    plugin.getNexusLogger().error(List.of(
                            "Database connection is inactive.",
                            "Attempting to reconnect..."
                    ));
                    Nexus.getInstance().getSqlDatabase().connect();
                    if(!connection.isClosed()) {
                        plugin.getNexusLogger().info(List.of("Database connection has been re-established."));
                    }
                }
            } catch (SQLException e) {
                plugin.getNexusLogger().error(List.of(
                        "Error while checking database connection: " + e.getMessage(),
                        "Attempting to reconnect..."
                ));
                Nexus.getInstance().getSqlDatabase().connect();
                if(getConnection() != null) {
                    plugin.getNexusLogger().info("Database connection has been re-established.");
                }
            }
        }, 5, 5, TimeUnit.MINUTES); // Adjust the interval as needed
    }

}
