package com.knowledge.base.statistics.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * AI statistics data Mapper
 *
 * <p>Queries AI usage statistics via the cross-database views kb_ai_conversation / kb_ai_message</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface AiStatisticsMapper {

    /**
     * Counts total AI conversations (smart search count)
     */
    Long countConversations();

    /**
     * Counts total AI user questions (Q&A count)
     */
    Long countUserMessages();
}
