package xyz.zedler.patrick.tack.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), ServiceConnection {

  private val viewModel: MainViewModel by viewModels {
    val app = application as TackApplication
    MainViewModel.Factory(app.settingsRepository, app.songRepository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.surface
        ) {
          // UI will be implemented here
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    Intent(this, MetronomeService::class.java).also { intent ->
      bindService(intent, this, BIND_AUTO_CREATE)
    }
  }

  override fun onStop() {
    super.onStop()
    unbindService(this)
    viewModel.onServiceDisconnected()
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    val binder = service as MetronomeService.MetronomeBinder
    viewModel.onServiceConnected(binder.getService())
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    viewModel.onServiceDisconnected()
  }
}
