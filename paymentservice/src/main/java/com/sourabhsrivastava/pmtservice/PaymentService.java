package com.sourabhsrivastava.pmtservice;

import java.lang.ProcessHandle.Info;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.sourabhsrivastava.paymentdto.CreatePaymentRequest;
import com.sourabhsrivastava.paymentdto.PaymentOrderResponse;
import com.sourabhsrivastava.paymententity.Payment;
import com.sourabhsrivastava.paymententity.PaymentStatus;
import com.sourabhsrivastava.paymentrepository.PaymentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

	
	
	private final PaymentRepository paymentRepository;
	private final KafkaTemplate< String, Object> kafkaTemplate;
	
	@Value("${razorpay.key-id}")
	private String keyId;
	
	@Value("${razorpay.key-secret}")
	private String keySecret;
	
	private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
	private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
	
	
	
	
	/**
	 * Create Razorpaypayment order
	 * 
	 * Flow ->
	 * 1. Create order in razorpay
	 * 2. Save payment order in db
	 * 3. Return orders details to frontend 
	 * 4.Frontend showsRazorpay checkout
	 * 5. User pays
	 * 6.Razorpay called webhook
	 * 
	 * @param request
	 * @return
	 * @throws RazorpayException 
	 */
	
	
	public PaymentOrderResponse createPaymentorder(CreatePaymentRequest request) throws RazorpayException {
		// TODO Auto-generated method stub
		log.info("Creating payment order for account : {} amount: {}",
				   request.getAccountNumber(),request.getAmount());
		
		RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
		
		//convertedAnount
		
		int convertedAmount = request.getAmount()
				                     .multiply(BigDecimal.valueOf(100))
				                     .intValue();
		
		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", convertedAmount);
		orderRequest.put("currency", "USD/INR");
		orderRequest.put("receipt","recpt_"+System.currentTimeMillis()+UUID
				                                .randomUUID()
		                                        .toString()
		                                        .replace("-", "")
		                                        .substring(0,10));
		
		Order razorpayOrder =razorpayClient.orders.create(orderRequest);
		log.info("Razorpay order created: {}",razorpayOrder.get("id").toString());
		
		
		//Save payment record in db
		
	   Payment payment = new Payment();
	   payment.setId(razorpayOrder.get("id").toString());
	   payment.setAccountNumber(request.getAccountNumber());
	   payment.setAmount(request.getAmount());
	   payment.setCurrency("INR");  // can keep USD, EURO, AUD etc
	   payment.setStatus(PaymentStatus.CREATED);
	   payment.setDescription(request.getDescription());
	   
	      Payment savedPayment = paymentRepository.save(payment);
	   
	   
	   
		
		return new PaymentOrderResponse(
				savedPayment.getId(),
				razorpayOrder.get("id").toString(),
				request.getAmount(),
				"INR",
				"CREATED",
				keyId
				);
	}
	
	public void handleWebhook(Map<String, Object> payload)
	{
		log.info("Received razorpay webhook: {}",payload.get("event"));
		
		String event = (String)payload.get("event");
		
		if("payment.captured".equals(event))
		{
			handlePaymentSuccess(payload);
		}
		else if("payment.failed".equals(event))
		{
			handlePaymentFailure(payload);
		}
	}
	
	private void handlePaymentSuccess(Map<String, Object> payload)
	{
		try
		{
			Map<String, Object> paymentData =  extractPaymentData(payload);
			String orderId = (String) payload.get("order_id");
			String paymentId = (String) payload.get("id");
			Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
					.orElseThrow(()-> new RuntimeException("Paymnet not found for order : "+ orderId));
			payment.setRazorpayPaymentsId(paymentId);
			payment.setStatus(PaymentStatus.COMPLETED);
			paymentRepository.save(payment);
			
			//publish payment completed event
			Map<String , Object>  event = new HashMap<>();
			event.put("paymentId", payment.getId());
			event.put("accountNumber", payment.getAccountNumber());
			event.put("amount", payment.getAmount());
			event.put("razorpayPaymentsId", paymentId);
			
			kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(), event);
			
			log.info("Payment completed: {}",payment.getId());
			
			
			
		}catch(Exception e)
		{
			
			log.error("Error handling in payment success: {}",e.getMessage());
		}
	}

	private void handlePaymentFailure(Map<String, Object> payload)
	{
		try {
		Map<String, Object> paymentData =  extractPaymentData(payload);
		String orderId = (String) payload.get("order_id");
		//String paymentId = (String) payload.get("id");
		
		Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
				.orElseThrow(()-> new RuntimeException("Paymnet not found for order : "+ orderId));


		payment.setStatus(PaymentStatus.FAILED);
		payment.setFailureReason("Payment failed via Razorpay");
		paymentRepository.save(payment);
		
		//publish payment failed event
		Map<String , Object>  failedEvent = new HashMap<>();
		failedEvent.put("paymentId", payment.getId());
		failedEvent.put("accountNumber", payment.getAccountNumber());
		failedEvent.put("amount", payment.getAmount());
		failedEvent.put("reason", "Payment failed via Razorpay");
		
		kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(), failedEvent);
		
		log.warn("Payment failed: {}",payment.getId());
		}
		catch(Exception e)
		{
			log.error("Error handling in payment failed: {}",e.getMessage());
		}
	}
	
	private Map<String, Object> extractPaymentData(Map<String, Object> payload)
	{
		Map<String, Object> entity = (Map<String, Object>) payload.get("payload");
		Map<String, Object> pamentWrapper = (Map<String, Object>) entity.get("payment");
		
		return (Map<String, Object>) pamentWrapper.get("entity");
	}
	
}
