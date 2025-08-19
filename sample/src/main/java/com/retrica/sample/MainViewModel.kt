package com.retrica.sample

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
     * 흑백 필터를 토글하는 함수
     *
     * - 현재 isGray 상태를 반전시킴
     * - 흑백 적용 시 ColorMatrix의 채도를 0으로 설정
     * - 해제 시 채도를 1로 복원
     * - 기존 밝기(ColorMatrix) 값과 concat하여 최종 colorFilter 갱신
     */
    fun toggleGrayScale() = viewModelScope.launch {
        _uiState.update { state ->

            val originBrightnessColorMatrix = state.brightnessColorMatrix
            val newGrayScaleColorMatrix = ColorMatrix().apply {
                setSaturation(if (state.isGray) 1f else 0f)
            }

            state.copy(
                isGray = !state.isGray,
                grayScaleColorMatrix = newGrayScaleColorMatrix,
                colorFilter = createColorFilter(
                    originBrightnessColorMatrix,
                    newGrayScaleColorMatrix,
                )
            )
        }
    }

    /**
     * 밝기 필터를 토글하는 함수
     *
     * - 현재 isBright 상태를 토글시킴
     * - 밝기 증가 시 RGB 채널 scale * 1.2, offset을 +10 적용
     * - 밝기 해제 시 RGB 채널 scale * 1, offset을 0으로 초기화
     * - 기존 흑백(ColorMatrix) 값과 concat하여 최종 colorFilter를 갱신
     */
    fun toggleBrightness() = viewModelScope.launch {
        _uiState.update { state ->

            val originGrayScaleColorMatrix = state.grayScaleColorMatrix
            val newBrightnessColorMatrix = lightingColorMatrix(
                scale = if (state.isBright) 1f else 1.2f,
                offset = if (state.isBright) 0f else 10f
            )

            state.copy(
                isBright = !state.isBright,
                brightnessColorMatrix = newBrightnessColorMatrix,
                colorFilter = createColorFilter(
                    newBrightnessColorMatrix,
                    originGrayScaleColorMatrix
                )
            )
        }
    }

    /**
     * 현재 적용된 필터 상태를 초기화하거나 복원하는 함수
     *
     * - state.isReverted == true 이면: 저장된 캐시(cachedIsGray, cachedIsBright, cachedColorFilter)로 복원
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
                    colorFilter = state.cachedColorFilter,
                    cachedIsGray = false,
                    cachedIsBright = false,
                    cachedColorFilter = ColorMatrixColorFilter(ColorMatrix())
                )
            } else {
                state.copy(
                    isReverted = true,
                    isGray = false,
                    isBright = false,
                    colorFilter = ColorMatrixColorFilter(ColorMatrix()),
                    cachedIsGray = state.isGray,
                    cachedIsBright = state.isBright,
                    cachedColorFilter = state.colorFilter
                )
            }

        }
    }

    /**
     * 두 개의 ColorMatrix(밝기, 흑백)를 concat
     * 하나의 ColorFilter로 합성하는 함수
     *
     * @param brightnessColorMatrix 밝기 조절용 ColorMatrix
     * @param grayScaleColorMatrix 흑백 변환용 ColorMatrix
     * @return 합성된 ColorFilter
     */
    private fun createColorFilter(
        brightnessColorMatrix: ColorMatrix,
        grayScaleColorMatrix: ColorMatrix
    ): ColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.postConcat(brightnessColorMatrix)
        colorMatrix.postConcat(grayScaleColorMatrix)
        return ColorMatrixColorFilter(colorMatrix)
    }
}

/**
 * RGB 채널에 대해 곱셈(scale)과 덧셈(offset)을 적용하는 ColorMatrix를 생성하는 함수.
 *
 * - 입력 색상 (R, G, B)에 대해 다음과 같이 변환이 수행됨:
 *   R' = R * scale + offset
 *   G' = G * scale + offset
 *   B' = B * scale + offset
 *   A' = A   (알파 채널은 그대로 유지)
 *
 * - 예시:
 *   scale = 1.2f, offset = 30f  → 색상을 20% 더 밝게 하고 모든 채널에 +30을 더함
 *   scale = 1.0f, offset = -50f → 전체적으로 어둡게 함
 *
 * @param scale  각 채널에 곱해질 값 (색상 대비 조절 역할)
 * @param offset 각 채널에 더해질 값 (밝기 조절 역할)
 * @return 변환된 ColorMatrix 객체
 */

fun lightingColorMatrix(scale: Float, offset: Float): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

data class MainUiState(
    val isGray: Boolean,
    val isBright: Boolean,
    val isReverted: Boolean,
    val grayScaleColorMatrix: ColorMatrix,
    val brightnessColorMatrix: ColorMatrix,
    val colorFilter: ColorFilter,
    val cachedIsGray: Boolean,
    val cachedIsBright: Boolean,
    val cachedColorFilter: ColorFilter
) {
    companion object {
        fun empty() = MainUiState(
            isGray = false,
            isBright = false,
            isReverted = false,
            grayScaleColorMatrix = ColorMatrix(),
            brightnessColorMatrix = ColorMatrix(),
            colorFilter = ColorMatrixColorFilter(ColorMatrix()),
            cachedIsGray = false,
            cachedIsBright = false,
            cachedColorFilter = ColorMatrixColorFilter(ColorMatrix())
        )
    }
}


