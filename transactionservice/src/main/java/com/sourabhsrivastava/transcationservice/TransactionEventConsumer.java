package com.sourabhsrivastava.transcationservice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.sourabhsrivastava.entity.TranscationEnitity;
import com.sourabhsrivastava.entity.TranscationStatus;
import com.sourabhsrivastava.transactionrepository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
	
	private final TransactionRepository repository;
	private final TranscationService transcationService; 
	
	private final RedisTemplate<String, String> redisTemplate;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transcation.otp.generated";
	
	private static final long OTP_EXPIRY_MINUTE = 5;

	/**
	 * consume verification.required
	 * generate OTP
	 * @param payload
	 */
	
	@KafkaListener(topics= "verification.required")
	public void consumeVerificationRequired(
			@Payload Map<String, Object> payload)
	{
		
		try {
			String transcationId= (String) payload.get("transactionid");
			String accountNumber = (String) payload.get("accountNumber");
			String reason = (String) payload.get("reason");
			
			log.info("verification requuired: transactionId: {} reason: {}",transcationId, reason);
			
			TranscationEnitity transaction = repository.findById(transcationId)
					                              .orElseThrow(() -> new RuntimeException(
					                            		  "Transcation not found"+ transcationId));
			
			
			/**
			 * now we have to use idempotency gaurd to prevent 
			 * duplicate credit and debit in sender and receiver account
			 * 
			 * verify the user on transaction status is inn PROCESSING
			 *  */
			
			
			if(transaction.getTransctionStatus() != TranscationStatus.PROCESSING){
				log.warn("Transaction {} not PROCESSING:  skipping ", transcationId);
				return;
			}
			
			//generate 6 digit OTP
			
			String otp = String.format("%06d", (Math.random() * 900000 + 100000));
			
			//Store OTP in Redis - expire in 5 minute
			String otpKey = "verfication:otp" + transcationId;
			
			redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTE, TimeUnit.MINUTES);
			
			//update status
			
			transaction.setTransctionStatus(TranscationStatus.PENDING_VERIFICATION);
			repository.save(transaction);
			
			log.info("OTP generated for transaction: {} - expire in minute: {}", transcationId, OTP_EXPIRY_MINUTE);
			
			//Notify user
			
			
			Map<String, Object> eventOtp = new HashMap<>();
			eventOtp.put("transactionId", transcationId);
			eventOtp.put("accountNumber", accountNumber);
			eventOtp.put("reason", reason);
			eventOtp.put("otp", otp);
			eventOtp.put("amount", payload.get("amount"));
			
			
			kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,eventOtp);
		}catch(Exception e)
		{
			log.error("Error handling verofocation required: {}", e.getMessage());
		}
		
		
	}
	
	@KafkaListener(topics = "fraud.check.clean")
	public void consumeFraudCheckCleanEvent(@Payload 
			Map<String, Object> payload)
	{
		try
		{
			String transcationId= (String) payload.get("transactionid");
			
			transcationService.processCleanResult(transcationId);
			
		}
		catch(Exception e)
		{
			log.error("Error processing fraud check event : ", e.getMessage());
			
		}
	}
}
