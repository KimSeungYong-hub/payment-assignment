package com.practice.paymentassignment.repository;

import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequestEntity, Long> {
    @Query("SELECT p FROM PaymentRequestEntity p JOIN FETCH p.merchant WHERE p.id = :id")
    Optional<PaymentRequestEntity> findByIdWithMerchant(@Param("id") Long id);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentRequestEntity p WHERE p.id = :id")
    Optional<PaymentRequestEntity> findByIdWithPessimisticLock(@Param("id") Long id);
}
