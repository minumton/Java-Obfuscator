package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Применяет таблицу переименований к синтаксическим деревьям:
 * заменяет имена в найденных местах и исправляет импорты.
 */
public class Renamer {

    private static final String SEPARATOR = ".";

    /**
     * Заменяет имена во всех файлах.
     *
     * @param units   синтаксические деревья файлов
     * @param targets места, где имя нужно заменить
     * @param renames таблица «старое имя → новое имя»
     */
    public void apply(List<CompilationUnit> units, List<RenameTarget> targets, Map<String, String> renames) {
        Map<String, String> typeRenames = qualifiedTypeRenames(units, renames);
        targets.forEach(target -> target.apply(renames));
        units.forEach(unit -> unit.getImports()
                .forEach(declaration -> declaration.setName(newImportName(declaration, typeRenames, renames))));
    }

    /**
     * Строит таблицу полных имён пользовательских типов: {@code bank.Account → bank.lIIII}.
     * Вызывается до замены имён, пока в дереве ещё стоят исходные имена.
     *
     * @param units   синтаксические деревья файлов
     * @param renames таблица «старое имя → новое имя»
     * @return таблица «старое полное имя типа → новое полное имя»
     */
    private Map<String, String> qualifiedTypeRenames(List<CompilationUnit> units, Map<String, String> renames) {
        Map<String, String> typeRenames = new HashMap<>();
        for (CompilationUnit unit : units) {
            String packageName = unit.getPackageDeclaration()
                    .map(declaration -> declaration.getNameAsString())
                    .orElse("");
            for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
                type.getFullyQualifiedName().ifPresent(name ->
                        typeRenames.put(name, renameQualifiedType(name, packageName, renames)));
            }
        }
        return typeRenames;
    }

    /**
     * Заменяет в полном имени типа все части после имени пакета.
     *
     * @param qualifiedName полное имя типа, например {@code bank.Outer.Inner}
     * @param packageName   имя пакета (не изменяется), пустое для пакета по умолчанию
     * @param renames       таблица «старое имя → новое имя»
     * @return новое полное имя типа
     */
    private String renameQualifiedType(String qualifiedName, String packageName, Map<String, String> renames) {
        String prefix = packageName.isEmpty() ? "" : packageName + SEPARATOR;
        String typePath = qualifiedName.substring(prefix.length());
        return prefix + Arrays.stream(typePath.split("\\."))
                .map(part -> renames.getOrDefault(part, part))
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * Вычисляет новое имя в директиве {@code import}.
     *
     * @param declaration директива импорта
     * @param typeRenames таблица полных имён пользовательских типов
     * @param renames     таблица «старое имя → новое имя»
     * @return новое импортируемое имя (или прежнее, если импорт не пользовательский)
     */
    private String newImportName(ImportDeclaration declaration, Map<String, String> typeRenames,
                                 Map<String, String> renames) {
        String name = declaration.getNameAsString();
        if (typeRenames.containsKey(name)) {
            return typeRenames.get(name);
        }
        int separator = name.lastIndexOf(SEPARATOR);
        boolean staticMember = declaration.isStatic() && !declaration.isAsterisk() && separator > 0;
        if (staticMember && typeRenames.containsKey(name.substring(0, separator))) {
            String member = name.substring(separator + 1);
            return typeRenames.get(name.substring(0, separator)) + SEPARATOR + renames.getOrDefault(member, member);
        }
        return name;
    }
}
