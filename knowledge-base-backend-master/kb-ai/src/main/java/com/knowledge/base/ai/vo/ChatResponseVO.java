package com.knowledge.base.ai.vo;

import com.knowledge.base.ai.rag.kag.retrieval.GraphContext;
import com.knowledge.base.ai.vo.CitationVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI chat response VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI chat response")
public class ChatResponseVO {

    /**
     * Conversation ID
     */
    @Schema(description = "Conversation ID")
    private Long conversationId;

    /**
     * Message ID
     */
    @Schema(description = "Message ID")
    private Long messageId;

    /**
     * AI reply content
     */
    @Schema(description = "AI reply content")
    private String content;

    /**
     * Token count used
     */
    @Schema(description = "Token count used")
    private Integer tokens;

    /**
     * Conversation title
     */
    @Schema(description = "Conversation title")
    private String title;

    /**
     * List of citation sources (RAG mode)
     */
    @Schema(description = "Citation sources")
    private List<CitationVO> citations;

    /**
     * Whether the response came from the knowledge base
     */
    @Schema(description = "Whether the response used knowledge base retrieval augmentation")
    private boolean fromKnowledgeBase;

    /**
     * Knowledge graph retrieval context (KAG mode, including entities, paths, and associated text chunks)
     */
    @Schema(description = "Knowledge graph context")
    private GraphContext graphContext;
}
