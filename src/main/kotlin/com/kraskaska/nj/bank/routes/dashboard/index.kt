package com.kraskaska.nj.bank.routes.dashboard

import com.kraskaska.nj.bank.*
import com.kraskaska.nj.bank.routes.dashboard.accounts.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*

fun Routing.configureDashboardRouting() {
    authenticate("discord-auth", "session-auth") {
        get("/dashboard", dashboardHome)
        get("/dashboard/accounts", dashboardAccounts)
        get("/dashboard/accounts/new/{type}", newAccount)
        get("/dashboard/accounts/delete/{id}", deleteAccount)
        get("/dashboard/transfer", accountTransfer)
        get("/dashboard/transactions", accountTransactions)
    }
}

val dashboardHome: RouteHandler = {
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            p { +user.name }
            p { +"Баланс: ${"%.2f".format(user.balance / 32.0)} $okaneSymbol" }
            a(classes = "action", href = "/dashboard/accounts") { +"Счета" }
            a(classes = "action", href = "/dashboard/transfer") { +"Перевод" }
            a(classes = "action", href = "/dashboard/transactions") { +"Транзакции" }
        }
    }
}