package org.dinghuang.activiti.dto;

import lombok.Data;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/4
 */
@Data
public class TaskDTO {

    private String id;

    private String name;

    private String description;

    private String assignee;
}
