package com.example.servlet;

import com.example.model.ApiResponse;
import com.example.model.AveragePrice;
import com.example.service.DatabaseService;
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

@WebServlet("/api/prices/average/*")
public class AveragePriceServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AveragePriceServlet.class);
	
	private final DatabaseService databaseService;
	private final ObjectMapper objectMapper;
	
	public AveragePriceServlet(DatabaseService databaseService) {
		this.databaseService = databaseService;
		this.objectMapper = new ObjectMapper();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		try {
			String pathInfo = request.getPathInfo();
			
			if(pathInfo == null || pathInfo.equals("/")) {
				List<AveragePrice> averagePrices = databaseService.getAllAveragePrices();
				logger.info("Получено {} средних цен", averagePrices.size());
				
				response.setStatus(HttpServletResponse.SC_OK);
				objectMapper.writeValue(response.getWriter(),
						new ApiResponse("success", "Средние цены успешно получены", averagePrices));
				
			} else {
				String productIdStr = pathInfo.substring(1);
				try {
					Long productId = Long.parseLong(productIdStr);
					AveragePrice averagePrice = databaseService.getAveragePrice(productId);
					
					if(averagePrice != null) {
						logger.info("Получена средняя цена для продукта {}", productId);
						response.setStatus(HttpServletResponse.SC_OK);
						objectMapper.writeValue(response.getWriter(),
								new ApiResponse("success", "Средняя цена успешно получена", averagePrice));
					} else {
						logger.warn("Средняя цена не найдена для продукта {}", productId);
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						objectMapper.writeValue(response.getWriter(),
								new ApiResponse("error", "Средняя цена не найдена для продукта " + productId, null));
					}
				} catch(NumberFormatException e) {
					logger.error("Некорректный ID продукта: {}", productIdStr);
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					objectMapper.writeValue(response.getWriter(),
							new ApiResponse("error", "Некорректный ID продукта: " + productIdStr, null));
				}
			}
			
		} catch(Exception e) {
			logger.error("Ошибка при получении средних цен", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			objectMapper.writeValue(response.getWriter(),
					new ApiResponse("error", "Внутренняя ошибка сервера: " + e.getMessage(), null));
		}
	}
	
	
}