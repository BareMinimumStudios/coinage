package net.bms.coinage

import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.UUID
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory

internal object PurseMigration {
    private val logger = LoggerFactory.getLogger("Coinage/Migration")
    private const val MAX_NBT_BYTES = 16L * 1024 * 1024
    private const val LEGACY_CURRENCY = "numismatic-overhaul:currency"

    fun importSavedBalances(server: MinecraftServer) {
        val ledger = PurseLedger.get(server)
        var imported = 0
        val legacyFile = server.getWorldPath(LevelResource.DATA).resolve("coinage.dat")
        val oldLedger = read(legacyFile)?.getCompoundOrEmpty("data")
        val oldBalances = oldLedger?.getCompoundOrEmpty("balances")
        val oldImports = oldLedger?.getCompoundOrEmpty("legacyImports")
        if (oldImports != null) for (key in oldImports.keySet()) {
            val id = runCatching { UUID.fromString(key) }.getOrNull() ?: continue
            if (oldImports.getBooleanOr(key, false) && ledger.importLegacy(id, oldBalances?.getLongOr(key, 0)?.coerceAtLeast(0) ?: 0)) imported++
        }
        if (oldBalances != null) for (key in oldBalances.keySet()) {
            val id = runCatching { UUID.fromString(key) }.getOrNull() ?: continue
            val amount = oldBalances.getLongOr(key, -1)
            if (amount >= 0 && ledger.importLegacy(id, amount)) imported++
        }
        val playerDirectories = listOf(server.getWorldPath(LevelResource.PLAYER_DATA_DIR),
            server.getWorldPath(LevelResource.ROOT).resolve("playerdata")).distinct()
        for (playerDirectory in playerDirectories) {
            if (Files.isDirectory(playerDirectory)) try {
                Files.newDirectoryStream(playerDirectory, "*.dat").use { files ->
                    for (file in files) {
                        val id = runCatching { UUID.fromString(file.fileName.toString().removeSuffix(".dat")) }.getOrNull() ?: continue
                        if (!id.toString().equals(file.fileName.toString().removeSuffix(".dat"), ignoreCase = true)) continue
                        if (ledger.hasLegacyImport(id) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue
                        val player = read(file) ?: continue
                        val amount = legacyValue(player) ?: continue
                        if (ledger.importLegacy(id, amount)) imported++
                    }
                }
            } catch (exception: IOException) {
                logger.warn("Could not inspect legacy player directory: {}", exception.message)
            }
        }
        // Integrated servers keep their owner's data inside level.dat.
        val levelFile = server.getWorldPath(LevelResource.LEVEL_DATA_FILE)
        val owner = read(levelFile)?.getCompoundOrEmpty("Data")?.getCompoundOrEmpty("Player")
        if (owner != null) {
            val uuid = owner.getIntArray("UUID").orElse(intArrayOf())
            if (uuid.size == 4) {
                val id = UUID((uuid[0].toLong() shl 32) or (uuid[1].toLong() and 0xffffffffL),
                    (uuid[2].toLong() shl 32) or (uuid[3].toLong() and 0xffffffffL))
                val amount = legacyValue(owner)
                if (amount != null && ledger.importLegacy(id, amount)) imported++
            }
        }
        if (imported > 0) logger.info("Imported {} legacy purse records; source files were preserved", imported)
    }

    private fun legacyValue(player: CompoundTag): Long? {
        val components = player.getCompoundOrEmpty("cardinal_components")
        if (!components.contains(LEGACY_CURRENCY)) return null
        val currency = components.getCompoundOrEmpty(LEGACY_CURRENCY)
        if (!currency.contains("Value")) return null
        return currency.getLongOr("Value", -1).takeIf { it >= 0 }
    }

    private fun read(path: Path): CompoundTag? {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return null
        return try {
            NbtIo.readCompressed(path, NbtAccounter.create(MAX_NBT_BYTES))
        } catch (exception: Exception) {
            logger.warn("Could not inspect legacy purse data in {}: {}", path.fileName, exception.message)
            null
        }
    }
}
