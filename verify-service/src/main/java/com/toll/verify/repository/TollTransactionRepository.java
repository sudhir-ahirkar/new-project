package com.toll.verify.repository;

import com.toll.verify.entity.TollTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TollTransactionRepository extends JpaRepository<TollTransaction, UUID> {
    Optional<TollTransaction> findByEventId(String eventId);
}



