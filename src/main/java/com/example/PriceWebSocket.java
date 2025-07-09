package com.example;

import com.example.model.PriceUpdate;
import com.example.model.AveragePrice;
import com.example.service.DatabaseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@ServerEndpoint("/ws/price")
public class PriceWebSocket {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DatabaseService dbService = new DatabaseService();
    private static final Map<Session, Void> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session, null);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(message);
            String action = root.path("action").asText();
            switch (action) {
                case "updatePrice":
                    PriceUpdate priceUpdate = objectMapper.treeToValue(root.path("data"), PriceUpdate.class);
                    dbService.updatePrice(priceUpdate);
                    sendResponse(session, "updatePrice", "success", null);
                    break;
                case "getAveragePrice":
                    Long productId = root.path("data").path("productId").asLong();
                    AveragePrice avg = dbService.getAveragePrice(productId);
                    sendResponse(session, "getAveragePrice", "success", avg);
                    break;
                default:
                    sendResponse(session, action, "error", "Unknown action");
            }
        } catch (Exception e) {
            sendResponse(session, "error", "error", e.getMessage());
        }
    }

    private void sendResponse(Session session, String action, String status, Object data) throws IOException {
        String resp = objectMapper.createObjectNode()
                .put("action", action)
                .put("status", status)
                .set("data", objectMapper.valueToTree(data))
                .toString();
        session.getBasicRemote().sendText(resp);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Можно добавить логирование
    }
} 