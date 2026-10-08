package com.sourabhsrivastava.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCompletedEvent {

	private String transcationId;
	private String senderAccountNumber;
	private String  receiverAccountNUmber;
	private BigDecimal amount;
	private String description;
	
}
