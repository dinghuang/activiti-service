package org.dinghuang.activiti.conf;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.core.common.spring.project.ProjectModelService;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.spring.SpringAsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.dinghuang.activiti.controller.ActivitiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/2
 */
@Configuration
public class ActivitiSpringIdentityAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiController.class);

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 30;
    private static final int KEEP_ALIVE_SECONDS = 300;
    private static final int QUEUE_CAPACITY = 300;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private UserGroupManager userGroupManager;

    @Autowired
    private ActivitiIdGeneratorConfiguration activitiIdGeneratorConfiguration;

    @Autowired
    private ProjectModelService projectModelService;

    /**
     * 处理引擎配置
     */
    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration() {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration(projectModelService);
        configuration.setDataSource(this.dataSource);
        configuration.setTransactionManager(this.platformTransactionManager);
        SpringAsyncExecutor asyncExecutor = new SpringAsyncExecutor();
        asyncExecutor.setTaskExecutor(workFlowAsync());
        configuration.setAsyncExecutor(asyncExecutor);
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setUserGroupManager(this.userGroupManager);
        configuration.setHistoryLevel(HistoryLevel.FULL);
        configuration.setDbHistoryUsed(true);
        configuration.setIdGenerator(this.activitiIdGeneratorConfiguration);
        Resource[] resources = null;
        // 启动自动部署流程
        try {
            resources = new PathMatchingResourcePatternResolver().getResources("classpath*:bpmn/*.bpmn");
        } catch (IOException e) {
            LOGGER.error("Start the automated deployment process error", e);
        }
        configuration.setDeploymentResources(resources);
        return configuration;
    }

    /**
     * 线程池
     */
    @Primary
    @Bean("workFlowTaskExecutor")
    public ThreadPoolTaskExecutor workFlowAsync() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("workFlowTaskExecutor-");
        executor.initialize();
        return executor;
    }

}
