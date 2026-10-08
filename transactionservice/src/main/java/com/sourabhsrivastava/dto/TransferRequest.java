package com.sourabhsrivastava.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

	@NotBlank(message= "Sender account number is required")
	private String senderAccountNumber;
	@NotBlank(message= "Receiver account number is required")
	private String receiverAccountNumber;
	@NotBlank(message= "Amount is required")
	@Positive(message= "amountmust be positive")
	private BigDecimal amount;
	private String description;
	
	
}
