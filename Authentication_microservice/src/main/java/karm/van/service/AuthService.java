package karm.van.service;

import karm.van.dto.request.AuthRequest;
import karm.van.dto.response.AuthResponse;
import karm.van.model.MyUser;
import karm.van.repo.jpaRepo.MyUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final MyUserDetailsService myUserDetailsService;
    private final JwtService jwtService;
    private final MyUserRepo myUserRepo;

    public AuthResponse login(AuthRequest authRequest) throws BadCredentialsException, DisabledException {
        MyUser user = myUserRepo.findByName(authRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException("User with this name doesn't exist"));

        if (!user.isEnable() && user.getUnlockAt().isAfter(LocalDateTime.now())) {
            throw new DisabledException(
                    String.format(
                            "User is disabled.\nReason: %s\nUnblock at: %s",
                            user.getBlockReason(),
                            user.getUnlockAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect username or password", e);
        } catch (Exception e) {
            log.error(e.getClass() + " " + e.getMessage());
        }

        UserDetails userDetails = myUserDetailsService.loadUserByUsername(authRequest.username());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, refreshToken);
    }

}
