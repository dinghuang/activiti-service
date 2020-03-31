package org.dinghuang.activiti.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.dinghuang.activiti.dto.TaskDTO;
import org.dinghuang.activiti.util.ActivitiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/3
 */
@Controller
@RequestMapping(value = "/v1/activiti")
@Api(value = "工作流", tags = {"工作流"})
public class ActivitiController {

    @Autowired
    private ActivitiUtils activitiUtils;

    @PostMapping(value = "/start")
    @ApiOperation(value = "启动实例流程")
    public ResponseEntity<String> start(@ApiParam(value = "xml中定义的流程id,这里是dinghuangTest", required = true)
                                        @RequestParam String processDefinitionKey) {
        org.activiti.api.process.model.ProcessInstance processInstance = activitiUtils.startProcessInstance(processDefinitionKey, processDefinitionKey, null);
        return new ResponseEntity<>(processInstance.getId(), HttpStatus.CREATED);
    }

    @GetMapping(value = "/task_list")
    @ApiOperation(value = "根据用户名称查询任务列表")
    public ResponseEntity<List<TaskDTO>> taskList(@ApiParam(value = "用户名称", required = true)
                                                  @RequestParam String userName) {
        List<TaskDTO> taskDTOS = new LinkedList<>();
        activitiUtils.queryTaskList(userName).forEach(task -> {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setAssignee(task.getAssignee());
            taskDTO.setId(task.getId());
            taskDTO.setName(task.getName());
            taskDTO.setDescription(task.getDescription());
            taskDTOS.add(taskDTO);
        });
        return new ResponseEntity<>(taskDTOS, HttpStatus.OK);
    }

    @PostMapping(value = "/approve")
    @ApiOperation(value = "完成任务")
    public ResponseEntity<Boolean> managerApprove(@ApiParam(value = "taskId", required = true)
                                                  @RequestParam String taskId,
                                                  @ApiParam(value = "任务参数key")
                                                  @RequestParam(required = false) String key,
                                                  @ApiParam(value = "任务参数value")
                                                  @RequestParam(required = false) String value) {
        Map<String, Object> variables = new HashMap<>(1);
        if (StringUtils.isNoneEmpty(key) && StringUtils.isNoneBlank(key)) {
            variables.put(key, value);
        }
        activitiUtils.completeTask(taskId, variables);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PostMapping(value = "/back")
    @ApiOperation(value = "撤回")
    public ResponseEntity<Boolean> back(@ApiParam(value = "currentTaskId", required = true)
                                        @RequestParam String currentTaskId,
                                        @ApiParam(value = "目标任务，如果为空，默认返回上级，如果找到上级有2个，那目标任务必须得传")
                                        @RequestParam(required = false) String targetTaskId) {
        activitiUtils.backTask(currentTaskId, targetTaskId);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @GetMapping(value = "/show_img")
    @ApiOperation(value = "查看当前流程图")
    public void showImg(@ApiParam(value = "实例id", required = true)
                        @RequestParam(required = false) String instanceKey, HttpServletResponse response) {
        activitiUtils.showImg(instanceKey, response);
    }

    @ApiOperation(value = "跳转到测试主页面")
    @GetMapping(value = "/index")
    public ModelAndView index(HttpServletRequest httpServletRequest) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/index.html");
        return mv;
    }

    @PostMapping("import")
    @ApiOperation(value = "导入流程定义")
    public ResponseEntity<Boolean> importXml(@ApiParam(value = "bpmn2.0格式的文件", required = true)
                                             @RequestParam("file") MultipartFile file,
                                             @ApiParam(value = "type(这里以后就是发起流程的processDefinitionKey)", required = true)
                                             @RequestParam String type,
                                             @ApiParam(value = "typeName", required = true)
                                             @RequestParam String typeName) {
        activitiUtils.importBpmnFile(file, type, typeName);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

}
