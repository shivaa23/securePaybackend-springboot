package com.securepay.modules.auth.service;

import com.securepay.common.security.PasswordService;
import com.securepay.modules.auth.entity.Otp;
import com.securepay.modules.auth.repository.OtpRepository;
import com.securepay.modules.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Port of App\Modules\Auth\Models\Otp helpers: {@code loginOtp}, {@code verifyOtp}, {@code verifyMpin}.
 * OTPs are stored peppered-bcrypt hashed (PasswordService), status "1" = unused, "0" = consumed.
 */
@Service
public class OtpService {

    public record Result(boolean success, String message) {
    }

    private final OtpRepository otpRepository;
    private final PasswordService passwordService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository otpRepository, PasswordService passwordService) {
        this.otpRepository = otpRepository;
        this.passwordService = passwordService;
    }

    /** Issue a 6-digit OTP + 7-digit reference for the user; returns {otp, otp_ref}. */
    @Transactional
    public Map<String, String> loginOtp(Long userId) {
        String newOtp = String.valueOf(100000 + random.nextInt(900000));
        String otpRef = String.valueOf(1000000 + random.nextInt(9000000));
        Otp row = new Otp();
        row.setUserId(userId);
        row.setOtpRef(otpRef);
        row.setOtp(passwordService.hashOtp(newOtp));
        row.setStatus("1");
        otpRepository.save(row);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("otp_ref", otpRef);
        out.put("otp", newOtp);
        return out;
    }

    /** Verify (and consume) an OTP issued to {@code user}. */
    @Transactional
    public Result verifyOtp(User user, String otpRef, String otp) {
        Optional<Otp> row = otpRepository.findFirstByUserIdAndOtpRefAndStatusOrderByIdDesc(
                user.getId(), otpRef, "1");
        if (row.isEmpty()) {
            return new Result(false, "Invalid OTP reference");
        }
        if (otp == null || !passwordService.matchesOtp(otp, row.get().getOtp())) {
            return new Result(false, "Invalid OTP");
        }
        row.get().setStatus("0");
        otpRepository.save(row.get());
        return new Result(true, "OTP Verified Successfully");
    }

    /** Otp::verifyMpin — peppered-bcrypt compare against the user's stored MPIN. */
    public boolean verifyMpin(User user, String mpin) {
        return mpin != null && user != null && user.getMpin() != null
                && passwordService.matchesMpin(mpin, user.getMpin());
    }
}
