package dev.jasonpearson.design.system.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import dev.jasonpearson.design.system.theme.JPDimensions
import dev.jasonpearson.design.system.theme.JPTheme

@Composable
fun JPFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null
) {
  FloatingActionButton(
      onClick = onClick,
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      elevation = FloatingActionButtonDefaults.elevation()) {
        Icon(imageVector = icon, contentDescription = contentDescription)
      }
}

@Composable
fun JPSmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null
) {
  SmallFloatingActionButton(
      onClick = onClick,
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      elevation = FloatingActionButtonDefaults.elevation()) {
        Icon(imageVector = icon, contentDescription = contentDescription)
      }
}

@Composable
fun JPExtendedFloatingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
  ExtendedFloatingActionButton(
      onClick = onClick,
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      elevation = FloatingActionButtonDefaults.elevation()) {
        if (icon != null) {
          Icon(imageVector = icon, contentDescription = contentDescription)
        }
        Text(text = text)
      }
}

@Preview(showBackground = true)
@Composable
private fun JPFloatingActionButtonPreview() {
  JPTheme {
    Column(verticalArrangement = Arrangement.spacedBy(JPDimensions.spacing4)) {
      JPFloatingActionButton(
          onClick = {}, icon = Icons.Default.Add, contentDescription = "Add")

      JPSmallFloatingActionButton(
          onClick = {}, icon = Icons.Default.Add, contentDescription = "Add")

      JPExtendedFloatingActionButton(
          text = "Add Item", onClick = {}, icon = Icons.Default.Add, contentDescription = "Add")
    }
  }
}
