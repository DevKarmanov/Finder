package karm.van.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import karm.van.dto.request.AuthRequest;
import karm.van.dto.request.UserDtoRequest;
import karm.van.dto.response.AuthResponse;
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
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {
    private final MyUserService myUserService;
    private final JwtService jwtService;
    private final AuthService authService;

    @Operation(
            summary = "Validate the access token",
            description = "Checks if the provided JWT token is valid."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Valid token",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"valid\": true}")
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid token",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"error\": \"Invalid token\"}")
            )
    )
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"refreshToken\": \"token\"}")
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "New access token generated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"accessToken\": \"token\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Refresh token is missing",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "\"Refresh token is missing\"")
                            )
                    )
            }
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @RequestBody Map<String, String> request
    ) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is missing");
        }

        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and generate access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns access and refresh tokens",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad credentials or user not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User account is disabled",
                    content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User login credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = AuthRequest.class)))
    public ResponseEntity<?> createAuthenticationToken(
            @RequestBody AuthRequest authRequest) {

        try {
            return ResponseEntity.ok(authService.login(authRequest));
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


    @Operation(summary = "Register a new user",
            description = "Registers a new user in the system with the provided details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User registered successfully.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400",
                    description = "If the user already exists.",
                    content = @Content(mediaType = "application/json"))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details of the user to be registered.")
    @PostMapping(value = "/register")
    public ResponseEntity<?> register(@RequestBody UserDtoRequest userDtoRequest) {
        try {
            myUserService.registerUser(userDtoRequest);
            return ResponseEntity.ok("User registered successfully");
        } catch (UserAlreadyExist e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @Operation(
            summary = "Change the admin registration key",
            description = "Generates a new admin registration key. Requires ADMIN authority."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Admin key successfully changed.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request if something goes wrong.",
                    content = @Content(mediaType = "application/json")
            )
    })
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

    @Operation(
            summary = "Get the admin registration key",
            description = "Retrieves the current admin registration key. Requires ADMIN authority."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved the admin registration key.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UUID.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request if something goes wrong.",
                    content = @Content(mediaType = "application/json")
            )
    })
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
