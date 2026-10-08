package com.sourabhsrivastava.service;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

	@KafkaListener(topics= "transaction.otp.generated")
	public void cosumedOtpGenerated(
			@Payload Map<String, Object> payload)
	{
		try {
			
			String accountNumber = (String) payload.get("accountNumber");
			String otp = (String) payload.get("otp");
			String amount = (String) payload.get("amount");
			String reason =  (String) payload.get("reason");
			
			sendAlert(accountNumber,"TRANSACTION VERIFICATION REQUIRED",
					String.format("Suspecious activity detected on your account."
							+"Reason: %s" + "A transaction of %s is pending verification."+
							"Your OTP is : %s valid for 5 minute."+
							"if this wasn't you ignore this message.",reason,amount,otp));
		}catch(Exception e)
		{
			log.error( "Error sending OTP notification : {}", e.getMessage());
		}
		
		
	}

	/** Topic -transation.completed :-now here we wil consume transation.completed
	 * in which we shown to user that 
	 * some amount debited and some amount credited
	 * 
	 * Topic- farud.detected to notify user
	 *Razorpay -notifivation 
	 *if payment success and payment failed 
	 *so notify every money movement to user
	 * @param accountNumber
	 * @param subject
	 * @param message
	 */
	
	@KafkaListener(topics = "transaction.completed")
	public void consumeTransactioncompleted(
			@Payload Map<String, Object> payload)
	{
		try {
			
			String senderAccount = (String) payload.get("senderAccountNumber");
			String receiverAccount = (String) payload.get("receiverAccountNumber");
			String amount = payload.get("Amount").toString();
			
			//DEBIT ALERT
			sendAlert(senderAccount, "DEBIT ALERT",String.format("%s Amount is debited from  account"
					, amount, senderAccount)
					
					);
			
			
			//Credit Alert
			sendAlert(receiverAccount, "CREDIT ALERT",String.format("%s Amount is credited from  account"
					, amount, receiverAccount)
					
					);
			
		}catch(Exception e)
		{
			log.error("Error in sending transaction notification : {}", e.getMessage());
		}
	}
	
	@KafkaListener(topics = "fraud.detected")
	public void consumeFarudDetetced(
			@Payload Map<String, Object> payload)
	{
		try {
			
			String accountNumber = (String) payload.get("accountNumber");
			String reason = (String) payload.get("reason");
			
			sendAlert(accountNumber, "SUSPECIOUS ACTIVITY DETECTED",String.format(" Your account %s "
					+ "has been blocked."+"Reason : %s"+ "Please contact to your bank immeditaly."
					, accountNumber, reason)
					
					);
			
		}catch(Exception e)
		{
			log.error("Error sending fraud alert", e.getMessage());
		}
	}
	
	@KafkaListener(topics = "transaction.refunded")
	public void cosnumeTransactionRefunded(@Payload Map<String, Object> payload)
	{
		
		try {
			String senderAccount = (String) payload.get("senderAccountNumber");
			String amount = payload.get("Amount").toString();
			String reason = (String) payload.get("reason");
			
			sendAlert(senderAccount, "REFUND PROCESSED",String.format(" Your transaction of %s "
					+ "was cancelled."+"Reason : %s"+ "%s has been refunded to account %s."
					, amount, reason, amount, senderAccount)
					
					);
			
		}catch(Exception e)
		{
			log.error("Error sending refund notification : {}", e.getMessage());
		}
	}
	
	@KafkaListener(topics = "payment.completed")
	public void consumePaymentCompleted(@Payload Map<String, Object> payload)
	{
		try {
			
			String accountNumber = (String) payload.get("accountNumber");
			String amount = payload.get("Amount").toString();
			
			sendAlert(accountNumber, "PAYMENT success",String.format(" Payment of %s completed."
					+ "Razorpay ID: %s"
					, amount, payload.get("razorpayPaymentsId"))
					
					);
			
		}catch(Exception e)
		{
			log.error("Error sending payment notification", e.getMessage());
		}
	}
	
	@KafkaListener(topics = "payment.failed")
	public void cosnumePaymentFailed(@Payload Map<String, Object> payload)
	{
		try {
			String accountNumber = (String) payload.get("accountNumber");
			String amount = payload.get("Amount").toString();
			
			sendAlert(accountNumber, "PAYMENT Failed",String.format(" your payment of %s could not be processed."
					+ "Please try again or contact support"
					, amount)
					
					);
		}catch(Exception e)
		{
			log.error("Error semnding payment failure notification : {}", e.getMessage());
		}
	}
	
	private void sendAlert(String accountNumber, String subject, String message) {
		// TODO Auto-generated method stub
		
		
		log.info("------------------------------------------------------------------------");
		log.info("Account : {}", accountNumber);
		log.info("Subject : {}", subject);
		log.info("Message : {}", message);
		
		log.info("------------------------------------------------------------------------");
		
		
	}
	
	
	
	
	
	
	
	
}
