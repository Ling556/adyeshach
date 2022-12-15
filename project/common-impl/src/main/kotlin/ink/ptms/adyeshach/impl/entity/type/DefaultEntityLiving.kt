package ink.ptms.adyeshach.impl.entity.type

import com.google.gson.annotations.Expose
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.ClientEntity
import ink.ptms.adyeshach.core.entity.EntityTypes
import ink.ptms.adyeshach.core.entity.type.AdyEntityLiving
import ink.ptms.adyeshach.impl.DefaultAdyeshachEntityFinder.Companion.clientEntityMap
import ink.ptms.adyeshach.impl.entity.DefaultEquipable
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.NumberConversions
import taboolib.common.platform.function.submit
import java.util.concurrent.ConcurrentHashMap

/**
 * Adyeshach
 * ink.ptms.adyeshach.impl.entity.type.DefaultEntityLiving
 *
 * @author 坏黑
 * @since 2022/6/20 00:44
 */
abstract class DefaultEntityLiving(entityType: EntityTypes) : DefaultEntity(entityType), DefaultEquipable, AdyEntityLiving {

    @Expose
    val equipment = ConcurrentHashMap<EquipmentSlot, ItemStack>()

    @Expose
    var isDie = false

    override fun visible(viewer: Player, visible: Boolean): Boolean {
        return if (visible) {
            prepareSpawn(viewer) {
                // 创建客户端对应表
                clientEntityMap.computeIfAbsent(viewer.name) { ConcurrentHashMap() }[index] = ClientEntity(this)
                // 生成实体
                Adyeshach.api().getMinecraftAPI().getEntitySpawner().spawnEntityLiving(viewer, entityType, index, normalizeUniqueId, position.toLocation())
                // 更新装备
                submit(delay = 1) { updateEquipment() }
                // 更新死亡状态
                submit(delay = 5) {
                    if (isDie) {
                        die(viewer = viewer)
                    }
                }
            }
        } else {
            prepareDestroy(viewer) {
                // 销毁实体
                Adyeshach.api().getMinecraftAPI().getEntityOperator().destroyEntity(viewer, index)
                // 移除客户端对应表
                clientEntityMap[viewer.name]?.remove(index)
            }
        }
    }

    override fun die(die: Boolean) {
        isDie = die
        if (isDie) {
            setHealth(-1f)
            submit(delay = 15) {
                if (isDie) {
                    setHealth(1f)
                }
            }
        } else {
            respawn()
        }
    }

    @Deprecated("不安全的实现，请使用 die(Boolean)")
    override fun die(viewer: Player, die: Boolean) {
        if (die) {
            val healthMeta = getAvailableEntityMeta().firstOrNull { it.key == "health" }!!
            val operator = Adyeshach.api().getMinecraftAPI().getEntityOperator()
            val metadataHandler = Adyeshach.api().getMinecraftAPI().getEntityMetadataHandler()
            operator.updateEntityMetadata(viewer, index, metadataHandler.createFloatMeta(healthMeta.index, NumberConversions.toFloat(-1)))
            submit(delay = 15) {
                operator.updateEntityMetadata(viewer, index, metadataHandler.createFloatMeta(healthMeta.index, NumberConversions.toFloat(1)))
            }
        } else {
            visible(viewer, true)
        }
    }
}