package com.knowledge.base.ai.service;

import com.knowledge.base.ai.dto.FeedbackDTO;
import com.knowledge.base.ai.entity.AiFeedback;

import java.util.List;

/**
 * AI feedback service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface AiFeedbackService {

    /**
     * Submit feedback
     *
     * @param feedbackDTO feedback information
     * @param userId      user ID
     * @return whether the operation succeeded
     */
    Boolean submitFeedback(FeedbackDTO feedbackDTO, Long userId);

    /**
     * Get the list of feedback for a user
     *
     * @param userId user ID
     * @return list of feedback
     */
    List<AiFeedback> getUserFeedbacks(Long userId);
}
