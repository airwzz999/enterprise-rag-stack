package com.knowledge.base.ai.rag.kag.extraction;

import com.knowledge.base.ai.config.KAGProperties;
import com.knowledge.base.ai.dto.kag.extraction.ExtractedEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity normalizer
 *
 * <p>Responsible for:
 * 1. Checking whether extracted entities already exist in the Neo4j graph
 * 2. Merging entities with the same name or high similarity
 * 3. Returning the final deduplicated and merged entity list</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityNormalizer {

    private final Neo4jClient neo4jClient;
    private final KAGProperties kagProperties;

    /**
     * Normalize the entity list (deduplication, merging, conflict resolution)
     *
     * @param entities the raw list of extracted entities
     * @return the normalized entity list
     */
    public List<ExtractedEntity> normalize(List<ExtractedEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: merge duplicate entities within the list (same name)
        entities = mergeDuplicateNames(entities);

        // Step 2: check for entities that already exist in Neo4j
        entities = resolveWithExisting(entities);

        // Step 3: merge by internal similarity (highly similar entities within the same batch)
        entities = mergeBySimilarity(entities);

        return entities;
    }

    /**
     * Merge entities with the same name (within the same batch)
     */
    private List<ExtractedEntity> mergeDuplicateNames(List<ExtractedEntity> entities) {
        Map<String, ExtractedEntity> merged = new LinkedHashMap<>();

        for (ExtractedEntity entity : entities) {
            String key = entity.getName().toLowerCase().trim();
            if (merged.containsKey(key)) {
                ExtractedEntity existing = merged.get(key);
                // Merge aliases
                Set<String> allAliases = new LinkedHashSet<>();
                if (existing.getAliases() != null) allAliases.addAll(existing.getAliases());
                if (entity.getAliases() != null) allAliases.addAll(entity.getAliases());
                existing.setAliases(new ArrayList<>(allAliases));
                // Keep the longer description
                if (entity.getDescription() != null && entity.getDescription().length() >
                        (existing.getDescription() != null ? existing.getDescription().length() : 0)) {
                    existing.setDescription(entity.getDescription());
                }
                // Keep the type with the highest confidence
                Double entityConf = entity.getConfidence() != null ? entity.getConfidence() : 0.0;
                Double existingConf = existing.getConfidence() != null ? existing.getConfidence() : 0.0;
                if (entityConf > existingConf) {
                    existing.setType(entity.getType());
                }
            } else {
                merged.put(key, entity);
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * Resolve conflicts against entities already present in Neo4j
     */
    private List<ExtractedEntity> resolveWithExisting(List<ExtractedEntity> entities) {
        if (entities.isEmpty()) return entities;

        // Collect all entity names for a batch query
        List<String> names = entities.stream()
                .map(ExtractedEntity::getName)
                .collect(Collectors.toList());

        // Batch-query entities already existing in Neo4j
        Set<String> existingNames = queryExistingEntityNames(names);
        log.debug("Found {} existing entities out of {} candidates", existingNames.size(), names.size());

        // For existing entities: mark as existing (keep new aliases)
        for (ExtractedEntity entity : entities) {
            // Ensure confidence is not null
            if (entity.getConfidence() == null) {
                entity.setConfidence(0.8);
            }
            if (existingNames.contains(entity.getName())) {
                entity.setConfidence(Math.min(entity.getConfidence(), 0.95)); // Lower the confidence
            }
            // Also check whether any alias matches an existing entity
            if (entity.getAliases() != null) {
                for (String alias : entity.getAliases()) {
                    if (existingNames.contains(alias)) {
                        // The alias already exists as an independent entity; add it to that entity's alias list
                        log.debug("Alias '{}' of entity '{}' exists as independent entity", alias, entity.getName());
                    }
                }
            }
        }

        return entities;
    }

    /**
     * Merge entities based on name similarity
     */
    private List<ExtractedEntity> mergeBySimilarity(List<ExtractedEntity> entities) {
        if (entities.size() <= 1) return entities;

        double threshold = kagProperties.getExtraction().getSimilarityThreshold();
        List<ExtractedEntity> result = new ArrayList<>();
        boolean[] merged = new boolean[entities.size()];

        for (int i = 0; i < entities.size(); i++) {
            if (merged[i]) continue;
            ExtractedEntity base = entities.get(i);

            for (int j = i + 1; j < entities.size(); j++) {
                if (merged[j]) continue;
                ExtractedEntity other = entities.get(j);

                double similarity = computeNameSimilarity(base.getName(), other.getName());
                if (similarity >= threshold) {
                    // Merge base and other
                    if (base.getAliases() == null) base.setAliases(new ArrayList<>());
                    if (!base.getAliases().contains(other.getName())) {
                        base.getAliases().add(other.getName());
                    }
                    if (other.getAliases() != null) {
                        base.getAliases().addAll(other.getAliases());
                    }
                    merged[j] = true;
                    log.debug("Merged similar entities: '{}' ← '{}' (similarity={})",
                            base.getName(), other.getName(), String.format("%.2f", similarity));
                }
            }
            result.add(base);
        }
        return result;
    }

    /**
     * Batch-query the set of entity names already existing in Neo4j
     */
    private Set<String> queryExistingEntityNames(List<String> names) {
        if (names.isEmpty()) return Collections.emptySet();

        try {
            var result = neo4jClient.query("""
                    UNWIND $names AS name
                    MATCH (e:KnowledgeEntity)
                    WHERE e.name = name OR name IN coalesce(e.aliases, [])
                    RETURN DISTINCT e.name AS existingName
                    """)
                    .bind(names).to("names")
                    .fetch()
                    .all();

            return result.stream()
                    .map(r -> (String) r.get("existingName"))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to query existing entities from Neo4j: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Compute a simple similarity between two entity names (normalized edit distance)
     */
    private double computeNameSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        String sa = a.toLowerCase().trim();
        String sb = b.toLowerCase().trim();
        if (sa.equals(sb)) return 1.0;
        if (sa.contains(sb) || sb.contains(sa)) return 0.9;

        int maxLen = Math.max(sa.length(), sb.length());
        if (maxLen == 0) return 1.0;

        int distance = levenshteinDistance(sa, sb);
        return 1.0 - (double) distance / maxLen;
    }

    /**
     * Levenshtein edit distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
