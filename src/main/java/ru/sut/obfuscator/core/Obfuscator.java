package ru.sut.obfuscator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Обфускатор исходного кода: переименовывает классы, методы и переменные
 * во всех переданных файлах и записывает результат в отдельный каталог.
 *
 * <p>Класс только координирует этапы работы; сама логика находится
 * в классах-помощниках этого пакета.</p>
 */
public class Obfuscator {

    private static final Logger LOGGER = LogManager.getLogger(Obfuscator.class);

    private final NameCollector nameCollector = new NameCollector();
    private final ProtectedNameCollector protectedNameCollector = new ProtectedNameCollector();
    private final Renamer renamer = new Renamer();
    private final SourceWriter writer = new SourceWriter();

    /**
     * Выполняет обфускацию.
     *
     * @param inputs          файлы {@code *.java} и/или каталоги с ними
     * @param outputDirectory каталог, в который записывается результат
     * @return сведения о записанных файлах и заменённых именах
     * @throws ObfuscationException если входные данные некорректны или результат не удалось записать
     */
    public ObfuscationResult obfuscate(List<Path> inputs, Path outputDirectory) throws ObfuscationException {
        SourceLoader loader = new SourceLoader();
        List<SourceFile> sources = loader.load(inputs, outputDirectory);
        List<CompilationUnit> units = sources.stream().map(SourceFile::unit).toList();
        units.forEach(Obfuscator::removeComments);

        Set<String> declaredNames = nameCollector.declaredNames(units);
        Set<String> renamableNames = new TreeSet<>(declaredNames);
        renamableNames.removeAll(protectedNameCollector.collect(units));

        UsageResolver resolver = new UsageResolver(loader.getTypeSolver());
        ReferenceCollector references = new ReferenceCollector(resolver, renamableNames);
        units.forEach(references::collect);
        renamableNames.removeAll(references.getUnresolvedNames());

        Map<String, String> renames = createRenames(renamableNames, nameCollector.usedIdentifiers(units));
        renamer.apply(units, references.getTargets(), renames);
        Map<Path, Path> files = writer.write(sources, renames, outputDirectory);

        Set<String> keptNames = new TreeSet<>(declaredNames);
        keptNames.removeAll(renames.keySet());
        LOGGER.info("Renamed {} name(s), kept {} unchanged", renames.size(), keptNames.size());
        return new ObfuscationResult(outputDirectory.toAbsolutePath().normalize(), files, renames, keptNames);
    }

    /**
     * Удаляет из файла все комментарии: они раскрывают смысл кода и исходные имена.
     *
     * @param unit синтаксическое дерево файла
     */
    private static void removeComments(CompilationUnit unit) {
        unit.getAllComments().forEach(Comment::remove);
    }

    /**
     * Назначает каждому заменяемому имени новое сгенерированное имя.
     *
     * @param names           имена, которые нужно заменить
     * @param usedIdentifiers все идентификаторы исходного кода (новые имена не должны с ними совпадать)
     * @return отсортированная таблица «старое имя → новое имя»
     */
    private Map<String, String> createRenames(Set<String> names, Set<String> usedIdentifiers) {
        NameGenerator generator = new NameGenerator(usedIdentifiers);
        Map<String, String> renames = new TreeMap<>();
        names.forEach(name -> renames.put(name, generator.next()));
        return renames;
    }
}
