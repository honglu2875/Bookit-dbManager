package com.bookit.dbManager.util

import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@Serializable
data class DefaultSuccessMsg(val status: String = "success")

fun <T> successResponse(content: T, log: Logger? = null, isJson: Boolean = true): ResponseEntity<T> {
    log?.debug(content.toString())
    return ResponseEntity
        .ok()
        .contentType(if (isJson) MediaType.APPLICATION_JSON else MediaType.TEXT_PLAIN)
        .body(content)
}

fun successResponseDefault(log: Logger? = null): ResponseEntity<DefaultSuccessMsg> {
    val content = DefaultSuccessMsg()
    log?.debug(content.toString())
    return ResponseEntity.ok(content)
}