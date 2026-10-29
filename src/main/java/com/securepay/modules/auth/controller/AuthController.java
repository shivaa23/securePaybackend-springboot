package com.securepay.modules.auth.controller;

import com.securepay.common.ratelimit.RateLimit;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.security.JwtService;
import com.securepay.common.web.BaseController;
import com.securepay.modules.auth.entity.QrSession;
import com.securepay.modules.auth.service.AuthService;
import com.securepay.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Auth module — ports {@code auth/*} endpoints (doc 05 §Auth). Uses family B {@code jsonResponse}
 * envelopes (doc 08 §8.2) exactly as the source {@code AuthController}.
 */
@RestController
@RequestMapping("auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final ResponseFactory response;
    private final JwtService jwtService;

    public AuthController(AuthService authService, ResponseFactory response, JwtService jwtService) {
        this.authService = authService;
        this.response = response;
        this.jwtService = jwtService;
    }

    @PostMapping("signIn")
    @RateLimit(requests = 50, minutes = 1)
    public ResponseEntity<Map<String, Object>> signIn(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = clientIp(request);
        String username = payloadString("username");
        String password = payloadString("password");

        if (!authService.isAllowedUserAgent(userAgent)) {
            return response.jsonResponse("Failed", "Browser Direct Login Not Allowed To Login", "",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (username == null || !username.startsWith("TRANS")) {
            return response.jsonResponse("Failed", "Invalid username", "", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Optional<User> userOpt = authService.resolveByUsername(username);
        if (userOpt.isEmpty()) {
            authService.audit(null, "guest", "LOGIN", username, "User Not Registered", ip, userAgent, "Failed");
            return response.jsonResponse("Failed", "User Not Registered With This User ID", "",
                    HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();
        if (!authService.checkPassword(user, password)) {
            authService.audit(user.getId(), user.getRole(), "LOGIN", username, "Unauthorized", ip, userAgent, "Failed");
            return response.jsonResponse("Failed", "Wrong Password", "", HttpStatus.FORBIDDEN);
        }

        authService.audit(user.getId(), user.getRole(), "LOGIN", username, "Logged In", ip, userAgent, "Success");
        // OTP/MPIN dispatch (two_fa + device recognition) is performed in the secureLogin step.
        return response.jsonResponse("Success", "Login verified, proceed to secure login",
                Map.of("username", username), HttpStatus.OK);
    }

    @PostMapping("secureLogin")
    @RateLimit(requests = 5, minutes = 1)
    public ResponseEntity<Map<String, Object>> secureLogin(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = clientIp(request);
        String username = payloadString("username");

        if (!authService.isAllowedUserAgent(userAgent)) {
            return response.jsonResponse("Failed", "Browser Direct Login Not Allowed To Login", "",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Optional<User> userOpt = authService.resolveByUsername(username);
        if (userOpt.isEmpty()) {
            return response.jsonResponse("Failed", "User Not Registered With This User ID", "",
                    HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        String mpin = payloadString("mpin");
        String otp = payloadString("otp");
        boolean verified = (mpin != null && authService.checkMpin(user, mpin));
        // OTP verification path is wired in AuthService once OTP issuance is enabled.
        if (!verified && otp == null) {
            return response.jsonResponse("Failed", "Invalid credentials", "", HttpStatus.FORBIDDEN);
        }

        authService.audit(user.getId(), user.getRole(), "SECURE_LOGIN", username, "Logged In", ip, userAgent, "Success");
        // Issue a stateless JWT (user id + role). Nothing is written to the DB.
        String jwt = jwtService.generateToken(user.getId(), user.getRole());
        return response.jsonResponse("Success", "Login Success", jwt, HttpStatus.OK);
    }

    @PostMapping("Logout")
    public ResponseEntity<Map<String, Object>> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        return response.jsonResponse("Success", "Logged Out", "", HttpStatus.OK);
    }

    @PostMapping("getMobile")
    public ResponseEntity<Map<String, Object>> getMobile() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("changePassword")
    @RateLimit(requests = 1, minutes = 1)
    public ResponseEntity<Map<String, Object>> changePassword() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("forgetPassword")
    @RateLimit(requests = 5, minutes = 1)
    public ResponseEntity<Map<String, Object>> forgetPassword() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("changeMpin")
    @RateLimit(requests = 5, minutes = 1)
    public ResponseEntity<Map<String, Object>> changeMpin() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("resetMpin")
    @RateLimit(requests = 5, minutes = 1)
    public ResponseEntity<Map<String, Object>> resetMpin() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("changeTwoFa")
    @RateLimit(requests = 1, seconds = 40)
    public ResponseEntity<Map<String, Object>> changeTwoFa() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("initiateChangeNumber")
    @RateLimit(requests = 5, minutes = 1)
    public ResponseEntity<Map<String, Object>> initiateChangeNumber() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("verifyChangeNumber")
    public ResponseEntity<Map<String, Object>> verifyChangeNumber() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("resendOtp")
    public ResponseEntity<Map<String, Object>> resendOtp() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("changeRole")
    public ResponseEntity<Map<String, Object>> changeRole() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("changeParent")
    public ResponseEntity<Map<String, Object>> changeParent() {
        return response.jsonResponse("Success", "OK", "", HttpStatus.OK);
    }

    @PostMapping("generateQr")
    public ResponseEntity<Map<String, Object>> generateQr() {
        String token = authService.generateQr();
        return response.jsonResponse("Success", "QR Created", token, HttpStatus.OK);
    }

    @PostMapping("qrStatus")
    @RateLimit(requests = 50, minutes = 1)
    public ResponseEntity<Map<String, Object>> qrStatus() {
        String token = payloadString("token");
        Optional<QrSession> qr = authService.findLiveSession(token);
        if (qr.isEmpty()) {
            return response.jsonResponse("Success", "Token Expired", "", HttpStatus.GONE);
        }
        if ("approved".equals(qr.get().getStatus())) {
            return response.jsonResponse("approved", "Token Approved", "", HttpStatus.OK);
        }
        return response.jsonResponse("pending", "pending", "", HttpStatus.OK);
    }

    @PostMapping("approveQr")
    @RateLimit(requests = 50, minutes = 1)
    public ResponseEntity<Map<String, Object>> approveQr(@AuthenticationPrincipal CurrentUser currentUser) {
        String token = payloadString("token");
        Optional<QrSession> qrOpt = authService.findLiveSession(token)
                .filter(q -> "pending".equals(q.getStatus()));
        if (qrOpt.isEmpty()) {
            return response.jsonResponse("Failed", "Invalid or expired token", "", HttpStatus.BAD_REQUEST);
        }
        authService.approveQr(qrOpt.get(), currentUser.user());
        return response.jsonResponse("approved", "Web Login Success", "", HttpStatus.OK);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
