package com.bookit.dbManager.db

import org.springframework.data.repository.CrudRepository

interface HistoryRepository : CrudRepository<Booking, Long> {
    fun findAllByHost(host: BackendUser): List<Booking>
}

interface LockedSlotRepository : CrudRepository<LockedSlot, Long> {
    fun findAllByHost(host: BackendUser): List<LockedSlot>
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