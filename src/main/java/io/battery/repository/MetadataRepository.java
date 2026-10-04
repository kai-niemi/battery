package io.battery.repository;

public interface MetadataRepository {
    String databaseName();

    String databaseVersion();

    String databaseIsolation();
}
