package org.dinghuang.activiti.infra.repository;

import org.dinghuang.core.annotation.Repository;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/16
 */
@Repository
public interface ActivitiRepository {

    void deleteEvtLogByExecutionId(String executionId);

    void deleteDetailByExecutionId(String executionId);

    void deleteVarinstByExecutionId(String executionId);

    void deleteTaskinstByExecutionId(String executionId);

    void deleteDeadletterJobByExecutionId(String executionId);

    void deleteEventSubscrByExecutionId(String executionId);

    void deleteIntegrationByExecutionId(String executionId);

    void deleteJobByExecutionId(String executionId);

    void deleteTimerJobByExecutionId(String executionId);

    void deleteSusoendedJobByExecutionId(String executionId);

    void deleteVariableByExecutionId(String executionId);

    void deleteTaskByExecutionId(String executionId);

    void deleteActinstByExecutionId(String executionId);

    void deleteExecutionById(String id);

    void deleteAttachmentByTaskId(String taskId);

    void deleteCommonByTaskId(String taskId);

    void deleteDetailByTaskId(String taskId);

    void deleteIdentityLinkByTaskId(String taskId);

    void deleteRuIdentityLinkByTaskId(String taskId);

}
