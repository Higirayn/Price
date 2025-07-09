package com.example;

import com.example.service.DatabaseService;
import com.example.service.PriceProcessingService;
import com.example.servlet.AveragePriceServlet;
import com.example.servlet.PriceUpdateServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private static final int PORT = 8080;
	
	public static void main(String[] args) {
		try {
			logger.info("Starting Price Service application...");
			
			DatabaseService databaseService = new DatabaseService();
			PriceProcessingService processingService = new PriceProcessingService(databaseService);
			
			PriceUpdateServlet priceUpdateServlet = new PriceUpdateServlet(processingService);
			AveragePriceServlet averagePriceServlet = new AveragePriceServlet(databaseService);
			
			Server server = new Server(PORT);
			
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			server.setHandler(context);
			
			context.addServlet(new ServletHolder(priceUpdateServlet), "/api/prices/update");
			context.addServlet(new ServletHolder(averagePriceServlet), "/api/prices/average/*");
			
			server.setStopAtShutdown(true);
			
			server.start();
			logger.info("Price Service started on port {}", PORT);
			logger.info("API endpoints:");
			logger.info("  POST /api/prices/update - Update product prices");
			logger.info("  GET  /api/prices/average - Get all average prices");
			logger.info("  GET  /api/prices/average/{productId} - Get average price for specific product");
			
			server.join();
			
		} catch(Exception e) {
			logger.error("Failed to start Price Service", e);
			System.exit(1);
		}
	}
}