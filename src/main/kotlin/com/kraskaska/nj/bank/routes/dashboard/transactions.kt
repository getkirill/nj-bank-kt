package com.kraskaska.nj.bank.routes.dashboard

import com.kraskaska.nj.bank.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.sessions.*
import kotlinx.html.*

val accountTransactions: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val user = session.toUser()
    val transactions = user.transactions

    val myAddresses = user.myAddresses
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(listOf("Панель управления" to "/dashboard", "Транзакции" to "#"))
            h2 { +"Транзакции" }
            if (transactions.isEmpty()) {
                p { +"У вас нет транзакций." }
            } else {
                ul {
                    transactions.sortedByDescending { it._id.timestamp }.forEach {
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
                                classes = if (myAddresses.contains(it.from?.toHexString()) && myAddresses.contains(
                                        it.to?.toHexString()
                                    )
                                ) "" else if (myAddresses.contains(it.from?.toHexString())) "negative-transaction" else if (myAddresses.contains(
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