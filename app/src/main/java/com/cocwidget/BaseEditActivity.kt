package com.cocwidget

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.cocwidget.bases.BaseKind
import com.cocwidget.bases.BaseLinks
import com.cocwidget.bases.BasePictures
import com.cocwidget.bases.MyBases
import com.cocwidget.bases.SavedBase
import com.cocwidget.store.Store
import com.cocwidget.ui.Screen
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

// Adds a base to My bases or changes one. Also what other apps' Share menus open, with a base link,
// a screenshot or both.
class BaseEditActivity : Screen() {
    private var editing: SavedBase? = null
    private var link = ""
    private var name = ""
    private var kind: BaseKind? = null
    private var account = ""
    // A new picture not saved yet, and whether the saved one is to go.
    private var draft: File? = null
    private var pictureRemoved = false
    // Something was shared that had no base link in it.
    private var sharedNothing = false
    private var accountTownHall = 0
    private lateinit var linkStatus: TextView

    // The picture being copied in, so tests can wait for it.
    internal var importing: Thread? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = intent.action == Intent.ACTION_SEND
        editing = intent.getStringExtra(EXTRA_BASE)?.takeIf { !shared }?.let { id -> Store.allBases(this).firstOrNull { it.id == id } }
        if (intent.hasExtra(EXTRA_BASE) && !shared && editing == null) {
            finish()
            return
        }
        when {
            savedInstanceState != null -> restore(savedInstanceState)
            editing != null -> editing?.let {
                link = it.link
                name = it.name
                kind = it.kind
                account = it.account
            }
            else -> {
                account = Store.selected(this).orEmpty()
                if (shared) receive(intent)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_LINK, link)
        outState.putString(STATE_NAME, name)
        outState.putString(STATE_KIND, kind?.key)
        outState.putString(STATE_ACCOUNT, account)
        outState.putString(STATE_DRAFT, draft?.path)
        outState.putBoolean(STATE_REMOVED, pictureRemoved)
        outState.putBoolean(STATE_NOTHING, sharedNothing)
    }

    private fun restore(state: Bundle) {
        link = state.getString(STATE_LINK).orEmpty()
        name = state.getString(STATE_NAME).orEmpty()
        kind = BaseKind.of(state.getString(STATE_KIND))
        account = state.getString(STATE_ACCOUNT).orEmpty()
        draft = state.getString(STATE_DRAFT)?.let(::File)?.takeIf { it.exists() }
        pictureRemoved = state.getBoolean(STATE_REMOVED)
        sharedNothing = state.getBoolean(STATE_NOTHING)
    }

    override fun onDestroy() {
        // Left without saving: the picked picture isn't wanted.
        if (isFinishing) draft?.delete()
        super.onDestroy()
    }

    // The link from shared text, and a shared picture. Only content links are read, so nothing
    // of the app's own files can be passed in as a picture.
    private fun receive(intent: Intent) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        BaseLinks.find(text)?.let { link = it }
        sharedNothing = link.isEmpty() && !text.isNullOrBlank()
        val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (stream?.scheme == "content") importPicture(stream)
    }

    override fun build() {
        accountTownHall = account.takeIf { it.isNotEmpty() }?.let { Store.village(this, it)?.townHall } ?: 0
        content.addView(backHeader(if (editing == null) R.string.base_add_title else R.string.base_edit_title))
        content.addView(ui.card().apply {
            addView(label(R.string.base_link))
            val linkField = field(link, getString(R.string.base_link_hint), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI) {
                if (it != link) sharedNothing = false
                link = it
                showLinkStatus()
            }
            addView(LinearLayout(this@BaseEditActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(linkField, ui.weighted())
                addView(ui.button(getString(R.string.base_paste), null, primary = false) { paste(linkField) }, ui.wrap().apply { marginStart = ui.dp(10) })
            }, ui.matchWidth(4))
            linkStatus = ui.text("", 13f)
            addView(linkStatus, ui.matchWidth(6))
            showLinkStatus()

            addView(label(R.string.base_name), ui.matchWidth(18))
            addView(field(name, getString(R.string.base_name_hint), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) { name = it }, ui.matchWidth(4))

            addView(label(R.string.base_kind), ui.matchWidth(18))
            addView(ui.flow(8).apply {
                for (each in BaseKind.entries) {
                    addView(pick(getString(each.label), each == kind) {
                        kind = if (kind == each) null else each
                        show()
                    })
                }
            }, ui.matchWidth(8))

            val accounts = Store.accounts(this@BaseEditActivity)
            if (accounts.size >= 2) {
                addView(label(R.string.base_account), ui.matchWidth(18))
                addView(ui.flow(8).apply {
                    for (tag in accounts) {
                        addView(pick(Store.label(this@BaseEditActivity, tag), tag == account) {
                            account = tag
                            show()
                        })
                    }
                }, ui.matchWidth(8))
            }
        }, ui.matchWidth(12))
        content.addView(pictureCard(), ui.matchWidth(16))
        content.addView(ui.button(getString(R.string.base_save), null, primary = true) { save() }, ui.matchWidth(16))
        if (editing != null) {
            content.addView(ui.button(getString(R.string.base_delete), null, primary = false) { confirmDelete() }, ui.matchWidth(10))
        }
    }

    // Says what the link is for, or what's wrong with it, as it's typed.
    private fun showLinkStatus() {
        val found = BaseLinks.find(link)
        val townHall = found?.let(BaseLinks::townHall) ?: 0
        val (text, colour) = when {
            found != null && accountTownHall > 0 && townHall > accountTownHall ->
                getString(R.string.base_link_ok, townHall) + "\n" + getString(R.string.bases_too_high, townHall, accountTownHall) to getColor(R.color.idle)
            found != null && townHall > 0 -> getString(R.string.base_link_ok, townHall) to ui.skin.green
            found != null -> getString(R.string.base_link_ok_plain) to ui.skin.green
            sharedNothing -> getString(R.string.base_shared_nothing) to getColor(R.color.idle)
            link.isBlank() -> getString(R.string.base_link_empty) to ui.skin.dim
            else -> getString(R.string.base_link_bad) to getColor(R.color.idle)
        }
        linkStatus.text = text
        linkStatus.setTextColor(colour)
    }

    private fun paste(field: EditText) {
        val copied = getSystemService(ClipboardManager::class.java)?.primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (copied.isBlank()) {
            Toast.makeText(this, R.string.base_paste_empty, Toast.LENGTH_SHORT).show()
            return
        }
        field.setText(BaseLinks.find(copied) ?: copied.trim())
        field.setSelection(field.text.length)
    }

    private fun pictureCard() = ui.card().apply {
        addView(label(R.string.base_picture))
        val saved = editing?.let { BasePictures.file(this@BaseEditActivity, it.id) }?.takeIf { !pictureRemoved && it.exists() }
        val shown = draft ?: saved
        val bitmap = shown?.let { BasePictures.load(it, resources.displayMetrics.widthPixels) }
        if (bitmap != null) {
            addView(ImageView(this@BaseEditActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                background = ui.rounded(ui.skin.chip, 10f)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                setImageBitmap(bitmap)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ui.dp(180)).apply { topMargin = ui.dp(8) })
            addView(LinearLayout(this@BaseEditActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(ui.button(getString(R.string.base_picture_change), null, primary = false) { pickPicture() }, ui.weighted())
                addView(ui.button(getString(R.string.base_picture_remove), null, primary = false) { removePicture() }, ui.weighted(10))
            }, ui.matchWidth(12))
        } else {
            addView(ui.text(getString(R.string.base_picture_note), 14f, ui.skin.dim), ui.matchWidth(6))
            addView(ui.button(getString(R.string.base_picture_add), null, primary = false) { pickPicture() }, ui.matchWidth(12))
        }
    }

    private fun pickPicture() {
        val pick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(pick, PICK_PICTURE)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.help_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PICTURE && resultCode == RESULT_OK) data?.data?.let(::importPicture)
    }

    // Copied off the main thread, as a big screenshot takes a moment to shrink.
    private fun importPicture(uri: Uri) {
        val target = BasePictures.draft(this, UUID.randomUUID().toString())
        importing = thread {
            val copied = BasePictures.copy(applicationContext, uri, target)
            runOnUiThread {
                if (isDestroyed) {
                    target.delete()
                } else if (copied) {
                    draft?.delete()
                    draft = target
                    pictureRemoved = false
                    show()
                } else {
                    Toast.makeText(this, R.string.base_picture_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removePicture() {
        draft?.delete()
        draft = null
        pictureRemoved = editing != null
        show()
    }

    private fun save() {
        val found = BaseLinks.find(link)
        if (found == null) {
            Toast.makeText(this, R.string.base_needs_link, Toast.LENGTH_SHORT).show()
            return
        }
        val all = Store.allBases(this)
        val id = editing?.id ?: MyBases.matching(all, account, found)?.id ?: UUID.randomUUID().toString()
        val before = all.firstOrNull { it.id == id }
        val base = SavedBase(id, account, found, name.trim(), kind, before?.added ?: System.currentTimeMillis(), before?.opened ?: 0)
        val picture = BasePictures.file(this, id)
        val waiting = draft
        if (waiting != null) BasePictures.keep(waiting, picture) else if (pictureRemoved) picture.delete()
        draft = null
        Store.saveBase(this, base)
        Toast.makeText(this, R.string.base_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val base = editing ?: return
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(getString(R.string.base_delete_title, MyBasesActivity.nameOf(this, base)))
            .setMessage(R.string.base_delete_message)
            .setPositiveButton(R.string.base_delete_confirm) { _, _ ->
                Store.removeBase(this, base.id)
                BasePictures.delete(this, base.id)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun label(text: Int) = ui.text(getString(text), 13f, ui.skin.dim, bold = true)

    private fun field(value: String, hint: String, type: Int, onChange: (String) -> Unit) = EditText(this).apply {
        setText(value)
        this.hint = hint
        setTextColor(ui.skin.text)
        setHintTextColor(ui.skin.dim)
        backgroundTintList = ColorStateList.valueOf(ui.skin.dim)
        inputType = type
        isSingleLine = true
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) = onChange(text.toString())
        })
    }

    private fun pick(label: String, chosen: Boolean, onPick: () -> Unit): TextView {
        val colours = if (chosen) ui.skin.primaryBg to ui.skin.primaryText else ui.skin.chip to ui.skin.text
        return ui.chip(label, colours.first, colours.second).apply {
            minHeight = ui.dp(36)
            gravity = Gravity.CENTER_VERTICAL
            isSelected = chosen
            ui.pressable(this, 8f) { onPick() }
        }
    }

    companion object {
        const val EXTRA_BASE = "base"
        private const val PICK_PICTURE = 1
        private const val STATE_LINK = "link"
        private const val STATE_NAME = "name"
        private const val STATE_KIND = "kind"
        private const val STATE_ACCOUNT = "account"
        private const val STATE_DRAFT = "draft"
        private const val STATE_REMOVED = "picture_removed"
        private const val STATE_NOTHING = "shared_nothing"
    }
}
