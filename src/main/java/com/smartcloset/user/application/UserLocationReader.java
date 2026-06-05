package com.smartcloset.user.application;

/**
 * 날씨 provider가 사용자 저장 위치를 읽기 위해 의존하는 application boundary다.
 *
 * <p>Provider가 JPA entity에 직접 의존하지 않도록 위치 snapshot만 반환한다.</p>
 */
public interface UserLocationReader {

    UserLocationSnapshot getRequiredLocationSnapshot(Long userId);
}
