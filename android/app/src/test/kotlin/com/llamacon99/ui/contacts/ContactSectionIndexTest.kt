package com.llamacon99.ui.contacts

import com.llamacon99.model.ContactPhoneNumber
import com.llamacon99.model.DeviceContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactSectionIndexTest {
    private fun group(letter: String, count: Int) = AlphabetGroup(letter, List(count) { index ->
        val number = ContactPhoneNumber("mobile", "5123000$index")
        ContactNumberEntry(DeviceContact("$letter$index", "$letter$index", listOf(number)), number)
    })

    @Test fun `jumps account for screen header section headers and variable row counts`() {
        val targets = contactSectionTargets(listOf(group("A", 3), group("C", 1), group("Z", 8)))
        assertEquals(listOf(1, 5, 7), targets.map { it.itemIndex })
        assertEquals(5, sectionTargetForLetter("B", targets))
        assertEquals(7, sectionTargetForLetter("Z", targets))
    }

    @Test fun `index handles accents numeric names and absent letters independently of section order`() {
        val targets = listOf(ContactSectionTarget("#", 1), ContactSectionTarget("C", 4),
            ContactSectionTarget("Z", 8), ContactSectionTarget("Á", 10))
        assertEquals(10, sectionTargetForLetter("A", targets))
        assertEquals(4, sectionTargetForLetter("B", targets))
        assertEquals(1, sectionTargetForLetter("#", targets))
        assertEquals(8, sectionTargetForLetter("Y", targets))
    }

    @Test fun `empty filtered results have no jump target`() {
        assertNull(sectionTargetForLetter("A", emptyList()))
    }
}
