package com.cocwidget.acceptance

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.cocwidget.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Get help has a Questions and answers fold: what the app can't do, and why things happen.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FaqTest {
    private fun answers(root: View): TextView =
        Screens.all(root).filterIsInstance<TextView>().single { it.text.startsWith("Why doesn't it update by itself") }

    @Test
    fun theAnswersOpenFromGetHelp() {
        val root = Screens.root(Screens.open<SettingsActivity>())
        assertEquals(View.GONE, answers(root).visibility)
        Screens.tappable(root, "Questions and answers")!!.performClick()
        val faq = answers(root)
        assertEquals(View.VISIBLE, faq.visibility)
        for (asked in listOf("Is it safe for my account?", "Why can't it show my gold, elixir, Clan Capital or the Forge?", "Is there an iPhone version?")) {
            assertTrue(asked, faq.text.contains(asked))
        }
    }

    // Each question is bold, so the list reads as questions and answers.
    @Test
    fun theQuestionsAreBold() {
        val faq = answers(Screens.root(Screens.open<SettingsActivity>())).text as Spanned
        val bold = faq.getSpans(0, faq.length, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
            .map { faq.subSequence(faq.getSpanStart(it), faq.getSpanEnd(it)).toString() }
        // Each block is a question line, then its answer.
        val questions = faq.split("\n\n").map { it.substringBefore('\n') }
        assertTrue(questions.size > 5)
        assertEquals(questions, bold)
    }
}
