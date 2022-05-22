package com.bookit.dbManager.config

import com.bookit.dbManager.api.SecuredAPIController
import com.bookit.dbManager.db.ApiAuthRepository
import com.bookit.dbManager.util.authenticateBASIC
import com.bookit.dbManager.util.logger
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.servlet.HandlerExceptionResolver
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@EnableWebSecurity
class SecurityConfig {

    val log: Logger = logger<SecuredAPIController>()

    @Configuration
    @Order(1)
    class ApiWebSecurityConfigurationAdapter
    @Autowired constructor(val apiAuthRepository: ApiAuthRepository) : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/api/**","/error").permitAll()
                .and()
                .antMatcher("/api/backend/**")
                .addFilterBefore(AuthorizationFilter(apiAuthRepository), BasicAuthenticationFilter::class.java)
        }
    }

    // Shield "/api/backend/**" behind a Basic Authorization check
    class AuthorizationFilter @Autowired constructor (val apiAuthRepository: ApiAuthRepository): GenericFilterBean() {
        val log = logger<SecuredAPIController>()

        @Autowired
        @Qualifier("handlerExceptionResolver")
        private val resolver: HandlerExceptionResolver? = null

        @Throws(IOException::class, ServletException::class)
        override fun doFilter(
            request: ServletRequest,
            response: ServletResponse,
            chain: FilterChain
        ) {

            if (request is HttpServletRequest){
                val httprequest: HttpServletRequest = request
                val authHeader = (httprequest.getHeader("Authorization")?:"")
                    .trim().replace("  "," ").split(" ")
                if (authHeader[0].trim()=="Basic" && authenticateBASIC(apiAuthRepository, authHeader[1].trim(), logger=log)) {
                    log.debug("Authorization successful:\n" + authHeader.toString())
                    chain.doFilter(request, response)
                }
                else {
                    log.error("Authorization failed.")
                    unauthorizedErr(response)
                }
            }
            else {
                log.error("Unable to convert to HttpServletRequest.")
                nonHttpRequestErr(response)
            }
        }

        fun unauthorizedErr(response: ServletResponse){
            log.debug("Access with invalid token.")
            (response as HttpServletResponse).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "The token is not valid."
            )
        }

        fun nonHttpRequestErr(response: ServletResponse){
            log.debug("Access with invalid HTTP request.")
            (response as HttpServletResponse).sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Not an HTTP request."
            )
        }
    }



}

