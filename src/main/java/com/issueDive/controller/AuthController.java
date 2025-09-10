package com.issueDive.controller;

import com.issueDive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.issueDive.dto.*;
import com.issueDive.service.UserService;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.issueDive.util.JwtUtil;

import com.issueDive.service.RedisService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Auth & User", description = "인증 및 사용자 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class
AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    /**
     * JavaDoc 스타일 주석 추가
     * Create: 회원가입
     * @param request email, password
     * @return 공통 응답 포맷 + 생성된 사용자 dto
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값 (중복된 이메일 등)", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> signUp(@Valid @RequestBody UserRequestDTO request){
        UserResponseDTO user = userService.signUp(request);
        // return 문 간소화
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiCommonResponse.ok(user));
    }

    /**
     * JavaDoc 스타일 주석 추가
     * 로그인
     * @param request email, password
     * @return 공통 응답 포맷 + JWT 토큰 정보
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 사용하여 로그인하고 JWT를 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 이메일 또는 비밀번호)", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ApiCommonResponse<JwtResponse>> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            // 인증된 사용자 정보 조회
            UserResponseDTO userResponse = userService.findUserByEmail(request.getEmail());

            // 9월10일 수정 - Access Token과 Refresh Token 모두 생성
            String accessToken = jwtUtil.generateAccessToken(userResponse.getId(), userResponse.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(userResponse.getId(), userResponse.getEmail());

            // 9월10일 수정 - Redis에 Refresh Token 저장
            redisService.saveRefreshToken(userResponse.getEmail(), refreshToken, jwtUtil.getRefreshExpiration());

            // 9월10일 수정 - JWT 응답에 Refresh Token 추가
            JwtResponse jwtResponse = JwtResponse.of(
                    accessToken,
                    refreshToken, // 9월10일 수정 - Refresh Token 추가
                    "Bearer",
                    14400L, // 4시간 (초 단위)
                    userResponse
            );
            return ResponseEntity.ok(ApiCommonResponse.ok(jwtResponse));
        } catch (Exception e) {
            //인증 실패시 예외 던지기 (GlobalExceptionHandler에서 처리)
            throw new com.issueDive.exception.AuthenticationFailedException();
        }
    }


    /**
     * JavaDoc 스타일 주석 추가
     * 로그아웃
     * @param bearerToken Authorization 헤더의 Bearer 토큰
     * @param userDetails 인증된 사용자 정보
     * @return 공통 응답 포맷 + 로그아웃 메시지
     */
    @Operation(summary = "로그아웃", description = "서버 측에서는 별도의 처리가 없으며, 클라이언트에서 토큰을 삭제해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiCommonResponse<Map<String, String>>> logout( @RequestHeader("Authorization") String bearerToken,
                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        //  Bearer 토큰에서 실제 토큰 추출
        String accessToken = bearerToken.substring(7);

        // 액세스 토큰의 남은 만료 시간 계산
        Long remainingTime = jwtUtil.getRemainingExpirationTime(accessToken);

        // 액세스 토큰을 블랙리스트에 추가
        if (remainingTime > 0) {
            redisService.addToBlacklist(accessToken, remainingTime);
        }

        // Redis에서 리프레시 토큰 삭제
        redisService.deleteRefreshToken(userDetails.getUsername());

        Map<String, String> responseData = Map.of(
                "message", "로그아웃되었습니다.",
                "instruction", "클라이언트에서 토큰을 삭제해주세요."
        );

        //  return 문 간소화
        return ResponseEntity.ok(ApiCommonResponse.ok(responseData));
    }

    /**
     * JavaDoc 스타일 주석 추가
     * Read: 단건 조회
     * @param id 조회할 사용자 ID
     * @return 공통 응답 포맷 + 해당 사용자 dto
     */
    @Operation(summary = "사용자 정보 조회", description = "ID로 특정 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> getUserById(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id){
        UserResponseDTO user = userService.findUserById(id);
        // return 문 간소화 및 HttpStatus.OK 제거 (ok()가 기본 200)
        return ResponseEntity.ok(ApiCommonResponse.ok(user));
    }

    /**
     * JavaDoc 스타일 주석 추가
     * Delete
     * @param id 삭제할 사용자 ID
     * @return 공통 응답 포맷 + 성공 메시지
     */
    @Operation(summary = "회원 탈퇴", description = "ID로 특정 사용자를 탈퇴 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @DeleteMapping("/user/{id}")
    public ResponseEntity<ApiCommonResponse<Void>> deleteUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long id){
        userService.deleteUser(id);
        userService.deleteUser(id);
        // 10월9일 수정 - return 문 간소화
        return ResponseEntity.ok(ApiCommonResponse.ok(null));
    }

    /**
     * JavaDoc 스타일 주석 추가
     * Read: 다중 조회
     * @return 공통 응답 포맷 + 전체 사용자 목록
     */
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
