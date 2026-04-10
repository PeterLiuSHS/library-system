package com.kexun.user.repository;

import com.kexun.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String nameKeyword,
            String emailKeyword,
            Pageable pageable);

    Long id(Long id);
}
