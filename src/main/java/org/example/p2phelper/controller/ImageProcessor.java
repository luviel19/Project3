package org.example.p2phelper.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

public class ImageProcessor {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws IOException {
        // Загрузите изображение
        BufferedImage image = ImageIO.read(new File("input.jpg"));

        // Преобразуйте BufferedImage в Mat
        Mat matImage = bufferedImageToMat(image);

        // Примените морфологическую операцию дилатации
        Mat dilatedImage = dilateImage(matImage, 3);

        // Преобразуйте Mat обратно в BufferedImage
        BufferedImage outputImage = matToBufferedImage(dilatedImage);

        // Сохраните результат
        ImageIO.write(outputImage, "jpg", new File("output.jpg"));
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        int[] data = new int[bi.getWidth() * bi.getHeight() * 3];
        bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), data, 0, bi.getWidth());
        mat.put(0, 0, data);
        return mat;
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    private static Mat dilateImage(Mat src, int kernelSize) {
        Mat dst = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        Imgproc.dilate(src, dst, kernel);
        return dst;
    }
}