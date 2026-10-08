package com.sourabhsrivastava.paymentdto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {

	@NotNull(message= "Account number is required")
	private String accountNumber;
	@NotNull(message= "Amount is required")
	@Positive(message= "Amount must be positive")
	private BigDecimal amount;
	@NotNull(message= "Account number is required")
	private String description;
	
	
}
