package com.sourabhsrivastava.accountMain.accountcontroller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourabhsrivastava.accountMain.accountservice.AccountService;
import com.sourabhsrivastava.accountMain.dto.AccountResponse;
import com.sourabhsrivastava.accountMain.dto.CreateAccountRequest;

import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {
	
 private final AccountService accountService;
 
 
 //endpoints like createAcoount()
 //getAccount()
 //getBalance()
 //blockedAccount()
 // SAGA step1
 // deductBalance
 //creditBalance   a)credit recever b)credit sender  refund
 
 @PostMapping
 public ResponseEntity<AccountResponse> createAccount(
		 @Valid @RequestBody  CreateAccountRequest request )
 {
	return ResponseEntity.status(HttpStatus.CREATED)
			.body(accountService.createAccount(request));
  }
 
 @GetMapping("/{accountNumber}")
 public ResponseEntity<AccountResponse>  getAccount(
		 @PathVariable String accountNumber)
 {
	return ResponseEntity.ok(accountService.getAccount(accountNumber));
	 
 }
 
 @GetMapping("/{accountNumber}/balance")
 public ResponseEntity<BigDecimal>  getBalance(
		 @PathVariable String accountNumber)
 {
	return ResponseEntity.ok(accountService.getBalance(accountNumber));
	 
 }
 
 
 @PutMapping("{accountNumber}/blocked")
 public ResponseEntity<String> blockedAccount(
		 @PathVariable String accountNumber)
 {
	 accountService.blockedAccount(accountNumber);
	return ResponseEntity.ok("Account Blocked Successfully");
	 
 }
 
 /**
  * SAGA Step -1  deductBalance
  * called by transcation serivce when transfer is inititated
  * 
  */
 
 @PutMapping("/{accountNumber}/deduct")
 public ResponseEntity<String> deductBalance(
		 @PathVariable String accountNumber,
		 @RequestParam BigDecimal amount)
 {
	 
	 accountService.deductBalance(accountNumber, amount);
	 return ResponseEntity.ok("Balance Deducted Successfully");
 }
 
 /**
  * Saga step 4 - compenseting transcation endpoint
  * called by transaction service in two scenerio
  * 1- fraud detected - refund sender(undo step 1)
  * 2- Transcation completed- credit reciver
  * 
  */
 
 @PutMapping("/{accountNumber}/credit")
 public ResponseEntity<String> creditBalance(
		 @PathVariable String accountNumber,
		 @RequestParam BigDecimal amount)
 {
	 accountService.creditBalance(accountNumber, amount);
	 return ResponseEntity.ok("Balance Credited Successfully");
 }
 
 
 
 
 
 
 
 

}
