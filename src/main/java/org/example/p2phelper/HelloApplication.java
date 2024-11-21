package org.example.p2phelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.example.p2phelper.controller.RealTimePriceChart;
import org.example.p2phelper.controller.WriteNumbersToFile;
import org.jfree.ui.RefineryUtilities;


public class HelloApplication {
    private JWindow triggerWindow;
    private Stage triggerStage;
    private Point windowLocation;
    private double exitx;
    private double exity;
    private double joinx;
    private double joiny;
    private double sellx;
    private double selly;
    private double sellW;
    private double sellH;
    private Dimension sell2;

    String variableName = "TESSDATA_PREFIX";
    String folderPath = System.getProperty("user.dir");
    String variableValue = folderPath + "/tesseract-5.4.1/tessdata/";
    private static boolean isWindowReady = false;
    private static int resizeDirection = 0; // 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W, 8=NW
    private static final int RESIZE_BORDER = 5;
    private boolean isTriggerWindowVisible = false;
    private boolean isPaused = true;
    private double newNumericValue = Double.NaN;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean shouldCheckRecognizedText = true;
    private ScheduledExecutorService periodicScheduler = Executors.newScheduledThreadPool(1);
    private String lastRecognizedText = "";
    private long lastUpdateTime = System.currentTimeMillis();




    @FXML
    private SplitMenuButton Valet;
    @FXML
    private ToggleButton autoAccept;

    @FXML
    private TextArea consoleOutput;

    @FXML
    private TextField maxByPrice;

    @FXML
    private TextField maxCurs;

    @FXML
    private TextField minPrice;

    @FXML
    private ToggleButton pause;

    @FXML
    private ToggleButton sellprice;
    @FXML
    private ToggleButton putoBy;

    @FXML
    private ToggleButton trigger;

    @FXML
    private ToggleButton settingJoin;

    @FXML
    private ToggleButton settingOut;
    @FXML
    private Button Graph;

    private Stage coordinateStage;
    private Stage coordinatePane;
    private Stage coordinatesell;
    private boolean isTransparentWindowVisible = false;
    private boolean isPaneVisible = false;
    private boolean isSellVisible = false;
    private boolean isAutoBy = false;
    private boolean isautoAccept = false;
    private boolean isScriptRunning;
    private Dimension windowSize;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private String selectedText;
    private RealTimePriceChart chart;
    private String selectedTextValue;
    @FXML
    private void initialize() throws UnsupportedEncodingException, FileNotFoundException {
        variableValue = variableValue.replace("/", "\\");
        variableValue = variableValue.replaceAll("file:", "").replaceAll("^/", "").replaceAll("^/", "");
        try {
            Process process = Runtime.getRuntime().exec("reg add \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment\" /v " + variableName + " /t REG_SZ /d " + variableValue + " /f /reg:64");
            process.waitFor();

            System.out.println("Системная переменная создана успешно!");
        } catch (Exception e) {
            System.out.println("Ошибка создания системной переменной: " + e.getMessage());
        }

        //// Создание переменной для пользователя
        try {
            Process process = Runtime.getRuntime().exec("reg add \"HKCU\\Environment\" /v " + variableName + " /t REG_SZ /d " + variableValue + " /f");
            process.waitFor();

            System.out.println("Переменная для пользователя создана успешно!");
        } catch (Exception e) {
            System.out.println("Ошибка создания переменной для пользователя: " + e.getMessage());
        }

        for (MenuItem item : Valet.getItems()) {
            item.setOnAction(event -> {
                String newText = item.getText();
                Valet.setText(newText);
                WriteNumbersToFile.setSelectedText(newText);
                System.out.println("Выбор изменен на: " + newText);
                if (chart != null) {
                    chart.dispose();
                    chart = null;
                }
            });
        }

        Graph.setOnAction(event -> {
            if (chart == null || !chart.isVisible()) {
                String selectedTextValue = Valet.getText();
                chart = new RealTimePriceChart("График цен:" + selectedTextValue);
                chart.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                chart.pack();
                RefineryUtilities.centerFrameOnScreen(chart);
                chart.setVisible(true);
            } else {
                chart.dispose();
                chart = null;
            }
        });


        originalErr = System.err;
        originalOut = System.out;
        // Перенаправляем вывод консоли в TextArea
        PrintStream consoleStream = new PrintStream(new OutputStream() {
            @Override
            public void write(byte[] b) throws IOException {
                String str = new String(b, "UTF-8");
                Platform.runLater(() -> consoleOutput.appendText(str));
            }

            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                String str = new String(b, off, len, "UTF-8");
                Platform.runLater(() -> consoleOutput.appendText(str));
            }
        });
        System.setOut(consoleStream);

        PrintStream errorStream = new PrintStream("CrashLog.txt");
        System.setErr(errorStream);

        maxCurs.setOnAction(event -> {
            String newText = maxCurs.getText();
            System.out.println("Новый текст: " + newText);
            // Дальнейшие действия с обновленным текстом
        });
        maxByPrice.setOnAction(event -> {
            String newText = maxByPrice.getText();
            System.out.println("Новый текст: " + newText);
            // Дальнейшие действия с обновленным текстом
        });
        minPrice.setOnAction(event -> {
            String newText = minPrice.getText();
            System.out.println("Новый текст: " + newText);
            // Дальнейшие действия с обновленным текстом
        });




        settingJoin.setOnAction(event -> {
            if (isTransparentWindowVisible) {
                coordinateStage.close();
                isTransparentWindowVisible = false;
            } else {
                createTransparentWindow();
                isTransparentWindowVisible = true;
            }
        });

        settingOut.setOnAction(event -> {
            if (isPaneVisible) {
                coordinatePane.close();
                isPaneVisible = false;
            } else {
                createPane();
                isPaneVisible = true;
            }
        });
        sellprice.setOnAction(event -> {
            if (isSellVisible) {
                coordinatesell.close();
                isSellVisible = false;
            } else {
                createSell();
                isSellVisible = true;
            }
        });
        putoBy.setOnAction(event -> {
            if (isAutoBy) {
                isAutoBy = false;
            } else {
                isAutoBy = true;
            }
        });
        autoAccept.setOnAction(event -> {
            if (isautoAccept) {
                isautoAccept = false;
            } else {
                isautoAccept = true;
            }
        });
        trigger.setOnAction(event -> {
            if (trigger.isSelected()) {
                showTriggerWindow();
            } else {
                hideTriggerWindow();
            }
        });

        pause.setOnAction(event -> {
            if (pause.isSelected()) {
                pauseScript();
                pause.setText("Старт ");
                shouldCheckRecognizedText = false;
            } else {
                resumeScript();
                pause.setText("Пауза");
                shouldCheckRecognizedText = true;
            }
        });
    }


    private Clip clip;

    public void playSound(String filePath) {
        try {
            // Загружаем аудио-файл
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Начинаем воспроизведение
            clip.start();

            // Ожидаем окончания воспроизведения
            while (clip.getFramePosition() < clip.getFrameLength()) {
                Thread.sleep(10);
            }

            // Закрываем аудио-файл
            clip.close();
        } catch (Exception e) {

        }
    }


    private void createTransparentWindow() {
        coordinateStage = new Stage();
        coordinateStage.initStyle(StageStyle.TRANSPARENT);
        coordinateStage.initModality(Modality.NONE);

        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: rgb(16,255,0);"); // semi-transparent background
        pane.setPrefSize(10, 10);

        pane.setOnMousePressed(event -> {
            coordinateStage.setX(event.getScreenX());
            coordinateStage.setY(event.getScreenY());
            joinx = event.getScreenX();
            joiny = event.getScreenY();
        });

        pane.setOnMouseDragged(event -> {
            coordinateStage.setX(event.getScreenX());
            coordinateStage.setY(event.getScreenY());
            joinx = event.getScreenX();
            joiny = event.getScreenY();

        });

        coordinateStage.xProperty().addListener((observable, oldValue, newValue) -> {

            // Save the x coordinate to a variable or a file
        });

        coordinateStage.yProperty().addListener((observable, oldValue, newValue) -> {

            // Save the y coordinate to a variable or a file
        });

        coordinateStage.setScene(new Scene(pane));
        coordinateStage.show();
    }

    private void createPane() {
        coordinatePane = new Stage();
        coordinatePane.initStyle(StageStyle.TRANSPARENT);
        coordinatePane.initModality(Modality.NONE);

        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: rgb(0,44,255);"); // semi-transparent background
        pane.setPrefSize(10, 10);

        pane.setOnMousePressed(event -> {
            coordinatePane.setX(event.getScreenX());
            coordinatePane.setY(event.getScreenY());
            joinx = event.getScreenX();
            joiny = event.getScreenY();
        });

        pane.setOnMouseDragged(event -> {
            coordinatePane.setX(event.getScreenX());
            coordinatePane.setY(event.getScreenY());
            exitx = event.getScreenX();
            exity = event.getScreenY();
        });

        coordinatePane.xProperty().addListener((observable, oldValue, newValue) -> {

            // Save the x coordinate to a variable or a file
        });

        coordinatePane.yProperty().addListener((observable, oldValue, newValue) -> {


            // Save the y coordinate to a variable or a file
        });

        coordinatePane.setScene(new Scene(pane));
        coordinatePane.show();
    }
    private void createSell() {
        coordinatesell = new Stage();
        coordinatesell.initStyle(StageStyle.TRANSPARENT);
        coordinatesell.initModality(Modality.NONE);

        Pane sell = new Pane();
        sell.setStyle("-fx-background-color: rgb(255,0,234);"); // semi-transparent background
        sell.setPrefSize(200, 20);

        sell.setOnMousePressed(event -> {
            coordinatesell.setX(event.getScreenX());
            coordinatesell.setY(event.getScreenY());
            sellx = event.getScreenX();
            selly = event.getScreenY();
            sellH = coordinatesell.getHeight();
            sellW = coordinatesell.getWidth();
        });

        sell.setOnMouseDragged(event -> {
            coordinatesell.setX(event.getScreenX());
            coordinatesell.setY(event.getScreenY());
            sellx = event.getScreenX();
            selly = event.getScreenY();
        });

        coordinatesell.xProperty().addListener((observable, oldValue, newValue) -> {

            // Save the x coordinate to a variable or a file
        });

        coordinatesell.yProperty().addListener((observable, oldValue, newValue) -> {


            // Save the y coordinate to a variable or a file
        });

        coordinatesell.setScene(new Scene(sell));
        coordinatesell.show();
    }

    private void showTriggerWindow() {
        if (triggerWindow == null) {
            createTriggerWindow();
        }
        triggerWindow.setVisible(true);
        isTriggerWindowVisible = true;
        startTracking(); // Запускаем сканирование после создания окна
    }

    private void hideTriggerWindow() {
        if (triggerWindow != null) {
            triggerWindow.setVisible(false);
            isTriggerWindowVisible = false;
            // Debugging
        }
    }

    private void createTriggerWindow() {
        triggerWindow = new JWindow(); // Initialize triggerWindow here
        triggerWindow.setAlwaysOnTop(true);
        triggerWindow.setBackground(new Color(0x76FF0000, true));

        windowLocation =triggerWindow.getLocation();
        windowSize = triggerWindow.getSize();
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point initialPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                initialPoint = e.getPoint();
                resizeDirection = getResizeDirection(e.getPoint(), triggerWindow);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                resizeDirection = 0;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                isWindowReady = true;
                Point point = e.getPoint();
                if (resizeDirection == 0) {
                    triggerWindow.setLocation(triggerWindow.getLocation().x + point.x - initialPoint.x,
                            triggerWindow.getLocation().y + point.y - initialPoint.y);
                    //System.out.println(triggerWindow.getLocation().x +" " + triggerWindow.getLocation().y);

                } else {
                    resizeWindow(triggerWindow, e.getX(), e.getY());
                }

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int resizeDir = getResizeDirection(e.getPoint(), triggerWindow);
                if (resizeDir != 0) {
                    setCursor(resizeDir, triggerWindow);
                } else {
                    triggerWindow.setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        triggerWindow.addMouseListener(mouseAdapter);
        triggerWindow.addMouseMotionListener(mouseAdapter);

        triggerWindow.setSize(60, 40);
        triggerWindow.setLocationRelativeTo(null);
        triggerWindow.setVisible(true);
    }

    private static void resizeWindow(JWindow window, int x, int y) {
        int oldWidth = window.getWidth();
        int oldHeight = window.getHeight();

        switch (resizeDirection) {
            case 1:
                window.setSize(oldWidth, oldHeight - y);
                window.setLocation(window.getLocation().x, window.getLocation().y + y);
                break;
            case 2:
                window.setSize(x, oldHeight - y);
                window.setLocation(window.getLocation().x, window.getLocation().y + y);
                break;
            case 3:
                window.setSize(x, oldHeight);
                break;
            case 4:
                window.setSize(x, y);
                break;
            case 5:
                window.setSize(oldWidth, y);
                break;
            case 6:
                window.setSize(oldWidth - x, y);
                window.setLocation(window.getLocation().x + x, window.getLocation().y);
                break;
            case 7:
                window.setSize(oldWidth - x, oldHeight);
                window.setLocation(window.getLocation().x + x, window.getLocation().y);
                break;
            case 8:
                window.setSize(oldWidth - x, oldHeight - y);
                window.setLocation(window.getLocation().x + x, window.getLocation().y + y);
                break;
        }
    }

    private static int getResizeDirection(Point p, JWindow window) {
        int x = p.x;
        int y = p.y;
        int w = window.getWidth();
        int h = window.getHeight();

        if (y <= RESIZE_BORDER) {
            if (x <= RESIZE_BORDER) return 8; // NW
            else if (x >= w - RESIZE_BORDER) return 2; // NE
            else return 1; // N
        } else if (y >= h - RESIZE_BORDER) {
            if (x <= RESIZE_BORDER) return 6; // SW
            else if (x >= w - RESIZE_BORDER) return 4; // SE
            else return 5; // S
        } else {
            if (x <= RESIZE_BORDER) return 7; // W
            else if (x >= w - RESIZE_BORDER) return 3; // E
            else return 0; // Not resizing
        }
    }

    private static void setCursor(int resizeDir, JWindow window) {
        switch (resizeDir) {
            case 1: window.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); break;
            case 2: window.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)); break;
            case 3: window.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)); break;
            case 4: window.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)); break;
            case 5: window.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)); break;
            case 6: window.setCursor (Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)); break;
            case 7: window.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)); break;
            case 8: window.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)); break;
            default: window.setCursor(Cursor.getDefaultCursor()); // Default cursor
        }
    }


    private void pauseScript() {
        isPaused = true;
        lastUpdateTime = System.currentTimeMillis();
    }

    private void resumeScript() {
        isPaused = false;
        lastUpdateTime = System.currentTimeMillis();
        startTracking();
    }

    private void startTracking() {
        ITesseract tesseract = new Tesseract();
        Thread trackingThread = new Thread(() -> {
            while (!isTriggerWindowVisible) {
                try {
                    Thread.sleep(100); // Wait for the window to be ready
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Robot robot = new Robot();
                while (true) {
                    if (!isPaused) {
                        periodicScheduler.scheduleAtFixedRate(this::checkRecognizedText, 10, 10, TimeUnit.SECONDS);

                        //Rectangle screenRect = new Rectangle(triggerWindow.getLocation().x, triggerWindow.getLocation().y, triggerWindow.getWidth(), triggerWindow.getHeight());

                        Rectangle screenRect = new Rectangle(triggerWindow.getLocation().x, triggerWindow.getLocation().y, triggerWindow.getWidth(), triggerWindow.getHeight());
                        if (triggerWindow.getWidth() <= 0 || triggerWindow.getHeight() <= 0) {
                            System.err.println("Invalid dimensions: width = " + triggerWindow.getWidth() + ", height = " + triggerWindow.getHeight());
                            return; // Or handle the error as appropriate
                        }
                        BufferedImage screenCapture = robot.createScreenCapture(screenRect);

                        // Удаление вызова ImagePreprocessing.preprocessImage
                        File outputImage1 = new File("captured_screen_1.jpg");
                        ImageIO.write(screenCapture, "jpg", outputImage1);

                        String recognizedText1 = tesseract.doOCR(outputImage1);
                        recognizedText1 = recognizedText1.replaceAll("[^0-9,.]", "");
                        recognizedText1 = recognizedText1.replace(",", ".");

                        // Обработка второго скрина
                        // screenRect = new Rectangle((int) sellx, (int) selly, (int) sellW, (int) sellH);
                        screenRect = new Rectangle((int) sellx, (int) selly, (int) sellW, (int) sellH);
                        if (sellW <= 0 || sellH <= 0) {
                            System.err.println("Invalid dimensions: width = " + sellW + ", height = " + sellH);
                            return; // Or handle the error as appropriate
                        }
                        BufferedImage screenCapture2 = robot.createScreenCapture(screenRect);

// Проверка, что изображение успешно захвачено
                        if (screenCapture2 != null) {
                            // Масштабирование изображения
                            BufferedImage scaledImage = new BufferedImage((int) (screenCapture2.getWidth() * 5), (int) (screenCapture2.getHeight() * 5), screenCapture2.getType());
                            Graphics2D g = scaledImage.createGraphics();
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.drawImage(screenCapture2, 0, 0, scaledImage.getWidth(), scaledImage.getHeight(), null);
                            g.dispose();

                            // Выполнение OCR на масштабированном изображении
                            String recognizedText2;
                            synchronized (tesseract) {
                                recognizedText2 = tesseract.doOCR(scaledImage);  // Используем scaledImage напрямую
                            }

                            recognizedText2 = recognizedText2.replaceAll("[^0-9,.— -]", "").replace(",", ".").replace(" ", "");
                            System.out.println(recognizedText1 + " " + recognizedText2);
                            processRecognizedText(recognizedText1, recognizedText2);

                            Thread.sleep(400); // Уменьшаем время ожидания между сканированиями
                        } else {
                            System.out.println("Ошибка: Второй скрин не был успешно захвачен.");
                        }
                    } else {
                        System.out.println("Скрипт на паузе. Ждём продолжения....");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (AWTException | IOException | TesseractException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        trackingThread.setDaemon(true); // Позволяет приложению закрыться, даже если поток работает
        trackingThread.start();
    }






    public void processRecognizedText(String recognizedText1, String recognizedText2) {
        if (!recognizedText1.isEmpty()) {
            lastUpdateTime = System.currentTimeMillis();
            try {
                double maxCOurs = Double.parseDouble(maxCurs.getText());
                newNumericValue = Double.parseDouble(recognizedText1);
                Robot robot = new Robot();

                if (newNumericValue <= maxCOurs && newNumericValue >= 65) {
                    System.out.println(newNumericValue);
                    String[] parts = recognizedText2.split("[—-]");
                    double firstScan = Double.parseDouble(parts[0]);
                    double secondScan = Double.parseDouble(parts[1]);
                    double maxMiney2 = Double.parseDouble(maxByPrice.getText());
                    double minMiney2 = Double.parseDouble(minPrice.getText());
                    System.out.println(firstScan + " " + secondScan);

                    if (secondScan <= maxMiney2) {

                        robot.mouseMove(triggerWindow.getLocation().x, triggerWindow.getLocation().y + 55);
                        robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
                        robot.mouseRelease(InputEvent.BUTTON1_MASK);
                        System.out.println("Цена упала!" + newNumericValue);

                        Thread alarmThread = new Thread(() -> playSound("alarm2.wav"));
                        alarmThread.setDaemon(true); // Позволяет приложению закрыться, даже если поток работает
                        alarmThread.start();
                        shouldCheckRecognizedText = false;

                        if (isAutoBy) {
                            boolean isAutoAccept = false;
                            if (secondScan < maxMiney2 && firstScan >= minMiney2 && secondScan >= minMiney2) {
                                String by = String.valueOf(secondScan);
                                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                StringSelection selection = new StringSelection(by);
                                clipboard.setContents(selection, null);
                                Thread.sleep(500);
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_V);
                                robot.keyRelease(KeyEvent.VK_V);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                Thread.sleep(500);
                                robot.mouseMove(triggerWindow.getLocation().x + 154, triggerWindow.getLocation().y + 500);
                                robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
                                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                                pauseScript();

                                if (isAutoAccept) {
                                    robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
                                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                                }
                            } else if (secondScan >= maxMiney2 && secondScan >= minMiney2) {
                                String by = maxByPrice.getText();
                                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                StringSelection selection = new StringSelection(by);
                                clipboard.setContents(selection, null);
                                Thread.sleep(500);
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_V);
                                robot.keyRelease(KeyEvent.VK_V);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                Thread.sleep(500);
                                pauseScript();

                                if (isAutoAccept) {
                                    robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
                                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                                }
                            }
                        } else {
                            lastUpdateTime = System.currentTimeMillis();
                            pauseScript();
                        }
                    } else {
                        handleHighPrice(newNumericValue, robot);
                    }
                } else {
                    handleHighPrice(newNumericValue, robot);
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка преобразования строки в число: " + e.getMessage());
            } catch (InterruptedException | AWTException e) {
                e.printStackTrace();
            }
        } else {
            if (!isPaused) {
                periodicScheduler.scheduleAtFixedRate(this::checkRecognizedText, 10, 10, TimeUnit.SECONDS);
            }
        }
    }
    private void handleHighPrice(double newNumericValue, Robot robot) throws AWTException, InterruptedException {
        if (!isScriptRunning) {
            isScriptRunning = true;
            lastUpdateTime = System.currentTimeMillis();
            System.out.println("Высокая цена!" + newNumericValue);
            robot.mouseMove(triggerWindow.getLocation().x + 2, triggerWindow.getLocation().y - 130);
            robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Thread.sleep(600);
            robot.mouseMove(triggerWindow.getLocation().x + 78, triggerWindow.getLocation().y + 140);

            robot.mousePress(InputEvent.BUTTON1_MASK); // Нажимаем левую кнопку мыши
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Thread.sleep(1000);

            isScriptRunning = false; // Сбрасываем флаг после выполнения скрипта
        }
    }

    private void checkRecognizedText() {
        if (!shouldCheckRecognizedText) {
            return; // Если флаг отключен, то не выполняем checkRecognizedText
        }
        try {
            Robot robot = new Robot();
            if (!isPaused && System.currentTimeMillis() - lastUpdateTime > 6000) {


                // Если recognizedText пустой в течение 10 секунд, выполняем другой скрипт
                System.out.println("recognizedText был пуст в течение 10 секунд. Выполняется другой скрипт.");

                robot.mouseMove((int) exitx, (int) exity); // Move the mouse to coordinates (100, 200)
                robot.mousePress(InputEvent.BUTTON1_MASK); // Press the left mouse button
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Thread.sleep(300);
                robot.mouseMove((int) joinx, (int) joiny); // Move the mouse to coordinates (100, 200)
                robot.mousePress(InputEvent.BUTTON1_MASK); // Press the left mouse button
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Thread.sleep(10000);
            }
        } catch (AWTException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
    private static BufferedImage setResolution(BufferedImage image, int dpi) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        newImage.createGraphics().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        return newImage;
    }
    public double getNumericValue() {
        return newNumericValue;
    }
    public String getSelectedText() {
        return selectedText;
    }
}
