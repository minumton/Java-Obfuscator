package ru.sut.obfuscator.core;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Одно место в коде, где имя нужно заменить: объявление или использование.
 *
 * @param oldName исходное имя
 * @param setter  действие, записывающее новое имя в узел синтаксического дерева
 */
public record RenameTarget(String oldName, Consumer<String> setter) {

    /**
     * Заменяет имя, если для него есть запись в таблице переименований.
     *
     * @param renames таблица «старое имя → новое имя»
     */
    public void apply(Map<String, String> renames) {
        String newName = renames.get(oldName);
        if (newName != null) {
            setter.accept(newName);
        }
    }
}
