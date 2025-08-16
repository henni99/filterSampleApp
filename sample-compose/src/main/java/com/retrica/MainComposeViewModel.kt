package com.retrica

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ColorMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retrica.sample_compose.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<MainComposeUiState> = MutableStateFlow(MainComposeUiState.empty())
    val uiState: StateFlow<MainComposeUiState>
        get() = _uiState.asStateFlow()


    fun toggleGrayScale() = viewModelScope.launch {
        _uiState.update { state ->

            val originBrightnessColorMatrix = state.brightnessColorMatrix
            val newGrayScaleColorMatrix = ColorMatrix().apply {
                setToSaturation(if (state.isGray) 1f else 0f)
            }

            state.copy(
                isGray = !state.isGray,
                grayScaleColorMatrix = newGrayScaleColorMatrix,
                colorMatrix = makeColorMatrix(
                    originBrightnessColorMatrix.values,
                    newGrayScaleColorMatrix.values,
                )
            )
        }
    }

    fun toggleBrightness() = viewModelScope.launch {
        _uiState.update { state ->

            val originGrayScaleColorMatrix = state.grayScaleColorMatrix
            val newBrightnessColorMatrix = ColorMatrix().apply {
                val offset = if (state.isBright) 0f else 50f
                values[4] = offset
                values[9] = offset
                values[14] = offset
            }

            state.copy(
                isBright = !state.isBright,
                brightnessColorMatrix = newBrightnessColorMatrix,
                colorMatrix = makeColorMatrix(
                    newBrightnessColorMatrix.values,
                    originGrayScaleColorMatrix.values,
                )
            )
        }
    }

    fun reset() = viewModelScope.launch {
        _uiState.update { state ->

            if (state.isReverted) { // 필터 적용 이전으로 되돌린 상태일 경우
                state.copy(
                    isReverted = false,
                    isGray = state.cachedIsGray,
                    isBright = state.cachedIsBright,
                    colorMatrix = state.cachedColorMatrix,
                    cachedIsGray = false,
                    cachedIsBright = false,
                    cachedColorMatrix = ColorMatrix()
                )
            } else { // 필터 적용 이전으로 되돌린 상태가 아닐 경우
                state.copy(
                    isReverted = true,
                    isGray = false,
                    isBright = false,
                    colorMatrix = ColorMatrix(),
                    cachedIsGray = state.isGray,
                    cachedIsBright = state.isBright,
                    cachedColorMatrix = state.colorMatrix
                )
            }

        }
    }

    private fun makeColorMatrix(vararg matrices: FloatArray): ColorMatrix {
        return ColorMatrix().apply {
            matrices.forEach { timesAssign(ColorMatrix(it)) }
        }
    }
}

@Immutable
data class MainComposeUiState(
    val drawable: Int,
    val isGray: Boolean,
    val isBright: Boolean,
    val isReverted: Boolean,
    val grayScaleColorMatrix: ColorMatrix,
    val brightnessColorMatrix: ColorMatrix,
    val colorMatrix: ColorMatrix,
    val cachedIsGray: Boolean,
    val cachedIsBright: Boolean,
    val cachedColorMatrix: ColorMatrix,
) {
    companion object {
        fun empty() = MainComposeUiState(
            drawable = R.drawable.ic_launcher_background,
            isGray = false,
            isBright = false,
            isReverted = false,
            grayScaleColorMatrix = ColorMatrix(),
            brightnessColorMatrix = ColorMatrix(),
            colorMatrix = ColorMatrix(),
            cachedIsGray = false,
            cachedIsBright = false,
            cachedColorMatrix = ColorMatrix()
        )
    }
}



fun ColorMatrix.withBrightnessOffset(offset: Float): ColorMatrix {
    val newMatrix = ColorMatrix()
    newMatrix.set(this)
    val array = newMatrix.values
    array[4] = offset
    array[9] = offset
    array[14] = offset
    return ColorMatrix(array)
}


fun ColorMatrix.withSaturation(saturation: Float): ColorMatrix {
    val offsets = floatArrayOf(values[4], values[9], values[14])

    return ColorMatrix().apply {
        setToSaturation(saturation)
        values[4] = offsets[0]
        values[9] = offsets[1]
        values[14] = offsets[2]
    }
}