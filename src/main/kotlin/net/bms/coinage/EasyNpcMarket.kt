package net.bms.coinage

import java.util.IdentityHashMap
import java.util.WeakHashMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MerchantContainer
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers

object EasyNpcMarket {
    private const val NPC_MERCHANT = "de.markusbordihn.easynpc.data.trading.SafeMerchantData"
    private val sessions = WeakHashMap<MerchantMenu, Session>()

    private data class Trade(val original: MerchantOffer, val item: String, val units: Int)
    private class Session(val merchant: Merchant, val container: MerchantContainer) {
        var prices = MerchantOffers()
        val originals = IdentityHashMap<MerchantOffer, Trade>()
        var revision = -1L
        var expression: String? = null
        var dirty = true
    }

    @JvmStatic
    fun open(menu: MerchantMenu, merchant: Merchant, container: MerchantContainer) {
        if (merchant.javaClass.name != NPC_MERCHANT) return
        val player = merchant.tradingPlayer as? ServerPlayer ?: return
        val session = Session(merchant, container)
        if (reprice(player, session)) sessions[menu] = session
    }

    @JvmStatic
    fun offers(merchant: Merchant, original: MerchantOffers): MerchantOffers {
        val player = merchant.tradingPlayer as? ServerPlayer ?: return original
        val menu = player.containerMenu as? MerchantMenu
        val session = sessions[menu]
        return if (session?.merchant === merchant) session.prices else original
    }

    @JvmStatic
    fun outgoing(player: ServerPlayer, offers: MerchantOffers): MerchantOffers =
        sessions[player.containerMenu]?.prices ?: offers

    @JvmStatic
    fun stale(player: ServerPlayer): Boolean {
        val session = sessions[player.containerMenu] ?: return false
        val config = CurrencyValues.configuration?.market
        return session.dirty || session.expression != config?.pricePercent ||
            session.revision != (if (config?.enabled == true) market(player, config).shopMarket.revision else -1L)
    }

    @JvmStatic
    fun update(menu: MerchantMenu) {
        val session = sessions[menu] ?: return
        val player = session.merchant.tradingPlayer as? ServerPlayer ?: return
        if (player.containerMenu !== menu || !stale(player)) return
        reprice(player, session)
        session.container.updateSellItem()
        player.sendMerchantOffers(menu.containerId, session.prices, 0, session.merchant.villagerXp,
            session.merchant.showProgressBar(), false)
    }

    @JvmStatic
    fun complete(merchant: Merchant, offer: MerchantOffer) {
        val player = merchant.tradingPlayer as? ServerPlayer
        val session = sessions[player?.containerMenu]
        val trade = session?.originals?.get(offer)
        merchant.notifyTrade(trade?.original ?: offer)
        val config = CurrencyValues.configuration?.market
        if (player == null || session == null || trade == null || config?.enabled != true) return
        val ledger = market(player, config)
        ledger.shopMarket.record(trade.item, trade.units)
        ledger.setDirty()
        session.dirty = true
        offer.setToOutOfStock()
    }

    private fun market(player: ServerPlayer, config: MarketConfig): PurseLedger {
        val server = player.server
        return PurseLedger.get(server).also {
            it.shopMarket.refresh(server.overworld().gameTime, config.resetIntervalSeconds.get(), it::setDirty)
        }
    }

    private fun reprice(player: ServerPlayer, session: Session): Boolean {
        val config = CurrencyValues.configuration?.market
        val market = if (config?.enabled == true) market(player, config).shopMarket else null
        session.prices = MerchantOffers()
        session.originals.clear()
        session.dirty = false
        session.revision = market?.revision ?: -1
        session.expression = config?.pricePercent
        for (offer in session.merchant.offers) {
            val payment = coinageValue(offer.costA)
            val payout = coinageValue(offer.result)
            val buying = payment > 0 && payout == 0L
            val selling = payout > 0 && payment == 0L
            if (market == null || !offer.costB.isEmpty || !offer.shouldRewardExp() || (!buying && !selling)) {
                session.prices.add(offer)
                continue
            }
            val goods = if (buying) offer.result else offer.costA
            val item = BuiltInRegistries.ITEM.getKey(goods.item).toString()
            val base = if (buying) payment else payout
            val price = market.quote(item, base, config?.pricePercent.orEmpty())
            val currency = if (price == base) (if (buying) offer.costA else offer.result).copy() else TradePlatform.currency(price)
            val quoted = MerchantOffer(if (buying) TradePlatform.cost(currency) else offer.itemCostA,
                offer.itemCostB, if (selling) currency else goods.copy(), offer.uses, offer.maxUses, offer.xp,
                if (buying) 0f else offer.priceMultiplier, if (buying) 0 else offer.demand)
            if (selling) quoted.specialPriceDiff = offer.specialPriceDiff
            session.prices.add(quoted)
            session.originals[quoted] = Trade(offer, item, if (buying) goods.count else -goods.count)
        }
        return session.originals.isNotEmpty()
    }

    private fun coinageValue(stack: ItemStack): Long {
        if (stack.item !is CoinItem && stack.item !is MoneyBagItem) return 0
        val value = PurseTransactions.value(stack) ?: return 0
        if (value <= 0) return 0
        val plain = if (stack.item is CoinItem) ItemStack(stack.item, stack.count)
            else MoneyBagItem.create(value).copyWithCount(stack.count)
        return if (ItemStack.isSameItemSameComponents(stack, plain)) value else 0
    }
}
