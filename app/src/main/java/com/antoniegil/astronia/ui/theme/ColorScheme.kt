package com.antoniegil.astronia.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.kyant.monet.TonalPalettes

@Immutable
data class FixedColorRoles(
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
) {
    companion object {
        internal val unspecified =
            FixedColorRoles(
                primaryFixed = Color.Unspecified,
                primaryFixedDim = Color.Unspecified,
                onPrimaryFixed = Color.Unspecified,
                onPrimaryFixedVariant = Color.Unspecified,
                secondaryFixed = Color.Unspecified,
                secondaryFixedDim = Color.Unspecified,
                onSecondaryFixed = Color.Unspecified,
                onSecondaryFixedVariant = Color.Unspecified,
                tertiaryFixed = Color.Unspecified,
                tertiaryFixedDim = Color.Unspecified,
                onTertiaryFixed = Color.Unspecified,
                onTertiaryFixedVariant = Color.Unspecified,
            )

        @Stable
        internal fun fromTonalPalettes(palettes: TonalPalettes): FixedColorRoles {
            return with(palettes) {
                FixedColorRoles(
                    primaryFixed = accent1(90.toDouble()),
                    primaryFixedDim = accent1(80.toDouble()),
                    onPrimaryFixed = accent1(10.toDouble()),
                    onPrimaryFixedVariant = accent1(30.toDouble()),
                    secondaryFixed = accent2(90.toDouble()),
                    secondaryFixedDim = accent2(80.toDouble()),
                    onSecondaryFixed = accent2(10.toDouble()),
                    onSecondaryFixedVariant = accent2(30.toDouble()),
                    tertiaryFixed = accent3(90.toDouble()),
                    tertiaryFixedDim = accent3(80.toDouble()),
                    onTertiaryFixed = accent3(10.toDouble()),
                    onTertiaryFixedVariant = accent3(30.toDouble()),
                )
            }
        }

        @Stable
        internal fun fromColorSchemes(
            lightColors: ColorScheme,
            darkColors: ColorScheme,
        ): FixedColorRoles {
            return FixedColorRoles(
                primaryFixed = lightColors.primaryContainer,
                onPrimaryFixed = lightColors.onPrimaryContainer,
                onPrimaryFixedVariant = darkColors.primaryContainer,
                secondaryFixed = lightColors.secondaryContainer,
                onSecondaryFixed = lightColors.onSecondaryContainer,
                onSecondaryFixedVariant = darkColors.secondaryContainer,
                tertiaryFixed = lightColors.tertiaryContainer,
                onTertiaryFixed = lightColors.onTertiaryContainer,
                onTertiaryFixedVariant = darkColors.tertiaryContainer,
                primaryFixedDim = darkColors.primary,
                secondaryFixedDim = darkColors.secondary,
                tertiaryFixedDim = darkColors.tertiary,
            )
        }
    }
}

object FixedAccentColors {
    val primaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixed

    val primaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixedDim

    val onPrimaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixed

    val onPrimaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixedVariant

    val secondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixed

    val secondaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixedDim

    val onSecondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixed

    val onSecondaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixedVariant

    val tertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixed

    val tertiaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixedDim

    val onTertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixed

    val onTertiaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixedVariant
}
