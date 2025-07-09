package com.example.service;

import com.example.model.PriceUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriceProcessingService {
	private static final Logger logger = LoggerFactory.getLogger(PriceProcessingService.class);
	
	private final DatabaseService databaseService;
	private final ExecutorService executorService;
	
	public PriceProcessingService(DatabaseService databaseService) {
		this.databaseService = databaseService;
		this.executorService = Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors(),
				new ThreadFactory() {
					private final AtomicInteger counter = new AtomicInteger(1);
					
					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r, "price-processor-" + counter.getAndIncrement());
						thread.setDaemon(true);
						return thread;
					}
				}
		);
	}
	
	public CompletableFuture<Void> processPriceUpdates(List<PriceUpdate> priceUpdates) {
		logger.info("Обработка {} обновлений цен", priceUpdates.size());
		
		List<CompletableFuture<Void>> futures = priceUpdates.stream()
														.map(this :: processPriceUpdateAsync)
														.toList();
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
					   .whenComplete((result, throwable) -> {
						   if(throwable != null) {
							   logger.error("Ошибка при обработке обновлений цен", throwable);
						   } else {
							   logger.info("Успешно обработано {} обновлений цен", priceUpdates.size());
						   }
					   });
	}
	
	private CompletableFuture<Void> processPriceUpdateAsync(PriceUpdate priceUpdate) {
		return CompletableFuture.runAsync(() -> {
			try {
				databaseService.updatePrice(priceUpdate);
				logger.debug("Обработано обновление цены: {}", priceUpdate);
			} catch(Exception e) {
				logger.error("Ошибка при обработке обновления цены: {}", priceUpdate, e);
				throw new RuntimeException("Не удалось обработать обновление цены", e);
			}
		}, executorService);
	}
	
	public void shutdown() {
		logger.info("Завершение работы сервиса обработки цен");
		executorService.shutdown();
	}
}