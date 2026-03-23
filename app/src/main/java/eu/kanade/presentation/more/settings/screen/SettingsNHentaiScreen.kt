package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNHentaiScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.pref_category_nhentai

    @Composable
    override fun getPreferences(): List<Preference> {
        val exhPreferences: ExhPreferences = remember { Injekt.get() }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.pref_category_nhentai),
                preferenceItems = persistentListOf(
                    titleDisplay(exhPreferences),
                    enhancedNhentaiView(exhPreferences),
                ),
            ),
        )
    }

    @Composable
    fun titleDisplay(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            preference = exhPreferences.nhentaiTitleDisplay(),
            title = stringResource(SYMR.strings.nhentai_title_display),
            subtitle = stringResource(SYMR.strings.nhentai_title_display_summary),
            entries = persistentMapOf(
                "full" to stringResource(SYMR.strings.nhentai_title_full),
                "short" to stringResource(SYMR.strings.nhentai_title_short),
            ),
        )
    }

    @Composable
    fun enhancedNhentaiView(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.enhancedNHentaiView(),
            title = stringResource(SYMR.strings.enhanced_nhentai_view),
            subtitle = stringResource(SYMR.strings.enhanced_nhentai_view_summary),
        )
    }
}
