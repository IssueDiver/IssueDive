package com.issueDive.controller;

import com.issueDive.dto.LoginRequestDTO;
import com.issueDive.service.UserService;
import com.issueDive.dto.ApiResponse;
import com.issueDive.dto.UserRequestDTO;
import com.issueDive.dto.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

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
     * @return 공통 응답 포맷 + 사용자 DTO (또는 인증 토큰)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request){
        UserResponseDTO user = userService.login(request.getEmail(), request.getPassword());
        ApiResponse<UserResponseDTO> response = ApiResponse.ok(user);
        return new ResponseEntity<>(response, HttpStatus.OK);
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
}
