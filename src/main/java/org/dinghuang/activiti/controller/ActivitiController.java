package org.dinghuang.activiti.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.history.HistoricProcessInstance;
import org.dinghuang.activiti.dto.TaskDTO;
import org.dinghuang.activiti.util.ActivitiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/3
 */
@Controller
@RequestMapping(value = "/v1/activiti")
@Api(value = "工作流", tags = {"工作流"})
public class ActivitiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiController.class);

    /**
     * 流程定义和部署相关的存储服务
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * 节点任务相关操作接口
     */
    @Autowired
    private TaskService taskService;

    /**
     * 历史记录相关服务接口
     */
    @Autowired
    private HistoryService historyService;

    @PostMapping(value = "/start")
    @ApiOperation(value = "启动实例流程")
    public ResponseEntity<String> start(@ApiParam(value = "xml中定义的流程id,这里是dinghuangTest", required = true)
                                        @RequestParam String processId) {
        org.activiti.api.process.model.ProcessInstance processInstance = ActivitiUtils.startProcessInstance(processId, processId, null);
        return new ResponseEntity<>(processInstance.getId(), HttpStatus.CREATED);
    }

    @GetMapping(value = "/task_list")
    @ApiOperation(value = "根据用户名称查询任务列表")
    public ResponseEntity<List<TaskDTO>> taskList(@ApiParam(value = "用户名称", required = true)
                                                  @RequestParam String userName) {
        List<TaskDTO> taskDTOS = new LinkedList<>();
        ActivitiUtils.queryTaskList(userName).forEach(task -> {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setAssignee(task.getAssignee());
            taskDTO.setId(task.getId());
            taskDTO.setName(task.getName());
            taskDTO.setDescription(task.getDescription());
            taskDTOS.add(taskDTO);
        });
        return new ResponseEntity<>(taskDTOS, HttpStatus.OK);
    }

    @PostMapping(value = "/manager_approve")
    @ApiOperation(value = "部门经理审批")
    public ResponseEntity<Boolean> managerApprove(@ApiParam(value = "taskId", required = true)
                                                  @RequestParam String taskId,
                                                  @ApiParam(value = "是否重要", required = true)
                                                  @RequestParam Boolean important) {
        Map<String, Object> variables = new HashMap<>(1);
        variables.put("important", important);
        ActivitiUtils.completeTask(taskId, variables);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PostMapping(value = "/admin_approve")
    @ApiOperation(value = "总经理审批")
    public ResponseEntity<Boolean> managerApprove(@ApiParam(value = "taskId", required = true)
                                                  @RequestParam String taskId) {
        ActivitiUtils.completeTask(taskId, null);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @GetMapping(value = "/show_img")
    @ApiOperation(value = "查看当前流程图")
    public void showImg(@ApiParam(value = "xml中定义的实例id", required = true)
                        @RequestParam(required = false) String instanceKey, HttpServletResponse response) {
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
            ActivitiUtils.outputImg(response, bpmnModel, null, null);
            return;
        }
        // 已执行的节点ID集合(将historicActivityInstanceList中元素的activityId字段取出封装到executedActivityIdList)
        List<String> executedActivityIdList = historicActivityInstanceList.stream().map(HistoricActivityInstance::getActivityId).collect(Collectors.toList());

        //获取流程走过的线
        List<String> flowIds = ActivitiUtils.getHighLightedFlows(bpmnModel, historicActivityInstanceList);

        //输出图像，并设置高亮
        ActivitiUtils.outputImg(response, bpmnModel, flowIds, executedActivityIdList);
    }


    @ApiOperation(value = "跳转到测试主页面")
    @GetMapping(value = "/index")
    public ModelAndView index(HttpServletRequest req) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/index.html");
        return mv;
    }

}
