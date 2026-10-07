package com.sourabhsrivastava.accountMain.accountservice;

import java.math.BigDecimal;
import java.security.SecureRandom;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sourabhsrivastava.accountMain.accountrepository.AccountRepository;
import com.sourabhsrivastava.accountMain.dto.AccountResponse;
import com.sourabhsrivastava.accountMain.dto.CreateAccountRequest;
import com.sourabhsrivastava.accountMain.entity.Account;
import com.sourabhsrivastava.accountMain.entity.AccountStatus;
import com.sourabhsrivastava.accountMain.entity.AccountType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

	
	public final AccountRepository accountRepository;
	private static SecureRandom  secureRandom;
	
	  public AccountResponse createAccount(CreateAccountRequest request) 
	  {
		  log.info("create account for :{}", request.getEmail());
		  
		  if(accountRepository.existsByEmail(request.getEmail()))
		  {
			  throw new RuntimeException("Account already exists for email : "+ request.getEmail());
		  }
		  
		  Account account = new Account();
		  
		  account.setAccountHolderName(request.getAccountHolderName());
		  account.setEmail(request.getEmail());
		  account.setPhone(request.getPhone());
		  account.setAccountType(request.getAccountType());
		  account.setAccountStatus(AccountStatus.ACTIVE);
		  account.setBalance(request.getInitialDeposite());
		  // genearte account number ,should be unique, must 12 digit
		  
		  account.setAccountNumber(generateAccountNumber());
		  //here dailyr transactionbased on account type 
		  //saving should be 100,000
		  // and current and FD should be 500,000
		  account.setDailyTransactionLimit(
				  request.getAccountType() == AccountType.SAVING
				                              ? new BigDecimal("100000")
				                            		  :new BigDecimal("500000")
				  );
		  
		  Account savedAccount= accountRepository.save(account);
		  log.info("Account created :{}", savedAccount.getAccountNumber());
		  
		  
		return mapToResponse(savedAccount);
	  }

	  public AccountResponse getAccount(String accountNumber)
	  {
		  Account account = accountRepository.findByAccountNumber(accountNumber)
				  .orElseThrow(()-> new RuntimeException("Account not found"));
				  
		  
		  return mapToResponse(account);
		  
	  }
	  
	  public BigDecimal getBalance(String accountNumber)
	  {
		  Account account = accountRepository.findByAccountNumber(accountNumber)
				  .orElseThrow(()-> new RuntimeException("Account not found"));
		  
		  return account.getBalance();
		  
	  }
	  
	  /**
	   * Block Account = called by fraud detection service by Kafka
	   * @param accountNumber
	   */
	  public void blockedAccount(String accountNumber)
	  {
		  log.info("Blocking Account : {}", accountNumber);
		  Account account = accountRepository.findByAccountNumber(accountNumber)
				  .orElseThrow(()-> new RuntimeException("Account not found"));
		  
		  account.setAccountStatus(AccountStatus.BLOCKED);
		  accountRepository.save(account);
		  log.info("Account is blocked : {}", accountNumber);
	  }
	  
	  /**
	   * deduct balance from sender account
	   * called by Transcation service
	   * @param accountNumber
	   * @param amount
	   */
	  
	  public void deductBalance(String accountNumber, BigDecimal amount)
	  {
		  Account account = accountRepository.findByAccountNumber(accountNumber)
				  .orElseThrow(()-> new RuntimeException("Account not found"));
		  if(account.getAccountStatus() != AccountStatus.ACTIVE)
		  {
			  throw new RuntimeException("Account is not Active");
		  }
		  
		  if(account.getBalance().compareTo(amount) < 0 )
		  {
			  throw new RuntimeException("Insufficient funds for account : " +accountNumber);
		  }
		  
		  account.setBalance(account.getBalance().subtract(amount));
		  accountRepository.save(account);
		  log.info("Balance update, New Balance : {}", account.getBalance());
	  }
	  
	  /**
	   * Credite balnace
	   * called by transaction Service by Kafka
	   * @param accountNumber
	   * @param amount
	   */
	  
	  public void creditBalance(String accountNumber, BigDecimal amount)
	  {
		  log.info("Crediting {} to account {}", amount, accountNumber);
		  Account account = accountRepository.findByAccountNumber(accountNumber)
				  .orElseThrow(()-> new RuntimeException("Account not found"));
		  
		  account.setBalance(account.getBalance().add(amount));
		  accountRepository.save(account);
		  
		  log.info("Balance credited, new Balance : {}" , account.getBalance());
		  
		  
	  }
	  
	  
	  private String generateAccountNumber()
	  {
		  String accountNumber;
		  
		  do {
			  /**
			   * account Number should be i 12 digit os this will from 0 to 9999 .. -1 = 12 digit
			   * if account generate 123 then we have to add 9 0s start in like 0000000123
			   */
			  
			  long number = secureRandom.nextLong(10000000000000L); 
			  accountNumber = String.format("%012d", number);
			  
			  
		  }while(accountRepository.existsByAccountNumber(accountNumber));
			
		  return accountNumber;
	  }
	  
	  
	  
	  private AccountResponse mapToResponse(Account account) {
		// TODO Auto-generated method stub
		  AccountResponse response = new AccountResponse();
		  response.setId(account.getId());
		  response.setAccountHolderName(account.getAccountHolderName());
		  response.setAccountNumber(account.getAccountNumber());
		  response.setEmail(account.getEmail());
		  response.setPhone(account.getPhone());
		  response.setAccountType(account.getAccountType());
		  response.setAccountStatus(account.getAccountStatus());
		  response.setBalance(account.getBalance());
		  response.setDailyTransactionLimit(account.getDailyTransactionLimit());
		  response.setCreatedAt(account.getCreatedAt());
		  
		return response;
	  }

	 
	
	
	
	
}
