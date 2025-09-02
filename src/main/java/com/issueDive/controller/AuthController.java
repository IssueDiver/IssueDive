package com.issueDive.controller;

import com.issueDive.dto.*;
import com.issueDive.service.UserService;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.issueDive.util.JwtUtil;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    // private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Create User (회원가입)
     * @param request 사용자 요청 DTO (name, email, password 등)
     * @return 공통 응답 포맷 + 생성된 사용자 DTO
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDTO>> signUp(@Valid @RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.signUp(request);
        ApiResponse<UserResponseDTO> response = ApiResponse.ok(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login
     * @param request 로그인 요청 DTO (email, password)
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            // 인증된 사용자 정보 조회
            UserResponseDTO userResponse = userService.findUserByEmail(request.getEmail());

            // JWT AccessToken만 생성 (RefreshToken 제거)
            String accessToken = jwtUtil.generateAccessToken(userResponse.getId(), userResponse.getEmail());

            // JWT 응답 생성 (RefreshToken 제거)
            JwtResponse jwtResponse = JwtResponse.of(
                    accessToken,
                    "Bearer",
                    14400L, // 9월1일 변경 - 4시간 (초 단위)
                    userResponse
            );
            return ResponseEntity.ok(ApiResponse.ok(jwtResponse));
        } catch (Exception e) {
            //인증 실패시 예외 던지기 (GlobalExceptionHandler에서 처리)
            throw new com.issueDive.exception.AuthenticationFailedException();
        }
    }


    /**
     * 9월1일 변경 - 로그아웃 (JWT 기반에서는 클라이언트에서 토큰 삭제)
     * @return 로그아웃 안내 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
        //JWT는 stateless하므로 서버에서 특별한 로그아웃 처리 불필요

        Map<String, String> responseData = Map.of(
                "message", "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요.",
                "instruction", "localStorage에서 accessToken을 제거하세요."
        );

        ApiResponse<Map<String, String>> response = ApiResponse.ok(responseData);
        return ResponseEntity.ok(response);
    }

    /**
     * Read: 사용자 조회 (by ID)
     * @param id 조회할 사용자 id
     * @return 공통 응답 포맷 + 사용자 DTO
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long id){
        UserResponseDTO user = userService.findUserById(id);
        ApiResponse<UserResponseDTO> response = ApiResponse.ok(user);
        return new  ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Delete User
     * @param id 삭제할 사용자 id
     * @return 공통 응답 포맷 + 성공 메시지
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        ApiResponse<Void> response = ApiResponse.ok(null);
        return  new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Read: 전체 사용자 목록 조회
     * @return 공통 응답 포맷 + 사용자 DTO 리스트
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
