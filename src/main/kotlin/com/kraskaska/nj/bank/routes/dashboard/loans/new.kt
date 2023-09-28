package com.kraskaska.nj.bank.routes.dashboard.loans

import com.kraskaska.nj.bank.*
import com.kraskaska.nj.bank.clock.clock
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId
import kotlin.math.roundToLong

val newLoan: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    if (call.request.queryParameters["amount"] != null && call.request.queryParameters["interest"] != null && call.request.queryParameters["duration"] != null && call.request.queryParameters["account"] != null && call.request.queryParameters["account"] != "no-account") {
        val loan = Loan(
            payerAccount = ObjectId(call.request.queryParameters["account"]),
            amount = (call.request.queryParameters["amount"]!!.toDouble() * 32).roundToLong(),
            interest = call.request.queryParameters["interest"]!!.toLong() / 100.0,
            expiryDate = clock.millis() + (call.request.queryParameters["duration"]!!.toLong() * 24 * 60 * 60 * 1000)
        )
        loan.submit()
        call.respondRedirect("/dashboard/loans")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(listOf("Панель управления" to "/dashboard", "Кредиты" to "/dashboard/loans", "Оформить" to "#"))
            h2 { +"Оформить кредит" }
            form {
                p {
                    +"Счёт получения и оплаты:"
                    select {
                        name = "account"
                        if (user.accounts.toList().isEmpty()) {
                            disabled = true
                            option {
                                value = "no-account";
                                +"Нет доступных счетов!"
                            }
                        } else user.accounts.forEach {
                            option {
                                value = it._id.toHexString()
                                +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                            }
                        }
                    }
                }
                p {
                    +"Сумма: "
                    numberInput(name = "amount") {
                        min = "10000"
                        max = "100000"
                        required = true
                    }
                }
                p {
                    +"Процент: "
                    numberInput(name = "interest") {
                        min = "10"
//                        max = "100"
                        step = "any"
                        required = true
                    }
                    +"%"
                }
                p {
                    +"Срок: "
                    numberInput(name = "duration") {
                        min = "7"
                        max = "90"
                        step = "any"
                        required = true
                    }
                    +" дней"
                }
                submitInput { value = "Оформить" }
            }
        }
    }
}