package com.kraskaska.nj.bank.routes.dashboard.loans

import com.kraskaska.nj.bank.*
import com.kraskaska.nj.bank.clock.clock
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
                    li { +"ID кредита - аккаунт получивший кредит - сумма кредита/со ставкой - оплачено/сумма сегодня - ставка" }
                    user.loansTaken.filter { !it.isPaidOff }.forEach {
                        li {
                            +"${it._id.toHexString()} - ${Account.get(it.payerAccount)!!.name} ("; code { +it.payerAccount.toHexString() }; +") - ${
                            "%.2f".format(
                                it.amount / 32.0
                            )
                        }/${"%.2f".format(it.amount * (1 + it.interest) / 32.0)} $okaneSymbol - ${
                            "%.2f".format(
                                Account.get(
                                    it.loanAccount
                                )!!.balance / 32.0
                            )
                        }/${"%.2f".format(it.moneyToBePaidAt(clock.millis()) / 32.0)} $okaneSymbol - ставка ${
                            "%.1f".format(
                                it.interest * 100
                            )
                        }% ("
                            a(href = "/dashboard/loans/pay?loan=${it._id.toHexString()}") { +"Оплатить" }
                            +" "
                            a(href = "/dashboard/loans/payoff?loan=${it._id.toHexString()}") { +"Погасить" }
                            +")"
                        }
                    }
                }
            }
        }
    }
}