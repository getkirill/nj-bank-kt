package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.Account
import com.kraskaska.nj.bank.DefaultTemplate
import com.kraskaska.nj.bank.DiscordSession
import com.kraskaska.nj.bank.RouteHandler
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.a
import kotlinx.html.code
import kotlinx.html.h1
import kotlinx.html.p
import org.bson.types.ObjectId

val adminDeposit: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if (!user.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val address = ObjectId(call.request.queryParameters["address"]!!)
    val amount = call.request.queryParameters["amount"]!!.toLong()
    val account = Account.get(address)
    if (account == null) {
        call.respondHtmlTemplate(DefaultTemplate()) {
            content {
                h1 { +"Первый новояпонский банк" }
                p { code { +"Account not found." } }
                a(classes = "action", href = "/admin") { +"Go back" }
            }
        }
        return@handler
    }
    account.deposit(amount)
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            p { code { +"Balance updated: +$amount (${account.balance})" } }
        }
    }
}