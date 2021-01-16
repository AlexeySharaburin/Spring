package ru.netology;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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


}
