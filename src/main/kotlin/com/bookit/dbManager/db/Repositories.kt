package com.bookit.dbManager.db

import org.springframework.data.repository.CrudRepository

interface BookedSlotRepository : CrudRepository<BookedSlot, Long> {
    fun findAllByHost(host: BackendUser): List<BookedSlot>
}

interface BackendUserRepository : CrudRepository<BackendUser, String> {
    fun findByEmail(email: String): BackendUser?
}

interface ScheduleTypeRepository : CrudRepository<ScheduleType, Long> {
    fun findByToken(token: String): ScheduleType?
}

interface ApiAuthRepository : CrudRepository<ApiToken, Long> {
    fun findByToken(token: String): ApiToken?
    fun existsByToken(token: String): Boolean
}