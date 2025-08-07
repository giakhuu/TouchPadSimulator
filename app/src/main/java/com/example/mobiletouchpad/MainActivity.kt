package com.example.mobiletouchpad

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobiletouchpad.ui.theme.MobileTouchPadTheme
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    private var isBound = false
    private var socketService: SocketService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            // 4. Lấy instance Service từ binder
            socketService = (binder as SocketService.LocalBinder).getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName) {
            socketService = null
            isBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi kết nối TCP trong background
        val intent = Intent(this, SocketService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        // (Nếu cần chạy foreground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }

        setContent {
            LockLandscape()
            MobileTouchPadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Grid(innerPadding, ::onDragStart, ::onDragMove, ::onDragEnd, ::onClick)
                }
            }
        }
    }



    private fun onDragStart(offset: Offset) { /* nếu cần */ }

    private fun onDragMove(position: Offset, delta: Offset) {
        val dx = delta.x.toInt().toByte()
        val dy = delta.y.toInt().toByte()
        println("$dx $dy")
        socketService?.sendPacket(byteArrayOf(0x00, dx, dy))
    }
    private fun onClick(type: String) {
        val code = if (type=="LEFTCLICK") 0x01.toByte() else 0x02.toByte()
        socketService?.sendPacket(byteArrayOf(code))
    }

    private fun onDragEnd() { /* nếu cần */ }
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}


@Composable
fun Grid(
    innerPadding: PaddingValues,
    onDragStart: (Offset)->Unit,
    onDragMove:  (Offset,Offset)->Unit,
    onDragEnd:   ()->Unit,
    onClick:     (String)->Unit
) {
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
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Pad(
                    modifier = modifier.weight(1f),
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
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
                    onClick = {onClick("LEFTCLICK")}
                )
                Spacer(modifier = Modifier.width(10.dp))
                ClickButton(
                    modifier.weight(1f),
                    "Right Click",
                    onClick = {onClick("RIGHTCLICK")}
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
    Grid(
        PaddingValues(),
        onDragStart = TODO(),
        onDragMove = TODO(),
        onDragEnd = TODO(),
        onClick = TODO()
    )
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
