package org.dinghuang.activiti.conf;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


/**
 * @author dinghuang123@gmail.com
 * @since 2020/1/16
 */
@Configuration
public class SwaggerConfigure {

    @Value("${spring.application.name:service}")
    String serviceName;

    @Bean
    public Docket moduleApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("activiti")
                .genericModelSubstitutes(DeferredResult.class)
                .useDefaultResponseMessages(false)
                .forCodeGeneration(false)
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.dinghuang.activiti"))
                .apis(input -> {
                    //只有添加了ApiOperation注解的method才在API中显示
                    return input != null && input.isAnnotatedWith(ApiOperation.class);
                })
                //过滤的接口
                .paths(PathSelectors.any())
                .build()
                .apiInfo(moduleApiInfo());
    }


    private ApiInfo moduleApiInfo() {
        return new ApiInfoBuilder()
                .title(serviceName + " RESTful APIs")
                .description(serviceName + "系统接口文档说明")
                .version("1.0")
                .termsOfServiceUrl("NO terms of service")
                .contact(new Contact("dinghuang", "https://strongsickcat.com", "dinghuang123@gmail.com"))
                .license("The Apache License, Version 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
                .build();
    }
}
