package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Обходит синтаксическое дерево и находит все места, где встречаются заменяемые имена:
 * объявления и использования.
 *
 * <p>Дерево на этом этапе не изменяется: замены только запоминаются. Если хотя бы одно
 * использование имени не удалось разрешить, имя попадает в список неразрешённых
 * и в итоге не заменяется нигде.</p>
 */
public class ReferenceCollector {

    private static final Logger LOGGER = LogManager.getLogger(ReferenceCollector.class);

    private final UsageResolver resolver;
    private final Set<String> renamableNames;
    private final List<RenameTarget> targets = new ArrayList<>();
    private final Set<String> unresolvedNames = new TreeSet<>();

    /**
     * Создаёт сборщик.
     *
     * @param resolver       анализатор, отличающий пользовательские объявления от внешних
     * @param renamableNames имена, которые разрешено заменять
     */
    public ReferenceCollector(UsageResolver resolver, Set<String> renamableNames) {
        this.resolver = resolver;
        this.renamableNames = renamableNames;
    }

    /**
     * Возвращает найденные места замены.
     *
     * @return список мест, где имя нужно заменить
     */
    public List<RenameTarget> getTargets() {
        return targets;
    }

    /**
     * Возвращает имена, использование которых не удалось разрешить.
     *
     * @return отсортированное множество неразрешённых имён
     */
    public Set<String> getUnresolvedNames() {
        return unresolvedNames;
    }

    /**
     * Находит в файле все объявления и использования заменяемых имён.
     *
     * @param unit синтаксическое дерево файла
     */
    public void collect(CompilationUnit unit) {
        unit.findAll(SimpleName.class).forEach(name ->
                register(name.getIdentifier(), name::setIdentifier, () -> originOf(name)));
        unit.findAll(MethodReferenceExpr.class).forEach(reference ->
                register(reference.getIdentifier(), reference::setIdentifier,
                        () -> resolver.originOfMethodReference(reference)));
        unit.findAll(AnnotationExpr.class).forEach(annotation ->
                collectQualifiedName(annotation, annotation.getName()));
        unit.findAll(ThisExpr.class).forEach(expression ->
                expression.getTypeName().ifPresent(name -> collectQualifiedName(expression, name)));
        unit.findAll(SuperExpr.class).forEach(expression ->
                expression.getTypeName().ifPresent(name -> collectQualifiedName(expression, name)));
    }

    /**
     * Обрабатывает составное имя типа ({@code Outer.Inner}): каждая его часть может быть
     * пользовательским типом. Такие имена встречаются в аннотациях и в {@code Outer.this}.
     *
     * @param context узел, в области видимости которого стоит имя
     * @param name    составное имя
     */
    private void collectQualifiedName(Node context, Name name) {
        register(name.getIdentifier(), name::setIdentifier,
                () -> resolver.originOfTypeName(context, name.asString()));
        name.getQualifier().ifPresent(qualifier -> collectQualifiedName(context, qualifier));
    }

    /**
     * Запоминает место замены, если имя заменяемое и ссылается на пользовательское объявление.
     * Происхождение вычисляется только для заменяемых имён, чтобы не анализировать лишнее.
     *
     * @param identifier текущее имя
     * @param setter     действие, записывающее новое имя в узел
     * @param origin     способ определить происхождение объявления
     */
    private void register(String identifier, Consumer<String> setter, Supplier<Origin> origin) {
        if (!renamableNames.contains(identifier)) {
            return;
        }
        Origin resolved = origin.get();
        if (resolved == Origin.USER) {
            targets.add(new RenameTarget(identifier, setter));
        } else if (resolved == Origin.UNKNOWN && unresolvedNames.add(identifier)) {
            LOGGER.warn("Usage of '{}' could not be resolved, the name is kept unchanged", identifier);
        }
    }

    /**
     * Определяет происхождение имени по виду узла, в котором оно стоит.
     *
     * @param name узел имени
     * @return происхождение объявления, на которое ссылается имя
     */
    private Origin originOf(SimpleName name) {
        Node parent = name.getParentNode().orElse(null);
        if (NameCollector.isDeclarationName(name)) {
            return Origin.USER;
        }
        if (parent instanceof MethodCallExpr call) {
            return resolver.originOfCall(call);
        }
        if (parent instanceof NameExpr expression) {
            return resolver.originOfName(expression);
        }
        if (parent instanceof FieldAccessExpr access) {
            return resolver.originOfFieldAccess(access);
        }
        if (parent instanceof ClassOrInterfaceType type) {
            return resolver.originOfType(type);
        }
        return Origin.EXTERNAL;
    }
}
