package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("allCustomers", bankService.getAllCustomers());
        model.addAttribute("customers", bankService.searchCustomers(keyword));
        model.addAttribute("totalCustomers", bankService.getAllCustomers().size());
        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("customer", new Customer());
        model.addAttribute("transactions", bankService.getAllTransactions());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessCount());
        model.addAttribute("failedCount", bankService.getFailedCount());
        model.addAttribute("keyword", keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed for new customer");
            return "redirect:/";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transferMoney(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix validation errors in transfer form");
            return "redirect:/";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully!");
        } catch (Exception e) {
            try {
                auditService.recordFailure(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage() != null ? e.getMessage() : "Transfer failed"
                );
            } catch (Exception ex) {
                // Ignore
            }
            redirectAttributes.addFlashAttribute("error", "Transfer failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("customer", bankService.getCustomerByAccount(accountNumber));
        model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSentByAccount(accountNumber));
        return "history";
    }

    @GetMapping("/deposit")
    public String showDepositPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String processDeposit(@RequestParam String accountNumber,
                                 @RequestParam BigDecimal amount,
                                 RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposited LKR " + amount + " to account " + accountNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String showWithdrawPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@RequestParam String accountNumber,
                                  @RequestParam BigDecimal amount,
                                  RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrew LKR " + amount + " from account " + accountNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/withdraw";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000.00") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfer";
    }
}