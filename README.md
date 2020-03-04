# Activiti7.X结合SpringBoot2.1、Mysql

## Activiti简介

### Activiti介绍

![image](https://firebasestorage.googleapis.com/v0/b/gitbook-28427.appspot.com/o/assets%2F-LHE-A0W4uh4yR1u7ql8%2F-LHE-EAcoOsHLfdz-Di9%2F-LHE-FC7enBS3Fh9WnbR%2FAcitiviti_Icon_FullColor_GitHub_400x400.png?generation=1531407568078053&alt=media)

Activiti 是由 jBPM 的创建者 Tom Baeyens 离开 JBoss 之后建立的项目，构建在开发 jBPM 版本 1 到 4 时积累的多年经验的基础之上，旨在创建下一代的 BPM 解决方案。

Activiti是一个开源的工作流引擎，它实现了BPMN 2.0规范，可以发布设计好的流程定义，并通过api进行流程调度。

Activiti 作为一个遵从 Apache 许可的工作流和业务流程管理开源平台，其核心是基于Java的超快速、超稳定的 BPMN2.0 流程引擎，强调流程服务的可嵌入性和可扩展性，同时更加强调面向业务人员。

Activiti 流程引擎重点关注在系统开发的易用性和轻量性上。每一项 BPM 业务功能 Activiti 流程引擎都以服务的形式提供给开发人员。通过使用这些服务，开发人员能够构建出功能丰富、轻便且高效的 BPM 应用程序。

Activiti是一个针对企业用户、开发人员、系统管理员的轻量级工作流业务管理平台，其核心是使用Java开发的快速、稳定的BPMN e 2.0流程引擎。Activiti是在ApacheV2许可下发布的，可以运行在任何类型的Java程序中，例如服务器、集群、云服务等。Activiti可以完美地与Spring集成。同时，基于简约思想的设计使Activiti非常轻量级。

目前Activiti有2个版本，一个本地的core，一个可以支持分布式的cloud，本文只介绍core，新版 Activiti 7.0.0 发布后，Activiti Cloud 现在是新一代商业自动化平台，提供一组旨在在分布式基础架构上运行的 Cloud原生构建块。Cloud可以参考[官网](https://activiti.gitbook.io/activiti-7-developers-guide/getting-started)

Activiti 7.x 主要突出了 Spring Boot 2.x 应用程序中的 ProcessRuntime 和 TaskRuntime API 的使用。

### BPMN

BPMN（Business Process Model And Notation）-业务流程模型和符号是由BPMI（Business Process Management Initiative）开发的一套标准的业务流程建模符号，使用BPMN提供的符号可以创建业务流程。

Activiti 就是使用BPMN 2.0 进行流程建模、流程执行管理，它包括很多的建模符号，比如：Event 用一个圆圈表示，它是流程中运行过程中发生的事情。

一个bpmn图形的例子：

首先当事人发起一个请假单
其次他所在部门的经理对请假单进行审核
然后人事经理进行复核并进行备案
最后请假流程结束


## 创建应用

代码库地址：https://github.com/dinghuang/activiti-service.git

创建maven应用，pom引入包
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>dinghuang-framework-parent</artifactId>
        <groupId>org.dinghuang</groupId>
        <version>0.1.0-RELEASE</version>
    </parent>

    <artifactId>activiti</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>activiti</name>
    <description>activiti project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <mapstruct>1.3.0.Final</mapstruct>
        <activiti.version>7.1.0.M6</activiti.version>
        <spring-boot>2.1.0.RELEASE</spring-boot>
        <fastjson>1.2.47</fastjson>
        <commons-collections>3.2.2</commons-collections>
        <commons-lang3>3.8.1</commons-lang3>
        <swagger-annotations>1.5.16</swagger-annotations>
        <springfox-swagger2>2.7.0</springfox-swagger2>
        <druid>1.1.10</druid>
        <mybatis-plus-boot-starter>3.1.0</mybatis-plus-boot-starter>
        <feign-hystrix>9.5.0</feign-hystrix>
        <lombok>1.16.20</lombok>
        <mysql-connector-java>8.0.15</mysql-connector-java>
        <liquibase-core>3.5.3</liquibase-core>
        <validation-api>2.0.1.Final</validation-api>
        <hibernate-validator>6.0.15.Final</hibernate-validator>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.activiti.dependencies</groupId>
                <artifactId>activiti-dependencies</artifactId>
                <version>${activiti.version}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.1.12.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.activiti</groupId>
            <artifactId>activiti-spring-boot-starter</artifactId>
            <version>${activiti.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.2.10</version>
        </dependency>
        <!-- Activiti生成流程图 -->
        <dependency>
            <groupId>org.activiti</groupId>
            <artifactId>activiti-image-generator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
            <version>${spring-boot}</version>
        </dependency>
        <dependency>
            <groupId>org.dinghuang</groupId>
            <artifactId>dinghuang-framework-core</artifactId>
            <version>0.1.0-RELEASE</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.6.2</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <annotationProcessorPaths>
                        <!--这个配置是因为lombok跟mapstruct一起用的时候maven对注解的解析器选择有点问题-->
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.16.20</version>
                        </path>
                    </annotationProcessorPaths>
                    <compilerArgs>
                        <compilerArg>
                            -Amapstruct.defaultComponentModel=spring
                        </compilerArg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
        <finalName>demo</finalName>
    </build>

</project>

```

### 与mysql结合
在``resource``目录下准备文件``activiti.cfg.xml``，内容如下：
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="processEngineConfiguration" class="org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration">
        <property name="databaseType" value="mysql"></property>
        <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/activiti"></property>
        <property name="jdbcDriver" value="com.mysql.jdbc.Driver"></property>
        <property name="jdbcUsername" value="root"></property>
        <property name="jdbcPassword" value="root"></property>
    </bean>
</beans>
```
执行下面的代码，初始化db数据库或者配置springboot配置文件，会自动生成25个表，代码执行如下：
```
package org.dinghuang.activiti.conf;

import org.activiti.engine.impl.db.DbSchemaCreate;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/2/28
 */
public class Test {

    public static void main(String[] args) {
        DbSchemaCreate.main(args);
    }
}
```
配置文件如下：
```
spring:
 activiti:
  # 自动建表
  database-schema: ACTIVITI
  #表示启动时检查数据库表，不存在则创建
  database-schema-update: true
  #表示哪种情况下使用历史表，这里配置为full表示全部记录历史，方便绘制流程图
  history-level: full
  #表示使用历史表，如果不配置，则工程启动后可以检查数据库，只建立了17张表，历史表没有建立，则流程图及运行节点无法展示
  db-history-used: true
project:
  manifest:
    file:
      path: classpath:/default-project.json
logging:
  level:
    org.activiti: debug
```

#### 数据库说明

数据库会生成25张表，ER图如图所示：
![image](https://minios.strongsickcat.com/dinghuang-blog-picture/activiti.png)
表的列表：
![image](https://minios.strongsickcat.com/dinghuang-blog-picture/WechatIMG430.png)

```
ACT_RE_*: RE表示repository，这个前缀的表包含了流程定义和流程静态资源
ACT_RU_*: RU表示runtime，这些运行时的表，包含流程实例，任务，变量，异步任务等运行中的数据。Activiti只在流程实例执行过程中保存这些数据， 在流程结束时就会删除这些记录。
ACT_ID_*: ID表示identity，这些表包含身份信息，比如用户，组等。这些表现在已废弃。
ACT_HI_*: HI表示history，这些表包含历史数据，比如历史流程实例， 变量，任务等。
ACT_GE_*: 通用数据， 用于不同场景下。
ACT_EVT_*: EVT表示EVENT，目前只有一张表ACT_EVT_LOG，存储事件处理日志，方便管理员跟踪处理
```
具体表详解可以参考[文章](https://www.chenmin.info/2018/07/28/Activiti-23%E5%BC%A0%E8%A1%A8%E5%8F%8A7%E5%A4%A7%E6%9C%8D%E5%8A%A1%E8%AF%A6%E8%A7%A3/#7%E5%A4%A7%E6%9C%8D%E5%8A%A1%E4%BB%8B%E7%BB%8D)

### 表详解

表 | 意义 | 备注
---|---|---
ACT_EVT_LOG|	事件处理日志|	
ACT_GE_BYTEARRAY| 二进制数据表 | 存储流程定义相关的部署信息。即流程定义文档的存放地。每部署一次就会增加两条记录，一条是关于bpmn规则文件的，一条是图片的（如果部署时只指定了bpmn一个文件，activiti会在部署时解析bpmn文件内容自动生成流程图）。两个文件不是很大，都是以二进制形式存储在数据库中。
ACT_GE_PROPERTY|	主键生成表|主张表将生成下次流程部署的主键ID。	
ACT_HI_ACTINST|	历史节点表|	只记录usertask内容,某一次流程的执行一共经历了多少个活动
ACT_HI_ATTACHMENT|	历史附件表|	
ACT_HI_COMMENT|	历史意见表|	
ACT_HI_DETAIL|	历史详情表，提供历史变量的查询|	流程中产生的变量详细，包括控制流程流转的变量等
ACT_HI_IDENTITYLINK|	历史流程人员表|	
ACT_HI_PROCINST|	历史流程实例表	|
ACT_HI_TASKINST|	历史任务实例表	|一次流程的执行一共经历了多少个任务
ACT_HI_VARINST|	历史变量表	|
ACT_PROCDEF_INFO| |
ACT_RE_DEPLOYMENT|	部署信息表	| 存放流程定义的显示名和部署时间，每部署一次增加一条记录
ACT_RE_MODEL|	流程设计模型部署表|	流程设计器设计流程后，保存数据到该表
ACT_RE_PROCDEF|	流程定义数据表	| 存放流程定义的属性信息，部署每个新的流程定义都会在这张表中增加一条记录。注意：当流程定义的key相同的情况下，使用的是版本升级
ACT_RU_EVENT_SUBSCR|	throwEvent，catchEvent时间监听信息表|
ACT_RU_EXECUTION|	运行时流程执行实例表|	历史流程变量
ACT_RU_IDENTITYLINK|运行时流程人员表|主要存储任务节点与参与者的相关信息
ACT_RU_INTEGRATION| | 
ACT_RU_JOB	|运行时定时任务数据表	
ACT_RU_TIMER_JOB| |
ACT_RU_SUSPENDED_JOB| | 
ACT_RU_TASK	|运行时任务节点表	|
ACT_RU_TIMER_JOB| |
ACT_RU_VARIABLE	| 运行时流程变量数据表| 通过JavaBean设置的流程变量，在act_ru_variable中存储的类型为serializable，变量真正存储的地方在act_ge_bytearray中。
ACT_ID_GROUP|	用户组信息表| 已废弃	
ACT_ID_INFO|	用户扩展信息表	|已废弃
ACT_ID_MEMBERSHIP|	用户与用户组对应信息表|已废弃	
ACT_ID_USER	|用户信息表	|已废弃

## 工作流使用
activiti7内置了Spring security框架,官方demo跟spring结合的必须与spring-security结合，这里我不用spring-security，因为现在没有用户表了，所以自定义一些用户角色表去结合，更容易理解。

> 关于security问题
activiti7最新的类似Runtime API和Task API都集成了security。
如果使用上述的API,那么必须要使用security，不能屏蔽security，否则会报错。
使用引擎服务类的时候，可以排除security，因为这些是最原始的API。但是activiti7官方已经明确说了，随时可能会干掉这些API。不建议开发人员直接使用引擎类以及引擎配置了、服务类等。

### 相关配置类

#### 自定义id策略
```
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
```

#### 自定义用户组

activiti7已经抛弃了identity相关的表，同时与springSecurity结合，所以这边如果想自定义可以这么改，但是实际用途不大，最重要的是要结合自己的用户表设计来。
```
package org.dinghuang.activiti.conf;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 重写用户权限
 *
 * @author dinghuang123@gmail.com
 * @since 2020/3/2
 */
@Component
public class ActivitiUserGroupManagerConfiguration implements UserGroupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiUserGroupManagerConfiguration.class);

    public static List<String> roles = new ArrayList<>(3);
    public static List<String> groups = new ArrayList<>(1);
    public static List<String> users = new ArrayList<>(3);
    public static Map<String, String> userRoleMap = new HashMap<>(3);

    static {
        roles.add("workCreate");
        roles.add("workPermit");
        roles.add("workLeader");

        groups.add("workGroupA");

        users.add("admin");
        users.add("laowang");
        users.add("xiaofang");

        userRoleMap.put("admin", "workCreate");
        userRoleMap.put("laowang", "workPermit");
        userRoleMap.put("xiaofang", "workLeader");
    }

    @Override
    public List<String> getUserGroups(String s) {
        LOGGER.info("get user groups");
        return groups;
    }

    @Override
    public List<String> getUserRoles(String s) {
        String role = userRoleMap.get(s);
        List<String> list = new ArrayList<>();
        list.add(role);
        LOGGER.info("get user roles");
        return list;
    }

    @Override
    public List<String> getGroups() {
        LOGGER.info("get groups");
        return groups;
    }

    @Override
    public List<String> getUsers() {
        LOGGER.info("get users");
        return users;
    }
}
```

#### 其他自定义配置
```
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
```
因为去掉了springSecurity,所以启动类得排除springSecurity的自动配置。
```
package org.dinghuang.activiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class,
        org.activiti.core.common.spring.identity.config.ActivitiSpringIdentityAutoConfiguration.class
})
@EnableWebMvc
public class ActivitiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitiApplication.class, args);
    }

    @Bean
    public InternalResourceViewResolver setupViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        /** 设置视图路径的前缀 */
        resolver.setPrefix("resources/templates");
        /** 设置视图路径的后缀 */
        resolver.setSuffix(".html");
        return resolver;
    }
}
```

## 流程绘制
流程绘制这里提供2种，一种用IDEA下载ActiBPM插件去画，这里给一段我自己画的xml
```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:activiti="http://activiti.org/bpmn" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:tns="http://sourceforge.net/bpmn/definitions/a123123" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:yaoqiang="http://bpmn.sourceforge.net" expressionLanguage="http://www.w3.org/1999/XPath" id="m1583310491794" name="" targetNamespace="http://sourceforge.net/bpmn/definitions/a123123" typeLanguage="http://www.w3.org/2001/XMLSchema">
  <process id="dinghuangTest" isClosed="false" isExecutable="true" name="请假流程" processType="None">
    <startEvent id="_2" name="开始"/>
    <userTask activiti:assignee="bilu" activiti:candidateGroups="manager" activiti:exclusive="true" id="_3" name="部门经理审批"/>
    <endEvent id="_4" name="结束"/>
    <sequenceFlow id="_5" sourceRef="_2" targetRef="_3"/>
    <sequenceFlow id="_6" name="事情不重要" sourceRef="_3" targetRef="_4">
      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${!important}]]></conditionExpression>
    </sequenceFlow>
    <userTask activiti:candidateGroups="admin" activiti:exclusive="true" id="_7" name="总经理审批"/>
    <sequenceFlow id="_8" name="事情重要" sourceRef="_3" targetRef="_7">
      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${important}]]></conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="_9" name="审批通过" sourceRef="_7" targetRef="_4"/>
  </process>
  <bpmndi:BPMNDiagram documentation="background=#3C3F41;count=1;horizontalcount=1;orientation=0;width=842.4;height=1195.2;imageableWidth=832.4;imageableHeight=1185.2;imageableX=5.0;imageableY=5.0" id="Diagram-_1" name="New Diagram">
    <bpmndi:BPMNPlane bpmnElement="dinghuangTest">
      <bpmndi:BPMNShape bpmnElement="_2" id="Shape-_2">
        <dc:Bounds height="32.0" width="32.0" x="100.0" y="135.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="32.0" width="32.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="_3" id="Shape-_3">
        <dc:Bounds height="55.0" width="85.0" x="235.0" y="140.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="55.0" width="85.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="_4" id="Shape-_4">
        <dc:Bounds height="32.0" width="32.0" x="565.0" y="160.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="32.0" width="32.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="_7" id="Shape-_7">
        <dc:Bounds height="55.0" width="85.0" x="415.0" y="55.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="55.0" width="85.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge bpmnElement="_5" id="BPMNEdge__5" sourceElement="_2" targetElement="_3">
        <di:waypoint x="132.0" y="151.0"/>
        <di:waypoint x="235.0" y="167.5"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="0.0" width="0.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="_6" id="BPMNEdge__6" sourceElement="_3" targetElement="_4">
        <di:waypoint x="320.0" y="167.5"/>
        <di:waypoint x="565.0" y="176.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="0.0" width="0.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="_8" id="BPMNEdge__8" sourceElement="_3" targetElement="_7">
        <di:waypoint x="320.0" y="167.5"/>
        <di:waypoint x="415.0" y="82.5"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="0.0" width="0.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="_9" id="BPMNEdge__9" sourceElement="_7" targetElement="_4">
        <di:waypoint x="500.0" y="82.5"/>
        <di:waypoint x="565.0" y="176.0"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds height="0.0" width="0.0" x="0.0" y="0.0"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>
```
这是一个很简单的流程，如图所示:
![image](https://minios.strongsickcat.com/dinghuang-blog-picture/WechatIMG432.png)

### 前端实现自定义流程、输出流程图
官方提供bpmn.js可以实现前端拖动画图，github地址：https://github.com/bpmn-io/bpmn-js

可以把这个加到项目中，这里就不解释了。

前端实现输出流程图效果如下，流程节点会高亮显示：

![image](https://minios.strongsickcat.com/dinghuang-blog-picture/WechatIMG433.png)

这里是我写的一个小demo，代码可以查看我的github代码库。项目启动后，访问http://localhost:8080/v1/activiti/index

## 接口调试
服务启动后访问地址：http://localhost:8080/swagger-ui.html#/
接口已经写在代码中，如果需要调试的话，顺序是
- 启动实例流程
- 根据用户名称查询任务列表（用户名bilu，拿到任务id后去完成任务）
- 部门经理审批
- 总经理审批
![image](https://minios.strongsickcat.com/dinghuang-blog-picture/WechatIMG434.png)


