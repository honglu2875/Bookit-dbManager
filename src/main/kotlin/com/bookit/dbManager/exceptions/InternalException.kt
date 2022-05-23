package com.bookit.dbManager.exceptions

class InternalException : RuntimeException {
    constructor() : super()
    constructor(msg: String) : super(msg)
}