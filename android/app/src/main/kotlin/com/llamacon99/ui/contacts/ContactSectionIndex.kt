package com.llamacon99.ui.contacts

import java.text.Normalizer

internal data class ContactSectionTarget(val letter: String, val itemIndex: Int)

/** Every contact list starts with one header item; each section adds a header and its rows. */
internal fun contactSectionTargets(groups: List<AlphabetGroup>): List<ContactSectionTarget> {
    var itemIndex = 1
    return groups.map { group ->
        ContactSectionTarget(group.letter, itemIndex).also { itemIndex += group.contacts.size + 1 }
    }
}

internal fun sectionTargetForLetter(letter: String, targets: List<ContactSectionTarget>): Int? {
    if (targets.isEmpty()) return null
    fun key(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").uppercase()
        return if (normalized.firstOrNull() in 'A'..'Z') normalized else "~"
    }
    val requested = key(letter)
    return targets.firstOrNull { key(it.letter) == requested }?.itemIndex
        ?: targets.filter { key(it.letter) >= requested }.minByOrNull { key(it.letter) }?.itemIndex
        ?: targets.maxByOrNull { key(it.letter) }?.itemIndex
}
