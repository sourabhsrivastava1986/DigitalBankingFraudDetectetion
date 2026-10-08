package com.sourabhsrivastava.transcationcontroller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourabhsrivastava.dto.TranscationResponse;
import com.sourabhsrivastava.dto.TransferRequest;
import com.sourabhsrivastava.transcationservice.TranscationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transcations")
@Slf4j
@RequiredArgsConstructor
public class TranscationController {
	
	private final TranscationService transcationService;
	
	@PostMapping("/transfer")
	public ResponseEntity<TranscationResponse> transfer(
			@Valid @RequestBody TransferRequest request)
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(transcationService.transfer(request));
		
	}
	
	@GetMapping("/{transactionId}")
	public ResponseEntity<TranscationResponse> getTransaction(
			@PathVariable String transactionId)
	{
		return ResponseEntity.ok(transcationService.getTransaction(transactionId));
		
	}
	
	
	@GetMapping("/account/{accountNumber}")
	public ResponseEntity<List<TranscationResponse>> getTransactionHistory(
			@PathVariable String accountNumber)
	{
		return ResponseEntity.ok(transcationService.getTransactionHistory(accountNumber));
		
	}

	@PostMapping("/{transcationId}/verify")
	public ResponseEntity<TranscationResponse>  verifryOTP(
			@PathVariable String transcationId,
			@RequestParam String otp)
	{
		log.info("OTP verification request - transaction: {}", transcationId);
		
		return ResponseEntity.ok(transcationService.verifyOtp(transcationId,otp));
	}
	
}
