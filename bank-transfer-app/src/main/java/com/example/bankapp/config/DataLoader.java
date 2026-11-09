package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() == 0) {
            Customer c1 = new Customer();
            c1.setAccountNumber("100001");
            c1.setFullName("Kamal");
            c1.setEmail("kamal@example.com");
            c1.setBalance(new BigDecimal("50000.00"));
            customerRepository.save(c1);

            Customer c2 = new Customer();
            c2.setAccountNumber("100002");
            c2.setFullName("Nimali");
            c2.setEmail("nimali@example.com");
            c2.setBalance(new BigDecimal("25000.00"));
            customerRepository.save(c2);

            Customer c3 = new Customer();
            c3.setAccountNumber("100003");
            c3.setFullName("Saman");
            c3.setEmail("saman@example.com");
            c3.setBalance(new BigDecimal("10000.00"));
            customerRepository.save(c3);

            Customer bankFeeAcc = new Customer();
            bankFeeAcc.setAccountNumber("999999");
            bankFeeAcc.setFullName("Bank Revenue Account");
            bankFeeAcc.setEmail("revenue@bank.com");
            bankFeeAcc.setBalance(BigDecimal.ZERO);
            customerRepository.save(bankFeeAcc);
        }
    }
}