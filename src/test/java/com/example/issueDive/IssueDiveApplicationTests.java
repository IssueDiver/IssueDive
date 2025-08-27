package com.example.issueDive;

import com.example.issueDive.entity.Issue;
import com.example.issueDive.entity.IssueStatus;
import com.example.issueDive.entity.User;
import com.example.issueDive.repository.IssueRepository;
import com.example.issueDive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 (실제 스프링 컨텍스트 + H2 DB/test container)
 */
//@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test") // test 프로파일 적용
@AutoConfigureMockMvc // 통합 테스트 MockMvc (addFilters = false)
class IssueDiveApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IssueRepository issueRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		// 각 테스트 실행 전에 데이터베이스를 비움
		issueRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	/**
	 * 다중 조회 (필터링, 페이징) 통합 테스트
	 */
	@Test
	@WithMockUser
	void getFilteredIssues_integrationTest() throws Exception {
		// given: DB에 테스트용 데이터를 저장
		User author = userRepository.save(User.builder().username("author").email("author@example.com").password("password").build());
		User assignee = userRepository.save(User.builder().username("assignee").email("assignee@example.com").password("password").build());
		issueRepository.save(Issue.builder()
				.title("작성자가 지정된 오픈 이슈")
				.status(IssueStatus.OPEN)
				.author(author)
				.assignee(assignee)
				.build());
		issueRepository.save(Issue.builder()
				.title("닫힌 이슈")
				.status(IssueStatus.CLOSED)
				.author(author)
				.build());

		// when: API를 호출해 필터링된 결과 요청
		mockMvc.perform(get("/issues")
						.param("status", "OPEN") // OPEN 상태인 이슈만 필터링
						.param("authorId", author.getId().toString())
						.param("assigneeId", assignee.getId().toString())
						.param("page", "0"))
				.andDo(print()) // 응답 내용을 콘솔에 출력
				// then: 실제 DB에서 조회된 결과 검증
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.totalElements").value(1)) // 1개의 결과만 나와야 함
				.andExpect(jsonPath("$.data.content[0].title").value("작성자가 지정된 오픈 이슈"));
	}

}
