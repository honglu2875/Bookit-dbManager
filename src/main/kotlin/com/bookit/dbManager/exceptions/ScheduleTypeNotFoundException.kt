package com.bookit.dbManager.exceptions

class ScheduleTypeNotFoundException : RuntimeException {
    constructor() : super()
    constructor(msg: String) : super(msg)
}