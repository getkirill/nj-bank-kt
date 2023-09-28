package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.*
import com.kraskaska.nj.bank.clock.clock
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*

val adminInspection: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val inspectedUser = call.request.queryParameters["uid"]?.toLongOrNull()?.let { User(it) }
    if (inspectedUser == null) {
        call.respond("User not found or invalid or you are dumbass and did not supply uid.")
        return@handler
    }
    val nonEmptyAccounts = inspectedUser.accounts.filter { it.balance > 0 }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Inspection of ${inspectedUser.id}" }
            code {
                pre {
                    +"""
                        admin = ${inspectedUser.isAdmin}
                        balance = ${inspectedUser.balance}
                        defaultAccount = ${inspectedUser.defaultAccount?.toHexString()}
                    """.trimIndent()
                }
            }
            details {
                summary { +"Freeze user" }
                form {
                    p { +"Reason:"; textArea { name = "reason" } }
                    submitInput()
                }
            }
            h2 { +"Accounts" }
            details {
                summary { +"Create new" }
                form {
                    p {
                        +"Type: ";
                        select {
                            name = "type"
                            option {
                                value = "checking"
                                +"checking"
                            }
                            option {
                                value = "savings"
                                +"savings"
                            }
                        }
                    }
                    p {
                        +"Name: "
                        textInput(name = "name")
                    }
                    submitInput()
                }
            }
            details {
                summary { +"Transfer" }
                form {
                    p {
                        +"Счёт отправки: "
                        select {
                            name = "from"
                            if (nonEmptyAccounts.isEmpty()) {
                                disabled = true
                                option {
                                    value = "no-accounts"
                                    +"Нет доступных счетов со средствами!"
                                }
                            }
                            nonEmptyAccounts.forEach {
                                option {
                                    value = it._id.toHexString()
                                    +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                                }
                            }
                        }
                    }
                    p {
                        +"Адрес доставки: "
                        textInput(name = "to") { required = true }
                    }
                    p {
                        +"Размер: "
                        numberInput(name = "amount") {
                            min = "0.03"
                            step = ".01"
                            required = true
                        }
                        +" $okaneSymbol"
                    }
                    p {
                        +"Сообщение (необязательно):"
                        br()
                        textArea { name = "message" }
                    }
                    submitInput { value = "Отправить" }
                }
            }
            details {
                summary { +"Add/remove funds" }
                form {
                    p {
                        select {
                            name = "action"
                            option {
                                value = "deposit"
                                +"Add"
                            }
                            option {
                                value = "withdraw"
                                +"Remove"
                            }
                        }
                        +" funds"
                    }
                    p { +"Количество (Peni, 1/32 okane): "; numberInput(name = "amount") { min = "0" } }
                    submitInput { value = "Добавить" }
                }
            }
            if (inspectedUser.accounts.toList().isEmpty()) {
                p { +"У вас нет счетов." }
            } else {
                ul {
                    inspectedUser.accounts.forEach {
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
            h2 { +"Loans (unpaid)" }
            details {
                summary {
                    +"New loan"
                }
                form {
                    p {
                        +"Счёт получения и оплаты:"
                        select {
                            name = "account"
                            if (inspectedUser.accounts.toList().isEmpty()) {
                                disabled = true
                                option {
                                    value = "no-account";
                                    +"Нет доступных счетов!"
                                }
                            } else inspectedUser.accounts.forEach {
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
            if (inspectedUser.loansTaken.isEmpty()) {
                p { +"Нет непогашенных кредитов." }
            } else {
                ul {
                    li { +"ID кредита - аккаунт получивший кредит - сумма кредита/со ставкой - оплачено/сумма сегодня - ставка" }
                    inspectedUser.loansTaken.filter { !it.isPaidOff }.forEach {
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
            h2 { +"Transactions" }
            if (inspectedUser.transactions.isEmpty()) {
                p { +"У вас нет транзакций." }
            } else {
                ul {
                    inspectedUser.transactions.sortedByDescending { it._id.timestamp }.forEach {
                        li {
                            if (it.from != null) {
                                +"${Account.get(it.from)?.name ?: "[unknown account]"} ("
                                code { +(Account.get(it.from)?._id?.toHexString() ?: "unknown") }
                                +") "
                            }
                            +"->"
                            if (it.to != null) {
                                +"${Account.get(it.to)?.name ?: "[unknown account]"} ("
                                code { +(Account.get(it.to)?._id?.toHexString() ?: "unknown") }
                                +") "
                            }
                            +" - "
                            span(
                                classes = if (inspectedUser.myAddresses.contains(it.from?.toHexString()) && inspectedUser.myAddresses.contains(
                                        it.to?.toHexString()
                                    )
                                ) "" else if (inspectedUser.myAddresses.contains(it.from?.toHexString())) "negative-transaction" else if (inspectedUser.myAddresses.contains(
                                        it.to?.toHexString()
                                    )
                                ) "positive-transaction" else ""
                            ) { +"${"%.2f".format(it.amount / 32.0)} $okaneSymbol" }
                            if (it.message != null) +" - ${it.message}"
                        }
                    }
                }
            }
        }
    }
}