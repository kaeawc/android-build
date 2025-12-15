package dev.jasonpearson.design.system.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jasonpearson.design.system.components.JPBackButton
import dev.jasonpearson.design.system.components.JPBodyText
import dev.jasonpearson.design.system.components.JPBottomNavigation
import dev.jasonpearson.design.system.components.JPButton
import dev.jasonpearson.design.system.components.JPCard
import dev.jasonpearson.design.system.components.JPExtendedFloatingActionButton
import dev.jasonpearson.design.system.components.JPFloatingActionButton
import dev.jasonpearson.design.system.components.JPHeadline
import dev.jasonpearson.design.system.components.JPLabel
import dev.jasonpearson.design.system.components.JPOutlinedButton
import dev.jasonpearson.design.system.components.JPOutlinedCard
import dev.jasonpearson.design.system.components.JPOutlinedTextField
import dev.jasonpearson.design.system.components.JPSecondaryButton
import dev.jasonpearson.design.system.components.JPTextButton
import dev.jasonpearson.design.system.components.JPTextField
import dev.jasonpearson.design.system.components.JPTitle
import dev.jasonpearson.design.system.components.JPTopAppBar
import dev.jasonpearson.design.system.components.BottomNavItem
import dev.jasonpearson.design.system.theme.JPDimensions
import dev.jasonpearson.design.system.theme.JPTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemDemoScreen(onBackClick: () -> Unit = {}) {
  var textFieldValue by remember { mutableStateOf("Sample text") }
  var outlinedTextFieldValue by remember { mutableStateOf("Outlined sample") }

  JPTheme {
    Scaffold(
        topBar = {
          JPTopAppBar(
              title = { JPTitle("Design System Demo") },
              navigationIcon = { JPBackButton(onBackClick = onBackClick) })
        }) { paddingValues ->
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .padding(JPDimensions.spacing4)
                      .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(JPDimensions.spacing4)) {
                // Typography Section
                JPCard {
                  JPHeadline("Typography")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))
                  JPTitle("Title Text")
                  JPBodyText("This is body text that demonstrates the typography system.")
                  JPLabel("Label Text")
                }

                // Buttons Section
                JPCard {
                  JPHeadline("Buttons")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))

                  Column(
                      verticalArrangement = Arrangement.spacedBy(JPDimensions.spacing2)) {
                        JPButton(
                            text = "Primary Button",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth())

                        JPSecondaryButton(
                            text = "Secondary Button",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth())

                        JPOutlinedButton(
                            text = "Outlined Button",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth())

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(JPDimensions.spacing2)) {
                              JPTextButton(text = "Text Button", onClick = {})

                              JPButton(text = "Disabled", onClick = {}, enabled = false)
                            }
                      }
                }

                // Cards Section
                JPOutlinedCard {
                  JPHeadline("Cards")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))
                  JPBodyText(
                      "This is an outlined card variant. Cards follow flat design principles and avoid nesting.")
                }

                // Text Fields Section
                JPCard {
                  JPHeadline("Text Fields")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))

                  Column(
                      verticalArrangement = Arrangement.spacedBy(JPDimensions.spacing2)) {
                        JPTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            label = { Text("Filled Text Field") },
                            modifier = Modifier.fillMaxWidth())

                        JPOutlinedTextField(
                            value = outlinedTextFieldValue,
                            onValueChange = { outlinedTextFieldValue = it },
                            label = { Text("Outlined Text Field") },
                            modifier = Modifier.fillMaxWidth())
                      }
                }

                // Color Showcase
                JPCard {
                  JPHeadline("Color System")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))
                  JPBodyText("Primary: Black (#000000)")
                  JPBodyText("Secondary: Red (#FF0000)")
                  JPBodyText("Background: Eggshell (#F8F8FF)")
                  JPBodyText(text = "This text uses the primary color")
                  JPBodyText(text = "This text uses the secondary color")
                }

                // Floating Action Buttons Section
                JPCard {
                  JPHeadline("Floating Action Buttons")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))

                  Row(horizontalArrangement = Arrangement.spacedBy(JPDimensions.spacing2)) {
                    JPFloatingActionButton(
                        onClick = {}, icon = Icons.Default.Add, contentDescription = "Add")

                    JPExtendedFloatingActionButton(
                        text = "Add Item",
                        onClick = {},
                        icon = Icons.Default.Add,
                        contentDescription = "Add Item")
                  }
                }

                // Bottom Navigation Section
                JPCard {
                  JPHeadline("Bottom Navigation")
                  Spacer(modifier = Modifier.height(JPDimensions.spacing2))

                  val bottomNavItems =
                      listOf(
                          BottomNavItem("Home", Icons.Default.Home),
                          BottomNavItem("Search", Icons.Default.Search),
                          BottomNavItem("Profile", Icons.Default.Person),
                          BottomNavItem("Settings", Icons.Default.Settings))

                  JPBottomNavigation(
                      items = bottomNavItems, selectedItemIndex = 0, onItemSelected = {})
                }

                Spacer(modifier = Modifier.height(JPDimensions.spacing8))
              }
        }
  }
}

@Preview(showBackground = true)
@Composable
private fun DesignSystemDemoScreenPreview() {
  DesignSystemDemoScreen()
}
