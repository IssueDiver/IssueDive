package com.issueDive.service;

import com.issueDive.dto.UserRequestDTO;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.entity.User;
import com.issueDive.exception.AuthenticationFailedException;
import com.issueDive.exception.DuplicateEmailException;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("회원가입 성공: 이메일 중복 없음 → 비번 암호화 → 저장 후 DTO 반환")
    void signUp_success() {
        UserRequestDTO req = new UserRequestDTO();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pw");

        given(userRepository.existsByEmail("alice@test.com")).willReturn(false);
        given(passwordEncoder.encode("pw")).willReturn("ENC");
        User saved = User.builder()
                .id(1L).username("alice").email("alice@test.com").password("ENC")
                .build();
        given(userRepository.save(any(User.class))).willReturn(saved);

        UserResponseDTO res = userService.signUp(req);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getEmail()).isEqualTo("alice@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패: 이메일 중복이면 DuplicateEmailException")
    void signUp_duplicateEmail() {
        UserRequestDTO req = new UserRequestDTO();
        req.setUsername("bob");
        req.setEmail("dup@test.com");
        req.setPassword("pw");
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.signUp(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공: 이메일로 조회 + 비밀번호 일치")
    void login_success() {
        User user = User.builder()
                .id(1L).username("alice").email("alice@test.com").password("ENC")
                .build();
        given(userRepository.findByEmail("alice@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pw", "ENC")).willReturn(true);

        UserResponseDTO res = userService.login("alice@test.com", "pw");

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("로그인 실패: 이메일 존재하지 않음 → AuthenticationFailedException")
    void login_userNotFound() {
        given(userRepository.findByEmail("nope@test.com")).willReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> userService.login("nope@test.com", "pw"));
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치 → AuthenticationFailedException")
    void login_wrongPassword() {
        User user = User.builder()
                .id(1L).username("alice").email("alice@test.com").password("ENC")
                .build();
        given(userRepository.findByEmail("alice@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "ENC")).willReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> userService.login("alice@test.com", "wrong"));
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findUserById_success() {
        User user = User.builder().id(10L).username("u").email("u@test.com").password("p").build();
        given(userRepository.findById(10L)).willReturn(Optional.of(user));

        UserResponseDTO res = userService.findUserById(10L);

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getEmail()).isEqualTo("u@test.com");
    }

    @Test
    @DisplayName("ID로 사용자 조회 실패: 없음 → UserNotFoundException")
    void findUserById_notFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findUserById(99L));
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findUserByEmail_success() {
        User user = User.builder().id(2L).username("mail").email("m@test.com").password("p").build();
        given(userRepository.findByEmail("m@test.com")).willReturn(Optional.of(user));

        UserResponseDTO res = userService.findUserByEmail("m@test.com");

        assertThat(res.getUsername()).isEqualTo("mail");
    }

    @Test
    @DisplayName("이메일로 사용자 조회 실패: 없음 → UserNotFoundException")
    void findUserByEmail_notFound() {
        given(userRepository.findByEmail("no@test.com")).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findUserByEmail("no@test.com"));
    }

    @Test
    @DisplayName("삭제 성공: 존재하면 삭제 호출")
    void deleteUser_success() {
        given(userRepository.existsById(1L)).willReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("삭제 실패: 존재하지 않으면 UserNotFoundException")
    void deleteUser_notFound() {
        given(userRepository.existsById(1L)).willReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getAllUsers_success() {
        List<User> users = List.of(
                User.builder().id(1L).username("a").email("a@test.com").password("p").build(),
                User.builder().id(2L).username("b").email("b@test.com").password("p").build()
        );
        given(userRepository.findAll()).willReturn(users);

        List<UserResponseDTO> res = userService.getAllUsers();

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getEmail()).isEqualTo("a@test.com");
        assertThat(res.get(1).getUsername()).isEqualTo("b");
    }
}
