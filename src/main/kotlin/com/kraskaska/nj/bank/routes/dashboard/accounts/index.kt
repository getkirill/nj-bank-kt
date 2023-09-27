package com.kraskaska.nj.bank.routes.dashboard.accounts

import com.kraskaska.nj.bank.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.sessions.*
import kotlinx.html.*

val dashboardAccounts: RouteHandler = {
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    val accounts =
        if (user.isAdmin && call.request.queryParameters["of"] != null) Account.byOwner(call.request.queryParameters["of"]!!.toLong()) else user.accounts
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(listOf("Панель управления" to "/dashboard", "Счета" to "#"))
            h2 { +"Cчета" }
            a(
                classes = "action",
                href = "/dashboard/accounts/new/checking${if (user.isAdmin && call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}"
            ) { +"Новый расчётный счёт" }
            a(
                classes = "action",
                href = "/dashboard/accounts/new/savings${if (user.isAdmin && call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}"
            ) { +"Новый сберегательный счёт" }
            if (accounts.toList().isEmpty()) {
                p { +"У вас нет счетов." }
            } else {
                ul {
                    accounts.forEach {
                        li {
                            +"${it.name} ("; code { +it._id.toHexString() }; +") - ${
                            mapOf(
                                "checking" to "расчётный",
                                "savings" to "сберегательный"
                            )[it.type.toString()]
                        } - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol "
                            span {
                                +"(";a(href = "/dashboard/accounts/delete/${it._id.toHexString()}") { +"Удалить" };/*+" ";a(
                                                href = "/dashboard/accounts/edit/${it._id.toHexString()}"
                                            ) { +"Переименовать" };*/+")"
                            }
                        }
                    }
                }
            }
        }
    }
}