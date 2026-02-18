package tachiyomi.domain.backup.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BackupPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun backupInterval() = preferenceStore.getInt("backup_interval", 12)

    fun autoBackupDestination() = preferenceStore.getInt("auto_backup_destination", AUTO_BACKUP_DESTINATION_LOCAL)

    fun autoBackupDriveDirectoryUri() =
        preferenceStore.getString(Preference.appStateKey("auto_backup_drive_directory_uri"), "")

    fun lastAutoBackupTimestamp() = preferenceStore.getLong(Preference.appStateKey("last_auto_backup_timestamp"), 0L)

    companion object {
        const val AUTO_BACKUP_DESTINATION_LOCAL = 0
        const val AUTO_BACKUP_DESTINATION_GOOGLE_DRIVE = 1
    }
}
