/**
 * Пример для негативного сценария: файл с синтаксической ошибкой.
 * Обфускатор должен сообщить об ошибке и ничего не записать.
 */
public class Broken {

    public static void main(String[] args) {
        int value = 5
        System.out.println(value);
    }
}
