package net.bms.coinage

internal object ClientPurseState {
    var balance = 0L
        private set
    var rates = ExchangeRates()
        private set
    var ready = false
        private set
    var revision = 0L
        private set

    fun accept(snapshot: PursePayloads.Snapshot) {
        if (snapshot.balance < 0) return
        balance = snapshot.balance
        rates = snapshot.rates
        ready = true
        revision++
    }

    fun reset() {
        balance = 0
        rates = ExchangeRates()
        ready = false
        revision++
    }
}
