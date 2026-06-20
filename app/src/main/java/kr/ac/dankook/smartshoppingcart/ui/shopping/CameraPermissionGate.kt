package kr.ac.dankook.smartshoppingcart.ui.shopping

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import kr.ac.dankook.smartshoppingcart.R

@Composable
fun CameraPermissionGate(
    marketName: String,
    onChangeMarket: () -> Unit,
    onOpenMarketInfo: () -> Unit,
    onCheckout: (List<RecognizedProduct>) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var requestedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        requestedPermission = true
    }

    if (hasCameraPermission) {
        ShoppingCameraScreen(
            marketName = marketName,
            onChangeMarket = onChangeMarket,
            onOpenMarketInfo = onOpenMarketInfo,
            onCheckout = onCheckout
        )
    } else {
        CameraPermissionScreen(
            permissionWasDenied = requestedPermission,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onChangeMarket = onChangeMarket
        )

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    permissionWasDenied: Boolean,
    onRequestPermission: () -> Unit,
    onChangeMarket: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.screen_horizontal_padding)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = if (permissionWasDenied) {
                stringResource(R.string.camera_permission_denied_message)
            } else {
                stringResource(R.string.camera_permission_message)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.action_allow))
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.list_item_spacing)))
        OutlinedButton(onClick = onChangeMarket) {
            Text(text = stringResource(R.string.action_change_market))
        }
    }
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
