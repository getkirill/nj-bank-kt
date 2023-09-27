import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

val mongoClient =
    MongoClient.create(env["MONGODB_URL"]).getDatabase(env["MONGODB_NAME"]).apply {
        runBlocking {
            createCollection("accounts")
            createCollection("transactions")
            createCollection("users")
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

    fun transferTo(id: ObjectId, amount: CurrencyPeni, message: String? = null) {
        if (amount <= 0) throw Exception("Can't transfer less or zero")
        if (amount > balance) throw Exception("Can't make money out of thin air")
        runBlocking { mongoClient.getCollection<Transaction>("transactions").insertOne(Transaction(_id, id, amount, message)) }
    }

    fun deposit(amount: CurrencyPeni, message: (Transaction) -> String = {"пополнение счёта"}) {
        val t = Transaction(to = _id, amount = amount).run { copy(message = message(this)) }
        runBlocking {
            mongoClient.getCollection<Transaction>("transactions").insertOne(t)
        }
    }

    fun withdraw(amount: CurrencyPeni) {
        runBlocking {
            mongoClient.getCollection<Transaction>("transactions").insertOne(Transaction(from = _id, amount = amount, message = "обналичивание средств"))
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

data class Transaction(val from: ObjectId? = null, val to: ObjectId? = null, val amount: CurrencyPeni, val message: String? = null, val _id: ObjectId = ObjectId.get())

fun Iterable<Transaction>.balance(of: ObjectId): CurrencyPeni =
    fold(0L) { acc, transaction -> if(transaction.from == transaction.to) acc else if (transaction.from == of) acc - transaction.amount else if (transaction.to == of) acc + transaction.amount else acc }