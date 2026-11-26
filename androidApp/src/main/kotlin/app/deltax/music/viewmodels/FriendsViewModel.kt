package app.deltax.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deltax.music.shared.viewmodels.FRIENDS_REFRESH_INTERVAL_MS
import app.deltax.music.shared.viewmodels.FriendsViewModel

class FriendsViewModel : ViewModel() {
    val sharedViewModel =
        FriendsViewModel(viewModelScope)
    val uiState = sharedViewModel.uiState

    fun startPolling(intervalMs: Long = FRIENDS_REFRESH_INTERVAL_MS) = sharedViewModel.startPolling(intervalMs)

    fun stopPolling() = sharedViewModel.stopPolling()

    override fun onCleared() {
        sharedViewModel.stopPolling()
    }
}
