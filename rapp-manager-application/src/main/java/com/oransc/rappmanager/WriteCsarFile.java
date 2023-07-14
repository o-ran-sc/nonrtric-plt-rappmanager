/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * ===============================================================================================
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
 * ============LICENSE_END========================================================================
 */

package com.oransc.rappmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WriteCsarFile {

    public static void main(String[] args) {
        String zipFileName =
                "rapp-manager-application\\src\\main\\resources\\resource-csar\\rapp.csar";
        String csarPath =
                "rapp-manager-application\\src\\main\\resources\\resource-csar";

        try {
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            File directory = new File(csarPath);
            System.out.println("Is directory " + directory.isDirectory());
            List<File> fileList = new ArrayList<>();
            getFiles(directory, directory, fileList);
            File[] files = fileList.toArray(new File[0]);
            System.out.println("File size :" + files.length);
            Arrays.sort(files, Collections.reverseOrder());


            for (File file : files) {
                System.out.println("Processing " + file.getPath());
                if (!file.isDirectory()) {
                    System.out.println("Processing " + file.getPath());
                    FileInputStream fis = new FileInputStream(csarPath + File.separator + file.getPath());

                    ZipEntry zipEntry = new ZipEntry(file.getPath().replaceAll("\\\\", "/"));
                    zos.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                    fis.close();
                } else {
                    System.out.println("Not a file: " + file.getPath());
                }
            }

            zos.close();
            fos.close();

            System.out.println("Zip file created successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getFiles(File baseDirectory, File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getFiles(baseDirectory, file, fileList);
                } else {
                    fileList.add(new File(file.getPath().replace(baseDirectory.getPath() + File.separator, "")));
                }
            }
        }
    }
}
