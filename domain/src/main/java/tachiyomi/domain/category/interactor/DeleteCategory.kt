package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

class DeleteCategory(
    private val categoryRepository: CategoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    suspend fun await(categoryId: Long) = withNonCancellableContext {
        // Recursively collect all descendant category IDs before deleting
        val allDescendantIds = mutableListOf<Long>()
        suspend fun collectDescendants(parentId: Long) {
            val children = try {
                categoryRepository.getSubCategories(parentId)
            } catch (_: Exception) {
                emptyList()
            }
            for (child in children) {
                collectDescendants(child.id)
                allDescendantIds.add(child.id)
            }
        }
        collectDescendants(categoryId)

        try {
            // Delete from deepest descendants up to avoid FK issues
            for (descendantId in allDescendantIds.reversed()) {
                categoryRepository.delete(descendantId)
            }
            categoryRepository.delete(categoryId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        val categories = categoryRepository.getAll()
        val updates = categories.mapIndexed { index, category ->
            CategoryUpdate(
                id = category.id,
                order = index.toLong(),
            )
        }

        val defaultCategory = libraryPreferences.defaultCategory().get()
        if (defaultCategory == categoryId.toInt()) {
            libraryPreferences.defaultCategory().delete()
        }

        // Collect all deleted category IDs (parent + all descendants) for preference cleanup
        val deletedIds = buildSet {
            add(categoryId.toString())
            allDescendantIds.forEach { add(it.toString()) }
        }

        val categoryPreferences = listOf(
            libraryPreferences.updateCategories(),
            libraryPreferences.updateCategoriesExclude(),
            downloadPreferences.removeExcludeCategories(),
            downloadPreferences.downloadNewChapterCategories(),
            downloadPreferences.downloadNewChapterCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val staleIds = ids.intersect(deletedIds)
            if (staleIds.isEmpty()) return@forEach
            preference.set(ids - staleIds)
        }

        try {
            categoryRepository.updatePartial(updates)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
