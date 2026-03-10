package app.revanced.manager.ui.screen.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SearchView as SearchViewComponent
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.component.settings.ThemeSelector
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.GeneralSettingsViewModel
import app.revanced.manager.util.transparentListItemColors
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: GeneralSettingsViewModel = koinViewModel()
) {
    val prefs = viewModel.prefs
    val coroutineScope = viewModel.viewModelScope
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )

    if (showLanguagePicker) {
        LanguagePicker(
            supportedLocales = viewModel.getSupportedLocales(),
            currentLocale = viewModel.getCurrentLocale(),
            onDismiss = { showLanguagePicker = false },
            onSelect = { viewModel.setLocale(it) }
        )
    }

    val animatedSurfaceColor = animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "surface"
    ).value

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.general)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = animatedSurfaceColor,
                    scrolledContainerColor = animatedSurfaceColor
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = animatedSurfaceColor,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = scrollState
        ) {
            ListSection(
                title = stringResource(R.string.appearance),
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                val currentLocale = viewModel.getCurrentLocale()
                val currentLanguageDisplay = remember(currentLocale) {
                    currentLocale?.let { viewModel.getLocaleDisplayName(it) }
                }
                val theme by prefs.theme.getAsState()
                
                ThemeSelector(
                    currentTheme = theme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )

                SettingsListItem(
                    headlineContent = stringResource(R.string.language),
                    supportingContent = stringResource(R.string.language_description),
                    onClick = { showLanguagePicker = true },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLanguageDisplay
                                    ?: stringResource(R.string.language_system_default),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BooleanItem(
                        preference = prefs.dynamicColor,
                        coroutineScope = coroutineScope,
                        headline = R.string.dynamic_color,
                        description = R.string.dynamic_color_description
                    )
                }
                AnimatedVisibility(theme != Theme.LIGHT) {
                    BooleanItem(
                        preference = prefs.pureBlackTheme,
                        coroutineScope = coroutineScope,
                        headline = R.string.pure_black_theme,
                        description = R.string.pure_black_theme_description
                    )
                }
            }

            ListSection(
                title = stringResource(R.string.networking),
                leadingContent = { Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                BooleanItem(
                    preference = prefs.allowMeteredNetworks,
                    coroutineScope = coroutineScope,
                    headline = R.string.allow_metered_networks,
                    description = R.string.allow_metered_networks_description
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguagePicker(
    supportedLocales: List<Locale>,
    currentLocale: Locale?,
    onDismiss: () -> Unit,
    onSelect: (Locale?) -> Unit
) {
    val systemDefaultString = stringResource(R.string.language_system_default)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val languageListState = rememberLazyListState()
    val isLanguageListScrollable by remember {
        derivedStateOf {
            languageListState.canScrollBackward || languageListState.canScrollForward
        }
    }

    val filteredLocales = remember(searchQuery, supportedLocales, currentLocale) {
        if (searchQuery.isEmpty()) {
            supportedLocales
        } else {
            supportedLocales.filter { locale ->
                val currentAppLocale = currentLocale ?: Locale.getDefault()
                val localizedName = locale.getDisplayName(currentAppLocale)
                val nativeName = locale.getDisplayName(locale)

                localizedName.contains(searchQuery, ignoreCase = true) ||
                nativeName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = { isLanguageListScrollable }
    )

    FullscreenDialog(onDismissRequest = onDismiss) {
        if (isSearchActive) {
            SearchViewComponent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onActiveChange = {
                    isSearchActive = it
                    if (!it) searchQuery = ""
                },
                placeholder = { Text(stringResource(R.string.search_languages)) },
            ) {
                if (searchQuery.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search_languages),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = stringResource(R.string.type_anything),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredLocales) { locale ->
                            val currentAppLocale = currentLocale ?: Locale.getDefault()
                            val localizedName = locale.getDisplayName(currentAppLocale)
                            val nativeName = locale.getDisplayName(locale)

                            ListItem(
                                modifier = Modifier.clickable {
                                    onSelect(locale)
                                    onDismiss()
                                },
                                leadingContent = {
                                    HapticRadioButton(
                                        selected = currentLocale == locale,
                                        onClick = {
                                            onSelect(locale)
                                            onDismiss()
                                        }
                                    )
                                },
                                headlineContent = { Text(localizedName) },
                                supportingContent = if (nativeName != localizedName) {
                                    { Text(nativeName) }
                                } else null,
                                colors = transparentListItemColors
                            )
                        }
                    }
                }
            }
        } else {
            Scaffold(
                topBar = {
                    MediumFlexibleTopAppBar(
                        title = { Text(stringResource(R.string.language)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }, shapes = IconButtonDefaults.shapes()) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            ) { paddingValues ->
                LazyColumnWithScrollbar(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    state = languageListState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        ListItem(
                            modifier = Modifier.clickable {
                                onSelect(null)
                                onDismiss()
                            },
                            leadingContent = {
                                HapticRadioButton(
                                    selected = currentLocale == null,
                                    onClick = {
                                        onSelect(null)
                                        onDismiss()
                                    }
                                )
                            },
                            headlineContent = { Text(systemDefaultString) }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    items(supportedLocales) { locale ->
                        val currentAppLocale = currentLocale ?: Locale.getDefault()
                        val localizedName = locale.getDisplayName(currentAppLocale)
                        val nativeName = locale.getDisplayName(locale)

                        ListItem(
                            modifier = Modifier.clickable {
                                onSelect(locale)
                                onDismiss()
                            },
                            leadingContent = {
                                HapticRadioButton(
                                    selected = currentLocale == locale,
                                    onClick = {
                                        onSelect(locale)
                                        onDismiss()
                                    }
                                )
                            },
                            headlineContent = { Text(localizedName) },
                            supportingContent = if (nativeName != localizedName) {
                                { Text(nativeName) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

