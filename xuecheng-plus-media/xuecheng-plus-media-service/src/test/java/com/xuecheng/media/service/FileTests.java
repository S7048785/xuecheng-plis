package com.xuecheng.media.service;

import org.junit.jupiter.api.Test;

import java.io.*;

public class FileTests {

    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("F:\\resource\\video\\“有些人天生就是主角“ - 1.“有些人天生就是主角“(Av113838359846767,P1).mp4");
        String chunkPath = "F:\\resource\\video\\chunk\\";
        // 分片大小
        long chunkSize = 1024 * 1024 * 5;
        // 分片数量
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        // 缓冲区大小
        byte[] bytes = new byte[1024];
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            for (int i = 0; i < chunkNum; i++) {
                // 分片
                File file = new File(chunkPath + i);
                // 写入文件
                FileOutputStream fos = new FileOutputStream(file);
                int len = 0;
                while ((len = fis.read(bytes)) != -1) {
                    // 写入分片
                    fos.write(bytes, 0, len);
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 合并分片
     * @throws IOException
     */
    @Test
    public void testMerge() throws IOException {
        File chunkPath = new File("F:\\resource\\video\\chunk\\");
        File sourceFile = new File("F:\\resource\\video\\2.mp4");
        // 获取分片文件夹下的所有文件
        File[] files = chunkPath.listFiles();

        byte[] bytes = new byte[1024];
        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
            if (files != null) {
                for (File file : files) {
                    FileInputStream fis = new FileInputStream(file);
                    int len = 0;
                    while ((len = fis.read(bytes)) != -1) {
                        fos.write(bytes, 0, len);
                    }
                    fis.close();
                }
            }
        }
    }
}
