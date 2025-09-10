package com.issueDive.controller;

import com.issueDive.entity.RefreshToken;
import com.issueDive.repository.RefreshTokenRepository;
import com.issueDive.dto.RefreshTokenRequest;
import com.issueDive.service.TokenBlacklistService;
import com.issueDive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.issueDive.dto.*;
import com.issueDive.service.UserService;
import jakarta.validation.Valid;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.issueDive.util.JwtUtil;

@Tag(name = "Auth & User", description = "인증 및 사용자 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class
AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired(required = false)
    private final TokenBlacklistService tokenBlacklistService;

    @Autowired(required = false)  // 9월 10일 최종
    private RefreshTokenRepository refreshTokenRepository;


    @Operation(summary = "회원가입 및 자동 로그인", description = "새로운 사용자를 등록하고, 성공 시 즉시 로그인 처리하여 JWT를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 및 자동 로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복된 이메일", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiCommonResponse<JwtResponse>> signUp(@RequestBody(description = "회원가입 정보", required = true, content = @Content(schema = @Schema(implementation = UserRequestDTO.class))) @Valid @org.springframework.web.bind.annotation.RequestBody UserRequestDTO request){
        // 1. 사용자 생성
        UserResponseDTO user = userService.signUp(request);

        // 2. 생성된 사용자 정보로 즉시 JWT 생성
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());

        // 3. 로그인 API와 동일한 JwtResponse 형식으로 응답 구성
        JwtResponse jwtResponse = JwtResponse.of(
                accessToken,
                "Bearer",
                14400L, // 4시간 (초 단위) : 설정 파일에서 관리하는 것이 더 좋지만 우선은 기존 코드에 맞게 여기서 작성함
                user
        );

        ApiCommonResponse<JwtResponse> response = ApiCommonResponse.ok(jwtResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "회원가입 (자동 로그인 없음)", description = "새로운 사용자를 등록만 합니다. (테스트용)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값 (중복된 이메일 등)", content = @Content)
    })
    @PostMapping("/signup-only")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> signUpOnly(@RequestBody(description = "회원가입 정보", required = true, content = @Content(schema = @Schema(implementation = UserRequestDTO.class))) @Valid @org.springframework.web.bind.annotation.RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.signUp(request);
        ApiCommonResponse<UserResponseDTO> response = ApiCommonResponse.ok(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 사용하여 로그인하고 JWT를 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 이메일 또는 비밀번호)", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ApiCommonResponse<JwtResponse>> login(@RequestBody(description = "로그인 정보", required = true, content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))) @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequestDTO request) {
        try {
            // 인증된 사용자 정보 조회
            UserResponseDTO userResponse = userService.findUserByEmail(request.getEmail());

            // JWT AccessToken만 생성 (RefreshToken 제거)
            String accessToken = jwtUtil.generateAccessToken(userResponse.getId(), userResponse.getEmail());

            //Refresh Token 생성 (Repository가 있을 때만)
            String refreshToken = null;
            if (refreshTokenRepository != null) {
                try {
                    // 기존 토큰 삭제
                    refreshTokenRepository.deleteByUserEmail(userResponse.getEmail());

                    // 새 Refresh Token 생성
                    refreshToken = jwtUtil.generateRefreshToken(userResponse.getId(), userResponse.getEmail());

                    // DB에 저장
                    RefreshToken token = RefreshToken.builder()
                            .token(refreshToken)
                            .userId(userResponse.getId())
                            .userEmail(userResponse.getEmail())
                            .expiresAt(LocalDateTime.now().plusDays(7))
                            .build();
                    refreshTokenRepository.save(token);
                } catch (Exception e) {
                    log.debug("9월 10일 최종 - Refresh Token 생성 실패 (무시): {}", e.getMessage());
                }
            }

            // 응답 생성
            JwtResponse jwtResponse = JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)  // 9월 10일 최종
                    .tokenType("Bearer")
                    .expiresIn(14400L)
                    .user(userResponse)
                    .build();
            return ResponseEntity.ok(ApiCommonResponse.ok(jwtResponse));
        } catch (Exception e) {
            //인증 실패시 예외 던지기 (GlobalExceptionHandler에서 처리)
            throw new com.issueDive.exception.AuthenticationFailedException();
        }
    }


    @Operation(summary = "로그아웃", description = "서버 측에서는 별도의 처리가 없으며, 클라이언트에서 토큰을 삭제해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiCommonResponse<Map<String, String>>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 9월 10일 최종 - 블랙리스트 처리
        if (tokenBlacklistService != null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);

                // Access Token 블랙리스트 추가
                Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
                tokenBlacklistService.addToBlacklist(token, expirationDate);

                // Refresh Token 삭제
                if (refreshTokenRepository != null) {
                    try {
                        String email = jwtUtil.getUserEmailFromToken(token);
                        refreshTokenRepository.deleteByUserEmail(email);
                    } catch (Exception e) {
                        log.debug("9월 10일 최종 - Refresh Token 삭제 실패 (무시)");
                    }
                }
            } catch (Exception e) {
                log.error("9월 10일 최종 - 로그아웃 처리 중 오류 (무시): {}", e.getMessage());
            }
        }
        Map<String, String> responseData = Map.of(
                "message", "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요.",
                "instruction", "localStorage에서 accessToken을 제거하세요."
        );

        ApiCommonResponse<Map<String, String>> response = ApiCommonResponse.ok(responseData);
        return ResponseEntity.ok(response);
    }

    // 4. 9월 10일 최종 - refresh 엔드포인트 추가 (최소 기능)
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiCommonResponse<JwtResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        if (refreshTokenRepository == null) {
            throw new com.issueDive.exception.AuthenticationFailedException("토큰 갱신 기능이 비활성화되어 있습니다.");
        }

        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new com.issueDive.exception.AuthenticationFailedException("Refresh Token이 필요합니다.");
            }

            // DB에서 토큰 조회
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new com.issueDive.exception.AuthenticationFailedException("유효하지 않은 Refresh Token"));

            // 만료 확인
            if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                // 만료된 토큰은 삭제
                refreshTokenRepository.delete(storedToken);
                throw new com.issueDive.exception.AuthenticationFailedException("만료된 Refresh Token");
            }

            // 사용자 정보 조회
            UserResponseDTO user = userService.findUserById(storedToken.getUserId());

            // 새 Access Token 생성
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());

            // 응답 생성
            JwtResponse jwtResponse = JwtResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)  // 기존 Refresh Token 유지
                    .tokenType("Bearer")
                    .expiresIn(14400L)
                    .user(user)
                    .build();

            return ResponseEntity.ok(ApiCommonResponse.ok(jwtResponse));

        } catch (com.issueDive.exception.AuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("9월 10일 최종 - 토큰 갱신 중 오류: ", e);
            throw new com.issueDive.exception.AuthenticationFailedException("토큰 갱신 실패");
        }
    }

    @Operation(summary = "사용자 정보 조회", description = "ID로 특정 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> getUserById(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id){
        UserResponseDTO user = userService.findUserById(id);
        ApiCommonResponse<UserResponseDTO> response = ApiCommonResponse.ok(user);
        return new  ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "회원 탈퇴", description = "ID로 특정 사용자를 탈퇴 처리합니다.")
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

    @Operation(summary = "전체 사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    })
    @GetMapping("/users")
    public ResponseEntity<ApiCommonResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiCommonResponse.ok(users));
    }
}
