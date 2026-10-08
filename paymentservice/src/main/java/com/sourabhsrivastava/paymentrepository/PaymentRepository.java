package com.sourabhsrivastava.paymentrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sourabhsrivastava.paymententity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {

	Optional<Payment> findByRazorpayOrderId(String orderId);
	
	
	

}
