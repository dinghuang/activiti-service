package org.dinghuang.activiti.conf;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.activiti.engine.impl.cfg.IdGenerator;
import org.springframework.stereotype.Component;

/**
 * 自定义id策略
 *
 * @author dinghuang123@gmail.com
 * @since 2020/3/4
 */
@Component
public class ActivitiIdGeneratorConfiguration implements IdGenerator {

    @Override
    public String getNextId() {
        return String.valueOf(IdWorker.getId());
    }
}
