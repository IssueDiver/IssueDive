package com.issueDive.repository;

import com.issueDive.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 9월 10일 최종 - Refresh Token Repository (최소 기능)

 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
    /**
     * 9월 10일 최종 - 토큰으로 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 9월 10일 최종 - 사용자 이메일로 삭제 (logout용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userEmail = :userEmail")
    void deleteByUserEmail(@Param("userEmail") String userEmail);


}
