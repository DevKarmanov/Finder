package karm.van.controller;

import karm.van.dto.request.AuthRequest;
import karm.van.dto.request.UserDtoRequest;
import karm.van.exception.UserAlreadyExist;
import karm.van.service.AuthService;
import karm.van.service.JwtService;
import karm.van.service.MyUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {
    private final MyUserService myUserService;
    private final JwtService jwtService;
    private final AuthService authService;

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is missing");
        }

        // Обновление Access Token с использованием Refresh Token
        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest){
        try {
            return ResponseEntity.ok(authService.login(authRequest));
        }catch (BadCredentialsException | UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (DisabledException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


    @PostMapping(value = "/register")
    public ResponseEntity<?> register(@RequestBody UserDtoRequest userDtoRequest) {
        try {
            myUserService.registerUser(userDtoRequest);
            return ResponseEntity.ok("User registered successfully");
        } catch (UserAlreadyExist e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    //todo описать в readme этот endpoint
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/admin/key/change")
    public ResponseEntity<?> changeAdminKey() {
        try {
            return ResponseEntity.ok(Map.of("key",myUserService.changeAdminKey()));
        } catch (Exception e) {
            System.err.println(e.getClass()+": "+e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //todo описать в readme этот endpoint
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = "/admin/key")
    public ResponseEntity<?> getAdminKey() {
        try {
            return ResponseEntity.ok(myUserService.getAdminKey());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
