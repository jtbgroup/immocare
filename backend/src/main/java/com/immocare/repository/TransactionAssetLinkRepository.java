package com.immocare.repository;

import com.immocare.model.entity.TransactionAssetLink;
import com.immocare.model.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionAssetLinkRepository extends JpaRepository<TransactionAssetLink, Long> {

    List<TransactionAssetLink> findByTransactionId(Long transactionId);

    boolean existsByTransactionIdAndAssetTypeAndAssetId(Long txId, AssetType type, Long assetId);

    long countByAssetTypeAndAssetId(AssetType type, Long assetId);
}
