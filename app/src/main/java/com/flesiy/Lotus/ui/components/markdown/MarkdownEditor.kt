package com.flesiy.Lotus.ui.components.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.VibrationEffect.createOneShot
import android.os.VibrationEffect.createWaveform
import android.os.Vibrator
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.doAfterTextChanged
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.flesiy.Lotus.ui.components.markdown.handler.BlockQuoteEditHandler
import com.flesiy.Lotus.ui.components.markdown.handler.CodeEditHandler
import com.flesiy.Lotus.ui.components.markdown.handler.HeadingEditHandler
import com.flesiy.Lotus.ui.components.markdown.handler.StrikethroughEditHandler
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import java.lang.reflect.Field
import java.util.concurrent.Executors

/**
 * A custom Markwon Editor view for editing Markdown text with syntax highlighting.
 *
 * @param value markdown text
 * @param onValueChange callback to set the markdown text in Compose
 * @param modifier modifiers for Jetpack Compose view
 * @param charLimit maximum number of character to allow
 * @param maxLines maximum number of lines to display
 * @param inputType InputType to use for the EditText
 * @param hint string resource for the hint to show
 * @param setView callback to provide a reference to the underlying View
 * @param onLinkClick callback to handle clicks on links in the text
 */
@Composable
@NonRestartableComposable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    charLimit: Int? = null,
    maxLines: Int? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
    @StringRes hint: Int? = null,
    setView: (EditText) -> Unit = {},
    onLinkClick: (String, String, TextRange) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION") val vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val (lastCounter, setLastCounter) = rememberSaveable { mutableIntStateOf(value.length) }

    AndroidView(modifier = modifier, factory = { ctx ->
        createEditor(
            context = ctx,
            value = value,
            onValueChange = onValueChange,
            maxLines = maxLines,
            inputType = inputType,
            charLimit = charLimit,
            hint = hint,
            onLinkClick = onLinkClick
        )
    }, update = { editText ->
        setView(editText)
        if (maxLines != null) editText.maxLines = maxLines
        if (value != editText.text.toString()) {
            editText.setText(value)
        }
        updateCounterRemaining(
            value.length,
            lastCounter,
            vibrator,
            editText,
            setLastCounter
        )
    })
}

@Suppress("MagicNumber")
private fun updateCounterRemaining(
    counterValue: Int,
    lastCounter: Int,
    vibrator: Vibrator,
    editText: EditText,
    setLastCounter: (Int) -> Unit
) {
    if (counterValue != lastCounter) {
        when {
            counterValue in 1..10 -> {
                if (lastCounter > 10 && SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(createOneShot(150, 96))
                }
                editText.makeCursorColor(Color.Red.toArgb())
            }

            counterValue in -9..0 -> {
                if (lastCounter > 0 && SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        createWaveform(
                            arrayOf(150L, 200L).toLongArray(),
                            arrayOf(255, 255).toIntArray(),
                            -1
                        )
                    )
                }
                editText.makeCursorColor(Color.Red.toArgb())
            }

            counterValue <= -10 -> {
                if (lastCounter > -10 && SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        createWaveform(
                            arrayOf(150L, 200L).toLongArray(),
                            arrayOf(255, 255).toIntArray(),
                            -1
                        )
                    )
                }
                editText.makeCursorColor(Color.Red.toArgb())
            }

            else -> editText.makeCursorColor(editText.currentTextColor)
        }
        setLastCounter(counterValue)
    }
}

@SuppressLint(
    "DiscouragedPrivateApi",
    "PrivateApi"
) // we need reflection to change the cursor color pre-API 29
private fun EditText.makeCursorColor(argb: Int) {
    if (SDK_INT >= Build.VERSION_CODES.Q) {
        val wrapped = DrawableCompat.wrap(textCursorDrawable!!).mutate()
        DrawableCompat.setTint(wrapped, argb)
        wrapped.setBounds(0, 0, wrapped.intrinsicWidth, wrapped.intrinsicHeight)
        textCursorDrawable = wrapped
    } else {
        try {
            // Get the cursor resource id
            var field: Field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            field.isAccessible = true
            val drawableResId: Int = field.getInt(this)

            // Get the editor
            field = TextView::class.java.getDeclaredField("mEditor")
            field.isAccessible = true
            val editor: Any = field.get(this)!!

            // Get the drawable and set a color filter
            val drawable: Drawable = ContextCompat.getDrawable(context, drawableResId)!!
            @Suppress("DEPRECATION") // non-deprecated version is API 29+
            drawable.setColorFilter(argb, PorterDuff.Mode.SRC_IN)
            val drawables = arrayOf(drawable, drawable)

            // Set the drawables
            field = editor.javaClass.getDeclaredField("mCursorDrawable")
            field.isAccessible = true
            field.set(editor, drawables)
        } catch (ignored: Exception) {
        }
    }
}

private fun createEditor(
    context: Context,
    value: String,
    onValueChange: (String) -> Unit,
    maxLines: Int?,
    inputType: Int,
    charLimit: Int?,
    @StringRes hint: Int?,
    onLinkClick: (String, String, TextRange) -> Unit
): EditText {
    return object : EditText(context) {
        private val TAG = "LOTUS_DEBUG"
        private var isUpdatingNumbers = false
        private var lastText = ""

        private fun handleListContinuation(text: Editable) {
            Log.e(TAG, "=== handleListContinuation STARTED ===")
            if (isUpdatingNumbers) {
                Log.e(TAG, "⚠️ handleListContinuation: пропущено из-за isUpdatingNumbers")
                return
            }
            
            val currentText = text.toString()
            Log.e(TAG, "📝 Текущий текст: '$currentText'")
            Log.e(TAG, "📝 Предыдущий текст: '$lastText'")
            
            // Проверяем, было ли нажато Enter
            if (currentText.length != lastText.length && currentText.contains('\n')) {
                val start = selectionStart
                val lineStart = text.lastIndexOf('\n', start - 2).let { if (it == -1) 0 else it + 1 }
                val prevLine = text.substring(lineStart, start - 1)
                
                Log.e(TAG, "🔍 АНАЛИЗ:")
                Log.e(TAG, "   - Позиция курсора: $start")
                Log.e(TAG, "   - Начало строки: $lineStart")
                Log.e(TAG, "   - Предыдущая строка: '$prevLine'")

                // Проверяем маркеры списков
                val bulletMatch = prevLine.matches(Regex("^\\s*[-*+]\\s.*"))
                val numberMatch = prevLine.matches(Regex("^\\s*\\d+\\.\\s.*"))
                
                Log.e(TAG, "📋 РЕЗУЛЬТАТ ПРОВЕРКИ:")
                Log.e(TAG, "   - Маркированный список: $bulletMatch")
                Log.e(TAG, "   - Нумерованный список: $numberMatch")

                when {
                    bulletMatch -> {
                        val marker = prevLine.substring(0, prevLine.indexOf(' ') + 1)
                        Log.e(TAG, "🔸 Обработка маркированного списка:")
                        Log.e(TAG, "   - Маркер: '$marker'")
                        if (prevLine.trim() == marker.trim()) {
                            Log.e(TAG, "   ❌ Удаление пустого маркера")
                            text.delete(lineStart, start)
                        } else {
                            Log.e(TAG, "   ✅ Добавление нового маркера")
                            text.insert(start, marker)
                        }
                    }
                    numberMatch -> {
                        Log.e(TAG, "🔢 Обработка нумерованного списка:")
                        val indent = prevLine.takeWhile { it.isWhitespace() }
                        val number = prevLine.substring(indent.length, prevLine.indexOf('.')).toIntOrNull() ?: 1
                        Log.e(TAG, "   - Отступ: '$indent'")
                        Log.e(TAG, "   - Номер: $number")
                        
                        if (prevLine.trim().matches(Regex("\\d+\\.\\s*"))) {
                            Log.e(TAG, "   ❌ Удаление пустого номера")
                            text.delete(lineStart, start)
                            updateNumberedList()
                        } else {
                            val newMarker = "$indent${number + 1}. "
                            Log.e(TAG, "   ✅ Добавление нового номера: '$newMarker'")
                            text.insert(start, newMarker)
                        }
                    }
                }
            }
            lastText = currentText
            Log.e(TAG, "=== handleListContinuation FINISHED ===\n")
        }

        private fun updateNumberedList() {
            if (isUpdatingNumbers) {
                Log.d(TAG, "updateNumberedList: пропущено из-за isUpdatingNumbers")
                return
            }
            isUpdatingNumbers = true
            Log.d(TAG, "updateNumberedList: начало обновления нумерации")

            try {
                val text = text as Editable
                val lines = text.toString().split('\n')
                Log.d(TAG, "updateNumberedList: всего строк: ${lines.size}")
                
                var currentNumber = 1
                var inNumberedList = false
                var lastIndent = ""
                val newContent = StringBuilder()

                lines.forEachIndexed { index, line ->
                    val indent = line.takeWhile { it.isWhitespace() }
                    val restOfLine = line.substring(indent.length)
                    val numberMatch = restOfLine.matches(Regex("^\\d+\\.\\s.*"))
                    val isEmptyLine = line.trim().isEmpty()
                    
                    Log.d(TAG, "updateNumberedList: обработка строки $index: '$line', numberMatch=$numberMatch")

                    when {
                        isEmptyLine -> {
                            inNumberedList = false
                            currentNumber = 1
                            newContent.append(line)
                        }
                        numberMatch && (!inNumberedList || indent != lastIndent) -> {
                            inNumberedList = true
                            currentNumber = 1
                            lastIndent = indent
                            val content = restOfLine.substringAfter('.')
                            val newLine = "$indent$currentNumber.$content"
                            Log.d(TAG, "updateNumberedList: начало нового списка: '$newLine'")
                            newContent.append(newLine)
                            currentNumber++
                        }
                        numberMatch && inNumberedList && indent == lastIndent -> {
                            val content = restOfLine.substringAfter('.')
                            val newLine = "$indent$currentNumber.$content"
                            Log.d(TAG, "updateNumberedList: продолжение списка: '$newLine'")
                            newContent.append(newLine)
                            currentNumber++
                        }
                        else -> {
                            inNumberedList = false
                            currentNumber = 1
                            Log.d(TAG, "updateNumberedList: обычная строка: '$line'")
                            newContent.append(line)
                        }
                    }

                    if (index < lines.lastIndex) {
                        newContent.append('\n')
                    }
                }

                val newText = newContent.toString()
                if (text.toString() != newText) {
                    val cursorPosition = selectionStart
                    Log.d(TAG, "updateNumberedList: применение изменений, позиция курсора: $cursorPosition")
                    text.replace(0, text.length, newText)
                    setSelection(cursorPosition.coerceIn(0, text.length))
                } else {
                    Log.d(TAG, "updateNumberedList: изменений не требуется")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateNumberedList: ошибка при обновлении нумерации", e)
            } finally {
                isUpdatingNumbers = false
                Log.d(TAG, "updateNumberedList: завершение обновления нумерации")
            }
        }

        init {
            Log.e(TAG, "🚀 ИНИЦИАЛИЗАЦИЯ РЕДАКТОРА")
            
            doAfterTextChanged { text ->
                if (text != null) {
                    Log.e(TAG, "\n=== doAfterTextChanged STARTED ===")
                    Log.e(TAG, "📝 Длина текста: ${text.length}")
                    handleListContinuation(text)
                    Log.e(TAG, "=== doAfterTextChanged FINISHED ===\n")
                }
            }

            addTextChangedListener(object : TextWatcher {
                private var beforeText = ""
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    beforeText = s?.toString() ?: ""
                    Log.e(TAG, "\n▶️ beforeTextChanged: start=$start, count=$count, after=$after")
                }
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    Log.e(TAG, "⏺️ onTextChanged: start=$start, before=$before, count=$count")
                }
                
                override fun afterTextChanged(s: Editable?) {
                    if (s == null) return
                    val currentText = s.toString()
                    Log.e(TAG, "⏹️ afterTextChanged: длина текста=${currentText.length}")
                }
            })
        }
    }.apply {
        setText(value)
        this.inputType = inputType
        maxLines?.let { this.maxLines = it }
        hint?.let { this.setHint(it) }

        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .logger(DebugLogger())
            .build()

        val markwon = Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context, imageLoader))
            .build()

        val editor = MarkwonEditor.builder(markwon)
            .useEditHandler(EmphasisEditHandler())
            .useEditHandler(StrongEmphasisEditHandler())
            .useEditHandler(StrikethroughEditHandler())
            .useEditHandler(CodeEditHandler())
            .useEditHandler(BlockQuoteEditHandler())
            .useEditHandler(HeadingEditHandler())
            .build()

        val textWatcher = MarkwonEditorTextWatcher.withPreRender(
            editor,
            Executors.newCachedThreadPool(),
            this
        )

        addTextChangedListener(textWatcher)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != value) {
                    onValueChange(s.toString())
                }
            }
        })

        if (charLimit != null) {
            addTextChangedListener(object : TextWatcher {
                private var lastText = ""
                private var lastStart = 0
                private var lastCount = 0

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    lastText = s.toString()
                    lastStart = start
                    lastCount = count
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s == null) return

                    val currentLength = s.length
                    if (currentLength > charLimit) {
                        s.replace(0, s.length, lastText)
                        text.delete(lastStart, lastStart + lastCount)
                        setSelection(lastStart)
                    }
                }
            })
        }

        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                postDelayed({
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }, 100)
            }
        }

        setOnClickListener {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        }

        setOnLongClickListener {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
            false
        }
    }
} 