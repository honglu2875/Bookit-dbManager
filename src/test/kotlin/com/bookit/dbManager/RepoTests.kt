package com.bookit.dbManager

import com.bookit.dbManager.db.*
import com.bookit.dbManager.util.authenticateBASIC
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.OffsetDateTime
import java.util.*

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepoTests @Autowired constructor(
    val bookedSlotRepository: BookedSlotRepository,
    val backenduserRepository: BackendUserRepository,
    val apiAuthRepository: ApiAuthRepository) {

    @Test
    fun `database test`() {
        val me = BackendUser("a@a.a", "me", "asdf")
        val slot1 = BookedSlot(me, OffsetDateTime.now(), OffsetDateTime.now(),listOf())

        bookedSlotRepository.save(slot1)
        backenduserRepository.save(me)
    }

    @Test
    fun `authenticate test`() {
        val clientAuth = ApiToken()
        val encoder = Base64.getEncoder()
        val encoded = encoder.encodeToString(clientAuth.token.toByteArray())
        apiAuthRepository.save(clientAuth)
        assert(authenticateBASIC(apiAuthRepository, encoded))

        /*
        val httpclient = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/backend/welcome"))
            .header("Authorization", "Basic " + encoded)
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build()

        val response = httpclient.send(request, HttpResponse.BodyHandlers.ofString())
        assert(response.statusCode()==201)
        */

    }
}