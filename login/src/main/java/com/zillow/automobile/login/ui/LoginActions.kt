package dev.jasonpearson.login.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jasonpearson.design.system.components.JPButton
import dev.jasonpearson.design.system.components.JPOutlinedButton
import dev.jasonpearson.design.system.theme.JPDimensions
import dev.jasonpearson.design.system.theme.JPTheme
import dev.jasonpearson.login.R

/** Action buttons section containing sign in button, loading indicator, and guest mode button. */
@Composable
internal fun LoginActions(
    isFormValid: Boolean,
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    onGuestModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    AnimatedVisibility(visible = isFormValid && !isLoading, enter = fadeIn(), exit = fadeOut()) {
      JPButton(
          text = stringResource(R.string.action_sign_in),
          onClick = onSignInClick,
          modifier = Modifier.wrapContentWidth())
    }

    if (isLoading) {
      Spacer(modifier = Modifier.height(JPDimensions.spacing4))
      CircularProgressIndicator()
    }

    Spacer(modifier = Modifier.height(JPDimensions.spacing4))

    JPOutlinedButton(
        text = "Continue as Guest",
        onClick = onGuestModeClick,
        modifier = Modifier.wrapContentWidth())
  }
}

@Preview(name = "Login Actions", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(
    name = "Login Actions - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginActionsPreview() {
  JPTheme {
    LoginActions(isFormValid = true, isLoading = false, onSignInClick = {}, onGuestModeClick = {})
  }
}
