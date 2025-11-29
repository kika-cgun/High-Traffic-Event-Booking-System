package com.example.hightrafficeventbookingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers("/api/**").authenticated()
//                        .anyRequest().permitAll()
//                )
//                .formLogin(formLogin ->
//                        formLogin
//                                .loginPage("/login")
//                                .loginProcessingUrl("/login")
//                                .defaultSuccessUrl("/", true)
//                                .permitAll()
//                )
//                .logout(logout ->
//                        logout
//                                .logoutUrl("/logout")
//                                .logoutSuccessUrl("/login?logout")
//                                .permitAll()
//                )
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers("/api/**")
//                );
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Wyłączamy CSRF (niepotrzebne w REST API bez sesji przeglądarkowej)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Konfiguracja uprawnień
                .authorizeHttpRequests(auth -> auth
                        // Pozwalamy każdemu uderzyć do endpointów rezerwacji
                        .requestMatchers("/api/reservations/**").permitAll()
                        // Wszystko inne wymagałoby autoryzacji (na przyszłość)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
