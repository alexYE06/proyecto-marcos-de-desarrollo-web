package pe.transporte.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.transporte.security.JwtAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) ->
                                escribirJsonError(res, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTORIZADO",
                                        "Debes iniciar sesión con un token JWT válido para acceder a este recurso."))
                        .accessDeniedHandler((req, res, accessEx) ->
                                escribirJsonError(res, HttpServletResponse.SC_FORBIDDEN, "ACCESO_DENEGADO",
                                        "Acceso denegado por Spring Security: tu cargo no cuenta con privilegios para esta operación."))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Rutas SSR Thymeleaf y archivos estáticos públicos
                        .requestMatchers("/", "/inicio", "/cartelera", "/*.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        // Autenticación pública y catálogo abierto
                        .requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/logout", "/api/catalogo/**").permitAll()
                        // Segregación estricta de APIs por Rol (RBAC con Spring Security + JWT)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/vendedor/**").hasAnyRole("VENDEDOR", "ADMIN")
                        .requestMatchers("/api/embarque/**").hasAnyRole("EMBARQUE", "ADMIN")
                        .requestMatchers("/api/cliente/**", "/api/auth/me", "/api/auth/perfil").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirJsonError(HttpServletResponse response, int status, String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", codigo);
        body.put("mensaje", mensaje);
        body.put("timestamp", LocalDateTime.now().toString());
        MAPPER.writeValue(response.getWriter(), body);
    }
}
