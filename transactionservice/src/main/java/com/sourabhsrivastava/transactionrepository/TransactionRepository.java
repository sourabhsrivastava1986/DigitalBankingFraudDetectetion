package com.sourabhsrivastava.transactionrepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sourabhsrivastava.entity.TranscationEnitity;

public interface TransactionRepository extends JpaRepository<TranscationEnitity, String> {

	List<TranscationEnitity> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
	
	
}
