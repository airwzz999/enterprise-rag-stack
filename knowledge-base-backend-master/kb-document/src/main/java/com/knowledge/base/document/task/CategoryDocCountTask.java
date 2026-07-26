package com.knowledge.base.document.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.document.dto.CategoryDocCountDTO;
import com.knowledge.base.document.entity.Category;
import com.knowledge.base.document.mapper.CategoryMapper;
import com.knowledge.base.document.mapper.DocumentMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Category document count synchronization scheduled task
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class CategoryDocCountTask {

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    /**
     * Runs a one-time data initialization asynchronously on application startup
     */
    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000); // Wait for the application to fully start
                recalculateCategoryDocumentCounts();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Startup initialization of category document counts was interrupted");
            }
        }, asyncTaskExecutor);
    }

    /**
     * Recalculates the document count for each category (including subcategories) every 5 minutes,
     * updates the database, and clears the cache
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @CacheEvict(value = "sidebar:categories", allEntries = true)
    public void recalculateCategoryDocumentCounts() {
        log.info("Scheduled task: starting recalculation of category document counts");
        try {
            // 1. Query all categories
            List<Category> allCategories = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>().eq(Category::getDeleted, 0));

            // 2. Query the direct document count per category, converted to a list for lookup
            List<CategoryDocCountDTO> directCounts = documentMapper.countByCategory();

            // 3. Compute the total document count for each category including its subcategories
            for (Category cat : allCategories) {
                int totalCount = calcTotalCount(cat.getId(), allCategories, directCounts);
                categoryMapper.updateDocumentCount(cat.getId(), totalCount);
            }

            log.info("Scheduled task: category document count recalculation complete, updated {} categories", allCategories.size());
        } catch (Exception e) {
            log.error("Scheduled task: failed to recalculate category document counts", e);
        }
    }

    /**
     * Recursively computes the total document count for a category and all its subcategories
     */
    private int calcTotalCount(Long categoryId, List<Category> allCategories,
                               List<CategoryDocCountDTO> directCounts) {
        // Direct document count for the current category (skip uncategorized documents where category_id is null)
        int total = 0;
        for (CategoryDocCountDTO dto : directCounts) {
            if (dto.getCategoryId() != null && dto.getCategoryId().equals(categoryId)) {
                total = dto.getCount();
                break;
            }
        }

        // Recursively add the document counts of all subcategories
        for (Category cat : allCategories) {
            Long parentId = cat.getParentId() != null ? cat.getParentId() : 0L;
            if (parentId.equals(categoryId)) {
                total += calcTotalCount(cat.getId(), allCategories, directCounts);
            }
        }

        return total;
    }
}
