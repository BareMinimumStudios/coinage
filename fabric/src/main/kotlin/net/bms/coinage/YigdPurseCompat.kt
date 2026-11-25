package net.bms.coinage

import java.util.Collections
import java.util.WeakHashMap
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

internal object YigdPurseCompat {
    private val logger = LoggerFactory.getLogger("Coinage/Graves")
    private val captured = Collections.newSetFromMap(WeakHashMap<LivingEntity, Boolean>())

    @JvmStatic
    fun capture(inventory: Any, context: Any) {
        try {
            val player = context.javaClass.getMethod("player").invoke(context) as? ServerPlayer ?: return
            if (player in captured) return
            val added = dropPurseOnDeath(player) { bag ->
                val loader = inventory.javaClass.classLoader
                val ruleType = Class.forName("com.b1n_ry.yigd.util.DropRule", false, loader)
                val graveItemType = Class.forName("com.b1n_ry.yigd.data.GraveItem", false, loader)
                val rule = ruleType.getField("PUT_IN_GRAVE").get(null)
                val graveItem = graveItemType.getConstructor(ItemStack::class.java, ruleType).newInstance(bag, rule)
                @Suppress("UNCHECKED_CAST")
                val items = inventory.javaClass.getMethod("getItems").invoke(inventory) as MutableList<Any>
                items.add(graveItem)
            }
            if (added) captured.add(player)
        } catch (error: ReflectiveOperationException) {
            logger.warn("Could not add the purse bag to You're in Grave Danger", error)
        }
    }

    fun wasCaptured(entity: LivingEntity): Boolean = captured.remove(entity)
}
