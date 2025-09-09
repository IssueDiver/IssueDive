package com.issueDive.controller;

import com.issueDive.service.TokenBlackListService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    // private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenBlackListService tokenBlackListService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값 (중복된 이메일 등)", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> signUp(@RequestBody(description = "회원가입 정보", required = true, content = @Content(schema = @Schema(implementation = UserRequestDTO.class))) @Valid @org.springframework.web.bind.annotation.RequestBody UserRequestDTO request){
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

            // JWT 응답 생성 (RefreshToken 제거)
            JwtResponse jwtResponse = JwtResponse.of(
                    accessToken,
                    "Bearer",
                    14400L, // 9월1일 변경 - 4시간 (초 단위)
                    userResponse
            );
            return ResponseEntity.ok(ApiCommonResponse.ok(jwtResponse));
        } catch (Exception e) {
            //인증 실패시 예외 던지기 (GlobalExceptionHandler에서 처리)
            throw new com.issueDive.exception.AuthenticationFailedException();
        }
    }


    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 추가하고 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiCommonResponse<Map<String, String>>> logout(@Parameter(description = "Authorization 헤더의 Bearer Token", required = true)
                                                                             @RequestHeader("Authorization") String bearerToken) {
        //JWT는 stateless하므로 서버에서 특별한 로그아웃 처리 불필요
        try {
            // Bearer 접두사 제거
            String token = bearerToken.substring(7);

            // 토큰의 남은 유효시간 계산
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            if (remainingSeconds > 0) {
                // 토큰을 블랙리스트에 추가
                tokenBlackListService.addToBlackList(token, remainingSeconds);
                log.info("Token is added to blacklist. Remaining time: {}seconds", remainingSeconds);
            }

            Map<String, String> responseData = Map.of(
                    "message", "로그아웃되었습니다.",
                    "instruction", "서버에서 토큰이 무효화되었습니다."
            );

            ApiCommonResponse<Map<String, String>> response = ApiCommonResponse.ok(responseData);
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            log.error("Error during logout process", e);
            throw new RuntimeException("로그아웃 처리 실패");
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
