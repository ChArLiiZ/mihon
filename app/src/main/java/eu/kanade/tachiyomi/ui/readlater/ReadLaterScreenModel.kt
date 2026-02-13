package eu.kanade.tachiyomi.ui.readlater

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReadLaterScreenModel(
    private val mangaRepository: MangaRepository = Injekt.get(),
) : StateScreenModel<ReadLaterScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            mangaRepository.getReadLaterMangaAsFlow().collect { manga ->
                mutableState.update {
                    State.Success(manga = manga)
                }
            }
        }
    }

    fun removeFromReadLater(mangaId: Long) {
        screenModelScope.launchIO {
            mangaRepository.update(MangaUpdate(id = mangaId, readLater = false))
        }
    }

    sealed interface State {
        data object Loading : State
        data class Success(val manga: List<Manga>) : State
    }
}
