package br.com.clubedojava.webstore.security;

import br.com.clubedojava.webstore.dto.LoginRequestDTO;
import br.com.clubedojava.webstore.dto.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequestDTO loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestDTO.class);
            System.out.println("Tentativa de login para: " + loginRequest.getEmail());
            
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );
            
            try {
                return authenticationManager.authenticate(authToken);
            } catch (AuthenticationException e) {
                System.out.println("Erro na autenticação: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar requisição: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse authentication request", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();

        String token = jwtTokenProvider.generateToken(userDetails);

        response.addHeader("Authorization", "Bearer " + token);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", token);
        responseBody.put("user", userDetails);

        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}