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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefriedlandgroup.XMLTools2.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class Getter {

    final static JsonFactory f = new JsonFactory();
    final static ObjectMapper mapper = new ObjectMapper();

    public Getter(final HYDRAAsyncClient2 hac2) throws InterruptedException, IOException, URISyntaxException {

        Test.testNull(hac2);

        final CountDownLatch latch = new CountDownLatch(hac2.getCurrentIDs().size());
        final File imgDir = hac2.imgDir;
        final HashMap<String, File> idHash = hac2.idHash;

        ArrayList<Header> harray = new ArrayList<>();
        harray.add(new BasicHeader("Authorization", "Basic " + Base64.encodeBase64String(
                (hac2.user + ":" + hac2.password).getBytes())));

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(hac2.user, hac2.password);
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(3000).build();
        long start = new Date().getTime();
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
                .setDefaultHeaders(harray)
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            httpclient.start();
            for (final String id : hac2.getCurrentIDs()) {
                URIBuilder builder = new URIBuilder();
                builder.setScheme("http").setHost(hac2.url).setPath("/api/v1/results")
                        .setParameter("id", id);
                URI uri = builder.build();
                final HttpGet request = new HttpGet(uri);
                httpclient.execute(request, new FutureCallback<HttpResponse>() {

                    @Override
                    public void completed(final HttpResponse response) {

                        latch.countDown();
                        int code = response.getStatusLine().getStatusCode();
                        if (code == 200) {
                            //System.out.println(request.getRequestLine() + "->" + response.getStatusLine());
                            String imgName = HYDRAAsyncClient2.getBaseName(idHash.get(id));
                            //System.out.println("imgname: " + imgName);
                            //System.out.println("entity: " + response.getEntity());
                            if (response.getEntity() != null) {
                                StringWriter writer = new StringWriter();
                                try {
                                    IOUtils.copy(response.getEntity().getContent(), writer);
                                    //System.out.println("result: " + writer.toString());
                                    StringBuilder sb = new StringBuilder(imgDir.getAbsolutePath());
                                    sb.append(File.separator);
                                    sb.append(imgName);
                                    sb.append(".json");
                                    JsonNode json = mapper.readTree(writer.toString());
                                    String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                                    try (FileWriter fw = new FileWriter(sb.toString())) {
                                        fw.write(s);
                                    }
                                    idHash.remove(id);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        latch.countDown();
                        System.out.println(request.getRequestLine() + "->" + ex);
                    }

                    @Override
                    public void cancelled() {
                        latch.countDown();
                        System.out.println(request.getRequestLine() + " cancelled");
                    }

                });
            }
            latch.await();
            System.out.println("to be processed: "+idHash.size());
            System.out.println("Shutting down");
        }
        long end = new Date().getTime();
        System.out.println("Done: post time elapsed: " + (end - start));
    }
}
