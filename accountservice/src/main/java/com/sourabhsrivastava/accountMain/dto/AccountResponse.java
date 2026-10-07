package com.sourabhsrivastava.accountMain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sourabhsrivastava.accountMain.entity.AccountStatus;
import com.sourabhsrivastava.accountMain.entity.AccountType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class AccountResponse {

	
	private String Id;
	
	private String accountNumber;
	
	private String accountHolderName;
	
	private String email;
	
	private String phone;
	
	private AccountType accountType;
	
	private AccountStatus accountStatus;
	
	
	private BigDecimal balance;
	
	private BigDecimal dailyTransactionLimit;
	
	
	private LocalDateTime createdAt;
	
	
}
