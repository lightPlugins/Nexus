package io.nexstudios.nexus.bukkit.database.api;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public interface NexusDatabaseService {
    DataSource getDataSource();

    default boolean isHealthy() {
        try (Connection c = getDataSource().getConnection()) {
            return c != null && c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    default <T> T inTransaction(Callable<T> work) throws Exception {
        try (Connection c = getDataSource().getConnection()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.call();
                c.commit();
                return result;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(old);
            }
        }
    }

    default void withConnection(Consumer<Connection> consumer) throws Exception {
        try (Connection c = getDataSource().getConnection()) {
            consumer.accept(c);
        }
    }

    default <T> T withConnection(Function<Connection, T> fn) throws Exception {
        try (Connection c = getDataSource().getConnection()) {
            return fn.apply(c);
        }
    }
}