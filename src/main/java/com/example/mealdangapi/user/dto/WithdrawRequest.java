package com.example.mealdangapi.user.dto;

import com.example.mealdangapi.user.entity.WithdrawalReasonCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {

    /**
     * 탈퇴 사유
     */
    private WithdrawalReasonCode reasonCode;

    /**
     * 기타 세부 내용
     *
     * ETC가 아니어도 값이 들어올 수 있도록
     * 단순 문자열로 받는다.
     */
    private String etcReason;
}