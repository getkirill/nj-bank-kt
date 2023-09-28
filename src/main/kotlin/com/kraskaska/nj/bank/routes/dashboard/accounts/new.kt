package com.kraskaska.nj.bank.routes.dashboard.accounts

import com.kraskaska.nj.bank.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*

val newAccount: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if (!listOf("checking", "savings").contains(call.parameters["type"])) {
        call.response.status(HttpStatusCode.NotFound)
        call.respond("Not found.")
        return@handler
    }
    if (call.request.queryParameters["account-name"] != null) {
//        if (user.isAdmin && call.request.queryParameters["of"] != null)
//            Account.create(
//                call.request.queryParameters["of"]!!.toLong(), mapOf(
//                    "checking" to Account.AccountType.CHECKING,
//                    "savings" to Account.AccountType.SAVINGS
//                )[call.parameters["type"]]!!, call.request.queryParameters["account-name"]!!
//            )
//        else
            user.createAccount(
                mapOf(
                    "checking" to Account.AccountType.CHECKING,
                    "savings" to Account.AccountType.SAVINGS
                )[call.parameters["type"]]!!, call.request.queryParameters["account-name"]!!
            )
        call.respondRedirect("/dashboard/accounts${if (call.request.queryParameters["of"] != null) "?of=${call.request.queryParameters["of"]}" else ""}")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(
                listOf(
                    "Панель управления" to "/dashboard",
                    "Счета" to "/dashboard/accounts",
                    "Новый счёт" to "#"
                )
            )
            h2 {
                +"Новый ${
                    mapOf(
                        "checking" to "расчётный",
                        "savings" to "сберегательный"
                    )[call.parameters["type"]]
                } счёт"
            }
            form {
                if (call.request.queryParameters["of"] != null) hiddenInput(name = "of") {
                    value = call.request.queryParameters["of"]!!
                }
                p { +"Имя счёта: "; textInput(name = "account-name") }
                submitInput { value = "Создать счёт..." }
            }
        }
    }
}