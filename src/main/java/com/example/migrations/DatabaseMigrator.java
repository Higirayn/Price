package com.example.migrations;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;

public class DatabaseMigrator {
	public static void runMigrations(DataSource dataSource) {
		try(Connection conn = dataSource.getConnection()) {
			Liquibase liquibase = new Liquibase(
					"db/changelog/db.changelog-master.yaml",
					new ClassLoaderResourceAccessor(),
					DatabaseFactory.getInstance()
							.findCorrectDatabaseImplementation(new JdbcConnection(conn))
			);
			liquibase.update("");
			System.out.println("Миграции успешно выполнены!");
		} catch(Exception e) {
			throw new RuntimeException("Ошибка при выполнении миграций", e);
		}
	}
}