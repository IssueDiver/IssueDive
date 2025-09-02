package com.issueDive.controller;

import com.issueDive.dto.ApiCommonResponse;
import com.issueDive.dto.LoginRequestDTO;
import com.issueDive.dto.UserRequestDTO;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Auth & User", description = "인증 및 사용자 관리 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

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
    public ResponseEntity<ApiCommonResponse<UserResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request){
        UserResponseDTO user = userService.login(request.getUsername(), request.getPassword());
        ApiCommonResponse<UserResponseDTO> response = ApiCommonResponse.ok(user);
        return new ResponseEntity<>(response, HttpStatus.OK);
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
