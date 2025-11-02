package com.shoppingcart.cartapp.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "embeddings")
public class Embedding implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "product:123" or "faq:uuid"
    @Column(nullable = false, unique = true)
    private String docId;

    @Column(nullable = false)
    private String docType; // "product", "faq", etc.

    @Lob
    @Column(nullable = false, length = 100000)
    private String vectorJson; // JSON array string for the vector

    @Column
    private Long referencedId; // product id or faq id (nullable)

    public Embedding() {}

    public Embedding(String docId, String docType, String vectorJson, Long referencedId) {
        this.docId = docId;
        this.docType = docType;
        this.vectorJson = vectorJson;
        this.referencedId = referencedId;
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getVectorJson() { return vectorJson; }
    public void setVectorJson(String vectorJson) { this.vectorJson = vectorJson; }
    public Long getReferencedId() { return referencedId; }
    public void setReferencedId(Long referencedId) { this.referencedId = referencedId; }
}
