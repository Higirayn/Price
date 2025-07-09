package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class PriceUpdate {
	@JsonProperty("product_id")
	private Long productId;
	
	@JsonProperty("manufacturer_name")
	private String manufacturerName;
	
	@JsonProperty("price")
	private Double price;
	
	public PriceUpdate() {
	}
	
	public PriceUpdate(Long productId, String manufacturerName, Double price) {
		this.productId = productId;
		this.manufacturerName = manufacturerName;
		this.price = price;
	}
	
	public Long getProductId() {
		return productId;
	}
	
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	
	public String getManufacturerName() {
		return manufacturerName;
	}
	
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}
	
	public Double getPrice() {
		return price;
	}
	
	public void setPrice(Double price) {
		this.price = price;
	}
	
	@Override
	public String toString() {
		return "PriceUpdate{" +
					   "productId=" + productId +
					   ", manufacturerName='" + manufacturerName + '\'' +
					   ", price=" + price +
					   '}';
	}
} 