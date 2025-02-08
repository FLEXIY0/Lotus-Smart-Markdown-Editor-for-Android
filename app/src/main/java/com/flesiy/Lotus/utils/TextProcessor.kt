package com.flesiy.Lotus.utils

import android.util.Log

class TextProcessor {
    companion object {
        private const val TAG = "TextProcessor"
        
        private val sentenceEndingPunctuation = setOf('.', '!', '?')
        private val abbreviations = setOf(
            "т.д", "т.п", "и.т.д", "и.т.п", "т.к", "т.е",
            "см", "им", "др", "проф", "доц", "акад",
            "г", "гг", "в", "вв", "н.э", "до н.э"
        )

        // Имена собственные и другие слова, которые должны начинаться с заглавной буквы
        private val properNouns = setOf(
            "Россия", "Москва", "Санкт-Петербург", "Владимир",
            "Google", "Android", "Windows", "Apple",
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )

        // Слова, после которых обычно нужна запятая
        private val wordsRequiringComma = setOf(
            // Подчинительные союзы
            "который", "которая", "которое", "которые",
            "где", "куда", "откуда", "когда", "пока",
            "если", "чтобы", "потому что", "так как",
            "поскольку", "хотя", "несмотря на то что",
            "как будто", "словно", "что", "чем",
            "поэтому", "затем", "причем", "притом",
            "ибо", "дабы", "пусть", "пускай",
            
            // Вводные слова и конструкции
            "конечно", "безусловно", "несомненно",
            "вероятно", "возможно", "наверное",
            "во-первых", "во-вторых", "в-третьих",
            "кстати", "впрочем", "однако",
            "следовательно", "итак", "таким образом",
            "например", "к примеру", "допустим",
            "кроме того", "более того", "к слову",
            "между прочим", "в частности",
            
            // Деепричастные обороты
            "глядя", "смотря", "думая", "считая",
            "полагая", "предполагая", "зная",
            "учитывая", "принимая во внимание",
            
            // Причастные обороты
            "расположенный", "находящийся",
            "стоящий", "лежащий", "идущий",
            "работающий", "сделанный", "созданный"
        )

        // Слова, перед которыми обычно нужна запятая
        private val wordsRequiringCommaBefore = setOf(
            // Противительные союзы
            "но", "однако", "зато", "хотя",
            "тем не менее", "все же", "все-таки",
            
            // Присоединительные союзы
            "также", "причем", "притом", "да и",
            
            // Пояснительные союзы
            "то есть", "а именно", "или", "либо",
            
            // Градационные союзы
            "не только", "но и", "как", "так и"
        )

        // Словарь глагольных форм с примерами
        private val verbForms = mapOf(
            // Возвратные глаголы (-ться)
            "учиться" to "учится",
            "смеяться" to "смеется",
            "пытаться" to "пытается",
            "стараться" to "старается",
            "начинаться" to "начинается",
            "заканчиваться" to "заканчивается",
            
            // Глаголы с -шь (не меняем)
            "идешь" to "идешь",
            "поешь" to "поешь",
            "живешь" to "живешь",
            "берешь" to "берешь",
            "несешь" to "несешь",
            "ждешь" to "ждешь",
            "пьешь" to "пьешь",
            "спишь" to "спишь",
            
            // Глаголы с -шься
            "учишься" to "учишься",
            "смеешься" to "смеешься",
            "стараешься" to "стараешься",
            "пытаешься" to "пытаешься",
            "занимаешься" to "занимаешься"
        )

        // Словарь окончаний с контекстом
        private val contextualEndings = mapOf(
            // Прилагательные
            Regex("(?i)тельный\\b") to "тельный",
            Regex("(?i)тельная\\b") to "тельная",
            Regex("(?i)тельное\\b") to "тельное",
            Regex("(?i)тельные\\b") to "тельные",
            
            // Причастия
            Regex("(?i)\\b(?<!у)щий\\b") to "щий", // не меняем "учащий"
            Regex("(?i)\\b(?<!у)щая\\b") to "щая",
            Regex("(?i)\\b(?<!у)щее\\b") to "щее",
            Regex("(?i)\\b(?<!у)щие\\b") to "щие",
            
            // Деепричастия
            Regex("(?i)вши\\b") to "вши",
            Regex("(?i)вшись\\b") to "вшись",
            
            // Инфинитивы
            Regex("(?i)(?<!е)ти\\b") to "ть" // не меняем "вести", "нести" и т.п.
        )

        // Исключения - слова, которые не нужно менять
        private val exceptions = setOf(
            "помощь",
            "вещь",
            "дочь",
            "ночь",
            "мощь",
            "печь",
            "речь",
            "течь" // как существительное
        )

        fun process(text: String): String {
            Log.d(TAG, "Processing text: $text")
            return text
                .trim()
                .let { 
                    capitalizeFirstLetter(it).also { 
                        Log.d(TAG, "After capitalizeFirstLetter: $it") 
                    }
                }
                .let { 
                    fixPunctuation(it).also { 
                        Log.d(TAG, "After fixPunctuation: $it") 
                    }
                }
                .let { 
                    fixSpaces(it).also { 
                        Log.d(TAG, "After fixSpaces: $it") 
                    }
                }
                .let { 
                    fixAbbreviations(it).also { 
                        Log.d(TAG, "After fixAbbreviations: $it") 
                    }
                }
                .let { 
                    fixCommonMistakes(it).also { 
                        Log.d(TAG, "After fixCommonMistakes: $it") 
                    }
                }
                .let { 
                    fixWordEndings(it).also { 
                        Log.d(TAG, "After fixWordEndings: $it") 
                    }
                }
                .let { 
                    addCommasAfterClauses(it).also { 
                        Log.d(TAG, "After addCommasAfterClauses: $it") 
                    }
                }
                .let { 
                    fixSentenceStructure(it).also { 
                        Log.d(TAG, "After fixSentenceStructure: $it") 
                    }
                }
                .let { 
                    fixCapitalization(it).also { 
                        Log.d(TAG, "Final result: $it") 
                    }
                }
        }

        private fun capitalizeFirstLetter(text: String): String {
            if (text.isEmpty()) return text
            return text.substring(0, 1).uppercase() + text.substring(1).lowercase()
        }

        private fun fixCapitalization(text: String): String {
            // Разбиваем текст на предложения (учитываем возможные пробелы после знаков препинания)
            val sentences = text.split(Regex("(?<=[.!?])\\s*"))
            
            return sentences.joinToString(" ") { sentence ->
                if (sentence.isBlank()) return@joinToString ""
                
                // Разбиваем предложение на слова, сохраняя пунктуацию
                val words = sentence.split(Regex("(?<=\\s)|(?=\\s)"))
                
                words.mapIndexed { index, word ->
                    when {
                        // Пробелы и пунктуация остаются без изменений
                        word.isBlank() || word.matches(Regex("[,.!?:;]")) -> word
                        
                        // Первое слово в предложении
                        index == 0 || words.take(index).all { it.isBlank() || it.matches(Regex("[,.!?:;]")) } ->
                            word.replaceFirstChar { it.uppercase() }
                            
                        // Имя собственное
                        properNouns.any { it.equals(word, ignoreCase = true) } -> 
                            properNouns.first { it.equals(word, ignoreCase = true) }
                            
                        // Все остальные слова переводим в нижний регистр
                        else -> word.lowercase()
                    }
                }.joinToString("")
            }.trim()
        }

        private fun fixPunctuation(text: String): String {
            var result = text
                .replace(" ,", ",")
                .replace(" .", ".")
                .replace(" !", "!")
                .replace(" ?", "?")
                .replace(" :", ":")
                .replace(" ;", ";")
                .replace("( ", "(")
                .replace(" )", ")")
                .replace("« ", "«")
                .replace(" »", "»")
                .replace(",,", ",")
                .replace("..", ".")
                .replace("!!", "!")
                .replace("??", "?")
                // Исправление запятых в числах
                .replace(Regex("(\\d),(\\d)"), "$1.$2")
                // Добавление запятых перед союзами
                .replace(Regex("\\s+(а|но|или|либо|однако)\\s+"), ", $1 ")

            // Добавляем точку в конце, если нет знаков препинания
            if (result.isNotEmpty() && !sentenceEndingPunctuation.contains(result.last())) {
                result += "."
            }

            return result
        }

        private fun fixSpaces(text: String): String {
            return text
                .replace(Regex("\\s+"), " ") // Убираем множественные пробелы
                .replace(Regex("\\s*([,.!?:;])\\s*"), "$1 ") // Исправляем пробелы вокруг знаков препинания
                .replace(Regex("\\s+$"), "") // Убираем пробелы в конце
                .replace(Regex("\\s*,\\s*,\\s*"), ", ") // Исправляем множественные запятые
        }

        private fun fixAbbreviations(text: String): String {
            var result = text
            for (abbr in abbreviations) {
                val pattern = Regex("(?i)\\b${abbr.replace(".", "\\.")}\\b")
                result = result.replace(pattern, abbr)
            }
            return result
        }

        private fun fixCommonMistakes(text: String): String {
            return text
                // Исправление частых ошибок распознавания
                .replace(Regex("(?i)\\bщас\\b"), "сейчас")
                .replace(Regex("(?i)\\bче\\b"), "что")
                .replace(Regex("(?i)\\bщто\\b"), "что")
                .replace(Regex("(?i)\\bкороч\\b"), "короче")
                .replace(Regex("(?i)\\bтож\\b"), "тоже")
                .replace(Regex("(?i)\\bнада\\b"), "надо")
                .replace(Regex("(?i)\\bтока\\b"), "только")
                .replace(Regex("(?i)\\bщщас\\b"), "сейчас")
                .replace(Regex("(?i)\\bпросто\\s+что\\b"), "потому что")
                .replace(Regex("(?i)\\bпотомушто\\b"), "потому что")
                .replace(Regex("(?i)\\bпотомучто\\b"), "потому что")
                .replace(Regex("(?i)\\bтакшто\\b"), "так что")
                // Исправление числительных
                .replace(Regex("(?i)\\bпервый\\b"), "1")
                .replace(Regex("(?i)\\bвторой\\b"), "2")
                .replace(Regex("(?i)\\bтретий\\b"), "3")
                // Добавление пробелов после цифр перед буквами
                .replace(Regex("(\\d)([а-яА-Я])"), "$1 $2")
                // Исправление союзов и предлогов
                .replace(Regex("\\s+и\\s+и\\s+"), " и ")
                .replace(Regex("\\s+а\\s+а\\s+"), " а ")
                // Исправление повторов
                .replace(Regex("(\\b\\w+\\b)\\s+\\1\\b"), "$1")
        }

        private fun fixWordEndings(text: String): String {
            var result = text

            // Сначала проверяем исключения
            if (exceptions.any { text.lowercase().endsWith(it) }) {
                return text
            }

            // Проверяем глагольные формы
            for ((pattern, replacement) in verbForms) {
                if (result.lowercase() == pattern.lowercase()) {
                    return replacement
                }
            }

            // Проверяем контекстные окончания
            for ((pattern, replacement) in contextualEndings) {
                result = result.replace(pattern) { matchResult ->
                    val word = matchResult.value
                    // Проверяем, не является ли слово исключением
                    if (exceptions.any { word.lowercase().endsWith(it) }) {
                        word
                    } else {
                        word.substring(0, word.length - replacement.length) + replacement
                    }
                }
            }

            return result
        }

        private fun addCommasAfterClauses(text: String): String {
            var result = text
            
            // Обрабатываем сначала конструкцию "не только... но и"
            result = result.replace(
                Regex("не только\\s+[^,.!?]+?\\s+но\\s+и"),
                { matchResult ->
                    val text = matchResult.value
                    text.replace("\\s+но\\s+и".toRegex(), ", но и")
                }
            )
            
            // Затем обрабатываем простые слова, требующие запятую перед ними
            for (word in wordsRequiringCommaBefore.filter { !it.contains(" ") }) {
                val before = result
                // Используем более простое регулярное выражение
                result = result.replace(
                    Regex("([^,])\\s+$word\\b(?!\\s*[,.])(?!\\s+и\\b)"),
                    "$1, $word"
                )
                if (before != result) {
                    Log.d(TAG, "Added comma before word '$word': $before -> $result")
                }
            }
            
            // Добавляем запятые после слов
            for (word in wordsRequiringComma) {
                val before = result
                result = result.replace(
                    Regex("\\b$word\\s+(?![,.])"),"$word, "
                )
                if (before != result) {
                    Log.d(TAG, "Added comma after '$word': $before -> $result")
                }
            }
            
            // Исправляем возможные двойные запятые
            val beforeFix = result
            result = result
                .replace(Regex(",\\s*,"), ",")
                .replace(Regex("\\s*,\\s*"), ", ")
            
            if (beforeFix != result) {
                Log.d(TAG, "Fixed multiple commas: $beforeFix -> $result")
            }
            
            return result
        }

        private fun fixSentenceStructure(text: String): String {
            var result = text

            // Обработка вводных слов в начале предложения
            result = result.replace(
                Regex("^(конечно|вероятно|возможно|наверное|кстати|впрочем)\\s+"),
                "$1, "
            )

            // Обработка вводных слов в середине предложения
            result = result.replace(
                Regex("\\s+(конечно|вероятно|возможно|наверное|кстати|впрочем)\\s+"),
                " $1, "
            )

            // Обработка деепричастных оборотов
            result = result.replace(
                Regex("^(глядя|смотря|думая|считая|полагая|предполагая|зная|учитывая)\\s+[^,.!?]+?(?=\\s+\\p{L}+)"),
                "$0, "
            )

            // Обработка конструкции "не только... но и"
            val parts = result.split(" но и ")
            if (parts.size == 2) {
                val beforeNoI = parts[0]
                if (beforeNoI.contains("не только")) {
                    result = beforeNoI.trimEnd() + ", но и " + parts[1]
                }
            }

            // Обработка противительных союзов
            val conjunctions = listOf("но", "однако", "зато", "хотя")
            for (conj in conjunctions) {
                result = result.replace(
                    Regex("([^,])\\s+$conj\\s+(?!и\\b)"),
                    "$1, $conj "
                )
            }

            // Обработка придаточных предложений с "который"
            result = result.replace(
                Regex("(\\p{L}+)\\s+который\\s+"),
                "$1, который "
            )

            // Обработка составных союзов
            result = result
                .replace(Regex("\\b(потому)\\s+что\\b"), "потому, что")
                .replace(Regex("\\b(для того)\\s+чтобы\\b"), "для того, чтобы")
                .replace(Regex("\\b(так)\\s+как\\b"), "так, как")

            // Исправление множественных запятых
            result = result
                .replace(Regex(",\\s*,"), ",")
                .replace(Regex(",\\s*и\\s*,"), " и ")
                .replace(Regex("\\s*,\\s*"), ", ")

            // Разбиваем на предложения и обрабатываем каждое
            result = result.split(Regex("(?<=[.!?])\\s+"))
                .joinToString(" ") { sentence ->
                    if (sentence.isNotEmpty()) {
                        sentence.replaceFirstChar { it.uppercase() }
                    } else sentence
                }

            Log.d(TAG, "fixSentenceStructure transformations: $text -> $result")
            return result
        }
    }
} 