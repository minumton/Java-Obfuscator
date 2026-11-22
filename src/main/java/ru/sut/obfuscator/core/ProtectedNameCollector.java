package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Определяет имена, которые нельзя заменять, чтобы программа
 * продолжала компилироваться и работать так же.
 *
 * <p>К ним относятся: точка входа {@code main}, методы класса {@link Object},
 * константы перечислений, элементы аннотаций и методы, которые переопределяют
 * методы классов и интерфейсов JDK (например, {@code run} или {@code compareTo}).</p>
 */
public class ProtectedNameCollector {

    private static final Logger LOGGER = LogManager.getLogger(ProtectedNameCollector.class);

    /** Имена, которые JVM ищет по названию. */
    private static final Set<String> RESERVED_NAMES = Set.of("main", "serialVersionUID");

    /** Методы, которые компилятор сам добавляет в каждое перечисление. */
    private static final Set<String> ENUM_METHOD_NAMES = Set.of("values", "valueOf");

    /**
     * Собирает защищённые имена по всем файлам.
     *
     * @param units синтаксические деревья файлов
     * @return отсортированное множество имён, которые нельзя заменять
     */
    public Set<String> collect(List<CompilationUnit> units) {
        Set<String> names = new TreeSet<>(RESERVED_NAMES);
        names.addAll(methodNamesOf(Object.class));
        units.forEach(unit -> names.addAll(collectFrom(unit)));
        return names;
    }

    /**
     * Возвращает имена всех методов класса JDK.
     *
     * @param type класс, методы которого нужно получить
     * @return множество имён методов
     */
    private static Set<String> methodNamesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Собирает защищённые имена из одного файла.
     *
     * @param unit синтаксическое дерево файла
     * @return множество защищённых имён этого файла
     */
    private Set<String> collectFrom(CompilationUnit unit) {
        Set<String> names = new HashSet<>();
        names.addAll(namesOf(unit, EnumConstantDeclaration.class));
        names.addAll(namesOf(unit, AnnotationMemberDeclaration.class));
        if (!unit.findAll(EnumDeclaration.class).isEmpty()) {
            names.addAll(ENUM_METHOD_NAMES);
        }
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
            names.addAll(overridingNames(type, () -> type.resolve().getAllAncestors()));
        }
        unit.findAll(ObjectCreationExpr.class).stream()
                .filter(creation -> creation.getAnonymousClassBody().isPresent())
                .forEach(creation -> names.addAll(overridingNames(creation, () -> anonymousAncestors(creation))));
        return names;
    }

    /**
     * Возвращает имена всех узлов заданного вида внутри дерева.
     *
     * @param root корень поддерева
     * @param type вид узлов (например, объявления методов)
     * @param <T>  тип узла, у которого есть простое имя
     * @return множество имён найденных узлов
     */
    private static <T extends Node & NodeWithSimpleName<?>> Set<String> namesOf(Node root, Class<T> type) {
        return root.findAll(type).stream()
                .map(node -> node.getNameAsString())
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Возвращает всех предков анонимного класса: тип, от которого он создан, и предков этого типа.
     *
     * @param creation выражение {@code new Тип() { ... }}
     * @return список предков анонимного класса
     */
    private static List<ResolvedReferenceType> anonymousAncestors(ObjectCreationExpr creation) {
        ResolvedReferenceType parent = creation.getType().resolve().asReferenceType();
        List<ResolvedReferenceType> ancestors = new ArrayList<>(parent.getAllAncestors());
        ancestors.add(parent);
        return ancestors;
    }

    /**
     * Находит имена методов класса, которые совпадают с методами внешних предков.
     * Такие методы переопределяют чужой код, поэтому их имена менять нельзя.
     *
     * <p>Если предков определить не удалось (класс из неизвестной библиотеки),
     * защищаются все методы класса.</p>
     *
     * @param typeNode  узел класса (или выражение создания анонимного класса)
     * @param ancestors способ получить всех предков класса
     * @return множество защищённых имён методов
     */
    private Set<String> overridingNames(Node typeNode, Supplier<List<ResolvedReferenceType>> ancestors) {
        Set<String> userMethods = namesOf(typeNode, MethodDeclaration.class);
        Set<String> externalMethods = new HashSet<>();
        try {
            for (ResolvedReferenceType ancestor : ancestors.get()) {
                Optional<ResolvedReferenceTypeDeclaration> declaration = ancestor.getTypeDeclaration();
                if (declaration.isPresent()) {
                    Set<String> target = isUserType(declaration.get()) ? userMethods : externalMethods;
                    target.addAll(declaredMethodNames(declaration.get()));
                }
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Ancestors of a type are unknown, all its methods are kept: {}", e.getMessage());
            return userMethods;
        }
        userMethods.retainAll(externalMethods);
        return userMethods;
    }

    /**
     * Проверяет, объявлен ли тип в обрабатываемых файлах.
     *
     * @param declaration объявление типа
     * @return {@code true} для пользовательского типа, {@code false} для типа из JDK
     */
    private static boolean isUserType(ResolvedReferenceTypeDeclaration declaration) {
        return declaration.toAst().isPresent();
    }

    /**
     * Возвращает имена методов, объявленных непосредственно в типе.
     *
     * @param declaration объявление типа
     * @return множество имён методов
     */
    private static Set<String> declaredMethodNames(ResolvedReferenceTypeDeclaration declaration) {
        return declaration.getDeclaredMethods().stream()
                .map(ResolvedMethodDeclaration::getName)
                .collect(Collectors.toSet());
    }
}
