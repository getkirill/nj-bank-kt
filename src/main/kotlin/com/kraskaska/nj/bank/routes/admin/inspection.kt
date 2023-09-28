package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.DefaultTemplate
import com.kraskaska.nj.bank.DiscordSession
import com.kraskaska.nj.bank.RouteHandler
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.h1

val adminInspection: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if (!user.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val inspectedUser = call.request.queryParameters["uid"]
    if(inspectedUser == null) {
        call.respond("Please supply uid.")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 {+"Inspection of $inspectedUser"}

        }
    }
}