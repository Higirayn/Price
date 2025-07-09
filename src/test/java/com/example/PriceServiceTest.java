package com.example;

import com.example.model.AveragePrice;
import com.example.model.PriceUpdate;
import com.example.service.DatabaseService;
import com.example.service.PriceProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class PriceServiceTest {
	
	private DatabaseService databaseService;
	private PriceProcessingService processingService;
	
	@BeforeEach
	void setUp() {
		databaseService = new DatabaseService();
		processingService = new PriceProcessingService(databaseService);
	}
	
	@AfterEach
	void tearDown() {
		if(processingService != null) {
			processingService.shutdown();
		}
		if(databaseService != null) {
			databaseService.close();
		}
	}
	
	@Test
	void testPriceUpdateAndAverageCalculation() {
		List<PriceUpdate> priceUpdates = Arrays.asList(
				new PriceUpdate(1L, "Производитель A", 100.0),
				new PriceUpdate(2L, "Производитель B", 150.0),
				new PriceUpdate(1L, "Производитель C", 200.0)
		);
		
		CompletableFuture<Void> future = processingService.processPriceUpdates(priceUpdates);
		
		future.join();
		
		AveragePrice avgPrice1 = databaseService.getAveragePrice(1L);
		AveragePrice avgPrice2 = databaseService.getAveragePrice(2L);
		
		assertNotNull(avgPrice1);
		assertNotNull(avgPrice2);
		
		assertEquals(150.0, avgPrice1.getAveragePrice(), 0.01);
		assertEquals(2, avgPrice1.getOfferCount());
		
		assertEquals(150.0, avgPrice2.getAveragePrice(), 0.01);
		assertEquals(1, avgPrice2.getOfferCount());
	}
	
	@Test
	void testConcurrentPriceUpdates() {
		List<PriceUpdate> updates1 = Arrays.asList(
				new PriceUpdate(3L, "Производитель A", 100.0),
				new PriceUpdate(3L, "Производитель B", 200.0)
		);
		
		List<PriceUpdate> updates2 = Arrays.asList(
				new PriceUpdate(4L, "Производитель C", 150.0),
				new PriceUpdate(4L, "Производитель D", 250.0)
		);
		
		CompletableFuture<Void> future1 = processingService.processPriceUpdates(updates1);
		CompletableFuture<Void> future2 = processingService.processPriceUpdates(updates2);
		
		CompletableFuture.allOf(future1, future2).join();
		
		AveragePrice avgPrice3 = databaseService.getAveragePrice(3L);
		AveragePrice avgPrice4 = databaseService.getAveragePrice(4L);
		
		assertNotNull(avgPrice3);
		assertNotNull(avgPrice4);
		
		assertEquals(150.0, avgPrice3.getAveragePrice(), 0.01);
		assertEquals(2, avgPrice3.getOfferCount());
		
		assertEquals(200.0, avgPrice4.getAveragePrice(), 0.01);
		assertEquals(2, avgPrice4.getOfferCount());
	}
	
	@Test
	void testGetAllAveragePrices() {
		List<PriceUpdate> priceUpdates = Arrays.asList(
				new PriceUpdate(5L, "Производитель A", 100.0),
				new PriceUpdate(6L, "Производитель B", 200.0)
		);
		
		processingService.processPriceUpdates(priceUpdates).join();
		
		List<AveragePrice> allPrices = databaseService.getAllAveragePrices();
		
		assertNotNull(allPrices);
		assertFalse(allPrices.isEmpty());
		
		boolean foundPrice5 = allPrices.stream()
									  .anyMatch(p -> p.getProductId().equals(5L) && p.getAveragePrice() == 100.0);
		boolean foundPrice6 = allPrices.stream()
									  .anyMatch(p -> p.getProductId().equals(6L) && p.getAveragePrice() == 200.0);
		
		assertTrue(foundPrice5, "Average price for product 5 not found");
		assertTrue(foundPrice6, "Average price for product 6 not found");
	}
} 