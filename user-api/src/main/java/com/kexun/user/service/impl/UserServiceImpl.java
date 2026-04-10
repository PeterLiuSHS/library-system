package com.kexun.user.service.impl;

import com.kexun.user.dto.UserUpdateRequest;
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

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(User user){
        return userRepository.save(user);
    }

    @Override
    public User getById(Long id){
        return userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Override
    public Page<User> list(String search, int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        if (search == null || search.isBlank()) {
            return userRepository.findAll(pageable);
        }

        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search,
                search,
                pageable
        );
    }

    @Override
    public User update(Long id, UserUpdateRequest request){
        User existing = userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User " + id + " not found"));

        existing.setName(request.getName());

        return userRepository.save(existing);
    }

    @Override
    public void delete(Long id){
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User " + id + " not found");
        }
        userRepository.deleteById(id);
    }
}
