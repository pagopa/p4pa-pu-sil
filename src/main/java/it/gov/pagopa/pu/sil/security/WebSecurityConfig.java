package it.gov.pagopa.pu.sil.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  public static final String SOAP_WS_BASE_PATH = "/soap";

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public WebSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(requests -> requests
        // Swagger endpoints
        .requestMatchers(
          "/swagger-ui.html",
          "/swagger-ui/**",
          "/v3/api-docs/**"
        ).permitAll()

        // Actuator endpoints
        .requestMatchers(
          "/actuator",
          "/actuator/**"
        ).permitAll()

        // WsSoap (only to retrieve WSDL and XSD)
//        .requestMatchers(
//          HttpMethod.GET,
//          SOAP_WS_BASE_PATH+"/*/*.wsdl",
//          SOAP_WS_BASE_PATH+"/*/*.xsd"
//        ).permitAll()

        // WsSoap (only to retrieve WSDL and XSD)
        // use regex matcher because request matcher causes this error:
        // No more pattern data allowed after {*...} or ** pattern element
        .requestMatchers(
          RegexRequestMatcher.regexMatcher(
            HttpMethod.GET,
            SOAP_WS_BASE_PATH + "/.*/.*\\.wsdl$"
          )
        ).permitAll()
        .requestMatchers(
          RegexRequestMatcher.regexMatcher(
            HttpMethod.GET,
            SOAP_WS_BASE_PATH + "/.*/.*\\.xsd$"
          )
        ).permitAll()

        .anyRequest().authenticated()
      )
      .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

}

