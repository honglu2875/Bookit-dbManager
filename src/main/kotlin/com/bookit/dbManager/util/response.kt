package com.bookit.dbManager.util

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun errorGenerator(
    errMsg: String,
    status: HttpStatus,
    statusMsg: String = "unsuccessful",
    log: Logger?=null
): ResponseEntity<Map<String, String>> {
    val body = mapOf("status" to statusMsg, "message" to errMsg)

    log?.error(errMsg)
    return ResponseEntity(body, status)
}

fun successGenerator(
    successMsg: String,
    statusMsg: String = "success",
    log: Logger?=null
    ): ResponseEntity<Map<String, String>> {
    val body = mapOf("status" to statusMsg, "message" to successMsg)

    log?.debug(successMsg)
    return ResponseEntity.ok(body)
}