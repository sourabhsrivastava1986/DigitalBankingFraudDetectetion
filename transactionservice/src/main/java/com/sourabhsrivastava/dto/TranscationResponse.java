package com.sourabhsrivastava.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.sourabhsrivastava.entity.TransactionType;
import com.sourabhsrivastava.entity.TranscationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscationResponse {
	
	private String id;
	
	private String senderAccountNumber;
	
	private String receiverAccountNumber;
	
	private BigDecimal amount;
	
	private TransactionType transcationType;
	
	private TranscationStatus transctionStatus;
	
	private String description;
	
	private String failureReaosn;
	
	private String  refrenceNumber;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime completedAt;
	

}
