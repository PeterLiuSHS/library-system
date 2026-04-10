package com.kexun.user.service;

import com.kexun.user.dto.UserUpdateRequest;
import com.kexun.user.model.User;
import org.springframework.data.domain.Page;

public interface UserService {

    User create(User user);
    User getById(Long id);
    Page<User> list(String search, int page, int size);
    User update(Long id, UserUpdateRequest request);
    void delete(Long id);
}
