/*
 * Copyright 2026 rpf.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.pfeifer_syscon.blur.blurandcatch;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.QRCode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStreamImpl;

/**
 * Prove of concept identify barcode.
 * Will be saved as result.png
 *
 * @author rpf
 */
public class BlurAndCatch {
 
    BlurAndCatch() {        
    }
    public BufferedImage progScale(BufferedImage imgSrc, int targetSize) {        
        while (true) {
            double factor = (double)targetSize / imgSrc.getWidth();
            if (factor < 0.5) {
                factor = 0.5;
            }
            int targetWidth =  (int)(imgSrc.getWidth() * factor);
            int targetHeight =  (int)(imgSrc.getHeight() * factor);
            BufferedImage imgRed = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            //Graphics2D gRed = imgRed.createGraphics();
            //double scale = (double)imgRed.getWidth() / (double)imgSrc.getWidth();
            final AffineTransform at = AffineTransform.getScaleInstance(factor, factor);
            final AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            ato.filter(imgSrc, imgRed);        
            imgSrc = imgRed;
            if (targetWidth <= targetSize) {
                break;
            }
        }
        return imgSrc;
    }
    
    public void save(BufferedImage imgSrc, String name) {    
        try {
            ImageIO.write(imgSrc, "PNG", new File(name));        
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
    
    public int scan(BufferedImage imgSrc, int x, int y, int block) {
        int sum = 0;
        for (int xi = 0; xi < block; xi++) {
            for (int yi = 0; yi < block; yi++) {
                int xp = (x + xi);
                int yp = (y + yi);
                if (xp < imgSrc.getWidth() 
                 && yp < imgSrc.getHeight()) {
                    int rgb = imgSrc.getRGB(xp, yp);
                    sum += (((rgb >> 24) & 0xff) + ((rgb >> 16) & 0xff) + (rgb & 0xff)) / 3;
                }
            }
        }
        return sum;
    }
    
    public int[] scan(int[][] arr, int white) {
        for (int x = 1; x < arr.length; ++x) {
            int[] row = arr[x];
            for (int y = 1; y < row.length; ++y) {
                if (arr[x][y] < white 
                 && arr[x-1][y] == white
                 && arr[x][y-1] == white) {     // look for edge 
                    int width = 1;
                    for (int yt = y+1; yt < row.length; ++yt) {
                        if (arr[x][yt] < white) {
                            ++width;
                        }
                        else {
                            break;
                        }
                    }
                    if (width > 1) {    // expect some width
                        int height = 1;
                        for (int xt = x+1; xt < arr.length; ++xt) { // could also check row
                            if (arr[xt][y] < white) {
                                ++height;
                            }
                            else {
                                break;
                            }
                        }
                        if (Math.abs(width - height) <= 1) { // expect rectangular
                            return new int[] {x, y, Math.max(width, height)};
                        }
                    }
                }
            }
        }        
        return new int[] {};
    }
    
    public void test() {
        BufferedImage imgSrc = new BufferedImage(2000, 3000, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgOrg = imgSrc;
        Graphics2D g2 = imgSrc.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, imgSrc.getWidth(), imgSrc.getHeight());
        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
        for (int i = 0; i < 10; ++i) {
            g2.drawString("Hello world! Hello world! Hello world! Hello world!", 30, 30 + 35 * i);
        }
        
        // barcode
        QRCodeWriter code128Writer = new QRCodeWriter();
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        try {
            BitMatrix bitMatrix = code128Writer.encode("BarcodeMessage,BarcodeMessage,BarcodeMessage,BarcodeMessage", BarcodeFormat.QR_CODE, 100, 100, hintMap);
            int block = 3;
            int x = 750;
            int y = 750;
            System.out.format("width %d height %d\n",bitMatrix.getWidth(), bitMatrix.getHeight());            
            for (int j = 0; j < bitMatrix.getHeight(); j++) {
                for (int i = 0; i < bitMatrix.getWidth(); i++) {
                    if (bitMatrix.get(j, i)) {
                        g2.fillRect( x + i * block, y + j * block, block, block);
                    }
                }
            }
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }                
        g2.dispose();
        save(imgSrc, "full.png");
        imgSrc = progScale(imgSrc, 100);
        save(imgSrc, "test.png");
        int block = 3;
        int cnt = 0;
        long sum = 0;
        int max = 0;
        int min = 255 * block * block;
        final int maxVal = 255 * block * block;
        int[][] arr = new int[(imgSrc.getWidth() / block) + 1][(imgSrc.getHeight() / block) + 1];
        for (int y = 0; y < imgSrc.getHeight(); y += block) {
            for (int x = 0; x < imgSrc.getWidth(); x += block) {
                int res = scan(imgSrc, x, y, block);
                arr[x/block][y/block] = res;
                max = Math.max(res, max);
                min = Math.min(res, min);                
                sum += res;
                ++cnt;
                //System.out.format("%08d ", res);
            }
            //System.out.format("\n");
        }
        System.out.format("Min %d Max %d Avg %f\n", min, max, (double)sum/(double)cnt);
        int[] res = scan(arr, maxVal);
        if (res.length > 0) {
            System.out.format("Result %dx%d %d\n", res[0], res[1], res[2]);
            double allScale = block * imgOrg.getWidth() / imgSrc.getWidth();
            int orgX = (int)(res[0] * allScale);
            int orgY = (int)(res[1] * allScale);
            int orgWidth = (int)(res[2] * allScale);
            BufferedImage imgRes = new BufferedImage(orgWidth, orgWidth, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr2 = imgRes.createGraphics();
            //gr2.setColor(Color.WHITE);
            //gr2.fillRect(0, 0, imgRes.getWidth(), imgRes.getHeight());            
            //gr2.setColor(Color.BLACK);
            gr2.drawImage(imgOrg.getSubimage(orgX, orgY, orgWidth, orgWidth), 0, 0, null);
            gr2.dispose();
            save(imgRes, "result.png");
        }
        else {
            System.out.format("No result...\n");
        }
    }
    
    public static void main(String[] args) {
        BlurAndCatch blurAndCatch = new BlurAndCatch();
        blurAndCatch.test();
    }
}
