package com.abs.pagamentos.services;

import com.abs.pagamentos.dtos.UserDTO;
import com.abs.pagamentos.model.transaction.Transaction;
import com.abs.pagamentos.model.user.User;
import com.abs.pagamentos.model.user.UserType;
import com.abs.pagamentos.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service

public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if (sender.getUserType() == UserType.MERCHANT) {
            throw new Exception("Usuário do tipo lojista não está autorizado a realizar transação");
        }

        if(sender.getBalance().compareTo(amount) < 0) {
            throw new Exception("Saldo insuficiente");
        }
    }

    public User findUserById(Long id) throws Exception {
        return userRepository.findById(id).orElseThrow(() -> new Exception("Usuário não encontrado"));
    }

    public User findUserByEmail(String email) throws Exception {
        return userRepository.findUserByEmail(email).orElseThrow(() -> new Exception("Usuário não encontrado"));
    }

    public void saveUser(User user) throws Exception {
        userRepository.save(user);
    }

    public User createUser(UserDTO data) throws Exception {
        User newUser = new User(data);
        this.saveUser(newUser);
        return newUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
