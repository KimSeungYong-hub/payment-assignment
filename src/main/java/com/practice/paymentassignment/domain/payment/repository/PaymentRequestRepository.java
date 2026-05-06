package com.practice.paymentassignment.domain.payment.repository;

import com.practice.paymentassignment.domain.payment.PaymentRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    @Query("SELECT p FROM PaymentRequest p JOIN FETCH p.merchant WHERE p.id = :id")
    Optional<PaymentRequest> findByIdWithMerchant(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentRequest p WHERE p.id = :id")
    Optional<PaymentRequest> findByIdWithPessimisticLock(@Param("id") Long id);

    Optional<PaymentRequest> findByOrderId(String orderId);
}
