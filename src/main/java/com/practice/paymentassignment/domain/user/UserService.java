package com.practice.paymentassignment.domain.user;

import com.practice.paymentassignment.global.exception.UserNotFoundException;
import com.practice.paymentassignment.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 컨벤션이 일관적이지 않습니다. 어떤 서브도메인은 service 패키지 하위에 존재하고, 어떤 건 아니고
// 스타일을 일관성 있게 변경해주세요.
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));
    }
}
