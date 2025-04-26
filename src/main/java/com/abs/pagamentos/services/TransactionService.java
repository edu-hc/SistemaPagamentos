package com.abs.pagamentos.services;

import com.abs.pagamentos.dtos.TransactionDTO;
import com.abs.pagamentos.model.transaction.Transaction;
import com.abs.pagamentos.model.user.User;
import com.abs.pagamentos.repositories.TransactionRepository;
import com.abs.pagamentos.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service

public class TransactionService {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = userService.findUserById(transaction.senderId());
        User receiver = userService.findUserById(transaction.receiverId());

        userService.validateTransaction(sender, transaction.amount());

        boolean isAuthorized = this.authorizeTransaction(sender, transaction.amount());

        if(!isAuthorized) {
            throw new Exception("Transação não autorizada");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.amount());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.amount()));
        receiver.setBalance(receiver.getBalance().add(transaction.amount()));

        transactionRepository.save(newTransaction);
        userService.saveUser(sender);
        userService.saveUser(receiver);

        return newTransaction;
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "https://util.devi.tools/api/v2/authorize",
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();

                // Verificação segura com tratamento de null
                if (responseBody != null &&
                        "success".equalsIgnoreCase((String) responseBody.get("status"))) {

                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    if (data != null) {
                        Boolean isAuthorized = (Boolean) data.get("authorization");
                        return Boolean.TRUE.equals(isAuthorized);
                    }
                }
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    public Transaction getTransactionById (Long id) throws Exception {
        return transactionRepository.findById(id).orElseThrow(() -> new Exception("Transação não encontrada"));
    }

}
