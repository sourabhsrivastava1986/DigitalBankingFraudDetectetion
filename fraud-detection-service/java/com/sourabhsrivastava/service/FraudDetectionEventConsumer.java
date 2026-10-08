package com.sourabhsrivastava.service;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

	
	private final FraudDetectionService detectionService;
	
	
	@KafkaListener(topics= "transcation.initiated", groupId="fraud-detection-group"	)
	public void consumeTransactionInitiated(
			@Payload Map<String, Object > payload)
	{
		log.info("Received Transaction for fraud check: {}",
				payload.get("transactionId"));
		
		try {
			detectionService.checkTransaction(payload);
			
		}catch(Exception e)
		{
			
		}
	}
	
	
	
}

