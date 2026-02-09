package com.antoniegil.astronia.ui.page.settings.appearance

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.antoniegil.astronia.R
import com.antoniegil.astronia.ui.component.BackButton
import com.antoniegil.astronia.ui.component.PreferenceItem
import com.antoniegil.astronia.ui.component.PreferenceSwitchWithDivider
import com.antoniegil.astronia.ui.component.PreferenceSwitch
import com.antoniegil.astronia.ui.theme.monet.LocalTonalPalettes
import com.antoniegil.astronia.ui.theme.monet.a1
import com.antoniegil.astronia.ui.theme.monet.a2
import com.antoniegil.astronia.ui.theme.monet.a3
import com.antoniegil.astronia.util.SettingsManager
import com.antoniegil.astronia.util.toDisplayName
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import io.material.hct.Hct

private val ColorList = ((4..10) + (1..3)).map { it * 35.0 }.map { 
    Color(Hct.from(it, 40.0, 40.0).toInt())
}

private val paletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Spritz,
    PaletteStyle.FruitSalad,
    PaletteStyle.Vibrant
)

private const val STYLE_MONOCHROME = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePage(
    onNavigateBack: () -> Unit,
    onNavigateToDarkTheme: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {}
) {
    val context = LocalContext.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )

    val themeMode = com.antoniegil.astronia.ui.theme.LocalThemeMode.current
    val dynamicColorSwitch = com.antoniegil.astronia.ui.theme.LocalDynamicColorSwitch.current
    val seedColor = com.antoniegil.astronia.ui.theme.LocalSeedColor.current
    val paletteStyleIndex = com.antoniegil.astronia.ui.theme.LocalPaletteStyleIndex.current

    val isDarkTheme = themeMode == 2 || (themeMode == 0 && isSystemDark)
    
    val darkThemeDesc = when (themeMode) {
        0 -> stringResource(R.string.follow_system)
        1 -> stringResource(R.string.close)
        2 -> stringResource(R.string.open)
        else -> stringResource(R.string.follow_system)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.appearance)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            val pageCount = ColorList.size + 1
            
            val pagerState = rememberPagerState(
                initialPage = if (paletteStyleIndex == STYLE_MONOCHROME) {
                    pageCount - 1
                } else {
                    ColorList.indexOfFirst { it.toArgb() == seedColor }.run {
                        if (this == -1) 0 else this
                    }
                }
            ) { pageCount }

            HorizontalPager(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) { page ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (page < ColorList.size) {
                        ColorButtons(color = ColorList[page])
                    } else {
                        ColorButtonMonochrome()
                    }
                }
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                pageCount = pageCount,
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.outlineVariant,
                indicatorHeight = 6.dp,
                indicatorWidth = 6.dp,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PreferenceSwitch(
                    title = stringResource(R.string.dynamic_color),
                    description = stringResource(R.string.dynamic_color_desc),
                    icon = Icons.Outlined.Colorize,
                    isChecked = dynamicColorSwitch,
                    onCheckedChange = { enabled ->
                        SettingsManager.setDynamicColor(context, enabled)
                    }
                )
            }

            PreferenceSwitchWithDivider(
                title = stringResource(R.string.dark_theme),
                description = darkThemeDesc,
                icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                isChecked = isDarkTheme,
                onChecked = { checked: Boolean ->
                    val newTheme = if (checked) 2 else 1
                    SettingsManager.setThemeMode(context, newTheme)
                },
                onClick = onNavigateToDarkTheme
            )

            val currentLocale = SettingsManager.getLocaleFromPreference(context)
            val languageDescription = currentLocale.toDisplayName()

            PreferenceItem(
                title = stringResource(R.string.language),
                description = languageDescription,
                icon = Icons.Outlined.Language,
                onClick = onNavigateToLanguage
            )
        }
    }
}

@Composable
fun RowScope.ColorButtons(color: Color) {
    paletteStyles.forEachIndexed { index, style ->
        ColorButton(color = color, index = index, tonalStyle = style)
    }
}

@Composable
fun RowScope.ColorButton(
    modifier: Modifier = Modifier,
    color: Color,
    index: Int,
    tonalStyle: PaletteStyle,
) {
    val context = LocalContext.current
    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(tonalStyle)) }
    val dynamicColorSwitch = com.antoniegil.astronia.ui.theme.LocalDynamicColorSwitch.current
    val seedColor = com.antoniegil.astronia.ui.theme.LocalSeedColor.current
    val paletteStyleIndex = com.antoniegil.astronia.ui.theme.LocalPaletteStyleIndex.current
    
    val isSelect = !dynamicColorSwitch && seedColor == color.toArgb() && paletteStyleIndex == index
    
    ColorButtonImpl(
        modifier = modifier,
        tonalPalettes = tonalPalettes,
        isSelected = { isSelect }
    ) {
        SettingsManager.setDynamicColor(context, false)
        SettingsManager.setPaletteStyle(context, index)
        SettingsManager.setSeedColor(context, color.toArgb())
    }
}

@Composable
fun RowScope.ColorButtonMonochrome(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val color = Color.Black
    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(PaletteStyle.Monochrome)) }
    val dynamicColorSwitch = com.antoniegil.astronia.ui.theme.LocalDynamicColorSwitch.current
    val paletteStyleIndex = com.antoniegil.astronia.ui.theme.LocalPaletteStyleIndex.current
    
    val isSelect = paletteStyleIndex == STYLE_MONOCHROME && !dynamicColorSwitch
    
    ColorButtonImpl(
        modifier = modifier,
        tonalPalettes = tonalPalettes,
        isSelected = { isSelect }
    ) {
        SettingsManager.setDynamicColor(context, false)
        SettingsManager.setPaletteStyle(context, STYLE_MONOCHROME)
        SettingsManager.setSeedColor(context, color.toArgb())
    }
}

@Composable
fun RowScope.ColorButtonImpl(
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    tonalPalettes: com.kyant.monet.TonalPalettes,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {},
) {
    val containerSize by animateDpAsState(targetValue = if (isSelected()) 28.dp else 0.dp, label = "")
    val iconSize by animateDpAsState(targetValue = if (isSelected()) 16.dp else 0.dp, label = "")
    Surface(
        modifier = modifier
            .padding(4.dp)
            .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
            .weight(1f, false)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        onClick = onClick,
    ) {
        CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
            val color1 = 80.a1
            val color2 = 90.a2
            val color3 = 60.a3
            
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .drawBehind { drawCircle(color1) }
                        .align(Alignment.Center)
                ) {
                    Surface(
                        color = color2,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(24.dp),
                    ) {}
                    Surface(
                        color = color3,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp),
                    ) {}
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .size(containerSize)
                            .drawBehind { drawCircle(containerColor) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(iconSize)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
