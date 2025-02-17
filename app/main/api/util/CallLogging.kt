package api.util

import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun CallLoggingConfig.logging() {
    level = Level.INFO
    format { call ->
        val status = call.response.status()
        val httpMethod = call.request.httpMethod.value
        val userAgent = call.request.headers["User-Agent"]
        val path = call.request.path()
        val callId = call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
        "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, callId: $callId, path: $path"
    }
    filter { call -> call.request.path().startsWith("/actuator").not() }
}
