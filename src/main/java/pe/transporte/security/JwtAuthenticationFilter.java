package pe.transporte.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro JWT de Spring Security: verifica la firma HMAC-SHA256 del token Bearer y establece
 * la identidad y rol (ROLE_ADMIN, ROLE_VENDEDOR, ROLE_EMBARQUE, ROLE_CLIENTE) en SecurityContextHolder.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SessionTokenStore tokenStore;

    public JwtAuthenticationFilter(SessionTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = AuthInterceptor.extraerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<SesionUsuario> sesionOpt = tokenStore.obtenerSesion(token);
            if (sesionOpt.isPresent()) {
                SesionUsuario sesion = sesionOpt.get();
                request.setAttribute(AuthInterceptor.ATRIBUTO_SESION, sesion);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + sesion.rol().name()));
                var authentication = new UsernamePasswordAuthenticationToken(
                        sesion, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
