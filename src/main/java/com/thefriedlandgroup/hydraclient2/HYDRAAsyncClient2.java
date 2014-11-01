/*
 * Copyright 2014 The Friedland Group, Inc.
 *
 * MKAM License
 * -----------------------------------------------------------------
 *          STTR DATA RIGHTS
 * Topic Number:    N10A-T019
 * Contract Number: N00014-11-C-0474
 * Contractor Name: The Friedland Group, Inc.
 * Contractor Address: 330 SW 43rd St., Suite K #489, Renton, WA 98057
 * Expiration of STTR data rights Period: Five (5) years after contract completion
 * 
 * The Government's rights to use, modify, reproduce, release, perform, display or disclose
 * technical data or computer software marked with this legend are restricted as provided in
 * paragraph (b)(4) of DFARS 252-277-7018, Rights in Noncommercial Technical Data and Computer
 * Software - Small Business Innovative Research (SBIR) Program.
 * 
 * -----------------------------------------------------------------
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.thefriedlandgroup.hydraclient2;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefriedlandgroup.XMLTools2.ImageFileFilter;
import com.thefriedlandgroup.XMLTools2.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class HYDRAAsyncClient2 {

    HashMap<String, File> idHash;
    final ArrayList<File> iFiles;
    public final String url;
    public final String user;
    public final String password;
    public final File imgDir;

    final static JsonFactory f = new JsonFactory();
    final static ObjectMapper mapper = new ObjectMapper();

    public HYDRAAsyncClient2(String url, String user, String password, File imgDir) {
        Test.testNull(url);
        Test.testNull(user);
        Test.testNull(password);
        Test.testDir(imgDir);

        this.url = url;
        this.user = user;
        this.password = password;
        this.imgDir = imgDir;
        idHash = new HashMap<>();
        iFiles = new ArrayList<>();

        File[] imgs = imgDir.listFiles(new ImageFileFilter());
        for (File img : imgs) {
            File jsonFile = getJsonFile(img);
            if (!jsonFile.exists()) {
                iFiles.add(img);
            }
        }
        System.out.println("Client:" + imgs.length + " image files detected, "
                + iFiles.size() + " without json");
    }

    public void setID(String ID, File file) {
        Test.testNull(ID);
        Test.testNull(file);

        idHash.put(ID, file);
    }

    public File getFile(String ID) {
        return idHash.get(ID);
    }

    public void removeID(String ID) {
        idHash.remove(ID);
    }

    public ArrayList<String> getCurrentIDs() {
        return new ArrayList<>(idHash.keySet());
    }
    
    public int getCurrentIDCount() {
        return idHash.size();
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("usage: <url> <user> <pw> <image dir>");
            System.exit(1);
        }
        final String url = args[0];
        System.out.println("HYDRAClient: URL: " + url);
        String user = args[1];
        String pw = args[2];
        File dir = new File(args[3]);
        HYDRAAsyncClient2 hac2 = new HYDRAAsyncClient2(url, user, pw, dir);
        
        
        if (hac2.iFiles.isEmpty()) {
            System.out.println("no image files left to process in image dir " + hac2.imgDir);
            System.exit(1);
        }
        try {
            Poster poster = new Poster(hac2);
            long start = new Date().getTime();
            
            boolean running = true;
            while (running) {
                try {
                    Thread.sleep(5000);
                    Getter getter = new Getter(hac2);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (hac2.getCurrentIDCount() == 0) {
                    running = false;
                }
            }
            long end = new Date().getTime();
            long elapsed = end - start;
            System.out.println("total running time: " + elapsed);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public static File getJsonFile(File file) {
        return new File(file.getParent() + File.separator + getBaseName(file) + "-picmod.json");
    }

    public static String getBaseName(File file) {
        if (file == null) {
            throw new IllegalArgumentException("null file");
        }
        int pos = file.getName().lastIndexOf(".");
        if (pos != -1) {
            return file.getName().substring(0, pos);
        }
        return file.getName();
    }

}
