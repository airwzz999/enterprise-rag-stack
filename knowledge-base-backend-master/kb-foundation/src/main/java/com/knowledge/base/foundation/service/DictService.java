package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.dto.DictDTO;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.vo.DictDataVO;
import com.knowledge.base.foundation.vo.DictVO;

import java.util.List;

/**
 * Dictionary Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DictService {

    /**
     * Paginated query of dictionaries
     *
     * @param current current page
     * @param size    page size
     * @param keyword keyword (optional)
     * @return paginated result
     */
    IPage<Dict> pageDicts(Long current, Long size, String keyword);

    /**
     * Get a dictionary by dictionary code
     *
     * @param code dictionary code
     * @return dictionary information
     */
    Dict getDictByCode(String code);

    /**
     * Create a dictionary
     *
     * @param dict dictionary information
     * @return whether it succeeded
     */
    Boolean createDict(Dict dict);

    /**
     * Update a dictionary
     *
     * @param code dictionary code
     * @param dict new dictionary information
     * @return whether it succeeded
     */
    Boolean updateDict(String code, Dict dict);

    /**
     * Delete a dictionary
     *
     * @param code dictionary code
     * @return whether it succeeded
     */
    Boolean deleteDict(String code);

    /**
     * Get the dictionary data list
     *
     * @param code dictionary code
     * @return dictionary data list
     */
    List<DictData> getDictData(String code);

    /**
     * Add a dictionary data item
     *
     * @param code     dictionary code
     * @param dictData dictionary data
     * @return whether it succeeded
     */
    Boolean addDictData(String code, DictData dictData);

    /**
     * Update a dictionary data item
     *
     * @param code     dictionary code
     * @param dictData dictionary data
     * @return whether it succeeded
     */
    Boolean updateDictData(String code, DictData dictData);

    /**
     * Delete a dictionary data item
     *
     * @param code dictionary code
     * @param id   data item ID
     * @return whether it succeeded
     */
    Boolean deleteDictData(String code, Long id);
}
