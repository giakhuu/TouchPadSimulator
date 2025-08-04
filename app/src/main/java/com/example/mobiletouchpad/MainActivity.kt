package com.example.mobiletouchpad

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobiletouchpad.ui.theme.MobileTouchPadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockLandscape()
            MobileTouchPadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Grid(innerPadding)
                }
            }
        }
    }
}

@Composable
fun Grid(innerPadding: PaddingValues) {
    val shape = RoundedCornerShape(10.dp)
    val modifier = Modifier
        .fillMaxHeight()
        .border(
            BorderStroke(2.dp, Color.Gray),
            shape = shape
        )
        .clip(shape)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Gray)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(20.dp, 4.dp)
            ) {
                Pad(
                    modifier = modifier.weight(3f),
                    onDragStart = dragStart,
                    onDragMove = dragMove,
                    onDragEnd = dragEnd,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Pad(
                    modifier = modifier.weight(1f),
                    onDragStart = dragStart,
                    onDragMove = dragMove,
                    onDragEnd = dragEnd,
                )
            }
            Row (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp, 4.dp)
            ) {
                ClickButton(
                    modifier.weight(1f),
                    "Left Click",
                    onClick = {clickEvent("LEFTCLICK")}
                )
                Spacer(modifier = Modifier.width(10.dp))
                ClickButton(
                    modifier.weight(1f),
                    "Right Click",
                    onClick = {clickEvent("LEFTCLICK")}
                )
            }
        }

    }
}

@Composable
fun Pad(
    modifier: Modifier,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier
            .background(color = Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        onDragStart(startOffset)
                    },
                    onDrag = { change, dragAmount ->
                        // tránh sự kiện rò rỉ xuống child khác
                        change.consume()

                        // change.position: tọa độ hiện tại
                        // dragAmount: delta lần này
                        onDragMove(change.position, dragAmount)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
    ) {
        Text(
            text = "TouchPad",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,                            // hoặc thiết lập tuỳ theo thiết kế
            modifier = Modifier
                .align(Alignment.BottomCenter)            // căn text ở cuối hộp theo chiều dọc, giữa theo chiều ngang
                .padding(bottom = 8.dp)
        )
    }

}


@Composable
fun ClickButton(
    modifier: Modifier,
    text: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(color = Color.Blue)
            .clickable(onClick = {onClick()})
    ) {
        Text(
            text = text,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 24.sp,                            // hoặc thiết lập tuỳ theo thiết kế
            modifier = Modifier
                .align(Alignment.Center)            // căn text ở cuối hộp theo chiều dọc, giữa theo chiều ngang
                .padding(bottom = 8.dp)
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420,orientation=landscape"
)
@Composable
fun GridPreview() {
    Grid(PaddingValues())
}


@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun LockLandscape() {
    val activity = LocalContext.current as? ComponentActivity ?: return
    DisposableEffect(Unit) {
        val old = activity.requestedOrientation
        // You can use SCREEN_ORIENTATION_LOCKED for full lock
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity.requestedOrientation = old
        }
    }
}
