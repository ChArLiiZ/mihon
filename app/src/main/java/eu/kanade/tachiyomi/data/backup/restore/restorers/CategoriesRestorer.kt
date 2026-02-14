package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) {

    suspend operator fun invoke(backupCategories: List<BackupCategory>) {
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getCategories.await()
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            // Build composite-key map: (name, parentName) -> Category
            // This correctly distinguishes subcategories with the same name under different parents
            val dbCategoriesById = dbCategories.associateBy { it.id }
            val dbCategoriesByIdentity = dbCategories.associateBy { cat ->
                val parentName = cat.parentId?.let { dbCategoriesById[it]?.name }
                Pair(cat.name, parentName)
            }

            // First pass: restore root categories (no parentName)
            val rootBackups = backupCategories.filter { it.parentName == null }.sortedBy { it.order }
            val subBackups = backupCategories.filter { it.parentName != null }.sortedBy { it.order }

            val restoredCategories = mutableListOf<tachiyomi.domain.category.model.Category>()

            for (backup in rootBackups) {
                val dbCategory = dbCategoriesByIdentity[Pair(backup.name, null)]
                if (dbCategory != null) {
                    restoredCategories.add(dbCategory)
                } else {
                    val order = nextOrder++
                    val id = handler.awaitOneExecutable {
                        categoriesQueries.insert(backup.name, order, backup.flags, null)
                        categoriesQueries.selectLastInsertedRowId()
                    }
                    restoredCategories.add(backup.toCategory(id, parentId = null).copy(order = order))
                }
            }

            // Build parent lookup from all known categories, preferring root categories
            val allCategories = (dbCategories + restoredCategories).distinctBy { it.id }
            val parentLookup = buildMap<String, tachiyomi.domain.category.model.Category> {
                allCategories.forEach { cat ->
                    val existing = get(cat.name)
                    if (existing == null || (cat.isRootCategory && !existing.isRootCategory)) {
                        put(cat.name, cat)
                    }
                }
            }

            // Second pass: restore subcategories with parent linkage
            for (backup in subBackups) {
                val parentId = backup.parentName?.let { parentLookup[it]?.id }
                val existing = dbCategoriesByIdentity[Pair(backup.name, backup.parentName)]
                if (existing != null) {
                    // Exact match: update parentId if it doesn't match
                    if (existing.parentId != parentId && parentId != null) {
                        handler.await {
                            categoriesQueries.updateParentId(parentId, existing.id)
                        }
                    }
                    restoredCategories.add(existing.copy(parentId = parentId ?: existing.parentId))
                } else {
                    // Fallback: find subcategory by name when parent was renamed.
                    // Only use if there's exactly one match — multiple means ambiguous, so create new.
                    val fallback = dbCategories.filter { it.name == backup.name && it.isSubCategory }.singleOrNull()
                    if (fallback != null) {
                        // Preserve existing parent relationship — the parent was likely just renamed
                        restoredCategories.add(fallback)
                    } else {
                        val order = nextOrder++
                        val id = handler.awaitOneExecutable {
                            categoriesQueries.insert(backup.name, order, backup.flags, parentId)
                            categoriesQueries.selectLastInsertedRowId()
                        }
                        restoredCategories.add(backup.toCategory(id, parentId = parentId).copy(order = order))
                    }
                }
            }

            libraryPreferences.categorizedDisplaySettings().set(
                (dbCategories + restoredCategories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }
}
