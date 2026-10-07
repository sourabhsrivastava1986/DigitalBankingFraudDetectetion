package com.sourabhsrivastava.accountMain.accountrepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sourabhsrivastava.accountMain.entity.Account;

public interface AccountRepository  extends JpaRepository<Account, String> {

	boolean existsByEmail(String email);

	boolean existsByAccountNumber(String accountNumber);

	Optional<Account>  findByAccountNumber(String accountNumber);

}
