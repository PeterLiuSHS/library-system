package com.kexun.user.service.impl;

import com.kexun.user.client.LoanClient;
import com.kexun.user.dto.UserUpdateRequest;
import com.kexun.user.exception.ConflictException;
import com.kexun.user.exception.ResourceNotFoundException;
import com.kexun.user.model.User;
import com.kexun.user.repository.UserRepository;
import com.kexun.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LoanClient loanClient;

    public UserServiceImpl(UserRepository userRepository, LoanClient loanClient) {
        this.userRepository = userRepository;
        this.loanClient = loanClient;
    }

    @Override
    public User create(User user){
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        return userRepository.save(user);
    }

    @Override
    public User getById(Long id){
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(()-> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    public Page<User> list(String search, int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        if (search == null || search.isBlank()) {
            return userRepository.findByDeletedFalse(pageable);
        }

        return userRepository.findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
                search,
                search,
                pageable
        );
    }

    @Override
    public User update(Long id, UserUpdateRequest request){
        User existing = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(()-> new ResourceNotFoundException("User " + id + " not found"));

        existing.setName(request.getName());

        return userRepository.save(existing);
    }

    @Override
    public void delete(Long id){
        User existing = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));

        if (loanClient.hasActiveLoans(id)) {
            throw new ConflictException("User " + id + " has active loans and cannot be deleted");
        }

        existing.setDeleted(true);
        userRepository.save(existing);
    }
}
