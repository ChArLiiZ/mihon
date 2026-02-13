package tachiyomi.domain.category.model

import java.io.Serializable

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
    val parentId: Long? = null,
) : Serializable {

    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID

    val isRootCategory: Boolean = parentId == null

    val isSubCategory: Boolean = parentId != null

    companion object {
        const val UNCATEGORIZED_ID = 0L
    }
}
