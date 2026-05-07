package com.practice.paymentassignment.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// @NoArgsConstructor, @AllArgsConstructor를 사용하고 Serializable을 상속받은 이유가 있나요?
// 저라면 field를 불변(immutable)하게 final로 선언한 후 @RequiredArgsConstructor를 사용할 것 같습니다.
// 혹은 record를 사용할 것 같습니다. 
// 단순히 수정 후 넘어가지 말고, 본인이 그렇게 사용하신 이유에 대해 다시 고민해보시길 바랍니다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRedisResponse implements Serializable {
    private int status;
    private byte[] body;
}
