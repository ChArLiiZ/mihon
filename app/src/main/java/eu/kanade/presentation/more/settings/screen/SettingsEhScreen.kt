package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.toast
import exh.source.ExhPreferences
import exh.ui.login.EhLoginActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext

import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

object SettingsEhScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.pref_category_eh



    @Composable
    fun Reconfigure(
        exhPreferences: ExhPreferences,
        openWarnConfigureDialogController: () -> Unit,
    ) {
        var initialLoadGuard by remember { mutableStateOf(false) }
        val useHentaiAtHome by exhPreferences.useHentaiAtHome().collectAsState()
        val useJapaneseTitle by exhPreferences.useJapaneseTitle().collectAsState()
        val useOriginalImages by exhPreferences.exhUseOriginalImages().collectAsState()
        val ehTagFilterValue by exhPreferences.ehTagFilterValue().collectAsState()
        val ehTagWatchingValue by exhPreferences.ehTagWatchingValue().collectAsState()
        val settingsLanguages by exhPreferences.exhSettingsLanguages().collectAsState()
        val enabledCategories by exhPreferences.exhEnabledCategories().collectAsState()
        val imageQuality by exhPreferences.imageQuality().collectAsState()
        DisposableEffect(
            useHentaiAtHome,
            useJapaneseTitle,
            useOriginalImages,
            ehTagFilterValue,
            ehTagWatchingValue,
            settingsLanguages,
            enabledCategories,
            imageQuality,
        ) {
            if (initialLoadGuard) {
                openWarnConfigureDialogController()
            }
            initialLoadGuard = true
            onDispose {}
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val exhPreferences: ExhPreferences = remember { Injekt.get() }

        val exhentaiEnabled by exhPreferences.enableExhentai().collectAsState()
        var runConfigureDialog by remember { mutableStateOf(false) }
        val openWarnConfigureDialogController = { runConfigureDialog = true }

        Reconfigure(exhPreferences, openWarnConfigureDialogController)

        ConfigureExhDialog(run = runConfigureDialog, onRunning = { runConfigureDialog = false })

        return listOf(
            Preference.PreferenceGroup(
                stringResource(SYMR.strings.ehentai_prefs_account_settings),
                preferenceItems = persistentListOf(
                    getLoginPreference(exhPreferences, openWarnConfigureDialogController),
                    useHentaiAtHome(exhentaiEnabled, exhPreferences),
                    useJapaneseTitle(exhentaiEnabled, exhPreferences),
                    useOriginalImages(exhentaiEnabled, exhPreferences),
                    watchedTags(exhentaiEnabled),
                    tagFilterThreshold(exhentaiEnabled, exhPreferences),
                    tagWatchingThreshold(exhentaiEnabled, exhPreferences),
                    settingsLanguages(exhentaiEnabled, exhPreferences),
                    enabledCategories(exhentaiEnabled, exhPreferences),
                    watchedListDefaultState(exhentaiEnabled, exhPreferences),
                    imageQuality(exhentaiEnabled, exhPreferences),
                    enhancedEhentaiView(exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                stringResource(SYMR.strings.favorites_sync),
                preferenceItems = persistentListOf(
                    readOnlySync(exhPreferences),
                    // syncFavoriteNotes(),
                    lenientSync(exhPreferences),
                    // forceSyncReset(deleteFavoriteEntries),
                ),
            ),
        )
    }

    @Composable
    fun getLoginPreference(
        exhPreferences: ExhPreferences,
        openWarnConfigureDialogController: () -> Unit,
    ): Preference.PreferenceItem.SwitchPreference {
        val activityResultContract =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    // Upload settings
                    openWarnConfigureDialogController()
                }
            }
        val context = LocalContext.current
        val value by exhPreferences.enableExhentai().collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.enableExhentai(),
            title = stringResource(SYMR.strings.enable_exhentai),
            subtitle = if (!value) {
                stringResource(SYMR.strings.requires_login)
            } else {
                null
            },
            onValueChanged = { newVal ->
                if (!newVal) {
                    exhPreferences.enableExhentai().set(false)
                    true
                } else {
                    activityResultContract.launch(EhLoginActivity.newIntent(context))
                    false
                }
            },
        )
    }

    @Composable
    fun useHentaiAtHome(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<Int> {
        return Preference.PreferenceItem.ListPreference(
            preference = exhPreferences.useHentaiAtHome(),
            title = stringResource(SYMR.strings.use_hentai_at_home),
            subtitle = stringResource(SYMR.strings.use_hentai_at_home_summary),
            entries = persistentMapOf(
                0 to stringResource(SYMR.strings.use_hentai_at_home_option_1),
                1 to stringResource(SYMR.strings.use_hentai_at_home_option_2),
            ),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun useJapaneseTitle(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        val value by exhPreferences.useJapaneseTitle().collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.useJapaneseTitle(),
            title = stringResource(SYMR.strings.show_japanese_titles),
            subtitle = if (value) {
                stringResource(SYMR.strings.show_japanese_titles_option_1)
            } else {
                stringResource(SYMR.strings.show_japanese_titles_option_2)
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun useOriginalImages(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        val value by exhPreferences.exhUseOriginalImages().collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.exhUseOriginalImages(),
            title = stringResource(SYMR.strings.use_original_images),
            subtitle = if (value) {
                stringResource(SYMR.strings.use_original_images_on)
            } else {
                stringResource(SYMR.strings.use_original_images_off)
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun watchedTags(exhentaiEnabled: Boolean): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        val title = stringResource(SYMR.strings.watched_tags_exh)
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.watched_tags),
            subtitle = stringResource(SYMR.strings.watched_tags_summary),
            onClick = {
                context.startActivity(
                    WebViewActivity.newIntent(
                        context,
                        url = "https://exhentai.org/mytags",
                        title = title,
                    ),
                    null,
                )
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun TagThresholdDialog(
        onDismissRequest: () -> Unit,
        title: String,
        initialValue: Int,
        valueRange: IntRange,
        outsideRangeError: String,
        onValueChange: (Int) -> Unit,
    ) {
        var value by remember(initialValue) {
            mutableStateOf(initialValue.toString())
        }
        val isValid = remember(value) { value.toIntOrNull().let { it != null && it in valueRange } }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = { onValueChange(value.toIntOrNull() ?: return@TextButton) },
                    enabled = isValid,
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            title = {
                Text(text = title)
            },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        maxLines = 1,
                        singleLine = true,
                        isError = !isValid,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (!isValid) {
                            { Icon(Icons.Outlined.Error, outsideRangeError) }
                        } else {
                            null
                        },
                    )
                    if (!isValid) {
                        Text(
                            text = outsideRangeError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            },
        )
    }

    @Composable
    fun tagFilterThreshold(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.ehTagFilterValue().collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            TagThresholdDialog(
                onDismissRequest = { dialogOpen = false },
                title = stringResource(SYMR.strings.tag_filtering_threshold),
                initialValue = value,
                valueRange = -9999..0,
                outsideRangeError = stringResource(SYMR.strings.tag_filtering_threshhold_error),
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.ehTagFilterValue().set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.tag_filtering_threshold),
            subtitle = stringResource(SYMR.strings.tag_filtering_threshhold_summary, value),
            onClick = {
                dialogOpen = true
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun tagWatchingThreshold(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.ehTagWatchingValue().collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            TagThresholdDialog(
                onDismissRequest = { dialogOpen = false },
                title = stringResource(SYMR.strings.tag_watching_threshhold),
                initialValue = value,
                valueRange = 0..9999,
                outsideRangeError = stringResource(SYMR.strings.tag_watching_threshhold_error),
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.ehTagWatchingValue().set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.tag_watching_threshhold),
            subtitle = stringResource(SYMR.strings.tag_watching_threshhold_summary, value),
            onClick = {
                dialogOpen = true
            },
            enabled = exhentaiEnabled,
        )
    }

    class LanguageDialogState(preference: String) {
        class RowState(original: ColumnState, translated: ColumnState, rewrite: ColumnState) {
            var original by mutableStateOf(original)
            var translated by mutableStateOf(translated)
            var rewrite by mutableStateOf(rewrite)

            fun toPreference() = "${original.value}*${translated.value}*${rewrite.value}"
        }
        enum class ColumnState(val value: String) {
            Unavailable("false"),
            Enabled("true"),
            Disabled("false"),
        }
        private fun String.toRowState(disableFirst: Boolean = false) = split("*")
            .map {
                if (it.toBoolean()) {
                    ColumnState.Enabled
                } else {
                    ColumnState.Disabled
                }
            }
            .let {
                if (disableFirst) {
                    RowState(ColumnState.Unavailable, it[1], it[2])
                } else {
                    RowState(it[0], it[1], it[2])
                }
            }

        val japanese: RowState
        val english: RowState
        val chinese: RowState
        val dutch: RowState
        val french: RowState
        val german: RowState
        val hungarian: RowState
        val italian: RowState
        val korean: RowState
        val polish: RowState
        val portuguese: RowState
        val russian: RowState
        val spanish: RowState
        val thai: RowState
        val vietnamese: RowState
        val notAvailable: RowState
        val other: RowState

        init {
            val settingsLanguages = preference.split("\n")
            japanese = settingsLanguages[0].toRowState(true)
            english = settingsLanguages[1].toRowState()
            chinese = settingsLanguages[2].toRowState()
            dutch = settingsLanguages[3].toRowState()
            french = settingsLanguages[4].toRowState()
            german = settingsLanguages[5].toRowState()
            hungarian = settingsLanguages[6].toRowState()
            italian = settingsLanguages[7].toRowState()
            korean = settingsLanguages[8].toRowState()
            polish = settingsLanguages[9].toRowState()
            portuguese = settingsLanguages[10].toRowState()
            russian = settingsLanguages[11].toRowState()
            spanish = settingsLanguages[12].toRowState()
            thai = settingsLanguages[13].toRowState()
            vietnamese = settingsLanguages[14].toRowState()
            notAvailable = settingsLanguages[15].toRowState()
            other = settingsLanguages[16].toRowState()
        }

        fun toPreference() = listOf(
            japanese,
            english,
            chinese,
            dutch,
            french,
            german,
            hungarian,
            italian,
            korean,
            polish,
            portuguese,
            russian,
            spanish,
            thai,
            vietnamese,
            notAvailable,
            other,
        ).joinToString("\n") { it.toPreference() }
    }

    @Composable
    fun LanguageDialogRowCheckbox(
        columnState: LanguageDialogState.ColumnState,
        onStateChange: (LanguageDialogState.ColumnState) -> Unit,
    ) {
        if (columnState != LanguageDialogState.ColumnState.Unavailable) {
            Checkbox(
                checked = columnState == LanguageDialogState.ColumnState.Enabled,
                onCheckedChange = {
                    if (it) {
                        onStateChange(LanguageDialogState.ColumnState.Enabled)
                    } else {
                        onStateChange(LanguageDialogState.ColumnState.Disabled)
                    }
                },
            )
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
    }

    @Composable
    fun LanguageDialogRow(
        language: String,
        row: LanguageDialogState.RowState,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language,
                modifier = Modifier
                    .padding(4.dp)
                    .width(80.dp),
                maxLines = 1,
            )
            LanguageDialogRowCheckbox(row.original, onStateChange = { row.original = it })
            LanguageDialogRowCheckbox(row.translated, onStateChange = { row.translated = it })
            LanguageDialogRowCheckbox(row.rewrite, onStateChange = { row.rewrite = it })
        }
    }

    @Composable
    fun LanguagesDialog(
        onDismissRequest: () -> Unit,
        initialValue: String,
        onValueChange: (String) -> Unit,
    ) {
        val state = remember(initialValue) { LanguageDialogState(initialValue) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(SYMR.strings.language_filtering)) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(SYMR.strings.language_filtering_summary))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(SYMR.strings.language), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(SYMR.strings.original), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(SYMR.strings.translated), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(SYMR.strings.rewrite), modifier = Modifier.padding(4.dp))
                    }
                    LanguageDialogRow(language = Locale.JAPANESE.getDisplayLanguage(Locale.getDefault()), row = state.japanese)
                    LanguageDialogRow(language = Locale.ENGLISH.getDisplayLanguage(Locale.getDefault()), row = state.english)
                    LanguageDialogRow(language = Locale.CHINESE.getDisplayLanguage(Locale.getDefault()), row = state.chinese)
                    LanguageDialogRow(language = Locale("nl").getDisplayLanguage(Locale.getDefault()), row = state.dutch)
                    LanguageDialogRow(language = Locale.FRENCH.getDisplayLanguage(Locale.getDefault()), row = state.french)
                    LanguageDialogRow(language = Locale.GERMAN.getDisplayLanguage(Locale.getDefault()), row = state.german)
                    LanguageDialogRow(language = Locale("hu").getDisplayLanguage(Locale.getDefault()), row = state.hungarian)
                    LanguageDialogRow(language = Locale.ITALIAN.getDisplayLanguage(Locale.getDefault()), row = state.italian)
                    LanguageDialogRow(language = Locale.KOREAN.getDisplayLanguage(Locale.getDefault()), row = state.korean)
                    LanguageDialogRow(language = Locale("pl").getDisplayLanguage(Locale.getDefault()), row = state.polish)
                    LanguageDialogRow(language = Locale("pt").getDisplayLanguage(Locale.getDefault()), row = state.portuguese)
                    LanguageDialogRow(language = Locale("ru").getDisplayLanguage(Locale.getDefault()), row = state.russian)
                    LanguageDialogRow(language = Locale("es").getDisplayLanguage(Locale.getDefault()), row = state.spanish)
                    LanguageDialogRow(language = Locale("th").getDisplayLanguage(Locale.getDefault()), row = state.thai)
                    LanguageDialogRow(language = Locale("vi").getDisplayLanguage(Locale.getDefault()), row = state.vietnamese)
                    LanguageDialogRow(language = stringResource(MR.strings.not_applicable), row = state.notAvailable)
                    LanguageDialogRow(language = stringResource(MR.strings.other_source), row = state.other)
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChange(state.toPreference()) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    @Composable
    fun settingsLanguages(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.exhSettingsLanguages().collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            LanguagesDialog(
                onDismissRequest = { dialogOpen = false },
                initialValue = value,
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.exhSettingsLanguages().set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.language_filtering),
            subtitle = stringResource(SYMR.strings.language_filtering_summary),
            onClick = {
                dialogOpen = true
            },
            enabled = exhentaiEnabled,
        )
    }

    class FrontPageCategoriesDialogState(
        preference: String,
    ) {
        private val enabledCategories = preference.split(",").map { !it.toBoolean() }
        var doujinshi by mutableStateOf(enabledCategories[0])
        var manga by mutableStateOf(enabledCategories[1])
        var artistCg by mutableStateOf(enabledCategories[2])
        var gameCg by mutableStateOf(enabledCategories[3])
        var western by mutableStateOf(enabledCategories[4])
        var nonH by mutableStateOf(enabledCategories[5])
        var imageSet by mutableStateOf(enabledCategories[6])
        var cosplay by mutableStateOf(enabledCategories[7])
        var asianPorn by mutableStateOf(enabledCategories[8])
        var misc by mutableStateOf(enabledCategories[9])

        fun toPreference() = listOf(
            doujinshi,
            manga,
            artistCg,
            gameCg,
            western,
            nonH,
            imageSet,
            cosplay,
            asianPorn,
            misc,
        ).joinToString(separator = ",") { (!it).toString() }
    }

    @Composable
    fun FrontPageCategoriesDialogRow(
        title: String,
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onValueChange(!value) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title)
            Switch(checked = value, onCheckedChange = null)
        }
    }

    @Composable
    fun FrontPageCategoriesDialog(
        onDismissRequest: () -> Unit,
        initialValue: String,
        onValueChange: (String) -> Unit,
    ) {
        val state = remember(initialValue) { FrontPageCategoriesDialogState(initialValue) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(SYMR.strings.frong_page_categories)) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(SYMR.strings.fromt_page_categories_summary))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(MR.strings.categories), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(SYMR.strings.enabled), modifier = Modifier.padding(4.dp))
                    }
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.doujinshi),
                        value = state.doujinshi,
                        onValueChange = { state.doujinshi = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.entry_type_manga),
                        value = state.manga,
                        onValueChange = { state.manga = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.artist_cg),
                        value = state.artistCg,
                        onValueChange = { state.artistCg = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.game_cg),
                        value = state.gameCg,
                        onValueChange = { state.gameCg = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.western),
                        value = state.western,
                        onValueChange = { state.western = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.non_h),
                        value = state.nonH,
                        onValueChange = { state.nonH = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.image_set),
                        value = state.imageSet,
                        onValueChange = { state.imageSet = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.cosplay),
                        value = state.cosplay,
                        onValueChange = { state.cosplay = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.asian_porn),
                        value = state.asianPorn,
                        onValueChange = { state.asianPorn = it },
                    )
                    FrontPageCategoriesDialogRow(
                        title = stringResource(SYMR.strings.misc),
                        value = state.misc,
                        onValueChange = { state.misc = it },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChange(state.toPreference()) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    @Composable
    fun enabledCategories(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.exhEnabledCategories().collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            FrontPageCategoriesDialog(
                onDismissRequest = { dialogOpen = false },
                initialValue = value,
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.exhEnabledCategories().set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.frong_page_categories),
            subtitle = stringResource(SYMR.strings.fromt_page_categories_summary),
            onClick = {
                dialogOpen = true
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun watchedListDefaultState(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        val value by exhPreferences.exhWatchedListDefaultState().collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.exhWatchedListDefaultState(),
            title = stringResource(SYMR.strings.watched_list_default),
            subtitle = stringResource(SYMR.strings.watched_list_state_summary),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun imageQuality(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            preference = exhPreferences.imageQuality(),
            title = stringResource(SYMR.strings.image_quality),
            subtitle = stringResource(SYMR.strings.image_quality_summary),
            entries = persistentMapOf(
                "auto" to stringResource(SYMR.strings.image_quality_auto),
                "ovrs_2400" to "2400x",
                "ovrs_1600" to "1600x",
                "ovrs_1280" to "1280x",
                "ovrs_980" to "980x",
                "ovrs_780" to "780x",
            ),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    fun enhancedEhentaiView(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.enhancedEHentaiView(),
            title = stringResource(SYMR.strings.enhanced_e_hentai_view),
            subtitle = stringResource(SYMR.strings.enhanced_e_hentai_view_summary),
        )
    }

    @Composable
    fun readOnlySync(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.exhReadOnlySync(),
            title = stringResource(SYMR.strings.disable_favorites_uploading),
            subtitle = stringResource(SYMR.strings.disable_favorites_uploading_summary),
        )
    }



    @Composable
    fun lenientSync(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            preference = exhPreferences.exhLenientSync(),
            title = stringResource(SYMR.strings.ignore_sync_errors),
            subtitle = stringResource(SYMR.strings.ignore_sync_errors_summary),
        )
    }


}
