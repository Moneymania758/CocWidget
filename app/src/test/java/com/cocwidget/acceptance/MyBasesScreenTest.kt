package com.cocwidget.acceptance

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import android.widget.EditText
import android.widget.ImageView
import com.cocwidget.BaseEditActivity
import com.cocwidget.BasesActivity
import com.cocwidget.GuideActivity
import com.cocwidget.MyBasesActivity
import com.cocwidget.R
import com.cocwidget.TestData
import com.cocwidget.bases.BaseKind
import com.cocwidget.bases.BaseLinks
import com.cocwidget.bases.BasePictures
import com.cocwidget.store.Store
import com.cocwidget.ui.Skin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// My bases: saving base links shared from other apps, and opening them in the game.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MyBasesScreenTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val th16Base = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH16%3AHV%3AAAAARQAAAAJPxPsGYjO2h85K_bNFQl17"
    private val th18Base = "https://link.clashofclans.com/en/?action=OpenLayout&id=TH18%3AWB%3AAAAARQAAAAJPxPsGYjO2h85K_bNFQl17"

    private fun syncTh16(): String = Store.saveExport(app, TestData.exportJson()).also { Store.setSelected(app, it) }

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun shareText(text: String) = Screens.open<BaseEditActivity> {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun tap(screen: android.app.Activity, text: String) {
        val view = Screens.tappable(Screens.root(screen), text)
        assertNotNull("Nothing tappable says $text", view)
        view!!.performClick()
        Screens.layOut(screen)
    }

    private fun texts(screen: android.app.Activity) = Screens.texts(Screens.root(screen))

    private fun fields(screen: android.app.Activity) = Screens.views<EditText>(Screens.root(screen))

    // A landscape screenshot's size, too big to keep as it is.
    private fun screenshot(): Uri {
        val bytes = ByteArrayOutputStream().also { Bitmap.createBitmap(2400, 1080, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val uri = Uri.parse("content://media/external/images/media/42")
        shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return uri
    }

    @Test
    fun aLinkSharedFromAnotherAppIsSavedForTheSelectedAccount() {
        val tag = syncTh16()
        val screen = shareText("Try my base! $th16Base")
        assertEquals(th16Base, fields(screen)[0].text.toString())
        assertTrue(texts(screen).contains(app.getString(R.string.base_link_ok, 16)))
        fields(screen)[1].setText("  Ring base ")
        tap(screen, app.getString(R.string.base_kind_war))
        tap(screen, app.getString(R.string.base_save))
        assertTrue(screen.isFinishing)
        assertEquals(app.getString(R.string.base_saved), ShadowToast.getTextOfLatestToast())

        val saved = Store.myBases(app, tag).single()
        assertEquals(th16Base, saved.link)
        assertEquals("Ring base", saved.name)
        assertEquals(BaseKind.WAR, saved.kind)
        assertEquals(tag, saved.account)
    }

    @Test
    fun sharedTextWithoutABaseLinkSaysSoAndSavesNothing() {
        syncTh16()
        val screen = shareText("https://clashcodes.com/bases/th16")
        assertTrue(texts(screen).contains(app.getString(R.string.base_shared_nothing)))
        tap(screen, app.getString(R.string.base_save))
        assertFalse(screen.isFinishing)
        assertEquals(app.getString(R.string.base_needs_link), ShadowToast.getTextOfLatestToast())
        assertTrue(Store.allBases(app).isEmpty())
        // Typing a link clears the message.
        fields(screen)[0].setText(th16Base)
        assertTrue(texts(screen).contains(app.getString(R.string.base_link_ok, 16)))
    }

    @Test
    fun myBasesListsThemAndOpensOneInTheGame() {
        val tag = syncTh16()
        val screen = shareText(th16Base)
        tap(screen, app.getString(R.string.base_save))

        val list = Screens.open<MyBasesActivity>()
        assertTrue(texts(list).contains(app.getString(R.string.my_bases_unnamed, 16)))
        assertTrue(texts(list).contains("TH16"))
        assertTrue(texts(list).contains(app.getString(R.string.my_bases_for, tag)))
        nextStarted()
        tap(list, app.getString(R.string.my_bases_open_game))
        val intent = nextStarted()
        assertEquals(BaseLinks.GAME_PACKAGE, intent?.`package`)
        assertEquals(th16Base, intent?.dataString)
        assertTrue(Store.allBases(app).single().opened > 0)
    }

    @Test
    fun aBaseAboveTheVillageIsFlaggedWhenSavedAndAsksWhenOpened() {
        syncTh16()
        val screen = shareText(th18Base)
        assertTrue(texts(screen).any { it.contains(app.getString(R.string.bases_too_high, 18, 16)) })
        tap(screen, app.getString(R.string.base_save))

        val list = Screens.open<MyBasesActivity>()
        nextStarted()
        tap(list, app.getString(R.string.my_bases_open_game))
        assertNull(nextStarted())
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        idle()
        assertEquals(th18Base, nextStarted()?.dataString)
    }

    @Test
    fun theSameLinkTwiceUpdatesTheOneBase() {
        val tag = syncTh16()
        tap(shareText(th16Base), app.getString(R.string.base_save))
        val again = shareText(th16Base)
        fields(again)[1].setText("Second go")
        tap(again, app.getString(R.string.base_save))
        assertEquals(listOf("Second go"), Store.myBases(app, tag).map { it.name })
    }

    @Test
    fun withTwoAccountsTheBaseGoesToThePickedOne() {
        val main = syncTh16()
        val second = Store.saveExport(app, TestData.th18ExportJson())
        Store.setSelected(app, main)
        val screen = shareText(th18Base)
        // TH16 is selected, so the TH18 base gets the warning until the TH18 account is picked.
        assertTrue(texts(screen).any { it.contains(app.getString(R.string.bases_too_high, 18, 16)) })
        tap(screen, second)
        assertFalse(texts(screen).any { it.contains(app.getString(R.string.bases_too_high, 18, 16)) })
        tap(screen, app.getString(R.string.base_save))

        assertTrue(Store.myBases(app, main).isEmpty())
        assertEquals(second, Store.myBases(app, second).single().account)
        assertFalse(texts(Screens.open<MyBasesActivity>()).contains(app.getString(R.string.my_bases_unnamed, 18)))
        Store.setSelected(app, second)
        assertTrue(texts(Screens.open<MyBasesActivity>()).contains(app.getString(R.string.my_bases_unnamed, 18)))
    }

    @Test
    fun editRenamesAndDeleteAsksThenRemoves() {
        val tag = syncTh16()
        tap(shareText(th16Base), app.getString(R.string.base_save))
        val id = Store.allBases(app).single().id

        val list = Screens.open<MyBasesActivity>()
        tap(list, app.getString(R.string.my_bases_edit))
        val opened = nextStarted()
        assertEquals(id, opened?.getStringExtra(BaseEditActivity.EXTRA_BASE))

        val edit = Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, id) }
        assertTrue(texts(edit).contains(app.getString(R.string.base_edit_title)))
        fields(edit)[1].setText("Anti 3 star")
        tap(edit, app.getString(R.string.base_kind_cwl))
        tap(edit, app.getString(R.string.base_save))
        val renamed = Store.myBases(app, tag).single()
        assertEquals(id, renamed.id)
        assertEquals("Anti 3 star", renamed.name)
        assertEquals(BaseKind.CWL, renamed.kind)

        val again = Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, id) }
        tap(again, app.getString(R.string.base_delete))
        assertTrue(Store.allBases(app).isNotEmpty())
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        idle()
        assertTrue(Store.allBases(app).isEmpty())
        assertTrue(again.isFinishing)
    }

    @Suppress("DEPRECATION")
    private fun back(screen: android.app.Activity) {
        screen.onBackPressed()
        idle()
    }

    @Test
    fun backFromAnEmptyFormJustCloses() {
        syncTh16()
        val add = Screens.open<BaseEditActivity>()
        back(add)
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
        assertTrue(add.isFinishing)
    }

    @Test
    fun backFromAFilledFormAsksBeforeDroppingIt() {
        syncTh16()
        val add = Screens.open<BaseEditActivity>()
        fields(add)[0].setText(th16Base)
        fields(add)[1].setText("War base")
        back(add)
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)
        assertEquals(app.getString(R.string.base_discard_new), shadowOf(dialog).title)

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        idle()
        assertFalse(add.isFinishing)
        assertEquals("War base", fields(add)[1].text.toString())

        // The back arrow asks the same.
        Screens.all(Screens.root(add)).first { it.contentDescription == app.getString(R.string.back) }.performClick()
        idle()
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        idle()
        assertTrue(add.isFinishing)
        assertTrue(Store.allBases(app).isEmpty())
    }

    @Test
    fun editingAsksOnlyWhenSomethingChanged() {
        syncTh16()
        tap(shareText(th16Base), app.getString(R.string.base_save))
        val id = Store.allBases(app).single().id

        val unchanged = Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, id) }
        back(unchanged)
        assertTrue(unchanged.isFinishing)

        val renamed = Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, id) }
        fields(renamed)[1].setText("Anti 3 star")
        back(renamed)
        assertFalse(renamed.isFinishing)
        assertEquals(app.getString(R.string.base_discard_changes), shadowOf(ShadowAlertDialog.getLatestAlertDialog()).title)
    }

    @Test
    fun aBaseThatsGoneClosesTheEditor() {
        assertTrue(Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, "gone") }.isFinishing)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun aSharedScreenshotIsKeptSmallerWithTheBase() {
        val tag = syncTh16()
        val screen = Screens.open<BaseEditActivity> {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, screenshot())
            putExtra(Intent.EXTRA_TEXT, th16Base)
        }
        screen.importing!!.join()
        idle()
        Screens.layOut(screen)
        assertTrue(texts(screen).contains(app.getString(R.string.base_picture_change)))
        tap(screen, app.getString(R.string.base_save))

        val base = Store.myBases(app, tag).single()
        val file = BasePictures.file(app, base.id)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        assertEquals(1600, bounds.outWidth)
        assertEquals(720, bounds.outHeight)

        val list = Screens.open<MyBasesActivity>()
        val description = app.getString(R.string.base_picture_of, app.getString(R.string.my_bases_unnamed, 16))
        assertTrue(Screens.views<ImageView>(Screens.root(list)).any { it.contentDescription == description })

        // Removing it in the editor deletes the file on save.
        val edit = Screens.open<BaseEditActivity> { putExtra(BaseEditActivity.EXTRA_BASE, base.id) }
        tap(edit, app.getString(R.string.base_picture_remove))
        tap(edit, app.getString(R.string.base_save))
        assertFalse(file.exists())
    }

    @Test
    fun onlySharedContentIsReadAsAPicture() {
        syncTh16()
        val screen = Screens.open<BaseEditActivity> {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///data/data/com.cocwidget/shared_prefs/village.xml"))
        }
        assertNull(screen.importing)
    }

    @Test
    fun removingAnAccountRemovesItsBases() {
        val tag = syncTh16()
        tap(shareText(th16Base), app.getString(R.string.base_save))
        val id = Store.allBases(app).single().id
        val picture = BasePictures.file(app, id).apply {
            parentFile!!.mkdirs()
            writeText("x")
        }
        Store.removeAccount(app, tag)
        assertTrue(Store.allBases(app).isEmpty())
        assertFalse(picture.exists())
    }

    @Test
    fun theBasesTabLeadsHereAndHereLeadsToTheSites() {
        val guide = Screens.open<GuideActivity>()
        nextStarted()
        tap(guide, app.getString(R.string.tab_bases))
        assertEquals(MyBasesActivity::class.java.name, nextStarted()?.component?.className)

        val list = Screens.open<MyBasesActivity>()
        assertTrue(texts(list).contains(app.getString(R.string.my_bases_empty)))
        nextStarted()
        tap(list, app.getString(R.string.bases_find))
        val find = nextStarted()
        assertEquals(BasesActivity::class.java.name, find?.component?.className)
        assertTrue(find!!.getBooleanExtra(BasesActivity.EXTRA_AS_TAB, false))
        assertTrue("My bases makes way for Find bases", list.isFinishing)

        val again = Screens.open<MyBasesActivity>()
        nextStarted()
        tap(again, app.getString(R.string.my_bases_add_plus))
        assertEquals(BaseEditActivity::class.java.name, nextStarted()?.component?.className)
    }

    @Test
    fun opensInEverySkin() {
        syncTh16()
        tap(shareText(th16Base), app.getString(R.string.base_save))
        for (skin in Skin.ALL) {
            Store.setSkin(app, skin.key)
            assertTrue(texts(Screens.open<MyBasesActivity>()).contains(app.getString(R.string.my_bases_unnamed, 16)))
            assertTrue(texts(Screens.open<BaseEditActivity>()).contains(app.getString(R.string.base_add_title)))
        }
    }
}
