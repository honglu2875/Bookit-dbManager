package com.bookit.dbManager.util

import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.springframework.http.ResponseEntity

@Serializable
data class DefaultSuccessMsg(val status: String = "success")

fun <T> successResponse(content: T, log: Logger? = null): ResponseEntity<T> {
    log?.debug(content.toString())
    return ResponseEntity.ok(content)
}

fun successResponseDefault(log: Logger? = null): ResponseEntity<DefaultSuccessMsg> {
    val content = DefaultSuccessMsg()
    log?.debug(content.toString())
    return ResponseEntity.ok(content)
}