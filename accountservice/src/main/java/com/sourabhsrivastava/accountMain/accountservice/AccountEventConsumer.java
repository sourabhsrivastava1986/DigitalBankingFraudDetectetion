package com.sourabhsrivastava.accountMain.accountservice;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

	private final AccountService accountService;
	
	/**
	 * consume Transcation.completed event from kafka
	 * credit Receiver Account
	 * @param payload
	 */
	
	@KafkaListener(topics="transaction.completed")
	public void consumeTransactionCompleted(
			@Payload Map<String, Object> payload)
	{
		try {
			
			String receiverAccount= (String) payload.get("receiverAccountNumber");
			BigDecimal amount = new BigDecimal(payload.get("amount").toString());
		
			log.info("Crediting Account : {} amount :{} ", receiverAccount,amount);
			
			accountService.creditBalance(receiverAccount, amount);
		}catch(Exception e)
		{
			log.error("error credting  account : {}", e.getMessage());
		}
		
	}
	
	/**
	 * consum consume.frauddected from kafka
	 * blocks the flagged account
	 * @param payload
	 */
	
	@KafkaListener(topics= "fraud.detected")
	public void cosumeFraudDetected(@Payload Map<String, Object> payload)
	{
		
		try
		{
			String accountNumber= (String) payload.get("accountNumber");
			log.info("fraud detected blocking Account : {} ", accountNumber);
			accountService.blockedAccount(accountNumber);
			
			
		}catch(Exception e)
		{
			log.error("error blocking  account : {}", e.getMessage());
			
		}
	}
	
}
