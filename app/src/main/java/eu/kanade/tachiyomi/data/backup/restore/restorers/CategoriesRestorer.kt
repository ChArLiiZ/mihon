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
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            // First pass: restore root categories (no parentName)
            val rootBackups = backupCategories.filter { it.parentName == null }.sortedBy { it.order }
            val subBackups = backupCategories.filter { it.parentName != null }.sortedBy { it.order }

            val restoredCategories = mutableListOf<tachiyomi.domain.category.model.Category>()

            for (backup in rootBackups) {
                val dbCategory = dbCategoriesByName[backup.name]
                if (dbCategory != null) {
                    restoredCategories.add(dbCategory)
                } else {
                    val order = nextOrder++
                    val id = handler.awaitOneExecutable {
                        categoriesQueries.insert(backup.name, order, backup.flags, null)
                        categoriesQueries.selectLastInsertedRowId()
                    }
                    restoredCategories.add(backup.toCategory(id).copy(order = order))
                }
            }

            // Build name -> id mapping for parent lookup
            val allCategoriesByName = (dbCategories + restoredCategories).associateBy { it.name }

            // Second pass: restore subcategories with parent linkage
            for (backup in subBackups) {
                val existing = dbCategoriesByName[backup.name]
                if (existing != null) {
                    // Update parentId to match backup's parent relationship
                    val expectedParentId = backup.parentName?.let { allCategoriesByName[it]?.id }
                    if (existing.parentId != expectedParentId) {
                        handler.await {
                            categoriesQueries.update(
                                name = null,
                                order = null,
                                flags = null,
                                parentId = expectedParentId,
                                categoryId = existing.id,
                            )
                        }
                    }
                    restoredCategories.add(existing.copy(parentId = expectedParentId))
                } else {
                    val parentId = backup.parentName?.let { allCategoriesByName[it]?.id }
                    val order = nextOrder++
                    val id = handler.awaitOneExecutable {
                        categoriesQueries.insert(backup.name, order, backup.flags, parentId)
                        categoriesQueries.selectLastInsertedRowId()
                    }
                    restoredCategories.add(backup.toCategory(id).copy(order = order, parentId = parentId))
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
