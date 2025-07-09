package com.example.model;

public class ApiResponse {
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
