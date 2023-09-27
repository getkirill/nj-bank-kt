package com.kraskaska.nj.bank

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import kotlin.math.roundToLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val mongoClient =
    MongoClient.create(env["MONGODB_URL"]).getDatabase(env["MONGODB_NAME"]).apply {
        runBlocking {
            createCollection("accounts")
            createCollection("transactions")
            createCollection("users")
            createCollection("loans")
        }
    }

data class Account(val ownerId: Long, val type: AccountType, val name: String, val _id: ObjectId = ObjectId.get()) {
    enum class AccountType {
        CHECKING,
        SAVINGS;

        override fun toString(): String {
            return super.toString().lowercase()
        }
    }

    val transactions: Iterable<Transaction>
        get() = runBlocking {
            mongoClient.getCollection<Transaction>("transactions")
                .find(Filters.or(Filters.eq("from", _id), Filters.eq("to", _id))).toList()
        }

    val balance = transactions.apply { println(transactions) }.balance(_id)

    fun transferTo(id: ObjectId, amount: CurrencyPeni, message: String? = null, force: Boolean = false) {
        if (amount <= 0 && !force) throw Exception("Can't transfer less or zero")
        if (amount > balance && !force) throw Exception("Can't make money out of thin air")
        runBlocking {
            mongoClient.getCollection<Transaction>("transactions").insertOne(Transaction(_id, id, amount, message))
        }
    }

    fun deposit(amount: CurrencyPeni, message: (Transaction) -> String = { "пополнение счёта" }) {
        val t = Transaction(to = _id, amount = amount).run { copy(message = message(this)) }
        runBlocking {
            mongoClient.getCollection<Transaction>("transactions").insertOne(t)
        }
    }

    fun withdraw(amount: CurrencyPeni) {
        runBlocking {
            mongoClient.getCollection<Transaction>("transactions")
                .insertOne(Transaction(from = _id, amount = amount, message = "обналичивание средств"))
        }
    }

    fun delete() {
        runBlocking { mongoClient.getCollection<Transaction>("accounts").deleteOne(Filters.eq("_id", _id)) }
    }

    companion object {
        fun get(id: ObjectId) =
            runBlocking { mongoClient.getCollection<Account>("accounts").find(Filters.eq("_id", id)).singleOrNull() }

        fun byOwner(ownerId: Long): Iterable<Account> =
            runBlocking { mongoClient.getCollection<Account>("accounts").find(Filters.eq("ownerId", ownerId)).toList() }

        fun create(ownerId: Long, type: AccountType, name: String) = runBlocking {
            mongoClient.getCollection<Account>("accounts").insertOne(Account(ownerId, type, name))
        }
    }
}
typealias CurrencyPeni = Long

//fun CurrencyPeni.toOkane(): Double {
//    return this / 32.0
//}

val okaneSymbol: String = "お金"

data class Transaction(
    val from: ObjectId? = null,
    val to: ObjectId? = null,
    val amount: CurrencyPeni,
    val message: String? = null,
    val _id: ObjectId = ObjectId.get()
)

fun Iterable<Transaction>.balance(of: ObjectId): CurrencyPeni =
    fold(0L) { acc, transaction -> if (transaction.from == transaction.to) acc else if (transaction.from == of) acc - transaction.amount else if (transaction.to == of) acc + transaction.amount else acc }

data class Loan(
    val _id: ObjectId = ObjectId.get(),
    val payerAccount: ObjectId,
    val loanAccount: ObjectId = Account.create(
        -1,
        Account.AccountType.CHECKING,
        "loan:${_id.toHexString()}"
    ).insertedId!!.asObjectId().value,
    val amount: CurrencyPeni,
    val interest: Double,
    val expiryDate: Long,
    val expiredInterest: Double = interest*interest,
) {
    fun moneyToBePaidAt(timestamp: Long): Long {
        return when {
            timestamp <= expiryDate -> (amount * interest * ((timestamp - _id.date.time).toDuration(DurationUnit.MILLISECONDS).inWholeDays / (expiryDate - _id.date.time).toDuration(
                DurationUnit.MILLISECONDS
            ).inWholeDays)).roundToLong()
            timestamp > expiryDate -> (amount * interest + (amount * interest * (Math.pow(
                expiredInterest,
                (timestamp - expiryDate).toDuration(DurationUnit.MILLISECONDS).inWholeDays.toDouble()
            )))).roundToLong()
            else -> TODO()
        }
    }

    val paidOffAt: Long? get() {
        val acc = Account.get(loanAccount)!!
        val lastTransaction = acc.transactions.maxByOrNull { it._id.date.time } ?: return null
        val balance = acc.balance
        return if(balance < moneyToBePaidAt(lastTransaction._id.date.time) || balance < amount * interest) {
            null
        } else {
            lastTransaction._id.date.time
        }
    }
    val isPaidOff get() = paidOffAt != null

    companion object {
        fun allLinkedTo(accountId: ObjectId): Iterable<Loan> = runBlocking {
            mongoClient.getCollection<Loan>("loans").find(Filters.eq("payerAccount", accountId)).toList()
        }
    }

    fun submit() = runBlocking {
        Account.get(payerAccount)!!.deposit(amount) {"пополнение кредита"}
        mongoClient.getCollection<Loan>("loans").insertOne(this@Loan)
    }
}