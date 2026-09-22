package lk.jmcinnovators.learning.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.jmcinnovators.learning.R
import lk.jmcinnovators.learning.ui.theme.JmcPalette
import lk.jmcinnovators.learning.utils.rememberIsOnline
import lk.jmcinnovators.learning.viewmodel.AuthViewModel
import lk.jmcinnovators.learning.viewmodel.LoginUiState
import lk.jmcinnovators.learning.viewmodel.ViewModelFactory

@Composable
fun LoginScreen(
    factory: ViewModelFactory,
    onSignedIn: (hasProfile: Boolean) -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by rememberIsOnline()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.SignedIn) onSignedIn(state.hasProfile)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(JmcPalette.DarkBg0, JmcPalette.DarkBg1, JmcPalette.Indigo.copy(alpha = 0.25f))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.jmc_logo),
                contentDescription = stringResource(R.string.cd_logo),
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = JmcPalette.DarkText0
            )
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = JmcPalette.DarkText1
            )
            Spacer(Modifier.height(40.dp))

            when (val state = uiState) {
                is LoginUiState.Loading -> {
                    GoogleSignInButton(
                        enabled = false,
                        loading = true,
                        onClick = {}
                    )
                }
                is LoginUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    GoogleSignInButton(
                        enabled = isOnline,
                        loading = false,
                        onClick = { viewModel.signInWithGoogle(context) }
                    )
                }
                else -> {
                    GoogleSignInButton(
                        enabled = isOnline,
                        loading = false,
                        onClick = { viewModel.signInWithGoogle(context) }
                    )
                }
            }

            if (!isOnline) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "You're offline — please connect to the internet to sign in.",
                    color = JmcPalette.DarkText2,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "By continuing you agree to JMC Innovators' privacy practices for student data.",
                style = MaterialTheme.typography.labelLarge,
                color = JmcPalette.DarkText2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = androidx.compose.ui.graphics.Color(0xFF1F1F1F),
            disabledContainerColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
            disabledContentColor = androidx.compose.ui.graphics.Color(0xFF757575)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = androidx.compose.ui.graphics.Color(0xFF1F1F1F)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Connecting to Google...",
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color(0xFF1F1F1F)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color(0xFF1F1F1F)
                )
            }
        }
    }
}
