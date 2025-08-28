package com.issueDive.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class SwaggerConfig {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(UserDetails.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("IssueDive API")
                .description("깃허브 이슈 트래커 클론 코딩 프로젝트 'IssueDive'의 API 명세서입니다.")
                .version("1.0.0");
    }
}