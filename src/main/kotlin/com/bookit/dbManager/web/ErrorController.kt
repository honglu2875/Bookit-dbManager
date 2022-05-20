package com.bookit.dbManager.web

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import java.util.stream.Collectors
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest


@Controller
class ErrorController : ErrorController {
    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest, model: Model): String {
        model.addAttribute("errorCode", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
        model.addAttribute("errorMessage", request.reader.lines().collect(Collectors.joining(System.lineSeparator())))
        return "error"
    }
}