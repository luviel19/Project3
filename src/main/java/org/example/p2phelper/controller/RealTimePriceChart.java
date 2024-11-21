package org.example.p2phelper.controller;



import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.example.p2phelper.controller.WriteNumbersToFile.selectedText;

public class RealTimePriceChart extends ApplicationFrame {
    String selectedTextValue = selectedText.get();
    private XYSeries series;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String txtFilePath = "output"+selectedTextValue+".txt"; // Update this to your text file path
    private int offsetX;
    private int offsetY;
    private JFreeChart xylineChart;
    public RealTimePriceChart(String title) {

        super(title);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setUndecorated(true);

        series = new XYSeries("Price");
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart xylineChart = ChartFactory.createXYLineChart(
                "Price Over Time",
                "Time",
                "Price",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        JPanel panel = new JPanel();
        panel.setBackground(Color.GRAY);
        panel.setPreferredSize(new Dimension(800, 20));

        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offsetX = e.getX();
                offsetY = e.getY();
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                setLocation(x - offsetX, y - offsetY);
            }
        });

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(panel, BorderLayout.NORTH);

        chartPanel.add(new ChartPanel(xylineChart), BorderLayout.CENTER); // теперь мы можем использовать переменную

        this.setContentPane(chartPanel);

        XYPlot plot = xylineChart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);
        // Изменение осей
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickUnit(new NumberTickUnit(5));
        rangeAxis.setLabelFont(new Font("Serif", Font.BOLD, 14));
        rangeAxis.setRange(0, 200.0);
        rangeAxis.setAutoRange(false);

        StandardXYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator(
                "{0}: ({1}, {2})", dateFormat, NumberFormat.getInstance());
        renderer.setDefaultToolTipGenerator(toolTipGenerator);

        // Set up the X axis to display dates properly
        DateAxis dateAxis = new DateAxis("Time");
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        plot.setDomainAxis(dateAxis);

        ChartPanel chart = new ChartPanel(xylineChart);
        chart.setPreferredSize(new java.awt.Dimension(800, 600));
        chart.setMouseWheelEnabled(true); // Enable zooming with mouse wheel
        chart.setHorizontalAxisTrace(true); // Enable horizontal axis tracing
        chart.setVerticalAxisTrace(true); // Enable vertical axis tracing
        chart.setDomainZoomable(true); // Enable panning
        chart.setRangeZoomable(false); // Disable range zooming (optional)
        chartPanel.add(chart, BorderLayout.CENTER);

        this.setContentPane(chartPanel);

        Timer timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDataset();
            }
        });
        timer.start();
         // Set the content pane
    }


    private void updateDataset() {
        series.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(": ");
                if (parts.length >= 3 && !parts[1].equals("NaN")) {
                    double price = Double.parseDouble(parts[1]);
                    Date date = dateFormat.parse(parts[2]);
                    series.add(date.getTime(), price);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        RealTimePriceChart chart = new RealTimePriceChart("Price Over Time");
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);

    }
}