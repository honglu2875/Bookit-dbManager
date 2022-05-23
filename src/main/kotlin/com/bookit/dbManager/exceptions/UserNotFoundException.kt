package com.bookit.dbManager.exceptions

class UserNotFoundException : RuntimeException {
    constructor() : super()
    constructor(msg: String) : super(msg)
}