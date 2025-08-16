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

    /**
     * 흑백(그레이스케일) 필터를 토글하는 함수
     *
     * - 현재 isGray 상태를 반전시킴
     * - 흑백 적용 시 ColorMatrix의 채도를 0으로 설정
     * - 해제 시 채도를 1로 복원
     * - 기존 밝기(ColorMatrix) 값과 합성하여 최종 colorMatrix를 갱신
     */
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

    /**
     * 밝기 필터를 토글하는 함수
     *
     * - 현재 isBright 상태를 반전시킴
     * - 밝기 증가 시 RGB 채널 offset을 +50 적용
     * - 밝기 해제 시 offset을 0으로 초기화
     * - 기존 흑백(ColorMatrix) 값과 합성하여 최종 colorMatrix를 갱신
     */
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

    /**
     * 현재 적용된 필터 상태를 초기화하거나 복원하는 함수
     *
     * - state.isReverted == true 이면: 저장된 캐시(cachedIsGray, cachedIsBright, cachedColorMatrix)로 복원
     * - state.isReverted == false 이면: 현재 필터 상태를 캐시에 저장 후 초기화
     * - 캐시를 이용해 복원/초기화를 반복할 수 있음
     */
    fun reset() = viewModelScope.launch {
        _uiState.update { state ->

            if (state.isReverted) {
                state.copy(
                    isReverted = false,
                    isGray = state.cachedIsGray,
                    isBright = state.cachedIsBright,
                    colorMatrix = state.cachedColorMatrix,
                    cachedIsGray = false,
                    cachedIsBright = false,
                    cachedColorMatrix = ColorMatrix()
                )
            } else {
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

    /**
     * 여러 ColorMatrix를 순서대로 곱하여 하나의 ColorMatrix로 합성하는 함수
     *
     * @param matrices 합성할 ColorMatrix의 values 배열(최대 5x4, length=20)
     * @return 합성된 ColorMatrix
     */
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


