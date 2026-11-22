package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.TypePatternExpr;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Собирает имена из синтаксических деревьев: объявленные в коде
 * и вообще все встречающиеся идентификаторы.
 */
public class NameCollector {

    /** Имя «безымянной» переменной Java: его заменять нельзя. */
    private static final String UNNAMED_VARIABLE = "_";

    /** Виды узлов, которые объявляют имя: типы, методы, конструкторы, переменные, параметры. */
    private static final List<Class<? extends Node>> DECLARATION_TYPES = List.of(
            TypeDeclaration.class,
            CallableDeclaration.class,
            CompactConstructorDeclaration.class,
            VariableDeclarator.class,
            Parameter.class,
            TypePatternExpr.class,
            EnumConstantDeclaration.class,
            AnnotationMemberDeclaration.class);

    /**
     * Проверяет, является ли имя именем объявления (а не использованием).
     *
     * @param name узел имени
     * @return {@code true}, если родительский узел объявляет это имя
     */
    public static boolean isDeclarationName(SimpleName name) {
        return name.getParentNode()
                .filter(parent -> DECLARATION_TYPES.stream().anyMatch(type -> type.isInstance(parent)))
                .isPresent();
    }

    /**
     * Собирает имена всех классов, методов и переменных, объявленных в файлах.
     *
     * @param units синтаксические деревья файлов
     * @return отсортированное множество объявленных имён
     */
    public Set<String> declaredNames(List<CompilationUnit> units) {
        return units.stream()
                .flatMap(unit -> unit.findAll(SimpleName.class).stream())
                .filter(NameCollector::isDeclarationName)
                .map(SimpleName::getIdentifier)
                .filter(name -> !UNNAMED_VARIABLE.equals(name))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Собирает все идентификаторы, встречающиеся в файлах. Новые имена
     * не должны с ними совпадать, чтобы не возникло конфликтов.
     *
     * @param units синтаксические деревья файлов
     * @return множество всех идентификаторов
     */
    public Set<String> usedIdentifiers(List<CompilationUnit> units) {
        Set<String> identifiers = new HashSet<>();
        for (CompilationUnit unit : units) {
            unit.findAll(SimpleName.class).forEach(name -> identifiers.add(name.getIdentifier()));
            unit.findAll(Name.class).forEach(name -> identifiers.add(name.getIdentifier()));
            unit.findAll(MethodReferenceExpr.class).forEach(reference -> identifiers.add(reference.getIdentifier()));
        }
        return identifiers;
    }
}
