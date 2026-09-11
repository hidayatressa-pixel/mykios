package com.app.mykios

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class GoogleDriveHelper(private val context: Context) {

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(context.getString(R.string.app_name)).build()
    }

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun backupDatabase(): Boolean = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext false
        try {
            val dbFile = context.getDatabasePath("mykios_database")
            if (!dbFile.exists()) {
                android.util.Log.e("CloudSync", "Database file not found")
                return@withContext false
            }

            // List existing backups
            val list = service.files().list()
                .setQ("name contains 'mykios_backup' and trashed = false")
                .execute()
            
            // Delete old backups to save space
            list.files?.forEach { file ->
                try {
                    service.files().delete(file.id).execute()
                } catch (e: Exception) {
                    android.util.Log.e("CloudSync", "Failed to delete old backup: ${e.message}")
                }
            }

            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = "mykios_backup_${System.currentTimeMillis()}.db"
            fileMetadata.parents = Collections.singletonList("root")

            val mediaContent = FileContent("application/octet-stream", dbFile)
            service.files().create(fileMetadata, mediaContent).execute()
            true
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Backup error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreDatabase(driveFileId: String): Boolean = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext false
        try {
            val dbFile = context.getDatabasePath("mykios_database")
            val outputStream = dbFile.outputStream()
            service.files().get(driveFileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            true
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Restore error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
