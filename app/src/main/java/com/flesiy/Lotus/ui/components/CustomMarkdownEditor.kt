package com.flesiy.Lotus.ui.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.widget.addTextChangedListener
import com.flesiy.Lotus.ui.components.markdown.handler.*
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
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

private class ImageKeyboardEditText(context: Context) : EditText(context) {
    private var isUpdatingNumbers = false // Флаг для предотвращения рекурсии

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic: InputConnection? = super.onCreateInputConnection(editorInfo)
        val allowedMimeTypes = arrayOf("image/gif", "image/jpeg", "image/png")
        EditorInfoCompat.setContentMimeTypes(editorInfo, allowedMimeTypes)

        ViewCompat.setOnReceiveContentListener(
            this,
            allowedMimeTypes,
            object : OnReceiveContentListener {
                override fun onReceiveContent(
                    view: View,
                    payload: ContentInfoCompat
                ): ContentInfoCompat? {
                    val (content, remaining) = payload.partition { item -> item.uri != null }
                    if (content == null) return remaining

                    val uri = content.linkUri
                    val description = content.clip.description
                    val newText = "![${description.label}]($uri)"
                    ic?.commitText(newText, (view as EditText).selectionEnd + newText.length)
                    return remaining
                }
            })
        return InputConnectionCompat.createWrapper(this, ic!!, editorInfo)
    }

    private fun updateNumberedList(startLineIndex: Int) {
        if (isUpdatingNumbers) return
        isUpdatingNumbers = true

        val text = text as SpannableStringBuilder
        val lines = text.toString().split('\n')
        var currentNumber = 1
        
        // Находим начало последовательного списка
        var listStartIndex = startLineIndex
        val baseIndent = lines[startLineIndex].takeWhile { it.isWhitespace() }
        
        while (listStartIndex > 0 &&
               lines[listStartIndex - 1].matches(Regex("^\\s*\\d+\\.\\s.*")) &&
               lines[listStartIndex - 1].takeWhile { it.isWhitespace() } == baseIndent &&
               !lines[listStartIndex - 1].trim().isEmpty()) {
            listStartIndex--
        }
        
        // Находим конец последовательного списка
        var listEndIndex = startLineIndex
        while (listEndIndex < lines.size - 1 &&
               lines[listEndIndex + 1].matches(Regex("^\\s*\\d+\\.\\s.*")) &&
               lines[listEndIndex + 1].takeWhile { it.isWhitespace() } == baseIndent &&
               !lines[listEndIndex + 1].trim().isEmpty()) {
            listEndIndex++
        }
        
        // Обновляем только последовательные элементы списка
        val newContent = StringBuilder()
        
        // Добавляем строки до списка
        for (i in 0 until listStartIndex) {
            newContent.append(lines[i]).append('\n')
        }
        
        // Обновляем нумерацию в списке
        for (i in listStartIndex..listEndIndex) {
            val line = lines[i]
            val indent = line.takeWhile { it.isWhitespace() }
            val content = line.substringAfter('.')
            newContent.append(indent)
                .append(currentNumber)
                .append(".")
                .append(content)
            if (i < lines.lastIndex) newContent.append('\n')
            currentNumber++
        }
        
        // Добавляем оставшиеся строки
        for (i in listEndIndex + 1 until lines.size) {
            if (i > listEndIndex + 1) newContent.append('\n')
            newContent.append(lines[i])
        }

        if (text.toString() != newContent.toString()) {
            val cursorPosition = selectionStart
            text.replace(0, text.length, newContent)
            setSelection(cursorPosition.coerceIn(0, text.length))
        }
        
        isUpdatingNumbers = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            val start = selectionStart
            val text = text as SpannableStringBuilder
            
            // Проверяем предыдущую строку на наличие маркера списка
            val prevLineStart = text.lastIndexOf('\n', start - 1).let { if (it == -1) 0 else it + 1 }
            val prevLine = text.substring(prevLineStart, start)
            
            // Проверяем на маркеры списка (маркированный и нумерованный)
            val bulletMatch = prevLine.matches(Regex("^\\s*[-*+]\\s.*"))
            val numberMatch = prevLine.matches(Regex("^\\s*\\d+\\.\\s.*"))
            
            when {
                bulletMatch -> {
                    // Маркированный список
                    val marker = prevLine.substring(0, prevLine.indexOf(' ') + 1)
                    if (prevLine.trim() == marker.trim()) {
                        // Если строка пустая, удаляем маркер и выходим из списка
                        text.delete(prevLineStart, start)
                        return true
                    }
                }
                numberMatch -> {
                    val prevLineContent = prevLine.trim()
                    if (prevLineContent.matches(Regex("\\d+\\.\\s*"))) {
                        // Если строка пустая (только номер), удаляем маркер
                        text.delete(prevLineStart, start)
                        updateNumberedList(prevLineStart)
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    init {
        // Добавляем слушатель изменений текста для автоматической перенумерации
        addTextChangedListener(object : TextWatcher {
            private var beforeText = ""
            private var beforeCursor = 0
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeText = s?.toString() ?: ""
                beforeCursor = selectionStart
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return
                val currentText = s.toString()
                
                // Проверяем, было ли удаление строки с номером
                if (beforeText != currentText) {
                    val beforeLines = beforeText.split('\n')
                    val currentLines = currentText.split('\n')
                    
                    // Находим измененную строку
                    val cursorLine = currentText.substring(0, selectionStart).count { it == '\n' }
                    val prevLine = if (cursorLine > 0) currentLines.getOrNull(cursorLine - 1) else null
                    val nextLine = currentLines.getOrNull(cursorLine + 1)
                    
                    // Проверяем, является ли текущая строка частью последовательного списка
                    val isInSequentialList = prevLine?.matches(Regex("^\\s*\\d+\\.\\s.*")) == true &&
                                           nextLine?.matches(Regex("^\\s*\\d+\\.\\s.*")) == true &&
                                           prevLine.takeWhile { it.isWhitespace() } == nextLine.takeWhile { it.isWhitespace() }
                    
                    if (isInSequentialList) {
                        updateNumberedList(cursorLine - 1)
                    }
                }
            }
        })
    }
}

@Composable
fun CustomMarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    onEditorCreated: ((EditText) -> Unit)? = null,
    fontSize: Float = 16f
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createEditor(
                context = ctx,
                value = value,
                onValueChange = onValueChange,
                hint = hint,
                fontSize = fontSize
            ).also { editor ->
                onEditorCreated?.invoke(editor)
            }
        },
        update = { editText ->
            editText.textSize = fontSize
            if (value != editText.text.toString()) {
                val newText = SpannableStringBuilder(value)
                editText.text = newText
                editText.setSelection(value.length)
            }
        }
    )
}

private fun createEditor(
    context: Context,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    fontSize: Float
): EditText {
    return ImageKeyboardEditText(context).apply {
        setText(value)
        textSize = fontSize
        inputType = InputType.TYPE_CLASS_TEXT or 
                   InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                   InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                   InputType.TYPE_TEXT_FLAG_AUTO_CORRECT

        background = null
        hint?.let { this.hint = it }
        gravity = android.view.Gravity.TOP
        setPadding(45, 40, 16, 16)

        // Расширенная настройка Markwon
        val markwon = Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context))
            .build()

        // Расширенная настройка редактора
        val editor = MarkwonEditor.builder(markwon)
            .useEditHandler(EmphasisEditHandler())
            .useEditHandler(StrongEmphasisEditHandler())
            .useEditHandler(BlockQuoteEditHandler())
            .useEditHandler(CodeEditHandler())
            .useEditHandler(StrikethroughEditHandler())
            .useEditHandler(HeadingEditHandler())
            .build()

        // Создание и настройка TextWatcher для предварительного рендеринга
        val textWatcher = MarkwonEditorTextWatcher.withPreRender(
            editor,
            Executors.newCachedThreadPool(),
            this
        )

        // Добавление обработчиков текста
        addTextChangedListener(textWatcher)
        addTextChangedListener { text ->
            if (text.toString() != value) {
                onValueChange(text.toString())
            }
        }

        // Настройка внешнего вида и поведения
        setPadding(45, 40, 16, 16)
        setTextIsSelectable(true)
        isFocusable = true
        isFocusableInTouchMode = true
        
        // Показываем клавиатуру при получении фокуса
        setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        // Настройка цвета курсора
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wrapped = DrawableCompat.wrap(textCursorDrawable!!).mutate()
            DrawableCompat.setTint(wrapped, currentTextColor)
            wrapped.setBounds(0, 0, wrapped.intrinsicWidth, wrapped.intrinsicHeight)
            textCursorDrawable = wrapped
        } else {
            try {
                // Получаем id ресурса курсора
                var field: Field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                field.isAccessible = true
                val drawableResId: Int = field.getInt(this)

                // Получаем редактор
                field = TextView::class.java.getDeclaredField("mEditor")
                field.isAccessible = true
                val editor: Any = field.get(this)!!

                // Получаем drawable и устанавливаем цветовой фильтр
                val drawable: Drawable = ContextCompat.getDrawable(context, drawableResId)!!
                @Suppress("DEPRECATION")
                drawable.setColorFilter(currentTextColor, PorterDuff.Mode.SRC_IN)
                val drawables = arrayOf(drawable, drawable)

                // Устанавливаем drawables
                field = editor.javaClass.getDeclaredField("mCursorDrawable")
                field.isAccessible = true
                field.set(editor, drawables)
            } catch (ignored: Exception) {
            }
        }
    }
} 