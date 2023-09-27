package com.kraskaska.nj.bank.routes.dashboard.loan

import com.kraskaska.nj.bank.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.sessions.*
import kotlinx.html.*

val loanIndex: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(listOf("Панель управления" to "/dashboard", "Кредиты" to "#"))
            h2 { +"Кредиты" }
            a(href = "/dashboard/loans/new", classes = "action") { +"Оформить кредит" }
            if (user.loansTaken.isEmpty()) {
                p { +"Нет непогашенных кредитов." }
            } else {
                ul {
                    li {+"ID кредита - аккаунт получивший кредит - сумма кредита - оплачено/сумма сегодня - ставка"}
                    user.loansTaken.filter { !it.isPaidOff }.forEach {
                        li { +"${it._id.toHexString()} - ${Account.get(it.payerAccount)!!.name} ("; code{+it.payerAccount.toHexString() }; +") - ${"%.2f".format(it.amount / 32.0)} $okaneSymbol - ${"%.2f".format(Account.get(it.loanAccount)!!.balance / 32.0)}/${"%.2f".format(it.moneyToBePaidAt(System.currentTimeMillis()) / 32.0)} $okaneSymbol - ставка ${"%.1f".format(it.interest*100)}%" }
                    }
                }
            }
        }
    }
}