package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    public Customer getCustomerByAccount(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<BankTransaction> getAllTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public long getSuccessCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {
        return transactionRepository.findLargeTransfers(minAmount);
    }

    public BigDecimal getTotalSentByAccount(String accountNumber) {
        return transactionRepository.getTotalSentByAccount(accountNumber);
    }

    @Transactional
    public void createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number " + customer.getAccountNumber() + " already exists");
        }
        customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        if (customerRepository.getByAccountNumber(accountNumber).isEmpty()) {
            throw new AccountNotFoundException(accountNumber);
        }
        customerRepository.credit(accountNumber, amount);

        BankTransaction tx = new BankTransaction();
        tx.setFromAccount("CASH_DEPOSIT");
        tx.setToAccount(accountNumber);
        tx.setAmount(amount);
        tx.setDescription("Deposit to account " + accountNumber);
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (customerRepository.getByAccountNumber(accountNumber).isEmpty()) {
            throw new AccountNotFoundException(accountNumber);
        }
        int rows = customerRepository.debit(accountNumber, amount);
        if (rows == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }

        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(accountNumber);
        tx.setToAccount("CASH_WITHDRAWAL");
        tx.setAmount(amount);
        tx.setDescription("Withdrawal from account " + accountNumber);
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Source and destination accounts must be different");
        }

        if (customerRepository.getByAccountNumber(fromAccount).isEmpty()) {
            throw new AccountNotFoundException(fromAccount);
        }
        if (customerRepository.getByAccountNumber(toAccount).isEmpty()) {
            throw new AccountNotFoundException(toAccount);
        }

        BigDecimal fee = amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduction = amount.add(fee);

        int updatedRows = customerRepository.debit(fromAccount, totalDeduction);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after the withdrawal");
        }

        customerRepository.credit(toAccount, amount);
        customerRepository.credit("999999", fee);

        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(fromAccount);
        tx.setToAccount(toAccount);
        tx.setAmount(amount);
        tx.setDescription("Transfer completed (Fee: LKR " + fee + ")");
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);
    }
}