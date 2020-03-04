package org.dinghuang.activiti.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.bpmn.model.*;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.FileUtils;
import org.dinghuang.core.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/3
 */
public class ActivitiUtils {

    private ActivitiUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiUtils.class);

    private static final String USER_TASK = "userTask";

    public static ProcessEngine getProcessEngine() {
        return ProcessEngines.getDefaultProcessEngine();
    }

    public static RepositoryService getRepositoryService() {
        return getProcessEngine().getRepositoryService();
    }

    public static TaskService getTaskService() {
        return getProcessEngine().getTaskService();
    }

    public static RuntimeService getRuntimeService() {
        return getProcessEngine().getRuntimeService();
    }

    public static ProcessRuntime getProcessRuntime() {
        return (ProcessRuntime) SpringContextUtils.getContext().getBean("processRuntime");
    }

    public static ProcessDiagramGenerator getProcessDiagramGenerator() {
        return new DefaultProcessDiagramGenerator();
    }

    public static HistoryService getHistoryService() {
        return getProcessEngine().getHistoryService();
    }

    public static Long getId() {
        return IdWorker.getId();
    }

    public static void main(String[] args) {
        System.out.println(getId());
    }

    /**
     * 根据路径部署(还有Inputstream addInputStream、字符串方式 addString部署、压缩包方式 addZipInputStream)
     *
     * @param bpmnPath diagrams/myHelloWorld.bpmn
     * @param bpmnPng  diagrams/myHelloWorld.png
     */
    public static void deployProcessByPath(String bpmnPath, String bpmnPng) {
        Deployment deployment = getProcessEngine().getRepositoryService()
                .createDeployment()
                .addClasspathResource(bpmnPath)
                .addClasspathResource(bpmnPng)
                .deploy();
        LOGGER.info("deploy id：{}", deployment.getId());
        LOGGER.info("deploy time：{}", deployment.getDeploymentTime());
    }

    /**
     * 删除给定的部署和级联删除到流程实例、历史流程实例和作业。
     *
     * @param deploymentId deploymentId
     * @param condition    级联删除
     */
    public static void deleteDeployment(String deploymentId, Boolean condition) {
        getRepositoryService().deleteDeployment(deploymentId, condition);
    }

    /**
     * 查看流程定义的资源文件
     *
     * @param outPutPath outPutPath
     */
    public static void viewPng(String outPutPath) {
        //部署ID
        String deploymentId = "1";
        //获取的资源名称
        List<String> list = getProcessEngine().getRepositoryService()
                .getDeploymentResourceNames(deploymentId);
        //获得资源名称后缀.png
        String resourceName = "";
        if (list != null && list.size() > 0) {
            for (String name : list) {
                //返回包含该字符串的第一个字母的索引位置
                if (name.contains(".png")) {
                    resourceName = name;
                }
            }
        }

        //获取输入流，输入流中存放.PNG的文件
        InputStream in = getProcessEngine().getRepositoryService()
                .getResourceAsStream(deploymentId, resourceName);

        //将获取到的文件保存到本地
        try {
            FileUtils.copyInputStreamToFile(in, new File(outPutPath + resourceName));
        } catch (IOException e) {
            LOGGER.error("viewPng error", e);
        }

    }

    /**
     * 设置流程变量(获取流程变量)
     *
     * @param processInstanceId processInstanceId
     * @param assignee          assignee
     * @param variables         variables
     */
    public static void setProcessVariables(String processInstanceId, String assignee, Map<String, Object> variables) {
        TaskService taskService = getProcessEngine().getTaskService();

        //查询当前办理人的任务ID
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(assignee)
                .singleResult();

        //设置流程变量【基本类型】
        taskService.setVariables(task.getId(), variables);
    }

    /**
     * 查询历史的流程变量
     *
     * @param variableName
     */
    public static List<HistoricVariableInstance> getHistoryProcessVariables(String variableName) {
        return getProcessEngine().getHistoryService()
                .createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询
                .variableName(variableName)
                .list();
    }

    /**
     * 查询组任务
     *
     * @param candidateUser candidateUser
     */
    public static List<Task> findGroupTaskList(String candidateUser) {
        return getProcessEngine().getTaskService()
                .createTaskQuery()
                .taskCandidateUser(candidateUser)
                .list();
    }

    /**
     * 将组任务指定个人任务(拾取任务)
     */
    public static void claim(String taskId, String userId) {
        getProcessEngine().getTaskService()
                .claim(taskId, userId);
    }

    /**
     * 将个人任务再回退到组任务（前提：之前这个任务是组任务）
     */
    public static void setAssignee(String taskId) {
        //任务ID
        getTaskService().setAssignee(taskId, null);
    }

    /**
     * 向组任务中添加成员
     */
    public static void addGroupUser(String taskId, String userId) {
        getTaskService()//
                .addCandidateUser(taskId, userId);
    }

    /**
     * 向组任务中删除成员
     */
    public static void deleteGroupUser(String taskId, String userId) {
        getTaskService()//
                .deleteCandidateUser(taskId, userId);
    }

    /**
     * 启动流程实例
     *
     * @param processDefinitionKey processDefinitionKey
     * @param name                 name
     * @param variable             参数
     */
    public static ProcessInstance startProcessInstance(String processDefinitionKey, String name, Map<String, Object> variable) {
        ProcessInstance processInstance = getProcessRuntime().start(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionId(processDefinitionKey)
                .withName(name)
                .withVariables(variable)
                .build());
        LOGGER.info("process {} instance start", processInstance.getId());
        LOGGER.info("process instance id：{}", processInstance.getId());
        LOGGER.info("process definition id：{}", processInstance.getProcessDefinitionId());
        return processInstance;
    }

    /**
     * 创建任务节点（多人审批）
     *
     * @param id       id
     * @param name     name
     * @param assignee assignee
     * @return UserTask
     */
    public static UserTask createUsersTask(String id, String name, List<String> assignee) {
        UserTask userTask = new UserTask();
        userTask.setName(name);
        userTask.setId(id);
        userTask.setCandidateUsers(assignee);
        return userTask;
    }

    /**
     * 创建任务节点(单人审批)
     *
     * @param id       id
     * @param name     name
     * @param assignee assignee
     * @return UserTask
     */
    public static UserTask createUserTask(String id, String name, String assignee) {
        UserTask userTask = new UserTask();
        userTask.setName(name);
        userTask.setId(id);
        userTask.setAssignee(assignee);
        return userTask;
    }

    /**
     * 完成任务
     *
     * @param taskId taskId
     */
    public static void completeTask(String taskId, Map<String, Object> variables) {
        getTaskService().setVariables(taskId, variables);
        getTaskService().complete(taskId);
    }

    /**
     * 处理当前用户的任务，背后操作的表：act_hi_actinst，act_hi_identitylink，act_hi_taskinst，act_ru_identitylink，act_ru_task
     *
     * @param processDefinitionKey 流程定义的key
     */
    public static void completeTaskByProcessDefinitionKey(String processDefinitionKey) {
        TaskService taskService = getProcessEngine().getTaskService();

        Task task = taskService.createTaskQuery().processDefinitionKey(processDefinitionKey)
                .taskAssignee(UserUtils.getCurrentUserDetails().getUsername()).singleResult();
        if (task != null) {
            //处理任务,结合当前用户任务列表的查询操作的话
            taskService.complete(task.getId());
        }
    }

    /**
     * 查询任务
     *
     * @param assignee assignee
     */
    public static List<Task> queryTaskList(String assignee) {
        return getTaskService().createTaskQuery().taskAssignee(assignee).list();
    }

    /**
     * 连线
     *
     * @param from from
     * @param to   to
     * @return SequenceFlow
     */
    public static SequenceFlow createSequenceFlow(String from, String to) {
        SequenceFlow flow = new SequenceFlow();
        flow.setSourceRef(from);
        flow.setTargetRef(to);
        return flow;
    }

    /**
     * 开始节点
     *
     * @return StartEvent
     */
    public static StartEvent createStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId("startEvent");
        startEvent.setName("start");
        return startEvent;
    }

    /**
     * 结束节点
     *
     * @return EndEvent
     */
    public static EndEvent createEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId("endEvent");
        endEvent.setName("end");
        return endEvent;
    }


    /**
     * 查询申请人已申请任务(完成状态)
     *
     * @param user                 user
     * @param processDefinitionKey processDefinitionKey
     * @return List<HistoricProcessInstance>
     */
    public static List<HistoricProcessInstance> queryApplyHistory(String user, String processDefinitionKey) {
        return getHistoryService().createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey).startedBy(user).finished()
                .orderByProcessInstanceEndTime().desc().list();
    }


    /**
     * 审批人已办理任务(完成状态)[学习使用]
     *
     * @param user                 user
     * @param processDefinitionKey processDefinitionKey
     * @return
     */
    public static List<HistoricTaskInstance> queryFinished(String user, String processDefinitionKey) {
        List<HistoricProcessInstance> hisProInstance = getHistoryService().createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey).involvedUser(user).finished()
                .orderByProcessInstanceEndTime().desc().list();
        List<HistoricTaskInstance> historicTaskInstanceList = new LinkedList<>();
        for (HistoricProcessInstance hisInstance : hisProInstance) {
            List<HistoricTaskInstance> hisTaskInstanceList = getHistoryService().createHistoricTaskInstanceQuery()
                    .processInstanceId(hisInstance.getId()).processFinished()
                    .taskAssignee(user)
                    .orderByHistoricTaskInstanceEndTime().desc().list();
            for (HistoricTaskInstance taskInstance : hisTaskInstanceList) {
                if (taskInstance.getAssignee().equals(user)) {
                    historicTaskInstanceList.add(taskInstance);
                }
            }

        }
        return historicTaskInstanceList;
    }


    /**
     * 起人查询执行中的任务
     *
     * @param user user
     */
    public static List<org.activiti.engine.runtime.ProcessInstance> queryNow(String user) {
        return getRuntimeService().createProcessInstanceQuery().startedBy(user).list();
    }


    /**
     * 根据人员查询待审批任务
     *
     * @param assignee assignee
     * @return TaskList
     */
    public static List<Task> findUnApprove(String assignee) {
        return getTaskService().createTaskQuery().taskCandidateOrAssigned(assignee).list();
    }

    /**
     * 进行审批
     *
     * @param msg       审批意见
     * @param isAgree   是否同意 1 同意 0 拒绝
     * @param taskId    任务id
     * @param processId 流程id
     * @return Boolean
     */
    public static Boolean approve(String msg, Integer isAgree, String taskId, String processId) {
        Task task = getTaskService().createTaskQuery().taskId(taskId).singleResult();
        //拒绝,结束流程
        if (isAgree == 0) {
            BpmnModel bpmnModel = getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
            Execution execution = getRuntimeService().createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            String activitId = execution.getActivityId();
            FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activitId);
            //清理流程未执行节点
            flowNode.getOutgoingFlows().clear();
            //建立新方向
            List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
            SequenceFlow newSequenceFlow = new SequenceFlow();
            newSequenceFlow.setId(String.valueOf(IdWorker.getId()));
            newSequenceFlow.setSourceFlowElement(flowNode);
            newSequenceFlow.setTargetFlowElement(createEndEvent());
            newSequenceFlowList.add(newSequenceFlow);
            flowNode.setOutgoingFlows(newSequenceFlowList);

        } else if (isAgree == 1) {
            //同意,继续下一节点
            getTaskService().addComment(task.getId(), task.getProcessInstanceId(), msg);
            getTaskService().complete(task.getId());
        }

        return true;
    }

    /**
     * 根据启动key获取最新流程
     *
     * @param processDefinitionKey processDefinitionKey
     * @return ProcessDefinition
     */
    public static List<ProcessDefinition> getLastestProcess(String processDefinitionKey) {
        return getRepositoryService()
                .createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey)
                ///使用流程定义的版本降序排列
                .orderByProcessDefinitionVersion().desc()
                .list();

    }

    /**
     * 获取流程走过的线
     *
     * @param bpmnModel                 流程对象模型
     * @param historicActivityInstances 历史流程已经执行的节点，并已经按执行的先后顺序排序
     * @return List<String> 流程走过的线
     */
    public static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // 用以保存高亮的线flowId
        List<String> highFlows = new ArrayList<>();
        if (historicActivityInstances == null || historicActivityInstances.size() == 0) {
            return highFlows;
        }

        // 遍历历史节点
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            // 取出已执行的节点
            HistoricActivityInstance historicActivityInstance = historicActivityInstances.get(i);

            // 用以保存后续开始时间相同的节点
            List<FlowNode> sameStartTimeNodes = new ArrayList<>();

            // 获取下一个节点（用于连线）
            FlowNode sameActivityImpl = getNextFlowNode(bpmnModel, historicActivityInstances, i, historicActivityInstance);

            // 将后面第一个节点放在时间相同节点的集合里
            if (sameActivityImpl != null) {
                sameStartTimeNodes.add(sameActivityImpl);
            }

            // 循环后面节点，看是否有与此后继节点开始时间相同的节点，有则添加到后继节点集合
            for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
                // 后续第一个节点
                HistoricActivityInstance activityImpl1 = historicActivityInstances.get(j);
                // 后续第二个节点
                HistoricActivityInstance activityImpl2 = historicActivityInstances.get(j + 1);
                if (activityImpl1.getStartTime().getTime() != activityImpl2.getStartTime().getTime()) {
                    break;
                }

                // 如果第一个节点和第二个节点开始时间相同保存
                FlowNode sameActivityImpl2 = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityImpl2.getActivityId());
                sameStartTimeNodes.add(sameActivityImpl2);
            }

            // 得到节点定义的详细信息
            FlowNode activityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i).getActivityId());
            // 取出节点的所有出去的线，对所有的线进行遍历
            List<SequenceFlow> pvmTransitions = activityImpl.getOutgoingFlows();
            for (SequenceFlow pvmTransition : pvmTransitions) {
                // 获取节点
                FlowNode pvmActivityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(pvmTransition.getTargetRef());

                // 不是后继节点
                if (!sameStartTimeNodes.contains(pvmActivityImpl)) {
                    continue;
                }

                // 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
                highFlows.add(pvmTransition.getId());
            }
        }

        //返回高亮的线
        return highFlows;
    }


    /**
     * 获取下一个节点信息
     *
     * @param bpmnModel                 流程模型
     * @param historicActivityInstances 历史节点
     * @param i                         当前已经遍历到的历史节点索引（找下一个节点从此节点后）
     * @param historicActivityInstance  当前遍历到的历史节点实例
     * @return FlowNode 下一个节点信息
     */
    private static FlowNode getNextFlowNode(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances, int i, HistoricActivityInstance historicActivityInstance) {
        // 保存后一个节点
        FlowNode flowNode = null;

        // 如果当前节点不是用户任务节点，则取排序的下一个节点为后续节点
        if (!USER_TASK.equals(historicActivityInstance.getActivityType())) {
            // 是最后一个节点，没有下一个节点
            if (i == historicActivityInstances.size()) {
                return flowNode;
            }
            // 不是最后一个节点，取下一个节点为后继节点
            // 找到紧跟在后面的一个节点
            flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i + 1).getActivityId());
            // 返回
            return flowNode;
        }

        // 遍历后续节点，获取当前节点后续节点
        for (int k = i + 1; k <= historicActivityInstances.size() - 1; k++) {
            // 后续节点
            HistoricActivityInstance historicActivityInstanceAfter = historicActivityInstances.get(k);
            // 都是userTask，且主节点与后续节点的开始时间相同，说明不是真实的后继节点
            if (USER_TASK.equals(historicActivityInstanceAfter.getActivityType()) && historicActivityInstance.getStartTime().getTime() == historicActivityInstanceAfter.getStartTime().getTime()) {
                continue;
            }
            // 找到紧跟在后面的一个节点
            flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(k).getActivityId());
            break;
        }
        return flowNode;
    }


    /**
     * 查询当前用户的任务列表
     *
     * @param processDefinitionKey 流程定义的key
     */
    public static List<Task> findPersonalTaskList(String processDefinitionKey) {
        TaskService taskService = getProcessEngine().getTaskService();

        //根据流程定义的key,负责人assignee来实现当前用户的任务列表查询
        return taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                .taskAssignee(UserUtils.getCurrentUserDetails().getUsername())
                .list();
    }

    /**
     * 查询流程历史信息
     *
     * @param processDefinitionKey processDefinitionKey
     * @return HistoricActivityInstance
     */
    public static List<HistoricActivityInstance> queryHistory(String processDefinitionKey) {
        HistoryService historyService = getProcessEngine().getHistoryService();
        RepositoryService repositoryService = getProcessEngine().getRepositoryService();
        //查询流程定义
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        //遍历查询结果
        ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc().singleResult();

        if (processDefinition != null) {
            HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
            //排序StartTime
            return query.processDefinitionId(processDefinition.getId())
                    .orderByHistoricActivityInstanceStartTime().asc().list();
        } else {
            return new ArrayList<>(0);
        }
    }

    /**
     * 输出图像
     *
     * @param response               响应实体
     * @param bpmnModel              图像对象
     * @param flowIds                已执行的线集合
     * @param executedActivityIdList void 已执行的节点ID集合
     */
    public static void outputImg(HttpServletResponse response, BpmnModel bpmnModel, List<String> flowIds, List<String> executedActivityIdList) {
        InputStream imageStream = null;
        try {
            imageStream = getProcessDiagramGenerator().generateDiagram(bpmnModel, executedActivityIdList, flowIds, "宋体", "微软雅黑", "黑体", true, "png");
            // 输出资源内容到相应对象
            byte[] b = new byte[1024];
            int len;
            while ((len = imageStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
            response.getOutputStream().flush();
        } catch (Exception e) {
            LOGGER.error("out put process img error！", e);
        } finally { // 流关闭
            try {
                if (imageStream != null) {
                    imageStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("IoException", e);
            }
        }
    }

    /**
     * 判断流程是否完成
     *
     * @param processInstanceId 流程实例ID
     * @return boolean 已完成-true，未完成-false
     */
    public static boolean isFinished(String processInstanceId) {
        return getHistoryService().createHistoricProcessInstanceQuery().finished().processInstanceId(processInstanceId).count() > 0;
    }

}
