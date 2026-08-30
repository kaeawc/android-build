/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.feature.login

/** Pure, framework-free UI state for the login screen. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    /** True when the form holds valid credentials and is not already submitting. */
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH && !isSubmitting

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}

/** The state a freshly opened login screen starts from. */
fun initialLoginState(): LoginUiState = LoginUiState()

/** Pure reducers that transform [LoginUiState] in response to user intent. */
object LoginReducer {
    fun username(state: LoginUiState, value: String): LoginUiState =
        state.copy(username = value, error = null)

    fun password(state: LoginUiState, value: String): LoginUiState =
        state.copy(password = value, error = null)

    fun submit(state: LoginUiState): LoginUiState =
        if (state.canSubmit) {
            state.copy(isSubmitting = true, error = null)
        } else {
            state.copy(
                error =
                    "Enter a username and a password of at least " +
                        "${LoginUiState.MIN_PASSWORD_LENGTH} characters."
            )
        }
}
