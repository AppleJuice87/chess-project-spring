package com.chessprojectspring.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 자동 생성
@Builder // 빌더 패턴을 사용하는 메소드 자동 생성
public class EditPwdRequest {

    @NotBlank(message = "Old password is mandatory")
    @Size(min = 6, max = 20, message = "Old password must be between 6 and 20 characters")
    private String oldPassword;

    @NotBlank(message = "New password is mandatory")
    @Size(min = 6, max = 20, message = "New password must be between 6 and 20 characters")
    private String newPassword;
}