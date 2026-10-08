package com.sourabhsrivastava.transcationservice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.TransactionState;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sourabhsrivastava.client.AccountServiceClient;
import com.sourabhsrivastava.dto.TranscationResponse;
import com.sourabhsrivastava.dto.TransferRequest;
import com.sourabhsrivastava.entity.TransactionType;
import com.sourabhsrivastava.entity.TranscationEnitity;
import com.sourabhsrivastava.entity.TranscationStatus;
import com.sourabhsrivastava.event.TransactionCompletedEvent;
import com.sourabhsrivastava.event.TransactionIntiatedEvent;
import com.sourabhsrivastava.transactionrepository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscationService {
	
	private final TransactionRepository transactionRepository;
	private final AccountServiceClient accountServiceClient;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RedisTemplate<String, String> redisTemplate;
	
	private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
	private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
	private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
	private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";
	
	
	/**
	 * SAGA STEP-1 Initiated transfer
	 * Deduct from sender via feign client
	 * Saves transaction as PROCESSING
	 * publish event to kafka for fraud check
	 * @param request
	 * @return
	 */
	
	public TranscationResponse transfer(TransferRequest request) {
		
		log.info("SAGA START- Transfer : {} -> {} amount: {}",
				request.getSenderAccountNumber(),
				request.getReceiverAccountNumber(),
				request.getAmount());
		
		accountServiceClient.deductBalance(request.getSenderAccountNumber(), request.getAmount());
		
		TranscationEnitity transcationEnitity = new TranscationEnitity();
		transcationEnitity.setSenderAccountNumber(request.getSenderAccountNumber());
		transcationEnitity.setReceiverAccountNumber(request.getReceiverAccountNumber());
		transcationEnitity.setAmount(request.getAmount());
		transcationEnitity.setTranscationType(TransactionType.TRANSFER);
		transcationEnitity.setTransctionStatus(TranscationStatus.PROCESSING);
		transcationEnitity.setDescription(request.getDescription());
		transcationEnitity.setRefrenceNumber(UUID.randomUUID().toString());
		
		TranscationEnitity savedTransaction = transactionRepository.save(transcationEnitity);
		log.info("Transaction saved as PROCESSING: ",savedTransaction.getId() );
		
		//SAGA Step-2 publish for fraud check
		TransactionIntiatedEvent event = new TransactionIntiatedEvent(
				
				savedTransaction.getId(),
				savedTransaction.getSenderAccountNumber(),
				savedTransaction.getReceiverAccountNumber(),
				savedTransaction.getAmount(),
				savedTransaction.getDescription()
				);
		
		kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
		log.info("SAGA Step-2-  TransactionInitiatedEvent published : {}", savedTransaction.getId());
		
		return mapToResponse(savedTransaction);
	}
	
	public TranscationResponse getTransaction(String transcationId)
	{
		return mapToResponse(transactionRepository.findById(transcationId)
				.orElseThrow(()-> new RuntimeException("Transaction Not Found" +transcationId ))
				
				);
	}

	public List<TranscationResponse> getTransactionHistory(String accountNumber)
	{
		return  transactionRepository.
				findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	private TranscationResponse mapToResponse(TranscationEnitity transaction) {
		// TODO Auto-generated method stub
		TranscationResponse response = new TranscationResponse();
		response.setId(transaction.getId());
		response.setSenderAccountNumber(transaction.getSenderAccountNumber());
		response.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
		response.setAmount(transaction.getAmount());
		response.setTranscationType(transaction.getTranscationType());
		response.setTransctionStatus(transaction.getTransctionStatus());
		response.setDescription(transaction.getDescription());
		response.setRefrenceNumber(transaction.getRefrenceNumber());
		response.setFailureReaosn(transaction.getFailureReaosn());
		response.setCreatedAt(transaction.getCreatedAt());
		response.setCompletedAt(transaction.getCompletedAt());
		
		
		return response;
	}

	public TranscationResponse verifyOtp(String transcationId, String otp) {
		// TODO Auto-generated method stub
		log.info("OTP verivfication for the transation: {}",transcationId );
		TranscationEnitity transaction = transactionRepository.findById(transcationId)
				   .orElseThrow(() -> new RuntimeException(
						   "Transaction not found"));
		String otpKey = "verfication:otp" + transcationId;
		
		String storedOtp = redisTemplate.opsForValue().get(otpKey);
		
		if(storedOtp == null)
		{
			//OTP Expried
			
			log.warn("OTP expired for transcation: {}", transcationId);
			compansateTransaction(transaction,"OTP expired - transaction cancelled and amount refunded.");
			
			return mapToResponse(transaction);
		}
		
		if(!storedOtp.equals(otp))
		{
			//Block account and refunded
			
			log.warn("Wrong OTP - blocking account and refunding: {}", transcationId);
			redisTemplate.delete(otpKey);
			blockAccountAndCompensate(transaction, "Wrong OTP entered- transaction cancelled,"+ "account blocked for security");
			
			return mapToResponse(transaction);
			
		}
		
		//OTP correct:- complete transaction
		log.info("OTP verified - completeing transaction : {}",transcationId);
		redisTemplate.delete(otpKey);
		completeTransaction(transaction);
		
		return mapToResponse(transaction);
	}

	private void completeTransaction(TranscationEnitity transaction) {
		// TODO Auto-generated method stub
		transaction.setTransctionStatus(TranscationStatus.COMPLTED);
		transaction.setCompletedAt(LocalDateTime.now());
		transactionRepository.save(transaction);
		
		TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(
				transaction.getId(),
				transaction.getSenderAccountNumber(),
				transaction.getReceiverAccountNumber(),
				transaction.getAmount(),
				transaction.getDescription()
				);
		
		kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), completedEvent);
		
		log.info("SAGA COMPETD- transaction : {} completed ",transaction.getId());
		
	}

	private void blockAccountAndCompensate(TranscationEnitity transaction, String reason) {
		// TODO Auto-generated method stub
		
		//PUBLISH fraud.dedtucted event-> Account service will block
		Map<String, Object> fraudEvent = new HashMap<>();
		fraudEvent.put("transactionId", transaction.getId());
		fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
		fraudEvent.put("reason", reason);
		
		kafkaTemplate.send(FRAUD_DETECTED_TOPIC,transaction.getSenderAccountNumber(),fraudEvent);
		
		log.warn("fraud.detected published - account: {} will be blocked, kindly contact to bank ",
				transaction.getSenderAccountNumber());
		
		//SAGA COMPENSATION - refund sender
		compansateTransaction(transaction,reason);
	}
	
	


	private void compansateTransaction(TranscationEnitity transaction, String reason) {
        log.warn("SAGA COMPENSATION - refunding: {}  amount:{}",
        		   transaction.getSenderAccountNumber(),transaction.getAmount());
        
        //CREDITE MONEY BACK TO SENDER SYNCHRONOULSY
        
        accountServiceClient.creditBalance(transaction.getSenderAccountNumber()
        		,transaction.getAmount());
        
        transaction.setTransctionStatus(TranscationStatus.FLAGGED);
        transaction.setFailureReaosn(reason + "- SAGA Compensation executed, amount refunded at " + LocalDateTime.now());
        transactionRepository.save(transaction) ;
        
        //publish refund event notification service will alert user for every money movenet i..e refunded
        
        Map<String , Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", reason);
        
        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent );
        
        log.info("SAGA COMPENSATE COMPLETE - {} refunded to: {}", transaction.getAmount()
        		, transaction.getSenderAccountNumber());
		
	}

	public void processCleanResult(String transcationId) {
		// TODO Auto-generated method stub
		
		TranscationEnitity transaction = transactionRepository.findById(transcationId)
				   .orElseThrow(() -> new RuntimeException(
						   "Transaction not found"));
		
		if(transaction.getTransctionStatus() != TranscationStatus.PROCESSING){
			log.warn("Transaction {} not PROCESSING:  skipping ", transcationId);
			return;
		}
		
		completeTransaction(transaction);
		
	}

}
