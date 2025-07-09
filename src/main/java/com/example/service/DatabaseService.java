package com.example.service;

import com.example.model.AveragePrice;
import com.example.model.PriceUpdate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseService {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
	
	private final DataSource dataSource;
	
	public DatabaseService() {
		this.dataSource = createDataSource();
	}
	
	private DataSource createDataSource() {
		try {
			Properties props = loadConfiguration();
			
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(props.getProperty("db.url"));
			config.setUsername(props.getProperty("db.username"));
			config.setPassword(props.getProperty("db.password"));
			
			config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max-size")));
			config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min-idle")));
			config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connection-timeout")));
			config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idle-timeout")));
			config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.max-lifetime")));
			
			return new HikariDataSource(config);
			
		} catch(Exception e) {
			throw new RuntimeException("Failed to initialize datasource", e);
		}
	}
	
	private Properties loadConfiguration() throws IOException {
		Properties props = new Properties();
		
		try(InputStream is = getClass().getClassLoader()
									 .getResourceAsStream("application.properties")) {
			if(is != null) {
				props.load(is);
			}
		}
		
		overrideWithEnvVars(props);
		
		return props;
	}
	
	private void overrideWithEnvVars(Properties props) {
		props.replaceAll((key, value) -> {
			String envKey = key.toString().replace('.', '_').toUpperCase();
			return System.getenv().getOrDefault(envKey, value.toString());
		});
	}
	
	
	public void updatePrice(PriceUpdate priceUpdate) {
		try(Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			try {
				String upsertSql = """
						    INSERT INTO product_prices (product_id, manufacturer_name, price, updated_at)
						    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
						    ON CONFLICT (product_id, manufacturer_name)
						    DO UPDATE SET price = EXCLUDED.price, updated_at = CURRENT_TIMESTAMP
						""";
				
				try(PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
					stmt.setLong(1, priceUpdate.getProductId());
					stmt.setString(2, priceUpdate.getManufacturerName());
					stmt.setDouble(3, priceUpdate.getPrice());
					stmt.executeUpdate();
				}
				
				recalculateAveragePrice(conn, priceUpdate.getProductId());
				
				conn.commit();
				logger.info("Price updated successfully for product {} from manufacturer {}",
						priceUpdate.getProductId(), priceUpdate.getManufacturerName());
				
			} catch(SQLException e) {
				conn.rollback();
				logger.error("Failed to update price", e);
				throw e;
			}
		} catch(SQLException e) {
			logger.error("Database error during price update", e);
			throw new RuntimeException("Failed to update price", e);
		}
	}
	
	private void recalculateAveragePrice(Connection conn, Long productId) throws SQLException {
		String avgSql = """
				    SELECT AVG(price) as avg_price, COUNT(*) as offer_count
				    FROM product_prices
				    WHERE product_id = ?
				""";
		
		try(PreparedStatement stmt = conn.prepareStatement(avgSql)) {
			stmt.setLong(1, productId);
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				Double avgPrice = rs.getDouble("avg_price");
				Integer offerCount = rs.getInt("offer_count");
				
				String upsertAvgSql = """
						    INSERT INTO average_prices (product_id, average_price, offer_count, updated_at)
						    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
						    ON CONFLICT (product_id)
						    DO UPDATE SET
						        average_price = EXCLUDED.average_price,
						        offer_count = EXCLUDED.offer_count,
						        updated_at = CURRENT_TIMESTAMP
						""";
				
				try(PreparedStatement avgStmt = conn.prepareStatement(upsertAvgSql)) {
					avgStmt.setLong(1, productId);
					avgStmt.setDouble(2, avgPrice);
					avgStmt.setInt(3, offerCount);
					avgStmt.executeUpdate();
				}
			}
		}
	}
	
	public AveragePrice getAveragePrice(Long productId) {
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT product_id, average_price, offer_count FROM average_prices WHERE product_id = ?")) {
			
			stmt.setLong(1, productId);
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				return new AveragePrice(
						rs.getLong("product_id"),
						rs.getDouble("average_price"),
						rs.getInt("offer_count")
				);
			}
			return null;
		} catch(SQLException e) {
			logger.error("Failed to get average price for product {}", productId, e);
			throw new RuntimeException("Failed to get average price", e);
		}
	}
	
	public List<AveragePrice> getAllAveragePrices() {
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT product_id, average_price, offer_count FROM average_prices ORDER BY product_id");
			ResultSet rs = stmt.executeQuery()) {
			
			List<AveragePrice> prices = new ArrayList<>();
			while(rs.next()) {
				prices.add(new AveragePrice(
						rs.getLong("product_id"),
						rs.getDouble("average_price"),
						rs.getInt("offer_count")
				));
			}
			return prices;
		} catch(SQLException e) {
			logger.error("Failed to get all average prices", e);
			throw new RuntimeException("Failed to get average prices", e);
		}
	}
	
	public void close() {
		if(dataSource instanceof HikariDataSource) {
			((HikariDataSource) dataSource).close();
		}
	}
}