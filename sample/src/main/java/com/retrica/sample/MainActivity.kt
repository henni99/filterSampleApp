package com.retrica.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.get
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.retrica.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

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

                    println("colorMatrix: ${state.colorMatrix.array.toList()}")
                    render(state)
                }
            }
        }
    }

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

    fun initializeClickListener() = with(binding) {
        btnGray.setOnClickListener { viewModel.toggleGrayScale() }
        btnBright.setOnClickListener { viewModel.toggleBrightness() }
        btnReset.setOnClickListener { viewModel.reset() }
    }

    fun render(state: MainUiState) = with(binding) {

        btnGray.isEnabled = !state.isReverted
        btnGray.text = if (state.isReverted) {
            if (state.cachedIsGray) "흑백 해제" else "흑백 적용"
        } else {
            if (state.isGray) "흑백 해제" else "흑백 적용"
        }

        btnBright.isEnabled = !state.isReverted
        btnBright.text = if (state.isReverted) {
            if (state.cachedIsBright) "밝기 감소" else "밝기 증가"
        } else {
            if (state.isBright) "밝기 감소" else "밝기 증가"
        }

        btnReset.text = if (state.isReverted) "복원하기" else "되돌리기"

        val filter = ColorMatrixColorFilter(state.colorMatrix)
        imageView.colorFilter = filter
    }
}
