package com.kraskaska.nj.bank.routes.dashboard.loans

import com.kraskaska.nj.bank.Account
import com.kraskaska.nj.bank.Loan
import com.kraskaska.nj.bank.RouteHandler
import com.kraskaska.nj.bank.clock.clock
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.bson.types.ObjectId
import kotlin.math.roundToLong

val loanPay: RouteHandler = handler@{
    val loan = Loan.get(ObjectId(call.request.queryParameters["loan"]!!))!!
    val toPay = loan.moneyToBePaidAt(clock.millis()) - loan.paidOff
    if (toPay > 0)
        Account.get(loan.payerAccount)!!.transferTo(loan.loanAccount, toPay, "оплата кредита")
    call.respondRedirect("/dashboard/loans")
}
val loanPayoff: RouteHandler = handler@{
    val loan = Loan.get(ObjectId(call.request.queryParameters["loan"]!!))!!
    val toPay = loan.moneyToBePaidAt(clock.millis())
        .coerceAtLeast((loan.amount * (1 + loan.interest)).roundToLong()) - loan.paidOff
    if (toPay > 0)
        Account.get(loan.payerAccount)!!.transferTo(loan.loanAccount, toPay, "погашение кредита")
    call.respondRedirect("/dashboard/loans")
}