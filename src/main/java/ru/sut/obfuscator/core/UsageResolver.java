package ru.sut.obfuscator.core;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Определяет, на какое объявление ссылается имя в коде: пользовательское
 * (из обрабатываемых файлов) или внешнее (из JDK).
 *
 * <p>Именно эта проверка позволяет заменить вызов собственного метода {@code size()}
 * и не тронуть {@code list.size()} из стандартной библиотеки.</p>
 */
public class UsageResolver {

    private static final Logger LOGGER = LogManager.getLogger(UsageResolver.class);

    /** Значение «параметры типа не заданы»: тип ищется только по имени. */
    private static final List<ResolvedType> ANY_TYPE_ARGUMENTS = null;

    private final TypeSolver typeSolver;

    /**
     * Создаёт анализатор использований.
     *
     * @param typeSolver источник объявлений пользовательских и стандартных типов
     */
    public UsageResolver(TypeSolver typeSolver) {
        this.typeSolver = typeSolver;
    }

    /**
     * Определяет происхождение вызываемого метода.
     *
     * @param call вызов метода, например {@code account.deposit(5)}
     * @return происхождение объявления метода
     */
    public Origin originOfCall(MethodCallExpr call) {
        Origin origin = attempt(() -> originOf(call.resolve().declaringType()));
        if (origin == Origin.UNKNOWN && call.getScope().isPresent()) {
            return originOfMember(call.getScope().get(), call.getNameAsString());
        }
        return origin;
    }

    /**
     * Определяет происхождение метода в ссылке на метод.
     *
     * @param reference ссылка на метод, например {@code Account::getBalance}
     * @return происхождение объявления метода
     */
    public Origin originOfMethodReference(MethodReferenceExpr reference) {
        Origin origin = attempt(() -> originOf(reference.resolve().declaringType()));
        return origin != Origin.UNKNOWN ? origin : originOfMember(reference.getScope(), reference.getIdentifier());
    }

    /**
     * Запасной способ для методов: определяет происхождение по типу выражения слева от точки.
     * Применяется, когда сам вызов разрешить не удалось (например, из-за сложного аргумента).
     *
     * <p>У объекта внешнего типа все методы внешние. У объекта пользовательского типа метод
     * считается пользовательским, если он объявлен в этом типе или в его пользовательских предках.</p>
     *
     * @param scope      выражение, у которого вызывается метод
     * @param methodName имя метода
     * @return происхождение объявления метода
     */
    private Origin originOfMember(Expression scope, String methodName) {
        return attempt(() -> {
            ResolvedType scopeType = scope.calculateResolvedType();
            if (!scopeType.isReferenceType()) {
                return Origin.UNKNOWN;
            }
            ResolvedReferenceType type = scopeType.asReferenceType();
            if (!isUserType(type)) {
                return Origin.EXTERNAL;
            }
            List<ResolvedReferenceType> hierarchy = new ArrayList<>(type.getAllAncestors());
            hierarchy.add(type);
            return originOfMethod(hierarchy, methodName);
        });
    }

    /**
     * Ищет метод по имени в иерархии типов: сначала в пользовательских типах, затем во внешних.
     *
     * @param hierarchy  тип и все его предки
     * @param methodName имя метода
     * @return происхождение метода либо {@link Origin#UNKNOWN}, если метод не найден
     */
    private static Origin originOfMethod(List<ResolvedReferenceType> hierarchy, String methodName) {
        List<ResolvedReferenceType> owners = hierarchy.stream()
                .filter(type -> declaresMethod(type, methodName))
                .toList();
        if (owners.isEmpty()) {
            return Origin.UNKNOWN;
        }
        return owners.stream().anyMatch(UsageResolver::isUserType) ? Origin.USER : Origin.EXTERNAL;
    }

    /**
     * Проверяет, объявлен ли в типе метод с заданным именем.
     *
     * @param type       проверяемый тип
     * @param methodName имя метода
     * @return {@code true}, если тип сам объявляет такой метод
     */
    private static boolean declaresMethod(ResolvedReferenceType type, String methodName) {
        return type.getTypeDeclaration().stream()
                .flatMap(declaration -> declaration.getDeclaredMethods().stream())
                .anyMatch(method -> method.getName().equals(methodName));
    }

    /**
     * Проверяет, объявлен ли тип в обрабатываемых файлах.
     *
     * @param type проверяемый тип
     * @return {@code true} для пользовательского типа
     */
    private static boolean isUserType(ResolvedReferenceType type) {
        return type.getTypeDeclaration().filter(declaration -> declaration.toAst().isPresent()).isPresent();
    }

    /**
     * Определяет происхождение простого имени в выражении: это может быть
     * переменная, поле или имя класса (как в {@code Math.max}).
     *
     * @param name имя в выражении
     * @return происхождение объявления
     */
    public Origin originOfName(NameExpr name) {
        return originOfValueOrType(name, name::resolve);
    }

    /**
     * Определяет происхождение имени после точки: поля ({@code this.balance})
     * или класса в составном имени ({@code bank.Account}).
     *
     * @param access обращение через точку
     * @return происхождение объявления
     */
    public Origin originOfFieldAccess(FieldAccessExpr access) {
        return originOfValueOrType(access, access::resolve);
    }

    /**
     * Определяет происхождение типа. В ссылках на методы на месте типа может
     * стоять переменная ({@code list::add}), поэтому проверяется и этот вариант.
     *
     * @param type имя типа в коде
     * @return происхождение объявления
     */
    public Origin originOfType(ClassOrInterfaceType type) {
        Origin origin = originOfTypeName(type, type.getNameWithScope());
        if (origin == Origin.UNKNOWN && type.getScope().isEmpty() && isMethodReferenceScope(type)) {
            return originOfVariable(type, type.getNameAsString());
        }
        return origin;
    }

    /**
     * Определяет происхождение типа по его имени.
     *
     * @param context  узел, в области видимости которого ищется тип
     * @param typeName простое или составное имя типа
     * @return происхождение объявления типа
     */
    public Origin originOfTypeName(Node context, String typeName) {
        return attempt(() -> {
            SymbolReference<ResolvedTypeDeclaration> reference =
                    contextOf(context).solveType(typeName, ANY_TYPE_ARGUMENTS);
            if (!reference.isSolved()) {
                return Origin.UNKNOWN;
            }
            ResolvedTypeDeclaration declaration = reference.getCorrespondingDeclaration();
            return declaration.isTypeParameter() ? Origin.EXTERNAL : originOf(declaration);
        });
    }

    /**
     * Сначала пробует разрешить выражение как переменную или поле,
     * а если не получилось — как имя типа.
     *
     * @param expression выражение-имя
     * @param value      способ получить объявление переменной или поля
     * @return происхождение объявления
     */
    private Origin originOfValueOrType(Expression expression, Supplier<ResolvedValueDeclaration> value) {
        Origin origin = attempt(() -> originOf(value.get()));
        return origin != Origin.UNKNOWN ? origin : originOfTypeName(expression, expression.toString());
    }

    /**
     * Определяет происхождение переменной по имени в области видимости узла.
     *
     * @param context узел, в области видимости которого ищется переменная
     * @param name    имя переменной
     * @return происхождение объявления переменной
     */
    private Origin originOfVariable(Node context, String name) {
        return attempt(() -> {
            SymbolReference<? extends ResolvedValueDeclaration> reference = contextOf(context).solveSymbol(name);
            return reference.isSolved() ? originOf(reference.getCorrespondingDeclaration()) : Origin.UNKNOWN;
        });
    }

    /**
     * Проверяет, стоит ли тип слева от {@code ::} в ссылке на метод.
     *
     * @param type имя типа в коде
     * @return {@code true}, если это левая часть ссылки на метод
     */
    private static boolean isMethodReferenceScope(ClassOrInterfaceType type) {
        Node parent = type.getParentNode().orElse(null);
        while (parent instanceof ClassOrInterfaceType) {
            parent = parent.getParentNode().orElse(null);
        }
        return parent instanceof TypeExpr;
    }

    /**
     * Возвращает область видимости для узла.
     *
     * @param node узел синтаксического дерева
     * @return контекст разрешения имён
     */
    private Context contextOf(Node node) {
        return JavaParserFactory.getContext(node, typeSolver);
    }

    /**
     * Определяет происхождение переменной, параметра или поля.
     * Поле проверяется по классу-владельцу, остальное — по наличию в исходном коде.
     *
     * @param value объявление значения
     * @return {@link Origin#USER} или {@link Origin#EXTERNAL}
     */
    private static Origin originOf(ResolvedValueDeclaration value) {
        if (value.isField()) {
            return originOf(value.asField().declaringType());
        }
        return value.toAst().isPresent() ? Origin.USER : Origin.EXTERNAL;
    }

    /**
     * Определяет происхождение типа: пользовательские типы связаны с узлом исходного кода.
     *
     * @param type объявление типа
     * @return {@link Origin#USER} или {@link Origin#EXTERNAL}
     */
    private static Origin originOf(ResolvedTypeDeclaration type) {
        return type.toAst().isPresent() ? Origin.USER : Origin.EXTERNAL;
    }

    /**
     * Выполняет разрешение имени и превращает любую неудачу в {@link Origin#UNKNOWN}.
     * Библиотека разбора может не справиться со сложным выражением; в этом случае
     * имя считается неразрешённым и не заменяется.
     *
     * @param resolution действие, определяющее происхождение
     * @return результат действия либо {@link Origin#UNKNOWN} при ошибке
     */
    private static Origin attempt(Supplier<Origin> resolution) {
        try {
            return resolution.get();
        } catch (RuntimeException | StackOverflowError e) {
            LOGGER.trace("Resolution failed: {}", e.toString());
            return Origin.UNKNOWN;
        }
    }
}
