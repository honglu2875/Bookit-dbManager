package com.bookit.dbManager.api

import com.bookit.dbManager.db.*
import com.bookit.dbManager.util.getBusyTime
import com.bookit.dbManager.util.logger
import com.bookit.dbManager.util.refreshAccessToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.annotation.Resource


// Overwrite WebSecurity configuration (if any) to expose the test endpoint for live testing
@EnableWebSecurity
class testSecurityConfig {
    @Configuration
    @Order(4)
    class UserLoginWebSecurityConfigurationAdapter : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http.authorizeRequests()
                .antMatchers("/error", "/api/**", "/test/**").permitAll()
                .antMatchers("/authtest/**").authenticated()
                .and()
                .oauth2Login().permitAll()
                .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessUrl("/")
        }
    }
}

@RestController
class testEndpoints @Autowired constructor(val bookedSlotRepository: BookedSlotRepository,
                                        val backendUserRepository: BackendUserRepository,
                                        val scheduleTypeRepository: ScheduleTypeRepository)
{

    val log = logger<testEndpoints>()

    @Resource
    private val env: Environment? = null

    // live db testing endpoint
    @GetMapping("/test/dbtest")
    fun dbtest(model: Model):String
    {
        val att1 = Attendee("aslkdjf@dalk.com","me")
        val att2 = Attendee("zzzzzz@dalk.com", "you")
        val timeslot = BusyTime(OffsetDateTime.now(), OffsetDateTime.now())

        val me = BackendUser("a@a.a", "me", "asdf", busyPeriods = listOf(timeslot))
        val slot1 = BookedSlot(me, OffsetDateTime.now(), OffsetDateTime.now(), listOf(att1,att2))
        val type1 = ScheduleType(me, 30, "")

        backendUserRepository.save(me)
        bookedSlotRepository.save(slot1)
        scheduleTypeRepository.save(type1)

        return "Please go to PSQL and play with the persisted records."
    }

    @GetMapping("/test/updatebusylist")
    fun updatebusylist():String {
        val clientId = env!!.getProperty("spring.security.oauth2.client.registration.google.clientId")
        val clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.clientSecret")
        val apiKey = env.getProperty("api.provider.google.key")
        val user = backendUserRepository.findByEmail("honglu2875@gmail.com")
        val accessToken = refreshAccessToken(clientId!!, clientSecret!!, user!!.refreshToken)

        val busyList = getBusyTime(
            apiKey!!,
            accessToken!!.getString("access_token"),
            user.email,
            log=log
        )
        backendUserRepository.save(user.updateBusyTime(busyList))
        return busyList.toString()
    }

    // auth test
    @GetMapping("/authtest")
    fun authtest(@RegisteredOAuth2AuthorizedClient("google") user: OAuth2AuthorizedClient,
                 @AuthenticationPrincipal principal: OAuth2User
    ):String {
        val newUser = BackendUser(
            principal.getAttribute<String>("email")!!,
            user.refreshToken!!.tokenValue,
        )
        backendUserRepository.save(newUser)
        return user.accessToken.tokenValue + "<hr>Refresh token:<br>" +
                user.refreshToken!!.tokenValue + "<hr>Authentication Principal<br>" +
                principal.toString() + "<hr>ClientRegistration:<br>" +
                user.clientRegistration.toString()
    }

    // refresh token test
    @GetMapping("/authtest/refresh")
    fun refresh(@RegisteredOAuth2AuthorizedClient("google") user: OAuth2AuthorizedClient,
                @AuthenticationPrincipal principal: OAuth2User):Any? {
        return refreshAccessToken(
            user.clientRegistration.clientId,
            user.clientRegistration.clientSecret,
            user.refreshToken!!.tokenValue
        ).toString()

    }
}