package com.issueDive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.issueDive.controller.IssueController;
import com.issueDive.dto.CreateIssueRequest;
import com.issueDive.dto.IssueResponse;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.security.CustomUserDetailsService;
import com.issueDive.service.IssueService;
import com.issueDive.service.UserService;
import com.issueDive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssueController.class) // 테스트 대상 컨트롤러 지정
@Import(XssSanitizerConfig.class) // 테스트 환경에 XSS 설정 파일을 명시적으로 포함
@WithMockUser // Spring Security 인증을 통과했다고 가정
class XssFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueService issueService;

    // IssueController가 의존하는 다른 빈들도 Mock으로 등록
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    private static final String API_PREFIX = "/api";

    @BeforeEach
    void setUp() {
        UserResponseDTO mockUser = new UserResponseDTO(1L, "testuser", "test@example.com");
        given(userService.findUserByEmail(any())).willReturn(mockUser);
        // 모든 테스트에서 Service가 어떤 값을 반환하든 상관없도록 기본 Mocking 설정
        given(issueService.createIssue(any(CreateIssueRequest.class), any(Long.class)))
                .willAnswer(invocation -> {
                    CreateIssueRequest req = invocation.getArgument(0);
                    return new IssueResponse(1L, req.title(), req.description(), "OPEN", 1L, List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
                });
    }

    @Test
    @DisplayName("가장 기본적인 <script> 태그가 포함된 경우")
    void whenInputContainsScriptTag_thenShouldBeRemoved() throws Exception {
        // given
        String maliciousInput = "<script>alert('xss')</script>Normal Text";
        String expected = "Normal Text";

        // when & then
        performTestAndAssert(maliciousInput, expected);
    }

    @Test
    @DisplayName("<img> 태그의 onerror 이벤트 핸들러가 포함된 경우")
    void whenInputContainsOnerrorAttribute_thenShouldBeRemoved() throws Exception {
        // given
        String maliciousInput = "<img src=x onerror=alert('xss')> Normal Text";
        String expected = " Normal Text"; // img 태그 전체가 제거될 수 있음

        // when & then
        performTestAndAssert(maliciousInput, expected);
    }

    @Test
    @DisplayName("<a> 태그의 href에 javascript: 프로토콜이 사용된 경우")
    void whenInputContainsJavascriptHref_thenShouldBeSanitized() throws Exception {
        // given
        String maliciousInput = "<a href=\"javascript:alert('xss')\">Click me</a>";
        String expected = "Click me"; // 위험한 href 속성만 제거됨

        // when & then
        performTestAndAssert(maliciousInput, expected);
    }

    @Test
    @DisplayName("onclick과 같은 다른 이벤트 핸들러가 포함된 경우")
    void whenInputContainsOnclickAttribute_thenShouldBeRemoved() throws Exception {
        // given
        String maliciousInput = "<div onclick=\"alert('xss')\">Click me</div>";
        String expected = "<div>Click me</div>"; // onclick 속성만 제거됨

        // when & then
        performTestAndAssert(maliciousInput, expected);
    }

    @Test
    @DisplayName("허용된 HTML 태그(<b>, <i> 등)는 유지되어야 한다")
    void whenInputContainsAllowedHtml_thenShouldBeKept() throws Exception {
        // given
        String allowedHtml = "This is <b>Bold</b> and <i>Italic</i> text.";
        String expected = "This is <b>Bold</b> and <i>Italic</i> text.";

        // when & then
        performTestAndAssert(allowedHtml, expected);
    }

    /**
     * 테스트 로직을 재사용하기 위한 헬퍼 메서드
     * @param maliciousInput 악성 스크립트가 포함된 원본 문자열
     * @param expectedSanitizedResult 소독 후 기대되는 결과 문자열
     */
    private void performTestAndAssert(String maliciousInput, String expectedSanitizedResult) throws Exception {
        // given
        CreateIssueRequest requestDto = new CreateIssueRequest(maliciousInput, "description", null, null);

        // when
        mockMvc.perform(post(API_PREFIX + "/issues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        // then
        // ArgumentCaptor를 사용하여 issueService.createIssue 메서드에 전달된 실제 CreateIssueRequest 객체를 '캡처'
        ArgumentCaptor<CreateIssueRequest> captor = ArgumentCaptor.forClass(CreateIssueRequest.class);
        verify(issueService).createIssue(captor.capture(), any(Long.class));

        // 캡처한 객체의 title 필드 값이 예상대로 sanitize(제거/유지)되었는지 검증
        assertThat(captor.getValue().title()).isEqualTo(expectedSanitizedResult);
    }

}
