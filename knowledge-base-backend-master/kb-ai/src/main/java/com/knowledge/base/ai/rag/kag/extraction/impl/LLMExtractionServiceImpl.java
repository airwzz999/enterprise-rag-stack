package com.knowledge.base.ai.rag.kag.extraction.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.knowledge.base.ai.config.ModelProvider;
import com.knowledge.base.ai.config.KAGProperties;
import com.knowledge.base.ai.dto.kag.extraction.ExtractedEntity;
import com.knowledge.base.ai.dto.kag.extraction.ExtractedRelation;
import com.knowledge.base.ai.dto.kag.extraction.ExtractionResult;
import com.knowledge.base.ai.rag.kag.extraction.ExtractionException;
import com.knowledge.base.ai.rag.kag.extraction.ExtractionService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-based entity and relation extraction service implementation
 *
 * <p>Core logic: Build prompt → LLM completion → JSON parse → return ExtractionResult.
 * Uses a carefully designed prompt template to ensure the LLM outputs structured JSON.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMExtractionServiceImpl implements ExtractionService {

    private final ModelProvider modelProvider;
    private final KAGProperties kagProperties;

    /** Regex used to extract JSON from the LLM's reply */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}",
            Pattern.DOTALL);

    /** Extraction system prompt */
    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You are a knowledge graph construction expert. Strictly extract knowledge entities and relations from the following document excerpt.

            ## Extraction Rules
            1. Entities should be technical concepts explicitly mentioned in the document that carry real meaning (tech stack names, API names, config item names, core concepts, tool names, process names)
            2. Do not extract overly generic concepts (such as "system", "feature", "data", etc.)
            3. Relations must be associations between entities that are explicitly supported by the document
            4. Extract at most %d entities and %d relations per document excerpt

            ## Entity Types
            - TECH_STACK: technology stack/framework (e.g. Spring Boot, Redis, MySQL)
            - API: interface/API (e.g. REST API, getUserById)
            - CONFIG: configuration item (e.g. application.yml, server.port)
            - CONCEPT: core concept (e.g. inversion of control, AOP, distributed transactions)
            - TOOL: tool/middleware (e.g. Docker, Maven, Git)
            - PROCESS: process/procedure (e.g. deployment process, authentication process)

            ## Relation Types
            - DEPENDS_ON: dependency relation (A depends on B to run)
            - USES: usage relation (A uses B's functionality)
            - CONFIGURES: configuration relation (A is used to configure B)
            - HAS_PART: composition relation (A is part of B)
            - RELATED_TO: other related relation

            ## Output Format
            Return strictly the following JSON format, with no markdown markup or extra explanation:
            {
              "entities": [
                {"name": "entity name", "type": "entity type", "description": "one-sentence description", "aliases": ["alias1"]}
              ],
              "relations": [
                {"source": "source entity name", "target": "target entity name", "relation": "relation type", "weight": 0.8}
              ]
            }
            """;

    /** {@inheritDoc} */
    @Override
    public ExtractionResult extract(String content, String heading, Long docId, String documentTitle) {
        if (content == null || content.isBlank()) {
            return ExtractionResult.builder()
                    .chunkId(null).docId(docId)
                    .entities(Collections.emptyList())
                    .relations(Collections.emptyList())
                    .build();
        }

        int maxEntities = kagProperties.getExtraction().getMaxEntitiesPerChunk();
        int maxRelations = kagProperties.getExtraction().getMaxRelationsPerChunk();
        String modelName = kagProperties.getExtraction().getModel();
        int maxRetries = kagProperties.getExtraction().getMaxRetries();

        String systemPrompt = EXTRACTION_SYSTEM_PROMPT.formatted(maxEntities, maxRelations);
        String userPrompt = buildUserPrompt(content, heading, documentTitle);

        ExtractionResult result = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ChatLanguageModel model = getModel(modelName);
                String response = model.generate(
                        dev.langchain4j.data.message.SystemMessage.from(systemPrompt),
                        dev.langchain4j.data.message.UserMessage.from(userPrompt)
                ).content().text();

                result = parseResponse(response, docId);
                break;
            } catch (Exception e) {
                log.warn("Entity extraction attempt {} failed for docId={}: {}",
                        attempt + 1, docId, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("Entity extraction failed after {} retries for docId={}",
                            maxRetries + 1, docId);
                    throw new ExtractionException(
                            "Entity extraction failed: " + e.getMessage(), docId, null, e);
                }
            }
        }

        log.debug("Extracted {} entities and {} relations for docId={}",
                result.getEntities().size(), result.getRelations().size(), docId);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<ExtractionResult> extractBatch(List<ExtractionInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExtractionResult> results = new ArrayList<>();
        for (ExtractionInput input : inputs) {
            try {
                ExtractionResult result = extract(
                        input.content(), input.heading(), input.docId(), input.documentTitle());
                if (result != null && !result.isEmpty()) {
                    results.add(result);
                }
            } catch (ExtractionException e) {
                log.warn("Skipping failed extraction for docId={}: {}", e.getDocId(), e.getMessage());
            }
        }
        return results;
    }

    // ==================== Private Methods ====================

    /**
     * Build the user prompt
     */
    private String buildUserPrompt(String content, String heading, String documentTitle) {
        StringBuilder sb = new StringBuilder();
        sb.append("Document title: ").append(documentTitle != null ? documentTitle : "Unknown").append("\n");
        if (heading != null && !heading.isBlank()) {
            sb.append("Section: ").append(heading).append("\n");
        }
        sb.append("\n=== Document Content ===\n");
        sb.append(content.length() > 3000 ? content.substring(0, 3000) : content);
        return sb.toString();
    }

    /**
     * Parse the LLM response into an ExtractionResult
     */
    private ExtractionResult parseResponse(String response, Long docId) {
        // Extract the JSON block
        String json = extractJson(response);
        if (json == null || json.isBlank()) {
            log.warn("No valid JSON found in LLM extraction response for docId={}", docId);
            return ExtractionResult.builder()
                    .docId(docId)
                    .entities(Collections.emptyList())
                    .relations(Collections.emptyList())
                    .build();
        }

        JSONObject root = JSON.parseObject(json);

        List<ExtractedEntity> entities = parseEntities(root.getJSONArray("entities"));
        List<ExtractedRelation> relations = parseRelations(root.getJSONArray("relations"));

        return ExtractionResult.builder()
                .docId(docId)
                .entities(entities)
                .relations(relations)
                .build();
    }

    /**
     * Extract a JSON string from the LLM's reply
     */
    private String extractJson(String response) {
        if (response == null) return null;

        // Try to find a JSON block wrapped in ```json ... ```
        Pattern markdownJson = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```");
        Matcher m = markdownJson.matcher(response);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Fallback: find the largest JSON object
        Matcher jsonMatcher = JSON_BLOCK_PATTERN.matcher(response);
        String longest = null;
        while (jsonMatcher.find()) {
            String candidate = jsonMatcher.group();
            if (longest == null || candidate.length() > longest.length()) {
                longest = candidate;
            }
        }
        return longest;
    }

    private List<ExtractedEntity> parseEntities(JSONArray entitiesArray) {
        if (entitiesArray == null || entitiesArray.isEmpty()) return Collections.emptyList();

        List<ExtractedEntity> entities = new ArrayList<>();
        for (int i = 0; i < entitiesArray.size(); i++) {
            JSONObject obj = entitiesArray.getJSONObject(i);
            String name = obj.getString("name");
            if (name == null || name.isBlank()) continue;

            List<String> aliases = new ArrayList<>();
            JSONArray aliasesArray = obj.getJSONArray("aliases");
            if (aliasesArray != null) {
                for (int j = 0; j < aliasesArray.size(); j++) {
                    aliases.add(aliasesArray.getString(j));
                }
            }

            entities.add(ExtractedEntity.builder()
                    .name(name.trim())
                    .type(obj.getString("type"))
                    .description(obj.getString("description"))
                    .aliases(aliases)
                    .confidence(obj.getDouble("confidence"))
                    .build());
        }
        return entities;
    }

    private List<ExtractedRelation> parseRelations(JSONArray relationsArray) {
        if (relationsArray == null || relationsArray.isEmpty()) return Collections.emptyList();

        List<ExtractedRelation> relations = new ArrayList<>();
        for (int i = 0; i < relationsArray.size(); i++) {
            JSONObject obj = relationsArray.getJSONObject(i);
            String source = obj.getString("source");
            String target = obj.getString("target");
            String relation = obj.getString("relation");
            if (source == null || target == null || relation == null) continue;

            Double weight = obj.getDouble("weight");
            relations.add(ExtractedRelation.builder()
                    .source(source.trim()).target(target.trim())
                    .relation(relation.trim().toUpperCase())
                    .weight(weight != null ? weight : 0.8)
                    .build());
        }
        return relations;
    }

    private ChatLanguageModel getModel(String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            return modelProvider.getModel(modelName);
        }
        return modelProvider.getDefaultModel();
    }
}
