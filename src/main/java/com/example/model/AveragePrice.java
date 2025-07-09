package com.example.model;


public class AveragePrice {
	private Long productId;
	private Double averagePrice;
	private Integer offerCount;
	
	public AveragePrice() {
	}
	
	public AveragePrice(Long productId, Double averagePrice, Integer offerCount) {
		this.productId = productId;
		this.averagePrice = averagePrice;
		this.offerCount = offerCount;
	}
	
	public Long getProductId() {
		return productId;
	}
	
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	
	public Double getAveragePrice() {
		return averagePrice;
	}
	
	public void setAveragePrice(Double averagePrice) {
		this.averagePrice = averagePrice;
	}
	
	public Integer getOfferCount() {
		return offerCount;
	}
	
	public void setOfferCount(Integer offerCount) {
		this.offerCount = offerCount;
	}
	
	@Override
	public String toString() {
		return "AveragePrice{" +
					   "productId=" + productId +
					   ", averagePrice=" + averagePrice +
					   ", offerCount=" + offerCount +
					   '}';
	}
} 