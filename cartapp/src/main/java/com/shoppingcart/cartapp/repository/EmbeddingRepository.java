package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    Optional<Embedding> findByDocId(String docId);
    List<Embedding> findByDocType(String docType);
}
