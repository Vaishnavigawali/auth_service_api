package com.library.auth.repository;

import com.library.auth.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {
    
    Optional<UserCredentials> findByUsername(String username);
    
    Optional<UserCredentials> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    void deleteByUsername(String username);
}
