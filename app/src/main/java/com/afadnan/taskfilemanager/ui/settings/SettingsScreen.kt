package com.afadnan.taskfilemanager.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afadnan.taskfilemanager.data.preferences.AppTheme
import com.afadnan.taskfilemanager.viewmodel.FileViewModel
import com.afadnan.taskfilemanager.viewmodel.ThemeViewModel


/*
 * =============================================================
 * SETTINGS SCREEN
 * =============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    fileViewModel: FileViewModel,
    themeViewModel: ThemeViewModel
) {

    /*
     * =========================================================
     * VIEWMODEL STATE
     * =========================================================
     */

    val currentDirectory by
    fileViewModel.currentDirectory.collectAsState()

    val sortState by
    fileViewModel.sortState.collectAsState()

    val selectedTheme by
    themeViewModel.theme.collectAsState()


    /*
     * =========================================================
     * LOCAL UI STATE
     * =========================================================
     */

    var showClearStorageDialog by remember {
        mutableStateOf(false)
    }

    var showThemeMenu by remember {
        mutableStateOf(false)
    }

    var foldersFirst by remember {
        mutableStateOf(
            sortState.directoriesFirst
        )
    }


    /*
     * =========================================================
     * FOLDER PICKER
     * =========================================================
     */

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                fileViewModel.saveSelectedFolder(

                    uri = uri,

                    flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }


    /*
     * =========================================================
     * CURRENT FOLDER NAME
     * =========================================================
     */

    val currentFolderName =
        currentDirectory
            ?.documentFile
            ?.name
            ?: "No folder selected"


    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Settings",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "Customize Task File Manager",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
        ) {

            /*
             * =================================================
             * HEADER
             * =================================================
             */

            SettingsHeader()


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            /*
             * =================================================
             * STORAGE
             * =================================================
             */

            SettingsSectionTitle(
                title = "Storage"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            StorageSettingsCard(

                folderName =
                    currentFolderName,

                onChangeFolder = {

                    folderPickerLauncher.launch(
                        null
                    )
                },

                onClearStorage = {

                    showClearStorageDialog =
                        true
                }
            )


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            /*
             * =================================================
             * APPEARANCE
             * =================================================
             */

            SettingsSectionTitle(
                title = "Appearance"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            AppearanceSettingsCard(

                selectedTheme =
                    selectedTheme,

                showThemeMenu =
                    showThemeMenu,

                onShowThemeMenu = {

                    showThemeMenu =
                        true
                },

                onDismissThemeMenu = {

                    showThemeMenu =
                        false
                },

                onThemeSelected = { theme ->

                    themeViewModel.setTheme(
                        theme
                    )

                    showThemeMenu =
                        false
                }
            )


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            /*
             * =================================================
             * FILE MANAGER
             * =================================================
             */

            SettingsSectionTitle(
                title = "File Manager"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            FileManagerSettingsCard(

                foldersFirst =
                    foldersFirst,

                onFoldersFirstChanged = { enabled ->

                    foldersFirst =
                        enabled

                    if (
                        fileViewModel
                            .sortState
                            .value
                            .directoriesFirst != enabled
                    ) {

                        fileViewModel
                            .toggleDirectoriesFirst()
                    }
                }
            )


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            /*
             * =================================================
             * ABOUT
             * =================================================
             */

            SettingsSectionTitle(
                title = "About"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            AboutSettingsCard()


            Spacer(
                modifier =
                    Modifier.height(32.dp)
            )


            /*
             * =================================================
             * FOOTER
             * =================================================
             */

            Text(
                text =
                    "Task File Manager",

                modifier =
                    Modifier.fillMaxWidth(),

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Medium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "Simple • Private • Efficient",

                modifier =
                    Modifier.fillMaxWidth(),

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .outline,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }
    }


    /*
     * =========================================================
     * CLEAR STORAGE DIALOG
     * =========================================================
     */

    if (showClearStorageDialog) {

        AlertDialog(

            onDismissRequest = {

                showClearStorageDialog =
                    false
            },

            icon = {

                Icon(
                    imageVector =
                        Icons.Default.Delete,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            },

            title = {

                Text(
                    text =
                        "Remove selected folder?"
                )
            },

            text = {

                Text(
                    text =
                        "Task File Manager will stop managing this folder. " +
                                "Your actual files will not be deleted."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showClearStorageDialog =
                            false

                        fileViewModel.clearStorage()
                    }

                ) {

                    Text(
                        text = "Remove",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showClearStorageDialog =
                            false
                    }

                ) {

                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }
}


/*
 * =============================================================
 * SETTINGS HEADER
 * =============================================================
 */

@Composable
private fun SettingsHeader() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(24.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                modifier =
                    Modifier.size(58.dp),

                shape =
                    RoundedCornerShape(18.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Settings,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(30.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )


            Column {

                Text(

                    text =
                        "Task File Manager",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Text(

                    text =
                        "Manage storage, appearance and file preferences.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )
            }
        }
    }
}


/*
 * =============================================================
 * SECTION TITLE
 * =============================================================
 */

@Composable
private fun SettingsSectionTitle(
    title: String
) {

    Text(

        text =
            title,

        style =
            MaterialTheme
                .typography
                .titleMedium,

        fontWeight =
            FontWeight.Bold,

        modifier =
            Modifier.padding(
                horizontal = 4.dp
            )
    )
}


/*
 * =============================================================
 * STORAGE SETTINGS CARD
 * =============================================================
 */

@Composable
private fun StorageSettingsCard(

    folderName: String,

    onChangeFolder: () -> Unit,

    onClearStorage: () -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            )
    ) {

        Column {

            /*
             * Current folder
             */

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                SettingsIconBox(

                    icon =
                        Icons.Default.Folder,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer,

                    iconTint =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )


                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Current storage",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            folderName,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        fontWeight =
                            FontWeight.Medium,

                        maxLines = 1
                    )
                }
            }


            HorizontalDivider()


            /*
             * Change folder
             */

            SettingsActionRow(

                icon =
                    Icons.Default.Storage,

                title =
                    "Change storage folder",

                subtitle =
                    "Choose another folder to manage",

                onClick =
                    onChangeFolder
            )


            HorizontalDivider()


            /*
             * Remove folder
             */

            SettingsActionRow(

                icon =
                    Icons.Default.Delete,

                title =
                    "Remove selected folder",

                subtitle =
                    "Stop managing this folder",

                onClick =
                    onClearStorage,

                iconTint =
                    MaterialTheme
                        .colorScheme
                        .error
            )
        }
    }
}


/*
 * =============================================================
 * APPEARANCE SETTINGS CARD
 * =============================================================
 */

@Composable
private fun AppearanceSettingsCard(

    selectedTheme: AppTheme,

    showThemeMenu: Boolean,

    onShowThemeMenu: () -> Unit,

    onDismissThemeMenu: () -> Unit,

    onThemeSelected: (AppTheme) -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            )
    ) {

        Box {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick =
                                onShowThemeMenu
                        )
                        .padding(18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                SettingsIconBox(

                    icon =
                        themeIcon(
                            selectedTheme
                        ),

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer,

                    iconTint =
                        MaterialTheme
                            .colorScheme
                            .onSecondaryContainer
                )


                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Theme",

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        fontWeight =
                            FontWeight.Medium
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            themeLabel(
                                selectedTheme
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                Text(
                    text =
                        themeLabel(
                            selectedTheme
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            DropdownMenu(

                expanded =
                    showThemeMenu,

                onDismissRequest =
                    onDismissThemeMenu
            ) {

                ThemeMenuItem(

                    title =
                        "System default",

                    icon =
                        Icons.Default.Brightness4,

                    selected =
                        selectedTheme ==
                                AppTheme.SYSTEM_DEFAULT,

                    onClick = {

                        onThemeSelected(
                            AppTheme.SYSTEM_DEFAULT
                        )
                    }
                )


                ThemeMenuItem(

                    title =
                        "Light",

                    icon =
                        Icons.Default.LightMode,

                    selected =
                        selectedTheme ==
                                AppTheme.LIGHT,

                    onClick = {

                        onThemeSelected(
                            AppTheme.LIGHT
                        )
                    }
                )


                ThemeMenuItem(

                    title =
                        "Dark",

                    icon =
                        Icons.Default.DarkMode,

                    selected =
                        selectedTheme ==
                                AppTheme.DARK,

                    onClick = {

                        onThemeSelected(
                            AppTheme.DARK
                        )
                    }
                )
            }
        }
    }
}


/*
 * =============================================================
 * THEME MENU ITEM
 * =============================================================
 */

@Composable
private fun ThemeMenuItem(

    title: String,

    icon: ImageVector,

    selected: Boolean,

    onClick: () -> Unit

) {

    DropdownMenuItem(

        text = {

            Text(
                text =
                    title
            )
        },

        leadingIcon = {

            Icon(
                imageVector =
                    icon,

                contentDescription =
                    null
            )
        },

        trailingIcon = {

            if (selected) {

                Icon(
                    imageVector =
                        Icons.Default.Check,

                    contentDescription =
                        "Selected",

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        },

        onClick =
            onClick
    )
}


/*
 * =============================================================
 * FILE MANAGER SETTINGS CARD
 * =============================================================
 */

@Composable
private fun FileManagerSettingsCard(

    foldersFirst: Boolean,

    onFoldersFirstChanged: (Boolean) -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            )
    ) {

        Column {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                SettingsIconBox(

                    icon =
                        Icons.Default.ViewList,

                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .tertiaryContainer,

                    iconTint =
                        MaterialTheme
                            .colorScheme
                            .onTertiaryContainer
                )


                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Folders first",

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        fontWeight =
                            FontWeight.Medium
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(

                        text =
                            "Show folders before files",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                Switch(

                    checked =
                        foldersFirst,

                    onCheckedChange =
                        onFoldersFirstChanged
                )
            }


            HorizontalDivider()


            SettingsInfoRow(

                icon =
                    Icons.Default.SwapVert,

                title =
                    "Sorting",

                subtitle =
                    "Use the sort menu in Files to change name, size or date order."
            )
        }
    }
}


/*
 * =============================================================
 * ABOUT SETTINGS CARD
 * =============================================================
 */

@Composable
private fun AboutSettingsCard() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            )
    ) {

        Column {

            SettingsInfoRow(

                icon =
                    Icons.Default.Info,

                title =
                    "About Task File Manager",

                subtitle =
                    "A simple and efficient Android file manager."
            )


            HorizontalDivider()


            SettingsInfoRow(

                icon =
                    Icons.Default.Description,

                title =
                    "Version",

                subtitle =
                    "1.0.0"
            )


            HorizontalDivider()


            SettingsInfoRow(

                icon =
                    Icons.Default.Storage,

                title =
                    "Storage access",

                subtitle =
                    "Storage is accessed through Android's selected-folder permission."
            )
        }
    }
}


/*
 * =============================================================
 * SETTINGS ICON BOX
 * =============================================================
 */

@Composable
private fun SettingsIconBox(

    icon: ImageVector,

    containerColor: Color,

    iconTint: Color

) {

    Surface(

        modifier =
            Modifier.size(46.dp),

        shape =
            RoundedCornerShape(14.dp),

        color =
            containerColor
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(22.dp),

                tint =
                    iconTint
            )
        }
    }
}


/*
 * =============================================================
 * SETTINGS ACTION ROW
 * =============================================================
 */

@Composable
private fun SettingsActionRow(

    icon: ImageVector,

    title: String,

    subtitle: String,

    onClick: () -> Unit,

    iconTint: Color =
        MaterialTheme
            .colorScheme
            .primary

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
                .padding(18.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        SettingsIconBox(

            icon =
                icon,

            containerColor =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,

            iconTint =
                iconTint
        )


        Spacer(
            modifier =
                Modifier.width(14.dp)
        )


        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(

                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(

                text =
                    subtitle,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/*
 * =============================================================
 * SETTINGS INFO ROW
 * =============================================================
 */

@Composable
private fun SettingsInfoRow(

    icon: ImageVector,

    title: String,

    subtitle: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(18.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        SettingsIconBox(

            icon =
                icon,

            containerColor =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,

            iconTint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.width(14.dp)
        )


        Column {

            Text(

                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(

                text =
                    subtitle,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/*
 * =============================================================
 * THEME LABEL
 * =============================================================
 */

private fun themeLabel(
    theme: AppTheme
): String {

    return when (theme) {

        AppTheme.SYSTEM_DEFAULT ->
            "System default"

        AppTheme.LIGHT ->
            "Light"

        AppTheme.DARK ->
            "Dark"
    }
}


/*
 * =============================================================
 * THEME ICON
 * =============================================================
 */

private fun themeIcon(
    theme: AppTheme
): ImageVector {

    return when (theme) {

        AppTheme.SYSTEM_DEFAULT ->
            Icons.Default.Brightness4

        AppTheme.LIGHT ->
            Icons.Default.LightMode

        AppTheme.DARK ->
            Icons.Default.DarkMode
    }
}
