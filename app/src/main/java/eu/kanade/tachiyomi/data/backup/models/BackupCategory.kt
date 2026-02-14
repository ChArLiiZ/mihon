package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.category.model.Category

@Serializable
class BackupCategory(
    @ProtoNumber(1) var name: String,
    @ProtoNumber(2) var order: Long = 0,
    @ProtoNumber(3) var id: Long = 0,
    // @ProtoNumber(3) val updateInterval: Int = 0, 1.x value not used in 0.x
    @ProtoNumber(100) var flags: Long = 0,
    @ProtoNumber(101) var parentName: String? = null,
) {
    fun toCategory(id: Long, parentId: Long? = null) = Category(
        id = id,
        name = this@BackupCategory.name,
        flags = this@BackupCategory.flags,
        order = this@BackupCategory.order,
        parentId = parentId,
    )
}

val backupCategoryMapper = { category: Category, allCategories: List<Category> ->
    val parentCategory = category.parentId?.let { pid -> allCategories.find { it.id == pid } }
    BackupCategory(
        id = category.id,
        name = category.name,
        order = category.order,
        flags = category.flags,
        parentName = parentCategory?.name,
    )
}
