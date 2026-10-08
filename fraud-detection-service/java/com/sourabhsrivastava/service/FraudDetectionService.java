package com.sourabhsrivastava.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sourabhsrivastava.frauddetectionClient.AccountServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.FraudCheckResult;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService 
{
	
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RedisTemplate<String, String> redisTemplate;
	private final AccountServiceClient accountServiceClient;
	private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
	private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "farud.check.clean";
	
	@Value("${fraud.max-transaction-per-minute}")
	private int maxTransactionPerMinute;
	
	@Value("${fraud.suspecious.amount-multiplier}")
	private double suspeciousAmountMultiplier;
	
	@Value("${fraud.max-balance-percentage}")
	private double maxBalancePercentage;
	
	public void checkTransaction(Map<String, Object> payload) {
		// TODO Auto-generated method stub
		
		String transcationId = (String)payload.get("transcationId");
		String accountNumber = (String)payload.get("senderAccountNumber");
		BigDecimal amount = new BigDecimal( payload.get("amount").toString());
		
		//fetch real balance from account service
		
		BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);
		log.info("checking transaction : {} account: {} amount:{} balance: {} ",transcationId, 
				accountNumber, amount, senderBalance);
		
		
		  FraudCheckResult result = performFraudCheck(accountNumber, amount, senderBalance);
		  
		  /**
		   * if below condiotn is true meanse farud detected 
		   * then publish a event to kafka for verification.rquired event
		   * verifyOTP  will call to confimr the transcation is intitated by
		   * user itself or by fraudsters and above event will cosnume 
		   * by transaction service that fraud detection service has cought some 
		   * suspecious activity then transaction sevice  will publish event generate.otp
		   * event, then generate.otp event will consume by notification service
		   * 
		   */
		
		if(result.isFraud())
		{
			
			log.info("Some Suspecious actvity detedted - account: {}"
					+ "reason: {} - requesting OTP verification",
					 accountNumber,result.getReason());
			
			Map<String, Object> verificationEvent = new HashMap<>();
			verificationEvent.put("transactionId", transcationId);
			verificationEvent.put("accountNumber", accountNumber);
			verificationEvent.put("amount", amount);
			verificationEvent.put("reason", result.getReason());
			
			kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transcationId,verificationEvent);
		}else
		{
			//Transaction is clean
			log.info("transcation ic clean");
			Map<String, Object> transcationCleanEvent =new HashMap<>();
			
			transcationCleanEvent.put("transactionId", transcationId);
			transcationCleanEvent.put("isFraud", false);
			transcationCleanEvent.put("reason", null);
			
			kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transcationId,transcationCleanEvent);
		}
		
		
	}


	/**
	 * performFraudCheck will check 3 pattrens 
	 * PATTERN-1 : velocity check i.e how many transaction happend within 60 seconds
	 * PATTREN-2 : check average of transcation done user i.e 50,50,100, but suspecious is
	 * transcation goes 3x,5x of average i.e 10000,50000 then marked as verifcation window
	 * 
	 * PATTERN-3 : Balance check if happend 90% of balance
	 * @param accountNumber
	 * @param amount
	 * @param senderBalance
	 * @return
	 */
	
	private FraudCheckResult performFraudCheck(String accountNumber, BigDecimal amount, BigDecimal senderBalance) {
	    
		//PATTREND-1 Velocity check
		if(isVelocityExceeded(accountNumber))
		{
			return new FraudCheckResult(true,"Too many transaction in 60 seconds"
					+"- velcoity limit exceeded");
					
		}
		
		//PATTERN 2: Amount check 
		if(isAmountSuspecious(accountNumber, amount))
		{
			return new FraudCheckResult(true,"Unusual transcation amount"
					+"- exceeds 3x amount of your average");
		}
			
		//PATTREN 3: Balance check 90% of balance used
		
		if(senderBalance.compareTo(BigDecimal.ZERO) > 0 &&
				               isBalanceCheckFailed(senderBalance, amount))
		{
			return new FraudCheckResult(true,"Transcation amount"
					+"- exceeds 90% of account balance");
			
		}
		
		return new FraudCheckResult(false, null);
	}


	private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {
		// TODO Auto-generated method stub
		BigDecimal maxAllowed =senderBalance.multiply(
				BigDecimal.valueOf(maxBalancePercentage));
				
		log.info("isBalanceCheckFailed method: Balance check : amount:{}"
				+ "-maxAllowed: {} suspecious:{}", amount, maxAllowed, amount.compareTo(maxAllowed) > 0);
		return amount.compareTo(maxAllowed) > 0;
	}


	private boolean isAmountSuspecious(String accountNumber, BigDecimal amount) {
		// TODO Auto-generated method stub
		String avgKey =  "fraud:avg_amount"+accountNumber;
		String avgStr= redisTemplate.opsForValue().get(avgKey);
		
		if(avgStr == null)
		{
			redisTemplate.opsForValue().set(avgKey,amount.toString());
			return false;
		}
				BigDecimal avgAMount = new BigDecimal(avgStr);
				BigDecimal threshold = avgAMount.multiply(BigDecimal.valueOf(maxTransactionPerMinute));
				
		//update running amount
				
			BigDecimal newAvg = avgAMount.add(amount)
					             .divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP);
			redisTemplate.opsForValue().set(avgKey, newAvg.toString());
			
			log.info("Amount check : amount: {} threshold: {} suspecious:{}"
					 , amount, threshold, amount.compareTo(threshold) > 0);
				
		
		
		return amount.compareTo(threshold) > 0;
	}


	private boolean isVelocityExceeded(String accountNumber) {
		// TODO Auto-generated method stub
		
		String key = "fraud:velocity" + accountNumber;
		Long count= redisTemplate.opsForValue().increment(key);
		if(count != null && count == 1)
		{
			redisTemplate.expire(key,60,TimeUnit.SECONDS);
		}
		log.info("velocity check : account: {} count: {}/{}",
				accountNumber,count,maxTransactionPerMinute);
		
		return count!= null && count > maxTransactionPerMinute;
	}
	
	
	
	
	

}
