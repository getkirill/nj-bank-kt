package com.kraskaska.nj.bank.routes.dashboard

import com.kraskaska.nj.bank.DefaultTemplate
import com.kraskaska.nj.bank.DiscordSession
import com.kraskaska.nj.bank.RouteHandler
import com.kraskaska.nj.bank.okaneSymbol
import com.kraskaska.nj.bank.routes.dashboard.accounts.*
import com.kraskaska.nj.bank.routes.dashboard.loan.loanIndex
import com.kraskaska.nj.bank.routes.dashboard.loan.newLoan
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId

fun Routing.configureDashboardRouting() {
    authenticate("discord-auth", "session-auth") {
        get("/dashboard", dashboardHome)
        get("/dashboard/accounts", dashboardAccounts)
        get("/dashboard/accounts/new/{type}", newAccount)
        get("/dashboard/accounts/delete/{id}", deleteAccount)
        get("/dashboard/transfer", accountTransfer)
        get("/dashboard/transactions", accountTransactions)
        get("/dashboard/loans", loanIndex)
        get("/dashboard/loans/new", newLoan)
    }
}

val dashboardHome: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if(call.request.queryParameters["set-default-account"] != null) {
        user.defaultAccount = ObjectId(call.request.queryParameters["set-default-account"])
        call.respondRedirect("/dashboard")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            p { +user.name }
            p { +"Баланс: ${"%.2f".format(user.balance / 32.0)} $okaneSymbol" }
            if(user.accounts.toList().size > 1) form {
                p {
                    span {title = "Этот счёт будет конечным для переводов по нику."; +"Основной счёт: "}
                    select {
                        name = "set-default-account";
                        user.accounts.forEach {
                            option {
                                value = it._id.toHexString()
                                if(it._id == user.defaultAccount) selected = true;
                                +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                            }
                        }
                    }
                    submitInput { value = "Сохранить" }
                }
            }
            a(classes = "action", href = "/dashboard/accounts") { +"Счета" }
            a(classes = "action", href = "/dashboard/transfer") { +"Перевод" }
            a(classes = "action", href = "/dashboard/transactions") { +"Транзакции" }
            a(classes = "action", href = "/dashboard/loans") { +"Кредиты" }
        }
    }
}