package com.kexun.user.repository;

import com.kexun.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedFalse(Long id);

    Page<User> findByDeletedFalse(Pageable pageable);

    Page<User> findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
            String nameKeyword,
            String emailKeyword,
            Pageable pageable
    );

    boolean existsByEmail(String email);
}
