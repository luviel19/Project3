package org.example.p2phelper.controller;

import org.example.p2phelper.HelloApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class WriteNumbersToFile {

    static AtomicReference<String> selectedText = new AtomicReference<>();

    public static void setSelectedText(String text) {
        selectedText.set(text);
    }
    public static void writeNumbers(HelloApplication helloApp) throws IOException {
        FileWriter fileWriter = null;
        double previousNumber = Double.NaN;
        String previousSelectedText = null;
        while (true) {
            String selectedTextValue = selectedText.get();
            if (selectedTextValue == null || selectedTextValue.isEmpty()) {
                System.out.println("Выберите файл для записи.");
                try {
                    Thread.sleep(1000); // Задержка в 1 секунду
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!selectedTextValue.equals(previousSelectedText) ) {
                if (fileWriter != null) {
                    fileWriter.close();
                }

                String fileName = "output" + selectedTextValue + ".txt";
                System.out.println("Используем файл: " + fileName);
                fileWriter = new FileWriter(fileName, true);
                System.out.println("Файл открыт для записи.");
                previousSelectedText = selectedTextValue;
            }

            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateTime.format(formatter);
            double number = helloApp.getNumericValue();// Лог

            // Check if the current number is different from the previous one and is not NaN
            if (!Double.isNaN(number) && number != previousNumber &&number>60) {

                fileWriter.write("Цена: " + number + ": " + timestamp + "\n");
                fileWriter.flush(); // Write to file only if the number is different and not NaN
            }

            previousNumber = number; // Update previous number

            // Добавляем задержку между итерациями
            try {
                Thread.sleep(3000); // Задержка в 3 секунды
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}