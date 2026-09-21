package com.docsense.rag.rag;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Native-SQL access to the pgvector {@code embedding} column: writing a
 * chunk's vector and performing cosine-nearest-neighbour retrieval. Kept
 * separate from the JPA repositories so the vendor-specific vector type never
 * enters the entity model. All search is scoped to the requesting user, and the
 * cosine operator {@code <=>} is served by the HNSW index created in V1.
 */
@Repository
public class VectorRepository {

    @PersistenceContext
    private EntityManager em;

    /** Stores the embedding vector for an already-persisted chunk row. */
    public void updateEmbedding(long chunkId, float[] embedding) {
        em.createNativeQuery("UPDATE document_chunks SET embedding = CAST(:vec AS vector) WHERE id = :id")
                .setParameter("vec", toPgVector(embedding))
                .setParameter("id", chunkId)
                .executeUpdate();
    }

    /**
     * Returns the top-K chunks whose embeddings are closest (cosine) to the
     * query vector, restricted to the given user and, optionally, a set of
     * document ids (multi-document RAG scoping).
     */
    @SuppressWarnings("unchecked")
    public List<RetrievedChunk> search(float[] query, Long userId, Collection<Long> documentIds, int topK) {
        boolean filterDocs = documentIds != null && !documentIds.isEmpty();
        String sql = "SELECT c.id, c.document_id, d.filename, c.page_number, c.content, "
                + " (c.embedding <=> CAST(:q AS vector)) AS distance "
                + "FROM document_chunks c "
                + "JOIN documents d ON d.id = c.document_id "
                + "WHERE d.user_id = :userId AND c.embedding IS NOT NULL "
                + (filterDocs ? "AND c.document_id IN (:ids) " : "")
                + "ORDER BY c.embedding <=> CAST(:q AS vector) ASC "
                + "LIMIT :topK";
        Query q = em.createNativeQuery(sql)
                .setParameter("q", toPgVector(query))
                .setParameter("userId", userId)
                .setParameter("topK", topK);
        if (filterDocs) {
            q.setParameter("ids", documentIds);
        }
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(VectorRepository::mapRow).toList();
    }

    private static RetrievedChunk mapRow(Object[] r) {
        return new RetrievedChunk(
                ((Number) r[0]).longValue(),
                ((Number) r[1]).longValue(),
                (String) r[2],
                r[3] == null ? null : ((Number) r[3]).intValue(),
                (String) r[4],
                ((Number) r[5]).doubleValue());
    }

    /** Formats a float array as a pgvector literal, e.g. {@code [0.1,0.2,...]}. */
    static String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
