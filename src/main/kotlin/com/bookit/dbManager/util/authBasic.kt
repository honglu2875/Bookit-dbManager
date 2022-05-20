package com.bookit.dbManager.util

import com.bookit.dbManager.db.ApiAuthRepository
import org.slf4j.Logger
import java.util.*

/**
 * handles BASIC authentication.
 *
 * Decode the base64 encoded string, and look up the database for record.
 * Return true or false depending on whether the database record exists.
 *
 * @param apiRepo ApiAuthRepository. Used to access the authorization record.
 * @param code an authorization string. Can be null.
 * @return whether the decoded authorization string exists in the database.
 */
fun authenticateBASIC(apiRepo: ApiAuthRepository, code: String?, logger: Logger?=null): Boolean{
    if (code == null) return false
    val decoder = Base64.getDecoder()
    return try {
        val decoded = String(decoder.decode(code))
        apiRepo.existsByToken(decoded)
    }catch (e:Exception){
        logger?.error(e.toString())
        false
    }

}