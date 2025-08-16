package com.retrica

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter.Companion.colorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retrica.sample_compose.R
import com.retrica.sample_compose.theme.RetricaSampleAppTheme

class MainComposeActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetricaSampleAppTheme {

                val state: MainComposeUiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current

                val bitmap = remember {
                    drawableToBitmap(
                        ContextCompat.getDrawable(
                            context,
                            state.drawable
                        )!!
                    ).asImageBitmap()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    bottomBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !state.isReverted,
                                onClick = viewModel::toggleGrayScale
                            ) {
                                if (state.isReverted) {
                                    Text(
                                        fontSize = 10.sp,
                                        text = stringResource(if(state.cachedIsGray) R.string.gray_remove else R.string.gray_apply)
                                    )
                                } else {
                                    Text(
                                        fontSize = 10.sp,
                                        text = stringResource(if(state.isGray) R.string.gray_remove else R.string.gray_apply)
                                    )
                                }

                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !state.isReverted,
                                onClick = viewModel::toggleBrightness
                            ) {
                                if (state.isReverted) {
                                    Text(
                                        fontSize = 10.sp,
                                        text = stringResource(if (state.cachedIsBright)R.string.bright_decrease else R.string.bright_increase)
                                    )
                                } else {
                                    Text(
                                        fontSize = 10.sp,
                                        text = stringResource(if (state.isBright)R.string.bright_decrease else R.string.bright_increase)
                                    )
                                }
                            }

                            Button(
                                modifier = Modifier.weight(1.5f),
                                onClick = viewModel::reset
                            ) {
                                Text(
                                    fontSize = 10.sp,
                                    text = if (state.isReverted) "복원하기" else "되돌리기"
                                )

                            }
                        }
                    },
                    content = { innerPadding ->

                        Image(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            bitmap = bitmap,
                            contentDescription = null,
                            colorFilter = colorMatrix(state.colorMatrix)
                        )
                    }
                )
            }

        }
    }
}


fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = createBitmap(drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
        drawable.intrinsicHeight.takeIf { it > 0 } ?: 1)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}