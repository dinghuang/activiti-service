package org.dinghuang.activiti.infra.repository;

import org.apache.ibatis.annotations.Param;
import org.dinghuang.core.annotation.Repository;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/16
 */
@Repository
public interface ActivitiRepository {

    void updateActReProcdef(String id);
}
