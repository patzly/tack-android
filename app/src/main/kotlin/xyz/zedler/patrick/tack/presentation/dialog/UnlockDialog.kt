package xyz.zedler.patrick.tack.presentation.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockDialog(
  onDismissRequest: () -> Unit = {}
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
      Text(stringResource(R.string.msg_unlock))
    },
    text = {
      Text(stringResource(R.string.msg_unlock_description))
    },
    confirmButton = {
      TextButton(
        onClick = {
          onDismissRequest()
        },
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_open_play_store))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismissRequest()
        },
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    }
  )
}

@Preview
@Composable
fun UnlockDialogPreview() {
  TackTheme {
    UnlockDialog()
  }
}
