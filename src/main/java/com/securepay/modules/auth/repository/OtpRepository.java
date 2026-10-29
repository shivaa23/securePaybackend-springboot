package com.securepay.modules.auth.repository;

import com.securepay.modules.auth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link Otp}. */
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findFirstByUserIdAndOtpRefOrderByIdDesc(Long userId, String otpRef);

    Optional<Otp> findFirstByUserIdOrderByIdDesc(Long userId);

    /** Active (status "1") OTP row for a user + reference (legacy Otp::verifyOtp). */
    Optional<Otp> findFirstByUserIdAndOtpRefAndStatusOrderByIdDesc(Long userId, String otpRef, String status);
}
