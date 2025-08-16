package com.retrica.sample

import android.graphics.ColorMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState.empty())
    val uiState: StateFlow<MainUiState>
        get() = _uiState.asStateFlow()

    fun toggleGrayScale() = viewModelScope.launch {
        _uiState.update { state ->

            val originBrightnessColorMatrix = state.brightnessColorMatrix
            val newGrayScaleColorMatrix = ColorMatrix().apply { setSaturation(
                if (state.isGray) 1f else 0f
            ) }

            state.copy(
                isGray = !state.isGray,
                grayScaleColorMatrix = newGrayScaleColorMatrix,
                colorMatrix = makeColorMatrix(
                    newGrayScaleColorMatrix,
                    originBrightnessColorMatrix,
                )
            )
        }
    }

    fun toggleBrightness() = viewModelScope.launch {
        _uiState.update { state ->

            val originGrayScaleColorMatrix = state.grayScaleColorMatrix
            val newBrightnessColorMatrix = ColorMatrix().apply {
                val offset = if (state.isBright) 0f else 50f
                array[4] = offset
                array[9] = offset
                array[14] = offset
            }

            state.copy(
                isBright = !state.isBright,
                brightnessColorMatrix = newBrightnessColorMatrix,
                colorMatrix = makeColorMatrix(
                    originGrayScaleColorMatrix,
                    newBrightnessColorMatrix,
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

    private fun makeColorMatrix(vararg matrices: ColorMatrix): ColorMatrix {
        return ColorMatrix().apply {
            matrices.forEach { postConcat(it) }
        }
    }
}

data class MainUiState(
    val isGray: Boolean,
    val isBright: Boolean,
    val isReverted: Boolean,
    val grayScaleColorMatrix: ColorMatrix,
    val brightnessColorMatrix: ColorMatrix,
    val colorMatrix: ColorMatrix,
    val cachedIsGray: Boolean,
    val cachedIsBright: Boolean,
    val cachedColorMatrix: ColorMatrix
) {
    companion object {
        fun empty() = MainUiState(
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


