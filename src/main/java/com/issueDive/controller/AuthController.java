package com.issueDive.controller;

import com.issueDive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.issueDive.dto.*;
import com.issueDive.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.issueDive.util.JwtUtil;

@Tag(name = "Auth & User", description = "인증 및 사용자 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Operation(summary = "회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값 (중복된 이메일 등)", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> signUp(@Valid @RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.signUp(request);
        ApiCommonResponse<UserResponseDTO> response = ApiCommonResponse.ok(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 이메일 또는 비밀번호)", content = @Content)
    })
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

    @Operation(summary = "사용자 정보 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> getUserById(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id){
        UserResponseDTO user = userService.findUserById(id);
        ApiCommonResponse<UserResponseDTO> response = ApiCommonResponse.ok(user);
        return new  ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "회원 탈퇴")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @DeleteMapping("/user/{id}")
    public ResponseEntity<ApiCommonResponse<Void>> deleteUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id){
        userService.deleteUser(id);
        ApiCommonResponse<Void> response = ApiCommonResponse.ok(null);
        return  new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "전체 사용자 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users")
    public ResponseEntity<ApiCommonResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiCommonResponse.ok(users));
    }
}
