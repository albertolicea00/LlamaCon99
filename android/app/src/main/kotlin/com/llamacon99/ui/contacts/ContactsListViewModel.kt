package com.llamacon99.ui.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamacon99.data.ContactsRepository
import com.llamacon99.data.DialRepository
import com.llamacon99.data.SettingsRepository
import com.llamacon99.model.DeviceContact
import com.llamacon99.telemetry.AppTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One A-Z section of the contacts list, mirroring iOS's `Section(group.letter)`. */
data class AlphabetGroup(
    val letter: String,
    val contacts: List<DeviceContact>,
)

data class ContactsListUiState(
    val groups: List<AlphabetGroup> = emptyList(),
    val anonymousSwipeEnabled: Boolean = false,
    val isEmpty: Boolean = false,
)

private const val COLLECT_CALL_PREFIX = "*99"
private const val ANONYMOUS_CALL_PREFIX = "#31#"

@HiltViewModel
class ContactsListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val dialRepository: DialRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = searchQuery.asStateFlow()

    val uiState: StateFlow<ContactsListUiState> = combine(
        contactsRepository.contacts,
        searchQuery,
        settingsRepository.anonymousSwipeEnabled,
    ) { contacts, query, anonymousSwipeEnabled ->
        val filtered = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                    contact.numbers.any { it.number.contains(query) }
            }
        }

        val grouped = filtered
            .sortedBy { it.name.lowercase() }
            .groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
            .toSortedMap()
            .map { (letter, contacts) -> AlphabetGroup(letter, contacts) }

        ContactsListUiState(
            groups = grouped,
            anonymousSwipeEnabled = anonymousSwipeEnabled,
            isEmpty = contacts.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsListUiState())

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val trace = AppTelemetry.startTrace(context, "contacts_load")
            try {
                contactsRepository.reload()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppTelemetry.recordNonFatal(context, error, "contacts_reload_failed")
            } finally {
                trace?.stop()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onDial(number: String) = dial(COLLECT_CALL_PREFIX + number)

    fun onDialAnonymous(number: String) = dial(ANONYMOUS_CALL_PREFIX + number)

    private fun dial(code: String) {
        viewModelScope.launch {
            when (settingsRepository.dialMode.first()) {
                SettingsRepository.DialMode.DIRECT -> dialRepository.callDirect(code)
                SettingsRepository.DialMode.PREFILL -> dialRepository.openDialerPrefilled(code)
            }
        }
    }
}
