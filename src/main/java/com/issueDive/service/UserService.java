package com.issueDive.service;

import com.issueDive.entity.User;
import com.issueDive.dto.UserResponseDTO;
import com.issueDive.dto.UserRequestDTO;
import com.issueDive.repository.UserRepository;
import com.issueDive.exception.UserNotFoundException;
import com.issueDive.exception.AuthenticationFailedException;
import com.issueDive.exception.DuplicateEmailException;

import lombok.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //회원가입
    @Transactional
    public UserResponseDTO signUp(UserRequestDTO request){
        //이메일 중복 검사
        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmailException(request.getEmail());
        }

        //비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        //User 엔티티 생성 및 저장
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .build();

        User saveUser = userRepository.save(user);

        return new UserResponseDTO(saveUser.getId(), saveUser.getUsername(), saveUser.getEmail());
    }

    //로그인
    public UserResponseDTO login(String email, String password){
        User user = userRepository.findByEmail(email)
                .orElseThrow(AuthenticationFailedException::new);
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new AuthenticationFailedException();
        }
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getEmail());
    }

    //사용자 id로 조회
    public UserResponseDTO findUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getEmail());
    }

    //사용자 email로 조회
    public UserResponseDTO findUserByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getEmail());
    }

    //사용자 삭제
    @Transactional
    public void deleteUser(Long id){
        if (!userRepository.existsById(id)){
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    // 전체 사용자 목록 조회
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponseDTO(u.getId(), u.getUsername(), u.getEmail()))
                .toList();
    }
}
