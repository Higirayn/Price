package com.example.servlet;

import com.example.model.PriceUpdate;
import com.example.service.PriceProcessingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@WebServlet("/api/prices/update")
public class PriceUpdateServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(PriceUpdateServlet.class);
	
	private final PriceProcessingService processingService;
	private final ObjectMapper objectMapper;
	
	public PriceUpdateServlet(PriceProcessingService processingService) {
		this.processingService = processingService;
		this.objectMapper = new ObjectMapper();
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		try {
			List<PriceUpdate> priceUpdates = objectMapper.readValue(
					request.getInputStream(),
					new TypeReference<List<PriceUpdate>>() {
					}
			);
			
			logger.info("Получено {} обновлений цен", priceUpdates.size());
			
			validatePriceUpdates(priceUpdates);
			
			CompletableFuture<Void> processingFuture = processingService.processPriceUpdates(priceUpdates);
			
			response.setStatus(HttpServletResponse.SC_ACCEPTED);
			objectMapper.writeValue(response.getWriter(),
					new ApiResponse("success", "Обновления цен приняты в обработку", priceUpdates.size()));
			
			processingFuture.whenComplete((result, throwable) -> {
				if(throwable != null) {
					logger.error("Ошибка при обработке обновлений цен", throwable);
				} else {
					logger.info("Обработка обновлений цен успешно завершена");
				}
			});
			
		} catch(Exception e) {
			logger.error("Ошибка при обработке запроса на обновление цен", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			objectMapper.writeValue(response.getWriter(),
					new ApiResponse("error", "Некорректный запрос: " + e.getMessage(), null));
		}
	}
	
	private void validatePriceUpdates(List<PriceUpdate> priceUpdates) {
		if(priceUpdates == null || priceUpdates.isEmpty()) {
			throw new IllegalArgumentException("Список обновлений цен не может быть пустым");
		}
		
		for(PriceUpdate update : priceUpdates) {
			if(update.getProductId() == null) {
				throw new IllegalArgumentException("ID продукта не может быть пустым");
			}
			if(update.getManufacturerName() == null || update.getManufacturerName().trim().isEmpty()) {
				throw new IllegalArgumentException("Название производителя не может быть пустым");
			}
			if(update.getPrice() == null || update.getPrice() <= 0) {
				throw new IllegalArgumentException("Цена должна быть положительным числом");
			}
		}
	}
	
	private static class ApiResponse {
		private final String status;
		private final String message;
		private final Object data;
		
		public ApiResponse(String status, String message, Object data) {
			this.status = status;
			this.message = message;
			this.data = data;
		}
		
		public String getStatus() {
			return status;
		}
		
		public String getMessage() {
			return message;
		}
		
		public Object getData() {
			return data;
		}
	}
}