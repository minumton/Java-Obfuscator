package ru.sut.obfuscator.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.sut.obfuscator.cli.CliParser;
import ru.sut.obfuscator.core.ObfuscationException;
import ru.sut.obfuscator.core.ObfuscationResult;
import ru.sut.obfuscator.core.Obfuscator;
import ru.sut.obfuscator.core.ReportFormatter;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/**
 * Графический интерфейс обфускатора: выбор исходных файлов и каталога результата,
 * запуск обфускации и просмотр отчёта.
 */
public class ObfuscatorApp extends Application {

    private static final Logger LOGGER = LogManager.getLogger(ObfuscatorApp.class);
    private static final double SPACING = 8;
    private static final double WINDOW_WIDTH = 760;
    private static final double WINDOW_HEIGHT = 600;

    /** Моноширинный шрифт отчёта: в нём различимы похожие символы новых имён. */
    private static final String REPORT_FONT = "Monospaced";

    private final ObservableList<Path> inputs = FXCollections.observableArrayList();
    private final TextField outputField = new TextField(Path.of(CliParser.DEFAULT_OUTPUT).toAbsolutePath().toString());
    private final TextArea reportArea = new TextArea();
    private final Button runButton = new Button("Obfuscate");
    private Stage stage;

    /**
     * Строит главное окно приложения.
     *
     * @param primaryStage главное окно, созданное JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setFont(Font.font(REPORT_FONT));
        runButton.setOnAction(event -> runObfuscation());
        VBox.setVgrow(reportArea, Priority.ALWAYS);

        VBox root = new VBox(SPACING,
                new Label("Source files and directories:"), createInputList(), createInputButtons(),
                new Label("Output directory:"), createOutputRow(),
                runButton,
                new Label("Report:"), reportArea);
        root.setPadding(new Insets(SPACING * 2));

        stage.setTitle("Java Obfuscator");
        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.show();
    }

    /**
     * Создаёт список выбранных исходных файлов и каталогов.
     *
     * @return элемент управления со списком путей
     */
    private ListView<Path> createInputList() {
        ListView<Path> list = new ListView<>(inputs);
        list.setPrefHeight(130);
        return list;
    }

    /**
     * Создаёт кнопки добавления файлов, добавления каталога и очистки списка.
     *
     * @return строка с кнопками
     */
    private HBox createInputButtons() {
        Button addFiles = new Button("Add files...");
        addFiles.setOnAction(event -> addFiles());
        Button addDirectory = new Button("Add directory...");
        addDirectory.setOnAction(event -> addDirectory());
        Button clear = new Button("Clear");
        clear.setOnAction(event -> inputs.clear());
        return new HBox(SPACING, addFiles, addDirectory, clear);
    }

    /**
     * Создаёт строку с полем каталога результата и кнопкой его выбора.
     *
     * @return строка с полем и кнопкой
     */
    private HBox createOutputRow() {
        Button browse = new Button("Browse...");
        browse.setOnAction(event -> chooseOutputDirectory());
        HBox.setHgrow(outputField, Priority.ALWAYS);
        return new HBox(SPACING, outputField, browse);
    }

    /**
     * Открывает диалог выбора java-файлов и добавляет выбранные файлы в список.
     */
    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Java source files");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java source files", "*.java"));
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) {
            files.forEach(this::addInput);
        }
    }

    /**
     * Открывает диалог выбора каталога и добавляет каталог в список.
     */
    private void addDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a directory with Java source files");
        File directory = chooser.showDialog(stage);
        if (directory != null) {
            addInput(directory);
        }
    }

    /**
     * Добавляет путь в список исходных данных, если его там ещё нет.
     *
     * @param file выбранный файл или каталог
     */
    private void addInput(File file) {
        Path path = file.toPath();
        if (!inputs.contains(path)) {
            inputs.add(path);
        }
    }

    /**
     * Открывает диалог выбора каталога результата.
     */
    private void chooseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the output directory");
        File directory = chooser.showDialog(stage);
        if (directory != null) {
            outputField.setText(directory.getAbsolutePath());
        }
    }

    /**
     * Проверяет введённые данные и запускает обфускацию в фоновом потоке,
     * чтобы окно не «зависало» во время работы.
     */
    private void runObfuscation() {
        if (inputs.isEmpty()) {
            reportArea.setText("Add at least one .java file or directory.");
            return;
        }
        Path output;
        try {
            output = Path.of(outputField.getText().trim());
        } catch (InvalidPathException e) {
            reportArea.setText("Invalid output directory: " + outputField.getText());
            return;
        }
        startTask(createTask(List.copyOf(inputs), output));
    }

    /**
     * Создаёт фоновую задачу обфускации.
     *
     * @param sources исходные файлы и каталоги
     * @param output  каталог результата
     * @return задача, возвращающая итог обфускации
     */
    private Task<ObfuscationResult> createTask(List<Path> sources, Path output) {
        return new Task<>() {
            @Override
            protected ObfuscationResult call() throws ObfuscationException {
                return new Obfuscator().obfuscate(sources, output);
            }
        };
    }

    /**
     * Запускает задачу и настраивает показ результата или ошибки по её завершении.
     *
     * @param task фоновая задача обфускации
     */
    private void startTask(Task<ObfuscationResult> task) {
        runButton.setDisable(true);
        reportArea.setText("Working...");
        task.setOnSucceeded(event -> showResult(task.getValue()));
        task.setOnFailed(event -> showError(task.getException()));
        Thread thread = new Thread(task, "obfuscation");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Показывает отчёт об успешной обфускации.
     *
     * @param result итог обфускации
     */
    private void showResult(ObfuscationResult result) {
        runButton.setDisable(false);
        reportArea.setText("Done." + System.lineSeparator() + new ReportFormatter().format(result));
    }

    /**
     * Показывает сообщение об ошибке и записывает её в лог.
     *
     * @param error исключение, прервавшее обфускацию
     */
    private void showError(Throwable error) {
        runButton.setDisable(false);
        LOGGER.error("Obfuscation failed: {}", error.getMessage());
        LOGGER.debug("Error details", error);
        boolean expected = error instanceof ObfuscationException;
        reportArea.setText((expected ? "Error: " : "Unexpected error: ") + error.getMessage());
    }
}
