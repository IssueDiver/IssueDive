package com.issueDive.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * XSS(Cross-Site Scripting) 공격을 방어하기 위한 전역 설정 클래스입니다.
 * Jackson ObjectMapper에 커스텀 Deserializer를 모듈로 등록하여
 * 모든 Controller로 들어오는 JSON 요청의 문자열 필드를 자동으로 소독(Sanitize)합니다.
 */
@Configuration
public class XssSanitizerConfig {

    /**
     * Jackson에 등록할 커스텀 모듈을 Bean으로 생성합니다.
     * Spring Boot는 이 Bean을 자동으로 감지하여 기본 ObjectMapper에 등록해줍니다.
     * @return XSS 방어 로직이 포함된 SimpleModule
     */
    @Bean
    public SimpleModule xssSanitizerModule() {
        SimpleModule module = new SimpleModule();
        // String.class를 역직렬화 할 때, 우리가 만든 XssStringJsonDeserializer를 사용하도록 설정합니다.
        module.addDeserializer(String.class, new XssStringJsonDeserializer());
        return module;
    }

    /**
     * 문자열 값을 역직렬화(Deserializing)할 때 XSS 필터링을 적용하는 커스텀 Deserializer입니다.
     */
    public static class XssStringJsonDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

        // OWASP Sanitizer의 정책을 설정합니다.
        // BLOCKS: 기본적인 블록 요소(p, div, h1-h6 등) 허용
        // FORMATTING: 기본적인 텍스트 포맷(b, i, em, strong 등) 허용
        // LINKS: a 태그와 href 속성 허용 (자동으로 nofollow 처리)
        private static final PolicyFactory POLICY = Sanitizers.BLOCKS
                .and(Sanitizers.FORMATTING)
                .and(Sanitizers.STYLES)
                .and(Sanitizers.LINKS);

        // 이 Deserializer가 XSS 필터링을 수행할지 여부를 결정하는 플래그
        private boolean enableXssFilter = true;

        // 기본 생성자
        public XssStringJsonDeserializer() {
            this.enableXssFilter = true;
        }

        // 필터링 비활성화를 위한 생성자
        public XssStringJsonDeserializer(boolean enableXssFilter) {
            this.enableXssFilter = enableXssFilter;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null) {
                return null;
            }

            // enableXssFilter 플래그가 true일 때만 Sanitization 수행
            if (this.enableXssFilter) {
                return POLICY.sanitize(value);
            }

            // false일 경우 원본 값 그대로 반환
            return value;
        }

        /**
         * 이 메서드가 Deserializer의 핵심입니다.
         * Jackson이 필드를 처리하기 전에 호출되며, 필드의 컨텍스트(어노테이션 등)를 보고
         * 어떤 Deserializer 인스턴스를 사용할지 결정합니다.
         */
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            if (property != null) {
                // 현재 처리 중인 필드에서 @NoXss 어노테이션을 찾습니다.
                NoXss noXss = property.getAnnotation(NoXss.class);

                // @NoXss 어노테이션이 존재하면, XSS 필터링을 비활성화한 새 Deserializer 인스턴스를 반환합니다.
                if (noXss != null) {
                    return new XssStringJsonDeserializer(false);
                }
            }
            // 어노테이션이 없으면, 기본적으로 XSS 필터링을 수행하는 현재 인스턴스를 그대로 사용합니다.
            return this;
        }
    }
}

