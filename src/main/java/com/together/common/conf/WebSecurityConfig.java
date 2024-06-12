package com.together.common.conf;

import com.together.common.utils.jwt.service.JwtTokenProvider;
import com.together.common.utils.jwt.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 라이브러리 = 상속 없이 간편하게 spring의 기능 사용. extends 안해도 된다는 소리.
@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
//@preAuthorize, @PostAuthorize, @Secured
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);
    private final JwtTokenProvider jwtTokenProvider;


    @Bean
    //5.5이상:요청을 가로채서 인증 및 인가를 체크하고 다음으로 넘어가게 함. spring의 약한 결합 방식: 내부 비지니스 로직에는 영향을 덜 미침. 5.4이하는 configure 오버라이드
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("WebSecurity - filterChain: " + http);
        //폼 기반 로그인 비활성화
        http.formLogin( (login) -> login.disable());
        //REST API이므로 http 및 csrf 비활성화
        //HTTP 기본 인증 비활성화
        http.httpBasic((basic) -> basic.disable());
        //CSRF 공격 방어 기능 비활성화
        http.csrf((csrf) -> csrf.disable() );
        //JWT를 사용하므로 세션 미사용.
        //세션 관리 정책: 세션 인증을 사용하지 않고 JWT를 사용하여 인증.
        http.sessionManagement(management
                -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests((authorizeRequests) ->
                        authorizeRequests
                            //.anyRequest().permitAll()
                                //해당 API에 대한 모든 요청 허가
                                .requestMatchers("/member/writeMember").permitAll()//회원가입
                                .requestMatchers("/member/loginCheck").permitAll()//로그인
                                .requestMatchers("/member/decodingToken").permitAll()//토큰이 유효한 토큰인지 확인
                                .requestMatchers("/board/getWriteList").permitAll()//글목록확인
                                .requestMatchers("/board/writeImage").permitAll()//이미지 임시 업로드. 권한 확인하려면 csrf 토큰 처리 필요.

                                //user권한이 있어야 요청 가능
                                .requestMatchers("/member/updateMember").hasRole("USER")
                                .requestMatchers("/member/deleteMember").hasRole("USER")
                                .requestMatchers("/member/getMemberInfo").hasRole("USER")
                                .requestMatchers("/board/writeBoard").hasRole("USER")//글 작성
                                .requestMatchers("/board/getUpdateBoard").hasRole("USER")//글 1개: User외에 다른 권한이 더 필요할 수도.
                                .requestMatchers("/board/updateBoard").hasRole("USER")//글 수정
                                .requestMatchers("/board/deleteBoard").hasRole("USER")//글 삭제
                                .requestMatchers("/calendar/getCalendarList").hasRole("USER")
                                .requestMatchers("/calendar/writeCalendar").hasRole("USER")
                                .requestMatchers("/calendar/updateCalendar").hasRole("USER")
                                .requestMatchers("/calendar/deleteCalendar").hasRole("USER")
                                .requestMatchers("/calendar/getOneCalendar").hasRole("USER")

                                //test
                                .requestMatchers("/member/test4").permitAll()
                                .requestMatchers("/board/test3").permitAll()
                                .requestMatchers("/board/test1").permitAll()
                                .requestMatchers("/board/test2").hasRole("USER")
                                .requestMatchers("/member/test").hasRole("USER")
                                .requestMatchers("/common/security/isUser").hasRole("USER")//post + user 확인

                                //이 외의 모든 요청은 인증을 필요로 함
                                //.anyRequest().authenticated()
                                .anyRequest().authenticated()// "/board/writeBoard

        //직접 구현한 필터로 JWT 인증
        ).addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        log.info("WebSecurity - passwordEncoder");
        //return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.info("WebSecurity - authenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }

}