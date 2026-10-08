package com.sourabhsrivastava.paymentcontroller;

import java.util.Map;

import org.apache.kafka.common.requests.CreateAclsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;
import com.sourabhsrivastava.paymentdto.CreatePaymentRequest;
import com.sourabhsrivastava.paymentdto.PaymentOrderResponse;
import com.sourabhsrivastava.pmtservice.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;
	
	
	@PostMapping("/create-order")
	public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
			@Valid @RequestBody CreatePaymentRequest request) throws RazorpayException
	{
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(paymentService.createPaymentorder(request));
	}
	
	
	//Razorpay webhook endpoints
	@PostMapping("/webhook")
	public ResponseEntity<String> handleWebhook(
			     @RequestBody Map<String, Object> payload)
	{
		paymentService.handleWebhook(payload);
		return ResponseEntity.ok("Webhook processed");
		
	}
	
	
}
