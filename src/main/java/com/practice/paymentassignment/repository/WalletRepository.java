package com.practice.paymentassignment.repository;

import com.practice.paymentassignment.entity.Wallet;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithPessimisticLock(@Param("userId") Long userId);

    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    // Optional<Wallet> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserId(@Param("userId") Long userId);
}
