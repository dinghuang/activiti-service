package org.dinghuang.activiti.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.bpmn.model.*;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.impl.HistoricTaskInstanceQueryProperty;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dinghuang.activiti.conf.DeleteTaskCmd;
import org.dinghuang.activiti.conf.SetFLowNodeAndGoCmd;
import org.dinghuang.activiti.infra.repository.ActivitiRepository;
import org.dinghuang.core.exception.CommonValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/3
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class ActivitiUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiUtils.class);

    private static final String USER_TASK = "userTask";
    private static final String PARALLEL_GATEWAY = "parallelGateway";
    private static final String EXCLUSIVE_GATEWAY = "exclusiveGateway";
    private static final String BPMN_NOT_SUPPORT = "bpmn not support";

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ProcessRuntime processRuntime;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ActivitiRepository activitiRepository;

    public RepositoryService getRepositoryService() {
        return this.repositoryService;
    }

    /**
     * 根据路径部署(还有Inputstream addInputStream、字符串方式 addString部署、压缩包方式 addZipInputStream)
     *
     * @param bpmnPath diagrams/myHelloWorld.bpmn
     * @param bpmnPng  diagrams/myHelloWorld.png
     */
    public void deployProcessByPath(String bpmnPath, String bpmnPng) {
        Deployment deployment = repositoryService
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
    public void deleteDeployment(String deploymentId, Boolean condition) {
        repositoryService.deleteDeployment(deploymentId, condition);
    }

    /**
     * 查看流程定义的资源文件
     *
     * @param outPutPath outPutPath
     */
    public void viewPng(String outPutPath) {
        //部署ID
        String deploymentId = "1";
        //获取的资源名称
        List<String> list = repositoryService
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
        InputStream in = repositoryService
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
    public void setProcessVariables(String processInstanceId, String assignee, Map<String, Object> variables) {

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
    public List<HistoricVariableInstance> getHistoryProcessVariables(String variableName) {
        return historyService.createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询
                .variableName(variableName)
                .list();
    }

    /**
     * 查询组任务
     *
     * @param candidateUser candidateUser
     */
    public List<Task> findGroupTaskList(String candidateUser) {
        return taskService.createTaskQuery()
                .taskCandidateUser(candidateUser)
                .list();
    }

    /**
     * 将组任务指定个人任务(拾取任务)
     */
    public void claim(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    /**
     * 将个人任务再回退到组任务（前提：之前这个任务是组任务）
     */
    public void setAssignee(String taskId) {
        //任务ID
        taskService.setAssignee(taskId, null);
    }

    /**
     * 向组任务中添加成员
     */
    public void addGroupUser(String taskId, String userId) {
        taskService.addCandidateUser(taskId, userId);
    }

    /**
     * 向组任务中删除成员
     */
    public void deleteGroupUser(String taskId, String userId) {
        taskService.deleteCandidateUser(taskId, userId);
    }

    /**
     * 启动流程实例
     *
     * @param processDefinitionKey processDefinitionKey
     * @param name                 name
     * @param variable             参数
     */
    public org.activiti.api.process.model.ProcessInstance startProcessInstance(String processDefinitionKey, String name, Map<String, Object> variable) {
        org.activiti.api.process.model.ProcessInstance processInstance = processRuntime.start(ProcessPayloadBuilder
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
    public UserTask createUsersTask(String id, String name, List<String> assignee) {
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
    public UserTask createUserTask(String id, String name, String assignee) {
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
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.setVariables(taskId, variables);
        taskService.complete(taskId);
    }

    /**
     * 处理当前用户的任务，背后操作的表：act_hi_actinst，act_hi_identitylink，act_hi_taskinst，act_ru_identitylink，act_ru_task
     *
     * @param processDefinitionKey 流程定义的key
     */
    public void completeTaskByProcessDefinitionKey(String processDefinitionKey) {
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
    public List<Task> queryTaskList(String assignee) {
        return taskService.createTaskQuery().taskAssignee(assignee).list();
    }

    /**
     * 连线
     *
     * @param from from
     * @param to   to
     * @return SequenceFlow
     */
    public SequenceFlow createSequenceFlow(String from, String to) {
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
    public StartEvent createStartEvent() {
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
    public EndEvent createEndEvent() {
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
    public List<HistoricProcessInstance> queryApplyHistory(String user, String processDefinitionKey) {
        return historyService.createHistoricProcessInstanceQuery()
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
    public List<HistoricTaskInstance> queryFinished(String user, String processDefinitionKey) {
        List<HistoricProcessInstance> hisProInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey).involvedUser(user).finished()
                .orderByProcessInstanceEndTime().desc().list();
        List<HistoricTaskInstance> historicTaskInstanceList = new LinkedList<>();
        for (HistoricProcessInstance hisInstance : hisProInstance) {
            List<HistoricTaskInstance> hisTaskInstanceList = historyService.createHistoricTaskInstanceQuery()
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
    public List<org.activiti.engine.runtime.ProcessInstance> queryNow(String user) {
        return runtimeService.createProcessInstanceQuery().startedBy(user).list();
    }


    /**
     * 根据人员查询待审批任务
     *
     * @param assignee assignee
     * @return TaskList
     */
    public List<Task> findUnApprove(String assignee) {
        return taskService.createTaskQuery().taskCandidateOrAssigned(assignee).list();
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
    public Boolean approve(String msg, Integer isAgree, String taskId, String processId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //拒绝,结束流程
        if (isAgree == 0) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
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
            taskService.addComment(task.getId(), task.getProcessInstanceId(), msg);
            taskService.complete(task.getId());
        }

        return true;
    }

    /**
     * 根据启动key获取最新流程
     *
     * @param processDefinitionKey processDefinitionKey
     * @return ProcessDefinition
     */
    public List<ProcessDefinition> getLastestProcess(String processDefinitionKey) {
        return repositoryService
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
    public List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances, List<String> taskIds, String processInstanceId) {
        // 用以保存高亮的线flowId
        List<String> highFlows = new ArrayList<>();
        if (historicActivityInstances == null || historicActivityInstances.size() == 0) {
            return highFlows;
        }
        Map<String, HistoricActivityInstance> historicActivityInstanceMap = historicActivityInstances.stream()
                .collect(Collectors.toMap(HistoricActivityInstance::getActivityId,
                        historicActivityInstance -> historicActivityInstance, BinaryOperator.maxBy(Comparator.comparing(HistoricActivityInstance::getId))));
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(historicActivityInstances.get(0).getProcessInstanceId()).list();
        Map<String, HistoricVariableInstance> historicVariableInstanceMap = historicVariableInstances.stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getVariableName,
                        historicVariableInstance -> historicVariableInstance, BinaryOperator.maxBy(Comparator.comparing(HistoricVariableInstance::getId))));
        // 遍历历史节点
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            // 取出已执行的节点
            HistoricActivityInstance historicActivityInstance = historicActivityInstances.get(i);

            // 用以保存后续开始时间相同的节点
            List<FlowNode> sameStartTimeNodes = new ArrayList<>();

            // 获取下一个节点（用于连线）
            List<FlowNode> sameActivityImpl = getNextFlowNode(bpmnModel, historicActivityInstanceMap, i, historicActivityInstance);

            // 将后面第一个节点放在时间相同节点的集合里
            if (sameActivityImpl != null) {
                sameStartTimeNodes.addAll(sameActivityImpl);
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
                if (historicActivityInstanceMap.get(pvmTransition.getSourceRef()) != null) {
                    if (taskIds != null && !taskIds.isEmpty()) {
                        if (!taskIds.contains(historicActivityInstanceMap.get(pvmTransition.getSourceRef()).getTaskId())) {
                            //有些线是有条件的
                            if (historicActivityInstanceMap.get(pvmTransition.getTargetRef()) != null && querySequenceFlowCondition(pvmTransition, historicVariableInstanceMap)) {
                                highFlows.add(pvmTransition.getId());
                            }
                        }
                    } else {
                        if (historicActivityInstanceMap.get(pvmTransition.getTargetRef()) != null && querySequenceFlowCondition(pvmTransition, historicVariableInstanceMap)) {
                            highFlows.add(pvmTransition.getId());
                        }
                    }
                }
            }
        }

        //返回高亮的线
        return highFlows;
    }

    private boolean querySequenceFlowCondition(SequenceFlow pvmTransition, Map<String, HistoricVariableInstance> historicVariableInstanceMap) {
        String conditionExpression = pvmTransition.getConditionExpression();
        if (StringUtils.isNotEmpty(conditionExpression) && StringUtils.isNotBlank(conditionExpression)) {
            conditionExpression = conditionExpression.substring(conditionExpression.indexOf('{') + 1, conditionExpression.indexOf('}'));
            List<String> strings = Arrays.asList(conditionExpression.split("=="));
            strings.forEach(s -> s = s.replace(" ", ""));
            if (historicVariableInstanceMap.get(strings.get(0)).getValue().equals(strings.get(1).replaceAll("\"", ""))) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }

    }


    /**
     * 获取下一个节点信息
     *
     * @param bpmnModel                   流程模型
     * @param historicActivityInstanceMap historicActivityInstanceMap
     * @param i                           当前已经遍历到的历史节点索引（找下一个节点从此节点后）
     * @param historicActivityInstance    当前遍历到的历史节点实例
     * @return FlowNode 下一个节点信息
     */
    private static List<FlowNode> getNextFlowNode(BpmnModel bpmnModel, Map<String, HistoricActivityInstance> historicActivityInstanceMap, int i, HistoricActivityInstance historicActivityInstance) {
        // 保存后一个节点
        List<FlowNode> flowNodeList = new ArrayList<>();

        // 如果当前节点不是用户任务节点，则取排序的下一个节点为后续节点
        // 是最后一个节点，没有下一个节点
        if (i == historicActivityInstanceMap.size()) {
            return flowNodeList;
        }
        // 不是最后一个节点，取下一个节点为后继节点
        FlowNode activityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId());
        // 取出节点的所有出去的线，对所有的线进行遍历
        List<SequenceFlow> pvmTransitions = activityImpl.getOutgoingFlows();
        if (pvmTransitions.size() == 1) {
            if (historicActivityInstanceMap.get(pvmTransitions.get(0).getTargetRef()) != null) {
                FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstanceMap.get(pvmTransitions.get(0).getTargetRef()).getActivityId());
                flowNodeList.add(flowNode);
                return flowNodeList;
            }

        } else {
            for (SequenceFlow sequenceFlow : pvmTransitions) {
                FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef());
                flowNodeList.add(flowNode);
            }
            // 返回
            return flowNodeList;
        }
        return flowNodeList;
    }


    /**
     * 查询当前用户的任务列表
     *
     * @param processDefinitionKey 流程定义的key
     */
    public List<Task> findPersonalTaskList(String processDefinitionKey) {
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
    public List<HistoricActivityInstance> queryHistory(String processDefinitionKey) {
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
    public void outputImg(HttpServletResponse response, BpmnModel bpmnModel, List<String> flowIds, List<String> executedActivityIdList) {
        InputStream imageStream = null;
        try {
            imageStream = new DefaultProcessDiagramGenerator().generateDiagram(bpmnModel, executedActivityIdList, flowIds, "宋体", "微软雅黑", "黑体", true, "png");
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
    public boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished().processInstanceId(processInstanceId).count() > 0;
    }

    public void showImg(String instanceKey, HttpServletResponse response) {
        if (instanceKey == null) {
            LOGGER.error("process instance not exist");
            return;
        }
        //获取流程实例
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceKey).singleResult();
        if (processInstance == null) {
            LOGGER.error("process instance {} not exist", instanceKey);
            return;
        }

        // 根据流程对象获取流程对象模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        //查看已执行的节点集合, 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
        // 构造历史流程查询
        HistoricActivityInstanceQuery historyInstanceQuery = historyService.createHistoricActivityInstanceQuery().processInstanceId(instanceKey);
        // 查询历史节点
        List<HistoricActivityInstance> historicActivityInstanceList = historyInstanceQuery.orderByHistoricActivityInstanceStartTime().asc().list();
        if (historicActivityInstanceList == null || historicActivityInstanceList.size() == 0) {
            LOGGER.info("process instance history node info not exist", instanceKey);
            outputImg(response, bpmnModel, null, null);
            return;
        }
        //根据id排序
        historicActivityInstanceList = historicActivityInstanceList.stream().sorted(Comparator.comparing(HistoricActivityInstance::getId)).collect(Collectors.toList());
        //处理撤回这种情况
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instanceKey).orderByTaskCreateTime().asc().list();
        List<String> taskIds = null;
        if (tasks != null) {
            //根据id排序
            tasks = tasks.stream().sorted(Comparator.comparing(Task::getId)).collect(Collectors.toList());
            taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
            if (taskIds != null && !taskIds.isEmpty()) {
                //todo 并行这种还有点问题
                List<HistoricActivityInstance> newHistoricActivityInstanceList = new ArrayList<>(historicActivityInstanceList.size());
                for (int i = 0; i < historicActivityInstanceList.size(); i++) {
                    if (historicActivityInstanceList.get(i).getTaskId() == null || !historicActivityInstanceList.get(i).getActivityId().equals(tasks.get(tasks.size() - 1).getTaskDefinitionKey())) {
                        newHistoricActivityInstanceList.add(historicActivityInstanceList.get(i));
                    } else {
                        newHistoricActivityInstanceList.add(historicActivityInstanceList.get(i));
                        historicActivityInstanceList = newHistoricActivityInstanceList;
                        break;
                    }
                }
            }
        }

        // 已执行的节点ID集合(将historicActivityInstanceList中元素的activityId字段取出封装到executedActivityIdList)
        List<String> executedActivityIdList = historicActivityInstanceList.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());

        //获取流程走过的线
        List<String> flowIds = getHighLightedFlows(bpmnModel, historicActivityInstanceList, taskIds, instanceKey);

        //输出图像，并设置高亮
        outputImg(response, bpmnModel, flowIds, executedActivityIdList);
    }

    /**
     * 撤回任务
     *
     * @param currentTaskId currentTaskId
     * @param targetTaskId  targetTaskId 目标任务，如果为空，默认返回上级，如果找到上级有2个，那目标任务必须得传
     */
    @Transactional(rollbackFor = Exception.class)
    public void backTask(String currentTaskId, String targetTaskId) {
        //准备数据
        RepositoryService repositoryService = processEngine.getRepositoryService();
        TaskService taskService = processEngine.getTaskService();
        // 当前任务
        Task currentTask = taskService.createTaskQuery().taskId(currentTaskId).singleResult();
        String processInstanceId = currentTask.getProcessInstanceId();
        // 获取流程定义
        org.activiti.bpmn.model.Process process = repositoryService
                .getBpmnModel(currentTask.getProcessDefinitionId())
                .getMainProcess();
        //任务历史数据
        List<HistoricTaskInstance> historicTaskInstances = historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceId(currentTask.getProcessInstanceId())
                .orderBy(HistoricTaskInstanceQueryProperty.HISTORIC_TASK_INSTANCE_ID)
                .desc()
                .list();
        Map<String, HistoricTaskInstance> historicTaskInstanceMap = historicTaskInstances.stream().collect(Collectors.toMap(HistoricTaskInstance::getId, Function.identity()));
        //所有节点操作数据
        HistoricActivityInstanceQuery historyInstanceQuery = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId);
        List<HistoricActivityInstance> historicActivityInstanceList = historyInstanceQuery.orderByHistoricActivityInstanceStartTime().asc().list();
        Map<String, List<HistoricActivityInstance>> historicActivityInstanceMap = historicActivityInstanceList.stream().collect(Collectors.groupingBy(HistoricActivityInstance::getActivityId));
        Map<String, FlowNode> flowNodeMap = new HashMap<>(historicActivityInstanceList.size());
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstanceList) {
            if (flowNodeMap.get(historicActivityInstance.getActivityId()) == null) {
                FlowNode sourceNode = (FlowNode) process.getFlowElement(historicActivityInstance.getActivityId());
                flowNodeMap.put(historicActivityInstance.getActivityId(), sourceNode);
            }
        }
        //排除当前任务外的所有正在进行的任务
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list().stream().filter(task -> !task.getId().equals(currentTask.getId())).collect(Collectors.toList());
        handleBackTask(currentTask, currentTask.getTaskDefinitionKey(), targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleBackTask(Task currentTask, String taskDefinitionKey, String targetTaskId, Map<String, HistoricTaskInstance> historicTaskInstanceMap, Map<String, List<HistoricActivityInstance>> historicActivityInstanceMap, Map<String, FlowNode> flowNodeMap, List<Task> taskList, List<HistoricActivityInstance> historicActivityInstanceList) {
        //判断是否并行
        if (taskList == null || taskList.isEmpty()) {
            //串行
            handleSerial(currentTask, taskDefinitionKey, targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
        } else {
            //并行
            handleParallel(currentTask, taskDefinitionKey, targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleParallel(Task currentTask, String taskDefinitionKey, String targetTaskId, Map<String, HistoricTaskInstance> historicTaskInstanceMap, Map<String, List<HistoricActivityInstance>> historicActivityInstanceMap, Map<String, FlowNode> flowNodeMap, List<Task> taskList, List<HistoricActivityInstance> historicActivityInstanceList) {
        List<SequenceFlow> sequenceFlows = flowNodeMap.get(taskDefinitionKey).getIncomingFlows();
        if (sequenceFlows.size() == 1) {
            //当前节点的上级节点只有一条
            SequenceFlow sequenceFlow = sequenceFlows.get(0);
            //查询历史节点
            HistoricActivityInstance historicActivityInstance = historicActivityInstanceList.stream().filter(query -> query.getActivityId().equals(sequenceFlow.getSourceRef())).collect(Collectors.toList()).get(0);
            //判断来源类型
            if (historicActivityInstance.getActivityType().equals(PARALLEL_GATEWAY)) {
                //网关
                //查找网关的父任务
                Set<String> parentFlowNodes = queryParentFlowNode(historicActivityInstance.getActivityId(), flowNodeMap);
                if (!parentFlowNodes.isEmpty()) {
                    if (parentFlowNodes.size() == 1) {
                        //如果只有一个父节点
                        String activityId = new ArrayList<>(parentFlowNodes).get(0);
                        if (historicActivityInstanceMap.get(activityId).get(0).getActivityType().equals(USER_TASK)) {
                            //用户任务
                            deleteTaskMultiple(flowNodeMap, null, null, activityId, currentTask, taskList, historicActivityInstance.getActivityId());
                        } else {
                            //递归去查找父任务的前一个
                            handleBackTask(currentTask, historicActivityInstanceMap.get(activityId).get(0).getActivityId(), targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
                        }
                    } else {
                        //当前节点的上级节点有多条 这里需要指定要回退的taskId
                        deleteTaskMultiple(flowNodeMap, historicTaskInstanceMap, targetTaskId, null, currentTask, taskList, historicActivityInstance.getActivityId());
                    }
                } else {
                    //没有父级任务，图有问题
                    throw new CommonValidateException("bpmn doc error");
                }

            } else if (historicActivityInstance.getActivityType().equals(USER_TASK)) {
                //用户任务
                deleteTaskMultiple(flowNodeMap, null, null, historicActivityInstance.getActivityId(), currentTask, taskList, historicActivityInstance.getActivityId());
            } else {
                //todo 还没想好这种场景
                throw new CommonValidateException(BPMN_NOT_SUPPORT);
            }
        } else {
            //当前节点的上级节点有多条 这里需要指定要回退的taskId
            deleteTaskMultiple(flowNodeMap, historicTaskInstanceMap, targetTaskId, null, currentTask, taskList, null);

        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleSerial(Task currentTask, String taskDefinitionKey, String targetTaskId, Map<String, HistoricTaskInstance> historicTaskInstanceMap, Map<String, List<HistoricActivityInstance>> historicActivityInstanceMap, Map<String, FlowNode> flowNodeMap, List<Task> taskList, List<HistoricActivityInstance> historicActivityInstanceList) {
        FlowNode currentNode = flowNodeMap.get(taskDefinitionKey);
        List<SequenceFlow> sequenceFlows = currentNode.getIncomingFlows();
        if (sequenceFlows.size() == 1) {
            SequenceFlow sequenceFlow = sequenceFlows.get(0);
            HistoricActivityInstance historicActivityInstance = historicActivityInstanceMap.get(sequenceFlow.getSourceRef()).get(0);
            //网关
            if (historicActivityInstance.getActivityType().equals(PARALLEL_GATEWAY) || historicActivityInstance.getActivityType().equals(EXCLUSIVE_GATEWAY)) {
                //查找网关的父任务
                Set<String> parentFlowNodes = queryParentFlowNode(historicActivityInstance.getActivityId(), flowNodeMap);
                if (!parentFlowNodes.isEmpty()) {
                    handleBackTaskSingle(parentFlowNodes, currentTask, targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
                } else {
                    //当前节点的上级节点有多条 这里需要指定要回退的taskId
                    deleteTaskMultiple(flowNodeMap, historicTaskInstanceMap, targetTaskId, null, currentTask, taskList, null);
                }
            } else if (historicActivityInstance.getActivityType().equals(USER_TASK)) {
                deleteTaskSingle(flowNodeMap, historicActivityInstance.getActivityId(), currentTask.getId());
            } else {
                //todo 还没想好这种场景
                throw new CommonValidateException(BPMN_NOT_SUPPORT);
            }
        } else {
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(currentTask.getProcessInstanceId()).list();
            Map<String, HistoricVariableInstance> historicVariableInstanceMap = historicVariableInstances.stream()
                    .collect(Collectors.toMap(HistoricVariableInstance::getVariableName,
                            historicVariableInstance -> historicVariableInstance, BinaryOperator.maxBy(Comparator.comparing(HistoricVariableInstance::getId))));
            //串行的也有多条连线，可能是通过排他网关过来的
            Set<HistoricActivityInstance> historicActivityInstances = new HashSet<>();
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                //这边他的parent可能是没做过的，要找做过的
                if (historicActivityInstanceMap.get(sequenceFlow.getSourceRef()) != null && querySequenceFlowCondition(sequenceFlow, historicVariableInstanceMap)) {
                    historicActivityInstances.addAll(historicActivityInstanceMap.get(sequenceFlow.getSourceRef()));
                }
            }
            //走过的只有一个
            if (historicActivityInstances.size() == 1) {
                List<HistoricActivityInstance> historicActivityInstancesList = new ArrayList<>(historicActivityInstances);
                if (historicActivityInstancesList.get(0).getActivityType().equals(USER_TASK)) {
                    deleteTaskSingle(flowNodeMap, historicActivityInstancesList.get(0).getActivityId(), currentTask.getId());
                } else if (historicActivityInstancesList.get(0).getActivityType().equals(EXCLUSIVE_GATEWAY)) {
                    //排他网关
                    Set<String> parentFlowNodes = queryParentFlowNode(historicActivityInstancesList.get(0).getActivityId(), flowNodeMap);
                    handleBackTaskSingle(parentFlowNodes, currentTask, targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
                } else {
                    //todo 还没想好这种场景
                    throw new CommonValidateException(BPMN_NOT_SUPPORT);
                }
            } else {
                //当前节点的上级节点有多条 这里需要指定要回退的taskId
                deleteTaskMultiple(flowNodeMap, historicTaskInstanceMap, targetTaskId, null, currentTask, taskList, null);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleBackTaskSingle(Set<String> parentFlowNodes, Task currentTask, String targetTaskId, Map<String, HistoricTaskInstance> historicTaskInstanceMap, Map<String, List<HistoricActivityInstance>> historicActivityInstanceMap, Map<String, FlowNode> flowNodeMap, List<Task> taskList, List<HistoricActivityInstance> historicActivityInstanceList) {
        if (parentFlowNodes.size() == 1) {
            List<String> parentFlowNodeList = new ArrayList<>(parentFlowNodes);
            if (historicActivityInstanceMap.get(parentFlowNodeList.get(0)).get(0).getActivityType().equals(USER_TASK)) {
                deleteTaskSingle(flowNodeMap, parentFlowNodeList.get(0), currentTask.getId());
            } else {
                //递归去查找父任务的前一个
                handleBackTask(currentTask, historicActivityInstanceMap.get(parentFlowNodeList.get(0)).get(0).getActivityId(), targetTaskId, historicTaskInstanceMap, historicActivityInstanceMap, flowNodeMap, taskList, historicActivityInstanceList);
            }
        } else {
            //当前节点的上级节点有多条 这里需要指定要回退的taskId
            deleteTaskMultiple(flowNodeMap, historicTaskInstanceMap, targetTaskId, null, currentTask, taskList, null);
        }
    }

    private void validatorTargetTask(Map<String, HistoricTaskInstance> historicTaskInstanceMap, String targetTaskId) {
        if (StringUtils.isEmpty(targetTaskId) || StringUtils.isBlank(targetTaskId)) {
            throw new CommonValidateException("target task id cannot be null");
        }
        if (historicTaskInstanceMap == null || historicTaskInstanceMap.isEmpty()) {
            throw new CommonValidateException("historic task instance cannot be null");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskMultiple(Map<String, FlowNode> flowNodeMap, Map<String, HistoricTaskInstance> historicTaskInstanceMap, String targetTaskId, String targetTaskDefinitionKey, Task currentTask, List<Task> taskList, String targetParentTaskDefinitionKey) {
        if (StringUtils.isEmpty(targetTaskDefinitionKey) || StringUtils.isBlank(targetTaskDefinitionKey)) {
            validatorTargetTask(historicTaskInstanceMap, targetTaskId);
            targetTaskDefinitionKey = historicTaskInstanceMap.get(targetTaskId).getTaskDefinitionKey();
        }
        FlowNode targetNode = flowNodeMap.get(targetTaskDefinitionKey);
        ManagementService managementService = processEngine.getManagementService();
        //删除当前任务
        managementService.executeCommand(new DeleteTaskCmd(currentTask.getId()));
        // 删除当前运行的其他相同父任务的子任务
        Set<Task> sameParentTasks = getSameParentTasks(flowNodeMap, taskList, targetParentTaskDefinitionKey);
        for (Task task : sameParentTasks) {
            managementService.executeCommand(new DeleteTaskCmd(task.getId()));
        }
        // 流程执行到来源节点
        managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, currentTask.getExecutionId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskSingle(Map<String, FlowNode> flowNodeMap, String targetTaskActivitiId, String currentTaskId) {
        ManagementService managementService = processEngine.getManagementService();
        FlowNode targetNode = flowNodeMap.get(targetTaskActivitiId);
        // 删除当前运行任务
        String executionEntityId = managementService.executeCommand(new DeleteTaskCmd(currentTaskId));
        // 流程执行到来源节点
        managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, executionEntityId));
    }

    private Set<String> queryParentFlowNode(String activityId, Map<String, FlowNode> flowNodeMap) {
        Set<String> flowNodeList = new HashSet<>();
        for (String key : flowNodeMap.keySet()) {
            if (!key.equals(activityId)) {
                FlowNode flowNode = flowNodeMap.get(key);
                List<SequenceFlow> sequenceFlows = flowNode.getOutgoingFlows();
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    if (sequenceFlow.getTargetRef().equals(activityId)) {
                        flowNodeList.add(key);
                        break;
                    }
                }
            }
        }
        return flowNodeList;
    }

    private Set<Task> getSameParentTasks(Map<String, FlowNode> flowNodeMap, List<Task> taskList, String taskDefinitionKey) {
        if (taskDefinitionKey == null) {
            return new HashSet<>(taskList);
        }
        Set<Task> tasks = new HashSet<>();
        for (Task task : taskList) {
            List<SequenceFlow> sequenceFlows = flowNodeMap.get(task.getTaskDefinitionKey()).getIncomingFlows();
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                if (sequenceFlow.getSourceRef().equals(taskDefinitionKey)) {
                    tasks.add(task);
                    break;
                }
            }
        }
        return tasks;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<FlowElement> importXml(MultipartFile file) {
        List<FlowElement> flowElements = new LinkedList<>();
        try {
            InputStream fileInputStream = file.getInputStream();
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getName() + ".bpmn", fileInputStream)
                    .key(IdWorker.getIdStr())
                    .deploy();
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());
            if (model != null) {
                Collection<FlowElement> flowElementCollection = model.getMainProcess().getFlowElements();
                for (FlowElement e : flowElementCollection) {
                    if (e.getClass().toString().equals("class:class org.activiti.bpmn.model.UserTask")) {
                        //todo 可以在这里用一个中间表去维护每个任务的处理人
                        e.setId(IdWorker.getIdStr());
                        flowElements.add(e);
                    }
                    LOGGER.info("flowelement id:" + e.getId() + "  name:" + e.getName() + "   class:" + e.getClass().toString());
                }
            }
            activitiRepository.updateActReProcdef(processDefinition.getId());
        } catch (Exception e) {
            LOGGER.error("导入流程定义失败:{}", e.getMessage(), e);
        }
        return flowElements;
    }
}
