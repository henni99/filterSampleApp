package com.retrica.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.retrica.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeView()
        initializeClickListener()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    /**
     * 화면 UI를 초기화하고 시스템 바 여백 적용
     */
    private fun initializeView() {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * 버튼 클릭 리스너 초기화
     * - btnGray: 흑백 토글
     * - btnBright: 밝기 토글
     * - btnReset: 원래 상태로 되돌리기
     */
    fun initializeClickListener() = with(binding) {
        btnGray.setOnClickListener { viewModel.toggleGrayScale() }
        btnBright.setOnClickListener { viewModel.toggleBrightness() }
        btnReset.setOnClickListener { viewModel.reset() }
    }

    /**
     * ViewModel에서 전달받은 상태를 기반으로 UI 요소를 업데이트
     *
     * @param state 현재 UI 상태(MainUiState)
     */
    fun render(state: MainUiState) = with(binding) {
        val ctx = root.context

        // 현재 적용된 필터 상태 계산 (isRestored에 따라 실제 적용 상태 결정)
        val isGrayNow = if (state.isRestored) state.cachedIsGray else state.isGray
        val isBrightNow = if (state.isRestored) state.cachedIsBright else state.isBright

        // Gray 버튼 상태 및 텍스트 변경
        btnGray.isEnabled = !state.isRestored
        btnGray.text = ctx.getString(
            if (isGrayNow) R.string.gray_remove else R.string.gray_apply
        )

        // Bright 버튼 상태 및 텍스트 변경
        btnBright.isEnabled = !state.isRestored
        btnBright.text = ctx.getString(
            if (isBrightNow) R.string.bright_decrease else R.string.bright_increase
        )

        // Reset 버튼 텍스트 변경
        btnReset.text = ctx.getString(
            if (state.isRestored) R.string.reset_restore else R.string.reset_revert
        )

        // Image에 ColorFilter 필터 적용
        imageView.colorFilter = state.colorFilter

    }
}
