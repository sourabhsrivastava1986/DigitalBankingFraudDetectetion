package com.sourabhsrivastava.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "transacation")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscationEnitity {
	

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;
	
	@Column(nullable = false)
	private String senderAccountNumber;
	
	@Column(nullable = false)
	private String receiverAccountNumber;
	
	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;
	
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TransactionType transcationType;
	
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TranscationStatus transctionStatus;
	
	private String description;
	
	private String failureReaosn;
	
	private String  refrenceNumber;
	
	@CreationTimestamp
	private LocalDateTime createdAt;
	
	private LocalDateTime completedAt;
	
	
	
	
}
