package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
// 모든 HTTP 요청당 한 번씩만 실행되도록 스프링 서블릿 컨테이너 환경의 OncePerRequestFilter를 상속받아 구현한 JWT 인증 필터입니다.
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // HTTP 요청 헤더 영역에서 "Authorization" 키에 매핑된 토큰 문자열을 추출합니다.
        String bearerJwt = request.getHeader("Authorization");

        // 인증 헤더 규격에 맞게 토큰이 존재하고 'Bearer '로 시작하는 유효한 상태인지 분기 검증합니다.
        if (bearerJwt != null && bearerJwt.startsWith("Bearer ")) {
            String jwt = jwtUtil.substringToken(bearerJwt);

            try {
                // SecurityContext에 인증 정보가 채워져 있는지 중복 검사하여 불필요한 파싱 연산을 방지합니다.
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    Claims claims = jwtUtil.extractClaims(jwt);
                    if (claims == null) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 JWT 토큰입니다.");
                        return;
                    }

                    // 정상 파싱된 Claims 정보를 기반으로 메서드를 호출합니다.
                    setAuthentication(claims, request);
                }
            } catch (SecurityException | MalformedJwtException e) {
                log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
                return;
            } catch (ExpiredJwtException e) {
                log.error("Expired JWT token, 만료된 JWT token 입니다.", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 JWT 토큰입니다.");
                return;
            } catch (UnsupportedJwtException e) {
                log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.", e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "지원되지 않는 JWT 토큰입니다.");
                return;
            } catch (Exception e) {
                log.error("Internal server error", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(Claims claims, HttpServletRequest request) {
        // JWT 페이로드 Subject 영역에서 파싱한 유저 고유 ID와 클레임 내의 이메일, 권한 등급을 순수 객체로 복원합니다.
        Long userId = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));

        // Spring Security 인가 아키텍처에 호환되도록 권한 등급 텍스트 앞에 규격 접두사("ROLE_")를 결합하여 GrantedAuthority 리스트를 생성합니다.
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.name()));

        // 컨트롤러 레이어 등에서 편리하게 꺼내 쓸 커스텀 인증 Principal DTO인 AuthUser 인스턴스를 매핑합니다.
        AuthUser authUser = new AuthUser(userId, email, userRole);

        // Principal 객체(AuthUser)와 권한 목록(authorities)을 담아 세션 프리 형태의 UsernamePasswordAuthenticationToken 토큰 인증 객체를 빌드합니다.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authUser, null, authorities
        );
        // 원격 IP 주소, 세션 ID 등 HTTP 요청 컨텍스트 세부 정보를 Spring Security 인증 토큰의 Details 영역에 동화시킵니다.
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 전역 컨텍스트 보관소(SecurityContextHolder) 내에 완성된 인증 토큰 객체를 바인딩하여 시스템에 인증이 완료되었음을 공표합니다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
