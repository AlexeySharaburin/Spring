package ru.netology;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerThread extends Thread {

    private final Socket socket;
    private final List<String> validPaths;
    private BufferedReader in;
    private BufferedOutputStream out;

    public ServerThread(Socket socket, List<String> validPaths) {
        this.socket = socket;
        this.validPaths = validPaths;
    }

    @Override
    public void run() {

        try {
            while (true) {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedOutputStream(socket.getOutputStream());
                processingRequest();

                System.out.println("QueryString ->" + getQueryParam(in.readLine()));
                printMap(getQueryParams(in.readLine()));

                System.out.println("x-www-form-urlencoded Request:" + getPostParam(in.readLine()));

                printMapXwww(getPostParamsXwww(in.readLine()));

                System.out.println("MultiValuedMap variant:");
                getPostParam(in.readLine());


                getPart(in.readLine());
                System.out.println("Multipart/form-data parameters:");
                printMapXwww(getParts(in.readLine()));


            }
        } catch (IOException | URISyntaxException ioException) {
            ioException.printStackTrace();
        }
    }


    public void processingRequest() {

        while (true) {
            try {
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    continue;
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    continue;
                }

                final var filePath = Paths.get(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);
                final var lenght = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + lenght + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getQueryParam(String name) {
        String query;
        String[] urlParts = name.split("\\?");
        query = urlParts[1];
        System.out.println("Path -> " + urlParts[0]);
        return query;
    }

    public Map<String, String> getQueryParams(String name) throws URISyntaxException {

        Map<String, String> params = new HashMap<>();

        List<NameValuePair> data = URLEncodedUtils.parse(new URI(name), StandardCharsets.UTF_8);

        for (NameValuePair nvp : data) {
            params.put(nvp.getName(), nvp.getValue());
        }
        return params;
    }

    public static void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", value: " + entry.getValue());
        }
    }


    public String getPostParam(String name) {
        String[] line = name.split("\\?");
        System.out.println("Path x-www-form-urlencoded Request: ->" + line[0]);
        return line[0];
    }

    public MultiValuedMap<String, String> getPostParams(String stringName) {

        MultiValuedMap<String, String> params = new ArrayListValuedHashMap<>();

        String[] urlParts = stringName.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            String[] pairs = query.split("\\&");
            for (int i = 0; i < pairs.length; i++) {
                String[] data = pairs[i].split("=");
                String name = URLDecoder.decode(data[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(data[1], StandardCharsets.UTF_8);
                params.put(name, value);
            }

        }

        for (Map.Entry<String, String> param : params.entries()) {
            System.out.println("Key: " + param.getKey() + ", value: " + param.getValue());
        }
        return params;
    }

    public Map<String, List<String>> getPostParamsXwww(String stringName) {

        Map<String, List<String>> params = new HashMap<>();

        String[] urlParts = stringName.split("\\?");

        if (urlParts.length > 1) {
            String query = urlParts[1];
            String[] pairs = query.split("\\&");
            for (int i = 0; i < pairs.length; i++) {
                String[] data = pairs[i].split("=");
                String name = URLDecoder.decode(data[0], StandardCharsets.UTF_8);
                String value = "";
                if (data.length > 1) {
                    value = URLDecoder.decode(data[1], StandardCharsets.UTF_8);
                }
                List<String> values = params.get(name);
                if (values == null) {
                    values = new ArrayList<>();
                    params.put(name, values);
                }
                values.add(value);
            }

        }
        return params;
    }

    public static void printMapXwww(Map<String, List<String>> map) {
        for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
            System.out.print("Name: " + mapEntry.getKey());
            System.out.print(", value: ");
            for (String value : mapEntry.getValue()) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    public String getPart(String name) {
        String[] line = name.split("\\?");
        System.out.println("Path multipart/form-data: ->" + line[0]);
        return line[0];
    }


    public Map<String, List<String>> getParts(String stringName) throws IOException {

        Map<String, List<String>> params = new HashMap<>();

        String pathName = "Data.jpg";

        String[] string = stringName.split("\\?"); // получаем часть строки с запросами

        String boundary = string[1].substring(string[1].indexOf("boundary=--"), string[1].indexOf(",")); // получаем границу

        String[] bodyString = string[1].split("\r\n"); // получаем тело запроса

        String[] fileBody = bodyString[1].split("\r\n\r\n"); // получаем  тело файла

        String[] pairs = bodyString[1].split(boundary); // получаем пары параметров  в виде "Content-Disposition: form-data; name="value""

        for (int i = 0; i < pairs.length; i++) {
            String[] data = pairs[i].split("=");
            String name = URLDecoder.decode(data[0].substring(0, data[0].indexOf(";")).trim(), StandardCharsets.UTF_8);
            if (name.equals("name")) {
                String value = "";
                if (data.length > 1) {
                    value = URLDecoder.decode(data[1].trim(), StandardCharsets.UTF_8);
                }
                List<String> values = params.get(name);
                if (values == null) {
                    values = new ArrayList<>();
                    params.put(name, values);
                }
                values.add(value);
            } else {
                System.out.println("Прикреплён файл" + URLDecoder.decode(data[1].trim(), StandardCharsets.UTF_8));
            }

            final byte[] bytes = fileBody[1].getBytes(StandardCharsets.UTF_8); // записываем в файл
            InputStream stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            ImageIO.write(image, "jpg", new File(pathName));

        }
        return params;
    }

}