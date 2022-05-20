package com.bookit.dbManager

import com.bookit.dbManager.db.ApiAuthRepository
import com.bookit.dbManager.db.ApiToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import java.util.*


@SpringBootApplication
class DbManagerApplication @Autowired constructor(val apiAuthRepository: ApiAuthRepository){
	@EventListener
	fun onApplicationEvent(event: ApplicationReadyEvent) {
		initializeBackendToken()
	}

	fun initializeBackendToken(){
		val clientAuth = ApiToken()
		val encoder = Base64.getEncoder()
		apiAuthRepository.save(clientAuth)
		println("""
			Backend server token:
			------
			Token:${clientAuth.token}
			Encoded:${encoder.encodeToString(clientAuth.token.toByteArray())}
			Id:${clientAuth.id}
			Scope:${clientAuth.scope}
			""")
	}
}

fun main(args: Array<String>) {
	runApplication<DbManagerApplication>(*args)
}

