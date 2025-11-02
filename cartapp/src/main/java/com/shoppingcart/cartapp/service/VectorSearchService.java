package com.shoppingcart.cartapp.service;

import com.shoppingcart.cartapp.model.Embedding;
import com.shoppingcart.cartapp.repository.EmbeddingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VectorSearchService {

    @Autowired
    private EmbeddingRepository embeddingRepository;

    @Autowired
    private EmbeddingService embeddingService;

    public static class SearchResult {
        public String docId;
        public Long referencedId;
        public String docType;
        public double score;
        public SearchResult(String docId, Long referencedId, String docType, double score) {
            this.docId = docId;
            this.referencedId = referencedId;
            this.docType = docType;
            this.score = score;
        }
    }

    /**
     * Search embeddings by cosine similarity.
     * docType: if non-null, restrict to that type (e.g., "product")
     * topK: number of results to return
     */
    public List<SearchResult> search(String query, String docType, int topK) {
        double[] qvec = embeddingService.createEmbedding(query);

        List<Embedding> candidates = (docType == null || docType.isBlank()) ?
                embeddingRepository.findAll() : embeddingRepository.findByDocType(docType);

        PriorityQueue<SearchResult> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> r.score));

        for (Embedding e : candidates) {
            try {
                double[] vec = embeddingService.fromJson(e.getVectorJson());
                double score = cosineSimilarity(qvec, vec);
                pq.add(new SearchResult(e.getDocId(), e.getReferencedId(), e.getDocType(), score));
                if (pq.size() > topK) pq.poll();
            } catch (Exception ex) {
                // ignore bad vector
            }
        }

        List<SearchResult> out = new ArrayList<>();
        while (!pq.isEmpty()) out.add(pq.poll());
        Collections.reverse(out);
        return out;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
