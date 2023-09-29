package com.kraskaska.nj.bank.routes.admin

import com.kraskaska.nj.bank.*
import com.kraskaska.nj.bank.clock.clock
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.bson.types.ObjectId
import kotlin.math.roundToLong

val adminInspection: RouteHandler = handler@{
    fun FORM.uid(data: String) {
        hiddenInput(name = "uid") { value = data }
    }

    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val inspectedUser = call.request.queryParameters["uid"]?.toLongOrNull()?.let { BankUser(it) }
    if (inspectedUser == null) {
        call.respond("User not found or invalid or you are dumbass and did not supply uid.")
        return@handler
    }
    val nonEmptyAccounts = inspectedUser.accounts.filter { it.balance > 0 }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Inspection of ${inspectedUser.id}" }
            form(action = "/admin/inspection/set-user-props") {
                uid(inspectedUser.id.toString())
                p {
                    checkBoxInput(name = "admin") { checked = inspectedUser.isAdmin }
                    +" Admin"
                }
                if (inspectedUser.accounts.toList().size > 1) {
                    p {
                        span { title = "Этот счёт будет конечным для переводов по нику."; +"Default: " }
                        select {
                            name = "default-account";
                            inspectedUser.accounts.forEach {
                                option {
                                    value = it._id.toHexString()
                                    if (it._id == inspectedUser.defaultAccount) selected = true;
                                    +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                                }
                            }
                        }
                    }
                }
                submitInput { value = "Save" }
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
                form(action = "/admin/inspection/new-account") {
                    uid(inspectedUser.id.toString())
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
                form(action = "/admin/inspection/transfer") {
                    uid(call.request.queryParameters["uid"]!!)
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
                form(action = "/admin/inspection/change-funds") {
                    uid(call.request.queryParameters["uid"]!!)
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
                    p {
                        +"Account: "
                        select {
                            name = "to"
                            if (inspectedUser.accounts.toList().isEmpty()) {
                                disabled = true
                                option {
                                    value = "no-accounts"
                                    +"Нет доступных счетов!"
                                }
                            }
                            inspectedUser.accounts.forEach {
                                option {
                                    value = it._id.toHexString()
                                    +"${it.name} - ${it._id.toHexString()} - ${"%.2f".format(it.balance / 32.0)} $okaneSymbol"
                                }
                            }
                        }
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
                    submitInput()
                }
            }
            details {
                summary { +"Modify account" }
                form(action = "/admin/inspection/modify-account") {
                    uid(call.request.queryParameters["uid"]!!)
                    p {
                        +"Account: "
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
                    p { +"New name: "; textInput(name = "name") { placeholder = "(unmodified)" } }
                    p {
                        +"New type: "
                        select {
                            name = "type"
                            option() {
                                value = "unmodified"
                                selected = true
                                +"(unmodified)"
                            }
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
                    submitInput()
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
                                +"(";a(href = "/admin/inspection/delete-account?id=${it._id.toHexString()}&uid=${call.request.queryParameters["uid"]!!}") { +"Удалить" };/*+" ";a(
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
                form(action = "/admin/inspection/new-loan") {
                    uid(call.request.queryParameters["uid"]!!)
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
                            min = "1"
//                            max = "100000"
                            step = "any"
                            required = true
                        }
                    }
                    p {
                        +"Процент: "
                        numberInput(name = "interest") {
                            min = "1"
//                        max = "100"
                            step = "any"
                            required = true
                        }
                        +"%"
                    }
                    p {
                        +"Срок: "
                        numberInput(name = "duration") {
                            min = "1"
//                            max = "90"
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
                            +" ("
                            a { +"Revert" }
                            +" "
                            a { +"Delete" }
                        }
                    }
                }
            }
        }
    }
}

// /admin/inspection/set-user-props
val inspectionSetUserProps: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val inspectedUser = BankUser(call.request.queryParameters["uid"]!!.toLong())
    inspectedUser.isAdmin = call.request.queryParameters["admin"] == "on"
    inspectedUser.defaultAccount = ObjectId(call.request.queryParameters["default-account"]!!)
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
}

// /admin/inspection/new-account
val inspectionNewAccount: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val inspectedUser = BankUser(call.request.queryParameters["uid"]!!.toLong())
    inspectedUser.createAccount(
        mapOf(
            "savings" to Account.AccountType.SAVINGS,
            "checking" to Account.AccountType.CHECKING
        )[call.request.queryParameters["type"]!!]!!, call.request.queryParameters["name"]!!
    )
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
}

// /admin/inspection/delete-account
val inspectionDeleteAccount: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
//    val inspectedUser = User(call.request.queryParameters["uid"]!!.toLong())
    val account = Account.get(ObjectId(call.parameters["id"]!!))!!
    if (call.request.queryParameters.contains("confirm")) {
        account.delete()
        call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
        return@handler
    }
    call.respondHtmlTemplate(DefaultTemplate()) {
        content {
            h1 { +"Первый новояпонский банк" }
            crumbBar(
                listOf(
                    "Панель управления" to "/dashboard",
                    "Счета" to "/dashboard/accounts",
                    "Удалить счет" to "#"
                )
            )
            h2 {
                +"Удаление счета ${account.name} ("; code { +account._id.toHexString() };+")"
            }
            p { +"Вы уверены, что хотите удалить счёт?" }
            p(classes = "danger") { +"Все средства, оставшиеся на счету, будут утеряны!" }
            (List(9) { "Нет" } + listOf("Да")).shuffled().map {
                a(
                    classes = "action",
                    href = mapOf(
                        "Нет" to "/admin/inspection?uid=${call.parameters["uid"]!!}",
                        "Да" to "/admin/inspection/delete-account?id=${call.parameters["id"]!!}&uid=${call.parameters["uid"]!!}&confirm"
                    )[it]
                ) { +it }
            }
        }
    }
}

// /admin/inspection/transfer
val inspectionAccountTransfer: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val from = Account.get(ObjectId(call.request.queryParameters["from"]!!))!!
    val to = ObjectId(call.request.queryParameters["to"]!!)
    val amount = (call.request.queryParameters["amount"]!!.toDouble() * 32).roundToLong()
    val message = call.request.queryParameters["message"]
    from.transferTo(to, amount, message)
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
//    val inspectedUser = User(call.request.queryParameters["uid"]!!.toLong())
}

// /admin/inspection/change-funds
val inspectionChangeFunds: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val to = Account.get(ObjectId(call.request.queryParameters["to"]!!))!!
    val amount = (call.request.queryParameters["amount"]!!.toDouble() * 32).roundToLong()
    val action = call.request.queryParameters["action"]!!
    when (action) {
        "deposit" -> to.deposit(amount)
        "withdraw" -> to.withdraw(amount)
        else -> TODO()
    }
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
}

// /admin/inspection/modify-account
val inspectionModifyAccount: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val account = Account.get(ObjectId(call.request.queryParameters["account"]!!))!!
    val name = call.request.queryParameters["name"]!!.ifBlank { null } ?: account.name
    val type = call.request.queryParameters["type"]!!.run { if (this == "unmodified") null else this }
        ?: account.type.toString()
    account.modify(
        name,
        mapOf("checking" to Account.AccountType.CHECKING, "savings" to Account.AccountType.SAVINGS)[type]!!
    )
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
}

// /admin/inspection/new-loan
val inspectionNewLoan: RouteHandler = handler@{
    val session = call.sessions.get<DiscordSession>()!!
    val adminUser = session.toBankUser()
    if (!adminUser.isAdmin) {
        call.respondRedirect("/admin")
        return@handler
    }
    val loan = Loan(
        payerAccount = ObjectId(call.request.queryParameters["account"]!!),
        amount = (call.request.queryParameters["amount"]!!.toDouble() * 32).roundToLong(),
        interest = call.request.queryParameters["interest"]!!.toLong() / 100.0,
        expiryDate = clock.millis() + (call.request.queryParameters["duration"]!!.toLong() * 24 * 60 * 60 * 1000)
    )
    loan.submit()
    call.respondRedirect("/admin/inspection?uid=${call.request.queryParameters["uid"]!!}")
    return@handler
}