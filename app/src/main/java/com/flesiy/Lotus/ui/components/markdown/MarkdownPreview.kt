package com.flesiy.Lotus.ui.components.markdown

import android.content.Context
import android.content.ContextWrapper
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flesiy.Lotus.ui.components.CustomMarkdownEditor
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.AbstractMarkwonPlugin
import java.util.regex.Pattern
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.flesiy.Lotus.viewmodel.MainViewModel

@Composable
fun MarkdownPreviewScreen(
    content: String,
    isPreviewMode: Boolean,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Crossfade(
            targetState = isPreviewMode,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.fillMaxSize()
        ) { inPreviewMode ->
            if (inPreviewMode) {
                MarkdownPreview(
                    content = content,
                    onContentChange = onContentChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                CustomMarkdownEditor(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

class CheckboxPreProcessor : AbstractMarkwonPlugin() {
    companion object {
        private const val TAG = "CHECKBOX_DEBUG"
    }

    override fun processMarkdown(markdown: String): String {
        Log.d(TAG, "🔲 Начало обработки markdown текста")
        val pattern = Pattern.compile("^([ \\t]*- \\[([ xX])\\].*?)(?=\\n|$)", Pattern.MULTILINE)
        val matcher = pattern.matcher(markdown)
        val result = StringBuilder()
        var lastPosition = 0
        var checkboxCount = 0

        while (matcher.find()) {
            checkboxCount++
            val fullMatch = matcher.group(1) // Вся строка с чекбоксом
            val isChecked = matcher.group(2)?.trim()?.lowercase() == "x"
            val textAfterCheckbox = fullMatch?.substring(fullMatch.indexOf("]") + 1)?.trimStart() ?: ""
            
            Log.d(TAG, "🔲 Найден чекбокс #$checkboxCount: ${matcher.group(0)}, состояние: ${if (isChecked) "отмечен" else "не отмечен"}")
            
            // Добавляем текст до чекбокса
            result.append(markdown.substring(lastPosition, matcher.start()))
            
            // Добавляем чекбокс и текст после него
            result.append("![](file:///android_asset/checkbox_${if (isChecked) "checked" else "unchecked"}.png)")
            result.append(" ") // Добавляем пробел между чекбоксом и текстом
            result.append(textAfterCheckbox)
            
            // Добавляем перенос строки, если его нет
            if (!textAfterCheckbox.endsWith("\n")) {
                result.append("\n")
            }
            
            lastPosition = matcher.end()
        }

        // Добавляем оставшийся текст
        result.append(markdown.substring(lastPosition))
        
        Log.d(TAG, "🔲 Обработка завершена. Всего найдено чекбоксов: $checkboxCount")
        return result.toString()
    }
}

class CheckboxClickHandler(
    private val content: String,
    private val onContentChange: (String) -> Unit
) : LinkMovementMethod() {
    companion object {
        private const val TAG = "CHECKBOX_DEBUG"
    }

    private val checkboxPattern = Regex("^([ \\t]*- \\[([ xX])\\].*?)$", RegexOption.MULTILINE)
    private var checkboxes: List<CheckboxInfo>? = null

    private fun initCheckboxes() {
        if (checkboxes == null) {
            // Находим все чекбоксы и сохраняем их позиции в тексте
            val lines = content.split("\n")
            var currentPosition = 0
            
            checkboxes = mutableListOf<CheckboxInfo>().apply {
                lines.forEachIndexed { lineIndex, line ->
                    val match = checkboxPattern.find(line)
                    if (match != null) {
                        add(CheckboxInfo(
                            index = lineIndex,
                            start = currentPosition + match.range.first,
                            lineStart = currentPosition,
                            lineEnd = currentPosition + line.length,
                            text = match.value,
                            isChecked = match.groupValues[2].trim().lowercase() == "x"
                        ))
                    }
                    currentPosition += line.length + 1 // +1 для символа новой строки
                }
            }
        }
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: android.view.MotionEvent): Boolean {
        if (event.action != android.view.MotionEvent.ACTION_UP) {
            return super.onTouchEvent(widget, buffer, event)
        }

        val layout = widget.layout
        val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
        val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
        
        // Определяем строку по вертикальной координате
        val clickedLine = layout.getLineForVertical(y)
        
        // Проверяем, что клик находится в пределах ширины строки
        val lineLeft = layout.getLineLeft(clickedLine)
        val lineRight = layout.getLineRight(clickedLine)
        if (x < lineLeft || x > lineRight) {
            return super.onTouchEvent(widget, buffer, event)
        }

        // Получаем позицию в тексте по координатам клика
        val offset = layout.getOffsetForHorizontal(clickedLine, x.toFloat())
        
        // Инициализируем чекбоксы при необходимости
        initCheckboxes()
        
        // Находим чекбокс, в строке которого был клик
        val checkbox = checkboxes?.find { info ->
            offset >= info.lineStart && offset <= info.lineEnd
        } ?: return super.onTouchEvent(widget, buffer, event)

        Log.d(TAG, "🔲 Обработка клика по чекбоксу #${checkbox.index + 1}")
        Log.d(TAG, "🔲 Текст: ${checkbox.text}")
        Log.d(TAG, "🔲 Текущее состояние: ${if (checkbox.isChecked) "отмечен" else "не отмечен"}")

        // Меняем состояние чекбокса
        val newContent = StringBuilder(content)
        val checkboxStart = checkbox.start + 3 // +3 чтобы попасть на символ в скобках
        newContent.setCharAt(checkboxStart, if (checkbox.isChecked) ' ' else 'x')
        
        // Сбрасываем кэш чекбоксов, так как контент изменился
        checkboxes = null
        
        Log.d(TAG, "🔲 Новое состояние: ${if (!checkbox.isChecked) "отмечен" else "не отмечен"}")
        onContentChange(newContent.toString())
        return true
    }

    private data class CheckboxInfo(
        val index: Int,
        val start: Int,
        val lineStart: Int, // Начало строки в тексте
        val lineEnd: Int,   // Конец строки в тексте
        val text: String,
        val isChecked: Boolean
    )
}

@Composable
fun MarkdownPreview(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f
) {
    val context = LocalContext.current
    val markwon = remember { createMarkwon(context) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                createPreviewTextView(ctx, fontSize).apply {
                    movementMethod = CheckboxClickHandler(content, onContentChange)
                    markwon.setMarkdown(this, content)
                }
            },
            update = { textView ->
                textView.textSize = fontSize
                textView.movementMethod = CheckboxClickHandler(content, onContentChange)
                markwon.setMarkdown(textView, content)
            }
        )
    }
}

private fun createPreviewTextView(context: Context, fontSize: Float): TextView {
    return TextView(context).apply {
        textSize = fontSize
        setPadding(40, 16, 26, 16)
        setTextIsSelectable(true)
        gravity = android.view.Gravity.TOP
    }
}

private fun createMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .apply {
            // Добавляем CheckboxPreProcessor только если включены TODO
            val viewModel = context.findActivity()?.let { 
                ViewModelProvider(it)[MainViewModel::class.java] 
            }
            if (viewModel?.isTodoEnabled?.value == true) {
                usePlugin(CheckboxPreProcessor())
            }
        }
        .build()
}

// Вспомогательная функция для получения Activity из Context
private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
} 