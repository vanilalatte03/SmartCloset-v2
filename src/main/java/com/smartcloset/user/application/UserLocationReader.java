package com.smartcloset.user.application;

public interface UserLocationReader {

    UserLocationSnapshot getRequiredLocationSnapshot(Long userId);
}
